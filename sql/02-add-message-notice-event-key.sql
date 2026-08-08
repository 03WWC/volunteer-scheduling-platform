USE `volunteer_platform`;

ALTER TABLE `message_notice`
    ADD COLUMN `event_key` VARCHAR(128) DEFAULT NULL COMMENT '消息事件唯一键' AFTER `receiver_id`;

ALTER TABLE `message_notice`
    ADD UNIQUE KEY `uk_event_key` (`event_key`);
