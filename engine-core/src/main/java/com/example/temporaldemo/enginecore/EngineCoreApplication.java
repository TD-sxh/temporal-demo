package com.example.temporaldemo.enginecore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.temporaldemo")
public class EngineCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineCoreApplication.class, args);
    }
}
