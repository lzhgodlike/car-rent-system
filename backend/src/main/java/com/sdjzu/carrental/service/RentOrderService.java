package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.FaultReportMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.mapper.ReturnOrderMapper;
import com.sdjzu.carrental.model.dto.CarInfo;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.FaultReport;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.entity.ReturnOrder;
import com.sdjzu.carrental.model.request.RentOrderRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RentOrderService {

    // 订单状态常量
    public static final String PENDING_PICKUP = "PENDING_PICKUP";
    public static final String RENTED = "RENTED";
    public static final String RETURN_PENDING = "RETURN_PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private final RentOrderMapper rentOrderMapper;
    private final CarMapper carMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final FaultReportMapper faultReportMapper;

    public RentOrderService(RentOrderMapper rentOrderMapper, CarMapper carMapper,
                            ReturnOrderMapper returnOrderMapper, FaultReportMapper faultReportMapper) {
        this.rentOrderMapper = rentOrderMapper;
        this.carMapper = carMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.faultReportMapper = faultReportMapper;
    }

    @Transactional
    public void create(RentOrderRequest request) {
        if (!request.getExpectedReturnDate().isAfter(request.getRentDate())) {
            throw new BusinessException("预计还车日期必须晚于租车日期");
        }
        if (request.getRentDate().isBefore(LocalDate.now())) {
            throw new BusinessException("租车日期不能早于今天");
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
        rentOrder.setOrderStatus(PENDING_PICKUP);
        rentOrder.setRemark(request.getRemark());
        rentOrderMapper.insert(rentOrder);

        recalculateCarStatus(request.getCarId());
    }

    /**
     * 管理员确认取车：PENDING_PICKUP → RENTED
     */
    @Transactional
    public void pickup(Long id) {
        SecurityUtils.requireAdmin();
        RentOrder rentOrder = rentOrderMapper.selectById(id);
        if (rentOrder == null) {
            throw new BusinessException("租车订单不存在");
        }
        if (!PENDING_PICKUP.equals(rentOrder.getOrderStatus())) {
            throw new BusinessException("当前订单状态不允许确认取车");
        }
        rentOrder.setOrderStatus(RENTED);
        rentOrderMapper.updateById(rentOrder);

        recalculateCarStatus(rentOrder.getCarId());
    }

    /**
     * 取消订单：PENDING_PICKUP → CANCELLED
     */
    @Transactional
    public void cancel(Long id) {
        RentOrder rentOrder = rentOrderMapper.selectById(id);
        if (rentOrder == null) {
            throw new BusinessException("租车订单不存在");
        }
        // 非管理员只能取消自己的订单
        if (!SecurityUtils.isAdmin() && !rentOrder.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能取消自己的订单");
        }
        if (!PENDING_PICKUP.equals(rentOrder.getOrderStatus())) {
            throw new BusinessException("只有待取车的订单才能取消");
        }
        rentOrder.setOrderStatus(CANCELLED);
        rentOrderMapper.updateById(rentOrder);

        recalculateCarStatus(rentOrder.getCarId());
    }

    public PageResult<RentOrder> list(int pageNum, int pageSize, Long carId) {
        Page<RentOrder> page = rentOrderMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<RentOrder>().orderByDesc(RentOrder::getId)
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId()));
        PageResult<RentOrder> result = PageResult.of(page);

        result.summary("pendingPickup", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, PENDING_PICKUP)));
        result.summary("active", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, RENTED)));
        result.summary("returnPending", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, RETURN_PENDING)));
        result.summary("completed", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, COMPLETED)));
        result.summary("cancelled", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, CANCELLED)));

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
        Map<Long, BigDecimal> feeMap = returns.stream()
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

    /**
     * 管理员直接修改订单状态（保留通用能力，但禁止直接设为 COMPLETED）
     */
    @Transactional
    public void updateStatus(Long id, String status) {
        SecurityUtils.requireAdmin();
        RentOrder rentOrder = rentOrderMapper.selectById(id);
        if (rentOrder == null) {
            throw new BusinessException("租车订单不存在");
        }
        if (COMPLETED.equals(status)) {
            throw new BusinessException("请在还车信息管理中确认还车，不能直接修改为已完成");
        }
        if (RETURN_PENDING.equals(status)) {
            throw new BusinessException("请由用户发起还车申请，不能直接修改为待确认还车");
        }
        rentOrder.setOrderStatus(status);
        rentOrderMapper.updateById(rentOrder);

        recalculateCarStatus(rentOrder.getCarId());
    }

    /**
     * 根据车辆的订单和工单状态，自动推导车辆状态
     */
    public void recalculateCarStatus(Long carId) {
        Car car = carMapper.selectById(carId);
        if (car == null || "DISABLED".equals(car.getStatus())) {
            return; // 停用状态不自动变更
        }

        // 检查是否有活跃订单
        boolean hasPickupOrder = rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(RentOrder::getCarId, carId)
                        .eq(RentOrder::getOrderStatus, PENDING_PICKUP)) > 0;
        boolean hasActiveOrder = rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(RentOrder::getCarId, carId)
                        .in(RentOrder::getOrderStatus, RENTED, RETURN_PENDING)) > 0;

        // 检查是否有活跃工单
        boolean hasPendingFault = faultReportMapper.selectCount(
                new LambdaQueryWrapper<FaultReport>()
                        .eq(FaultReport::getCarId, carId)
                        .eq(FaultReport::getFaultStatus, "PENDING")) > 0;
        boolean hasRepairingFault = faultReportMapper.selectCount(
                new LambdaQueryWrapper<FaultReport>()
                        .eq(FaultReport::getCarId, carId)
                        .eq(FaultReport::getFaultStatus, "REPAIRING")) > 0;

        String newStatus;
        if (hasPickupOrder) {
            newStatus = "RESERVED";
        } else if (hasActiveOrder) {
            newStatus = "RENTED";
        } else if (hasRepairingFault) {
            newStatus = "REPAIRING";
        } else if (hasPendingFault) {
            newStatus = "AWAITING_REPAIR";
        } else {
            newStatus = "AVAILABLE";
        }

        car.setStatus(newStatus);
        carMapper.updateById(car);
    }
}
