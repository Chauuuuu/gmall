package com.atguigu.gmall.phonecode.listener;

import com.atguigu.core.exception.MemberException;
import com.atguigu.gmall.phonecode.utils.SmsTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@EnableConfigurationProperties(SmsTemplate.class)
public class PhonecodeListener {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SmsTemplate smsTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "gmall-phonecode-queue", durable = "true"),
            exchange = @Exchange(value = "GMALL-PHONECODE-EXCHANGE", type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"phonecode.1", "phonecode.2"}
    ))
    public void sendMs(Map<String,String> phonecode) {
        String phoneNum = phonecode.get("phoneNum");
        String type = phonecode.get("type");
        String code = phonecode.get("code");
        String tplId = "TP1711063";
        String phoneCountKey = "phone:code:"+phoneNum+":"+type+":count";
        String phoneCodeKey = "phone:code:"+phoneNum+":"+type+":code";
        int count = 0;
        boolean flag = stringRedisTemplate.hasKey(phoneCountKey);
        if (flag){
            String countStr = stringRedisTemplate.opsForValue().get(phoneCountKey);
            count = Integer.parseInt(countStr);
        }
        flag = smsTemplate.sendMs(phoneNum, code, tplId);
        if (!flag){
            throw new MemberException("发送验证码失败");
        }
        stringRedisTemplate.opsForValue().set(phoneCodeKey, code, 15, TimeUnit.MINUTES);
        if (count == 0){
            stringRedisTemplate.opsForValue().set(phoneCountKey, "1", 1440, TimeUnit.MINUTES);
        }
        else {
            count++;
            stringRedisTemplate.opsForValue().increment(phoneCountKey);
        }
    }
}
