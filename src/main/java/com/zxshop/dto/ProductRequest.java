package com.zxshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品创建/更新请求 DTO
 * 
 * @author 谭鹏
 */
@Data
@Schema(description = "商品创建/更新请求")
public class ProductRequest {
    
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200")
    @Schema(description = "商品名称", example = "iPhone 15 Pro Max")
    private String name;
    
    @Schema(description = "商品副标题", example = "苹果最新旗舰手机")
    private String subtitle;
    
    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID", example = "4")
    private Long categoryId;
    
    @Schema(description = "主图URL", example = "https://example.com/main.jpg")
    private String mainImage;
    
    @Schema(description = "副图URL列表（JSON数组）", example = "[\"img1.jpg\",\"img2.jpg\"]")
    private String subImages;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    @Schema(description = "价格（元）", example = "9999.00")
    private BigDecimal price;
    
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    @Schema(description = "库存数量", example = "100")
    private Integer stock;
    
    @Schema(description = "商品详情（HTML）", example = "<p>商品详情</p>")
    private String detail;
    
    @Schema(description = "状态（0-下架，1-上架）", example = "1")
    private Integer status = 1;
}
