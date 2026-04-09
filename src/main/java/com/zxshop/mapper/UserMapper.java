package com.zxshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxshop.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * 
 * @author 谭鹏
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
}
