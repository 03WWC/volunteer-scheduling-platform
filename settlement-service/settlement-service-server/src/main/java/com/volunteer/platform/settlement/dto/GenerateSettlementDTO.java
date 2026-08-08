package com.volunteer.platform.settlement.dto;

import java.math.BigDecimal;
import java.util.List;

public class GenerateSettlementDTO {

    private Long activityId;
    private Long userId;
    private BigDecimal baseAmount;
    private BigDecimal hourlyRate;
    private BigDecimal rewardAmount;
    private BigDecimal deductAmount;
    private List<GenerateSettlementDetailDTO> details;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public BigDecimal getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(BigDecimal rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public BigDecimal getDeductAmount() {
        return deductAmount;
    }

    public void setDeductAmount(BigDecimal deductAmount) {
        this.deductAmount = deductAmount;
    }

    public List<GenerateSettlementDetailDTO> getDetails() {
        return details;
    }

    public void setDetails(List<GenerateSettlementDetailDTO> details) {
        this.details = details;
    }
}
