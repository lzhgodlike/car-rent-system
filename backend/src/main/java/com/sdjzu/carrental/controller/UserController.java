package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.common.PageResult;
import com.sdjzu.carrental.model.entity.User;
import com.sdjzu.carrental.model.request.UserManageRequest;
import com.sdjzu.carrental.model.request.UserProfileRequest;
import com.sdjzu.carrental.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ApiResponse<User> profile() {
        return ApiResponse.success(userService.profile());
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody UserProfileRequest request) {
        userService.updateProfile(request);
        return ApiResponse.success("修改成功", null);
    }

    @GetMapping
    public ApiResponse<PageResult<User>> listUsers(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(userService.listUsers(pageNum, pageSize));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @RequestBody UserManageRequest request) {
        userService.updateUser(id, request);
        return ApiResponse.success("修改成功", null);
    }
}
