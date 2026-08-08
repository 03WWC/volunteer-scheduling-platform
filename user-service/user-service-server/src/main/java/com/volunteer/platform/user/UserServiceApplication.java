package com.volunteer.platform.user;

import com.volunteer.platform.common.auth.JwtProperties;
import com.volunteer.platform.user.config.AdminAuthProperties;
import com.volunteer.platform.user.config.WechatProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.volunteer.platform.user.dao")
@EnableConfigurationProperties({JwtProperties.class, AdminAuthProperties.class, WechatProperties.class})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
