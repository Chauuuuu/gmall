package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class CategoryVo extends CategoryEntity{
    private List<CategoryEntity> subs;
}
