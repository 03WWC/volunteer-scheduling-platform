package com.volunteer.platform.message.sender;

import com.volunteer.platform.message.dto.SendNoticeDTO;

public interface NoticeSender {

    void send(SendNoticeDTO dto);
}
