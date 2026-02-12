package com.example.scoi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableRetry
public class ScoiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScoiApplication.class, args);
	}

}
