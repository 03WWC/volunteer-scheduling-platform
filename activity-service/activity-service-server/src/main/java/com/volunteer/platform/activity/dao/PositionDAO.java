package com.volunteer.platform.activity.dao;

import com.volunteer.platform.activity.entity.PositionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PositionDAO {

    int insert(PositionDO positionDO);

    int updateById(PositionDO positionDO);

    PositionDO selectById(@Param("id") Long id);

    List<PositionDO> selectByActivityId(@Param("activityId") Long activityId);

    int deleteById(@Param("id") Long id);
}
