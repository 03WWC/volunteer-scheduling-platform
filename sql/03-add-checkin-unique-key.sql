ALTER TABLE `checkin_record`
    ADD UNIQUE KEY `uk_assignment_user_type` (`assignment_id`, `user_id`, `checkin_type`, `is_deleted`);
