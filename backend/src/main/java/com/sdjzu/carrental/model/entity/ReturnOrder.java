package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("return_order")
public class ReturnOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long rentOrderId;
    private LocalDateTime actualReturnTime;
    private Integer actualMileage;
    private String damageDesc;
    private BigDecimal extraFee;
    private String status;
    private Long operatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
