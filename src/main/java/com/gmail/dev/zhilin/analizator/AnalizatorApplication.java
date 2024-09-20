package com.gmail.dev.zhilin.analizator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AnalizatorApplication {
    
	public static void main(String[] args) {
		SpringApplication.run(AnalizatorApplication.class, args);
	}

}
