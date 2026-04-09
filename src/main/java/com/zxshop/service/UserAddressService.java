package com.zxshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zxshop.dto.AddressRequest;
import com.zxshop.entity.UserAddress;

import java.util.List;

/**
 * 用户地址服务接口
 * 
 * @author 谭鹏
 */
public interface UserAddressService extends IService<UserAddress> {
    
    /**
     * 添加地址
     * 
     * @param userId 用户ID
     * @param request 地址请求
     * @return 是否成功
     */
    Boolean addAddress(Long userId, AddressRequest request);
    
    /**
     * 更新地址
     * 
     * @param userId 用户ID
     * @param addressId 地址ID
     * @param request 地址请求
     * @return 是否成功
     */
    Boolean updateAddress(Long userId, Long addressId, AddressRequest request);
    
    /**
     * 删除地址
     * 
     * @param userId 用户ID
     * @param addressId 地址ID
     * @return 是否成功
     */
    Boolean deleteAddress(Long userId, Long addressId);
    
    /**
     * 查询用户地址列表
     * 
     * @param userId 用户ID
     * @return 地址列表
     */
    List<UserAddress> getAddressList(Long userId);
    
    /**
     * 设置默认地址
     * 
     * @param userId 用户ID
     * @param addressId 地址ID
     * @return 是否成功
     */
    Boolean setDefaultAddress(Long userId, Long addressId);
}
