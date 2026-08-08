package com.volunteer.platform.message.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.service.MessageService;
import com.volunteer.platform.mq.RabbitDomainEventNames;
import com.volunteer.platform.schedule.client.api.ScheduleClient;
import com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO;
import com.volunteer.platform.schedule.client.dto.ScheduleDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SchedulePublishedMessageListener {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MessageService messageService;

    private final ScheduleClient scheduleClient;

    private final ObjectMapper objectMapper;

    public SchedulePublishedMessageListener(MessageService messageService, ScheduleClient scheduleClient) {
        this(messageService, scheduleClient, new ObjectMapper());
    }

    @Autowired
    public SchedulePublishedMessageListener(MessageService messageService, ScheduleClient scheduleClient,
                                            ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.scheduleClient = scheduleClient;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitDomainEventNames.SCHEDULE_PUBLISHED_QUEUE)
    public void consume(String message) {
        SchedulePublishedEvent event = readEvent(message);
        if (!RabbitDomainEventNames.SCHEDULE_PUBLISHED.equals(event.eventType())) {
            return;
        }
        SchedulePublishedPayload payload = event.payload();
        ScheduleDTO scheduleDTO = getSchedule(payload.activityId());
        if (payload.planId() != null && !payload.planId().equals(scheduleDTO.getPlanId())) {
            return;
        }
        for (ScheduleAssignmentDTO assignmentDTO : safeAssignments(scheduleDTO)) {
            if (assignmentDTO.getUserId() == null) {
                continue;
            }
            messageService.send(toNoticeDTO(event, scheduleDTO, assignmentDTO));
        }
    }

    private SchedulePublishedEvent readEvent(String message) {
        try {
            return objectMapper.readValue(message, SchedulePublishedEvent.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "schedule published event is invalid");
        }
    }

    private ScheduleDTO getSchedule(Long activityId) {
        if (activityId == null) {
            throw new BusinessException(400, "schedule published activity id is required");
        }
        Result<ScheduleDTO> result = scheduleClient.getActivityDetail(activityId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(404, "published schedule detail not found");
        }
        return result.getData();
    }

    private List<ScheduleAssignmentDTO> safeAssignments(ScheduleDTO scheduleDTO) {
        return scheduleDTO.getAssignments() == null ? List.of() : scheduleDTO.getAssignments();
    }

    private SendNoticeDTO toNoticeDTO(SchedulePublishedEvent event, ScheduleDTO scheduleDTO,
                                      ScheduleAssignmentDTO assignmentDTO) {
        SendNoticeDTO dto = new SendNoticeDTO();
        dto.setEventKey(event.eventKey() + "-" + assignmentDTO.getId());
        dto.setReceiverId(assignmentDTO.getUserId());
        dto.setNoticeType("SCHEDULE");
        dto.setTitle("排班已发布");
        dto.setContent(buildContent(scheduleDTO, assignmentDTO));
        dto.setSendChannel("IN_APP");
        return dto;
    }

    private String buildContent(ScheduleDTO scheduleDTO, ScheduleAssignmentDTO assignmentDTO) {
        return "你有新的排班：" + nullToDefault(scheduleDTO.getPlanName(), "排班计划")
            + "，岗位 " + assignmentDTO.getPositionId()
            + "，时间 " + formatTime(assignmentDTO) + "。";
    }

    private String formatTime(ScheduleAssignmentDTO assignmentDTO) {
        if (assignmentDTO.getStartTime() == null || assignmentDTO.getEndTime() == null) {
            return "待确认";
        }
        return assignmentDTO.getStartTime().format(TIME_FORMATTER) + " - "
            + assignmentDTO.getEndTime().format(TIME_FORMATTER);
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SchedulePublishedEvent(String eventType, String eventKey, SchedulePublishedPayload payload) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SchedulePublishedPayload(Long planId, Long activityId) {
    }
}
