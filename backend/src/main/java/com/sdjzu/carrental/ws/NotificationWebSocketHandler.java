package com.sdjzu.carrental.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdjzu.carrental.model.dto.LoginUser;
import com.sdjzu.carrental.model.entity.MessageNotice;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LoginUser loginUser = getLoginUser(session);
        session.getAttributes().put("userId", loginUser.getUserId());
        userSessions.computeIfAbsent(loginUser.getUserId(), key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            NotificationSocketClientMessage request = objectMapper.readValue(message.getPayload(), NotificationSocketClientMessage.class);
            if ("PING".equalsIgnoreCase(request.getType())) {
                send(session, NotificationSocketServerMessage.of("PONG", null));
                return;
            }
            send(session, NotificationSocketServerMessage.error("不支持的消息类型"));
        } catch (Exception ex) {
            send(session, NotificationSocketServerMessage.error(ex.getMessage() == null ? "通知连接异常" : ex.getMessage()));
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

    public void sendNotice(Long receiverId, MessageNotice notice) {
        Set<WebSocketSession> sessions = userSessions.get(receiverId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        NotificationSocketServerMessage payload = NotificationSocketServerMessage.of("NOTICE_CREATED", notice);
        for (WebSocketSession session : sessions) {
            try {
                send(session, payload);
            } catch (IOException ex) {
                removeSession(session);
                if (session.isOpen()) {
                    try {
                        session.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private void send(WebSocketSession session, NotificationSocketServerMessage payload) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void removeSession(WebSocketSession session) {
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
