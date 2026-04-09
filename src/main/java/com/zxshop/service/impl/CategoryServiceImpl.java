package com.zxshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxshop.common.BusinessException;
import com.zxshop.dto.CategoryRequest;
import com.zxshop.dto.CategoryTreeResponse;
import com.zxshop.entity.Category;
import com.zxshop.mapper.CategoryMapper;
import com.zxshop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现类
 * 
 * @author 谭鹏
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    
    @Override
    public List<CategoryTreeResponse> getCategoryTree() {
        // 1. 查询所有启用的分类，按排序字段排序
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
               .orderByAsc(Category::getSortOrder)
               .orderByAsc(Category::getId);
        List<Category> allCategories = this.list(wrapper);
        
        // 2. 构建分类树
        return buildCategoryTree(allCategories, 0L);
    }
    
    /**
     * 递归构建分类树
     * 
     * @param allCategories 所有分类
     * @param parentId 父分类ID
     * @return 分类树列表
     */
    private List<CategoryTreeResponse> buildCategoryTree(List<Category> allCategories, Long parentId) {
        return allCategories.stream()
                .filter(category -> category.getParentId().equals(parentId))
                .map(category -> {
                    CategoryTreeResponse treeResponse = new CategoryTreeResponse();
                    BeanUtils.copyProperties(category, treeResponse);
                    
                    // 递归查找子分类
                    List<CategoryTreeResponse> children = buildCategoryTree(allCategories, category.getId());
                    if (!children.isEmpty()) {
                        treeResponse.setChildren(children);
                    }
                    
                    return treeResponse;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public Boolean createCategory(CategoryRequest request) {
        // 1. 检查父分类是否存在
        if (request.getParentId() != 0) {
            Long count = this.count(new LambdaQueryWrapper<Category>().eq(Category::getId, request.getParentId()));
            if (count == 0) {
                throw new BusinessException("父分类不存在");
            }
        }
        
        // 2. 创建分类
        Category category = new Category();
        BeanUtils.copyProperties(request, category);
        
        // 3. 自动设置层级
        if (request.getParentId() == 0) {
            category.setLevel(1);
        } else {
            Category parent = this.getById(request.getParentId());
            category.setLevel(parent.getLevel() + 1);
        }
        
        return this.save(category);
    }
    
    @Override
    @Transactional
    public Boolean updateCategory(Long id, CategoryRequest request) {
        // 1. 检查分类是否存在
        Category category = this.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 2. 不能将自己设置为父分类
        if (request.getParentId().equals(id)) {
            throw new BusinessException("不能将分类设置为自己的父分类");
        }
        
        // 3. 检查父分类是否存在
        if (request.getParentId() != 0) {
            Long count = this.count(new LambdaQueryWrapper<Category>().eq(Category::getId, request.getParentId()));
            if (count == 0) {
                throw new BusinessException("父分类不存在");
            }
        }
        
        // 4. 更新分类
        BeanUtils.copyProperties(request, category);
        
        // 5. 重新计算层级
        if (request.getParentId() == 0) {
            category.setLevel(1);
        } else {
            Category parent = this.getById(request.getParentId());
            category.setLevel(parent.getLevel() + 1);
        }
        
        return this.updateById(category);
    }
    
    @Override
    @Transactional
    public Boolean deleteCategory(Long id) {
        // 1. 检查分类是否存在
        Category category = this.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 2. 检查是否有子分类
        Long childCount = this.count(new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("该分类下有子分类，请先删除子分类");
        }
        
        // 3. 逻辑删除分类
        return this.removeById(id);
    }
}
