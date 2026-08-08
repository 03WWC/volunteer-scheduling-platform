package com.volunteer.platform.schedule.client.dto;

import java.util.List;

public class ScheduleDTO {

    private Long planId;
    private Long activityId;
    private String planNo;
    private String planName;
    private String planStatus;
    private List<ScheduleAssignmentDTO> assignments;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getPlanNo() {
        return planNo;
    }

    public void setPlanNo(String planNo) {
        this.planNo = planNo;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
    }

    public List<ScheduleAssignmentDTO> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<ScheduleAssignmentDTO> assignments) {
        this.assignments = assignments;
    }
}
