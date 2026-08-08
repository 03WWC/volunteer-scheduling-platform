package com.volunteer.platform.dispatch.dao;

import com.volunteer.platform.dispatch.DispatchServiceApplication;
import com.volunteer.platform.dispatch.entity.DispatchRecommendationDO;
import com.volunteer.platform.dispatch.entity.DispatchTaskDO;
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

@SpringBootTest(classes = DispatchServiceApplication.class)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class DispatchMapperIntegrationTest {

    private static final Long TEST_ACTIVITY_ID = 920_001L;

    @Autowired
    private DispatchTaskDAO dispatchTaskDAO;

    @Autowired
    private DispatchRecommendationDAO dispatchRecommendationDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("""
            UPDATE dispatch_recommendation dr
            JOIN dispatch_task dt ON dr.dispatch_task_id = dt.id
            SET dr.is_deleted = 1
            WHERE dt.activity_id = ?
            """, TEST_ACTIVITY_ID);
        jdbcTemplate.update("UPDATE dispatch_task SET is_deleted = 1 WHERE activity_id = ?", TEST_ACTIVITY_ID);
    }

    @Test
    void insertsUpdatesAndSelectsDispatchTaskWithRecommendations() {
        DispatchTaskDO taskDO = new DispatchTaskDO();
        taskDO.setActivityId(TEST_ACTIVITY_ID);
        taskDO.setAreaId(1001L);
        taskDO.setPositionId(2001L);
        taskDO.setRequiredCount(2);
        taskDO.setReason("INTEGRATION_TEST");
        taskDO.setDispatchStatus("PENDING");
        taskDO.setCreatedBy(1L);

        assertThat(dispatchTaskDAO.insert(taskDO)).isEqualTo(1);

        DispatchRecommendationDO firstRecommendationDO = createRecommendation(taskDO.getId(), 3001L, 0);
        DispatchRecommendationDO secondRecommendationDO = createRecommendation(taskDO.getId(), 3002L, 1);

        assertThat(dispatchRecommendationDAO.insert(firstRecommendationDO)).isEqualTo(1);
        assertThat(dispatchRecommendationDAO.insert(secondRecommendationDO)).isEqualTo(1);

        DispatchTaskDO statusDO = new DispatchTaskDO();
        statusDO.setId(taskDO.getId());
        statusDO.setDispatchStatus("FINISHED");
        statusDO.setFinishedTime(LocalDateTime.of(2026, 8, 1, 8, 30));

        assertThat(dispatchTaskDAO.updateStatus(statusDO)).isEqualTo(1);
        assertThat(dispatchTaskDAO.selectById(taskDO.getId()).getDispatchStatus()).isEqualTo("FINISHED");
        assertThat(dispatchRecommendationDAO.selectByTaskId(taskDO.getId())).hasSize(2);
    }

    private DispatchRecommendationDO createRecommendation(Long taskId, Long userId, int index) {
        DispatchRecommendationDO recommendationDO = new DispatchRecommendationDO();
        recommendationDO.setDispatchTaskId(taskId);
        recommendationDO.setUserId(userId);
        recommendationDO.setDistanceMeter(BigDecimal.valueOf(index * 100L));
        recommendationDO.setMatchScore(BigDecimal.valueOf(100L - index));
        recommendationDO.setRecommendStatus("RECOMMENDED");
        return recommendationDO;
    }
}
