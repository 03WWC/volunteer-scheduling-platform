package com.volunteer.platform.ai.client.dto;

import java.util.List;

public class AiPredictionResultDTO {

    private Long activityId;

    private List<AiShortageRiskDTO> shortageRisks;

    private List<AiVolunteerAttritionRiskDTO> attritionRisks;

    private List<AiAreaRiskDTO> areaRisks;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public List<AiShortageRiskDTO> getShortageRisks() {
        return shortageRisks;
    }

    public void setShortageRisks(List<AiShortageRiskDTO> shortageRisks) {
        this.shortageRisks = shortageRisks;
    }

    public List<AiVolunteerAttritionRiskDTO> getAttritionRisks() {
        return attritionRisks;
    }

    public void setAttritionRisks(List<AiVolunteerAttritionRiskDTO> attritionRisks) {
        this.attritionRisks = attritionRisks;
    }

    public List<AiAreaRiskDTO> getAreaRisks() {
        return areaRisks;
    }

    public void setAreaRisks(List<AiAreaRiskDTO> areaRisks) {
        this.areaRisks = areaRisks;
    }
}
