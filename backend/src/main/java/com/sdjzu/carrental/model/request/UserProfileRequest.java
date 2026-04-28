package com.sdjzu.carrental.model.request;

import lombok.Data;

@Data
public class UserProfileRequest {

    private String realName;
    private String phone;
    private String idCard;
    private String gender;
    private String password;
}
