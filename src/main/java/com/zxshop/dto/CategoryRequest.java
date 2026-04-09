package com.zxshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商品分类创建/更新请求 DTO
 * 
 * @author 谭鹏
 */
@Data
@Schema(description = "分类创建/更新请求")
public class CategoryRequest {
    
    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", example = "手机数码")
    private String name;
    
    @Schema(description = "父分类ID（0为一级分类）", example = "0")
    private Long parentId = 0L;
    
    @Schema(description = "分类图标URL", example = "https://example.com/icon.png")
    private String icon;
    
    @Schema(description = "排序（数字越小越靠前）", example = "1")
    private Integer sortOrder = 0;
    
    @Schema(description = "状态（0-禁用，1-启用）", example = "1")
    private Integer status = 1;
}
