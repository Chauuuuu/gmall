package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.service.SkuFullReductionService;
import com.atguigu.gmall.sms.service.SkuLadderService;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuLadderService skuLadderService;
    @Autowired
    private SkuFullReductionService skuFullReductionService;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public void saveSale(SkuSaleVo skuSaleVo) {
        //保存Bounds
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(skuSaleVo, skuBoundsEntity);
        List<Integer> work = skuSaleVo.getWork();
        if (!CollectionUtils.isEmpty(work)){
            skuBoundsEntity.setWork(work.get(0) * 8 + work.get(1) * 4 + work.get(2) * 2 + work.get(3));
        }
        skuBoundsEntity.setSkuId(skuSaleVo.getSkuId());
        this.save(skuBoundsEntity);
        //保存ladder
        skuLadderService.skuLadderSave(skuSaleVo);
        //保存fullReduction
        skuFullReductionService.skuFullReductionSave(skuSaleVo);
    }

    @Override
    public List<SaleVo> querySaleBySkuId(Long skuId) {
        List<SaleVo> voList = new ArrayList<>();
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity!=null){
            SaleVo boundsVo = new SaleVo();
            boundsVo.setType("积分");
            StringBuffer sb = new StringBuffer();
            if (skuBoundsEntity.getGrowBounds()!=null && skuBoundsEntity.getGrowBounds().intValue() >0){
                sb.append("成长积分送"+ skuBoundsEntity.getGrowBounds());
            }
            if (skuBoundsEntity.getBuyBounds()!=null && skuBoundsEntity.getBuyBounds().intValue()>0){
                if (!StringUtils.isEmpty(sb)){
                    sb.append(",");
                }
                sb.append("购买积分送"+skuBoundsEntity.getBuyBounds());
            }
            boundsVo.setDesc(sb.toString());
            voList.add(boundsVo);
        }

        SkuLadderEntity skuLadderEntity = this.skuLadderService.getOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity != null) {
            SaleVo ladderVO = new SaleVo();
            ladderVO.setType("打折");
            ladderVO.setDesc("满" + skuLadderEntity.getFullCount() + "件，打" + skuLadderEntity.getDiscount().divide(new BigDecimal(10)) + "折");
            voList.add(ladderVO);
        }

        SkuFullReductionEntity reductionEntity = this.skuFullReductionService.getOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (reductionEntity != null) {
            SaleVo reductionVO = new SaleVo();
            reductionVO.setType("满减");
            reductionVO.setDesc("满" + reductionEntity.getFullPrice() + "减" + reductionEntity.getReducePrice());
            voList.add(reductionVO);
        }
        return voList;
    }
}