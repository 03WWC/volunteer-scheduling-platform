package com.volunteer.platform.dispatch.vo;

import java.time.LocalDateTime;
import java.util.List;

public class DispatchResultVO {

    private Long id;

    private Long activityId;

    private Long areaId;

    private Long positionId;

    private Integer requiredCount;

    private String reason;

    private String dispatchStatus;

    private Long createdBy;

    private LocalDateTime finishedTime;

    private List<DispatchRecommendationVO> recommendations;

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

    public List<DispatchRecommendationVO> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<DispatchRecommendationVO> recommendations) {
        this.recommendations = recommendations;
    }
}
