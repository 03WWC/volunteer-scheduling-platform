package com.volunteer.platform.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.platform.common.exception.BusinessException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Primary
@Component
@ConditionalOnBean(RabbitTemplate.class)
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private final JdbcDomainEventPublisher fallbackPublisher;

    private final ObjectMapper objectMapper;

    public RabbitDomainEventPublisher(RabbitTemplate rabbitTemplate, JdbcDomainEventPublisher fallbackPublisher) {
        this(rabbitTemplate, fallbackPublisher, new ObjectMapper());
    }

    @Autowired
    public RabbitDomainEventPublisher(RabbitTemplate rabbitTemplate, JdbcDomainEventPublisher fallbackPublisher,
                                      ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.fallbackPublisher = fallbackPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, String eventType, String eventKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(RabbitDomainEventNames.DOMAIN_EVENT_EXCHANGE, topic,
                toJson(topic, eventType, eventKey, payload));
        } catch (RuntimeException e) {
            fallbackPublisher.publish(topic, eventType, eventKey, payload);
        }
    }

    private String toJson(String topic, String eventType, String eventKey, Object payload) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("topic", topic);
        message.put("eventType", eventType);
        message.put("eventKey", eventKey);
        message.put("payload", payload);
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "rabbit event payload serialize failed");
        }
    }
}
