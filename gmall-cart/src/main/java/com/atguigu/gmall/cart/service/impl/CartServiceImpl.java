package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptors.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService{

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "gmall:cart:";
    private static final String PRICE_PREFIX = "gmall:currentprice:";

    @Override
    public void addCart(Cart cart) {
        String key = KEY_PREFIX;
        Long skuId = cart.getSkuId();
        Integer count = cart.getCount();
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getId() != null){
            key += userInfo.getId();
        }
        else {
            key += userInfo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> cartHash = stringRedisTemplate.boundHashOps(key);
        if (cartHash.hasKey(skuId.toString())){
            cart = JSON.parseObject(cartHash.get(skuId.toString()).toString(), Cart.class);
            cart.setCount(count + cart.getCount());
            cartHash.put(skuId.toString(), JSON.toJSONString(cart));
        }
        else {
            SkuInfoEntity skuInfoEntity = gmallPmsClient.querySkuInfoBySkuId(skuId).getData();
            if (skuInfoEntity == null){
                return;
            }
            cart.setChecked(true);
            cart.setTitle(skuInfoEntity.getSkuTitle());
            cart.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
            List<SaleVo> saleVos = gmallSmsClient.querySaleBySkuId(skuId).getData();
            cart.setPrice(skuInfoEntity.getPrice());
            cart.setSales(saleVos);
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = gmallPmsClient.querySkuAttrValuesBySkuId(skuId).getData();
            cart.setSkuAttrValue(skuSaleAttrValueEntities);
            List<WareSkuEntity> wareSkuEntities = gmallWmsClient.queryWareBySkuId(skuId).getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                boolean stock = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                cart.setStock(stock);
            }
            stringRedisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuInfoEntity.getPrice().toString());
        }
        cartHash.put(skuId.toString(), JSON.toJSONString(cart));
    }

    @Override
    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        List<Cart> userKeyCart = null;
        String userkey = KEY_PREFIX + userInfo.getUserKey();
        BoundHashOperations<String, Object, Object> unloginOps = stringRedisTemplate.boundHashOps(userkey);
        List<Object> unloginCartJsonList = unloginOps.values();
        if (!CollectionUtils.isEmpty(unloginCartJsonList)){
            userKeyCart = unloginCartJsonList.stream().map(unloginCartJson ->{
                Cart cart = JSON.parseObject(unloginCartJson.toString(), Cart.class);
                String priceKey = PRICE_PREFIX +cart.getSkuId();
                cart.setCurrenPrice(new BigDecimal(stringRedisTemplate.opsForValue().get(priceKey)));
                return cart;
            }).collect(Collectors.toList());
        }
        if (userInfo.getId() == null){
            return userKeyCart;
        }

        String key = KEY_PREFIX + userInfo.getId();
        BoundHashOperations<String, Object, Object> loginOps = stringRedisTemplate.boundHashOps(key);
        if (!CollectionUtils.isEmpty(userKeyCart)){
            userKeyCart.forEach(cart -> {
                Integer count = cart.getCount();
                Long skuId = cart.getSkuId();
                if (loginOps.hasKey(skuId.toString())){
                    String loginCartJson = loginOps.get(skuId.toString()).toString();
                    cart = JSON.parseObject(loginCartJson, Cart.class);
                    cart.setCount(count + cart.getCount());
                }
                loginOps.put(skuId.toString(), JSON.toJSONString(cart));
            });
            stringRedisTemplate.delete(userkey);
        }
        List<Object> loginCartJsonList = loginOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsonList)){
            return loginCartJsonList.stream().map(loginCartJson ->{
                Cart cart = JSON.parseObject(loginCartJson.toString(), Cart.class);
                String priceKey = PRICE_PREFIX +cart.getSkuId();
                cart.setCurrenPrice(new BigDecimal(stringRedisTemplate.opsForValue().get(priceKey)));
                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void updateCart(Cart cart) {
        String key = KEY_PREFIX;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getId() == null){
            key += userInfo.getUserKey();
        }
        else {
            key += userInfo.getId();
        }
        BoundHashOperations<String, Object, Object> cartOps = stringRedisTemplate.boundHashOps(key);
        Long skuId = cart.getSkuId();
        if (cartOps.hasKey(skuId.toString())){
            Integer count = cart.getCount();
            String cartJson = cartOps.get(skuId.toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            cartOps.put(skuId.toString(), JSON.toJSONString(cart));
        }
    }

    @Override
    public void deleteCart(Long skuId) {
        String key = KEY_PREFIX;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getId() == null){
            key += userInfo.getUserKey();
        }
        else {
            key += userInfo.getId();
        }
        BoundHashOperations<String, Object, Object> cartOps = stringRedisTemplate.boundHashOps(key);
        if (cartOps.hasKey(skuId.toString())){
            cartOps.delete(skuId.toString());
        }
    }

    @Override
    public List<Cart> queryCheckedCartsByUserId(Long userId) {
        BoundHashOperations<String, Object, Object> checkedOPS = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<Object> cartsJson = checkedOPS.values();
        List<Cart> cartList = cartsJson.stream().map(cartJson ->
                JSON.parseObject(cartJson.toString(), Cart.class)).collect(Collectors.toList());
        return cartList;
    }
}
