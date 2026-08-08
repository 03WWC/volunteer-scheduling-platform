package com.volunteer.platform.activity.dao;

import com.volunteer.platform.activity.entity.ActivityDO;
import com.volunteer.platform.activity.query.ActivityQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivityDAO {

    int insert(ActivityDO activityDO);

    int updateById(ActivityDO activityDO);

    ActivityDO selectById(@Param("id") Long id);

    List<ActivityDO> selectPage(@Param("query") ActivityQuery query);

    int deleteById(@Param("id") Long id);
}
