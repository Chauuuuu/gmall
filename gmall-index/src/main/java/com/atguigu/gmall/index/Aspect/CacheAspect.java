package com.atguigu.gmall.index.Aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class CacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();
        int timeout = gmallCache.timeout();
        int random = gmallCache.random();
        Class<?> returnType = method.getReturnType();

        Object[] args = joinPoint.getArgs();
        String key = prefix + Arrays.asList(args).toString();
        result = cacheHit(key, returnType);
        if (result!=null){
            return result;
        }

        RLock lock = redissonClient.getLock("lock" + Arrays.asList(args).toString());
        lock.lock();
        result = cacheHit(key, returnType);
        if (result!=null){
            lock.unlock();
            return result;
        }
        result = joinPoint.proceed(args);
        redisTemplate.opsForValue().set(key, JSON.toJSONString(result),(int)(timeout+(Math.random()*random)), TimeUnit.MINUTES);
        lock.unlock();
        return result;
    }

    private Object cacheHit(String key,Class<?> returnType){
        String json = redisTemplate.opsForValue().get(key);

        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, returnType);
        }
        return null;
    }

}
