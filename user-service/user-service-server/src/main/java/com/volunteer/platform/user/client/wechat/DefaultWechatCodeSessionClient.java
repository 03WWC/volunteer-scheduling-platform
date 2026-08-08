package com.volunteer.platform.user.client.wechat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.user.config.WechatProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class DefaultWechatCodeSessionClient implements WechatCodeSessionClient {

    private final WechatProperties wechatProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DefaultWechatCodeSessionClient(WechatProperties wechatProperties, ObjectMapper objectMapper) {
        this.wechatProperties = wechatProperties;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    @Override
    public WechatCodeSession exchange(String code) {
        String uri = UriComponentsBuilder.fromHttpUrl(wechatProperties.getCode2SessionUrl())
            .queryParam("appid", wechatProperties.getAppId())
            .queryParam("secret", wechatProperties.getAppSecret())
            .queryParam("js_code", code)
            .queryParam("grant_type", "authorization_code")
            .toUriString();
        try {
            String responseBody = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
            WechatCodeSession session = parseSession(responseBody);
            if (session == null) {
                throw new BusinessException(502, "wechat session response is empty");
            }
            if (session.getErrcode() != null && session.getErrcode() != 0) {
                throw new BusinessException(400, "wechat code exchange failed: " + session.getErrmsg());
            }
            if (session.getOpenid() == null || session.getOpenid().isBlank()) {
                throw new BusinessException(400, "wechat openid is empty");
            }
            return session;
        } catch (RestClientException exception) {
            throw new BusinessException(502, "wechat code exchange unavailable");
        }
    }

    private WechatCodeSession parseSession(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, WechatCodeSession.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(502, "wechat session response is invalid");
        }
    }
}
