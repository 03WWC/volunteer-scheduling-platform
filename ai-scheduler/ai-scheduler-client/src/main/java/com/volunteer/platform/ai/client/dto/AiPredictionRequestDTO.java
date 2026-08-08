package com.volunteer.platform.ai.client.dto;

import java.util.List;

public class AiPredictionRequestDTO {

    private Long activityId;

    private List<AiPositionRiskInputDTO> positions;

    private List<AiVolunteerRiskInputDTO> volunteers;

    private List<AiAreaRiskInputDTO> areas;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public List<AiPositionRiskInputDTO> getPositions() {
        return positions;
    }

    public void setPositions(List<AiPositionRiskInputDTO> positions) {
        this.positions = positions;
    }

    public List<AiVolunteerRiskInputDTO> getVolunteers() {
        return volunteers;
    }

    public void setVolunteers(List<AiVolunteerRiskInputDTO> volunteers) {
        this.volunteers = volunteers;
    }

    public List<AiAreaRiskInputDTO> getAreas() {
        return areas;
    }

    public void setAreas(List<AiAreaRiskInputDTO> areas) {
        this.areas = areas;
    }
}
