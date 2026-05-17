package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.FaultReportMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.mapper.UserMapper;
import com.sdjzu.carrental.model.dto.CarInfo;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.FaultReport;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.entity.User;
import com.sdjzu.carrental.model.request.FaultHandleRequest;
import com.sdjzu.carrental.model.request.FaultReportRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FaultReportService {

    private final FaultReportMapper faultReportMapper;
    private final CarMapper carMapper;
    private final RentOrderMapper rentOrderMapper;
    private final RentOrderService rentOrderService;
    private final UserMapper userMapper;
    private final CarService carService;
    private final MessageNoticeService messageNoticeService;

    public FaultReportService(FaultReportMapper faultReportMapper, CarMapper carMapper,
                              RentOrderMapper rentOrderMapper, RentOrderService rentOrderService,
                              UserMapper userMapper, CarService carService,
                              MessageNoticeService messageNoticeService) {
        this.faultReportMapper = faultReportMapper;
        this.carMapper = carMapper;
        this.rentOrderMapper = rentOrderMapper;
        this.rentOrderService = rentOrderService;
        this.userMapper = userMapper;
        this.carService = carService;
        this.messageNoticeService = messageNoticeService;
    }

    @Transactional
    public void create(FaultReportRequest request) {
        if (!SecurityUtils.isAdmin()) {
            // 查找用户该车辆最近的相关订单
            RentOrder latestOrder = rentOrderMapper.selectOne(new LambdaQueryWrapper<RentOrder>()
                    .eq(RentOrder::getUserId, SecurityUtils.getUserId())
                    .eq(RentOrder::getCarId, request.getCarId())
                    .in(RentOrder::getOrderStatus, RentOrderService.RETURN_PENDING, RentOrderService.CANCELLED, RentOrderService.COMPLETED)
                    .orderByDesc(RentOrder::getId)
                    .last("LIMIT 1"));
            if (latestOrder == null) {
                throw new BusinessException("您没有租过该车辆，无法提交故障报告");
            }
            // 待还车状态可直接报修
            if (RentOrderService.RETURN_PENDING.equals(latestOrder.getOrderStatus())) {
                // 允许报修
            } else {
                // 已完成/已取消：检查7天限制
                java.time.LocalDate referenceDate = RentOrderService.COMPLETED.equals(latestOrder.getOrderStatus())
                        ? latestOrder.getActualReturnDate() : latestOrder.getRentDate();
                if (referenceDate != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, java.time.LocalDate.now());
                    if (days > 7) {
                        throw new BusinessException("已超过7天，无法提交报修申请");
                    }
                }
            }
        }
        FaultReport faultReport = new FaultReport();
        faultReport.setUserId(SecurityUtils.getUserId());
        faultReport.setCarId(request.getCarId());
        faultReport.setFaultContent(request.getFaultContent());
        faultReport.setFaultStatus("PENDING");
        faultReport.setReportTime(LocalDateTime.now());
        faultReportMapper.insert(faultReport);

        // 重新推导车辆状态
        rentOrderService.recalculateCarStatus(request.getCarId());

        if (!SecurityUtils.isAdmin()) {
            Car car = carMapper.selectById(request.getCarId());
            String carName = car == null ? "未知车辆" : (car.getBrand() + " " + car.getModel());
            messageNoticeService.notifyAdmins(
                    "故障报修提醒",
                    resolveUserDisplayName(SecurityUtils.getUserId()) + " 提交了故障报修，车辆：" + carName,
                    "FAULT_REPORT_CREATED",
                    "FAULT_REPORT",
                    faultReport.getId()
            );
        }
    }

    public PageResult<FaultReport> list(int pageNum, int pageSize, Long carId, String status, String keyword) {
        LambdaQueryWrapper<FaultReport> wrapper = new LambdaQueryWrapper<FaultReport>().orderByDesc(FaultReport::getId);
        if (carId != null) {
            wrapper.eq(FaultReport::getCarId, carId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(FaultReport::getFaultStatus, status);
        }
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(FaultReport::getUserId, SecurityUtils.getUserId());
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> carIds = carMapper.selectList(
                    new LambdaQueryWrapper<Car>()
                            .like(Car::getBrand, keyword)
                            .or().like(Car::getModel, keyword)
                            .or().like(Car::getPlateNumber, keyword)
                            .or().like(Car::getCarNo, keyword))
                    .stream().map(Car::getId).collect(Collectors.toList());
            List<Long> userIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getUsername, keyword)
                            .or().like(User::getRealName, keyword))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (!carIds.isEmpty() || !userIds.isEmpty()) {
                wrapper.and(w -> {
                    if (!carIds.isEmpty()) w.in(FaultReport::getCarId, carIds);
                    if (!carIds.isEmpty() && !userIds.isEmpty()) w.or();
                    if (!userIds.isEmpty()) w.in(FaultReport::getUserId, userIds);
                });
            } else {
                wrapper.apply("1=0");
            }
        }
        Page<FaultReport> page = faultReportMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        PageResult<FaultReport> result = PageResult.of(page);
        boolean isAdmin = SecurityUtils.isAdmin();
        result.summary("pending", faultReportMapper.selectCount(
                new LambdaQueryWrapper<FaultReport>().eq(FaultReport::getFaultStatus, "PENDING")
                        .eq(carId != null, FaultReport::getCarId, carId)
                        .eq(!isAdmin, FaultReport::getUserId, SecurityUtils.getUserId())));
        result.summary("repairing", faultReportMapper.selectCount(
                new LambdaQueryWrapper<FaultReport>().eq(FaultReport::getFaultStatus, "REPAIRING")
                        .eq(carId != null, FaultReport::getCarId, carId)
                        .eq(!isAdmin, FaultReport::getUserId, SecurityUtils.getUserId())));
        result.summary("resolved", faultReportMapper.selectCount(
                new LambdaQueryWrapper<FaultReport>().eq(FaultReport::getFaultStatus, "RESOLVED")
                        .eq(carId != null, FaultReport::getCarId, carId)
                        .eq(!isAdmin, FaultReport::getUserId, SecurityUtils.getUserId())));
        enrichWithCarInfo(page.getRecords());
        enrichWithReporterName(page.getRecords());
        return result;
    }

    private void enrichWithCarInfo(List<FaultReport> reports) {
        if (reports == null || reports.isEmpty()) return;
        List<Long> carIds = reports.stream().map(FaultReport::getCarId).distinct().collect(Collectors.toList());
        List<Car> cars = carService.enrichCarsForDisplay(carMapper.selectBatchIds(carIds), true);
        Map<Long, Car> carMap = cars.stream().collect(Collectors.toMap(Car::getId, c -> c, (a, b) -> a));
        for (FaultReport report : reports) {
            Car car = carMap.get(report.getCarId());
            if (car != null) {
                CarInfo info = new CarInfo();
                BeanUtils.copyProperties(car, info);
                info.setCarImages(car.getCarImages() == null ? java.util.List.of() : car.getCarImages().stream()
                        .map(image -> image.getImageUrl())
                        .collect(Collectors.toList()));
                report.setCarInfo(info);
            }
        }
    }

    private void enrichWithReporterName(List<FaultReport> reports) {
        if (reports == null || reports.isEmpty()) return;
        List<Long> userIds = reports.stream().map(FaultReport::getUserId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) return;
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        for (FaultReport report : reports) {
            User user = userMap.get(report.getUserId());
            if (user != null) {
                report.setReporterName(user.getUsername());
            }
        }
    }

    /**
     * 管理员处理故障：PENDING → REPAIRING，车辆状态自动推导
     */
    @Transactional
    public void handle(Long id, FaultHandleRequest request) {
        SecurityUtils.requireAdmin();
        FaultReport faultReport = faultReportMapper.selectById(id);
        if (faultReport == null) {
            throw new BusinessException("故障记录不存在");
        }
        if (!"PENDING".equals(faultReport.getFaultStatus())) {
            throw new BusinessException("当前故障记录不处于待处理状态");
        }
        faultReport.setFaultStatus("REPAIRING");
        faultReport.setHandleResult(request.getHandleResult());
        faultReport.setHandleTime(LocalDateTime.now());
        faultReportMapper.updateById(faultReport);

        rentOrderService.recalculateCarStatus(faultReport.getCarId());
    }

    /**
     * 管理员完成维修：REPAIRING → RESOLVED，车辆状态自动推导
     */
    @Transactional
    public void completeRepair(Long id, FaultHandleRequest request) {
        SecurityUtils.requireAdmin();
        FaultReport faultReport = faultReportMapper.selectById(id);
        if (faultReport == null) {
            throw new BusinessException("故障记录不存在");
        }
        if (!"REPAIRING".equals(faultReport.getFaultStatus())) {
            throw new BusinessException("当前故障记录不处于维修中状态");
        }

        faultReport.setFaultStatus("RESOLVED");
        if (request != null && request.getHandleResult() != null) {
            faultReport.setHandleResult(request.getHandleResult());
        }
        faultReport.setHandleTime(LocalDateTime.now());
        faultReportMapper.updateById(faultReport);

        rentOrderService.recalculateCarStatus(faultReport.getCarId());
    }

    /**
     * 管理员拒绝故障报告：PENDING → REJECTED
     */
    @Transactional
    public void reject(Long id, FaultHandleRequest request) {
        SecurityUtils.requireAdmin();
        FaultReport faultReport = faultReportMapper.selectById(id);
        if (faultReport == null) {
            throw new BusinessException("故障记录不存在");
        }
        if (!"PENDING".equals(faultReport.getFaultStatus())) {
            throw new BusinessException("只能拒绝待处理的故障报告");
        }
        faultReport.setFaultStatus("REJECTED");
        faultReport.setHandleResult(request.getHandleResult());
        faultReport.setHandleTime(LocalDateTime.now());
        faultReportMapper.updateById(faultReport);

        rentOrderService.recalculateCarStatus(faultReport.getCarId());
    }

    private String resolveUserDisplayName(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "用户";
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "用户";
    }
}
