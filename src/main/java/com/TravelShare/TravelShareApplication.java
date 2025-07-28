package com.TravelShare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TravelShareApplication {
	public static void main(String[] args) {
		SpringApplication.run(TravelShareApplication.class, args);
	}

}
