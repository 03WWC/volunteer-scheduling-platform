package com.volunteer.platform.message.mq;

import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.service.MessageService;
import com.volunteer.platform.message.vo.MessageNoticeVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchNoticeMessageListenerTest {

    @Test
    void consumesDispatchNoticeEventAndSendsNotice() {
        RecordingMessageService messageService = new RecordingMessageService();
        DispatchNoticeMessageListener listener = new DispatchNoticeMessageListener(messageService);

        listener.consume(dispatchNoticeMessage());

        assertThat(messageService.sentNotices).hasSize(1);
        assertThat(messageService.sentNotices.get(0).getEventKey()).isEqualTo("dispatch-notice-1-101");
        assertThat(messageService.sentNotices.get(0).getReceiverId()).isEqualTo(101L);
        assertThat(messageService.sentNotices.get(0).getNoticeType()).isEqualTo("DISPATCH");
        assertThat(messageService.sentNotices.get(0).getTitle()).isEqualTo("调度通知");
        assertThat(messageService.sentNotices.get(0).getSendChannel()).isEqualTo("APP");
    }

    @Test
    void passesSameEventKeyWhenRabbitMqRedeliversMessage() {
        RecordingMessageService messageService = new RecordingMessageService();
        DispatchNoticeMessageListener listener = new DispatchNoticeMessageListener(messageService);

        listener.consume(dispatchNoticeMessage());
        listener.consume(dispatchNoticeMessage());

        assertThat(messageService.sentNotices).extracting(SendNoticeDTO::getEventKey)
            .containsExactly("dispatch-notice-1-101", "dispatch-notice-1-101");
    }

    private String dispatchNoticeMessage() {
        return """
            {
              "topic": "message-topic",
              "eventType": "DISPATCH_NOTICE_REQUESTED",
              "eventKey": "dispatch-notice-1-101",
              "payload": {
                "dispatchTaskId": 1,
                "activityId": 100,
                "positionId": 300,
                "receiverId": 101,
                "noticeType": "DISPATCH",
                "title": "调度通知",
                "content": "活动 100 岗位 300 需要支援，请及时处理。",
                "sendChannel": "APP"
              }
            }
            """;
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
            vo.setSendTime(LocalDateTime.now());
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
