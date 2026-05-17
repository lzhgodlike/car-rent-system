package com.sdjzu.carrental.model.request;

import lombok.Data;

@Data
public class AdminCustomNoticeRequest {

    private Long receiverId;
    private String title;
    private String content;
}
