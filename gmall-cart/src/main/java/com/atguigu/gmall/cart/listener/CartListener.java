package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CartListener {

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String PRICE_PREFIX = "gmall:currentprice:";
    private static final String KEY_PREFIX = "gmall:cart:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-ITEM-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE",type = ExchangeTypes.TOPIC,ignoreDeclarationExceptions = "true"),
            key = "item.update"
    ))
    public void updateListener(Long spuId){
        List<SkuInfoEntity> skuInfoEntities = gmallPmsClient.querySkusBySpuId(spuId).getData();
        skuInfoEntities.forEach(skuInfoEntity -> {
            stringRedisTemplate.opsForValue().set(PRICE_PREFIX+skuInfoEntity.getSkuId(),skuInfoEntity.getPrice().toString());
        });
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-ORDER-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE",type = ExchangeTypes.TOPIC,ignoreDeclarationExceptions = "true"),
            key = "cart.delete"
    ))
    public void deleteListner(Map<String,Object> map){
        Long userId = (Long)map.get("userId");
        List<Object> skuIdsJson = (List<Object>)map.get("skuIds");
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<String> skuIdString = skuIdsJson.stream().map(skuId -> skuId.toString()).collect(Collectors.toList());
        hashOps.delete(skuIdString.toArray(new String[skuIdString.size()]));
    }
}
