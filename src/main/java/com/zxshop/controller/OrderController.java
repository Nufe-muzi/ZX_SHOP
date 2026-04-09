package com.zxshop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxshop.common.Result;
import com.zxshop.dto.OrderCreateRequest;
import com.zxshop.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单控制器
 * 
 * @author 谭鹏
 */
@Tag(name = "订单管理", description = "订单创建、查询、取消等")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * 创建订单
     */
    @Operation(summary = "创建订单")
    @PostMapping
    public Result<Map<String, Object>> createOrder(
            @RequestParam Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        Map<String, Object> result = orderService.createOrder(userId, request);
        return Result.success("创建成功", result);
    }
    
    /**
     * 分页查询订单列表
     */
    @Operation(summary = "查询订单列表")
    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> getOrderPage(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Page<Map<String, Object>> page = orderService.getOrderPage(userId, pageNum, pageSize, status);
        return Result.success(page);
    }
    
    /**
     * 查询订单详情
     */
    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getOrderDetail(
            @RequestParam Long userId,
            @PathVariable Long id) {
        Map<String, Object> detail = orderService.getOrderDetail(userId, id);
        return Result.success(detail);
    }
    
    /**
     * 取消订单
     */
    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancelOrder(
            @RequestParam Long userId,
            @PathVariable Long id) {
        Boolean result = orderService.cancelOrder(userId, id);
        return Result.success("取消成功", result);
    }
    
    /**
     * 删除订单
     */
    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteOrder(
            @RequestParam Long userId,
            @PathVariable Long id) {
        Boolean result = orderService.deleteOrder(userId, id);
        return Result.success("删除成功", result);
    }
    
    /**
     * 确认收货
     */
    @Operation(summary = "确认收货")
    @PostMapping("/{id}/confirm")
    public Result<Boolean> confirmReceipt(
            @RequestParam Long userId,
            @PathVariable Long id) {
        Boolean result = orderService.confirmReceipt(userId, id);
        return Result.success("确认成功", result);
    }
}
