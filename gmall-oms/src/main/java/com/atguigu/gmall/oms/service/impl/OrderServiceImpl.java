package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.dao.OrderItemDao;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private GmallUmsClient gmallUmsClient;
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private OrderItemDao orderItemDao;
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo) {
        OrderEntity orderEntity = new OrderEntity();
        MemberReceiveAddressEntity address = submitVo.getAddress();
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        orderEntity.setReceiverCity(address.getCity());
        MemberEntity memberEntity = gmallUmsClient.queryMemberById(submitVo.getUserId()).getData();
        orderEntity.setMemberUsername(memberEntity.getUsername());
        orderEntity.setMemberId(submitVo.getUserId());
        // 清算每个商品赠送积分
        orderEntity.setIntegration(0);
        orderEntity.setGrowth(0);

        orderEntity.setDeleteStatus(0);
        orderEntity.setStatus(0);

        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(orderEntity.getCreateTime());
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        orderEntity.setSourceType(1);
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setOrderSn(submitVo.getOrderToken());
        this.save(orderEntity);
        Long orderId = orderEntity.getId();

        List<OrderItemVo> items = submitVo.getItems();
        items.forEach(item -> {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setSkuId(item.getSkuId());
            Resp<SkuInfoEntity> skuInfoEntityResp = this.gmallPmsClient.querySkuInfoBySkuId(item.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();

            Resp<SpuInfoEntity> spuInfoEntityResp = this.gmallPmsClient.querySpuById(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();

            itemEntity.setSkuPrice(skuInfoEntity.getPrice());
            itemEntity.setSkuAttrsVals(JSON.toJSONString(item.getSkuAttrValue()));
            itemEntity.setCategoryId(skuInfoEntity.getCatalogId());
            itemEntity.setOrderId(orderId);
            itemEntity.setOrderSn(submitVo.getOrderToken());
            itemEntity.setSpuId(spuInfoEntity.getId());
            itemEntity.setSkuName(skuInfoEntity.getSkuName());
            itemEntity.setSkuPic(skuInfoEntity.getSkuDefaultImg());
            itemEntity.setSkuQuantity(item.getCount());
            itemEntity.setSpuName(spuInfoEntity.getSpuName());

            this.orderItemDao.insert(itemEntity);
        });

        amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "order.ttl", submitVo.getOrderToken());

        return orderEntity;
    }
}