package com.zxshop.controller;

import com.zxshop.common.Result;
import com.zxshop.dto.CategoryRequest;
import com.zxshop.dto.CategoryTreeResponse;
import com.zxshop.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类控制器
 * 
 * @author 谭鹏
 */
@Tag(name = "商品分类管理", description = "分类增删改查及树形结构")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    /**
     * 获取分类树
     */
    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<CategoryTreeResponse>> getCategoryTree() {
        List<CategoryTreeResponse> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }
    
    /**
     * 创建分类
     */
    @Operation(summary = "创建分类")
    @PostMapping
    public Result<Boolean> createCategory(@Valid @RequestBody CategoryRequest request) {
        Boolean result = categoryService.createCategory(request);
        return Result.success("创建成功", result);
    }
    
    /**
     * 更新分类
     */
    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Boolean> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        Boolean result = categoryService.updateCategory(id, request);
        return Result.success("更新成功", result);
    }
    
    /**
     * 删除分类
     */
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteCategory(@PathVariable Long id) {
        Boolean result = categoryService.deleteCategory(id);
        return Result.success("删除成功", result);
    }
}
