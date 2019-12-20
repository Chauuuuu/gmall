package com.atguigu.gmall.order.service;

import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.OrderSubmitVo;

public interface OrderService {
    OrderConfirmVo confirm();

    void submit(OrderSubmitVo submitVo);
}
