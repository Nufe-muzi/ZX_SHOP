package com.zxshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zxshop.dto.OrderCreateRequest;
import com.zxshop.entity.Order;
import com.zxshop.entity.OrderItem;

import java.util.List;
import java.util.Map;

/**
 * 订单服务接口
 * 
 * @author 谭鹏
 */
public interface OrderService extends IService<Order> {
    
    /**
     * 创建订单
     * 
     * @param userId 用户ID
     * @param request 创建请求
     * @return 订单信息
     */
    Map<String, Object> createOrder(Long userId, OrderCreateRequest request);
    
    /**
     * 分页查询订单列表
     * 
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param status 订单状态（可选）
     * @return 分页结果
     */
    Page<Map<String, Object>> getOrderPage(Long userId, Integer pageNum, Integer pageSize, Integer status);
    
    /**
     * 查询订单详情
     * 
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 订单详情（包含订单明细）
     */
    Map<String, Object> getOrderDetail(Long userId, Long orderId);
    
    /**
     * 取消订单
     * 
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 是否成功
     */
    Boolean cancelOrder(Long userId, Long orderId);
    
    /**
     * 删除订单
     * 
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 是否成功
     */
    Boolean deleteOrder(Long userId, Long orderId);
    
    /**
     * 确认收货
     * 
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 是否成功
     */
    Boolean confirmReceipt(Long userId, Long orderId);
}
