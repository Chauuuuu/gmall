package com.atguigu.gmall.phonecode;

import com.atguigu.gmall.phonecode.listener.PhonecodeListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class GmallPhonecodeApplicationTests {

	@Test
	void contextLoads() {
		PhonecodeListener phonecodeListener = new PhonecodeListener();
		Map<String,String> map = new HashMap<>();
		map.put("phoneNum", "13501817897");
		map.put("type", "1");
		map.put("code", "321123");
		phonecodeListener.sendMs(map);
	}

}
