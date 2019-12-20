package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * 会员
 *
 * @author Chau
 * @email 188376728@qq.com
 * @date 2019-12-02 12:07:46
 */
public interface MemberService extends IService<MemberEntity> {

    PageVo queryPage(QueryCondition params);

    void register(MemberEntity memberEntity, String code);

    MemberEntity queryUser(String username, String password);

    void sendMsg(String phoneNum);
}

