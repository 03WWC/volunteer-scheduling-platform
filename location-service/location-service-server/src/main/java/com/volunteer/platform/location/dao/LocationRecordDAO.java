package com.volunteer.platform.location.dao;

import com.volunteer.platform.location.entity.LocationRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LocationRecordDAO {

    int insert(LocationRecordDO locationRecordDO);
}
