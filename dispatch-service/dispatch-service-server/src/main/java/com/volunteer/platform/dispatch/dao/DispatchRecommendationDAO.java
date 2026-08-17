package com.volunteer.platform.dispatch.dao;

import com.volunteer.platform.dispatch.entity.DispatchRecommendationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DispatchRecommendationDAO {

    int insert(DispatchRecommendationDO recommendationDO);

    List<DispatchRecommendationDO> selectByTaskId(Long dispatchTaskId);

    DispatchRecommendationDO selectById(Long id);

    int updateStatus(DispatchRecommendationDO recommendationDO);
}
