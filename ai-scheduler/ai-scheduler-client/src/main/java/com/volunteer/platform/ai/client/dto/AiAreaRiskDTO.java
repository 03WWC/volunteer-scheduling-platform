package com.volunteer.platform.ai.client.dto;

import java.math.BigDecimal;

public class AiAreaRiskDTO {

    private Long areaId;

    private String areaName;

    private Integer shortageCount;

    private BigDecimal riskScore;

    private String riskLevel;

    private String reason;

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
