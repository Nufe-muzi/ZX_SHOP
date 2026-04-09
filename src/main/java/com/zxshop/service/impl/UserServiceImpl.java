package com.zxshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxshop.common.BusinessException;
import com.zxshop.common.JwtUtil;
import com.zxshop.common.PasswordEncoderUtil;
import com.zxshop.dto.LoginRequest;
import com.zxshop.dto.LoginResponse;
import com.zxshop.dto.RegisterRequest;
import com.zxshop.entity.User;
import com.zxshop.mapper.UserMapper;
import com.zxshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现类
 * 
 * @author 谭鹏
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final PasswordEncoderUtil passwordEncoderUtil;
    private final JwtUtil jwtUtil;
    
    @Override
    public Boolean register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        Long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        
        // 2. 检查手机号是否已存在
        if (StringUtils.hasText(request.getPhone())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, request.getPhone());
            count = this.count(wrapper);
            if (count > 0) {
                throw new BusinessException("手机号已被注册");
            }
        }
        
        // 3. 检查邮箱是否已存在
        if (StringUtils.hasText(request.getEmail())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, request.getEmail());
            count = this.count(wrapper);
            if (count > 0) {
                throw new BusinessException("邮箱已被注册");
            }
        }
        
        // 4. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoderUtil.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setStatus(1); // 正常状态
        
        return this.save(user);
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = this.getOne(wrapper);
        
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 2. 验证密码
        if (!passwordEncoderUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        
        // 4. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        // 5. 返回登录响应
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }
}
