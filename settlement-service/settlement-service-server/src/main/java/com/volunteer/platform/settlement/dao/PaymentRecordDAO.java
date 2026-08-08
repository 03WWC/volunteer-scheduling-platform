package com.volunteer.platform.settlement.dao;

import com.volunteer.platform.settlement.entity.PaymentRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PaymentRecordDAO {

    int insert(PaymentRecordDO paymentRecordDO);

    List<PaymentRecordDO> selectByBillId(Long billId);
}
