package com.sdjzu.carrental.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdjzu.carrental.common.UserContext;
import com.sdjzu.carrental.model.dto.LoginUser;
import com.sdjzu.carrental.model.dto.SupportMessageDispatchResult;
import com.sdjzu.carrental.model.entity.SupportConversation;
import com.sdjzu.carrental.service.SupportMessageService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SupportChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SupportMessageService supportMessageService;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> adminSessions = ConcurrentHashMap.newKeySet();

    public SupportChatWebSocketHandler(ObjectMapper objectMapper, SupportMessageService supportMessageService) {
        this.objectMapper = objectMapper;
        this.supportMessageService = supportMessageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LoginUser loginUser = getLoginUser(session);
        session.getAttributes().put("userId", loginUser.getUserId());
        session.getAttributes().put("role", loginUser.getRole());
        if ("ADMIN".equalsIgnoreCase(loginUser.getRole())) {
            adminSessions.add(session);
            return;
        }
        userSessions.computeIfAbsent(loginUser.getUserId(), key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        LoginUser loginUser = getLoginUser(session);
        UserContext.set(loginUser);
        try {
            SupportSocketClientMessage request = objectMapper.readValue(message.getPayload(), SupportSocketClientMessage.class);
            if ("PING".equalsIgnoreCase(request.getType())) {
                send(session, SupportSocketServerMessage.of("PONG", null));
                return;
            }
            if (!"SEND_MESSAGE".equalsIgnoreCase(request.getType())) {
                send(session, SupportSocketServerMessage.error("不支持的消息类型"));
                return;
            }
            SupportMessageDispatchResult result = supportMessageService.sendMessage(request.getConversationId(), request.getContent());
            SupportConversation conversation = result.getConversation();
            SupportSocketServerMessage messageCreated = SupportSocketServerMessage.of("MESSAGE_CREATED", result.getMessage());
            SupportSocketServerMessage conversationUpdated = SupportSocketServerMessage.of("CONVERSATION_UPDATED", conversation);
            broadcastConversation(conversation, messageCreated, conversationUpdated);
        } catch (Exception ex) {
            send(session, SupportSocketServerMessage.error(ex.getMessage() == null ? "消息发送失败" : ex.getMessage()));
        } finally {
            UserContext.clear();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        removeSession(session);
        if (session.isOpen()) {
            session.close();
        }
    }

    private void broadcastConversation(SupportConversation conversation, SupportSocketServerMessage... messages) throws IOException {
        Set<WebSocketSession> targets = ConcurrentHashMap.newKeySet();
        Set<WebSocketSession> ownerSessions = userSessions.get(conversation.getUserId());
        if (ownerSessions != null) {
            targets.addAll(ownerSessions);
        }
        targets.addAll(adminSessions);
        for (WebSocketSession session : targets) {
            for (SupportSocketServerMessage message : messages) {
                send(session, message);
            }
        }
    }

    private void send(WebSocketSession session, SupportSocketServerMessage payload) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void removeSession(WebSocketSession session) {
        adminSessions.remove(session);
        Object userId = session.getAttributes().get("userId");
        if (!(userId instanceof Long uid)) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(uid);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(uid);
        }
    }

    private LoginUser getLoginUser(WebSocketSession session) {
        Object loginUser = session.getAttributes().get("loginUser");
        if (loginUser instanceof LoginUser user) {
            return user;
        }
        throw new IllegalStateException("未找到登录信息");
    }
}
