package com.sdjzu.carrental.controller;

import com.sdjzu.carrental.common.ApiResponse;
import com.sdjzu.carrental.model.request.LoginRequest;
import com.sdjzu.carrental.model.request.RegisterRequest;
import com.sdjzu.carrental.model.vo.TokenVO;
import com.sdjzu.carrental.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("注册成功", null);
    }

    @PostMapping("/login")
    public ApiResponse<TokenVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<Object> me() {
        return ApiResponse.success(authService.me());
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenVO> refresh() {
        return ApiResponse.success(authService.refresh());
    }
}
