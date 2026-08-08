package com.volunteer.platform.user.dao;

import com.volunteer.platform.user.entity.UserAvailabilityDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserAvailabilityDAO {

    int insert(UserAvailabilityDO availabilityDO);

    int update(UserAvailabilityDO availabilityDO);

    UserAvailabilityDO selectById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    List<UserAvailabilityDO> selectByUserId(@Param("userId") Long userId);
}
