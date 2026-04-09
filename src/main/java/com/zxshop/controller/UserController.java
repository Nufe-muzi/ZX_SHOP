package com.zxshop.controller;

import com.zxshop.common.Result;
import com.zxshop.dto.LoginRequest;
import com.zxshop.dto.LoginResponse;
import com.zxshop.dto.RegisterRequest;
import com.zxshop.entity.User;
import com.zxshop.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 * 
 * @author 谭鹏
 */
@Tag(name = "用户管理", description = "用户注册、登录相关接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 用户注册
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterRequest request) {
        Boolean result = userService.register(request);
        return Result.success("注册成功", result);
    }
    
    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success("登录成功", response);
    }
    
    /**
     * 获取所有用户列表（测试用）
     */
    @Operation(summary = "获取用户列表", deprecated = true)
    @GetMapping("/list")
    public Result<List<User>> list() {
        List<User> users = userService.list();
        return Result.success(users);
    }
}
