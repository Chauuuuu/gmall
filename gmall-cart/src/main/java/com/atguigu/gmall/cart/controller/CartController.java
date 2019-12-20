package com.atguigu.gmall.cart.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.interceptors.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("cart")
@RestController
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("{userId}")
    public Resp<List<Cart>> queryCheckedCartsByUserId(@PathVariable("userId") Long userId){
        List<Cart> cartList = cartService.queryCheckedCartsByUserId(userId);
        return Resp.ok(cartList);
    }

    @PostMapping("delete/{skuId}")
    public Resp<Object> deleteCart(@PathVariable("skuId") Long skuId){
        cartService.deleteCart(skuId);
        return Resp.ok(null);
    }

    @PostMapping("update")
    public Resp<Object> updateCart(@RequestBody Cart cart){
        cartService.updateCart(cart);
        return Resp.ok(null);
    }

    @GetMapping("/query")
    public Resp<List<Cart>> queryCarts(){
        List<Cart> cartList = cartService.queryCarts();
        return Resp.ok(cartList);
    }

    @GetMapping("test")
    public Resp<Object> test(){
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        return Resp.ok(userInfo);
    }

    @PostMapping
    public Resp<Object> addCart(@RequestBody Cart cart){
        cartService.addCart(cart);
        return Resp.ok(null);
    }

}
