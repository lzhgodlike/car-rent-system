package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.model.entity.ReturnOrder;
import com.sdjzu.carrental.model.request.ReturnConfirmRequest;
import com.sdjzu.carrental.model.request.ReturnOrderRequest;
import com.sdjzu.carrental.service.ReturnOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/return-orders")
public class ReturnOrderController {

    private final ReturnOrderService returnOrderService;

    public ReturnOrderController(ReturnOrderService returnOrderService) {
        this.returnOrderService = returnOrderService;
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody ReturnOrderRequest request) {
        returnOrderService.create(request);
        return ApiResponse.success("还车申请提交成功", null);
    }

    @GetMapping
    public ApiResponse<PageResult<ReturnOrder>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String keyword) {
        return ApiResponse.success(returnOrderService.list(pageNum, pageSize, status, keyword));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id, @Valid @RequestBody ReturnConfirmRequest request) {
        returnOrderService.confirm(id, request);
        return ApiResponse.success("还车确认成功", null);
    }

    @PostMapping("/{id}/pay-extra-fee")
    public ApiResponse<Void> payExtraFee(@PathVariable Long id, @RequestBody Map<String, String> body) {
        returnOrderService.payExtraFee(id, body.get("paymentMethod"));
        return ApiResponse.success("附加费用支付成功", null);
    }
}
