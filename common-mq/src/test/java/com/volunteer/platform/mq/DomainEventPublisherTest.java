package com.volunteer.platform.mq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DomainEventPublisherTest {

    @Test
    void publishesDomainEventIntoMqEventLog() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DomainEventPublisher publisher = new JdbcDomainEventPublisher(jdbcTemplate);

        publisher.publish("schedule-topic", "SCHEDULE_PUBLISHED", "schedule-1", Map.of("planId", 1L));

        verify(jdbcTemplate).update(contains("INSERT INTO mq_event_log"),
            eq("schedule-topic"), eq("schedule-1"), eq("SCHEDULE_PUBLISHED"), contains("\"planId\":1"),
            eq("NEW"));
    }

    @Test
    void publishesDomainEventToRabbitMq() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        DomainEventPublisher publisher = new RabbitDomainEventPublisher(rabbitTemplate,
            new JdbcDomainEventPublisher(mock(JdbcTemplate.class)));

        publisher.publish("schedule-topic", "SCHEDULE_PUBLISHED", "schedule-1", Map.of("planId", 1L));

        verify(rabbitTemplate).convertAndSend(eq("volunteer.domain.events"), eq("schedule-topic"),
            contains("\"eventType\":\"SCHEDULE_PUBLISHED\""));
    }

    @Test
    void fallsBackToMqEventLogWhenRabbitMqPublishFails() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doThrow(new IllegalStateException("rabbit offline")).when(rabbitTemplate)
            .convertAndSend(anyString(), anyString(), anyString());
        DomainEventPublisher publisher = new RabbitDomainEventPublisher(rabbitTemplate,
            new JdbcDomainEventPublisher(jdbcTemplate));

        publisher.publish("dispatch-topic", "DISPATCH_FINISHED", "dispatch-1", Map.of("dispatchTaskId", 1L));

        verify(jdbcTemplate).update(contains("INSERT INTO mq_event_log"),
            eq("dispatch-topic"), eq("dispatch-1"), eq("DISPATCH_FINISHED"), contains("\"dispatchTaskId\":1"),
            eq("NEW"));
    }
}
