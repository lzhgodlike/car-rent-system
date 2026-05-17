package com.sdjzu.carrental.config;

import com.sdjzu.carrental.security.WebSocketAuthHandshakeInterceptor;
import com.sdjzu.carrental.ws.NotificationWebSocketHandler;
import com.sdjzu.carrental.ws.SupportChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SupportChatWebSocketHandler supportChatWebSocketHandler;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor;

    public WebSocketConfig(SupportChatWebSocketHandler supportChatWebSocketHandler,
                           NotificationWebSocketHandler notificationWebSocketHandler,
                           WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor) {
        this.supportChatWebSocketHandler = supportChatWebSocketHandler;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.webSocketAuthHandshakeInterceptor = webSocketAuthHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(supportChatWebSocketHandler, "/ws/support-chat")
                .addInterceptors(webSocketAuthHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(webSocketAuthHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
