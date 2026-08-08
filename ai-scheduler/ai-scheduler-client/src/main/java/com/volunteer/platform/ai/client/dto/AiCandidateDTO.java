package com.volunteer.platform.ai.client.dto;

import java.math.BigDecimal;

public class AiCandidateDTO {

    private Long userId;

    private BigDecimal distanceMeter;

    private Boolean available;

    private BigDecimal historicalScore;

    private BigDecimal skillScore;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getDistanceMeter() {
        return distanceMeter;
    }

    public void setDistanceMeter(BigDecimal distanceMeter) {
        this.distanceMeter = distanceMeter;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public BigDecimal getHistoricalScore() {
        return historicalScore;
    }

    public void setHistoricalScore(BigDecimal historicalScore) {
        this.historicalScore = historicalScore;
    }

    public BigDecimal getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(BigDecimal skillScore) {
        this.skillScore = skillScore;
    }
}
