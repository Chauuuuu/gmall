package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface GmallPmsApi {

    @GetMapping("pms/spuinfo/${status}")
    public Resp<PageVo> querySpuInfoByStatus(QueryCondition condition, @PathVariable("status")Integer status);

    @GetMapping("pms/productattrvalue/${spuId}")
    public Resp<List<ProductAttrValueEntity>> queryAttrBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCategoryByCatId(@PathVariable("catId")Long catId);

    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrandByBrandId(@PathVariable("brandId") Long brandId);
}
