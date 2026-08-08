package com.volunteer.platform.user.dao;

import com.volunteer.platform.user.entity.UserAvailabilityDO;
import com.volunteer.platform.user.entity.UserDO;
import com.volunteer.platform.user.entity.UserSkillDO;
import com.volunteer.platform.user.query.UserQuery;
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
class UserMapperIntegrationTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserSkillDAO userSkillDAO;

    @Autowired
    private UserAvailabilityDAO userAvailabilityDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("""
            UPDATE volunteer_availability
            SET is_deleted = 1
            WHERE user_id IN (
                SELECT id FROM user_info WHERE username LIKE ?
            )
            """, "it_user_%");
        jdbcTemplate.update("""
            UPDATE user_skill
            SET is_deleted = 1
            WHERE user_id IN (
                SELECT id FROM user_info WHERE username LIKE ?
            )
            """, "it_user_%");
        jdbcTemplate.update("""
            UPDATE user_info
            SET is_deleted = 1
            WHERE username LIKE ?
            """, "it_user_%");
    }

    @Test
    void shouldInsertAndQueryUserSkillAndAvailability() {
        long suffix = System.currentTimeMillis();
        UserDO userDO = new UserDO();
        userDO.setUsername("it_user_" + suffix);
        userDO.setMobile("139" + String.valueOf(suffix).substring(3, 11));
        userDO.setUserType("VOLUNTEER");
        userDO.setAuthStatus("UNAUTHENTICATED");
        userDO.setStatus("ENABLED");

        assertThat(userDAO.insert(userDO)).isEqualTo(1);
        assertThat(userDO.getId()).isNotNull();

        UserQuery query = new UserQuery();
        query.setMobileKeyword(userDO.getMobile());
        assertThat(userDAO.selectVolunteers(query)).extracting(UserDO::getId).contains(userDO.getId());

        UserDO authDO = new UserDO();
        authDO.setId(userDO.getId());
        authDO.setRealName("Integration User");
        authDO.setIdCardNo("110101199001010011");
        authDO.setAuthStatus("AUTHENTICATED");
        assertThat(userDAO.updateAuth(authDO)).isEqualTo(1);
        assertThat(userDAO.selectById(userDO.getId()).getAuthStatus()).isEqualTo("AUTHENTICATED");

        UserSkillDO skillDO = new UserSkillDO();
        skillDO.setUserId(userDO.getId());
        skillDO.setSkillCode("GUIDE");
        skillDO.setSkillName("Guide");
        skillDO.setSkillLevel("SENIOR");
        assertThat(userSkillDAO.insert(skillDO)).isEqualTo(1);
        assertThat(userSkillDAO.selectByUserId(userDO.getId())).extracting(UserSkillDO::getSkillCode)
            .containsExactly("GUIDE");

        UserAvailabilityDO availabilityDO = new UserAvailabilityDO();
        availabilityDO.setUserId(userDO.getId());
        availabilityDO.setAvailableDate(LocalDate.of(2026, 8, 1));
        availabilityDO.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        availabilityDO.setEndTime(LocalDateTime.of(2026, 8, 1, 18, 0));
        availabilityDO.setStatus("AVAILABLE");
        assertThat(userAvailabilityDAO.insert(availabilityDO)).isEqualTo(1);
        assertThat(userAvailabilityDAO.selectByUserId(userDO.getId())).extracting(UserAvailabilityDO::getAvailableDate)
            .containsExactly(LocalDate.of(2026, 8, 1));
    }
}
