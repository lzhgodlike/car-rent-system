package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnOrderRequest {

    @NotNull(message = "租车订单不能为空")
    private Long rentOrderId;

    @NotNull(message = "当前公里数不能为空")
    private Integer actualMileage;

    private String damageDesc;
}
