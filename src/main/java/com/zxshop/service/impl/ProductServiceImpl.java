package com.zxshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxshop.common.BusinessException;
import com.zxshop.dto.ProductRequest;
import com.zxshop.entity.Product;
import com.zxshop.mapper.ProductMapper;
import com.zxshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品服务实现类
 * 
 * @author 谭鹏
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    
    @Override
    @Transactional
    public Boolean createProduct(ProductRequest request) {
        // 创建商品
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        product.setSales(0); // 初始化销量为0
        
        return this.save(product);
    }
    
    @Override
    @Transactional
    public Boolean updateProduct(Long id, ProductRequest request) {
        // 检查商品是否存在
        Product product = this.getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 更新商品
        BeanUtils.copyProperties(request, product);
        
        return this.updateById(product);
    }
    
    @Override
    @Transactional
    public Boolean deleteProduct(Long id) {
        // 检查商品是否存在
        Product product = this.getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 逻辑删除商品
        return this.removeById(id);
    }
    
    @Override
    public List<Product> getProductsByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getCategoryId, categoryId)
               .eq(Product::getStatus, 1)
               .orderByDesc(Product::getCreateTime);
        return this.list(wrapper);
    }
    
    @Override
    public List<Product> searchProducts(String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Product::getName, keyword)
               .or()
               .like(Product::getSubtitle, keyword)
               .eq(Product::getStatus, 1)
               .orderByDesc(Product::getCreateTime);
        return this.list(wrapper);
    }
    
    @Override
    @Transactional
    public Boolean updateProductStatus(Long id, Integer status) {
        // 检查商品是否存在
        Product product = this.getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 验证状态值
        if (status != 0 && status != 1) {
            throw new BusinessException("状态值不正确，只能是 0（下架）或 1（上架）");
        }
        
        // 更新状态
        product.setStatus(status);
        return this.updateById(product);
    }
}
