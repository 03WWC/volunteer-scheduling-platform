package com.volunteer.platform.schedule.manager;

public interface ScheduleLockManager {

    boolean tryLock(Long activityId);

    void unlock(Long activityId);
}
