package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;

import java.util.List;

public interface IndexService {
    List<CategoryEntity> queryLevel1Category();

    List<CategoryVo> queryAllCategory(Long pid);

    void testLock();
}
