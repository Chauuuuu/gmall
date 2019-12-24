package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * 订单
 *
 * @author Chau
 * @email 188376728@qq.com
 * @date 2019-12-02 11:58:49
 */
public interface OrderService extends IService<OrderEntity> {

    PageVo queryPage(QueryCondition params);

    OrderEntity saveOrder(OrderSubmitVo submitVo);
}

