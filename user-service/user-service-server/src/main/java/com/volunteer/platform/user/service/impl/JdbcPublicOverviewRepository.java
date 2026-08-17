package com.volunteer.platform.user.service.impl;

import com.volunteer.platform.user.service.PublicOverviewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPublicOverviewRepository implements PublicOverviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPublicOverviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int countTodayActivities() {
        return queryInt("SELECT COUNT(1) "
            + "FROM activity "
            + "WHERE is_deleted = 0 "
            + "AND DATE(start_time) <= CURRENT_DATE "
            + "AND DATE(end_time) >= CURRENT_DATE");
    }

    @Override
    public int countActiveVolunteers() {
        return queryInt("SELECT COUNT(DISTINCT ci.user_id) "
            + "FROM checkin_record ci "
            + "WHERE ci.is_deleted = 0 "
            + "AND ci.checkin_status = 'NORMAL' "
            + "AND ci.checkin_type = 'CHECK_IN' "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM checkin_record co "
            + "WHERE co.is_deleted = 0 "
            + "AND co.checkin_status = 'NORMAL' "
            + "AND co.checkin_type = 'CHECK_OUT' "
            + "AND co.assignment_id = ci.assignment_id "
            + "AND co.user_id = ci.user_id)");
    }

    @Override
    public int countAssignedVolunteers() {
        return queryInt("SELECT COUNT(1) "
            + "FROM schedule_assignment "
            + "WHERE is_deleted = 0");
    }

    @Override
    public int countRequiredPositions() {
        return queryInt("SELECT COALESCE(SUM(need_count), 0) "
            + "FROM position "
            + "WHERE is_deleted = 0");
    }

    private int queryInt(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
