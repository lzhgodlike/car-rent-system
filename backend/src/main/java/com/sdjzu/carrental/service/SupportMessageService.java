package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.SupportConversationMapper;
import com.sdjzu.carrental.mapper.SupportMessageMapper;
import com.sdjzu.carrental.mapper.UserMapper;
import com.sdjzu.carrental.model.dto.SupportMessageDispatchResult;
import com.sdjzu.carrental.model.entity.SupportConversation;
import com.sdjzu.carrental.model.entity.SupportMessage;
import com.sdjzu.carrental.model.entity.User;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SupportMessageService {

    private final SupportMessageMapper supportMessageMapper;
    private final SupportConversationMapper supportConversationMapper;
    private final SupportConversationService supportConversationService;
    private final UserMapper userMapper;
    private final MessageNoticeService messageNoticeService;

    public SupportMessageService(SupportMessageMapper supportMessageMapper,
                                 SupportConversationMapper supportConversationMapper,
                                 SupportConversationService supportConversationService,
                                 UserMapper userMapper,
                                 MessageNoticeService messageNoticeService) {
        this.supportMessageMapper = supportMessageMapper;
        this.supportConversationMapper = supportConversationMapper;
        this.supportConversationService = supportConversationService;
        this.userMapper = userMapper;
        this.messageNoticeService = messageNoticeService;
    }

    public List<SupportMessage> listMine(Long conversationId) {
        SupportConversation conversation = supportConversationService.requireOwnedConversation(conversationId);
        List<SupportMessage> messages = supportMessageMapper.selectList(new LambdaQueryWrapper<SupportMessage>()
                .eq(SupportMessage::getConversationId, conversation.getId())
                .orderByAsc(SupportMessage::getId));
        enrichMessages(messages);
        return messages;
    }

    public List<SupportMessage> listAdmin(Long conversationId) {
        SupportConversation conversation = supportConversationService.requireAdminAccessibleConversation(conversationId);
        List<SupportMessage> messages = supportMessageMapper.selectList(new LambdaQueryWrapper<SupportMessage>()
                .eq(SupportMessage::getConversationId, conversation.getId())
                .orderByAsc(SupportMessage::getId));
        enrichMessages(messages);
        return messages;
    }

    @Transactional
    public SupportMessageDispatchResult sendMessage(Long conversationId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("消息内容不能为空");
        }
        String normalizedContent = content.trim();
        if (normalizedContent.length() > 1000) {
            throw new BusinessException("消息内容不能超过1000个字符");
        }
        SupportConversation conversation = supportConversationService.requireConversation(conversationId);
        if (SupportConversationService.STATUS_CLOSED.equals(conversation.getStatus())) {
            throw new BusinessException("当前会话已关闭");
        }

        Long senderId = SecurityUtils.getUserId();
        boolean admin = SecurityUtils.isAdmin();
        if (!admin && !conversation.getUserId().equals(senderId)) {
            throw new BusinessException("无权限发送该消息");
        }

        SupportMessage message = new SupportMessage();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setSenderRole(admin ? "ADMIN" : "USER");
        message.setContent(normalizedContent);
        message.setReadStatus(0);
        supportMessageMapper.insert(message);

        conversation.setLastMessagePreview(buildPreview(normalizedContent));
        conversation.setLastMessageTime(LocalDateTime.now());
        if (admin) {
            conversation.setUserUnreadCount(defaultInt(conversation.getUserUnreadCount()) + 1);
            conversation.setAssignedAdminId(conversation.getAssignedAdminId() == null ? senderId : conversation.getAssignedAdminId());
            notifyUserReply(conversation, normalizedContent);
        } else {
            conversation.setAdminUnreadCount(defaultInt(conversation.getAdminUnreadCount()) + 1);
            notifyAdminNewMessage(conversation, normalizedContent);
        }
        supportConversationMapper.updateById(conversation);

        SupportMessage created = supportMessageMapper.selectById(message.getId());
        SupportConversation updatedConversation = supportConversationMapper.selectById(conversationId);
        enrichMessages(List.of(created));
        supportConversationService.enrichConversations(List.of(updatedConversation));
        return new SupportMessageDispatchResult(created, updatedConversation);
    }

    public void enrichMessages(List<SupportMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        Set<Long> senderIds = messages.stream()
                .map(SupportMessage::getSenderId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = senderIds.isEmpty() ? Collections.emptyMap() : userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        for (SupportMessage message : messages) {
            User user = userMap.get(message.getSenderId());
            if (user != null) {
                message.setSenderName(user.getUsername());
                message.setSenderRealName(user.getRealName());
            }
        }
    }

    private void notifyAdminNewMessage(SupportConversation conversation, String content) {
        String name = resolveUserDisplayName(conversation.getUserId());
        messageNoticeService.notifyAdmins(
                "新的客服消息",
                name + " 发来新的客服消息：" + buildPreview(content),
                "SUPPORT_MESSAGE_CREATED",
                "SUPPORT_CONVERSATION",
                conversation.getId()
        );
    }

    private void notifyUserReply(SupportConversation conversation, String content) {
        messageNoticeService.notifyUser(
                conversation.getUserId(),
                "客服已回复",
                "管理员回复了您的消息：" + buildPreview(content),
                "SUPPORT_MESSAGE_REPLIED",
                "SUPPORT_CONVERSATION",
                conversation.getId()
        );
    }

    private String resolveUserDisplayName(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "用户";
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "用户";
    }

    private String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.trim();
        return normalized.length() <= 60 ? normalized : normalized.substring(0, 60) + "...";
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
