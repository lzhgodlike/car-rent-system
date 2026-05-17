package com.sdjzu.carrental.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("support_conversation")
public class SupportConversation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String status;
    private Long assignedAdminId;
    private String sourceBizType;
    private Long sourceBizId;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
    private Integer userUnreadCount;
    private Integer adminUnreadCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userRealName;

    @TableField(exist = false)
    private String assignedAdminName;
}
