package com.atguigu.gmall.cart.pojo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Cart {
    private Long skuId;
    private String title;
    private String defaultImage;
    private BigDecimal price;
    private BigDecimal currenPrice;
    private Integer count;
    private List<SkuSaleAttrValueEntity> skuAttrValue;
    private List<SaleVo> sales;
    private boolean checked;
    private boolean stock;
}
