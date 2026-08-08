package com.volunteer.platform.schedule.dao;

import com.volunteer.platform.schedule.entity.ScheduleAssignmentDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScheduleAssignmentDAO {

    int insert(ScheduleAssignmentDO assignmentDO);

    int updateStatus(ScheduleAssignmentDO assignmentDO);

    ScheduleAssignmentDO selectById(@Param("id") Long id);

    List<ScheduleAssignmentDO> selectByPlanId(@Param("planId") Long planId);

    List<ScheduleAssignmentDO> selectByUserId(@Param("userId") Long userId);

    int countUserTimeConflict(ScheduleAssignmentDO assignmentDO);
}
