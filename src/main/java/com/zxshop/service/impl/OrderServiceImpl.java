package com.zxshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxshop.common.BusinessException;
import com.zxshop.dto.OrderCreateRequest;
import com.zxshop.entity.*;
import com.zxshop.mapper.OrderMapper;
import com.zxshop.service.CartService;
import com.zxshop.service.OrderItemService;
import com.zxshop.service.OrderService;
import com.zxshop.service.ProductService;
import com.zxshop.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 
 * @author 谭鹏
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    
    private final CartService cartService;
    private final ProductService productService;
    private final UserAddressService addressService;
    private final OrderItemService orderItemService;
    
    @Override
    @Transactional
    public Map<String, Object> createOrder(Long userId, OrderCreateRequest request) {
        // 1. 查询收货地址
        UserAddress address = addressService.getById(request.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }
        
        // 2. 查询购物车项
        List<Cart> cartList;
        if (request.getCartIds() != null && !request.getCartIds().isEmpty()) {
            // 指定购物车项
            cartList = cartService.listByIds(request.getCartIds());
            cartList = cartList.stream()
                    .filter(cart -> cart.getUserId().equals(userId) && cart.getChecked() == 1)
                    .collect(Collectors.toList());
        } else {
            // 查询所有选中的购物车项
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getUserId, userId)
                   .eq(Cart::getChecked, 1);
            cartList = cartService.list(wrapper);
        }
        
        if (cartList.isEmpty()) {
            throw new BusinessException("请选择要结算的商品");
        }
        
        // 3. 校验库存并计算总价
        List<Long> productIds = cartList.stream()
                .map(Cart::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productService.listByIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在");
            }
            
            // 检查库存
            if (product.getStock() < cart.getQuantity()) {
                throw new BusinessException("商品【" + product.getName() + "】库存不足");
            }
            
            // 检查是否上架
            if (product.getStatus() == 0) {
                throw new BusinessException("商品【" + product.getName() + "】已下架");
            }
            
            // 计算小计
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
            
            // 创建订单明细
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setSkuId(cart.getSkuId());
            item.setPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setTotalAmount(subtotal);
            orderItems.add(item);
        }
        
        // 4. 生成订单号
        String orderNo = generateOrderNo();
        
        // 5. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount); // 暂时无优惠
        order.setFreightAmount(BigDecimal.ZERO); // 暂时无运费
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setStatus(0); // 待付款
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + 
                address.getDistrict() + address.getDetailAddress());
        order.setRemark(request.getRemark());
        
        boolean saved = this.save(order);
        if (!saved) {
            throw new BusinessException("创建订单失败");
        }
        
        // 6. 保存订单明细
        orderItems.forEach(item -> {
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
        });
        orderItemService.saveBatch(orderItems);
        
        // 7. 扣减库存
        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProductId());
            product.setStock(product.getStock() - cart.getQuantity());
            product.setSales(product.getSales() + cart.getQuantity());
        }
        productService.updateBatchById(products);
        
        // 8. 删除购物车中已结算的商品
        List<Long> cartIds = cartList.stream().map(Cart::getId).collect(Collectors.toList());
        cartService.removeByIds(cartIds);
        
        // 9. 返回订单信息
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", orderNo);
        result.put("totalAmount", totalAmount);
        result.put("status", order.getStatus());
        
        return result;
    }
    
    @Override
    public Page<Map<String, Object>> getOrderPage(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .orderByDesc(Order::getCreateTime);
        
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        
        Page<Order> orderPage = this.page(page, wrapper);
        
        // 转换为 Map 并添加商品数量
        Page<Map<String, Object>> resultPage = new Page<>();
        resultPage.setCurrent(orderPage.getCurrent());
        resultPage.setSize(orderPage.getSize());
        resultPage.setTotal(orderPage.getTotal());
        resultPage.setPages(orderPage.getPages());
        
        List<Map<String, Object>> records = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("totalAmount", order.getTotalAmount());
            map.put("status", order.getStatus());
            map.put("createTime", order.getCreateTime());
            
            // 查询订单商品数量
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            long itemCount = orderItemService.count(itemWrapper);
            map.put("itemCount", itemCount);
            
            return map;
        }).collect(Collectors.toList());
        
        resultPage.setRecords(records);
        return resultPage;
    }
    
    @Override
    public Map<String, Object> getOrderDetail(Long userId, Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        // 2. 查询订单明细
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemService.list(wrapper);
        
        // 3. 组装返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("totalAmount", order.getTotalAmount());
        result.put("payAmount", order.getPayAmount());
        result.put("freightAmount", order.getFreightAmount());
        result.put("discountAmount", order.getDiscountAmount());
        result.put("status", order.getStatus());
        result.put("payType", order.getPayType());
        result.put("payTime", order.getPayTime());
        result.put("receiverName", order.getReceiverName());
        result.put("receiverPhone", order.getReceiverPhone());
        result.put("receiverAddress", order.getReceiverAddress());
        result.put("remark", order.getRemark());
        result.put("createTime", order.getCreateTime());
        result.put("items", items);
        
        return result;
    }
    
    @Override
    @Transactional
    public Boolean cancelOrder(Long userId, Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        // 2. 只有待付款状态可以取消
        if (order.getStatus() != 0) {
            throw new BusinessException("只能取消待付款的订单");
        }
        
        // 3. 更新订单状态
        order.setStatus(4); // 已取消
        boolean updated = this.updateById(order);
        
        if (updated) {
            // 4. 恢复库存
            LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderItem::getOrderId, orderId);
            List<OrderItem> items = orderItemService.list(wrapper);
            
            List<Long> productIds = items.stream()
                    .map(OrderItem::getProductId)
                    .distinct()
                    .collect(Collectors.toList());
            List<Product> products = productService.listByIds(productIds);
            Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
            
            for (OrderItem item : items) {
                Product product = productMap.get(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    product.setSales(product.getSales() - item.getQuantity());
                }
            }
            productService.updateBatchById(products);
        }
        
        return updated;
    }
    
    @Override
    @Transactional
    public Boolean deleteOrder(Long userId, Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        // 2. 只能删除已取消或已完成的订单
        if (order.getStatus() != 3 && order.getStatus() != 4) {
            throw new BusinessException("只能删除已完成或已取消的订单");
        }
        
        // 3. 逻辑删除
        return this.removeById(orderId);
    }
    
    @Override
    @Transactional
    public Boolean confirmReceipt(Long userId, Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        // 2. 只有待收货状态可以确认收货
        if (order.getStatus() != 2) {
            throw new BusinessException("订单状态不正确");
        }
        
        // 3. 更新订单状态为已完成
        order.setStatus(3);
        return this.updateById(order);
    }
    
    /**
     * 生成订单号
     * 格式：年月日时分秒 + 6位随机数
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900000) + 100000;
        return timestamp + random;
    }
}
