package com.volunteer.platform.ai.client.dto;

import java.util.List;

public class AiScheduleResultDTO {

    private Long activityId;

    private Long positionId;

    private List<AiRecommendationDTO> recommendations;

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

    public List<AiRecommendationDTO> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<AiRecommendationDTO> recommendations) {
        this.recommendations = recommendations;
    }
}
