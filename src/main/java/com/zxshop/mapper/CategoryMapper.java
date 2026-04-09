package com.zxshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxshop.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类 Mapper 接口
 * 
 * @author 谭鹏
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    
}
