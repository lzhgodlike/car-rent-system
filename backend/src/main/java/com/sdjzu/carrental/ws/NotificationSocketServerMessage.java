package com.sdjzu.carrental.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSocketServerMessage {

    private String type;
    private String message;
    private Object payload;

    public static NotificationSocketServerMessage of(String type, Object payload) {
        return new NotificationSocketServerMessage(type, null, payload);
    }

    public static NotificationSocketServerMessage error(String message) {
        return new NotificationSocketServerMessage("ERROR", message, null);
    }
}
