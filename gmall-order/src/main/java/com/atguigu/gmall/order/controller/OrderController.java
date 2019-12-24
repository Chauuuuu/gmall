package com.atguigu.gmall.order.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.config.AlipayTemplate;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.PayAsyncVo;
import com.atguigu.gmall.order.vo.PayVo;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private AlipayTemplate alipayTemplate;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @PostMapping("seckill/{skuId}")
    public Resp<Object> seckill(@PathVariable("skuId")Long skuId) {
        RSemaphore semaphore = redissonClient.getSemaphore("semaphore:lock:" + skuId);
        semaphore.trySetPermits(1000);

        if (semaphore.tryAcquire()) {
            String countString = stringRedisTemplate.opsForValue().get("order:seckill:" + skuId);
            if (StringUtils.isEmpty(countString) || Integer.parseInt(countString) == 0) {
                return Resp.fail("秒杀结束");
            }
            int count = Integer.parseInt(countString);
            stringRedisTemplate.opsForValue().set("order:seckill:" + skuId, String.valueOf(--count));
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(skuId);
            skuLockVo.setCount(1);
            String orderToken = IdWorker.getIdStr();
            skuLockVo.setOrderToken(orderToken);
            amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "order.seckill", skuLockVo);
            RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("count:down:" + orderToken);
            countDownLatch.trySetCount(1);
            //减库存创建订单业务
            countDownLatch.countDown();

            semaphore.release();
            return Resp.ok("恭喜你，秒杀成功");
        }
        return Resp.ok("商品已卖完");
    }

    @GetMapping("seckill/{orderToken}")
    public Resp<Object> querySeckill(@PathVariable("orderToken")String orderToken) throws InterruptedException {
        RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("count:down:" + orderToken);
        countDownLatch.await();

        //查询订单

        return Resp.ok(null);
    }

    @PostMapping("pay/success")
    public Resp<Object> paySuccess(PayAsyncVo payAsyncVo){
        amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "order.pay", payAsyncVo.getOut_trade_no());
        return Resp.ok(null);
    }

    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVo submitVo){
        OrderEntity orderEntity = orderService.submit(submitVo);
        try {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setTotal_amount(orderEntity.getPayAmount() != null?orderEntity.getPayAmount().toString():"100");
            payVo.setBody("支付平台");
            payVo.setSubject("谷粒商城");
            String payForm = alipayTemplate.pay(payVo);
            System.out.println(payForm);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return Resp.ok(null);
    }

    @GetMapping("confirm")
    public Resp<OrderConfirmVo> confirm(){
        OrderConfirmVo confirmVo = orderService.confirm();
        return Resp.ok(confirmVo);
    }
}
