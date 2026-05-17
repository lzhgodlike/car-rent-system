package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.mapper.SupportConversationMapper;
import com.sdjzu.carrental.mapper.UserMapper;
import com.sdjzu.carrental.model.entity.SupportConversation;
import com.sdjzu.carrental.model.entity.User;
import com.sdjzu.carrental.model.request.SupportConversationCreateRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SupportConversationService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    private final SupportConversationMapper supportConversationMapper;
    private final UserMapper userMapper;

    public SupportConversationService(SupportConversationMapper supportConversationMapper, UserMapper userMapper) {
        this.supportConversationMapper = supportConversationMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public SupportConversation createOrGetCurrent(SupportConversationCreateRequest request) {
        Long userId = SecurityUtils.getUserId();
        SupportConversation exists = supportConversationMapper.selectOne(new LambdaQueryWrapper<SupportConversation>()
                .eq(SupportConversation::getUserId, userId)
                .eq(SupportConversation::getStatus, STATUS_OPEN)
                .orderByDesc(SupportConversation::getId)
                .last("limit 1"));
        if (exists != null) {
            enrichConversations(List.of(exists));
            return exists;
        }
        SupportConversation conversation = new SupportConversation();
        conversation.setUserId(userId);
        conversation.setStatus(STATUS_OPEN);
        conversation.setSourceBizType(normalizeText(request == null ? null : request.getSourceBizType()));
        conversation.setSourceBizId(request == null ? null : request.getSourceBizId());
        conversation.setUserUnreadCount(0);
        conversation.setAdminUnreadCount(0);
        supportConversationMapper.insert(conversation);
        SupportConversation created = supportConversationMapper.selectById(conversation.getId());
        enrichConversations(List.of(created));
        return created;
    }

    public SupportConversation currentMine() {
        SupportConversation conversation = supportConversationMapper.selectOne(new LambdaQueryWrapper<SupportConversation>()
                .eq(SupportConversation::getUserId, SecurityUtils.getUserId())
                .eq(SupportConversation::getStatus, STATUS_OPEN)
                .orderByDesc(SupportConversation::getId)
                .last("limit 1"));
        if (conversation == null) {
            return null;
        }
        enrichConversations(List.of(conversation));
        return conversation;
    }

    public PageResult<SupportConversation> listAdmin(int pageNum, int pageSize, String status, String keyword) {
        SecurityUtils.requireAdmin();
        LambdaQueryWrapper<SupportConversation> wrapper = new LambdaQueryWrapper<SupportConversation>()
                .eq(StringUtils.hasText(status), SupportConversation::getStatus, status)
                .orderByDesc(SupportConversation::getLastMessageTime)
                .orderByDesc(SupportConversation::getId);
        Page<SupportConversation> page = supportConversationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        enrichConversations(page.getRecords());
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            List<SupportConversation> filtered = page.getRecords().stream()
                    .filter(item -> matchKeyword(item, trimmed))
                    .collect(Collectors.toList());
            PageResult<SupportConversation> result = new PageResult<>(filtered, filtered.size(), pageNum, pageSize);
            result.summary("open", supportConversationMapper.selectCount(new LambdaQueryWrapper<SupportConversation>()
                    .eq(SupportConversation::getStatus, STATUS_OPEN)));
            result.summary("closed", supportConversationMapper.selectCount(new LambdaQueryWrapper<SupportConversation>()
                    .eq(SupportConversation::getStatus, STATUS_CLOSED)));
            return result;
        }
        PageResult<SupportConversation> result = PageResult.of(page);
        result.summary("open", supportConversationMapper.selectCount(new LambdaQueryWrapper<SupportConversation>()
                .eq(SupportConversation::getStatus, STATUS_OPEN)));
        result.summary("closed", supportConversationMapper.selectCount(new LambdaQueryWrapper<SupportConversation>()
                .eq(SupportConversation::getStatus, STATUS_CLOSED)));
        return result;
    }

    @Transactional
    public SupportConversation closeByAdmin(Long conversationId) {
        SecurityUtils.requireAdmin();
        SupportConversation conversation = requireConversation(conversationId);
        if (STATUS_CLOSED.equals(conversation.getStatus())) {
            enrichConversations(List.of(conversation));
            return conversation;
        }
        conversation.setStatus(STATUS_CLOSED);
        supportConversationMapper.updateById(conversation);
        SupportConversation updated = supportConversationMapper.selectById(conversationId);
        enrichConversations(List.of(updated));
        return updated;
    }

    @Transactional
    public SupportConversation markReadByUser(Long conversationId) {
        SupportConversation conversation = requireOwnedConversation(conversationId);
        conversation.setUserUnreadCount(0);
        supportConversationMapper.updateById(conversation);
        SupportConversation updated = supportConversationMapper.selectById(conversationId);
        enrichConversations(List.of(updated));
        return updated;
    }

    @Transactional
    public SupportConversation markReadByAdmin(Long conversationId) {
        SecurityUtils.requireAdmin();
        SupportConversation conversation = requireConversation(conversationId);
        conversation.setAdminUnreadCount(0);
        if (conversation.getAssignedAdminId() == null) {
            conversation.setAssignedAdminId(SecurityUtils.getUserId());
        }
        supportConversationMapper.updateById(conversation);
        SupportConversation updated = supportConversationMapper.selectById(conversationId);
        enrichConversations(List.of(updated));
        return updated;
    }

    public SupportConversation requireOwnedConversation(Long conversationId) {
        SupportConversation conversation = requireConversation(conversationId);
        if (!conversation.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("无权限访问该会话");
        }
        return conversation;
    }

    public SupportConversation requireAdminAccessibleConversation(Long conversationId) {
        SecurityUtils.requireAdmin();
        return requireConversation(conversationId);
    }

    public SupportConversation requireConversation(Long conversationId) {
        SupportConversation conversation = supportConversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("客服会话不存在");
        }
        return conversation;
    }

    public void enrichConversations(List<SupportConversation> conversations) {
        if (conversations == null || conversations.isEmpty()) {
            return;
        }
        Set<Long> userIds = conversations.stream()
                .map(SupportConversation::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> adminIds = conversations.stream()
                .map(SupportConversation::getAssignedAdminId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> allIds = userIds.stream().collect(Collectors.toSet());
        allIds.addAll(adminIds);
        Map<Long, User> userMap = allIds.isEmpty() ? Collections.emptyMap() : userMapper.selectBatchIds(allIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        for (SupportConversation conversation : conversations) {
            User user = userMap.get(conversation.getUserId());
            if (user != null) {
                conversation.setUserName(user.getUsername());
                conversation.setUserRealName(user.getRealName());
            }
            User admin = userMap.get(conversation.getAssignedAdminId());
            if (admin != null) {
                conversation.setAssignedAdminName(StringUtils.hasText(admin.getRealName()) ? admin.getRealName() : admin.getUsername());
            }
        }
    }

    private boolean matchKeyword(SupportConversation conversation, String keyword) {
        return contains(conversation.getUserName(), keyword)
                || contains(conversation.getUserRealName(), keyword)
                || contains(conversation.getLastMessagePreview(), keyword)
                || contains(conversation.getAssignedAdminName(), keyword)
                || contains(conversation.getStatus(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.contains(keyword);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
