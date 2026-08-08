package com.volunteer.platform.ai.client.dto;

import java.math.BigDecimal;

public class AiShortageRiskDTO {

    private Long positionId;

    private Long areaId;

    private String positionName;

    private Integer shortageCount;

    private BigDecimal riskScore;

    private String riskLevel;

    private String reason;

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

    public Integer getShortageCount() {
        return shortageCount;
    }

    public void setShortageCount(Integer shortageCount) {
        this.shortageCount = shortageCount;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
