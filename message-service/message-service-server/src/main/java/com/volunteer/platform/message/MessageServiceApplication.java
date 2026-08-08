package com.volunteer.platform.message;

import com.volunteer.platform.message.config.NoticeChannelProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.volunteer.platform.message.dao")
@EnableFeignClients(basePackages = "com.volunteer.platform")
@EnableConfigurationProperties(NoticeChannelProperties.class)
@SpringBootApplication(scanBasePackages = "com.volunteer.platform")
public class MessageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
