package com.zxshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加商品到购物车请求 DTO
 * 
 * @author 谭鹏
 */
@Data
@Schema(description = "添加商品到购物车请求")
public class CartAddRequest {
    
    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1")
    private Long productId;
    
    @Schema(description = "SKU ID", example = "1")
    private Long skuId;
    
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    @Schema(description = "数量", example = "1")
    private Integer quantity = 1;
}
