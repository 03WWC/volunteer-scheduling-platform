package com.volunteer.platform.schedule.manager.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.schedule.manager.FreeVolunteerCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
public class RedisFreeVolunteerCacheManager implements FreeVolunteerCacheManager {

    private static final Duration CACHE_TTL = Duration.ofHours(12);

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public RedisFreeVolunteerCacheManager(StringRedisTemplate stringRedisTemplate) {
        this(stringRedisTemplate, new ObjectMapper());
    }

    @Autowired
    public RedisFreeVolunteerCacheManager(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void cacheFreeVolunteerIds(LocalDate date, List<Long> userIds) {
        stringRedisTemplate.opsForValue().set(buildKey(date), toJson(userIds), CACHE_TTL);
    }

    @Override
    public List<Long> listFreeVolunteerIds(LocalDate date) {
        String cachedValue = stringRedisTemplate.opsForValue().get(buildKey(date));
        if (cachedValue == null || cachedValue.isBlank()) {
            return Collections.emptyList();
        }
        return fromJson(cachedValue);
    }

    private String buildKey(LocalDate date) {
        return "volunteer:free:" + date;
    }

    private String toJson(List<Long> userIds) {
        try {
            return objectMapper.writeValueAsString(userIds == null ? Collections.emptyList() : userIds);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "free volunteer cache serialize failed");
        }
    }

    private List<Long> fromJson(String cachedValue) {
        try {
            return objectMapper.readValue(cachedValue, new TypeReference<List<Long>>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "free volunteer cache parse failed");
        }
    }
}
