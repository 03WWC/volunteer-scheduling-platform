package com.volunteer.platform.ai.client.dto;

public class AiPositionRiskInputDTO {

    private Long positionId;

    private Long areaId;

    private String positionName;

    private Integer requiredCount;

    private Integer assignedCount;

    private Integer checkedInCount;

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public Integer getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(Integer requiredCount) {
        this.requiredCount = requiredCount;
    }

    public Integer getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(Integer assignedCount) {
        this.assignedCount = assignedCount;
    }

    public Integer getCheckedInCount() {
        return checkedInCount;
    }

    public void setCheckedInCount(Integer checkedInCount) {
        this.checkedInCount = checkedInCount;
    }
}
