package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.model.entity.MessageNotice;
import com.sdjzu.carrental.model.request.AdminCustomNoticeRequest;
import com.sdjzu.carrental.service.MessageNoticeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class MessageNoticeController {

    private final MessageNoticeService messageNoticeService;

    public MessageNoticeController(MessageNoticeService messageNoticeService) {
        this.messageNoticeService = messageNoticeService;
    }

    @GetMapping
    public ApiResponse<PageResult<MessageNotice>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(required = false) Boolean unreadOnly) {
        return ApiResponse.success(messageNoticeService.listMine(pageNum, pageSize, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.success(messageNoticeService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        messageNoticeService.markRead(id);
        return ApiResponse.success("已标记为已读", null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        messageNoticeService.markAllRead();
        return ApiResponse.success("已全部标记为已读", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        messageNoticeService.deleteMine(id);
        return ApiResponse.success("消息已删除", null);
    }

    @PostMapping("/admin/custom")
    public ApiResponse<Void> sendAdminCustomNotice(@RequestBody AdminCustomNoticeRequest request) {
        messageNoticeService.sendAdminCustomNotice(request.getReceiverId(), request.getTitle(), request.getContent());
        return ApiResponse.success("发送成功", null);
    }
}
