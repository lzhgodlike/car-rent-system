package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.FaultReportMapper;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.FaultReport;
import com.sdjzu.carrental.model.request.FaultHandleRequest;
import com.sdjzu.carrental.model.request.FaultReportRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FaultReportService {

    private final FaultReportMapper faultReportMapper;
    private final CarMapper carMapper;

    public FaultReportService(FaultReportMapper faultReportMapper, CarMapper carMapper) {
        this.faultReportMapper = faultReportMapper;
        this.carMapper = carMapper;
    }

    public void create(FaultReportRequest request) {
        FaultReport faultReport = new FaultReport();
        faultReport.setUserId(SecurityUtils.getUserId());
        faultReport.setCarId(request.getCarId());
        faultReport.setFaultContent(request.getFaultContent());
        faultReport.setFaultStatus("PENDING");
        faultReport.setReportTime(LocalDateTime.now());
        faultReportMapper.insert(faultReport);
    }

    public List<FaultReport> list() {
        LambdaQueryWrapper<FaultReport> wrapper = new LambdaQueryWrapper<FaultReport>().orderByDesc(FaultReport::getId);
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(FaultReport::getUserId, SecurityUtils.getUserId());
        }
        return faultReportMapper.selectList(wrapper);
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

        car.setStatus("AVAILABLE");
        carMapper.updateById(car);
    }
}
