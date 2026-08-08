package com.volunteer.platform.settlement.service.impl;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.location.client.api.LocationClient;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.settlement.dao.PaymentRecordDAO;
import com.volunteer.platform.settlement.dao.SettlementBillDAO;
import com.volunteer.platform.settlement.dao.SettlementDetailDAO;
import com.volunteer.platform.settlement.dto.GenerateSettlementDTO;
import com.volunteer.platform.settlement.dto.GenerateSettlementDetailDTO;
import com.volunteer.platform.settlement.dto.PaySettlementDTO;
import com.volunteer.platform.settlement.entity.PaymentRecordDO;
import com.volunteer.platform.settlement.entity.SettlementBillDO;
import com.volunteer.platform.settlement.entity.SettlementDetailDO;
import com.volunteer.platform.settlement.service.SettlementService;
import com.volunteer.platform.settlement.vo.PaymentRecordVO;
import com.volunteer.platform.settlement.vo.SettlementBillVO;
import com.volunteer.platform.settlement.vo.SettlementDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettlementServiceImpl implements SettlementService {

    private static final String CREATED_STATUS = "CREATED";
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String PAID_STATUS = "PAID";
    private static final String PAY_SUCCESS_STATUS = "SUCCESS";
    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SettlementBillDAO settlementBillDAO;
    private final SettlementDetailDAO settlementDetailDAO;
    private final PaymentRecordDAO paymentRecordDAO;
    private final LocationClient locationClient;

    public SettlementServiceImpl(SettlementBillDAO settlementBillDAO, SettlementDetailDAO settlementDetailDAO,
                                 PaymentRecordDAO paymentRecordDAO, LocationClient locationClient) {
        this.settlementBillDAO = settlementBillDAO;
        this.settlementDetailDAO = settlementDetailDAO;
        this.paymentRecordDAO = paymentRecordDAO;
        this.locationClient = locationClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementBillVO generate(GenerateSettlementDTO dto) {
        validateGenerateTarget(dto);
        List<GenerateSettlementDetailDTO> details = resolveDetails(dto);
        validateDetails(details);
        SettlementBillDO billDO = createBill(dto, details);
        settlementBillDAO.insert(billDO);
        for (GenerateSettlementDetailDTO detailDTO : details) {
            settlementDetailDAO.insert(toDetailDO(billDO.getId(), detailDTO));
        }
        return getDetail(billDO.getId());
    }

    @Override
    public SettlementBillVO getDetail(Long id) {
        SettlementBillDO billDO = settlementBillDAO.selectById(id);
        if (billDO == null) {
            throw new BusinessException(404, "settlement bill not found");
        }
        return toBillVO(billDO, settlementDetailDAO.selectByBillId(id), paymentRecordDAO.selectByBillId(id));
    }

    @Override
    public List<SettlementBillVO> listByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "user id is required");
        }
        return settlementBillDAO.selectByUserId(userId).stream()
            .map(billDO -> toBillVO(billDO, settlementDetailDAO.selectByBillId(billDO.getId()),
                paymentRecordDAO.selectByBillId(billDO.getId())))
            .toList();
    }

    @Override
    public SettlementBillVO confirm(Long id) {
        updateBillStatus(id, CONFIRMED_STATUS);
        return getDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementBillVO pay(PaySettlementDTO dto) {
        validatePay(dto);
        SettlementBillDO billDO = settlementBillDAO.selectById(dto.getBillId());
        if (billDO == null) {
            throw new BusinessException(404, "settlement bill not found");
        }
        PaymentRecordDO paymentRecordDO = new PaymentRecordDO();
        paymentRecordDO.setBillId(dto.getBillId());
        paymentRecordDO.setPayNo("PAY" + LocalDateTime.now().format(NO_TIME_FORMATTER));
        paymentRecordDO.setPayChannel(dto.getPayChannel());
        paymentRecordDO.setPayAmount(dto.getPayAmount());
        paymentRecordDO.setPayStatus(PAY_SUCCESS_STATUS);
        paymentRecordDO.setPayTime(LocalDateTime.now());
        paymentRecordDAO.insert(paymentRecordDO);
        updateBillStatus(dto.getBillId(), PAID_STATUS);
        return getDetail(dto.getBillId());
    }

    private void validateGenerateTarget(GenerateSettlementDTO dto) {
        if (dto == null || dto.getActivityId() == null || dto.getUserId() == null) {
            throw new BusinessException(400, "settlement target is required");
        }
    }

    private void validateDetails(List<GenerateSettlementDetailDTO> details) {
        if (details == null || details.isEmpty()) {
            throw new BusinessException(400, "settlement detail is required");
        }
    }

    private void validatePay(PaySettlementDTO dto) {
        if (dto == null || dto.getBillId() == null || dto.getPayChannel() == null || dto.getPayAmount() == null) {
            throw new BusinessException(400, "payment info is required");
        }
    }

    private SettlementBillDO createBill(GenerateSettlementDTO dto, List<GenerateSettlementDetailDTO> details) {
        BigDecimal baseAmount = amountOrZero(dto.getBaseAmount());
        BigDecimal hourAmount = details.stream()
            .map(detail -> amountOrZero(detail.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rewardAmount = amountOrZero(dto.getRewardAmount());
        BigDecimal deductAmount = amountOrZero(dto.getDeductAmount());
        SettlementBillDO billDO = new SettlementBillDO();
        billDO.setBillNo("BILL" + LocalDateTime.now().format(NO_TIME_FORMATTER));
        billDO.setActivityId(dto.getActivityId());
        billDO.setUserId(dto.getUserId());
        billDO.setTotalWorkMinutes(details.stream()
            .mapToInt(detail -> detail.getWorkMinutes() == null ? 0 : detail.getWorkMinutes())
            .sum());
        billDO.setBaseAmount(baseAmount);
        billDO.setHourAmount(hourAmount);
        billDO.setRewardAmount(rewardAmount);
        billDO.setDeductAmount(deductAmount);
        billDO.setTotalAmount(baseAmount.add(hourAmount).add(rewardAmount).subtract(deductAmount));
        billDO.setBillStatus(CREATED_STATUS);
        return billDO;
    }

    private List<GenerateSettlementDetailDTO> resolveDetails(GenerateSettlementDTO dto) {
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            return dto.getDetails();
        }
        Result<List<CheckinDTO>> checkinResult = locationClient.listCheckins(dto.getActivityId(), dto.getUserId());
        if (checkinResult == null || !checkinResult.isSuccess() || checkinResult.getData() == null) {
            return List.of();
        }
        return buildDetailsFromCheckins(checkinResult.getData(), dto.getHourlyRate());
    }

    private List<GenerateSettlementDetailDTO> buildDetailsFromCheckins(List<CheckinDTO> checkins,
                                                                       BigDecimal hourlyRate) {
        Map<String, CheckinDTO> startCheckins = new LinkedHashMap<>();
        Map<String, GenerateSettlementDetailDTO> details = new LinkedHashMap<>();
        checkins.stream()
            .filter(checkin -> checkin.getCheckinTime() != null && "NORMAL".equals(checkin.getCheckinStatus()))
            .sorted(Comparator.comparing(CheckinDTO::getCheckinTime))
            .forEach(checkin -> collectCheckin(checkin, hourlyRate, startCheckins, details));
        return new ArrayList<>(details.values());
    }

    private void collectCheckin(CheckinDTO checkin, BigDecimal hourlyRate, Map<String, CheckinDTO> startCheckins,
                                Map<String, GenerateSettlementDetailDTO> details) {
        String key = checkin.getAssignmentId() + ":" + checkin.getPositionId();
        if (isStartCheckin(checkin.getCheckinType())) {
            startCheckins.putIfAbsent(key, checkin);
            return;
        }
        if (!isEndCheckin(checkin.getCheckinType()) || !startCheckins.containsKey(key)) {
            return;
        }
        CheckinDTO startCheckin = startCheckins.remove(key);
        long minutes = Duration.between(startCheckin.getCheckinTime(), checkin.getCheckinTime()).toMinutes();
        if (minutes <= 0) {
            return;
        }
        GenerateSettlementDetailDTO detailDTO = details.computeIfAbsent(key,
            ignored -> createAutoDetail(checkin.getAssignmentId(), checkin.getPositionId()));
        int workMinutes = detailDTO.getWorkMinutes() == null ? 0 : detailDTO.getWorkMinutes();
        detailDTO.setWorkMinutes(workMinutes + Math.toIntExact(minutes));
        detailDTO.setAmount(amountOrZero(detailDTO.getAmount()).add(calculateHourAmount(minutes, hourlyRate)));
    }

    private GenerateSettlementDetailDTO createAutoDetail(Long assignmentId, Long positionId) {
        GenerateSettlementDetailDTO detailDTO = new GenerateSettlementDetailDTO();
        detailDTO.setAssignmentId(assignmentId);
        detailDTO.setPositionId(positionId);
        detailDTO.setWorkMinutes(0);
        detailDTO.setAmount(BigDecimal.ZERO);
        detailDTO.setRemark("auto generated from checkin records");
        return detailDTO;
    }

    private boolean isStartCheckin(String checkinType) {
        return "CHECK_IN".equals(checkinType) || "CHECKIN".equals(checkinType) || "IN".equals(checkinType)
            || "START".equals(checkinType);
    }

    private boolean isEndCheckin(String checkinType) {
        return "CHECK_OUT".equals(checkinType) || "CHECKOUT".equals(checkinType) || "OUT".equals(checkinType)
            || "END".equals(checkinType);
    }

    private BigDecimal calculateHourAmount(long minutes, BigDecimal hourlyRate) {
        if (hourlyRate == null) {
            return BigDecimal.ZERO;
        }
        return hourlyRate.multiply(BigDecimal.valueOf(minutes)).divide(BigDecimal.valueOf(60L), 2,
            RoundingMode.HALF_UP);
    }

    private SettlementDetailDO toDetailDO(Long billId, GenerateSettlementDetailDTO dto) {
        SettlementDetailDO detailDO = new SettlementDetailDO();
        detailDO.setBillId(billId);
        detailDO.setAssignmentId(dto.getAssignmentId());
        detailDO.setPositionId(dto.getPositionId());
        detailDO.setWorkMinutes(dto.getWorkMinutes() == null ? 0 : dto.getWorkMinutes());
        detailDO.setAmount(amountOrZero(dto.getAmount()));
        detailDO.setRemark(dto.getRemark());
        return detailDO;
    }

    private void updateBillStatus(Long id, String billStatus) {
        SettlementBillDO billDO = settlementBillDAO.selectById(id);
        if (billDO == null) {
            throw new BusinessException(404, "settlement bill not found");
        }
        SettlementBillDO statusDO = new SettlementBillDO();
        statusDO.setId(id);
        statusDO.setBillStatus(billStatus);
        settlementBillDAO.updateStatus(statusDO);
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private SettlementBillVO toBillVO(SettlementBillDO billDO, List<SettlementDetailDO> details,
                                      List<PaymentRecordDO> payments) {
        SettlementBillVO billVO = new SettlementBillVO();
        billVO.setId(billDO.getId());
        billVO.setBillNo(billDO.getBillNo());
        billVO.setActivityId(billDO.getActivityId());
        billVO.setUserId(billDO.getUserId());
        billVO.setTotalWorkMinutes(billDO.getTotalWorkMinutes());
        billVO.setBaseAmount(billDO.getBaseAmount());
        billVO.setHourAmount(billDO.getHourAmount());
        billVO.setRewardAmount(billDO.getRewardAmount());
        billVO.setDeductAmount(billDO.getDeductAmount());
        billVO.setTotalAmount(billDO.getTotalAmount());
        billVO.setBillStatus(billDO.getBillStatus());
        billVO.setDetails(details.stream().map(this::toDetailVO).toList());
        billVO.setPayments(payments.stream().map(this::toPaymentVO).toList());
        return billVO;
    }

    private SettlementDetailVO toDetailVO(SettlementDetailDO detailDO) {
        SettlementDetailVO detailVO = new SettlementDetailVO();
        detailVO.setId(detailDO.getId());
        detailVO.setAssignmentId(detailDO.getAssignmentId());
        detailVO.setPositionId(detailDO.getPositionId());
        detailVO.setWorkMinutes(detailDO.getWorkMinutes());
        detailVO.setAmount(detailDO.getAmount());
        detailVO.setRemark(detailDO.getRemark());
        return detailVO;
    }

    private PaymentRecordVO toPaymentVO(PaymentRecordDO paymentDO) {
        PaymentRecordVO paymentVO = new PaymentRecordVO();
        paymentVO.setId(paymentDO.getId());
        paymentVO.setPayNo(paymentDO.getPayNo());
        paymentVO.setPayChannel(paymentDO.getPayChannel());
        paymentVO.setPayAmount(paymentDO.getPayAmount());
        paymentVO.setPayStatus(paymentDO.getPayStatus());
        paymentVO.setPayTime(paymentDO.getPayTime());
        return paymentVO;
    }
}
