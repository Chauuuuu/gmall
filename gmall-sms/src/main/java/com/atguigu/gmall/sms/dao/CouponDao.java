package com.atguigu.gmall.sms.dao;

import com.atguigu.gmall.sms.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author Chau
 * @email 188376728@qq.com
 * @date 2019-12-02 12:03:51
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
