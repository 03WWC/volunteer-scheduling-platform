package com.volunteer.platform.dispatch.vo;

import java.math.BigDecimal;

public class DispatchRecommendationVO {

    private Long id;

    private Long userId;

    private BigDecimal distanceMeter;

    private BigDecimal matchScore;

    private String recommendStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(BigDecimal matchScore) {
        this.matchScore = matchScore;
    }

    public String getRecommendStatus() {
        return recommendStatus;
    }

    public void setRecommendStatus(String recommendStatus) {
        this.recommendStatus = recommendStatus;
    }
}
