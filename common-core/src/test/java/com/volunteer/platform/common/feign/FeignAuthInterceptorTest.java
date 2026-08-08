package com.volunteer.platform.common.feign;

import feign.Request;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAuthInterceptorTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void propagatesAuthorizationHeaderFromCurrentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        bindRequest(request);
        RequestTemplate template = new RequestTemplate();

        new FeignAuthInterceptor().apply(template);

        assertThat(template.headers()).containsKey("Authorization");
        assertThat(template.headers().get("Authorization")).containsExactly("Bearer access-token");
    }

    @Test
    void ignoresMissingAuthorizationHeader() {
        bindRequest(new MockHttpServletRequest());
        RequestTemplate template = new RequestTemplate();

        new FeignAuthInterceptor().apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void feignOptionsUseConfiguredTimeouts() {
        Request.Options options = new FeignConfig().options();

        assertThat(options.connectTimeoutMillis()).isEqualTo(3000);
        assertThat(options.readTimeoutMillis()).isEqualTo(10000);
        assertThat(options.isFollowRedirects()).isTrue();
    }

    private void bindRequest(HttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
