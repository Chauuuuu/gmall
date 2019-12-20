package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    private Long skuId;
    private String title;
    private String defaultImage;
    private BigDecimal price;
    private Integer count;
    private boolean stock;
    private List<SkuSaleAttrValueEntity> skuAttrValue;
    private List<SaleVo> sales;
    private BigDecimal weight;
}
