package com.atguigu.gmall.pms;

import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.dao.BrandDao;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Array;
import java.util.Arrays;

@SpringBootTest
class GmallPmsApplicationTests {

	@Autowired
	AttrDao attrDao;

	@Test
	void contextLoads() {
//		QueryWrapper<AttrEntity> wrapper = new QueryWrapper<>();
		UpdateWrapper<AttrEntity> wrapper = new UpdateWrapper<>(new AttrEntity(4l,null,1,0,"bbb","cccc",0,1l,225l,0));
		wrapper.set("icon",null);
		attrDao.update(new AttrEntity(null,"ananan",1,0,"bbb","cccc",0,1l,225l,0), wrapper);
//		attrDao.insert(new AttrEntity(null,"ananan",1,0,"bbb","cccc",0,1l,225l,0));
//		wrapper.ge("attr_id", 3);
//		wrapper.select("attr_id","attr_name","value_type");
//		wrapper.orderByDesc("attr_id");
//		wrapper.in("attr_id", 3,4,5,6,7,8,9,10);
//		IPage<AttrEntity> page = attrDao.selectPage(new Page<>(1, 4), wrapper);
//		page.getRecords().forEach(System.out::println);
//		attrDao.selectBatchIds(Arrays.asList(3,5,6,7,8,10)).forEach(System.out::println);
	}

}
