package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.BoundsVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OmsListener {

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private AmqpTemplate amqpTemplate;

    @RabbitListener(queues = {"ORDER-DEAD-QUEUE"})
    public void closeOrder(String orderToken){
        if (orderDao.closeOrder(orderToken) ==1 ){
            amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "stock.unlock", orderToken);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER-PAY-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.pay"}
    ))
    public void payOrder(String orderToken){
        if (orderDao.payOrder(orderToken) == 1){
            amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "stock.minus", orderToken);
            OrderEntity orderEntity = orderDao.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderToken));
            BoundsVo boundsVo = new BoundsVo();
            boundsVo.setUserId(orderEntity.getMemberId());
            boundsVo.setGrowth(orderEntity.getGrowth());
            boundsVo.setIntegration(orderEntity.getIntegration());
            amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE", "user.bounds", boundsVo);
        }
    }
}
