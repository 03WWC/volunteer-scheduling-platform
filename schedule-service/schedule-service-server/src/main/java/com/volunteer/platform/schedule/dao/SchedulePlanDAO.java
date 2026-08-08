package com.volunteer.platform.schedule.dao;

import com.volunteer.platform.schedule.entity.SchedulePlanDO;
import org.apache.ibatis.annotations.Param;

public interface SchedulePlanDAO {

    int insert(SchedulePlanDO planDO);

    int updateStatus(SchedulePlanDO planDO);

    SchedulePlanDO selectById(@Param("id") Long id);

    SchedulePlanDO selectLatestByActivityId(@Param("activityId") Long activityId);
}
