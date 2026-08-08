package com.volunteer.platform.settlement.service;

import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.location.client.api.LocationClient;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.client.dto.CheckinQrCodeDTO;
import com.volunteer.platform.location.client.dto.LocationDTO;
import com.volunteer.platform.location.client.dto.NearbyUserDTO;
import com.volunteer.platform.location.client.dto.UserRealtimeStatusDTO;
import com.volunteer.platform.settlement.dao.PaymentRecordDAO;
import com.volunteer.platform.settlement.dao.SettlementBillDAO;
import com.volunteer.platform.settlement.dao.SettlementDetailDAO;
import com.volunteer.platform.settlement.dto.GenerateSettlementDTO;
import com.volunteer.platform.settlement.dto.GenerateSettlementDetailDTO;
import com.volunteer.platform.settlement.dto.PaySettlementDTO;
import com.volunteer.platform.settlement.entity.PaymentRecordDO;
import com.volunteer.platform.settlement.entity.SettlementBillDO;
import com.volunteer.platform.settlement.entity.SettlementDetailDO;
import com.volunteer.platform.settlement.service.impl.SettlementServiceImpl;
import com.volunteer.platform.settlement.vo.SettlementBillVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementServiceImplTest {

    @Test
    void generatesSettlementBillWithCalculatedTotalAmount() {
        SettlementService service = new SettlementServiceImpl(new InMemorySettlementBillDAO(),
            new InMemorySettlementDetailDAO(), new InMemoryPaymentRecordDAO(), new StubLocationClient());

        SettlementBillVO billVO = service.generate(createGenerateDTO());

        assertThat(billVO.getId()).isEqualTo(1L);
        assertThat(billVO.getBillStatus()).isEqualTo("CREATED");
        assertThat(billVO.getTotalWorkMinutes()).isEqualTo(180);
        assertThat(billVO.getHourAmount()).isEqualByComparingTo("90.00");
        assertThat(billVO.getTotalAmount()).isEqualByComparingTo("115.00");
        assertThat(billVO.getDetails()).hasSize(2);
    }

    @Test
    void confirmsBillAndPaysBill() {
        SettlementService service = new SettlementServiceImpl(new InMemorySettlementBillDAO(),
            new InMemorySettlementDetailDAO(), new InMemoryPaymentRecordDAO(), new StubLocationClient());
        SettlementBillVO generated = service.generate(createGenerateDTO());

        SettlementBillVO confirmed = service.confirm(generated.getId());
        SettlementBillVO paid = service.pay(createPayDTO(generated.getId()));

        assertThat(confirmed.getBillStatus()).isEqualTo("CONFIRMED");
        assertThat(paid.getBillStatus()).isEqualTo("PAID");
        assertThat(paid.getPayments()).hasSize(1);
        assertThat(paid.getPayments().get(0).getPayStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void listsSettlementBillsByUser() {
        SettlementService service = new SettlementServiceImpl(new InMemorySettlementBillDAO(),
            new InMemorySettlementDetailDAO(), new InMemoryPaymentRecordDAO(), new StubLocationClient());
        service.generate(createGenerateDTO());
        GenerateSettlementDTO otherUserDTO = createGenerateDTO();
        otherUserDTO.setUserId(201L);
        service.generate(otherUserDTO);

        List<SettlementBillVO> billVOS = service.listByUser(200L);

        assertThat(billVOS).hasSize(1);
        assertThat(billVOS.get(0).getUserId()).isEqualTo(200L);
        assertThat(billVOS.get(0).getDetails()).hasSize(2);
    }

    @Test
    void getDetailThrowsBusinessExceptionWhenBillMissing() {
        SettlementService service = new SettlementServiceImpl(new InMemorySettlementBillDAO(),
            new InMemorySettlementDetailDAO(), new InMemoryPaymentRecordDAO(), new StubLocationClient());

        assertThatThrownBy(() -> service.getDetail(404L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("settlement bill not found");
    }

    @Test
    void generateThrowsBusinessExceptionWhenDetailMissing() {
        SettlementService service = new SettlementServiceImpl(new InMemorySettlementBillDAO(),
            new InMemorySettlementDetailDAO(), new InMemoryPaymentRecordDAO(), new StubLocationClient());
        GenerateSettlementDTO dto = createGenerateDTO();
        dto.setDetails(List.of());

        assertThatThrownBy(() -> service.generate(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("settlement detail is required");
    }

    @Test
    void generatesSettlementDetailsFromCheckinRecordsWhenDetailMissing() {
        StubLocationClient locationClient = new StubLocationClient();
        locationClient.checkins = List.of(
            createCheckin(1L, 101L, "CHECK_IN", LocalDateTime.of(2026, 7, 29, 9, 0)),
            createCheckin(1L, 101L, "CHECK_OUT", LocalDateTime.of(2026, 7, 29, 11, 30)));
        SettlementService service = new SettlementServiceImpl(new InMemorySettlementBillDAO(),
            new InMemorySettlementDetailDAO(), new InMemoryPaymentRecordDAO(), locationClient);
        GenerateSettlementDTO dto = createGenerateDTO();
        dto.setDetails(null);
        dto.setBaseAmount(BigDecimal.ZERO);
        dto.setRewardAmount(BigDecimal.ZERO);
        dto.setDeductAmount(BigDecimal.ZERO);
        dto.setHourlyRate(new BigDecimal("30.00"));

        SettlementBillVO billVO = service.generate(dto);

        assertThat(locationClient.checkinsCalled).isTrue();
        assertThat(billVO.getTotalWorkMinutes()).isEqualTo(150);
        assertThat(billVO.getHourAmount()).isEqualByComparingTo("75.00");
        assertThat(billVO.getTotalAmount()).isEqualByComparingTo("75.00");
        assertThat(billVO.getDetails()).hasSize(1);
        assertThat(billVO.getDetails().get(0).getAssignmentId()).isEqualTo(1L);
    }

    private GenerateSettlementDTO createGenerateDTO() {
        GenerateSettlementDTO dto = new GenerateSettlementDTO();
        dto.setActivityId(100L);
        dto.setUserId(200L);
        dto.setBaseAmount(new BigDecimal("30.00"));
        dto.setRewardAmount(new BigDecimal("5.00"));
        dto.setDeductAmount(new BigDecimal("10.00"));
        dto.setDetails(List.of(createDetailDTO(1L, 101L, 60, "30.00"),
            createDetailDTO(2L, 102L, 120, "60.00")));
        return dto;
    }

    private GenerateSettlementDetailDTO createDetailDTO(Long assignmentId, Long positionId, Integer workMinutes,
                                                        String amount) {
        GenerateSettlementDetailDTO dto = new GenerateSettlementDetailDTO();
        dto.setAssignmentId(assignmentId);
        dto.setPositionId(positionId);
        dto.setWorkMinutes(workMinutes);
        dto.setAmount(new BigDecimal(amount));
        dto.setRemark("normal");
        return dto;
    }

    private PaySettlementDTO createPayDTO(Long billId) {
        PaySettlementDTO dto = new PaySettlementDTO();
        dto.setBillId(billId);
        dto.setPayChannel("BANK");
        dto.setPayAmount(new BigDecimal("115.00"));
        return dto;
    }

    private CheckinDTO createCheckin(Long assignmentId, Long positionId, String checkinType, LocalDateTime checkinTime) {
        CheckinDTO dto = new CheckinDTO();
        dto.setActivityId(100L);
        dto.setUserId(200L);
        dto.setAssignmentId(assignmentId);
        dto.setPositionId(positionId);
        dto.setCheckinType(checkinType);
        dto.setCheckinStatus("NORMAL");
        dto.setCheckinTime(checkinTime);
        return dto;
    }

    private static class InMemorySettlementBillDAO implements SettlementBillDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<SettlementBillDO> bills = new ArrayList<>();

        @Override
        public int insert(SettlementBillDO billDO) {
            billDO.setId(idGenerator.getAndIncrement());
            bills.add(billDO);
            return 1;
        }

        @Override
        public SettlementBillDO selectById(Long id) {
            return bills.stream().filter(bill -> bill.getId().equals(id)).findFirst().orElse(null);
        }

        @Override
        public List<SettlementBillDO> selectByUserId(Long userId) {
            return bills.stream().filter(bill -> bill.getUserId().equals(userId)).toList();
        }

        @Override
        public int updateStatus(SettlementBillDO billDO) {
            SettlementBillDO oldBill = selectById(billDO.getId());
            if (oldBill == null) {
                return 0;
            }
            oldBill.setBillStatus(billDO.getBillStatus());
            return 1;
        }
    }

    private static class InMemorySettlementDetailDAO implements SettlementDetailDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<SettlementDetailDO> details = new ArrayList<>();

        @Override
        public int insert(SettlementDetailDO detailDO) {
            detailDO.setId(idGenerator.getAndIncrement());
            details.add(detailDO);
            return 1;
        }

        @Override
        public List<SettlementDetailDO> selectByBillId(Long billId) {
            return details.stream().filter(detail -> detail.getBillId().equals(billId)).toList();
        }
    }

    private static class InMemoryPaymentRecordDAO implements PaymentRecordDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<PaymentRecordDO> payments = new ArrayList<>();

        @Override
        public int insert(PaymentRecordDO paymentRecordDO) {
            paymentRecordDO.setId(idGenerator.getAndIncrement());
            payments.add(paymentRecordDO);
            return 1;
        }

        @Override
        public List<PaymentRecordDO> selectByBillId(Long billId) {
            return payments.stream().filter(payment -> payment.getBillId().equals(billId)).toList();
        }
    }

    private static class StubLocationClient implements LocationClient {

        private boolean checkinsCalled;
        private List<CheckinDTO> checkins = List.of();

        @Override
        public Result<UserRealtimeStatusDTO> report(LocationDTO locationDTO) {
            return Result.success();
        }

        @Override
        public Result<List<NearbyUserDTO>> nearby(Long activityId, BigDecimal longitude, BigDecimal latitude,
                                                  Integer radiusMeter) {
            return Result.success(List.of());
        }

        @Override
        public Result<UserRealtimeStatusDTO> getStatus(Long userId, Long activityId) {
            return Result.success();
        }

        @Override
        public Result<CheckinDTO> checkin(CheckinDTO checkinDTO) {
            return Result.success(checkinDTO);
        }

        @Override
        public Result<CheckinDTO> submitAsyncCheckin(CheckinDTO checkinDTO) {
            return Result.success(checkinDTO);
        }

        @Override
        public Result<CheckinQrCodeDTO> generateCheckinQrCode(CheckinDTO checkinDTO) {
            return Result.success();
        }

        @Override
        public Result<List<CheckinDTO>> listCheckins(Long activityId, Long userId) {
            checkinsCalled = true;
            return Result.success(checkins);
        }
    }
}
