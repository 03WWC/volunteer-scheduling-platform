package com.volunteer.platform.location.manager.impl;

import com.volunteer.platform.location.manager.GeoLocationManager;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RedisGeoLocationManager implements GeoLocationManager {

    private static final String GEO_KEY_PREFIX = "volunteer:location:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisGeoLocationManager(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void save(Long activityId, Long userId, BigDecimal longitude, BigDecimal latitude) {
        stringRedisTemplate.opsForGeo()
            .add(buildGeoKey(activityId), new Point(longitude.doubleValue(), latitude.doubleValue()),
                String.valueOf(userId));
    }

    @Override
    public List<Long> nearby(Long activityId, BigDecimal longitude, BigDecimal latitude, Integer radiusMeter) {
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
            .includeDistance()
            .sortAscending();
        var results = stringRedisTemplate.opsForGeo()
            .radius(buildGeoKey(activityId), new Circle(new Point(longitude.doubleValue(), latitude.doubleValue()),
                new Distance(radiusMeter / 1000.0D, Metrics.KILOMETERS)), args);
        if (results == null) {
            return List.of();
        }
        return results.getContent().stream()
            .map(result -> Long.valueOf(result.getContent().getName()))
            .toList();
    }

    private String buildGeoKey(Long activityId) {
        return GEO_KEY_PREFIX + activityId;
    }
}
