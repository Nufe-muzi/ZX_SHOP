package com.zxshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxshop.entity.OrderItem;
import com.zxshop.mapper.OrderItemMapper;
import com.zxshop.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 订单明细服务实现类
 * 
 * @author 谭鹏
 */
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
    
}
