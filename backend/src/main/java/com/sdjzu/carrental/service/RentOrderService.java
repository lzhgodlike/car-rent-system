package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.CarMapper;
import com.sdjzu.carrental.mapper.CarTypeMapper;
import com.sdjzu.carrental.mapper.FaultReportMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.mapper.ReturnOrderMapper;
import com.sdjzu.carrental.mapper.UserMapper;
import com.sdjzu.carrental.model.dto.CarInfo;
import com.sdjzu.carrental.model.entity.Car;
import com.sdjzu.carrental.model.entity.CarType;
import com.sdjzu.carrental.model.entity.FaultReport;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.entity.ReturnOrder;
import com.sdjzu.carrental.model.entity.User;
import com.sdjzu.carrental.model.request.RentOrderRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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

    // 支付状态常量
    public static final String PAYMENT_UNPAID = "UNPAID";
    public static final String PAYMENT_PAID = "PAID";

    private final RentOrderMapper rentOrderMapper;
    private final CarMapper carMapper;
    private final CarTypeMapper carTypeMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final FaultReportMapper faultReportMapper;
    private final UserMapper userMapper;
    private final CarService carService;
    private final MessageNoticeService messageNoticeService;

    public RentOrderService(RentOrderMapper rentOrderMapper, CarMapper carMapper,
                            CarTypeMapper carTypeMapper, ReturnOrderMapper returnOrderMapper,
                            FaultReportMapper faultReportMapper, UserMapper userMapper,
                            CarService carService, MessageNoticeService messageNoticeService) {
        this.rentOrderMapper = rentOrderMapper;
        this.carMapper = carMapper;
        this.carTypeMapper = carTypeMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.faultReportMapper = faultReportMapper;
        this.userMapper = userMapper;
        this.carService = carService;
        this.messageNoticeService = messageNoticeService;
    }

    @Transactional
    public Long create(RentOrderRequest request) {
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
        rentOrder.setPaymentStatus(PAYMENT_UNPAID);
        rentOrder.setRemark(request.getRemark());
        rentOrderMapper.insert(rentOrder);

        recalculateCarStatus(request.getCarId());

        if (!SecurityUtils.isAdmin()) {
            String displayName = resolveUserDisplayName(rentOrder.getUserId());
            messageNoticeService.notifyAdmins(
                    "新租车订单",
                    displayName + " 提交了租车订单 " + rentOrder.getOrderNo() + "，车辆：" + car.getBrand() + " " + car.getModel(),
                    "RENT_ORDER_CREATED",
                    "RENT_ORDER",
                    rentOrder.getId()
            );
        }
        return rentOrder.getId();
    }

    /**
     * 模拟支付：更新订单支付状态
     */
    @Transactional
    public void pay(Long orderId, String paymentMethod) {
        RentOrder rentOrder = rentOrderMapper.selectById(orderId);
        if (rentOrder == null) {
            throw new BusinessException("订单不存在");
        }
        if (!SecurityUtils.isAdmin() && !rentOrder.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能支付自己的订单");
        }
        if (PAYMENT_PAID.equals(rentOrder.getPaymentStatus())) {
            throw new BusinessException("该订单已支付");
        }
        rentOrder.setPaymentStatus(PAYMENT_PAID);
        rentOrder.setPaymentMethod(paymentMethod);
        rentOrder.setPaymentTime(LocalDateTime.now());
        rentOrderMapper.updateById(rentOrder);
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
        if (PAYMENT_UNPAID.equals(rentOrder.getPaymentStatus())) {
            throw new BusinessException("订单未支付，无法确认取车");
        }
        rentOrder.setOrderStatus(RENTED);
        rentOrderMapper.updateById(rentOrder);

        recalculateCarStatus(rentOrder.getCarId());
    }

    /**
     * 管理员拒绝取车：PENDING_PICKUP → CANCELLED
     */
    @Transactional
    public void rejectPickup(Long id) {
        SecurityUtils.requireAdmin();
        RentOrder rentOrder = rentOrderMapper.selectById(id);
        if (rentOrder == null) {
            throw new BusinessException("租车订单不存在");
        }
        if (!PENDING_PICKUP.equals(rentOrder.getOrderStatus())) {
            throw new BusinessException("当前订单状态不允许拒绝取车");
        }
        rentOrder.setOrderStatus(CANCELLED);
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

    @Transactional
    public void remindReturn(Long id) {
        SecurityUtils.requireAdmin();
        RentOrder rentOrder = rentOrderMapper.selectById(id);
        if (rentOrder == null) {
            throw new BusinessException("租车订单不存在");
        }
        if (!RENTED.equals(rentOrder.getOrderStatus())) {
            throw new BusinessException("只有租赁中的订单才能发送还车提醒");
        }
        Car car = carMapper.selectById(rentOrder.getCarId());
        String carName = car == null ? "该车辆" : (car.getBrand() + " " + car.getModel());
        String plateNumber = car == null || !StringUtils.hasText(car.getPlateNumber()) ? "未知" : car.getPlateNumber();
        messageNoticeService.notifyUser(
                rentOrder.getUserId(),
                "还车提醒",
                "管理员提醒您尽快归还" + carName + "，车牌号为" + plateNumber,
                "RETURN_REMINDER",
                "RENT_ORDER",
                rentOrder.getId()
        );
    }

    public PageResult<RentOrder> list(int pageNum, int pageSize, Long carId, String status, String keyword) {
        Set<String> statuses = parseStatuses(status);
        LambdaQueryWrapper<RentOrder> wrapper = new LambdaQueryWrapper<RentOrder>().orderByDesc(RentOrder::getId)
                .eq(carId != null, RentOrder::getCarId, carId)
                .in(!statuses.isEmpty(), RentOrder::getOrderStatus, statuses)
                .eq(!SecurityUtils.isAdmin(), RentOrder::getUserId, SecurityUtils.getUserId());

        if (StringUtils.hasText(keyword)) {
            List<Long> matchedCarIds = carMapper.selectList(
                            new LambdaQueryWrapper<Car>()
                                    .like(Car::getBrand, keyword)
                                    .or().like(Car::getModel, keyword)
                                    .or().like(Car::getPlateNumber, keyword))
                    .stream().map(Car::getId).collect(Collectors.toList());
            List<Long> matchedUserIds = userMapper.selectList(
                            new LambdaQueryWrapper<User>()
                                    .like(User::getUsername, keyword)
                                    .or().like(User::getRealName, keyword))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (!matchedCarIds.isEmpty() || !matchedUserIds.isEmpty()) {
                wrapper.and(w -> {
                    w.like(RentOrder::getOrderNo, keyword);
                    if (!matchedCarIds.isEmpty()) w.or().in(RentOrder::getCarId, matchedCarIds);
                    if (!matchedUserIds.isEmpty()) w.or().in(RentOrder::getUserId, matchedUserIds);
                });
            } else {
                wrapper.and(w -> w.like(RentOrder::getOrderNo, keyword));
            }
        }

        Page<RentOrder> page = rentOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        PageResult<RentOrder> result = PageResult.of(page);

        boolean isAdmin = SecurityUtils.isAdmin();
        result.summary("pendingPickup", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!isAdmin, RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, PENDING_PICKUP)));
        result.summary("active", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!isAdmin, RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, RENTED)));
        result.summary("returnPending", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!isAdmin, RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, RETURN_PENDING)));
        result.summary("completed", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!isAdmin, RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, COMPLETED)));
        result.summary("cancelled", rentOrderMapper.selectCount(
                new LambdaQueryWrapper<RentOrder>()
                        .eq(carId != null, RentOrder::getCarId, carId)
                        .eq(!isAdmin, RentOrder::getUserId, SecurityUtils.getUserId())
                        .eq(RentOrder::getOrderStatus, CANCELLED)));

        enrichWithCarInfo(page.getRecords());
        enrichReturnRequestFlag(page.getRecords());
        enrichWithUserName(page.getRecords());
        enrichAvailableActions(page.getRecords());
        return result;
    }

    private Set<String> parseStatuses(String status) {
        if (!StringUtils.hasText(status)) {
            return Set.of();
        }
        if ("active".equalsIgnoreCase(status.trim())) {
            return Set.of(PENDING_PICKUP, RENTED);
        }
        return Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private void enrichReturnRequestFlag(List<RentOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> orderIds = orders.stream().map(RentOrder::getId).collect(Collectors.toList());
        List<ReturnOrder> returns = returnOrderMapper.selectList(
                new LambdaQueryWrapper<ReturnOrder>().in(ReturnOrder::getRentOrderId, orderIds));
        Map<Long, ReturnOrder> returnMap = returns.stream()
                .collect(Collectors.toMap(ReturnOrder::getRentOrderId, r -> r, (a, b) -> a));
        orders.forEach(o -> {
            ReturnOrder ret = returnMap.get(o.getId());
            o.setHasReturnRequest(ret != null);
            o.setExtraFee(ret != null && ret.getExtraFee() != null ? ret.getExtraFee() : BigDecimal.ZERO);
            o.setReturnOrder(ret);
        });
    }

    private void enrichWithUserName(List<RentOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> userIds = orders.stream().map(RentOrder::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        orders.forEach(o -> {
            User user = userMap.get(o.getUserId());
            if (user != null) {
                o.setUserName(user.getUsername());
                o.setUserRealName(user.getRealName());
                o.setUserPhone(user.getPhone());
                o.setUserIdCard(user.getIdCard());
            }
        });
    }

    private void enrichAvailableActions(List<RentOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        java.time.LocalDate today = java.time.LocalDate.now();
        for (RentOrder o : orders) {
            java.util.List<String> actions = new java.util.ArrayList<>();
            actions.add("view_detail");
            // 未支付订单显示支付按钮
            if (PAYMENT_UNPAID.equals(o.getPaymentStatus()) && !CANCELLED.equals(o.getOrderStatus())) {
                actions.add("pay");
            }
            switch (o.getOrderStatus()) {
                case PENDING_PICKUP:
                    actions.add("cancel");
                    break;
                case RENTED:
                    actions.add("return_car");
                    break;
                case RETURN_PENDING:
                    actions.add("return_pending");
                    break;
                case COMPLETED:
                    actions.add("repurchase");
                    // 检查是否有未支付的附加费用
                    if (o.getReturnOrder() != null && o.getReturnOrder().getExtraFee() != null
                            && o.getReturnOrder().getExtraFee().compareTo(BigDecimal.ZERO) > 0
                            && "UNPAID".equals(o.getReturnOrder().getExtraFeePaymentStatus())) {
                        actions.add("pay_extra_fee");
                    }
                    if (o.getActualReturnDate() != null) {
                        long days = java.time.temporal.ChronoUnit.DAYS.between(o.getActualReturnDate(), today);
                        if (days <= 7) actions.add("report_fault");
                    }
                    break;
                case CANCELLED:
                    actions.add("repurchase");
                    if (o.getRentDate() != null) {
                        long days = java.time.temporal.ChronoUnit.DAYS.between(o.getRentDate(), today);
                        if (days <= 7) actions.add("report_fault");
                    }
                    break;
            }
            o.setAvailableActions(actions);
        }
    }

    private void enrichWithCarInfo(List<RentOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> carIds = orders.stream().map(RentOrder::getCarId).distinct().collect(Collectors.toList());
        List<Car> cars = carService.enrichCarsForDisplay(carMapper.selectBatchIds(carIds), true);
        Map<Long, Car> carMap = cars.stream().collect(Collectors.toMap(Car::getId, c -> c, (a, b) -> a));
        List<Long> typeIds = carMap.values().stream().map(Car::getTypeId).filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, String> typeNameMap = typeIds.isEmpty() ? java.util.Collections.emptyMap() :
                carTypeMapper.selectBatchIds(typeIds).stream()
                        .collect(Collectors.toMap(CarType::getId, CarType::getTypeName, (a, b) -> a));
        for (RentOrder order : orders) {
            Car car = carMap.get(order.getCarId());
            if (car != null) {
                CarInfo info = new CarInfo();
                BeanUtils.copyProperties(car, info);
                info.setTypeName(typeNameMap.get(car.getTypeId()));
                info.setCarImages(car.getCarImages() == null ? java.util.List.of() : car.getCarImages().stream()
                        .map(image -> image.getImageUrl())
                        .collect(Collectors.toList()));
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
