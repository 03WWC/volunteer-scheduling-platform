package com.volunteer.platform.settlement.service;

import com.volunteer.platform.settlement.dto.GenerateSettlementDTO;
import com.volunteer.platform.settlement.dto.PaySettlementDTO;
import com.volunteer.platform.settlement.vo.SettlementBillVO;

import java.util.List;

public interface SettlementService {

    SettlementBillVO generate(GenerateSettlementDTO dto);

    SettlementBillVO getDetail(Long id);

    List<SettlementBillVO> listByUser(Long userId);

    SettlementBillVO confirm(Long id);

    SettlementBillVO pay(PaySettlementDTO dto);
}
