package com.atguigu.gmall.order.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVo submitVo){
        orderService.submit(submitVo);
        return Resp.ok(null);
    }

    @GetMapping("confirm")
    public Resp<OrderConfirmVo> confirm(){
        OrderConfirmVo confirmVo = orderService.confirm();
        return Resp.ok(confirmVo);
    }
}