package com.volunteer.platform.common.auth;

import com.volunteer.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-29T08:00:00Z");

    @Test
    void issuesAndParsesTokenSubject() {
        JwtTokenService service = new JwtTokenService(properties("test-secret"), fixedClock(NOW));
        JwtTokenService.AuthSubject subject = new JwtTokenService.AuthSubject(10L, "admin", "MANAGER");

        JwtTokenService.AuthToken token = service.issue(subject, Duration.ofHours(2));
        JwtTokenService.AuthSubject parsed = service.parse(token.getToken());

        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(2)));
        assertThat(parsed.getUserId()).isEqualTo(10L);
        assertThat(parsed.getUsername()).isEqualTo("admin");
        assertThat(parsed.getUserType()).isEqualTo("MANAGER");
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenService service = new JwtTokenService(properties("test-secret"), fixedClock(NOW));
        JwtTokenService.AuthToken token = service.issue(new JwtTokenService.AuthSubject(10L, "admin", "MANAGER"),
            Duration.ofHours(2));
        String tampered = token.getToken().substring(0, token.getToken().length() - 2) + "xx";

        assertThatThrownBy(() -> service.parse(tampered))
            .isInstanceOf(BusinessException.class)
            .hasMessage("invalid token");
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenService issuer = new JwtTokenService(properties("test-secret"), fixedClock(NOW));
        JwtTokenService parser = new JwtTokenService(properties("test-secret"), fixedClock(NOW.plus(Duration.ofHours(3))));
        JwtTokenService.AuthToken token = issuer.issue(new JwtTokenService.AuthSubject(10L, "admin", "MANAGER"),
            Duration.ofHours(2));

        assertThatThrownBy(() -> parser.parse(token.getToken()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("token expired");
    }

    private JwtProperties properties(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        return properties;
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneId.of("UTC"));
    }
}
