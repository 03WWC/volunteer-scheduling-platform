package com.volunteer.platform.mq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class JdbcDomainEventPublisherIntegrationTest {

    private static final String EVENT_KEY = "integration-common-mq-950001";

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());

    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("UPDATE mq_event_log SET is_deleted = 1 WHERE event_key = ?", EVENT_KEY);
    }

    @Test
    void publishesEventIntoMysqlEventLog() {
        DomainEventPublisher publisher = new JdbcDomainEventPublisher(jdbcTemplate);

        publisher.publish("schedule-topic", "SCHEDULE_PUBLISHED", EVENT_KEY, Map.of("planId", 950001L));

        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM mq_event_log
            WHERE event_key = ?
              AND topic = 'schedule-topic'
              AND event_status = 'NEW'
              AND is_deleted = 0
            """, Integer.class, EVENT_KEY);
        assertThat(count).isEqualTo(1);
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:13306}/volunteer_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        dataSource.setUsername("root");
        dataSource.setPassword("change-me");
        return dataSource;
    }
}
