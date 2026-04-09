package com.zxshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项 VO（带商品信息）
 * 
 * @author 谭鹏
 */
@Data
@Schema(description = "购物车项")
public class CartItemVO {
    
    @Schema(description = "购物车ID")
    private Long cartId;
    
    @Schema(description = "商品ID")
    private Long productId;
    
    @Schema(description = "商品名称")
    private String productName;
    
    @Schema(description = "商品副标题")
    private String productSubtitle;
    
    @Schema(description = "商品主图")
    private String productImage;
    
    @Schema(description = "商品价格")
    private BigDecimal productPrice;
    
    @Schema(description = "SKU ID")
    private Long skuId;
    
    @Schema(description = "购买数量")
    private Integer quantity;
    
    @Schema(description = "是否选中")
    private Integer checked;
    
    @Schema(description = "小计（价格*数量）")
    private BigDecimal subtotal;
}
