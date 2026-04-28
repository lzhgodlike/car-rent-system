package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rent_order")
public class RentOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long carId;
    private LocalDate rentDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private Integer rentDays;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String orderStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
