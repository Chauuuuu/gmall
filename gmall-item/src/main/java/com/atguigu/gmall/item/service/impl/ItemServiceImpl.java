package com.atguigu.gmall.item.service.impl;

import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class ItemServiceImpl implements ItemService{

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;

    @Override
    public ItemVo querySkuDetails(Long skuId) {
        ItemVo itemVo = new ItemVo();
        SkuInfoEntity skuInfoEntity = gmallPmsClient.querySkuInfoBySkuId(skuId).getData();
        if (skuInfoEntity != null){
            BeanUtils.copyProperties(skuInfoEntity, itemVo);
        }
        Long spuId = skuInfoEntity.getSpuId();
        List<SkuImagesEntity> imagesEntities = gmallPmsClient.querySkuImagesBySkuId(skuId).getData();
        if (!CollectionUtils.isEmpty(imagesEntities)){
            itemVo.setPics(imagesEntities);
        }
        SpuInfoEntity spuInfoEntity = gmallPmsClient.querySpuById(spuId).getData();
        itemVo.setSpuName(spuInfoEntity.getSpuName());
        BrandEntity brandEntity = gmallPmsClient.queryBrandByBrandId(skuInfoEntity.getBrandId()).getData();
        itemVo.setBrandEntity(brandEntity);
        CategoryEntity categoryEntity = gmallPmsClient.queryCategoryByCatId(skuInfoEntity.getCatalogId()).getData();
        itemVo.setCategoryEntity(categoryEntity);

        List<SaleVo> saleVos = gmallSmsClient.querySaleBySkuId(skuId).getData();
        itemVo.setSales(saleVos);

        List<WareSkuEntity> wareSkuEntities = gmallWmsClient.queryWareBySkuId(skuId).getData();
        boolean stock = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
        itemVo.setStore(stock);

        List<SkuSaleAttrValueEntity> saleAttrValueEntities = gmallPmsClient.querySkuAttrValuesBySpuId(spuId).getData();
        itemVo.setSaleAttrs(saleAttrValueEntities);

        //spu海报
        SpuInfoDescEntity spuInfoDescEntity = gmallPmsClient.querySpuDescBySpuId(spuId).getData();
        if (spuInfoDescEntity!=null){
            String decript = spuInfoDescEntity.getDecript();
            String[] split = StringUtils.split(decript, ",");
            itemVo.setImages(Arrays.asList(split));
        }

        List<ItemGroupVo> itemGroupVoList = gmallPmsClient.queryItemGroupByCatIdAndSpuId(skuInfoEntity.getCatalogId(), spuId).getData();
        itemVo.setGroups(itemGroupVoList);
        return itemVo;
    }
}
