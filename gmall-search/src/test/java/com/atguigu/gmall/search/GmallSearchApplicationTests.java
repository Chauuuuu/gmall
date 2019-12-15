package com.atguigu.gmall.search;

import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.entity.Goods;
import com.atguigu.gmall.search.entity.SearchAttr;
import com.atguigu.gmall.search.feign.GmallPmsFeign;
import com.atguigu.gmall.search.feign.GmallWmsFeign;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private GmallPmsFeign gmallPmsFeign;
    @Autowired
    private GmallWmsFeign gmallWmsFeign;

    @Test
    void contextLoads() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
    }

    @Test
    public void importData() {
        Long pageNum = 1l;
        Long pageSize = 100l;
        do {
            //分页查询
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setPage(pageNum);
            queryCondition.setLimit(pageSize);
            Resp<List<SpuInfoEntity>> spuListResp = gmallPmsFeign.querySpuInfoByStatus(queryCondition, 1);
            if (spuListResp == null){
                continue;
            }
            List<SpuInfoEntity> spuInfoEntityList = spuListResp.getData();
            pageSize = (long) spuInfoEntityList.size();
            //遍历spu，查询sku
            spuInfoEntityList.forEach(spuInfoEntity -> {
                Resp<List<SkuInfoEntity>> skuListResp = gmallPmsFeign.querySkusBySpuId(spuInfoEntity.getId());
            List<SkuInfoEntity> skuInfoEntityList = skuListResp.getData();
            //把sku转成goods
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
                    goods.setSale(0l);
                    List<WareSkuEntity> wareSkuEntities = this.gmallWmsFeign.queryWareBySkuId(skuInfoEntity.getSkuId()).getData();
                    boolean flag = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                    goods.setStock(flag);
                    goods.setTitle(skuInfoEntity.getSkuTitle());
                    goods.setSkuId(skuInfoEntity.getSkuId());
                    List<ProductAttrValueEntity> attrValueEntities = gmallPmsFeign.queryAttrBySpuId(spuInfoEntity.getId()).getData();
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
        });
            //导入索引库
            pageNum++;
        }
        while (pageSize == 100);
    }
}
