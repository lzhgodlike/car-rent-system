package com.sdjzu.carrental.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportSocketServerMessage {

    private String type;
    private String message;
    private Object payload;

    public static SupportSocketServerMessage of(String type, Object payload) {
        return new SupportSocketServerMessage(type, null, payload);
    }

    public static SupportSocketServerMessage error(String message) {
        return new SupportSocketServerMessage("ERROR", message, null);
    }
}
