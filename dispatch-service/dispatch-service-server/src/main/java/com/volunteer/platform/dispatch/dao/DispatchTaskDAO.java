package com.volunteer.platform.dispatch.dao;

import com.volunteer.platform.dispatch.entity.DispatchTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DispatchTaskDAO {

    int insert(DispatchTaskDO dispatchTaskDO);

    DispatchTaskDO selectById(Long id);

    int updateStatus(DispatchTaskDO dispatchTaskDO);
}
