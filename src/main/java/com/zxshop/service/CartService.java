package com.zxshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zxshop.dto.CartAddRequest;
import com.zxshop.dto.CartItemVO;
import com.zxshop.entity.Cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务接口
 * 
 * @author 谭鹏
 */
public interface CartService extends IService<Cart> {
    
    /**
     * 添加商品到购物车
     * 
     * @param userId 用户ID
     * @param request 添加请求
     * @return 是否成功
     */
    Boolean addToCart(Long userId, CartAddRequest request);
    
    /**
     * 查询购物车列表
     * 
     * @param userId 用户ID
     * @return 购物车项列表（带商品信息）
     */
    List<CartItemVO> getCartList(Long userId);
    
    /**
     * 更新购物车商品数量
     * 
     * @param userId 用户ID
     * @param cartId 购物车ID
     * @param quantity 数量
     * @return 是否成功
     */
    Boolean updateQuantity(Long userId, Long cartId, Integer quantity);
    
    /**
     * 删除购物车商品
     * 
     * @param userId 用户ID
     * @param cartId 购物车ID
     * @return 是否成功
     */
    Boolean removeFromCart(Long userId, Long cartId);
    
    /**
     * 选中/取消选中商品
     * 
     * @param userId 用户ID
     * @param cartId 购物车ID
     * @param checked 是否选中
     * @return 是否成功
     */
    Boolean updateChecked(Long userId, Long cartId, Integer checked);
    
    /**
     * 全选/取消全选
     * 
     * @param userId 用户ID
     * @param checked 是否选中
     * @return 是否成功
     */
    Boolean updateAllChecked(Long userId, Integer checked);
    
    /**
     * 计算购物车总价
     * 
     * @param userId 用户ID
     * @return 总价信息（总价和总数量）
     */
    Map<String, Object> calculateTotal(Long userId);
}
