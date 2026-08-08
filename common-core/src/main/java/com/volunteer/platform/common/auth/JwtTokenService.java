package com.volunteer.platform.common.auth;

import com.volunteer.platform.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class JwtTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final JwtProperties jwtProperties;
    private final Clock clock;

    public JwtTokenService(JwtProperties jwtProperties) {
        this(jwtProperties, Clock.systemUTC());
    }

    public JwtTokenService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public AuthToken issue(AuthSubject subject, Duration ttl) {
        Instant expiresAt = Instant.now(clock).plus(ttl);
        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uid", subject.getUserId());
        payload.put("username", subject.getUsername());
        payload.put("userType", subject.getUserType());
        payload.put("exp", expiresAt.getEpochSecond());
        String body = encodeJson(payload);
        String unsigned = header + "." + body;
        return new AuthToken(unsigned + "." + sign(unsigned), expiresAt);
    }

    public AuthSubject parse(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "invalid token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(401, "invalid token");
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsigned), parts[2])) {
            throw new BusinessException(401, "invalid token");
        }
        Map<String, String> payload = parseJson(decode(parts[1]));
        long expiresAt = Long.parseLong(payload.getOrDefault("exp", "0"));
        if (Instant.now(clock).getEpochSecond() >= expiresAt) {
            throw new BusinessException(401, "token expired");
        }
        return new AuthSubject(Long.parseLong(payload.get("uid")), payload.get("username"), payload.get("userType"));
    }

    private String encodeJson(Map<String, ?> values) {
        StringBuilder builder = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (index++ > 0) {
                builder.append(',');
            }
            builder.append('"').append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number) {
                builder.append(value);
            } else {
                builder.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        builder.append('}');
        return encode(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        String content = json.substring(1, json.length() - 1);
        if (!StringUtils.hasText(content)) {
            return values;
        }
        for (String item : content.split(",")) {
            String[] pair = item.split(":", 2);
            String key = unquote(pair[0]);
            String value = unquote(pair[1]);
            values.put(key, value);
        }
        return values;
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return encode(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(500, "token sign failed");
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }

    private String encode(byte[] content) {
        return BASE64_URL_ENCODER.encodeToString(content);
    }

    private String decode(String content) {
        return new String(BASE64_URL_DECODER.decode(content), StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return trimmed;
    }

    public static class AuthToken {

        private final String token;
        private final Instant expiresAt;

        public AuthToken(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        public String getToken() {
            return token;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    public static class AuthSubject {

        private final Long userId;
        private final String username;
        private final String userType;

        public AuthSubject(Long userId, String username, String userType) {
            this.userId = userId;
            this.username = username;
            this.userType = userType;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getUserType() {
            return userType;
        }
    }
}
