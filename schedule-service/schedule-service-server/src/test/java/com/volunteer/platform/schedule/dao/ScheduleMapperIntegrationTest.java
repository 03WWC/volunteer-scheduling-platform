package com.volunteer.platform.schedule.dao;

import com.volunteer.platform.schedule.entity.ScheduleAssignmentDO;
import com.volunteer.platform.schedule.entity.SchedulePlanDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.cloud.nacos.discovery.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class ScheduleMapperIntegrationTest {

    private static final Long TEST_ACTIVITY_ID = 99000001L;

    @Autowired
    private SchedulePlanDAO schedulePlanDAO;

    @Autowired
    private ScheduleAssignmentDAO scheduleAssignmentDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("""
            UPDATE schedule_assignment
            SET is_deleted = 1
            WHERE activity_id = ?
            """, TEST_ACTIVITY_ID);
        jdbcTemplate.update("""
            UPDATE schedule_plan
            SET is_deleted = 1
            WHERE activity_id = ?
            """, TEST_ACTIVITY_ID);
    }

    @Test
    void shouldInsertAndQuerySchedulePlanAndAssignments() {
        SchedulePlanDO planDO = new SchedulePlanDO();
        planDO.setActivityId(TEST_ACTIVITY_ID);
        planDO.setPlanNo("SCH-IT-" + System.currentTimeMillis());
        planDO.setPlanName("Integration Schedule");
        planDO.setPlanStatus("GENERATED");
        planDO.setGeneratedBy(1L);

        assertThat(schedulePlanDAO.insert(planDO)).isEqualTo(1);
        assertThat(planDO.getId()).isNotNull();
        assertThat(schedulePlanDAO.selectLatestByActivityId(TEST_ACTIVITY_ID).getPlanNo()).isEqualTo(planDO.getPlanNo());

        ScheduleAssignmentDO assignmentDO = new ScheduleAssignmentDO();
        assignmentDO.setPlanId(planDO.getId());
        assignmentDO.setActivityId(TEST_ACTIVITY_ID);
        assignmentDO.setAreaId(10L);
        assignmentDO.setPositionId(20L);
        assignmentDO.setUserId(30L);
        assignmentDO.setWorkDate(LocalDate.of(2026, 8, 1));
        assignmentDO.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        assignmentDO.setEndTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        assignmentDO.setAssignmentStatus("WAIT_CONFIRM");

        assertThat(scheduleAssignmentDAO.insert(assignmentDO)).isEqualTo(1);
        assertThat(assignmentDO.getId()).isNotNull();
        assertThat(scheduleAssignmentDAO.selectByPlanId(planDO.getId())).extracting(ScheduleAssignmentDO::getUserId)
            .containsExactly(30L);

        SchedulePlanDO publishDO = new SchedulePlanDO();
        publishDO.setId(planDO.getId());
        publishDO.setPlanStatus("PUBLISHED");
        publishDO.setPublishedTime(LocalDateTime.of(2026, 8, 1, 8, 0));
        assertThat(schedulePlanDAO.updateStatus(publishDO)).isEqualTo(1);
        assertThat(schedulePlanDAO.selectById(planDO.getId()).getPlanStatus()).isEqualTo("PUBLISHED");

        ScheduleAssignmentDO confirmDO = new ScheduleAssignmentDO();
        confirmDO.setId(assignmentDO.getId());
        confirmDO.setAssignmentStatus("CONFIRMED");
        assertThat(scheduleAssignmentDAO.updateStatus(confirmDO)).isEqualTo(1);
        assertThat(scheduleAssignmentDAO.selectById(assignmentDO.getId()).getAssignmentStatus()).isEqualTo("CONFIRMED");

        ScheduleAssignmentDO conflictQuery = new ScheduleAssignmentDO();
        conflictQuery.setActivityId(TEST_ACTIVITY_ID);
        conflictQuery.setUserId(30L);
        conflictQuery.setWorkDate(LocalDate.of(2026, 8, 1));
        conflictQuery.setStartTime(LocalDateTime.of(2026, 8, 1, 11, 0));
        conflictQuery.setEndTime(LocalDateTime.of(2026, 8, 1, 13, 0));
        assertThat(scheduleAssignmentDAO.countUserTimeConflict(conflictQuery)).isEqualTo(1);
    }
}
