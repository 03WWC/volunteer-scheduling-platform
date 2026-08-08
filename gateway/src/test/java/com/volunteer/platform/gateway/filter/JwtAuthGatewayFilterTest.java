package com.volunteer.platform.gateway.filter;

import com.volunteer.platform.common.auth.JwtProperties;
import com.volunteer.platform.common.auth.JwtTokenService;
import com.volunteer.platform.gateway.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthGatewayFilterTest {

    @Test
    void allowsConfiguredPublicPathWithoutToken() {
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        ServerWebExchange exchange = exchange("/auth/login", null);
        JwtAuthGatewayFilter filter = new JwtAuthGatewayFilter(properties(List.of("/auth/**")), tokenService());

        StepVerifier.create(filter.filter(exchange, chain(chainInvoked)))
            .verifyComplete();

        assertThat(chainInvoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectsProtectedPathWithoutBearerToken() {
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        ServerWebExchange exchange = exchange("/activity/page", null);
        JwtAuthGatewayFilter filter = new JwtAuthGatewayFilter(properties(List.of("/auth/**")), tokenService());

        StepVerifier.create(filter.filter(exchange, chain(chainInvoked)))
            .verifyComplete();

        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsProtectedPathWithInvalidBearerToken() {
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        ServerWebExchange exchange = exchange("/activity/page", "Bearer access-token");
        JwtAuthGatewayFilter filter = new JwtAuthGatewayFilter(properties(List.of("/auth/**")), tokenService());

        StepVerifier.create(filter.filter(exchange, chain(chainInvoked)))
            .verifyComplete();

        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsProtectedPathWithValidBearerTokenAndForwardsIdentityHeaders() {
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        String token = tokenService().issue(new JwtTokenService.AuthSubject(9L, "admin", "MANAGER"),
            java.time.Duration.ofHours(1)).getToken();
        ServerWebExchange exchange = exchange("/activity/page", "Bearer " + token);
        JwtAuthGatewayFilter filter = new JwtAuthGatewayFilter(properties(List.of("/auth/**")), tokenService());

        StepVerifier.create(filter.filter(exchange, nextExchange -> {
                chainInvoked.set(true);
                capturedExchange.set(nextExchange);
                return Mono.empty();
            }))
            .verifyComplete();

        assertThat(chainInvoked).isTrue();
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("9");
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Type")).isEqualTo("MANAGER");
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Name")).isEqualTo("admin");
    }

    private SecurityProperties properties(List<String> publicPaths) {
        SecurityProperties properties = new SecurityProperties();
        properties.setPublicPaths(publicPaths);
        return properties;
    }

    private JwtTokenService tokenService() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret");
        return new JwtTokenService(properties);
    }

    private ServerWebExchange exchange(String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return MockServerWebExchange.from(builder);
    }

    private GatewayFilterChain chain(AtomicBoolean invoked) {
        return exchange -> {
            invoked.set(true);
            return Mono.empty();
        };
    }
}
