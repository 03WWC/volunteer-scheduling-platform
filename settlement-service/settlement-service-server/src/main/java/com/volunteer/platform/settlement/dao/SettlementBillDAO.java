package com.volunteer.platform.settlement.dao;

import com.volunteer.platform.settlement.entity.SettlementBillDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementBillDAO {

    int insert(SettlementBillDO billDO);

    SettlementBillDO selectById(Long id);

    List<SettlementBillDO> selectByUserId(@Param("userId") Long userId);

    int updateStatus(SettlementBillDO billDO);
}
