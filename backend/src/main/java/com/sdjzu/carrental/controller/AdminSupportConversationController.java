package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.model.entity.SupportConversation;
import com.sdjzu.carrental.model.entity.SupportMessage;
import com.sdjzu.carrental.service.SupportConversationService;
import com.sdjzu.carrental.service.SupportMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support/conversations")
public class AdminSupportConversationController {

    private final SupportConversationService supportConversationService;
    private final SupportMessageService supportMessageService;

    public AdminSupportConversationController(SupportConversationService supportConversationService,
                                              SupportMessageService supportMessageService) {
        this.supportConversationService = supportConversationService;
        this.supportMessageService = supportMessageService;
    }

    @GetMapping
    public ApiResponse<PageResult<SupportConversation>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                             @RequestParam(defaultValue = "10") int pageSize,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String keyword) {
        return ApiResponse.success(supportConversationService.listAdmin(pageNum, pageSize, status, keyword));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<SupportMessage>> messages(@PathVariable Long id) {
        return ApiResponse.success(supportMessageService.listAdmin(id));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<SupportConversation> read(@PathVariable Long id) {
        return ApiResponse.success("已标记为已读", supportConversationService.markReadByAdmin(id));
    }

    @PutMapping("/{id}/close")
    public ApiResponse<SupportConversation> close(@PathVariable Long id) {
        return ApiResponse.success("会话已关闭", supportConversationService.closeByAdmin(id));
    }
}
