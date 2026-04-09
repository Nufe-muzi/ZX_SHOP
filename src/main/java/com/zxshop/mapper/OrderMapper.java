package com.zxshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxshop.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper 接口
 * 
 * @author 谭鹏
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
}
