package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.mapper.ReturnOrderMapper;
import com.sdjzu.carrental.model.dto.CarInfo;
import com.sdjzu.carrental.model.dto.RentOrderBrief;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.entity.ReturnOrder;
import com.sdjzu.carrental.model.request.ReturnConfirmRequest;
import com.sdjzu.carrental.model.request.ReturnOrderRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReturnOrderService {

    private final ReturnOrderMapper returnOrderMapper;
    private final RentOrderMapper rentOrderMapper;
    private final CarMapper carMapper;
    private final RentOrderService rentOrderService;

    public ReturnOrderService(ReturnOrderMapper returnOrderMapper, RentOrderMapper rentOrderMapper,
                              CarMapper carMapper, RentOrderService rentOrderService) {
        this.returnOrderMapper = returnOrderMapper;
        this.rentOrderMapper = rentOrderMapper;
        this.carMapper = carMapper;
        this.rentOrderService = rentOrderService;
    }

    @Transactional
    public void create(ReturnOrderRequest request) {
        RentOrder rentOrder = rentOrderMapper.selectById(request.getRentOrderId());
        if (rentOrder == null) {
            throw new BusinessException("租车订单不存在");
        }
        if (!SecurityUtils.isAdmin() && !rentOrder.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能提交自己的还车申请");
        }
        if (!RentOrderService.RENTED.equals(rentOrder.getOrderStatus())) {
            throw new BusinessException("当前订单状态不允许申请还车");
        }

        ReturnOrder exists = returnOrderMapper.selectOne(new LambdaQueryWrapper<ReturnOrder>()
                .eq(ReturnOrder::getRentOrderId, request.getRentOrderId()));
        if (exists != null) {
            throw new BusinessException("该订单已提交还车申请");
        }

        // 校验还车公里数必须大于出租时公里数
        Car car = carMapper.selectById(rentOrder.getCarId());
        if (car != null && request.getActualMileage() != null && request.getActualMileage() <= car.getMileage()) {
            throw new BusinessException("还车公里数必须大于出租时的 " + car.getMileage() + " km");
        }

        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setRentOrderId(request.getRentOrderId());
        returnOrder.setActualReturnTime(LocalDateTime.now());
        returnOrder.setActualMileage(request.getActualMileage());
        returnOrder.setDamageDesc(request.getDamageDesc());
        returnOrder.setStatus("PENDING");
        returnOrderMapper.insert(returnOrder);

        // 更新订单状态为待确认还车
        rentOrder.setOrderStatus(RentOrderService.RETURN_PENDING);
        rentOrderMapper.updateById(rentOrder);

        // 重新推导车辆状态
        rentOrderService.recalculateCarStatus(rentOrder.getCarId());
    }

    public PageResult<ReturnOrder> list(int pageNum, int pageSize) {
        if (SecurityUtils.isAdmin()) {
            Page<ReturnOrder> page = returnOrderMapper.selectPage(new Page<>(pageNum, pageSize),
                    new LambdaQueryWrapper<ReturnOrder>().orderByDesc(ReturnOrder::getId));
            PageResult<ReturnOrder> result = PageResult.of(page);
            result.summary("pending", returnOrderMapper.selectCount(
                    new LambdaQueryWrapper<ReturnOrder>().eq(ReturnOrder::getStatus, "PENDING")));
            result.summary("confirmed", returnOrderMapper.selectCount(
                    new LambdaQueryWrapper<ReturnOrder>().eq(ReturnOrder::getStatus, "CONFIRMED")));
            enrichWithCarInfo(page.getRecords());
            return result;
        }

        List<Long> rentIds = rentOrderMapper.selectList(new LambdaQueryWrapper<RentOrder>()
                        .eq(RentOrder::getUserId, SecurityUtils.getUserId()))
                .stream()
                .map(RentOrder::getId)
                .collect(Collectors.toList());
        if (rentIds.isEmpty()) {
            PageResult<ReturnOrder> result = new PageResult<>();
            result.setRecords(Collections.emptyList());
            result.setTotal(0);
            result.setPageNum(pageNum);
            result.setPageSize(pageSize);
            return result;
        }
        Page<ReturnOrder> page = returnOrderMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ReturnOrder>()
                        .in(ReturnOrder::getRentOrderId, rentIds)
                        .orderByDesc(ReturnOrder::getId));
        PageResult<ReturnOrder> result = PageResult.of(page);
        result.summary("pending", returnOrderMapper.selectCount(
                new LambdaQueryWrapper<ReturnOrder>().in(ReturnOrder::getRentOrderId, rentIds).eq(ReturnOrder::getStatus, "PENDING")));
        result.summary("confirmed", returnOrderMapper.selectCount(
                new LambdaQueryWrapper<ReturnOrder>().in(ReturnOrder::getRentOrderId, rentIds).eq(ReturnOrder::getStatus, "CONFIRMED")));
        enrichWithCarInfo(page.getRecords());
        return result;
    }

    private void enrichWithCarInfo(List<ReturnOrder> returns) {
        if (returns == null || returns.isEmpty()) return;
        List<Long> rentOrderIds = returns.stream().map(ReturnOrder::getRentOrderId).distinct().collect(Collectors.toList());
        Map<Long, RentOrder> rentOrderMap = rentOrderMapper.selectBatchIds(rentOrderIds).stream()
                .collect(Collectors.toMap(RentOrder::getId, r -> r, (a, b) -> a));
        List<Long> carIds = rentOrderMap.values().stream().map(RentOrder::getCarId).distinct().collect(Collectors.toList());
        Map<Long, Car> carMap = carIds.isEmpty() ? Collections.emptyMap() :
                carMapper.selectBatchIds(carIds).stream()
                        .collect(Collectors.toMap(Car::getId, c -> c, (a, b) -> a));
        for (ReturnOrder ret : returns) {
            RentOrder rentOrder = rentOrderMap.get(ret.getRentOrderId());
            if (rentOrder != null) {
                RentOrderBrief brief = new RentOrderBrief();
                BeanUtils.copyProperties(rentOrder, brief);
                ret.setRentOrderBrief(brief);
                Car car = carMap.get(rentOrder.getCarId());
                if (car != null) {
                    CarInfo info = new CarInfo();
                    BeanUtils.copyProperties(car, info);
                    ret.setCarInfo(info);
                }
            }
        }
    }

    /**
     * 管理员确认还车：还车记录 CONFIRMED，订单 COMPLETED，车辆重新推导
     */
    @Transactional
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

        // 更新还车记录
        returnOrder.setExtraFee(request.getExtraFee());
        returnOrder.setStatus("CONFIRMED");
        returnOrder.setOperatorId(SecurityUtils.getUserId());
        returnOrderMapper.updateById(returnOrder);

        // 更新订单为已完成
        rentOrder.setOrderStatus(RentOrderService.COMPLETED);
        rentOrder.setActualReturnDate(LocalDateTime.now().toLocalDate());
        rentOrderMapper.updateById(rentOrder);

        // 更新车辆公里数
        if (returnOrder.getActualMileage() != null) {
            car.setMileage(returnOrder.getActualMileage());
            carMapper.updateById(car);
        }

        // 重新推导车辆状态
        rentOrderService.recalculateCarStatus(rentOrder.getCarId());
    }
}
