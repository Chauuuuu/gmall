package com.atguigu.gmall.order.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptors.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private GmallUmsClient gmallUmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;
    @Autowired
    private GmallOmsClient gmallOmsClient;
    @Autowired
    private GmallCartClient gmallCartClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    private static final String TOKEN_PREFIX = "order:token:";

    @Override
    public OrderConfirmVo confirm() {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getId();
        if (userInfo == null) {
            return null;
        }
        CompletableFuture<Void> addressesFu = CompletableFuture.runAsync(() -> {
            List<MemberReceiveAddressEntity> addressEntityList = gmallUmsClient.queryAddresses(userId).getData();
            orderConfirmVo.setAddresses(addressEntityList);
        }, threadPoolExecutor);

        CompletableFuture<Void> boundsFu = CompletableFuture.runAsync(() -> {
            MemberEntity memberEntity = gmallUmsClient.queryMemberById(userId).getData();
            orderConfirmVo.setBounds(memberEntity.getIntegration());
        }, threadPoolExecutor);

        CompletableFuture<Void> orderItemFu = CompletableFuture.supplyAsync(() -> {
            List<Cart> cartList = gmallCartClient.queryCheckedCartsByUserId(userId).getData();
            if (CollectionUtils.isEmpty(cartList)) {
                throw new OrderException("请勾选购物车商品");
            }
            return cartList;
        }, threadPoolExecutor).thenAcceptAsync(cartList -> {
            List<OrderItemVo> orderItemVos = cartList.stream().map(cart -> {
                OrderItemVo orderItemVo = new OrderItemVo();
                Long skuId = cart.getSkuId();
                CompletableFuture<Void> itemVoFu = CompletableFuture.runAsync(() -> {
                    SkuInfoEntity skuInfoEntity = gmallPmsClient.querySkuInfoBySkuId(skuId).getData();
                    if (skuInfoEntity != null) {
                        orderItemVo.setTitle(skuInfoEntity.getSkuTitle());
                        orderItemVo.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVo.setWeight(skuInfoEntity.getWeight());
                        orderItemVo.setPrice(skuInfoEntity.getPrice());
                        orderItemVo.setSkuId(skuId);
                        orderItemVo.setCount(cart.getCount());
                    }
                }, threadPoolExecutor);
                CompletableFuture<Void> salesFu = CompletableFuture.runAsync(() -> {
                    List<SaleVo> saleVos = gmallSmsClient.querySaleBySkuId(skuId).getData();
                    orderItemVo.setSales(saleVos);
                }, threadPoolExecutor);
                CompletableFuture<Void> saleAttrsFu = CompletableFuture.runAsync(() -> {
                    List<SkuSaleAttrValueEntity> skuSaleAttrValueEntityList = gmallPmsClient.querySkuAttrValuesBySkuId(skuId).getData();
                    orderItemVo.setSkuAttrValue(skuSaleAttrValueEntityList);
                }, threadPoolExecutor);
                CompletableFuture<Void> stockFu = CompletableFuture.runAsync(() -> {
                    List<WareSkuEntity> wareSkuEntities = gmallWmsClient.queryWareBySkuId(skuId).getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        boolean stock = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                        orderItemVo.setStock(stock);
                    }
                }, threadPoolExecutor);
                CompletableFuture.allOf(itemVoFu, salesFu, saleAttrsFu, stockFu).join();
                return orderItemVo;
            }).collect(Collectors.toList());
            orderConfirmVo.setOrderItems(orderItemVos);
        });

        CompletableFuture<Void> orderTokenFu = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getIdStr();
            orderConfirmVo.setOrderToken(orderToken);
            stringRedisTemplate.opsForValue().set(TOKEN_PREFIX + orderToken, orderToken);
        }, threadPoolExecutor);
        CompletableFuture.allOf(addressesFu, boundsFu, orderItemFu, orderTokenFu).join();
        return orderConfirmVo;
    }

    @Override
    public OrderEntity submit(OrderSubmitVo submitVo) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
//        验证令牌防止重复提交
        String orderToken = submitVo.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = stringRedisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList(TOKEN_PREFIX + orderToken), orderToken);
        if(flag == 0){
            throw new OrderException("订单不可重复提交");
        }
//        验证价格
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("购物车中没有商品，请选择商品");
        }
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            SkuInfoEntity skuInfoEntity = gmallPmsClient.querySkuInfoBySkuId(item.getSkuId()).getData();
            if (skuInfoEntity == null) {
                return new BigDecimal(0);
            }
            BigDecimal price = skuInfoEntity.getPrice().multiply(new BigDecimal(item.getCount()));
            return price;
        }).reduce((a, b) -> a.add(b)).get();
        if (currentTotalPrice.compareTo(totalPrice)!=0){
            throw new OrderException("页面已过期，请刷新页面后重试");
        }
//        验证库存，并锁定库存
        List<SkuLockVo> skuLockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setCount(item.getCount());
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setOrderToken(orderToken);
            return skuLockVo;
        }).collect(Collectors.toList());
        Resp<Object> wareResp = gmallWmsClient.checkAndLockStock(skuLockVos);
        if (wareResp.getCode() != 0){
            throw new OrderException(wareResp.getMsg());
        }

//        int i = 1/0;
//        生成订单
        Resp<OrderEntity> orderEntityResp = null;
        try {
            submitVo.setUserId(userInfo.getId());
            orderEntityResp = gmallOmsClient.saveOrder(submitVo);
        } catch (Exception e) {
            e.printStackTrace();
            amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "stock.unlock", orderToken);
            throw new OrderException("创建订单失败，请联系客服");
        }
//        删购物车中对应的记录（消息队列）
        Map<String,Object> map = new HashMap<>();
        map.put("userId", userInfo.getId());
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds", skuIds);
        amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE","cart.delete",map);

        if (orderEntityResp.getData() != null){
            return  orderEntityResp.getData();
        }
        return null;
    }
}
