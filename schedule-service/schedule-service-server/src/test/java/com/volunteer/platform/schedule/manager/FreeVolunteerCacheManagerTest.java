package com.volunteer.platform.schedule.manager;

import com.volunteer.platform.schedule.manager.impl.RedisFreeVolunteerCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FreeVolunteerCacheManagerTest {

    @Test
    void cachesFreeVolunteerIdsByDate() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        FreeVolunteerCacheManager manager = new RedisFreeVolunteerCacheManager(redisTemplate);

        manager.cacheFreeVolunteerIds(LocalDate.of(2026, 8, 1), List.of(101L, 102L));

        verify(valueOperations).set(eq("volunteer:free:2026-08-01"), eq("[101,102]"), eq(Duration.ofHours(12)));
    }

    @Test
    void returnsCachedFreeVolunteerIdsByDate() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("volunteer:free:2026-08-01")).thenReturn("[101,102]");
        FreeVolunteerCacheManager manager = new RedisFreeVolunteerCacheManager(redisTemplate);

        List<Long> userIds = manager.listFreeVolunteerIds(LocalDate.of(2026, 8, 1));

        assertThat(userIds).containsExactly(101L, 102L);
    }

    @Test
    void returnsEmptyListWhenFreeVolunteerCacheMisses() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        FreeVolunteerCacheManager manager = new RedisFreeVolunteerCacheManager(redisTemplate);

        List<Long> userIds = manager.listFreeVolunteerIds(LocalDate.of(2026, 8, 1));

        assertThat(userIds).isEmpty();
    }
}
