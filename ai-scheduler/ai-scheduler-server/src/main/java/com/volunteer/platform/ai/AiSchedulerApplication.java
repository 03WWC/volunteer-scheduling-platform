package com.volunteer.platform.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.volunteer.platform")
public class AiSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSchedulerApplication.class, args);
    }
}
