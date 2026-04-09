package com.zxshop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxshop.common.Result;
import com.zxshop.dto.ProductRequest;
import com.zxshop.entity.Product;
import com.zxshop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 * 
 * @author 谭鹏
 */
@Tag(name = "商品管理", description = "商品增删改查及搜索")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * 分页查询商品列表
     */
    @Operation(summary = "分页查询商品列表")
    @GetMapping("/list")
    public Result<Page<Product>> getProductList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId) {
        
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        // 按分类查询
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        
        // 只查询上架商品
        wrapper.eq(Product::getStatus, 1)
               .orderByDesc(Product::getCreateTime);
        
        Page<Product> result = productService.page(page, wrapper);
        return Result.success(result);
    }
    
    /**
     * 根据ID查询商品详情
     */
    @Operation(summary = "查询商品详情")
    @GetMapping("/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }
    
    /**
     * 根据分类ID查询商品列表
     */
    @Operation(summary = "根据分类查询商品")
    @GetMapping("/category/{categoryId}")
    public Result<List<Product>> getProductsByCategoryId(@PathVariable Long categoryId) {
        List<Product> products = productService.getProductsByCategoryId(categoryId);
        return Result.success(products);
    }
    
    /**
     * 搜索商品
     */
    @Operation(summary = "搜索商品")
    @GetMapping("/search")
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        return Result.success(products);
    }
    
    /**
     * 创建商品
     */
    @Operation(summary = "创建商品")
    @PostMapping
    public Result<Boolean> createProduct(@Valid @RequestBody ProductRequest request) {
        Boolean result = productService.createProduct(request);
        return Result.success("创建成功", result);
    }
    
    /**
     * 更新商品
     */
    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public Result<Boolean> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        Boolean result = productService.updateProduct(id, request);
        return Result.success("更新成功", result);
    }
    
    /**
     * 删除商品
     */
    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteProduct(@PathVariable Long id) {
        Boolean result = productService.deleteProduct(id);
        return Result.success("删除成功", result);
    }
    
    /**
     * 商品上下架
     */
    @Operation(summary = "商品上下架")
    @PatchMapping("/{id}/status")
    public Result<Boolean> updateProductStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        Boolean result = productService.updateProductStatus(id, status);
        return Result.success("操作成功", result);
    }
}
