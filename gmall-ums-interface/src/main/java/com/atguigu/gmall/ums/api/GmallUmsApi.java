package com.atguigu.gmall.ums.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface GmallUmsApi {

    @GetMapping("ums/member/query")
    public Resp<MemberEntity> query(@RequestParam("username")String username, @RequestParam("password")String password);

    @GetMapping("ums/memberreceiveaddress/{memberId}")
    public Resp<List<MemberReceiveAddressEntity>> queryAddresses(@PathVariable("memberId") Long memberId);

    @GetMapping("ums/member/info/{id}")
    public Resp<MemberEntity> queryMemberById(@PathVariable("id") Long id);
}
