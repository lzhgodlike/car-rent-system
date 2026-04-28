package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReturnConfirmRequest {

    @NotNull(message = "附加费用不能为空")
    private BigDecimal extraFee;
}
