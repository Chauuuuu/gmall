package com.atguigu.gmall.search.listener;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.entity.Goods;
import com.atguigu.gmall.search.entity.SearchAttr;
import com.atguigu.gmall.search.feign.GmallPmsFeign;
import com.atguigu.gmall.search.feign.GmallWmsFeign;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SpuInfoListener {

    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private GmallPmsFeign gmallPmsFeign;
    @Autowired
    private GmallWmsFeign gmallWmsFeign;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "gmall-search-queue", durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE", type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"item.insert", "item.update"}
    ))
    public void listenCreate(Long spuId) {
        Resp<List<SkuInfoEntity>> listResp = gmallPmsFeign.querySkusBySpuId(spuId);
        SpuInfoEntity spuInfoEntity = gmallPmsFeign.querySpuById(spuId).getData();
        List<SkuInfoEntity> skuInfoEntityList = listResp.getData();
        if (!CollectionUtils.isEmpty(skuInfoEntityList)) {
            List<Goods> goodsList = skuInfoEntityList.stream().map(skuInfoEntity -> {
                Goods goods = new Goods();
                goods.setBrandId(skuInfoEntity.getBrandId());
                BrandEntity brandEntity = this.gmallPmsFeign.queryBrandByBrandId(skuInfoEntity.getBrandId()).getData();
                if (brandEntity != null){
                    goods.setBrandName(brandEntity.getName());
                }
                goods.setCategoryId(skuInfoEntity.getCatalogId());
                CategoryEntity categoryEntity = this.gmallPmsFeign.queryCategoryByCatId(skuInfoEntity.getCatalogId()).getData();
                if (categoryEntity != null){
                    goods.setCategoryName(categoryEntity.getName());
                }
                goods.setPic(skuInfoEntity.getSkuDefaultImg());
                goods.setPrice(skuInfoEntity.getPrice().doubleValue());
                goods.setCreateTime(spuInfoEntity.getCreateTime());
                goods.setSale(0L);
                List<WareSkuEntity> wareSkuEntities = this.gmallWmsFeign.queryWareBySkuId(skuInfoEntity.getSkuId()).getData();
                boolean flag = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                goods.setStock(flag);
                goods.setTitle(skuInfoEntity.getSkuTitle());
                goods.setSkuId(skuInfoEntity.getSkuId());
                List<ProductAttrValueEntity> attrValueEntities = gmallPmsFeign.queryAttrBySpuId(spuId).getData();
                if (!CollectionUtils.isEmpty(attrValueEntities)){
                    List<SearchAttr> searchAttrs = attrValueEntities.stream().map(attrValueEntity -> {
                        SearchAttr searchAttr = new SearchAttr();
                        searchAttr.setAttrId(attrValueEntity.getAttrId());
                        searchAttr.setAttrName(attrValueEntity.getAttrName());
                        searchAttr.setAttrValue(attrValueEntity.getAttrValue());
                        return searchAttr;
                    }).collect(Collectors.toList());
                    goods.setAttrs(searchAttrs);
                }
                return goods;
            }).collect(Collectors.toList());
            goodsRepository.saveAll(goodsList);
        }
    }
}

