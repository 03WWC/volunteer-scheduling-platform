CREATE DATABASE IF NOT EXISTS `volunteer_platform`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `volunteer_platform`;

CREATE TABLE IF NOT EXISTS `user_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    `mobile` VARCHAR(20) NOT NULL COMMENT '手机号',
    `id_card_no` VARCHAR(32) DEFAULT NULL COMMENT '身份证号',
    `user_type` VARCHAR(32) NOT NULL COMMENT '用户类型 ORGANIZER/VOLUNTEER/MANAGER/SECURITY',
    `auth_status` VARCHAR(32) NOT NULL DEFAULT 'UNAUTHENTICATED' COMMENT '实名认证状态',
    `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mobile` (`mobile`),
    KEY `idx_user_type` (`user_type`),
    KEY `idx_auth_status` (`auth_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `user_skill` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `skill_code` VARCHAR(64) NOT NULL COMMENT '技能编码',
    `skill_name` VARCHAR(64) NOT NULL COMMENT '技能名称',
    `skill_level` VARCHAR(32) DEFAULT NULL COMMENT '技能等级',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id_skill_code` (`user_id`, `skill_code`),
    KEY `idx_skill_code` (`skill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户技能表';

CREATE TABLE IF NOT EXISTS `volunteer_availability` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `available_date` DATE NOT NULL COMMENT '可服务日期',
    `start_time` DATETIME NOT NULL COMMENT '可服务开始时间',
    `end_time` DATETIME NOT NULL COMMENT '可服务结束时间',
    `status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT '可用状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_date` (`user_id`, `available_date`),
    KEY `idx_available_date` (`available_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者可服务时间表';

CREATE TABLE IF NOT EXISTS `activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(128) NOT NULL COMMENT '活动名称',
    `activity_type` VARCHAR(64) NOT NULL COMMENT '活动类型',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `location` VARCHAR(255) NOT NULL COMMENT '活动地点',
    `owner_id` BIGINT DEFAULT NULL COMMENT '负责人ID',
    `owner_name` VARCHAR(64) DEFAULT NULL COMMENT '负责人姓名',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系方式',
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_type` (`activity_type`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

CREATE TABLE IF NOT EXISTS `area` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `name` VARCHAR(128) NOT NULL COMMENT '区域名称',
    `gps` TEXT DEFAULT NULL COMMENT 'GPS范围',
    `owner_id` BIGINT DEFAULT NULL COMMENT '区域负责人ID',
    `owner_name` VARCHAR(64) DEFAULT NULL COMMENT '区域负责人姓名',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '区域状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动区域表';

CREATE TABLE IF NOT EXISTS `position` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `area_id` BIGINT NOT NULL COMMENT '区域ID',
    `name` VARCHAR(128) NOT NULL COMMENT '岗位名称',
    `position_type` VARCHAR(64) DEFAULT NULL COMMENT '岗位类型',
    `need_count` INT NOT NULL COMMENT '需要人数',
    `start_time` DATETIME NOT NULL COMMENT '工作开始时间',
    `end_time` DATETIME NOT NULL COMMENT '工作结束时间',
    `skill_requirement` VARCHAR(255) DEFAULT NULL COMMENT '技能要求',
    `salary` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '补贴金额',
    `status` VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT '岗位状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_area_id` (`area_id`),
    KEY `idx_time_range` (`start_time`, `end_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

CREATE TABLE IF NOT EXISTS `activity_signup` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `position_id` BIGINT DEFAULT NULL COMMENT '报名岗位ID',
    `user_id` BIGINT NOT NULL COMMENT '报名用户ID',
    `signup_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '报名状态',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_id_user_id_position_id` (`activity_id`, `user_id`, `position_id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_signup_status` (`signup_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动报名表';

CREATE TABLE IF NOT EXISTS `schedule_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `plan_no` VARCHAR(64) NOT NULL COMMENT '排班方案编号',
    `plan_name` VARCHAR(128) DEFAULT NULL COMMENT '排班方案名称',
    `plan_status` VARCHAR(32) NOT NULL DEFAULT 'GENERATED' COMMENT '方案状态',
    `generated_by` BIGINT DEFAULT NULL COMMENT '生成人员ID',
    `published_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_no` (`plan_no`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_plan_status` (`plan_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班方案表';

CREATE TABLE IF NOT EXISTS `schedule_assignment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id` BIGINT NOT NULL COMMENT '排班方案ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `area_id` BIGINT NOT NULL COMMENT '区域ID',
    `position_id` BIGINT NOT NULL COMMENT '岗位ID',
    `user_id` BIGINT NOT NULL COMMENT '被排班用户ID',
    `work_date` DATE NOT NULL COMMENT '工作日期',
    `start_time` DATETIME NOT NULL COMMENT '工作开始时间',
    `end_time` DATETIME NOT NULL COMMENT '工作结束时间',
    `assignment_status` VARCHAR(32) NOT NULL DEFAULT 'WAIT_CONFIRM' COMMENT '排班状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_user_id_work_date` (`user_id`, `work_date`),
    KEY `idx_position_id` (`position_id`),
    KEY `idx_assignment_status` (`assignment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班明细表';

CREATE TABLE IF NOT EXISTS `checkin_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `assignment_id` BIGINT NOT NULL COMMENT '排班明细ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `position_id` BIGINT NOT NULL COMMENT '岗位ID',
    `user_id` BIGINT NOT NULL COMMENT '签到用户ID',
    `checkin_type` VARCHAR(32) NOT NULL COMMENT '签到类型 CHECK_IN/CHECK_OUT',
    `checkin_status` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '签到状态',
    `checkin_time` DATETIME NOT NULL COMMENT '签到时间',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
    `qr_code` VARCHAR(128) DEFAULT NULL COMMENT '二维码编码',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_assignment_user_type` (`assignment_id`, `user_id`, `checkin_type`, `is_deleted`),
    KEY `idx_assignment_id` (`assignment_id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_checkin_time` (`checkin_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到记录表';

CREATE TABLE IF NOT EXISTS `location_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `longitude` DECIMAL(10,7) NOT NULL COMMENT '经度',
    `latitude` DECIMAL(10,7) NOT NULL COMMENT '纬度',
    `location_time` DATETIME NOT NULL COMMENT '定位时间',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ONLINE' COMMENT '定位状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id_user_id` (`activity_id`, `user_id`),
    KEY `idx_location_time` (`location_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定位轨迹表';

CREATE TABLE IF NOT EXISTS `user_realtime_status` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT DEFAULT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `status` VARCHAR(32) NOT NULL DEFAULT 'FREE' COMMENT '实时状态 FREE/WORKING/OFFLINE',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
    `last_report_time` DATETIME DEFAULT NULL COMMENT '最后上报时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_id_user_id` (`activity_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人员实时状态表';

CREATE TABLE IF NOT EXISTS `dispatch_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `area_id` BIGINT NOT NULL COMMENT '区域ID',
    `position_id` BIGINT NOT NULL COMMENT '岗位ID',
    `required_count` INT NOT NULL COMMENT '缺口人数',
    `reason` VARCHAR(255) DEFAULT NULL COMMENT '调度原因',
    `dispatch_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '调度状态',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `finished_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_position_id` (`position_id`),
    KEY `idx_dispatch_status` (`dispatch_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度任务表';

CREATE TABLE IF NOT EXISTS `dispatch_recommendation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dispatch_task_id` BIGINT NOT NULL COMMENT '调度任务ID',
    `user_id` BIGINT NOT NULL COMMENT '推荐用户ID',
    `distance_meter` DECIMAL(10,2) DEFAULT NULL COMMENT '距离米数',
    `match_score` DECIMAL(6,2) DEFAULT NULL COMMENT '匹配分',
    `recommend_status` VARCHAR(32) NOT NULL DEFAULT 'RECOMMENDED' COMMENT '推荐状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_dispatch_task_id` (`dispatch_task_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_recommend_status` (`recommend_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度推荐表';

CREATE TABLE IF NOT EXISTS `settlement_bill` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bill_no` VARCHAR(64) NOT NULL COMMENT '结算单号',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_work_minutes` INT NOT NULL DEFAULT 0 COMMENT '总工作分钟',
    `base_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '基础金额',
    `hour_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '小时工资',
    `reward_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '奖励金额',
    `deduct_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '扣款金额',
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应结金额',
    `bill_status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '账单状态',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bill_no` (`bill_no`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_bill_status` (`bill_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算单表';

CREATE TABLE IF NOT EXISTS `settlement_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bill_id` BIGINT NOT NULL COMMENT '结算单ID',
    `assignment_id` BIGINT NOT NULL COMMENT '排班明细ID',
    `position_id` BIGINT NOT NULL COMMENT '岗位ID',
    `work_minutes` INT NOT NULL DEFAULT 0 COMMENT '工作分钟',
    `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '明细金额',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_bill_id` (`bill_id`),
    KEY `idx_assignment_id` (`assignment_id`),
    KEY `idx_position_id` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算明细表';

CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bill_id` BIGINT NOT NULL COMMENT '结算单ID',
    `pay_no` VARCHAR(64) NOT NULL COMMENT '支付流水号',
    `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道',
    `pay_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    `pay_status` VARCHAR(32) NOT NULL DEFAULT 'WAIT_PAY' COMMENT '支付状态',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_no` (`pay_no`),
    KEY `idx_bill_id` (`bill_id`),
    KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

CREATE TABLE IF NOT EXISTS `message_notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `receiver_id` BIGINT NOT NULL COMMENT '接收人ID',
    `event_key` VARCHAR(128) DEFAULT NULL COMMENT '消息事件唯一键',
    `notice_type` VARCHAR(64) NOT NULL COMMENT '通知类型',
    `title` VARCHAR(128) NOT NULL COMMENT '标题',
    `content` VARCHAR(1000) NOT NULL COMMENT '内容',
    `send_channel` VARCHAR(32) NOT NULL COMMENT '发送渠道',
    `send_status` VARCHAR(32) NOT NULL DEFAULT 'WAIT_SEND' COMMENT '发送状态',
    `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读 0-否 1-是',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_key` (`event_key`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_notice_type` (`notice_type`),
    KEY `idx_send_status` (`send_status`),
    KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';

CREATE TABLE IF NOT EXISTS `mq_event_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic` VARCHAR(128) NOT NULL COMMENT '消息主题',
    `event_key` VARCHAR(128) NOT NULL COMMENT '事件唯一键',
    `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型',
    `payload` TEXT NOT NULL COMMENT '消息内容',
    `event_status` VARCHAR(32) NOT NULL DEFAULT 'NEW' COMMENT '事件状态',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `last_error` VARCHAR(1000) DEFAULT NULL COMMENT '最后错误',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_key` (`event_key`),
    KEY `idx_topic` (`topic`),
    KEY `idx_event_status` (`event_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQ事件日志表';

CREATE TABLE IF NOT EXISTS `ai_schedule_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `job_no` VARCHAR(64) NOT NULL COMMENT '任务编号',
    `job_type` VARCHAR(64) NOT NULL COMMENT 'AI任务类型',
    `job_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    `input_payload` TEXT DEFAULT NULL COMMENT '输入参数',
    `output_payload` TEXT DEFAULT NULL COMMENT '输出结果',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_no` (`job_no`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_job_status` (`job_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI排班任务表';

CREATE TABLE IF NOT EXISTS `risk_prediction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `area_id` BIGINT DEFAULT NULL COMMENT '区域ID',
    `position_id` BIGINT DEFAULT NULL COMMENT '岗位ID',
    `risk_type` VARCHAR(64) NOT NULL COMMENT '风险类型',
    `risk_level` VARCHAR(32) NOT NULL COMMENT '风险等级',
    `risk_score` DECIMAL(6,2) NOT NULL DEFAULT 0.00 COMMENT '风险分',
    `prediction_time` DATETIME NOT NULL COMMENT '预测时间',
    `suggestion` VARCHAR(1000) DEFAULT NULL COMMENT '处理建议',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_area_id` (`area_id`),
    KEY `idx_risk_type_level` (`risk_type`, `risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险预测表';
