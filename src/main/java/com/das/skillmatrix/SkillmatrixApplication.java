package com.das.skillmatrix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkillmatrixApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkillmatrixApplication.class, args);
	}

}
