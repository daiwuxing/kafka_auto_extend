package com.example.kafka.dynamic.annotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableKafkaDynamicConsumer
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
} 