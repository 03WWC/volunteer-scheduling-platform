package com.volunteer.platform.location;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.volunteer.platform.location.dao")
@EnableFeignClients(basePackages = "com.volunteer.platform")
@SpringBootApplication(scanBasePackages = "com.volunteer.platform")
public class LocationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationServiceApplication.class, args);
    }
}
