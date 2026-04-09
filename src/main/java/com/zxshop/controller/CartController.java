package com.zxshop.controller;

import com.zxshop.common.Result;
import com.zxshop.dto.CartAddRequest;
import com.zxshop.dto.CartItemVO;
import com.zxshop.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 * 
 * @author 谭鹏
 */
@Tag(name = "购物车管理", description = "购物车增删改查")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    
    /**
     * 添加商品到购物车
     */
    @Operation(summary = "添加商品到购物车")
    @PostMapping
    public Result<Boolean> addToCart(
            @RequestParam Long userId,
            @Valid @RequestBody CartAddRequest request) {
        Boolean result = cartService.addToCart(userId, request);
        return Result.success("添加成功", result);
    }
    
    /**
     * 查询购物车列表
     */
    @Operation(summary = "查询购物车列表")
    @GetMapping("/list")
    public Result<List<CartItemVO>> getCartList(@RequestParam Long userId) {
        List<CartItemVO> list = cartService.getCartList(userId);
        return Result.success(list);
    }
    
    /**
     * 更新购物车商品数量
     */
    @Operation(summary = "更新购物车商品数量")
    @PutMapping("/{cartId}/quantity")
    public Result<Boolean> updateQuantity(
            @RequestParam Long userId,
            @PathVariable Long cartId,
            @RequestParam Integer quantity) {
        Boolean result = cartService.updateQuantity(userId, cartId, quantity);
        return Result.success("更新成功", result);
    }
    
    /**
     * 删除购物车商品
     */
    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/{cartId}")
    public Result<Boolean> removeFromCart(
            @RequestParam Long userId,
            @PathVariable Long cartId) {
        Boolean result = cartService.removeFromCart(userId, cartId);
        return Result.success("删除成功", result);
    }
    
    /**
     * 选中/取消选中商品
     */
    @Operation(summary = "选中/取消选中商品")
    @PutMapping("/{cartId}/checked")
    public Result<Boolean> updateChecked(
            @RequestParam Long userId,
            @PathVariable Long cartId,
            @RequestParam Integer checked) {
        Boolean result = cartService.updateChecked(userId, cartId, checked);
        return Result.success("操作成功", result);
    }
    
    /**
     * 全选/取消全选
     */
    @Operation(summary = "全选/取消全选")
    @PutMapping("/all-checked")
    public Result<Boolean> updateAllChecked(
            @RequestParam Long userId,
            @RequestParam Integer checked) {
        Boolean result = cartService.updateAllChecked(userId, checked);
        return Result.success("操作成功", result);
    }
    
    /**
     * 计算购物车总价
     */
    @Operation(summary = "计算购物车总价")
    @GetMapping("/total")
    public Result<Map<String, Object>> calculateTotal(@RequestParam Long userId) {
        Map<String, Object> result = cartService.calculateTotal(userId);
        return Result.success(result);
    }
}
