package com.volunteer.platform.user.vo;

public class PublicOverviewVO {

    private Integer todayActivityCount;

    private Integer activeVolunteerCount;

    private Double positionSatisfactionRate;

    public Integer getTodayActivityCount() {
        return todayActivityCount;
    }

    public void setTodayActivityCount(Integer todayActivityCount) {
        this.todayActivityCount = todayActivityCount;
    }

    public Integer getActiveVolunteerCount() {
        return activeVolunteerCount;
    }

    public void setActiveVolunteerCount(Integer activeVolunteerCount) {
        this.activeVolunteerCount = activeVolunteerCount;
    }

    public Double getPositionSatisfactionRate() {
        return positionSatisfactionRate;
    }

    public void setPositionSatisfactionRate(Double positionSatisfactionRate) {
        this.positionSatisfactionRate = positionSatisfactionRate;
    }
}
