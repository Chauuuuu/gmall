package com.atguigu.gmall.oms.dao;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author Chau
 * @email 188376728@qq.com
 * @date 2019-12-02 11:58:49
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
