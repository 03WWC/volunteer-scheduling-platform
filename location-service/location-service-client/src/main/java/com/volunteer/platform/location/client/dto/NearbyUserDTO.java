package com.volunteer.platform.location.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NearbyUserDTO {

    private Long userId;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private BigDecimal distanceMeter;

    private String status;

    private LocalDateTime lastReportTime;

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

    public BigDecimal getDistanceMeter() {
        return distanceMeter;
    }

    public void setDistanceMeter(BigDecimal distanceMeter) {
        this.distanceMeter = distanceMeter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastReportTime() {
        return lastReportTime;
    }

    public void setLastReportTime(LocalDateTime lastReportTime) {
        this.lastReportTime = lastReportTime;
    }
}
