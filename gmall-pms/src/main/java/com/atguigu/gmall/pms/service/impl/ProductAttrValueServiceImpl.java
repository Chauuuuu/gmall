package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.vo.ProductAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.ProductAttrValueDao;
import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import com.atguigu.gmall.pms.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Autowired
    private ProductAttrValueDao productAttrValueDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public void saveProductAttrValue(SpuInfoVo spuInfoVo, Long spuId) {
        List<ProductAttrValueVo> baseAttrs = spuInfoVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<ProductAttrValueEntity> productAttrValueEntityList = baseAttrs.stream()
                    .map(productAttrValueVo -> {
                        ProductAttrValueEntity productAttrValueEntity = productAttrValueVo;
                        productAttrValueVo.setQuickShow(0);
                        productAttrValueVo.setAttrSort(0);
                        productAttrValueVo.setSpuId(spuId);
                        return productAttrValueVo;
                    }).collect(Collectors.toList());
            this.saveBatch(productAttrValueEntityList);
        }
    }

    @Override
    public List<ProductAttrValueEntity> querySearchAttrValue(Long spuId) {
        List<ProductAttrValueEntity> attrValues = productAttrValueDao.querySearchAttrValue(spuId);
        return attrValues;
    }

}