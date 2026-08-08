package com.volunteer.platform.message.sender;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CompositeNoticeSender implements NoticeSender {

    private final List<ChannelNoticeSender> channelNoticeSenders;

    public CompositeNoticeSender(List<ChannelNoticeSender> channelNoticeSenders) {
        this.channelNoticeSenders = channelNoticeSenders;
    }

    @Override
    public void send(SendNoticeDTO dto) {
        Arrays.stream(dto.getSendChannel().split(","))
            .map(String::trim)
            .filter(channel -> !channel.isBlank())
            .forEach(channel -> sendByChannel(dto, channel));
    }

    private void sendByChannel(SendNoticeDTO dto, String sendChannel) {
        ChannelNoticeSender sender = channelNoticeSenders.stream()
            .filter(channelNoticeSender -> channelNoticeSender.supports(sendChannel))
            .findFirst()
            .orElseThrow(() -> new BusinessException(400, "notice channel is unsupported"));
        sender.send(copyForChannel(dto, sendChannel));
    }

    private SendNoticeDTO copyForChannel(SendNoticeDTO dto, String sendChannel) {
        SendNoticeDTO copyDTO = new SendNoticeDTO();
        copyDTO.setReceiverId(dto.getReceiverId());
        copyDTO.setEventKey(dto.getEventKey());
        copyDTO.setNoticeType(dto.getNoticeType());
        copyDTO.setTitle(dto.getTitle());
        copyDTO.setContent(dto.getContent());
        copyDTO.setSendChannel(sendChannel);
        copyDTO.setReceiverOpenid(dto.getReceiverOpenid());
        copyDTO.setReceiverMobile(dto.getReceiverMobile());
        return copyDTO;
    }
}
