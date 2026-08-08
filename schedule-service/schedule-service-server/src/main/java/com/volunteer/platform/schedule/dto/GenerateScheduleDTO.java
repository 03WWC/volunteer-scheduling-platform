package com.volunteer.platform.schedule.dto;

import java.util.List;

public class GenerateScheduleDTO {

    private Long activityId;
    private String planName;
    private Long generatedBy;
    private List<ScheduleAssignmentDTO> assignments;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Long getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(Long generatedBy) {
        this.generatedBy = generatedBy;
    }

    public List<ScheduleAssignmentDTO> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<ScheduleAssignmentDTO> assignments) {
        this.assignments = assignments;
    }
}
