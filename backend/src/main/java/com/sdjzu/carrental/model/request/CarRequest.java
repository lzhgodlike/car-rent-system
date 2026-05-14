package com.sdjzu.carrental.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CarRequest {

    @NotNull(message = "车辆类型不能为空")
    private Long typeId;

    @NotBlank(message = "车辆品牌不能为空")
    private String brand;

    @NotBlank(message = "车辆型号不能为空")
    private String model;

    @NotBlank(message = "车牌号不能为空")
    @Pattern(
            regexp = "^([一-龥][A-Z][A-Z0-9]{5,6}|[A-Z]{3}[A-Z0-9]{5})$",
            message = "请输入合法车牌号，例如 鲁A12345 或 LUA12345"
    )
    private String plateNumber;

    @NotNull(message = "日租金不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "日租金必须大于0")
    private BigDecimal dayPrice;

    @NotNull(message = "公里数不能为空")
    @Min(value = 0, message = "公里数不能小于0")
    private Integer mileage;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    @NotEmpty(message = "请至少添加一张车辆图片")
    @Valid
    private List<CarImageItemRequest> images;
}
