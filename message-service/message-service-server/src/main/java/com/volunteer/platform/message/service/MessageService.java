package com.volunteer.platform.message.service;

import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.vo.MessageNoticeVO;

import java.util.List;

public interface MessageService {

    MessageNoticeVO send(SendNoticeDTO dto);

    List<MessageNoticeVO> listByReceiver(Long receiverId);

    MessageNoticeVO markRead(Long id);
}
