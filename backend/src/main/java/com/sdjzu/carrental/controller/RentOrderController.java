package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.request.RentOrderRequest;
import com.sdjzu.carrental.service.RentOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rent-orders")
public class RentOrderController {

    private final RentOrderService rentOrderService;

    public RentOrderController(RentOrderService rentOrderService) {
        this.rentOrderService = rentOrderService;
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody RentOrderRequest request) {
        rentOrderService.create(request);
        return ApiResponse.success("下单成功", null);
    }

    @GetMapping
    public ApiResponse<PageResult<RentOrder>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) Long carId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String keyword) {
        return ApiResponse.success(rentOrderService.list(pageNum, pageSize, carId, status, keyword));
    }

    @PutMapping("/{id}/pickup")
    public ApiResponse<Void> pickup(@PathVariable Long id) {
        rentOrderService.pickup(id);
        return ApiResponse.success("确认取车成功", null);
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        rentOrderService.cancel(id);
        return ApiResponse.success("订单已取消", null);
    }

    @PutMapping("/{id}/reject-pickup")
    public ApiResponse<Void> rejectPickup(@PathVariable Long id) {
        rentOrderService.rejectPickup(id);
        return ApiResponse.success("已拒绝取车", null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        rentOrderService.updateStatus(id, status);
        return ApiResponse.success("状态更新成功", null);
    }

    @PutMapping("/{id}/remind-return")
    public ApiResponse<Void> remindReturn(@PathVariable Long id) {
        rentOrderService.remindReturn(id);
        return ApiResponse.success("提醒已发送", null);
    }
}
