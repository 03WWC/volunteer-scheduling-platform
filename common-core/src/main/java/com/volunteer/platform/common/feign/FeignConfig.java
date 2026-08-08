package com.volunteer.platform.common.feign;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class FeignConfig {

    @Bean
    public Request.Options options() {
        return new Request.Options(
            Duration.ofSeconds(3),
            Duration.ofSeconds(10),
            true
        );
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 2);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public FeignAuthInterceptor feignAuthInterceptor() {
        return new FeignAuthInterceptor();
    }
}
