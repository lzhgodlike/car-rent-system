package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.FaultReportMapper;
import com.sdjzu.carrental.mapper.MessageNoticeMapper;
import com.sdjzu.carrental.mapper.RentOrderMapper;
import com.sdjzu.carrental.mapper.ReturnOrderMapper;
import com.sdjzu.carrental.mapper.UserMapper;
import com.sdjzu.carrental.model.entity.FaultReport;
import com.sdjzu.carrental.model.entity.MessageNotice;
import com.sdjzu.carrental.model.entity.RentOrder;
import com.sdjzu.carrental.model.entity.ReturnOrder;
import com.sdjzu.carrental.model.entity.User;
import com.sdjzu.carrental.security.SecurityUtils;
import com.sdjzu.carrental.ws.NotificationWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageNoticeService {

    private final MessageNoticeMapper messageNoticeMapper;
    private final UserMapper userMapper;
    private final RentOrderMapper rentOrderMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final FaultReportMapper faultReportMapper;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    public MessageNoticeService(MessageNoticeMapper messageNoticeMapper,
                                UserMapper userMapper,
                                RentOrderMapper rentOrderMapper,
                                ReturnOrderMapper returnOrderMapper,
                                FaultReportMapper faultReportMapper,
                                NotificationWebSocketHandler notificationWebSocketHandler) {
        this.messageNoticeMapper = messageNoticeMapper;
        this.userMapper = userMapper;
        this.rentOrderMapper = rentOrderMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.faultReportMapper = faultReportMapper;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    public PageResult<MessageNotice> listMine(int pageNum, int pageSize, Boolean unreadOnly) {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<MessageNotice> wrapper = new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getReceiverId, userId)
                .eq(Boolean.TRUE.equals(unreadOnly), MessageNotice::getReadStatus, 0)
                .orderByDesc(MessageNotice::getId);
        Page<MessageNotice> page = messageNoticeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        enrichActionable(page.getRecords());
        PageResult<MessageNotice> result = PageResult.of(page);
        result.summary("unread", unreadCount());
        return result;
    }

    public long unreadCount() {
        return messageNoticeMapper.selectCount(new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getReceiverId, SecurityUtils.getUserId())
                .eq(MessageNotice::getReadStatus, 0));
    }

    @Transactional
    public void markRead(Long id) {
        MessageNotice notice = messageNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("消息不存在");
        }
        if (!notice.getReceiverId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("无权限操作该消息");
        }
        if (Integer.valueOf(1).equals(notice.getReadStatus())) {
            return;
        }
        notice.setReadStatus(1);
        notice.setReadTime(LocalDateTime.now());
        messageNoticeMapper.updateById(notice);
    }

    @Transactional
    public void markAllRead() {
        Long userId = SecurityUtils.getUserId();
        MessageNotice updateEntity = new MessageNotice();
        updateEntity.setReadStatus(1);
        updateEntity.setReadTime(LocalDateTime.now());
        messageNoticeMapper.update(updateEntity, new LambdaUpdateWrapper<MessageNotice>()
                .eq(MessageNotice::getReceiverId, userId)
                .eq(MessageNotice::getReadStatus, 0));
    }

    @Transactional
    public void deleteMine(Long id) {
        MessageNotice notice = messageNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("消息不存在");
        }
        if (!notice.getReceiverId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("无权限删除该消息");
        }
        messageNoticeMapper.deleteById(id);
    }

    @Transactional
    public void notifyAdmins(String title, String content, String messageType, String bizType, Long bizId) {
        Long senderId = SecurityUtils.getUserId();
        List<Long> adminIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "ADMIN")
                        .eq(User::getStatus, 1))
                .stream()
                .map(User::getId)
                .collect(Collectors.toList());
        for (Long adminId : adminIds) {
            createNotice(adminId, senderId, title, content, messageType, bizType, bizId);
        }
    }

    @Transactional
    public void notifyUser(Long receiverId, String title, String content, String messageType, String bizType, Long bizId) {
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null || !Integer.valueOf(1).equals(receiver.getStatus())) {
            return;
        }
        createNotice(receiverId, SecurityUtils.getUserId(), title, content, messageType, bizType, bizId);
    }

    @Transactional
    public void sendAdminCustomNotice(Long receiverId, String title, String content) {
        SecurityUtils.requireAdmin();
        if (receiverId == null) {
            throw new BusinessException("接收用户不能为空");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("消息标题不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("消息内容不能为空");
        }
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null || !Integer.valueOf(1).equals(receiver.getStatus())) {
            throw new BusinessException("目标用户不存在或已停用");
        }
        if (!"USER".equals(receiver.getRole())) {
            throw new BusinessException("只能发送给普通用户");
        }
        createNotice(receiverId, SecurityUtils.getUserId(), title.trim(), content.trim(),
                "ADMIN_CUSTOM_MESSAGE", "USER", receiverId);
    }

    private void createNotice(Long receiverId, Long senderId, String title, String content,
                              String messageType, String bizType, Long bizId) {
        MessageNotice notice = new MessageNotice();
        notice.setReceiverId(receiverId);
        notice.setSenderId(senderId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setMessageType(messageType);
        notice.setBizType(bizType);
        notice.setBizId(bizId);
        notice.setReadStatus(0);
        messageNoticeMapper.insert(notice);
        notice.setActionable(resolveActionable(notice));
        notificationWebSocketHandler.sendNotice(receiverId, notice);
    }

    private void enrichActionable(List<MessageNotice> notices) {
        if (notices == null || notices.isEmpty()) {
            return;
        }
        notices.forEach(notice -> notice.setActionable(resolveActionable(notice)));
    }

    private boolean resolveActionable(MessageNotice notice) {
        if (notice == null) {
            return false;
        }
        if ("SUPPORT_MESSAGE_REPLIED".equals(notice.getMessageType()) || "SUPPORT_MESSAGE_CREATED".equals(notice.getMessageType())) {
            return true;
        }
        if ("RENT_ORDER_CREATED".equals(notice.getMessageType()) && notice.getBizId() != null) {
            RentOrder rentOrder = rentOrderMapper.selectById(notice.getBizId());
            return rentOrder != null && RentOrderService.PENDING_PICKUP.equals(rentOrder.getOrderStatus());
        }
        if ("RETURN_REMINDER".equals(notice.getMessageType()) && notice.getBizId() != null) {
            RentOrder rentOrder = rentOrderMapper.selectById(notice.getBizId());
            return rentOrder != null && !RentOrderService.COMPLETED.equals(rentOrder.getOrderStatus());
        }
        if ("RETURN_ORDER_CREATED".equals(notice.getMessageType()) && notice.getBizId() != null) {
            ReturnOrder returnOrder = returnOrderMapper.selectById(notice.getBizId());
            return returnOrder != null && "PENDING".equals(returnOrder.getStatus());
        }
        if ("FAULT_REPORT_CREATED".equals(notice.getMessageType()) && notice.getBizId() != null) {
            FaultReport faultReport = faultReportMapper.selectById(notice.getBizId());
            return faultReport != null && "PENDING".equals(faultReport.getFaultStatus());
        }
        if ("ADMIN_CUSTOM_MESSAGE".equals(notice.getMessageType())) {
            return false;
        }
        return true;
    }
}
