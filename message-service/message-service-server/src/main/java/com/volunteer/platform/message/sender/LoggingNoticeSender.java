package com.volunteer.platform.message.sender;

import com.volunteer.platform.message.dto.SendNoticeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNoticeSender implements ChannelNoticeSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNoticeSender.class);

    @Override
    public boolean supports(String sendChannel) {
        return "APP".equalsIgnoreCase(sendChannel) || "IN_APP".equalsIgnoreCase(sendChannel);
    }

    @Override
    public void send(SendNoticeDTO dto) {
        LOGGER.info("app notice sent, receiverId={}, noticeType={}", dto.getReceiverId(), dto.getNoticeType());
    }
}
