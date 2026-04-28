package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.request.RentOrderRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RentOrderService {

    private final RentOrderMapper rentOrderMapper;
    private final CarMapper carMapper;

    public RentOrderService(RentOrderMapper rentOrderMapper, CarMapper carMapper) {
        this.rentOrderMapper = rentOrderMapper;
        this.carMapper = carMapper;
    }

    public void create(RentOrderRequest request) {
        if (!request.getExpectedReturnDate().isAfter(request.getRentDate())) {
            throw new BusinessException("预计还车日期必须晚于租车日期");
        }

        Car car = carMapper.selectById(request.getCarId());
        if (car == null) {
            throw new BusinessException("车辆不存在");
        }
        if (!"AVAILABLE".equals(car.getStatus())) {
            throw new BusinessException("当前车辆不可租赁");
        }

        int days = (int) ChronoUnit.DAYS.between(request.getRentDate(), request.getExpectedReturnDate());
        BigDecimal totalPrice = car.getDayPrice().multiply(BigDecimal.valueOf(days));

        RentOrder rentOrder = new RentOrder();
        rentOrder.setOrderNo("RENT" + System.currentTimeMillis());
        rentOrder.setUserId(SecurityUtils.getUserId());
        rentOrder.setCarId(request.getCarId());
        rentOrder.setRentDate(request.getRentDate());
        rentOrder.setExpectedReturnDate(request.getExpectedReturnDate());
        rentOrder.setRentDays(days);
        rentOrder.setUnitPrice(car.getDayPrice());
        rentOrder.setTotalPrice(totalPrice);
        rentOrder.setOrderStatus("RENTED");
        rentOrder.setRemark(request.getRemark());
        rentOrderMapper.insert(rentOrder);

        car.setStatus("RENTED");
        carMapper.updateById(car);
    }

    public List<RentOrder> list() {
        LambdaQueryWrapper<RentOrder> wrapper = new LambdaQueryWrapper<RentOrder>().orderByDesc(RentOrder::getId);
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(RentOrder::getUserId, SecurityUtils.getUserId());
        }
        return rentOrderMapper.selectList(wrapper);
    }

    public void updateStatus(Long id, String status) {
        SecurityUtils.requireAdmin();
        RentOrder rentOrder = rentOrderMapper.selectById(id);
        if (rentOrder == null) {
            throw new BusinessException("租车订单不存在");
        }
        if ("RETURNED".equals(status)) {
            throw new BusinessException("请在还车信息管理中确认还车，不能直接修改租车订单状态");
        }
        rentOrder.setOrderStatus(status);
        if ("RETURNED".equals(status)) {
            rentOrder.setActualReturnDate(LocalDate.now());
        }
        rentOrderMapper.updateById(rentOrder);
    }
}
