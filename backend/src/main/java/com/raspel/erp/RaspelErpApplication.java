package com.raspel.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableAsync
public class RaspelErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaspelErpApplication.class, args);
    }
}
