package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CarImageItemRequest {

    private Long id;

    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    @NotNull(message = "图片排序不能为空")
    private Integer sortOrder;
}
