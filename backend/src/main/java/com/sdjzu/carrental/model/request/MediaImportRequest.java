package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MediaImportRequest {

    @NotBlank(message = "图片链接不能为空")
    @Pattern(regexp = "^https?://.+$", message = "图片链接必须以 http 或 https 开头")
    private String url;
}
