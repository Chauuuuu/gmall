package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class WareListener {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private WareSkuDao wareSkuDao;
    private static final String KEY_PREFIX = "stock:lock";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS-UNLOCK-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlockListener(String orderToken){
        String lockJSON = stringRedisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (lockJSON!=null){
            List<SkuLockVo> skuLockVos = JSON.parseArray(lockJSON, SkuLockVo.class);
            skuLockVos.forEach(skuLockVo -> {
                wareSkuDao.unlockStock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            });
            stringRedisTemplate.delete(KEY_PREFIX + orderToken);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS-MINUS-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void stockMinusListener(String orderToken){
        String lockJSON = stringRedisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (lockJSON!=null){
            List<SkuLockVo> skuLockVos = JSON.parseArray(lockJSON, SkuLockVo.class);
            skuLockVos.forEach(skuLockVo -> {
                wareSkuDao.minusStock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            });
            stringRedisTemplate.delete(KEY_PREFIX + orderToken);
        }
    }
}
