package com.zxshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zxshop.dto.LoginRequest;
import com.zxshop.dto.LoginResponse;
import com.zxshop.dto.RegisterRequest;
import com.zxshop.entity.User;

/**
 * 用户服务接口
 * 
 * @author 谭鹏
 */
public interface UserService extends IService<User> {
    
    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 是否成功
     */
    Boolean register(RegisterRequest request);
    
    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录响应（包含 Token）
     */
    LoginResponse login(LoginRequest request);
}
