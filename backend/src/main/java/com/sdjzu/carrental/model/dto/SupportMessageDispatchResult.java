package com.sdjzu.carrental.model.dto;

import com.sdjzu.carrental.model.entity.SupportConversation;
import com.sdjzu.carrental.model.entity.SupportMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SupportMessageDispatchResult {

    private SupportMessage message;
    private SupportConversation conversation;
}
