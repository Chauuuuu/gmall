package com.atguigu.gmall.wms.service.impl;

import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private WareSkuDao wareSkuDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public List<WareSkuEntity> queryWareBySkuId(Long skuId) {
        List<WareSkuEntity> wareList = this.list(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId));
        return wareList;
    }

    @Override
    public String checkAndLockStock(List<SkuLockVo> skuLockVos) {
        skuLockVos.forEach(skuLockVo -> {
            lockStock(skuLockVo);
        });

        List<SkuLockVo> unlockVos = skuLockVos.stream().filter(skuLockVo ->
                skuLockVo.getLockState() == false
        ).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(unlockVos)){
            List<SkuLockVo> lockVos = skuLockVos.stream().filter(SkuLockVo::getLockState).collect(Collectors.toList());
            lockVos.forEach(lockVo ->{
                wareSkuDao.unlockStock(lockVo.getWareSkuId(),lockVo.getCount());
            });
            List<Long> unlockSkuIds = unlockVos.stream().map(unlockVo ->
                    unlockVo.getSkuId()).collect(Collectors.toList());
            return "下单失败，商品库存不足:"+unlockSkuIds.toString();

        }
        return "下单成功";
    }

    private void lockStock(SkuLockVo skuLockVo){
        RLock lock = redissonClient.getLock("stock:" + skuLockVo.getSkuId());
        lock.lock();
        List<WareSkuEntity> wareSkuEntityList = wareSkuDao.checkStock(skuLockVo.getSkuId(),skuLockVo.getCount());
        if (!CollectionUtils.isEmpty(wareSkuEntityList)){
            Long id = wareSkuEntityList.get(0).getId();
            wareSkuDao.lockStock(id,skuLockVo.getCount());
            skuLockVo.setWareSkuId(id);
            skuLockVo.setLockState(true);
        }
        else {
            skuLockVo.setLockState(false);
        }
        lock.unlock();
    }

}