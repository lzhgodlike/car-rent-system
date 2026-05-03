package com.sdjzu.carrental.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CarInfo {
    private Long id;
    private String carNo;
    private String brand;
    private String model;
    private String plateNumber;
    private BigDecimal dayPrice;
    private Integer mileage;
    private String carImage;
    private String status;
}
