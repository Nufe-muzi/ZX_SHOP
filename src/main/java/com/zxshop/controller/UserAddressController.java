package com.zxshop.controller;

import com.zxshop.common.Result;
import com.zxshop.dto.AddressRequest;
import com.zxshop.entity.UserAddress;
import com.zxshop.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户地址控制器
 * 
 * @author 谭鹏
 */
@Tag(name = "收货地址管理", description = "地址增删改查及默认地址设置")
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class UserAddressController {
    
    private final UserAddressService addressService;
    
    /**
     * 添加地址
     */
    @Operation(summary = "添加地址")
    @PostMapping
    public Result<Boolean> addAddress(
            @RequestParam Long userId,
            @Valid @RequestBody AddressRequest request) {
        Boolean result = addressService.addAddress(userId, request);
        return Result.success("添加成功", result);
    }
    
    /**
     * 更新地址
     */
    @Operation(summary = "更新地址")
    @PutMapping("/{id}")
    public Result<Boolean> updateAddress(
            @RequestParam Long userId,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        Boolean result = addressService.updateAddress(userId, id, request);
        return Result.success("更新成功", result);
    }
    
    /**
     * 删除地址
     */
    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAddress(
            @RequestParam Long userId,
            @PathVariable Long id) {
        Boolean result = addressService.deleteAddress(userId, id);
        return Result.success("删除成功", result);
    }
    
    /**
     * 查询地址列表
     */
    @Operation(summary = "查询地址列表")
    @GetMapping("/list")
    public Result<List<UserAddress>> getAddressList(@RequestParam Long userId) {
        List<UserAddress> list = addressService.getAddressList(userId);
        return Result.success(list);
    }
    
    /**
     * 设置默认地址
     */
    @Operation(summary = "设置默认地址")
    @PutMapping("/{id}/default")
    public Result<Boolean> setDefaultAddress(
            @RequestParam Long userId,
            @PathVariable Long id) {
        Boolean result = addressService.setDefaultAddress(userId, id);
        return Result.success("设置成功", result);
    }
}
