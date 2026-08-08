package com.volunteer.platform.user.client.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.user.config.WechatProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultWechatCodeSessionClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesWechatTextPlainJsonResponse() throws IOException {
        startServer("{\"openid\":\"wx-openid-001\",\"session_key\":\"session-key\"}");
        DefaultWechatCodeSessionClient client = new DefaultWechatCodeSessionClient(properties(), new ObjectMapper());

        WechatCodeSession session = client.exchange("login-code");

        assertThat(session.getOpenid()).isEqualTo("wx-openid-001");
        assertThat(session.getSessionKey()).isEqualTo("session-key");
    }

    @Test
    void keepsWechatErrorMessageWhenTextPlainJsonResponseContainsError() throws IOException {
        startServer("{\"errcode\":40029,\"errmsg\":\"invalid code\"}");
        DefaultWechatCodeSessionClient client = new DefaultWechatCodeSessionClient(properties(), new ObjectMapper());

        assertThatThrownBy(() -> client.exchange("bad-code"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("wechat code exchange failed: invalid code");
    }

    private void startServer(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sns/jscode2session", exchange -> {
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    private WechatProperties properties() {
        WechatProperties properties = new WechatProperties();
        properties.setAppId("appid");
        properties.setAppSecret("secret");
        properties.setCode2SessionUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/sns/jscode2session");
        return properties;
    }
}
