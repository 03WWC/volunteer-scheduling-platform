package com.volunteer.platform.dispatch.dto;

import java.math.BigDecimal;

public class DetectShortageDTO {

    private Long activityId;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer radiusMeter;

    private Long createdBy;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
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

    public Integer getRadiusMeter() {
        return radiusMeter;
    }

    public void setRadiusMeter(Integer radiusMeter) {
        this.radiusMeter = radiusMeter;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
