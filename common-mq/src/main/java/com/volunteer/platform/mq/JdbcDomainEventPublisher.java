package com.volunteer.platform.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.platform.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcDomainEventPublisher implements DomainEventPublisher {

    private static final String NEW_STATUS = "NEW";

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public JdbcDomainEventPublisher(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new ObjectMapper());
    }

    @Autowired
    public JdbcDomainEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, String eventType, String eventKey, Object payload) {
        jdbcTemplate.update("""
                INSERT INTO mq_event_log (
                  topic,
                  event_key,
                  event_type,
                  payload,
                  event_status
                ) VALUES (?, ?, ?, ?, ?)
                """,
            topic, eventKey, eventType, toJson(payload), NEW_STATUS);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "domain event payload serialize failed");
        }
    }
}
