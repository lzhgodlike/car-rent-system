package com.sdjzu.carrental.model.request;

import lombok.Data;

@Data
public class UserManageRequest {

    private String username;
    private String realName;
    private String phone;
    private String idCard;
    private String gender;
    private String role;
    private Integer status;
    private String password;
}
