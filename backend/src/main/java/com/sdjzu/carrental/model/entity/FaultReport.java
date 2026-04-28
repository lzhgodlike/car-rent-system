package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fault_report")
public class FaultReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long carId;
    private String faultContent;
    private String faultStatus;
    private String handleResult;
    private LocalDateTime reportTime;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
