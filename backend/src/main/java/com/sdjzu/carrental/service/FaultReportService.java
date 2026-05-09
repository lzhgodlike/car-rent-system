package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.FaultReportMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.model.dto.CarInfo;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.FaultReport;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.request.FaultHandleRequest;
import com.sdjzu.carrental.model.request.FaultReportRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public FaultReportService(FaultReportMapper faultReportMapper, CarMapper carMapper,
                              RentOrderMapper rentOrderMapper, RentOrderService rentOrderService) {
        this.faultReportMapper = faultReportMapper;
        this.carMapper = carMapper;
        this.rentOrderMapper = rentOrderMapper;
        this.rentOrderService = rentOrderService;
    }

    @Transactional
    public void create(FaultReportRequest request) {
        if (!SecurityUtils.isAdmin()) {
            Long count = rentOrderMapper.selectCount(new LambdaQueryWrapper<RentOrder>()
                    .eq(RentOrder::getUserId, SecurityUtils.getUserId())
                    .eq(RentOrder::getCarId, request.getCarId()));
            if (count == null || count == 0) {
                throw new BusinessException("您没有租过该车辆，无法提交故障报告");
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
    }

    public PageResult<FaultReport> list(int pageNum, int pageSize, Long carId) {
        LambdaQueryWrapper<FaultReport> wrapper = new LambdaQueryWrapper<FaultReport>().orderByDesc(FaultReport::getId);
        if (carId != null) {
            wrapper.eq(FaultReport::getCarId, carId);
        }
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(FaultReport::getUserId, SecurityUtils.getUserId());
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
        return result;
    }

    private void enrichWithCarInfo(List<FaultReport> reports) {
        if (reports == null || reports.isEmpty()) return;
        List<Long> carIds = reports.stream().map(FaultReport::getCarId).distinct().collect(Collectors.toList());
        Map<Long, Car> carMap = carMapper.selectBatchIds(carIds).stream()
                .collect(Collectors.toMap(Car::getId, c -> c, (a, b) -> a));
        for (FaultReport report : reports) {
            Car car = carMap.get(report.getCarId());
            if (car != null) {
                CarInfo info = new CarInfo();
                BeanUtils.copyProperties(car, info);
                report.setCarInfo(info);
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
    public void completeRepair(Long id) {
        SecurityUtils.requireAdmin();
        FaultReport faultReport = faultReportMapper.selectById(id);
        if (faultReport == null) {
            throw new BusinessException("故障记录不存在");
        }
        if (!"REPAIRING".equals(faultReport.getFaultStatus())) {
            throw new BusinessException("当前故障记录不处于维修中状态");
        }

        faultReport.setFaultStatus("RESOLVED");
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
}
