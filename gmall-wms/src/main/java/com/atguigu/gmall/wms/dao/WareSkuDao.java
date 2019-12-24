package com.atguigu.gmall.wms.dao;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author Chau
 * @email 188376728@qq.com
 * @date 2019-12-02 12:12:09
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    List<WareSkuEntity> checkStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    int lockStock(@Param("id") Long id,@Param("count") Integer count);

    void unlockStock(@Param("wareSkuId") Long wareSkuId, @Param("count") Integer count);

    void minusStock(@Param("wareSkuId") Long wareSkuId, @Param("count") Integer count);
}
