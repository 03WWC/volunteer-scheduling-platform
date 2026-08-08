package com.volunteer.platform.activity.entity;

public class AreaDO {

    private Long id;
    private Long activityId;
    private String name;
    private String gps;
    private String ownerName;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    @Override
    public String toString() {
        return "AreaDO{"
            + "id=" + id
            + ", activityId=" + activityId
            + ", name='" + name + '\''
            + ", gps='" + gps + '\''
            + ", ownerName='" + ownerName + '\''
            + '}';
    }
}
