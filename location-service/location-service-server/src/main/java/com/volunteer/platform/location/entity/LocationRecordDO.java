package com.volunteer.platform.location.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LocationRecordDO {

    private Long id;

    private Long activityId;

    private Long userId;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private LocalDateTime locationTime;

    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public LocalDateTime getLocationTime() {
        return locationTime;
    }

    public void setLocationTime(LocalDateTime locationTime) {
        this.locationTime = locationTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
