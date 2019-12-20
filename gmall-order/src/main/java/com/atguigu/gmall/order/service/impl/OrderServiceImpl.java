package com.atguigu.gmall.order.service.impl;

import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptors.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.OrderItemVo;
import com.atguigu.gmall.order.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
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
    public void submit(OrderSubmitVo submitVo) {

    }
}
