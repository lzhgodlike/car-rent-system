package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaultReportRequest {

    @NotNull(message = "车辆不能为空")
    private Long carId;

    @NotBlank(message = "故障内容不能为空")
    private String faultContent;
}
