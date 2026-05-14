package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("car_image")
public class CarImage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long carId;
    private String imageUrl;
    private Integer sortOrder;
    private String sourceType;
    private String originUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
