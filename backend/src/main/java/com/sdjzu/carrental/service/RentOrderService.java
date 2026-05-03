package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.mapper.ReturnOrderMapper;
import com.sdjzu.carrental.model.dto.CarInfo;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.entity.ReturnOrder;
import com.sdjzu.carrental.model.request.RentOrderRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.PageResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RentOrderService {

    private final RentOrderMapper rentOrderMapper;
    private final CarMapper carMapper;
    private final ReturnOrderMapper returnOrderMapper;

    public RentOrderService(RentOrderMapper rentOrderMapper, CarMapper carMapper, ReturnOrderMapper returnOrderMapper) {
        this.rentOrderMapper = rentOrderMapper;
        this.carMapper = carMapper;
        this.returnOrderMapper = returnOrderMapper;
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

    public PageResult<RentOrder> list(int pageNum, int pageSize, Long carId) {
        Page<RentOrder> page = rentOrderMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<RentOrder>().orderByDesc(RentOrder::getId)
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId()));
        PageResult<RentOrder> result = PageResult.of(page);
        result.summary("active", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>().eq(RentOrder::getOrderStatus, "RENTED")
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId())));
        result.summary("returned", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>().eq(RentOrder::getOrderStatus, "RETURNED")
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId())));
        enrichWithCarInfo(page.getRecords());
        enrichReturnRequestFlag(page.getRecords());
        return result;
    }

    private void enrichReturnRequestFlag(List<RentOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> orderIds = orders.stream().map(RentOrder::getId).collect(Collectors.toList());
        List<ReturnOrder> returns = returnOrderMapper.selectList(
                new LambdaQueryWrapper<ReturnOrder>().in(ReturnOrder::getRentOrderId, orderIds));
        Set<Long> hasRequest = returns.stream().map(ReturnOrder::getRentOrderId).collect(Collectors.toSet());
        Map<Long, java.math.BigDecimal> feeMap = returns.stream()
                .filter(r -> r.getExtraFee() != null)
                .collect(Collectors.toMap(ReturnOrder::getRentOrderId, ReturnOrder::getExtraFee, (a, b) -> a));
        orders.forEach(o -> {
            o.setHasReturnRequest(hasRequest.contains(o.getId()));
            o.setExtraFee(feeMap.get(o.getId()));
        });
    }

    private void enrichWithCarInfo(List<RentOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> carIds = orders.stream().map(RentOrder::getCarId).distinct().collect(Collectors.toList());
        Map<Long, Car> carMap = carMapper.selectBatchIds(carIds).stream()
                .collect(Collectors.toMap(Car::getId, c -> c, (a, b) -> a));
        for (RentOrder order : orders) {
            Car car = carMap.get(order.getCarId());
            if (car != null) {
                CarInfo info = new CarInfo();
                BeanUtils.copyProperties(car, info);
                order.setCarInfo(info);
            }
        }
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
        rentOrderMapper.updateById(rentOrder);
    }
}
