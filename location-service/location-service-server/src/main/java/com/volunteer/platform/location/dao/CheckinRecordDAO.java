package com.volunteer.platform.location.dao;

import com.volunteer.platform.location.entity.CheckinRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CheckinRecordDAO {

    int insert(CheckinRecordDO checkinRecordDO);

    int countByAssignmentUserType(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId,
                                  @Param("checkinType") String checkinType);

    List<CheckinRecordDO> selectByActivityAndUser(@Param("activityId") Long activityId,
                                                  @Param("userId") Long userId);
}
