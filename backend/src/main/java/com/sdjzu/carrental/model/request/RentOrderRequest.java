package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RentOrderRequest {

    @NotNull(message = "车辆不能为空")
    private Long carId;

    @NotNull(message = "租车日期不能为空")
    @FutureOrPresent(message = "租车日期不能早于今天")
    private LocalDate rentDate;

    @NotNull(message = "预期还车日期不能为空")
    private LocalDate expectedReturnDate;

    private String remark;
}
