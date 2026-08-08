package com.volunteer.platform.activity.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PositionDO {

    private Long id;
    private Long activityId;
    private Long areaId;
    private String name;
    private Integer needCount;
    private String skillRequirement;
    private BigDecimal salary;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public String getSkillRequirement() {
        return skillRequirement;
    }

    public void setSkillRequirement(String skillRequirement) {
        this.skillRequirement = skillRequirement;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "PositionDO{"
            + "id=" + id
            + ", activityId=" + activityId
            + ", areaId=" + areaId
            + ", name='" + name + '\''
            + ", needCount=" + needCount
            + ", skillRequirement='" + skillRequirement + '\''
            + ", salary=" + salary
            + ", startTime=" + startTime
            + ", endTime=" + endTime
            + '}';
    }
}
