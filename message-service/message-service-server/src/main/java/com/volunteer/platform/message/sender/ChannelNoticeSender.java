package com.volunteer.platform.message.sender;

import com.volunteer.platform.message.dto.SendNoticeDTO;

public interface ChannelNoticeSender {

    boolean supports(String sendChannel);

    void send(SendNoticeDTO dto);
}
