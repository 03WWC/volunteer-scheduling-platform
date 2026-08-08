package com.volunteer.platform.message.sender;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.message.config.NoticeChannelProperties;
import com.volunteer.platform.message.dto.SendNoticeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class SmsNoticeSender implements ChannelNoticeSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsNoticeSender.class);

    private final NoticeChannelProperties noticeChannelProperties;

    private final RestClient restClient;

    public SmsNoticeSender(NoticeChannelProperties noticeChannelProperties) {
        this.noticeChannelProperties = noticeChannelProperties;
        this.restClient = RestClient.create();
    }

    @Override
    public boolean supports(String sendChannel) {
        return "SMS".equalsIgnoreCase(sendChannel);
    }

    @Override
    public void send(SendNoticeDTO dto) {
        NoticeChannelProperties.Sms sms = noticeChannelProperties.getSms();
        if (!sms.isEnabled() || isBlank(sms.getEndpoint()) || isBlank(dto.getReceiverMobile())) {
            LOGGER.info("sms notice skipped, receiverId={}, noticeType={}, title={}, enabled={}, hasMobile={}",
                dto.getReceiverId(), dto.getNoticeType(), dto.getTitle(), sms.isEnabled(),
                !isBlank(dto.getReceiverMobile()));
            return;
        }
        try {
            restClient.post()
                .uri(sms.getEndpoint())
                .body(Map.of(
                    "mobile", dto.getReceiverMobile(),
                    "signName", blankToEmpty(sms.getSignName()),
                    "templateCode", blankToEmpty(sms.getTemplateCode()),
                    "title", dto.getTitle(),
                    "content", dto.getContent(),
                    "noticeType", dto.getNoticeType()
                ))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException exception) {
            LOGGER.warn("sms notice failed, receiverId={}, noticeType={}", dto.getReceiverId(), dto.getNoticeType());
            throw new BusinessException(502, "sms notice send failed");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
