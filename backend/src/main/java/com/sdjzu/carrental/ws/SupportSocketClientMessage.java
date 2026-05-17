package com.sdjzu.carrental.ws;

import lombok.Data;

@Data
public class SupportSocketClientMessage {

    private String type;
    private Long conversationId;
    private String content;
}
