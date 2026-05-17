package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.model.entity.SupportConversation;
import com.sdjzu.carrental.model.entity.SupportMessage;
import com.sdjzu.carrental.model.request.SupportConversationCreateRequest;
import com.sdjzu.carrental.service.SupportConversationService;
import com.sdjzu.carrental.service.SupportMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support/conversations")
public class SupportConversationController {

    private final SupportConversationService supportConversationService;
    private final SupportMessageService supportMessageService;

    public SupportConversationController(SupportConversationService supportConversationService,
                                         SupportMessageService supportMessageService) {
        this.supportConversationService = supportConversationService;
        this.supportMessageService = supportMessageService;
    }

    @PostMapping
    public ApiResponse<SupportConversation> create(@RequestBody(required = false) SupportConversationCreateRequest request) {
        return ApiResponse.success(supportConversationService.createOrGetCurrent(request));
    }

    @GetMapping("/current")
    public ApiResponse<SupportConversation> current() {
        return ApiResponse.success(supportConversationService.currentMine());
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<SupportMessage>> messages(@PathVariable Long id) {
        return ApiResponse.success(supportMessageService.listMine(id));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<SupportConversation> read(@PathVariable Long id) {
        return ApiResponse.success("已标记为已读", supportConversationService.markReadByUser(id));
    }
}
