package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("car_type")
public class CarType {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeName;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
