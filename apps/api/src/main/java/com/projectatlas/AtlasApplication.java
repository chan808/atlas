package com.projectatlas;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "Project Atlas")
@SpringBootApplication
public class AtlasApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtlasApplication.class, args);
	}

	@Bean
	Clock applicationClock() {
		return Clock.systemUTC();
	}

}
