package com.volunteer.platform.location.manager;

import java.math.BigDecimal;
import java.util.List;

public interface GeoLocationManager {

    void save(Long activityId, Long userId, BigDecimal longitude, BigDecimal latitude);

    List<Long> nearby(Long activityId, BigDecimal longitude, BigDecimal latitude, Integer radiusMeter);
}
