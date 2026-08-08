package com.volunteer.platform.activity.dao;

import com.volunteer.platform.activity.entity.ActivitySignupDO;
import com.volunteer.platform.activity.query.ActivitySignupQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivitySignupDAO {

    int insert(ActivitySignupDO signupDO);

    ActivitySignupDO selectExisting(@Param("activityId") Long activityId, @Param("userId") Long userId,
                                    @Param("positionId") Long positionId);

    ActivitySignupDO selectById(@Param("id") Long id);

    List<ActivitySignupDO> selectPage(ActivitySignupQuery query);

    List<ActivitySignupDO> selectByUserId(@Param("userId") Long userId);

    int updateStatus(@Param("id") Long id, @Param("signupStatus") String signupStatus);
}
