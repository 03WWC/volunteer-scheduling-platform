package com.volunteer.platform.location.dao;

import com.volunteer.platform.location.entity.UserRealtimeStatusDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface UserRealtimeStatusDAO {

    int upsert(UserRealtimeStatusDO statusDO);

    UserRealtimeStatusDO selectByUserId(@Param("activityId") Long activityId, @Param("userId") Long userId);

    List<UserRealtimeStatusDO> selectNearby(@Param("activityId") Long activityId,
                                            @Param("longitude") BigDecimal longitude,
                                            @Param("latitude") BigDecimal latitude,
                                            @Param("radiusMeter") Integer radiusMeter);
}
