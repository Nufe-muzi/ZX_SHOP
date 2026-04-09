package com.zxshop.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类树形响应 DTO
 * 
 * @author 谭鹏
 */
@Data
@Schema(description = "分类树形响应")
public class CategoryTreeResponse {
    
    @Schema(description = "分类ID")
    private Long id;
    
    @Schema(description = "分类名称")
    private String name;
    
    @Schema(description = "父分类ID")
    private Long parentId;
    
    @Schema(description = "分类层级")
    private Integer level;
    
    @Schema(description = "分类图标URL")
    private String icon;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "子分类列表")
    private List<CategoryTreeResponse> children;
}
