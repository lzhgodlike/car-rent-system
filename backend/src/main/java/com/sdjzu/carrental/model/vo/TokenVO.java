package com.sdjzu.carrental.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenVO {

    private String token;
    private Object userInfo;
}
