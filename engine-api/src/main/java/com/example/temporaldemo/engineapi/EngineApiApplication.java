package com.example.temporaldemo.engineapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.example.temporaldemo")
@EnableJpaRepositories(basePackages = "com.example.temporaldemo")
@EntityScan(basePackages = "com.example.temporaldemo")
public class EngineApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApiApplication.class, args);
    }
}
