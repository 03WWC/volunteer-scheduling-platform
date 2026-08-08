package com.volunteer.platform.mq;

public interface DomainEventPublisher {

    void publish(String topic, String eventType, String eventKey, Object payload);
}
