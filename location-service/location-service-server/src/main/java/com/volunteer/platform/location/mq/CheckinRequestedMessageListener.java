package com.volunteer.platform.location.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.service.LocationService;
import com.volunteer.platform.mq.RabbitDomainEventNames;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckinRequestedMessageListener {

    private final LocationService locationService;

    private final ObjectMapper objectMapper;

    public CheckinRequestedMessageListener(LocationService locationService) {
        this(locationService, new ObjectMapper());
    }

    @Autowired
    public CheckinRequestedMessageListener(LocationService locationService, ObjectMapper objectMapper) {
        this.locationService = locationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitDomainEventNames.CHECKIN_REQUESTED_QUEUE)
    public void consume(String message) {
        CheckinRequestedEvent event = readEvent(message);
        if (!RabbitDomainEventNames.CHECKIN_REQUESTED.equals(event.eventType())) {
            return;
        }
        locationService.checkin(event.payload());
    }

    private CheckinRequestedEvent readEvent(String message) {
        try {
            return objectMapper.readValue(message, CheckinRequestedEvent.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "checkin requested event is invalid");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CheckinRequestedEvent(String eventType, CheckinDTO payload) {
    }
}
