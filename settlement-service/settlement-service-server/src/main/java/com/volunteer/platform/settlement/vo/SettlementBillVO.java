package com.volunteer.platform.settlement.vo;

import java.math.BigDecimal;
import java.util.List;

public class SettlementBillVO {

    private Long id;
    private String billNo;
    private Long activityId;
    private Long userId;
    private Integer totalWorkMinutes;
    private BigDecimal baseAmount;
    private BigDecimal hourAmount;
    private BigDecimal rewardAmount;
    private BigDecimal deductAmount;
    private BigDecimal totalAmount;
    private String billStatus;
    private List<SettlementDetailVO> details;
    private List<PaymentRecordVO> payments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

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

    public Integer getTotalWorkMinutes() {
        return totalWorkMinutes;
    }

    public void setTotalWorkMinutes(Integer totalWorkMinutes) {
        this.totalWorkMinutes = totalWorkMinutes;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getHourAmount() {
        return hourAmount;
    }

    public void setHourAmount(BigDecimal hourAmount) {
        this.hourAmount = hourAmount;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getBillStatus() {
        return billStatus;
    }

    public void setBillStatus(String billStatus) {
        this.billStatus = billStatus;
    }

    public List<SettlementDetailVO> getDetails() {
        return details;
    }

    public void setDetails(List<SettlementDetailVO> details) {
        this.details = details;
    }

    public List<PaymentRecordVO> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentRecordVO> payments) {
        this.payments = payments;
    }
}
