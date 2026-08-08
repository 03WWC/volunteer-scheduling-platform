package com.volunteer.platform.mq;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RabbitDomainEventConfig.class)
@ComponentScan(basePackageClasses = DomainEventPublisher.class)
public class DomainEventPublisherConfig {
}
