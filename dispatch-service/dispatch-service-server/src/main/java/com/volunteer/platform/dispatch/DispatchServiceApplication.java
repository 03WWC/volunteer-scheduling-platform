package com.volunteer.platform.dispatch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.volunteer.platform.dispatch.dao")
@EnableFeignClients(basePackages = "com.volunteer.platform")
@SpringBootApplication(scanBasePackages = "com.volunteer.platform")
public class DispatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchServiceApplication.class, args);
    }
}
