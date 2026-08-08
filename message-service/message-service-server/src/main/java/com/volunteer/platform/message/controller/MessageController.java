package com.volunteer.platform.message.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.message.client.dto.MessageNoticeDTO;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.service.MessageService;
import com.volunteer.platform.message.vo.MessageNoticeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/notices")
    public Result<MessageNoticeDTO> send(@RequestBody SendNoticeDTO dto) {
        return Result.success(toDTO(messageService.send(dto)));
    }

    @GetMapping("/receivers/{receiverId}/notices")
    public Result<List<MessageNoticeDTO>> listByReceiver(@PathVariable("receiverId") Long receiverId) {
        return Result.success(messageService.listByReceiver(receiverId).stream().map(this::toDTO).toList());
    }

    @PostMapping("/notices/{id}/read")
    public Result<MessageNoticeDTO> markRead(@PathVariable("id") Long id) {
        return Result.success(toDTO(messageService.markRead(id)));
    }

    private MessageNoticeDTO toDTO(MessageNoticeVO vo) {
        MessageNoticeDTO dto = new MessageNoticeDTO();
        dto.setId(vo.getId());
        dto.setReceiverId(vo.getReceiverId());
        dto.setNoticeType(vo.getNoticeType());
        dto.setTitle(vo.getTitle());
        dto.setContent(vo.getContent());
        dto.setSendChannel(vo.getSendChannel());
        dto.setSendStatus(vo.getSendStatus());
        dto.setSendTime(vo.getSendTime());
        dto.setIsRead(vo.getIsRead());
        return dto;
    }
}
