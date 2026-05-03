package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.PageResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FaultReportService {

    private final FaultReportMapper faultReportMapper;
    private final CarMapper carMapper;
    private final RentOrderMapper rentOrderMapper;

    public FaultReportService(FaultReportMapper faultReportMapper, CarMapper carMapper, RentOrderMapper rentOrderMapper) {
        this.faultReportMapper = faultReportMapper;
        this.carMapper = carMapper;
        this.rentOrderMapper = rentOrderMapper;
    }

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

    public void handle(Long id, FaultHandleRequest request) {
        SecurityUtils.requireAdmin();
        FaultReport faultReport = faultReportMapper.selectById(id);
        if (faultReport == null) {
            throw new BusinessException("故障记录不存在");
        }
        faultReport.setFaultStatus("REPAIRING");
        faultReport.setHandleResult(request.getHandleResult());
        faultReport.setHandleTime(LocalDateTime.now());
        faultReportMapper.updateById(faultReport);

        Car car = carMapper.selectById(faultReport.getCarId());
        if (car != null) {
            car.setStatus("MAINTENANCE");
            carMapper.updateById(car);
        }
    }

    public void completeRepair(Long id) {
        SecurityUtils.requireAdmin();
        FaultReport faultReport = faultReportMapper.selectById(id);
        if (faultReport == null) {
            throw new BusinessException("故障记录不存在");
        }
        if (!"REPAIRING".equals(faultReport.getFaultStatus()) && !"RESOLVED".equals(faultReport.getFaultStatus())) {
            throw new BusinessException("当前故障记录不处于可完修状态");
        }

        Car car = carMapper.selectById(faultReport.getCarId());
        if (car == null) {
            throw new BusinessException("关联车辆不存在");
        }

        faultReport.setFaultStatus("RESOLVED");
        faultReportMapper.updateById(faultReport);

        Long openFaults = faultReportMapper.selectCount(new LambdaQueryWrapper<FaultReport>()
                .eq(FaultReport::getCarId, faultReport.getCarId())
                .ne(FaultReport::getId, id)
                .in(FaultReport::getFaultStatus, "PENDING", "REPAIRING"));
        if (openFaults == null || openFaults == 0) {
            car.setStatus("AVAILABLE");
            carMapper.updateById(car);
        }
    }
}
