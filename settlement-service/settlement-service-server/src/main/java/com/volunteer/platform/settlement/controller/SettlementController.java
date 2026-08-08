package com.volunteer.platform.settlement.controller;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.settlement.client.dto.PaymentRecordDTO;
import com.volunteer.platform.settlement.client.dto.SettlementBillDTO;
import com.volunteer.platform.settlement.client.dto.SettlementDetailDTO;
import com.volunteer.platform.settlement.dto.GenerateSettlementDTO;
import com.volunteer.platform.settlement.dto.PaySettlementDTO;
import com.volunteer.platform.settlement.service.SettlementService;
import com.volunteer.platform.settlement.vo.PaymentRecordVO;
import com.volunteer.platform.settlement.vo.SettlementBillVO;
import com.volunteer.platform.settlement.vo.SettlementDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/generate")
    public Result<SettlementBillDTO> generate(@RequestBody GenerateSettlementDTO dto) {
        return Result.success(toBillDTO(settlementService.generate(dto)));
    }

    @GetMapping("/{id}/detail")
    public Result<SettlementBillDTO> getDetail(@PathVariable("id") Long id) {
        return Result.success(toBillDTO(settlementService.getDetail(id)));
    }

    @GetMapping("/users/{userId}/bills")
    public Result<List<SettlementBillDTO>> listByUser(@PathVariable("userId") Long userId) {
        return Result.success(settlementService.listByUser(userId).stream().map(this::toBillDTO).toList());
    }

    @PostMapping("/{id}/confirm")
    public Result<SettlementBillDTO> confirm(@PathVariable("id") Long id) {
        return Result.success(toBillDTO(settlementService.confirm(id)));
    }

    @PostMapping("/{id}/pay")
    public Result<SettlementBillDTO> pay(@PathVariable("id") Long id, @RequestBody PaySettlementDTO dto) {
        dto.setBillId(id);
        return Result.success(toBillDTO(settlementService.pay(dto)));
    }

    private SettlementBillDTO toBillDTO(SettlementBillVO billVO) {
        SettlementBillDTO dto = new SettlementBillDTO();
        dto.setId(billVO.getId());
        dto.setBillNo(billVO.getBillNo());
        dto.setActivityId(billVO.getActivityId());
        dto.setUserId(billVO.getUserId());
        dto.setTotalWorkMinutes(billVO.getTotalWorkMinutes());
        dto.setBaseAmount(billVO.getBaseAmount());
        dto.setHourAmount(billVO.getHourAmount());
        dto.setRewardAmount(billVO.getRewardAmount());
        dto.setDeductAmount(billVO.getDeductAmount());
        dto.setTotalAmount(billVO.getTotalAmount());
        dto.setBillStatus(billVO.getBillStatus());
        dto.setDetails(billVO.getDetails().stream().map(this::toDetailDTO).toList());
        dto.setPayments(billVO.getPayments().stream().map(this::toPaymentDTO).toList());
        return dto;
    }

    private SettlementDetailDTO toDetailDTO(SettlementDetailVO detailVO) {
        SettlementDetailDTO dto = new SettlementDetailDTO();
        dto.setId(detailVO.getId());
        dto.setAssignmentId(detailVO.getAssignmentId());
        dto.setPositionId(detailVO.getPositionId());
        dto.setWorkMinutes(detailVO.getWorkMinutes());
        dto.setAmount(detailVO.getAmount());
        dto.setRemark(detailVO.getRemark());
        return dto;
    }

    private PaymentRecordDTO toPaymentDTO(PaymentRecordVO paymentVO) {
        PaymentRecordDTO dto = new PaymentRecordDTO();
        dto.setId(paymentVO.getId());
        dto.setPayNo(paymentVO.getPayNo());
        dto.setPayChannel(paymentVO.getPayChannel());
        dto.setPayAmount(paymentVO.getPayAmount());
        dto.setPayStatus(paymentVO.getPayStatus());
        dto.setPayTime(paymentVO.getPayTime());
        return dto;
    }
}
