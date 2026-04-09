package com.zxshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxshop.common.BusinessException;
import com.zxshop.dto.AddressRequest;
import com.zxshop.entity.UserAddress;
import com.zxshop.mapper.UserAddressMapper;
import com.zxshop.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户地址服务实现类
 * 
 * @author 谭鹏
 */
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {
    
    @Override
    @Transactional
    public Boolean addAddress(Long userId, AddressRequest request) {
        // 1. 创建地址
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(request, address);
        address.setUserId(userId);
        
        // 2. 如果设置为默认地址，取消其他地址的默认状态
        if (request.getIsDefault() == 1) {
            LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(UserAddress::getUserId, userId)
                   .set(UserAddress::getIsDefault, 0);
            this.update(wrapper);
        }
        
        return this.save(address);
    }
    
    @Override
    @Transactional
    public Boolean updateAddress(Long userId, Long addressId, AddressRequest request) {
        // 1. 查询地址是否存在且属于该用户
        UserAddress address = this.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        
        // 2. 更新地址信息
        BeanUtils.copyProperties(request, address);
        
        // 3. 如果设置为默认地址，取消其他地址的默认状态
        if (request.getIsDefault() == 1) {
            LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(UserAddress::getUserId, userId)
                   .ne(UserAddress::getId, addressId)
                   .set(UserAddress::getIsDefault, 0);
            this.update(wrapper);
        }
        
        return this.updateById(address);
    }
    
    @Override
    @Transactional
    public Boolean deleteAddress(Long userId, Long addressId) {
        // 1. 查询地址是否存在且属于该用户
        UserAddress address = this.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        
        // 2. 逻辑删除
        return this.removeById(addressId);
    }
    
    @Override
    public List<UserAddress> getAddressList(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
               .orderByDesc(UserAddress::getIsDefault)
               .orderByDesc(UserAddress::getCreateTime);
        return this.list(wrapper);
    }
    
    @Override
    @Transactional
    public Boolean setDefaultAddress(Long userId, Long addressId) {
        // 1. 查询地址是否存在且属于该用户
        UserAddress address = this.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        
        // 2. 取消该用户所有地址的默认状态
        LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
               .set(UserAddress::getIsDefault, 0);
        this.update(wrapper);
        
        // 3. 设置指定地址为默认
        address.setIsDefault(1);
        return this.updateById(address);
    }
}
