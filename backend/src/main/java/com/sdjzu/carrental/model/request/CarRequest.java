package com.sdjzu.carrental.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CarRequest {

    @NotBlank(message = "车辆编号不能为空")
    @Pattern(regexp = "^[A-Z]{2,6}[0-9]{2,6}$", message = "车辆编号需为大写字母加数字，例如 CAR001")
    private String carNo;

    @NotNull(message = "车辆类型不能为空")
    private Long typeId;

    @NotBlank(message = "车辆品牌不能为空")
    private String brand;

    @NotBlank(message = "车辆型号不能为空")
    private String model;

    @NotBlank(message = "车牌号不能为空")
    @Pattern(
            regexp = "^([\u4e00-\u9fa5][A-Z][A-Z0-9]{5,6}|[A-Z]{3}[A-Z0-9]{5})$",
            message = "请输入合法车牌号，例如 鲁A12345 或 LUA12345"
    )
    private String plateNumber;

    @NotNull(message = "日租金不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "日租金必须大于0")
    private BigDecimal dayPrice;

    @NotNull(message = "公里数不能为空")
    @Min(value = 0, message = "公里数不能小于0")
    private Integer mileage;

    private String pickupAddress;

    @Pattern(regexp = "^(|https?://.+)$", message = "图片链接需为空或以 http/https 开头")
    private String carImage;

    private String status;
}
