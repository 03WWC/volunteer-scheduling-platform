package com.volunteer.platform.schedule;

import com.volunteer.platform.mq.DomainEventPublisherConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@MapperScan("com.volunteer.platform.schedule.dao")
@EnableFeignClients(basePackages = "com.volunteer.platform")
@Import(DomainEventPublisherConfig.class)
public class ScheduleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScheduleServiceApplication.class, args);
    }
}
