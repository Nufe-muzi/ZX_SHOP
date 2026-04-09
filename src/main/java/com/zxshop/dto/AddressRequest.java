package com.zxshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 地址创建/更新请求 DTO
 * 
 * @author 谭鹏
 */
@Data
@Schema(description = "地址创建/更新请求")
public class AddressRequest {
    
    @NotBlank(message = "收货人姓名不能为空")
    @Schema(description = "收货人姓名", example = "张三")
    private String receiverName;
    
    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "收货人电话", example = "13800138000")
    private String receiverPhone;
    
    @NotBlank(message = "省份不能为空")
    @Schema(description = "省份", example = "北京市")
    private String province;
    
    @NotBlank(message = "城市不能为空")
    @Schema(description = "城市", example = "北京市")
    private String city;
    
    @NotBlank(message = "区县不能为空")
    @Schema(description = "区县", example = "朝阳区")
    private String district;
    
    @NotBlank(message = "详细地址不能为空")
    @Schema(description = "详细地址", example = "朝阳路 123 号")
    private String detailAddress;
    
    @Schema(description = "邮编", example = "100000")
    private String postalCode;
    
    @Schema(description = "是否默认地址（0-否，1-是）", example = "1")
    private Integer isDefault = 0;
}
