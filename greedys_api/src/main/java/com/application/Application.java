package com.application;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableTransactionManagement
@Slf4j
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		log.info("✅ ✅ ✅ --- APPLICATION SUCCESSFULLY STARTED --- ✅ ✅ ✅");
		log.info("🚀 Greedy's Restaurant API is now ready to accept requests!");
		log.info("📋 All setup completed successfully");
	}

	@Bean
	Path path() {
		return Paths.get(System.getProperty("java.io.tmpdir"));
	}
	
/*
	@Bean
	PasswordEncoder getEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }*/
}
