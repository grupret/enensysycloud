package com.gaian.service.enensyscloud.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.gaian.service.enensyscloud")
public class EnensyscloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnensyscloudApplication.class, args);
	}

}
