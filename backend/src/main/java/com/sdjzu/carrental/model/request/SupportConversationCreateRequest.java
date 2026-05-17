package com.sdjzu.carrental.model.request;

import lombok.Data;

@Data
public class SupportConversationCreateRequest {

    private String sourceBizType;
    private Long sourceBizId;
}
