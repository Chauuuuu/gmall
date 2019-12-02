package com.atguigu.gmall.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GmallSmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(GmallSmsApplication.class, args);
	}

}
