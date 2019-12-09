package com.atguigu.gmall.search;

import com.atguigu.gmall.search.entity.Goods;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

@SpringBootTest
class GmallSearchApplicationTests {

	@Autowired
	private ElasticsearchRestTemplate restTemplate;

	@Test
	void contextLoads() {
		restTemplate.createIndex(Goods.class);
		restTemplate.putMapping(Goods.class);
	}

}
