package com.volunteer.platform.activity.dao;

import com.volunteer.platform.activity.entity.AreaDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AreaDAO {

    int insert(AreaDO areaDO);

    int updateById(AreaDO areaDO);

    AreaDO selectById(@Param("id") Long id);

    List<AreaDO> selectByActivityId(@Param("activityId") Long activityId);

    int deleteById(@Param("id") Long id);
}
