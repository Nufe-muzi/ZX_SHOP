package com.zxshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求 DTO
 * 
 * @author 谭鹏
 */
@Data
@Schema(description = "创建订单请求")
public class OrderCreateRequest {
    
    @NotNull(message = "收货地址 ID 不能为空")
    @Schema(description = "收货地址 ID", example = "1")
    private Long addressId;
    
    @Schema(description = "购物车项 ID 列表（不传则结算所有选中商品）", example = "[1, 2, 3]")
    private List<Long> cartIds;
    
    @Schema(description = "订单备注", example = "请尽快发货")
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
