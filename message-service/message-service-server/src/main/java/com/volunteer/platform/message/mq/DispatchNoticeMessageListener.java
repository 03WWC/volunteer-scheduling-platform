package com.volunteer.platform.message.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.service.MessageService;
import com.volunteer.platform.mq.RabbitDomainEventNames;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DispatchNoticeMessageListener {

    private final MessageService messageService;

    private final ObjectMapper objectMapper;

    public DispatchNoticeMessageListener(MessageService messageService) {
        this(messageService, new ObjectMapper());
    }

    @Autowired
    public DispatchNoticeMessageListener(MessageService messageService, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitDomainEventNames.DISPATCH_NOTICE_QUEUE)
    public void consume(String message) {
        DispatchNoticeEvent event = readEvent(message);
        if (!RabbitDomainEventNames.DISPATCH_NOTICE_REQUESTED.equals(event.eventType())) {
            return;
        }
        DispatchNoticePayload payload = event.payload();
        SendNoticeDTO dto = new SendNoticeDTO();
        dto.setEventKey(event.eventKey());
        dto.setReceiverId(payload.receiverId());
        dto.setNoticeType(payload.noticeType());
        dto.setTitle(payload.title());
        dto.setContent(payload.content());
        dto.setSendChannel(payload.sendChannel());
        dto.setReceiverOpenid(payload.receiverOpenid());
        dto.setReceiverMobile(payload.receiverMobile());
        messageService.send(dto);
    }

    private DispatchNoticeEvent readEvent(String message) {
        try {
            return objectMapper.readValue(message, DispatchNoticeEvent.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "dispatch notice event is invalid");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DispatchNoticeEvent(String eventType, String eventKey, DispatchNoticePayload payload) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DispatchNoticePayload(Long receiverId, String noticeType, String title, String content,
                                         String sendChannel, String receiverOpenid, String receiverMobile) {
    }
}
