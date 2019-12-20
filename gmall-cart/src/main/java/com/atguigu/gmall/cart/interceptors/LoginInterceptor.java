package com.atguigu.gmall.cart.interceptors;

import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.cart.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@EnableConfigurationProperties(JwtProperties.class)
@Component
public class LoginInterceptor extends HandlerInterceptorAdapter{

    @Autowired
    private JwtProperties jwtProperties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfo userInfo = new UserInfo();

        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        String userkey = CookieUtils.getCookieValue(request, jwtProperties.getUserKeyName());
        if (StringUtils.isEmpty(userkey)){
            userkey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, jwtProperties.getUserKeyName(), userkey, jwtProperties.getExpire());
        }
        userInfo.setUserKey(userkey);

        if (!StringUtils.isEmpty(token)){
            Map<String, Object> tokenInfo = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            userInfo.setId(new Long(tokenInfo.get("id").toString()));
        }

        THREAD_LOCAL.set(userInfo);
        return super.preHandle(request, response, handler);
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
