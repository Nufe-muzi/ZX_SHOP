package com.zxshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zxshop.dto.CategoryRequest;
import com.zxshop.dto.CategoryTreeResponse;
import com.zxshop.entity.Category;

import java.util.List;

/**
 * 商品分类服务接口
 * 
 * @author 谭鹏
 */
public interface CategoryService extends IService<Category> {
    
    /**
     * 获取分类树
     * 
     * @return 分类树
     */
    List<CategoryTreeResponse> getCategoryTree();
    
    /**
     * 创建分类
     * 
     * @param request 分类请求
     * @return 是否成功
     */
    Boolean createCategory(CategoryRequest request);
    
    /**
     * 更新分类
     * 
     * @param id 分类ID
     * @param request 分类请求
     * @return 是否成功
     */
    Boolean updateCategory(Long id, CategoryRequest request);
    
    /**
     * 删除分类
     * 
     * @param id 分类ID
     * @return 是否成功
     */
    Boolean deleteCategory(Long id);
}
