package com.volunteer.platform.message.client.api;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.message.client.dto.MessageNoticeDTO;
import com.volunteer.platform.message.client.dto.SendNoticeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "message-service")
public interface MessageClient {

    @PostMapping("/message/notices")
    Result<MessageNoticeDTO> send(@RequestBody SendNoticeDTO dto);

    @GetMapping("/message/receivers/{receiverId}/notices")
    Result<List<MessageNoticeDTO>> listByReceiver(@PathVariable("receiverId") Long receiverId);

    @PostMapping("/message/notices/{id}/read")
    Result<MessageNoticeDTO> markRead(@PathVariable("id") Long id);
}
