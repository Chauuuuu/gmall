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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService{

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public ItemVo querySkuDetails(Long skuId) {
        ItemVo itemVo = new ItemVo();

        CompletableFuture<SkuInfoEntity> skuInfoEntityCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity skuInfoEntity = gmallPmsClient.querySkuInfoBySkuId(skuId).getData();
            if (skuInfoEntity != null) {
                BeanUtils.copyProperties(skuInfoEntity, itemVo);
            }
            return skuInfoEntity;
        }, threadPoolExecutor);

        CompletableFuture<Void> setImages = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> imagesEntities = gmallPmsClient.querySkuImagesBySkuId(skuId).getData();
            if (!CollectionUtils.isEmpty(imagesEntities)) {
                itemVo.setPics(imagesEntities);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> setSpuName = skuInfoEntityCompletableFuture.thenAcceptAsync(sku -> {
            SpuInfoEntity spuInfoEntity = gmallPmsClient.querySpuById(sku.getSpuId()).getData();
            itemVo.setSpuName(spuInfoEntity.getSpuName());
        }, threadPoolExecutor);

        CompletableFuture<Void> setBrandEntity = skuInfoEntityCompletableFuture.thenAcceptAsync(sku -> {
            BrandEntity brandEntity = gmallPmsClient.queryBrandByBrandId(sku.getBrandId()).getData();
            itemVo.setBrandEntity(brandEntity);
        }, threadPoolExecutor);

        CompletableFuture<Void> setCategoryEntity = skuInfoEntityCompletableFuture.thenAcceptAsync(sku -> {
            CategoryEntity categoryEntity = gmallPmsClient.queryCategoryByCatId(sku.getCatalogId()).getData();
            itemVo.setCategoryEntity(categoryEntity);
        }, threadPoolExecutor);

        CompletableFuture<Void> setSales = CompletableFuture.runAsync(() -> {
            List<SaleVo> saleVos = gmallSmsClient.querySaleBySkuId(skuId).getData();
            itemVo.setSales(saleVos);
        }, threadPoolExecutor);

        CompletableFuture<Void> setStore = CompletableFuture.runAsync(() -> {
            List<WareSkuEntity> wareSkuEntities = gmallWmsClient.queryWareBySkuId(skuId).getData();
            boolean stock = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
            itemVo.setStore(stock);
        }, threadPoolExecutor);

        CompletableFuture<Void> setSaleAttrs = skuInfoEntityCompletableFuture.thenAcceptAsync(sku -> {
            List<SkuSaleAttrValueEntity> saleAttrValueEntities = gmallPmsClient.querySkuAttrValuesBySpuId(sku.getSpuId()).getData();
            itemVo.setSaleAttrs(saleAttrValueEntities);
        }, threadPoolExecutor);

        //spu海报
        CompletableFuture<Void> setSpuDesc = skuInfoEntityCompletableFuture.thenAcceptAsync(sku -> {
            SpuInfoDescEntity spuInfoDescEntity = gmallPmsClient.querySpuDescBySpuId(sku.getSpuId()).getData();
            if (spuInfoDescEntity != null) {
                String decript = spuInfoDescEntity.getDecript();
                String[] split = StringUtils.split(decript, ",");
                itemVo.setImages(Arrays.asList(split));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> setGroups = skuInfoEntityCompletableFuture.thenAcceptAsync(sku -> {
            List<ItemGroupVo> itemGroupVoList = gmallPmsClient.queryItemGroupByCatIdAndSpuId(sku.getCatalogId(), sku.getSpuId()).getData();
            itemVo.setGroups(itemGroupVoList);
        }, threadPoolExecutor);

        CompletableFuture.allOf(skuInfoEntityCompletableFuture,setImages,setSpuName,setBrandEntity,
                setCategoryEntity,setSales,setStore,setSaleAttrs,setSpuDesc,setGroups).join();
        return itemVo;
    }
}
