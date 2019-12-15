package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    @PostMapping("pms/spuinfo/{status}")
    public Resp<List<SpuInfoEntity>> querySpuInfoByStatus(@RequestBody QueryCondition condition, @PathVariable("status")Integer status);

    @GetMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> queryAttrBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> querySpuById(@PathVariable("id") Long id);

    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCategoryByCatId(@PathVariable("catId")Long catId);

    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrandByBrandId(@PathVariable("brandId") Long brandId);

    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> getCategory(@RequestParam(value = "level",defaultValue = "0")Integer level, @RequestParam(value = "parentCid",required = false)Long parentCid);

    @GetMapping("pms/category/{pid}")
    public Resp<List<CategoryVo>> getSubCategory(@PathVariable("pid") Long pid);

    @GetMapping("pms/skuinfo/info/{skuId}")
    public Resp<SkuInfoEntity> querySkuInfoBySkuId(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/skuimages/{skuId}")
    public Resp<List<SkuImagesEntity>> querySkuImagesBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/skusaleattrvalue/{spuId}")
    public Resp<List<SkuSaleAttrValueEntity>> querySkuAttrValuesBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/spuinfodesc/info/{spuId}")
    public Resp<SpuInfoDescEntity> querySpuDescBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/attrgroup/item/group/{catId}/{spuId}")
    public Resp<List<ItemGroupVo>> queryItemGroupByCatIdAndSpuId(@PathVariable("catId")Long catId, @PathVariable("spuId")Long spuId);


}
