package com.atguigu.gmall.auth.service.impl;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class AuthServiceImpl implements AuthService{

    @Autowired
    private GmallUmsClient gmallUmsClient;
    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public String accredit(String username, String password) {
        MemberEntity memberEntity = gmallUmsClient.query(username, password).getData();
        if (memberEntity == null) {
            return null;
        }
        try {
            Map<String,Object> map = new HashMap<>();
            map.put("id", memberEntity.getId());
            map.put("nickname", memberEntity.getNickname());
            String token = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            return token;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
