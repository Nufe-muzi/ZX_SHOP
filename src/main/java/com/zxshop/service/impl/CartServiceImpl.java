package com.zxshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxshop.common.BusinessException;
import com.zxshop.dto.CartAddRequest;
import com.zxshop.dto.CartItemVO;
import com.zxshop.entity.Cart;
import com.zxshop.entity.Product;
import com.zxshop.mapper.CartMapper;
import com.zxshop.service.CartService;
import com.zxshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 购物车服务实现类
 * 
 * @author 谭鹏
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {
    
    private final ProductService productService;
    
    @Override
    @Transactional
    public Boolean addToCart(Long userId, CartAddRequest request) {
        // 1. 检查商品是否存在
        Product product = productService.getById(request.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 2. 检查商品是否上架
        if (product.getStatus() == 0) {
            throw new BusinessException("商品已下架");
        }
        
        // 3. 检查库存
        if (product.getStock() < request.getQuantity()) {
            throw new BusinessException("库存不足");
        }
        
        // 4. 检查购物车中是否已有该商品
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .eq(Cart::getProductId, request.getProductId());
        
        if (request.getSkuId() != null) {
            wrapper.eq(Cart::getSkuId, request.getSkuId());
        } else {
            wrapper.isNull(Cart::getSkuId);
        }
        
        Cart existCart = this.getOne(wrapper);
        
        if (existCart != null) {
            // 已存在，增加数量
            int newQuantity = existCart.getQuantity() + request.getQuantity();
            
            // 检查总数量是否超过库存
            if (newQuantity > product.getStock()) {
                throw new BusinessException("超出商品库存限制");
            }
            
            existCart.setQuantity(newQuantity);
            return this.updateById(existCart);
        } else {
            // 不存在，新增购物车项
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(request.getProductId());
            cart.setSkuId(request.getSkuId());
            cart.setQuantity(request.getQuantity());
            cart.setChecked(1); // 默认选中
            return this.save(cart);
        }
    }
    
    @Override
    public List<CartItemVO> getCartList(Long userId) {
        // 1. 查询用户购物车
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .orderByDesc(Cart::getCreateTime);
        List<Cart> cartList = this.list(wrapper);
        
        if (cartList.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 2. 获取所有商品ID
        List<Long> productIds = cartList.stream()
                .map(Cart::getProductId)
                .distinct()
                .collect(Collectors.toList());
        
        // 3. 批量查询商品信息
        List<Product> products = productService.listByIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        
        // 4. 组装购物车项 VO
        return cartList.stream().map(cart -> {
            CartItemVO vo = new CartItemVO();
            vo.setCartId(cart.getId());
            vo.setProductId(cart.getProductId());
            vo.setSkuId(cart.getSkuId());
            vo.setQuantity(cart.getQuantity());
            vo.setChecked(cart.getChecked());
            
            // 填充商品信息
            Product product = productMap.get(cart.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductSubtitle(product.getSubtitle());
                vo.setProductImage(product.getMainImage());
                vo.setProductPrice(product.getPrice());
                vo.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public Boolean updateQuantity(Long userId, Long cartId, Integer quantity) {
        if (quantity < 1) {
            throw new BusinessException("数量必须大于0");
        }
        
        // 1. 查询购物车项
        Cart cart = this.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        
        // 2. 检查商品库存
        Product product = productService.getById(cart.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        if (quantity > product.getStock()) {
            throw new BusinessException("超出商品库存限制");
        }
        
        // 3. 更新数量
        cart.setQuantity(quantity);
        return this.updateById(cart);
    }
    
    @Override
    @Transactional
    public Boolean removeFromCart(Long userId, Long cartId) {
        // 1. 查询购物车项
        Cart cart = this.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        
        // 2. 删除
        return this.removeById(cartId);
    }
    
    @Override
    @Transactional
    public Boolean updateChecked(Long userId, Long cartId, Integer checked) {
        // 1. 查询购物车项
        Cart cart = this.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        
        // 2. 更新选中状态
        cart.setChecked(checked);
        return this.updateById(cart);
    }
    
    @Override
    @Transactional
    public Boolean updateAllChecked(Long userId, Integer checked) {
        // 1. 查询用户所有购物车项
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        List<Cart> cartList = this.list(wrapper);
        
        if (cartList.isEmpty()) {
            return true;
        }
        
        // 2. 批量更新选中状态
        cartList.forEach(cart -> cart.setChecked(checked));
        return this.updateBatchById(cartList);
    }
    
    @Override
    public Map<String, Object> calculateTotal(Long userId) {
        // 1. 查询用户购物车
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        List<Cart> cartList = this.list(wrapper);
        
        if (cartList.isEmpty()) {
            return Map.of(
                    "totalAmount", BigDecimal.ZERO,
                    "totalCount", 0,
                    "checkedCount", 0
            );
        }
        
        // 2. 获取所有商品ID
        List<Long> productIds = cartList.stream()
                .map(Cart::getProductId)
                .distinct()
                .collect(Collectors.toList());
        
        // 3. 批量查询商品信息
        List<Product> products = productService.listByIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        
        // 4. 计算总价和数量
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = 0;
        int checkedCount = 0;
        
        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProductId());
            if (product != null) {
                BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
                totalCount += cart.getQuantity();
                
                if (cart.getChecked() == 1) {
                    totalAmount = totalAmount.add(itemTotal);
                    checkedCount += cart.getQuantity();
                }
            }
        }
        
        return Map.of(
                "totalAmount", totalAmount,
                "totalCount", totalCount,
                "checkedCount", checkedCount
        );
    }
}
