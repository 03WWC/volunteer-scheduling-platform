package com.volunteer.platform.schedule.manager;

import com.volunteer.platform.schedule.manager.impl.RedisScheduleLockManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleLockManagerTest {

    @Test
    void locksScheduleGenerationByActivityId() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("lock:schedule:100", "LOCKED", Duration.ofMinutes(10))).thenReturn(true);
        ScheduleLockManager manager = new RedisScheduleLockManager(redisTemplate);

        boolean locked = manager.tryLock(100L);

        assertThat(locked).isTrue();
    }

    @Test
    void unlocksScheduleGenerationByActivityId() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ScheduleLockManager manager = new RedisScheduleLockManager(redisTemplate);

        manager.unlock(100L);

        verify(redisTemplate).delete("lock:schedule:100");
    }
}
