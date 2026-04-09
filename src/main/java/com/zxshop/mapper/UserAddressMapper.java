package com.zxshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxshop.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户地址 Mapper 接口
 * 
 * @author 谭鹏
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
    
}
