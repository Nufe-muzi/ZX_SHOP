package com.zxshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxshop.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车 Mapper 接口
 * 
 * @author 谭鹏
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {
    
}
