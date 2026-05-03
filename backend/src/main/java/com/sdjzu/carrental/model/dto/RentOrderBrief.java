package com.sdjzu.carrental.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RentOrderBrief {
    private Long id;
    private String orderNo;
    private Long carId;
    private LocalDate rentDate;
    private LocalDate expectedReturnDate;
    private Integer rentDays;
    private BigDecimal totalPrice;
}
