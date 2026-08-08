package com.volunteer.platform.settlement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.volunteer.platform.settlement.dao")
@EnableFeignClients(basePackages = "com.volunteer.platform")
@SpringBootApplication(scanBasePackages = "com.volunteer.platform")
public class SettlementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementServiceApplication.class, args);
    }
}
