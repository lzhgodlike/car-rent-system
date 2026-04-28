package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaultHandleRequest {

    @NotBlank(message = "处理结果不能为空")
    private String handleResult;
}
