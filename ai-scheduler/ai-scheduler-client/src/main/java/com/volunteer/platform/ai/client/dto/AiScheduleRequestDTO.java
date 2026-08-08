package com.volunteer.platform.ai.client.dto;

import java.util.List;

public class AiScheduleRequestDTO {

    private Long activityId;

    private Long positionId;

    private Integer requiredCount;

    private List<AiCandidateDTO> candidates;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
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

    public List<AiCandidateDTO> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<AiCandidateDTO> candidates) {
        this.candidates = candidates;
    }
}
