package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private String province;
    private String city;
    private String detailAddress;
    private String pickupAddress;
    private String carImage;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer rentCount;

    @TableField(exist = false)
    private BigDecimal totalIncome;

    @TableField(exist = false)
    private String currentRenterName;

    @TableField(exist = false)
    private List<CarImage> carImages;
}
