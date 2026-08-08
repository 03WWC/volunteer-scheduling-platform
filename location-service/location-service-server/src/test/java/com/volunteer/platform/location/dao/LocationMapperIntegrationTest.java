package com.volunteer.platform.location.dao;

import com.volunteer.platform.location.LocationServiceApplication;
import com.volunteer.platform.location.entity.CheckinRecordDO;
import com.volunteer.platform.location.entity.LocationRecordDO;
import com.volunteer.platform.location.entity.UserRealtimeStatusDO;
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

@SpringBootTest(classes = LocationServiceApplication.class)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class LocationMapperIntegrationTest {

    private static final Long TEST_ACTIVITY_ID = 930_001L;

    @Autowired
    private LocationRecordDAO locationRecordDAO;

    @Autowired
    private UserRealtimeStatusDAO userRealtimeStatusDAO;

    @Autowired
    private CheckinRecordDAO checkinRecordDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("UPDATE checkin_record SET is_deleted = 1 WHERE activity_id = ?", TEST_ACTIVITY_ID);
        jdbcTemplate.update("UPDATE location_record SET is_deleted = 1 WHERE activity_id = ?", TEST_ACTIVITY_ID);
        jdbcTemplate.update("UPDATE user_realtime_status SET is_deleted = 1 WHERE activity_id = ?", TEST_ACTIVITY_ID);
    }

    @Test
    void insertsLocationUpsertsRealtimeStatusAndSelectsNearbyUsers() {
        LocationRecordDO recordDO = createLocationRecord(5001L);

        assertThat(locationRecordDAO.insert(recordDO)).isEqualTo(1);
        assertThat(recordDO.getId()).isNotNull();

        UserRealtimeStatusDO statusDO = createRealtimeStatus(5001L, new BigDecimal("113.0000000"),
            new BigDecimal("23.0000000"));
        assertThat(userRealtimeStatusDAO.upsert(statusDO)).isGreaterThanOrEqualTo(1);
        assertThat(userRealtimeStatusDAO.selectByUserId(TEST_ACTIVITY_ID, 5001L).getStatus()).isEqualTo("ONLINE");
        assertThat(userRealtimeStatusDAO.selectNearby(TEST_ACTIVITY_ID, new BigDecimal("113.0000000"),
            new BigDecimal("23.0000000"), 1000)).hasSize(1);

        CheckinRecordDO checkinRecordDO = createCheckinRecord();
        assertThat(checkinRecordDAO.insert(checkinRecordDO)).isEqualTo(1);
        assertThat(checkinRecordDO.getId()).isNotNull();
    }

    private LocationRecordDO createLocationRecord(Long userId) {
        LocationRecordDO recordDO = new LocationRecordDO();
        recordDO.setActivityId(TEST_ACTIVITY_ID);
        recordDO.setUserId(userId);
        recordDO.setLongitude(new BigDecimal("113.0000000"));
        recordDO.setLatitude(new BigDecimal("23.0000000"));
        recordDO.setLocationTime(LocalDateTime.of(2026, 8, 1, 8, 0));
        recordDO.setStatus("ONLINE");
        return recordDO;
    }

    private UserRealtimeStatusDO createRealtimeStatus(Long userId, BigDecimal longitude, BigDecimal latitude) {
        UserRealtimeStatusDO statusDO = new UserRealtimeStatusDO();
        statusDO.setActivityId(TEST_ACTIVITY_ID);
        statusDO.setUserId(userId);
        statusDO.setStatus("ONLINE");
        statusDO.setLongitude(longitude);
        statusDO.setLatitude(latitude);
        statusDO.setLastReportTime(LocalDateTime.of(2026, 8, 1, 8, 0));
        return statusDO;
    }

    private CheckinRecordDO createCheckinRecord() {
        CheckinRecordDO recordDO = new CheckinRecordDO();
        recordDO.setAssignmentId(7001L);
        recordDO.setActivityId(TEST_ACTIVITY_ID);
        recordDO.setPositionId(8001L);
        recordDO.setUserId(5001L);
        recordDO.setCheckinType("CHECK_IN");
        recordDO.setCheckinStatus("NORMAL");
        recordDO.setCheckinTime(LocalDateTime.of(2026, 8, 1, 8, 5));
        recordDO.setLongitude(new BigDecimal("113.0000000"));
        recordDO.setLatitude(new BigDecimal("23.0000000"));
        recordDO.setQrCode("QR-TEST");
        return recordDO;
    }
}
