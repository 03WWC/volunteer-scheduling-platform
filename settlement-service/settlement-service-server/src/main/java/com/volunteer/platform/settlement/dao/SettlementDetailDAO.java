package com.volunteer.platform.settlement.dao;

import com.volunteer.platform.settlement.entity.SettlementDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SettlementDetailDAO {

    int insert(SettlementDetailDO detailDO);

    List<SettlementDetailDO> selectByBillId(Long billId);
}
