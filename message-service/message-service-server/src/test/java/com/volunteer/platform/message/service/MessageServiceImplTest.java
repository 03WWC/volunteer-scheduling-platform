package com.volunteer.platform.message.service;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.message.dao.MessageNoticeDAO;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.entity.MessageNoticeDO;
import com.volunteer.platform.message.sender.NoticeSender;
import com.volunteer.platform.message.service.impl.EmptyReceiverContactResolver;
import com.volunteer.platform.message.service.impl.MessageServiceImpl;
import com.volunteer.platform.message.service.impl.ReceiverContact;
import com.volunteer.platform.message.service.impl.ReceiverContactResolver;
import com.volunteer.platform.message.vo.MessageNoticeVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageServiceImplTest {

    @Test
    void sendsNoticeAndListsByReceiver() {
        RecordingNoticeSender noticeSender = new RecordingNoticeSender();
        MessageService service = new MessageServiceImpl(new InMemoryMessageNoticeDAO(), noticeSender);

        MessageNoticeVO noticeVO = service.send(createSendNoticeDTO());
        List<MessageNoticeVO> notices = service.listByReceiver(100L);

        assertThat(noticeVO.getId()).isEqualTo(1L);
        assertThat(noticeVO.getSendStatus()).isEqualTo("SUCCESS");
        assertThat(noticeVO.getIsRead()).isFalse();
        assertThat(noticeVO.getSendTime()).isNotNull();
        assertThat(noticeSender.sentNotices).hasSize(1);
        assertThat(notices).extracting(MessageNoticeVO::getTitle).containsExactly("调度通知");
    }

    @Test
    void marksNoticeAsRead() {
        MessageService service = new MessageServiceImpl(new InMemoryMessageNoticeDAO(), new RecordingNoticeSender());
        MessageNoticeVO noticeVO = service.send(createSendNoticeDTO());

        MessageNoticeVO readNoticeVO = service.markRead(noticeVO.getId());

        assertThat(readNoticeVO.getIsRead()).isTrue();
    }

    @Test
    void sendReturnsExistingNoticeWhenEventKeyAlreadyConsumed() {
        RecordingNoticeSender noticeSender = new RecordingNoticeSender();
        MessageService service = new MessageServiceImpl(new InMemoryMessageNoticeDAO(), noticeSender);
        SendNoticeDTO dto = createSendNoticeDTO();
        dto.setEventKey("dispatch-notice-1-100");

        MessageNoticeVO firstNoticeVO = service.send(dto);
        MessageNoticeVO secondNoticeVO = service.send(dto);

        assertThat(secondNoticeVO.getId()).isEqualTo(firstNoticeVO.getId());
        assertThat(noticeSender.sentNotices).hasSize(1);
    }

    @Test
    void sendEnrichesReceiverContactBeforeSending() {
        RecordingNoticeSender noticeSender = new RecordingNoticeSender();
        ReceiverContactResolver resolver = receiverId -> new ReceiverContact("wx-openid-100", "13800000100");
        MessageService service = new MessageServiceImpl(new InMemoryMessageNoticeDAO(), noticeSender, resolver);

        service.send(createSendNoticeDTO());

        SendNoticeDTO sentNotice = noticeSender.sentNotices.get(0);
        assertThat(sentNotice.getReceiverOpenid()).isEqualTo("wx-openid-100");
        assertThat(sentNotice.getReceiverMobile()).isEqualTo("13800000100");
    }

    @Test
    void sendKeepsExplicitReceiverContact() {
        RecordingNoticeSender noticeSender = new RecordingNoticeSender();
        MessageService service = new MessageServiceImpl(new InMemoryMessageNoticeDAO(), noticeSender,
            new EmptyReceiverContactResolver());
        SendNoticeDTO dto = createSendNoticeDTO();
        dto.setReceiverOpenid("wx-explicit");
        dto.setReceiverMobile("13900000100");

        service.send(dto);

        SendNoticeDTO sentNotice = noticeSender.sentNotices.get(0);
        assertThat(sentNotice.getReceiverOpenid()).isEqualTo("wx-explicit");
        assertThat(sentNotice.getReceiverMobile()).isEqualTo("13900000100");
    }

    @Test
    void sendThrowsBusinessExceptionWhenReceiverMissing() {
        MessageService service = new MessageServiceImpl(new InMemoryMessageNoticeDAO(), new RecordingNoticeSender());
        SendNoticeDTO dto = createSendNoticeDTO();
        dto.setReceiverId(null);

        assertThatThrownBy(() -> service.send(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("notice target is required");
    }

    private SendNoticeDTO createSendNoticeDTO() {
        SendNoticeDTO dto = new SendNoticeDTO();
        dto.setReceiverId(100L);
        dto.setNoticeType("DISPATCH");
        dto.setTitle("调度通知");
        dto.setContent("请前往 A 区支援");
        dto.setSendChannel("APP");
        return dto;
    }

    private static class InMemoryMessageNoticeDAO implements MessageNoticeDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<MessageNoticeDO> notices = new ArrayList<>();

        @Override
        public int insert(MessageNoticeDO noticeDO) {
            noticeDO.setId(idGenerator.getAndIncrement());
            notices.add(noticeDO);
            return 1;
        }

        @Override
        public MessageNoticeDO selectById(Long id) {
            return notices.stream()
                .filter(notice -> notice.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public MessageNoticeDO selectByEventKey(String eventKey) {
            return notices.stream()
                .filter(notice -> eventKey.equals(notice.getEventKey()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<MessageNoticeDO> selectByReceiverId(Long receiverId) {
            return notices.stream()
                .filter(notice -> notice.getReceiverId().equals(receiverId))
                .toList();
        }

        @Override
        public int updateRead(Long id) {
            MessageNoticeDO noticeDO = selectById(id);
            if (noticeDO == null) {
                return 0;
            }
            noticeDO.setIsRead(Boolean.TRUE);
            return 1;
        }
    }

    private static class RecordingNoticeSender implements NoticeSender {

        private final List<SendNoticeDTO> sentNotices = new ArrayList<>();

        @Override
        public void send(SendNoticeDTO dto) {
            sentNotices.add(dto);
        }
    }
}
