package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("car_info")
public class Car {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String carNo;
    private Long typeId;
    private String brand;
    private String model;
    private String plateNumber;
    private BigDecimal dayPrice;
    private Integer mileage;
    private String pickupAddress;
    private String carImage;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
