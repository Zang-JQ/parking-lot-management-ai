package com.jr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AaaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AaaApplication.class, args);
    }
}