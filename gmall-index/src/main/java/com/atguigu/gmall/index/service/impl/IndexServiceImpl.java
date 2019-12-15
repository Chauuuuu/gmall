package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexServiceImpl implements IndexService{

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public List<CategoryEntity> queryLevel1Category() {
        List<CategoryEntity> categoryEntities = gmallPmsClient.getCategory(1, 0L).getData();
        return categoryEntities;
    }

    @Override
    @GmallCache(prefix = "index:category:",timeout = 7200,random = 100)
    public List<CategoryVo> queryAllCategory(Long pid) {
//        String categoryCache = redisTemplate.opsForValue().get(CATEGORY_CACHE_KEY_PREFIX + pid);
//        if (StringUtils.isNotBlank(categoryCache)){
//            return JSON.parseArray(categoryCache, CategoryVo.class);
//        }
//        RLock lock = redissonClient.getLock("lock" + pid);
//        lock.lock();
//
//        String categoryCache2 = redisTemplate.opsForValue().get(CATEGORY_CACHE_KEY_PREFIX + pid);
//        if (StringUtils.isNotBlank(categoryCache2)){
//            lock.unlock();
//            return JSON.parseArray(categoryCache2, CategoryVo.class);
//        }

        List<CategoryVo> categoryVos = gmallPmsClient.getSubCategory(pid).getData();
//        redisTemplate.opsForValue().set(CATEGORY_CACHE_KEY_PREFIX+pid, JSON.toJSONString(categoryVos),new Random().nextInt(7)+7, TimeUnit.DAYS);
//        lock.unlock();
        return categoryVos;
    }

    @Override
    public void testLock() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        String num = redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(num)){
            return;
        }
        int i = Integer.parseInt(num);
        redisTemplate.opsForValue().set("num", String.valueOf(++i));
        lock.unlock();
    }
}
