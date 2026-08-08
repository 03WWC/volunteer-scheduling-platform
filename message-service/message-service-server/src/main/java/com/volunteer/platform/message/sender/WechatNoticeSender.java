package com.volunteer.platform.message.sender;

import com.volunteer.platform.message.dto.SendNoticeDTO;
import com.volunteer.platform.message.config.NoticeChannelProperties;
import com.volunteer.platform.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class WechatNoticeSender implements ChannelNoticeSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(WechatNoticeSender.class);

    private final NoticeChannelProperties noticeChannelProperties;

    private final RestClient restClient;

    public WechatNoticeSender(NoticeChannelProperties noticeChannelProperties) {
        this.noticeChannelProperties = noticeChannelProperties;
        this.restClient = RestClient.create();
    }

    @Override
    public boolean supports(String sendChannel) {
        return "WECHAT".equalsIgnoreCase(sendChannel);
    }

    @Override
    public void send(SendNoticeDTO dto) {
        NoticeChannelProperties.Wechat wechat = noticeChannelProperties.getWechat();
        if (!wechat.isEnabled() || isBlank(wechat.getEndpoint()) || isBlank(dto.getReceiverOpenid())) {
            LOGGER.info("wechat notice skipped, receiverId={}, noticeType={}, title={}, enabled={}, hasOpenid={}",
                dto.getReceiverId(), dto.getNoticeType(), dto.getTitle(), wechat.isEnabled(),
                !isBlank(dto.getReceiverOpenid()));
            return;
        }
        try {
            restClient.post()
                .uri(wechat.getEndpoint())
                .body(Map.of(
                    "touser", dto.getReceiverOpenid(),
                    "templateId", blankToEmpty(wechat.getTemplateId()),
                    "page", blankToEmpty(wechat.getPage()),
                    "title", dto.getTitle(),
                    "content", dto.getContent(),
                    "noticeType", dto.getNoticeType()
                ))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException exception) {
            LOGGER.warn("wechat notice failed, receiverId={}, noticeType={}", dto.getReceiverId(), dto.getNoticeType());
            throw new BusinessException(502, "wechat notice send failed");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
