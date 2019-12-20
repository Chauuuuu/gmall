package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.exception.MemberException;
import com.atguigu.core.utils.PhonecodeUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${code.rabbitmq.exchange}")
    private String EXCHANGE_NAME;

    @Override
    public void sendMsg(String phoneNum){
        Map<String,String> phonecode = new HashMap<>();
        String type = "1";
        String code = UUID.randomUUID().toString().replace("-","").substring(0,6);
        phonecode.put("code", code);
        phonecode.put("phoneNum", phoneNum);
        phonecode.put("type", type);
        boolean flag = PhonecodeUtil.isMobile(phoneNum);
        if (!flag){
            throw new MemberException("手机号码有误");
        }
        int count = 0;
        String phoneCountKey = "phone:code:"+phoneNum+":"+type+":count";
        String phoneCodeKey = "phone:code:"+phoneNum+":"+type+":code";
        flag = stringRedisTemplate.hasKey(phoneCountKey);
        if(flag){
            String countStr = stringRedisTemplate.opsForValue().get(phoneCountKey);
            count = Integer.parseInt(countStr);
            if (count >= 3){
                throw new MemberException("手机获取验证码次数上限");
            }
        }
        flag = stringRedisTemplate.hasKey(phoneCodeKey);
        if (flag){
            throw new MemberException("验证码获取过于频繁，请稍后重试");
        }
        amqpTemplate.convertAndSend(EXCHANGE_NAME, "phonecode.1",phonecode);
    }

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public void register(MemberEntity memberEntity, String code) {
        String phoneNum = memberEntity.getMobile();
        String phoneCodeKey = "phone:code:"+phoneNum+":1:code";
        String phoneCode = stringRedisTemplate.opsForValue().get(phoneCodeKey);
        if (StringUtils.isBlank(phoneCode)){
            throw new MemberException("请发送手机验证码");
        }
        boolean flag = code.equals(phoneCode);
        if (!flag){
            throw new MemberException("验证码错误");
        }
        String salt = UUID.randomUUID().toString().substring(0,6);
        memberEntity.setSalt(salt);

        memberEntity.setPassword(DigestUtils.md5Hex(memberEntity.getPassword()+salt));
        memberEntity.setCreateTime(new Date());
        this.save(memberEntity);
        stringRedisTemplate.delete(phoneCodeKey);
    }

    @Override
    public MemberEntity queryUser(String username, String password) {
        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));
        if (memberEntity == null){
            throw new MemberException("用户名不存在");
        }
        String salt = memberEntity.getSalt();
        String passwordSalt = DigestUtils.md5Hex(password + salt);
        if (!passwordSalt.equals(memberEntity.getPassword())){
            throw new MemberException("密码错误");
        }
        return memberEntity;
    }

}