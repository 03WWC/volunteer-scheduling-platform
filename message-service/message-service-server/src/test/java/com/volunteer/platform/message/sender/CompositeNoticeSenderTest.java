package com.volunteer.platform.message.sender;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeNoticeSenderTest {

    @Test
    void sendsNoticeToMultipleChannels() {
        RecordingChannelNoticeSender appSender = new RecordingChannelNoticeSender("APP");
        RecordingChannelNoticeSender wechatSender = new RecordingChannelNoticeSender("WECHAT");
        RecordingChannelNoticeSender smsSender = new RecordingChannelNoticeSender("SMS");
        CompositeNoticeSender sender = new CompositeNoticeSender(List.of(appSender, wechatSender, smsSender));
        SendNoticeDTO dto = createSendNoticeDTO();
        dto.setSendChannel("APP,WECHAT,SMS");

        sender.send(dto);

        assertThat(appSender.sentChannels).containsExactly("APP");
        assertThat(wechatSender.sentChannels).containsExactly("WECHAT");
        assertThat(smsSender.sentChannels).containsExactly("SMS");
    }

    @Test
    void sendThrowsBusinessExceptionWhenChannelUnsupported() {
        CompositeNoticeSender sender = new CompositeNoticeSender(List.of(new RecordingChannelNoticeSender("APP")));
        SendNoticeDTO dto = createSendNoticeDTO();
        dto.setSendChannel("EMAIL");

        assertThatThrownBy(() -> sender.send(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("notice channel is unsupported");
    }

    private SendNoticeDTO createSendNoticeDTO() {
        SendNoticeDTO dto = new SendNoticeDTO();
        dto.setReceiverId(100L);
        dto.setNoticeType("DISPATCH");
        dto.setTitle("Dispatch notice");
        dto.setContent("Please go to area A");
        dto.setSendChannel("APP");
        return dto;
    }

    private static class RecordingChannelNoticeSender implements ChannelNoticeSender {

        private final String channel;
        private final List<String> sentChannels = new ArrayList<>();

        private RecordingChannelNoticeSender(String channel) {
            this.channel = channel;
        }

        @Override
        public boolean supports(String sendChannel) {
            return channel.equals(sendChannel);
        }

        @Override
        public void send(SendNoticeDTO dto) {
            sentChannels.add(dto.getSendChannel());
        }
    }
}
