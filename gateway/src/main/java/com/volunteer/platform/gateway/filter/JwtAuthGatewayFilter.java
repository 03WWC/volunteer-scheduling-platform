package com.volunteer.platform.gateway.filter;

import com.volunteer.platform.common.auth.JwtProperties;
import com.volunteer.platform.common.auth.JwtTokenService;
import com.volunteer.platform.gateway.config.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_TYPE_HEADER = "X-User-Type";
    private static final String USER_NAME_HEADER = "X-User-Name";
    private static final int AUTH_FILTER_ORDER = -100;

    private final SecurityProperties securityProperties;
    private final JwtTokenService jwtTokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    public JwtAuthGatewayFilter(SecurityProperties securityProperties, JwtProperties jwtProperties) {
        this.securityProperties = securityProperties;
        this.jwtTokenService = new JwtTokenService(jwtProperties);
    }

    public JwtAuthGatewayFilter(SecurityProperties securityProperties, JwtTokenService jwtTokenService) {
        this.securityProperties = securityProperties;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        String token = resolveBearerToken(exchange);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange);
        }
        try {
            JwtTokenService.AuthSubject subject = jwtTokenService.parse(token);
            ServerWebExchange authorizedExchange = exchange.mutate()
                .request(builder -> builder
                    .header(USER_ID_HEADER, String.valueOf(subject.getUserId()))
                    .header(USER_TYPE_HEADER, subject.getUserType())
                    .header(USER_NAME_HEADER, subject.getUsername()))
                .build();
            return chain.filter(authorizedExchange);
        } catch (RuntimeException ex) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return AUTH_FILTER_ORDER;
    }

    private boolean isPublicPath(String path) {
        return securityProperties.getPublicPaths().stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveBearerToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
