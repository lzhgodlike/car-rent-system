package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sdjzu.carrental.model.dto.CarInfo;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime paymentTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private CarInfo carInfo;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userRealName;

    @TableField(exist = false)
    private String userPhone;

    @TableField(exist = false)
    private String userIdCard;

    @TableField(exist = false)
    private ReturnOrder returnOrder;

    @TableField(exist = false)
    private Boolean hasReturnRequest;

    @TableField(exist = false)
    private java.math.BigDecimal extraFee;

    @TableField(exist = false)
    private List<String> availableActions;
}
