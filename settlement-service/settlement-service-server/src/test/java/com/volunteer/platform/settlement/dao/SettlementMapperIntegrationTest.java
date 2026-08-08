package com.volunteer.platform.settlement.dao;

import com.volunteer.platform.settlement.SettlementServiceApplication;
import com.volunteer.platform.settlement.entity.PaymentRecordDO;
import com.volunteer.platform.settlement.entity.SettlementBillDO;
import com.volunteer.platform.settlement.entity.SettlementDetailDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SettlementServiceApplication.class)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class SettlementMapperIntegrationTest {

    private static final Long TEST_ACTIVITY_ID = 940_001L;

    @Autowired
    private SettlementBillDAO settlementBillDAO;

    @Autowired
    private SettlementDetailDAO settlementDetailDAO;

    @Autowired
    private PaymentRecordDAO paymentRecordDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("""
            UPDATE payment_record pr
            JOIN settlement_bill sb ON pr.bill_id = sb.id
            SET pr.is_deleted = 1
            WHERE sb.activity_id = ?
            """, TEST_ACTIVITY_ID);
        jdbcTemplate.update("""
            UPDATE settlement_detail sd
            JOIN settlement_bill sb ON sd.bill_id = sb.id
            SET sd.is_deleted = 1
            WHERE sb.activity_id = ?
            """, TEST_ACTIVITY_ID);
        jdbcTemplate.update("UPDATE settlement_bill SET is_deleted = 1 WHERE activity_id = ?", TEST_ACTIVITY_ID);
    }

    @Test
    void insertsBillDetailAndPaymentThenUpdatesBillStatus() {
        SettlementBillDO billDO = createBill();

        assertThat(settlementBillDAO.insert(billDO)).isEqualTo(1);

        SettlementDetailDO detailDO = createDetail(billDO.getId());
        PaymentRecordDO paymentDO = createPayment(billDO.getId());

        assertThat(settlementDetailDAO.insert(detailDO)).isEqualTo(1);
        assertThat(paymentRecordDAO.insert(paymentDO)).isEqualTo(1);

        SettlementBillDO statusDO = new SettlementBillDO();
        statusDO.setId(billDO.getId());
        statusDO.setBillStatus("PAID");

        assertThat(settlementBillDAO.updateStatus(statusDO)).isEqualTo(1);
        assertThat(settlementBillDAO.selectById(billDO.getId()).getBillStatus()).isEqualTo("PAID");
        assertThat(settlementDetailDAO.selectByBillId(billDO.getId())).hasSize(1);
        assertThat(paymentRecordDAO.selectByBillId(billDO.getId())).hasSize(1);
    }

    private SettlementBillDO createBill() {
        SettlementBillDO billDO = new SettlementBillDO();
        billDO.setBillNo("BILL-INTEGRATION-940001");
        billDO.setActivityId(TEST_ACTIVITY_ID);
        billDO.setUserId(5001L);
        billDO.setTotalWorkMinutes(120);
        billDO.setBaseAmount(new BigDecimal("30.00"));
        billDO.setHourAmount(new BigDecimal("60.00"));
        billDO.setRewardAmount(new BigDecimal("5.00"));
        billDO.setDeductAmount(new BigDecimal("10.00"));
        billDO.setTotalAmount(new BigDecimal("85.00"));
        billDO.setBillStatus("CREATED");
        return billDO;
    }

    private SettlementDetailDO createDetail(Long billId) {
        SettlementDetailDO detailDO = new SettlementDetailDO();
        detailDO.setBillId(billId);
        detailDO.setAssignmentId(7001L);
        detailDO.setPositionId(8001L);
        detailDO.setWorkMinutes(120);
        detailDO.setAmount(new BigDecimal("60.00"));
        detailDO.setRemark("integration");
        return detailDO;
    }

    private PaymentRecordDO createPayment(Long billId) {
        PaymentRecordDO paymentDO = new PaymentRecordDO();
        paymentDO.setBillId(billId);
        paymentDO.setPayNo("PAY-INTEGRATION-940001");
        paymentDO.setPayChannel("BANK");
        paymentDO.setPayAmount(new BigDecimal("85.00"));
        paymentDO.setPayStatus("SUCCESS");
        paymentDO.setPayTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        return paymentDO;
    }
}
