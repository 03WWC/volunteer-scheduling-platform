package com.volunteer.platform.activity.dao;

import com.volunteer.platform.activity.entity.ActivityDO;
import com.volunteer.platform.activity.entity.AreaDO;
import com.volunteer.platform.activity.entity.PositionDO;
import com.volunteer.platform.activity.query.ActivityQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.cloud.nacos.discovery.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class ActivityMapperIntegrationTest {

    @Autowired
    private ActivityDAO activityDAO;

    @Autowired
    private AreaDAO areaDAO;

    @Autowired
    private PositionDAO positionDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("""
            UPDATE position
            SET is_deleted = 1
            WHERE activity_id IN (
                SELECT id FROM activity WHERE owner_name = ? AND name LIKE ?
            )
            """, "System Test", "IT Activity %");
        jdbcTemplate.update("""
            UPDATE area
            SET is_deleted = 1
            WHERE activity_id IN (
                SELECT id FROM activity WHERE owner_name = ? AND name LIKE ?
            )
            """, "System Test", "IT Activity %");
        jdbcTemplate.update("""
            UPDATE activity
            SET is_deleted = 1
            WHERE owner_name = ? AND name LIKE ?
            """, "System Test", "IT Activity %");
    }

    @Test
    void shouldInsertAndQueryActivityAreaAndPosition() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withNano(0);
        ActivityDO activityDO = new ActivityDO();
        activityDO.setName("IT Activity " + System.currentTimeMillis());
        activityDO.setActivityType("SPORTS");
        activityDO.setStartTime(startTime);
        activityDO.setEndTime(startTime.plusHours(4));
        activityDO.setLocation("Main Stadium");
        activityDO.setOwnerName("System Test");
        activityDO.setContactPhone("13800000000");

        int activityRows = activityDAO.insert(activityDO);

        assertThat(activityRows).isEqualTo(1);
        assertThat(activityDO.getId()).isNotNull();
        assertThat(activityDAO.selectById(activityDO.getId()).getName()).isEqualTo(activityDO.getName());

        ActivityQuery query = new ActivityQuery();
        query.setNameKeyword(activityDO.getName());
        List<ActivityDO> activities = activityDAO.selectPage(query);
        assertThat(activities).extracting(ActivityDO::getId).contains(activityDO.getId());

        AreaDO areaDO = new AreaDO();
        areaDO.setActivityId(activityDO.getId());
        areaDO.setName("North Gate");
        areaDO.setGps("116.391000,39.907000");
        areaDO.setOwnerName("Area Manager");
        assertThat(areaDAO.insert(areaDO)).isEqualTo(1);
        assertThat(areaDO.getId()).isNotNull();
        assertThat(areaDAO.selectByActivityId(activityDO.getId())).extracting(AreaDO::getId).contains(areaDO.getId());
        areaDO.setName("East Gate");
        assertThat(areaDAO.updateById(areaDO)).isEqualTo(1);
        assertThat(areaDAO.selectById(areaDO.getId()).getName()).isEqualTo("East Gate");

        PositionDO positionDO = new PositionDO();
        positionDO.setActivityId(activityDO.getId());
        positionDO.setAreaId(areaDO.getId());
        positionDO.setName("Entrance Guide");
        positionDO.setNeedCount(3);
        positionDO.setSkillRequirement("GUIDE");
        positionDO.setSalary(BigDecimal.valueOf(80));
        positionDO.setStartTime(startTime);
        positionDO.setEndTime(startTime.plusHours(4));
        assertThat(positionDAO.insert(positionDO)).isEqualTo(1);
        assertThat(positionDO.getId()).isNotNull();
        assertThat(positionDAO.selectById(positionDO.getId()).getName()).isEqualTo(positionDO.getName());
        assertThat(positionDAO.selectByActivityId(activityDO.getId())).extracting(PositionDO::getId)
            .contains(positionDO.getId());
        positionDO.setName("Entrance Support");
        positionDO.setNeedCount(5);
        assertThat(positionDAO.updateById(positionDO)).isEqualTo(1);
        assertThat(positionDAO.selectById(positionDO.getId()).getNeedCount()).isEqualTo(5);

        assertThat(positionDAO.deleteById(positionDO.getId())).isEqualTo(1);
        assertThat(positionDAO.selectById(positionDO.getId())).isNull();
        assertThat(areaDAO.deleteById(areaDO.getId())).isEqualTo(1);
        assertThat(areaDAO.selectById(areaDO.getId())).isNull();
        assertThat(activityDAO.deleteById(activityDO.getId())).isEqualTo(1);
        assertThat(activityDAO.selectById(activityDO.getId())).isNull();
    }
}
