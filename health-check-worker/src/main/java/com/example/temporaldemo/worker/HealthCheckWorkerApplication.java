package com.example.temporaldemo.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.temporaldemo")
public class HealthCheckWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthCheckWorkerApplication.class, args);
    }
}
