package com.volunteer.platform.dispatch.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DispatchDTO {

    private Long id;

    private Long activityId;

    private Long areaId;

    private Long positionId;

    private Integer requiredCount;

    private String reason;

    private String dispatchStatus;

    private Long createdBy;

    private LocalDateTime finishedTime;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer radiusMeter;

    private List<Long> candidateUserIds;

    private List<DispatchRecommendationDTO> recommendations;

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

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Integer getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(Integer requiredCount) {
        this.requiredCount = requiredCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(String dispatchStatus) {
        this.dispatchStatus = dispatchStatus;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(LocalDateTime finishedTime) {
        this.finishedTime = finishedTime;
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

    public List<Long> getCandidateUserIds() {
        return candidateUserIds;
    }

    public void setCandidateUserIds(List<Long> candidateUserIds) {
        this.candidateUserIds = candidateUserIds;
    }

    public List<DispatchRecommendationDTO> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<DispatchRecommendationDTO> recommendations) {
        this.recommendations = recommendations;
    }
}
