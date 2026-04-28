package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CarTypeRequest {

    @NotBlank(message = "车辆类型名称不能为空")
    private String typeName;

    private String description;
}
