package com.volunteer.platform.schedule.manager.impl;

import com.volunteer.platform.schedule.manager.ScheduleLockManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisScheduleLockManager implements ScheduleLockManager {

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisScheduleLockManager(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(Long activityId) {
        Boolean locked = stringRedisTemplate.opsForValue()
            .setIfAbsent(buildKey(activityId), "LOCKED", LOCK_TTL);
        return Boolean.TRUE.equals(locked);
    }

    @Override
    public void unlock(Long activityId) {
        stringRedisTemplate.delete(buildKey(activityId));
    }

    private String buildKey(Long activityId) {
        return "lock:schedule:" + activityId;
    }
}
