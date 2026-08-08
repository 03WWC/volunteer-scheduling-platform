package com.volunteer.platform.ai.client.dto;

public class AiAreaRiskInputDTO {

    private Long areaId;

    private String areaName;

    private Integer requiredCount;

    private Integer assignedCount;

    private Integer checkedInCount;

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
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
