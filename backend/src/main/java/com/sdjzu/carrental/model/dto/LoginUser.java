package com.sdjzu.carrental.model.dto;

import lombok.Data;

@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String role;
}
