package com.volunteer.platform.message.mq;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.service.MessageService;
import com.volunteer.platform.message.vo.MessageNoticeVO;
import com.volunteer.platform.schedule.client.api.ScheduleClient;
import com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO;
import com.volunteer.platform.schedule.client.dto.ScheduleDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulePublishedMessageListenerTest {

    @Test
    void consumesSchedulePublishedEventAndSendsNoticeToAssignedUsers() {
        RecordingMessageService messageService = new RecordingMessageService();
        SchedulePublishedMessageListener listener = new SchedulePublishedMessageListener(messageService,
            new StubScheduleClient());

        listener.consume(schedulePublishedMessage());

        assertThat(messageService.sentNotices).hasSize(2);
        assertThat(messageService.sentNotices).extracting(SendNoticeDTO::getReceiverId)
            .containsExactly(101L, 102L);
        assertThat(messageService.sentNotices).extracting(SendNoticeDTO::getEventKey)
            .containsExactly("schedule-9001-1", "schedule-9001-2");
        assertThat(messageService.sentNotices).extracting(SendNoticeDTO::getNoticeType)
            .containsOnly("SCHEDULE");
        assertThat(messageService.sentNotices).extracting(SendNoticeDTO::getSendChannel)
            .containsOnly("IN_APP");
    }

    @Test
    void ignoresEventWhenPublishedPlanIsNotLatestSchedulePlan() {
        RecordingMessageService messageService = new RecordingMessageService();
        StubScheduleClient scheduleClient = new StubScheduleClient();
        scheduleClient.scheduleDTO.setPlanId(9002L);
        SchedulePublishedMessageListener listener = new SchedulePublishedMessageListener(messageService,
            scheduleClient);

        listener.consume(schedulePublishedMessage());

        assertThat(messageService.sentNotices).isEmpty();
    }

    private String schedulePublishedMessage() {
        return """
            {
              "topic": "schedule-topic",
              "eventType": "SCHEDULE_PUBLISHED",
              "eventKey": "schedule-9001",
              "payload": {
                "planId": 9001,
                "activityId": 100
              }
            }
            """;
    }

    private static class StubScheduleClient implements ScheduleClient {

        private final ScheduleDTO scheduleDTO = createScheduleDTO();

        @Override
        public Result<ScheduleDTO> getActivityDetail(Long activityId) {
            return Result.success(scheduleDTO);
        }

        @Override
        public Result<List<ScheduleAssignmentDTO>> listUserAssignments(Long userId) {
            return Result.success(List.of());
        }

        private static ScheduleDTO createScheduleDTO() {
            ScheduleDTO dto = new ScheduleDTO();
            dto.setPlanId(9001L);
            dto.setActivityId(100L);
            dto.setPlanName("周末志愿服务排班");
            dto.setPlanStatus("PUBLISHED");
            dto.setAssignments(List.of(createAssignment(1L, 101L), createAssignment(2L, 102L)));
            return dto;
        }

        private static ScheduleAssignmentDTO createAssignment(Long id, Long userId) {
            ScheduleAssignmentDTO dto = new ScheduleAssignmentDTO();
            dto.setId(id);
            dto.setAreaId(10L);
            dto.setPositionId(20L + id);
            dto.setUserId(userId);
            dto.setWorkDate(LocalDate.of(2026, 8, 1));
            dto.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
            dto.setEndTime(LocalDateTime.of(2026, 8, 1, 12, 0));
            dto.setAssignmentStatus("WAIT_CONFIRM");
            return dto;
        }
    }

    private static class RecordingMessageService implements MessageService {

        private final List<SendNoticeDTO> sentNotices = new ArrayList<>();

        @Override
        public MessageNoticeVO send(SendNoticeDTO dto) {
            sentNotices.add(dto);
            MessageNoticeVO vo = new MessageNoticeVO();
            vo.setId(1L);
            vo.setReceiverId(dto.getReceiverId());
            vo.setNoticeType(dto.getNoticeType());
            vo.setTitle(dto.getTitle());
            vo.setContent(dto.getContent());
            vo.setSendChannel(dto.getSendChannel());
            vo.setSendStatus("SUCCESS");
            vo.setIsRead(Boolean.FALSE);
            return vo;
        }

        @Override
        public List<MessageNoticeVO> listByReceiver(Long receiverId) {
            return List.of();
        }

        @Override
        public MessageNoticeVO markRead(Long id) {
            return null;
        }
    }
}
