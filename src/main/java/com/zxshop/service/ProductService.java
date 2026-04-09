package com.zxshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zxshop.dto.ProductRequest;
import com.zxshop.entity.Product;

import java.util.List;
import java.util.Map;

/**
 * 商品服务接口
 * 
 * @author 谭鹏
 */
public interface ProductService extends IService<Product> {
    
    /**
     * 创建商品
     * 
     * @param request 商品请求
     * @return 是否成功
     */
    Boolean createProduct(ProductRequest request);
    
    /**
     * 更新商品
     * 
     * @param id 商品ID
     * @param request 商品请求
     * @return 是否成功
     */
    Boolean updateProduct(Long id, ProductRequest request);
    
    /**
     * 删除商品
     * 
     * @param id 商品ID
     * @return 是否成功
     */
    Boolean deleteProduct(Long id);
    
    /**
     * 根据分类ID查询商品列表
     * 
     * @param categoryId 分类ID
     * @return 商品列表
     */
    List<Product> getProductsByCategoryId(Long categoryId);
    
    /**
     * 搜索商品
     * 
     * @param keyword 关键词
     * @return 商品列表
     */
    List<Product> searchProducts(String keyword);
    
    /**
     * 商品上下架
     * 
     * @param id 商品ID
     * @param status 状态（0-下架，1-上架）
     * @return 是否成功
     */
    Boolean updateProductStatus(Long id, Integer status);
}
