package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.mapper.ReturnOrderMapper;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.entity.ReturnOrder;
import com.sdjzu.carrental.model.request.ReturnConfirmRequest;
import com.sdjzu.carrental.model.request.ReturnOrderRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReturnOrderService {

    private final ReturnOrderMapper returnOrderMapper;
    private final RentOrderMapper rentOrderMapper;
    private final CarMapper carMapper;

    public ReturnOrderService(ReturnOrderMapper returnOrderMapper, RentOrderMapper rentOrderMapper, CarMapper carMapper) {
        this.returnOrderMapper = returnOrderMapper;
        this.rentOrderMapper = rentOrderMapper;
        this.carMapper = carMapper;
    }

    public void create(ReturnOrderRequest request) {
        RentOrder rentOrder = rentOrderMapper.selectById(request.getRentOrderId());
        if (rentOrder == null) {
            throw new BusinessException("租车订单不存在");
        }
        if (!SecurityUtils.isAdmin() && !rentOrder.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能提交自己的还车申请");
        }
        if (!"RENTED".equals(rentOrder.getOrderStatus())) {
            throw new BusinessException("当前订单不允许还车");
        }
        ReturnOrder exists = returnOrderMapper.selectOne(new LambdaQueryWrapper<ReturnOrder>()
                .eq(ReturnOrder::getRentOrderId, request.getRentOrderId()));
        if (exists != null) {
            throw new BusinessException("该订单已提交还车申请");
        }
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setRentOrderId(request.getRentOrderId());
        returnOrder.setActualReturnTime(LocalDateTime.now());
        returnOrder.setActualMileage(request.getActualMileage());
        returnOrder.setDamageDesc(request.getDamageDesc());
        returnOrder.setStatus("PENDING");
        returnOrderMapper.insert(returnOrder);
    }

    public List<ReturnOrder> list() {
        if (SecurityUtils.isAdmin()) {
            return returnOrderMapper.selectList(new LambdaQueryWrapper<ReturnOrder>().orderByDesc(ReturnOrder::getId));
        }

        List<Long> rentIds = rentOrderMapper.selectList(new LambdaQueryWrapper<RentOrder>()
                        .eq(RentOrder::getUserId, SecurityUtils.getUserId()))
                .stream()
                .map(RentOrder::getId)
                .collect(Collectors.toList());
        if (rentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return returnOrderMapper.selectList(new LambdaQueryWrapper<ReturnOrder>()
                .in(ReturnOrder::getRentOrderId, rentIds)
                .orderByDesc(ReturnOrder::getId));
    }

    public void confirm(Long id, ReturnConfirmRequest request) {
        SecurityUtils.requireAdmin();
        ReturnOrder returnOrder = returnOrderMapper.selectById(id);
        if (returnOrder == null) {
            throw new BusinessException("还车记录不存在");
        }
        if (!"PENDING".equals(returnOrder.getStatus())) {
            throw new BusinessException("该还车申请已处理");
        }

        RentOrder rentOrder = rentOrderMapper.selectById(returnOrder.getRentOrderId());
        if (rentOrder == null) {
            throw new BusinessException("关联租车订单不存在");
        }
        Car car = carMapper.selectById(rentOrder.getCarId());
        if (car == null) {
            throw new BusinessException("关联车辆不存在");
        }

        returnOrder.setExtraFee(request.getExtraFee());
        returnOrder.setStatus("CONFIRMED");
        returnOrder.setOperatorId(SecurityUtils.getUserId());
        returnOrderMapper.updateById(returnOrder);

        rentOrder.setOrderStatus("RETURNED");
        rentOrder.setActualReturnDate(LocalDateTime.now().toLocalDate());
        rentOrderMapper.updateById(rentOrder);

        car.setMileage(returnOrder.getActualMileage() == null ? 0 : returnOrder.getActualMileage());
        car.setStatus("AVAILABLE");
        carMapper.updateById(car);
    }
}
