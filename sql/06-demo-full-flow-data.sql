USE `volunteer_platform`;

SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

-- Full-flow demo data for defense and acceptance testing.
-- Re-runnable: it only clears records created by this script.

SET @today = CURDATE();
SET @tomorrow = DATE_ADD(CURDATE(), INTERVAL 1 DAY);
SET @now = NOW();
SET @admin_id = COALESCE((SELECT id FROM admin_user WHERE account = 'admin' AND is_deleted = 0 LIMIT 1), 1);

DROP TEMPORARY TABLE IF EXISTS demo_activity_ids;
CREATE TEMPORARY TABLE demo_activity_ids AS
SELECT id
FROM activity
WHERE name IN ('DEMO-城市图书馆周末志愿服务', 'DEMO-社区公益嘉年华保障活动')
  AND is_deleted = 0;

DROP TEMPORARY TABLE IF EXISTS demo_user_ids;
CREATE TEMPORARY TABLE demo_user_ids AS
SELECT id
FROM user_info
WHERE mobile IN (
    '19900001000', '19900001001', '19900001002', '19900001003', '19900001004',
    '19900001005', '19900001006', '19900001007', '19900001008', '19900001009',
    '19900001010'
)
   OR openid LIKE 'demo-openid-%';

DROP TEMPORARY TABLE IF EXISTS demo_assignment_ids;
CREATE TEMPORARY TABLE demo_assignment_ids AS
SELECT id
FROM schedule_assignment
WHERE activity_id IN (SELECT id FROM demo_activity_ids)
   OR user_id IN (SELECT id FROM demo_user_ids);

DROP TEMPORARY TABLE IF EXISTS demo_bill_ids;
CREATE TEMPORARY TABLE demo_bill_ids AS
SELECT id
FROM settlement_bill
WHERE bill_no LIKE 'DEMO-BILL-%'
   OR activity_id IN (SELECT id FROM demo_activity_ids)
   OR user_id IN (SELECT id FROM demo_user_ids);

DROP TEMPORARY TABLE IF EXISTS demo_dispatch_task_ids;
CREATE TEMPORARY TABLE demo_dispatch_task_ids AS
SELECT id
FROM dispatch_task
WHERE activity_id IN (SELECT id FROM demo_activity_ids);

DELETE FROM payment_record WHERE bill_id IN (SELECT id FROM demo_bill_ids) OR pay_no LIKE 'DEMO-PAY-%';
DELETE FROM settlement_detail WHERE bill_id IN (SELECT id FROM demo_bill_ids) OR assignment_id IN (SELECT id FROM demo_assignment_ids);
DELETE FROM settlement_bill WHERE id IN (SELECT id FROM demo_bill_ids);
DELETE FROM message_notice WHERE event_key LIKE 'DEMO-%' OR receiver_id IN (SELECT id FROM demo_user_ids);
DELETE FROM dispatch_recommendation WHERE dispatch_task_id IN (SELECT id FROM demo_dispatch_task_ids) OR user_id IN (SELECT id FROM demo_user_ids);
DELETE FROM dispatch_task WHERE id IN (SELECT id FROM demo_dispatch_task_ids);
DELETE FROM risk_prediction WHERE activity_id IN (SELECT id FROM demo_activity_ids);
DELETE FROM ai_schedule_job WHERE activity_id IN (SELECT id FROM demo_activity_ids) OR job_no LIKE 'DEMO-AI-%';
DELETE FROM mq_event_log WHERE event_key LIKE 'DEMO-%';
DELETE FROM checkin_record
WHERE assignment_id IN (SELECT id FROM demo_assignment_ids)
   OR activity_id IN (SELECT id FROM demo_activity_ids)
   OR user_id IN (SELECT id FROM demo_user_ids);
DELETE FROM location_record WHERE activity_id IN (SELECT id FROM demo_activity_ids) OR user_id IN (SELECT id FROM demo_user_ids);
DELETE FROM user_realtime_status WHERE activity_id IN (SELECT id FROM demo_activity_ids) OR user_id IN (SELECT id FROM demo_user_ids);
DELETE FROM schedule_assignment WHERE id IN (SELECT id FROM demo_assignment_ids);
DELETE FROM schedule_plan WHERE activity_id IN (SELECT id FROM demo_activity_ids) OR plan_no LIKE 'DEMO-SCH-%';
DELETE FROM activity_signup WHERE activity_id IN (SELECT id FROM demo_activity_ids) OR user_id IN (SELECT id FROM demo_user_ids);
DELETE FROM position WHERE activity_id IN (SELECT id FROM demo_activity_ids);
DELETE FROM area WHERE activity_id IN (SELECT id FROM demo_activity_ids);
DELETE FROM activity WHERE id IN (SELECT id FROM demo_activity_ids);
DELETE FROM volunteer_availability WHERE user_id IN (SELECT id FROM demo_user_ids);
DELETE FROM user_skill WHERE user_id IN (SELECT id FROM demo_user_ids);
DELETE FROM user_info WHERE id IN (SELECT id FROM demo_user_ids);

INSERT INTO user_info
    (username, real_name, mobile, openid, id_card_no, user_type, auth_status, avatar_url, status, is_deleted)
VALUES
    ('demo-organizer', '陈老师', '19900001000', 'demo-openid-organizer', '440800199001010010', 'MANAGER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-wang', '王文川', '19900001001', 'demo-openid-wang', '440800200301010011', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-chen', '陈广东', '19900001002', 'demo-openid-chen', '440800200302020012', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-liu-xiangyang', '刘向阳', '19900001003', 'demo-openid-liuxy', '440800200303030013', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-liu-zhongjian', '刘忠健', '19900001004', 'demo-openid-liuzj', '440800200304040014', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-lin', '林致澄', '19900001005', 'demo-openid-lin', '440800200305050015', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-lv', '吕良坤', '19900001006', 'demo-openid-lv', '440800200306060016', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-li', '李炳恒', '19900001007', 'demo-openid-li', '440800200307070017', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-wen', '温梓傲', '19900001008', 'demo-openid-wen', '440800200308080018', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-zhang', '张晓敏', '19900001009', 'demo-openid-zhang', '440800200309090019', 'VOLUNTEER', 'AUTHENTICATED', NULL, 'ENABLED', 0),
    ('demo-huang', '黄梓涵', '19900001010', 'demo-openid-huang', '440800200310100020', 'VOLUNTEER', 'UNAUTHENTICATED', NULL, 'ENABLED', 0);

SET @organizer_id = (SELECT id FROM user_info WHERE mobile = '19900001000');
SET @u_wang = (SELECT id FROM user_info WHERE mobile = '19900001001');
SET @u_chen = (SELECT id FROM user_info WHERE mobile = '19900001002');
SET @u_liuxy = (SELECT id FROM user_info WHERE mobile = '19900001003');
SET @u_liuzj = (SELECT id FROM user_info WHERE mobile = '19900001004');
SET @u_lin = (SELECT id FROM user_info WHERE mobile = '19900001005');
SET @u_lv = (SELECT id FROM user_info WHERE mobile = '19900001006');
SET @u_li = (SELECT id FROM user_info WHERE mobile = '19900001007');
SET @u_wen = (SELECT id FROM user_info WHERE mobile = '19900001008');
SET @u_zhang = (SELECT id FROM user_info WHERE mobile = '19900001009');
SET @u_huang = (SELECT id FROM user_info WHERE mobile = '19900001010');

INSERT INTO user_skill (user_id, skill_code, skill_name, skill_level)
VALUES
    (@u_wang, 'GUIDE', '现场引导', 'ADVANCED'),
    (@u_wang, 'ORDER', '秩序维护', 'INTERMEDIATE'),
    (@u_chen, 'ORDER', '秩序维护', 'ADVANCED'),
    (@u_chen, 'COMMUNICATION', '沟通协调', 'INTERMEDIATE'),
    (@u_liuxy, 'INFO', '信息咨询', 'ADVANCED'),
    (@u_liuxy, 'GUIDE', '现场引导', 'INTERMEDIATE'),
    (@u_liuzj, 'MATERIAL', '物资发放', 'ADVANCED'),
    (@u_liuzj, 'LOGISTICS', '后勤保障', 'INTERMEDIATE'),
    (@u_lin, 'MEDICAL', '医疗协助', 'INTERMEDIATE'),
    (@u_lv, 'PHOTO', '摄影记录', 'ADVANCED'),
    (@u_lv, 'MEDIA', '宣传采编', 'INTERMEDIATE'),
    (@u_li, 'INFO', '信息咨询', 'INTERMEDIATE'),
    (@u_wen, 'ORDER', '秩序维护', 'INTERMEDIATE'),
    (@u_wen, 'TECH', '设备协助', 'INTERMEDIATE'),
    (@u_zhang, 'MATERIAL', '物资发放', 'INTERMEDIATE'),
    (@u_huang, 'GUIDE', '现场引导', 'BASIC');

INSERT INTO volunteer_availability (user_id, available_date, start_time, end_time, status)
VALUES
    (@u_wang, @today, TIMESTAMP(@today, '08:00:00'), TIMESTAMP(@today, '18:30:00'), 'AVAILABLE'),
    (@u_chen, @today, TIMESTAMP(@today, '08:00:00'), TIMESTAMP(@today, '18:30:00'), 'AVAILABLE'),
    (@u_liuxy, @today, TIMESTAMP(@today, '08:30:00'), TIMESTAMP(@today, '18:00:00'), 'AVAILABLE'),
    (@u_liuzj, @today, TIMESTAMP(@today, '08:00:00'), TIMESTAMP(@today, '18:00:00'), 'AVAILABLE'),
    (@u_lin, @today, TIMESTAMP(@today, '08:30:00'), TIMESTAMP(@today, '18:00:00'), 'AVAILABLE'),
    (@u_lv, @today, TIMESTAMP(@today, '13:00:00'), TIMESTAMP(@today, '18:30:00'), 'AVAILABLE'),
    (@u_li, @today, TIMESTAMP(@today, '08:30:00'), TIMESTAMP(@today, '12:30:00'), 'AVAILABLE'),
    (@u_wen, @today, TIMESTAMP(@today, '08:00:00'), TIMESTAMP(@today, '18:30:00'), 'AVAILABLE'),
    (@u_zhang, @today, TIMESTAMP(@today, '13:30:00'), TIMESTAMP(@today, '18:00:00'), 'AVAILABLE'),
    (@u_wang, @tomorrow, TIMESTAMP(@tomorrow, '08:00:00'), TIMESTAMP(@tomorrow, '18:30:00'), 'AVAILABLE'),
    (@u_chen, @tomorrow, TIMESTAMP(@tomorrow, '08:00:00'), TIMESTAMP(@tomorrow, '18:30:00'), 'AVAILABLE'),
    (@u_liuxy, @tomorrow, TIMESTAMP(@tomorrow, '08:30:00'), TIMESTAMP(@tomorrow, '18:00:00'), 'AVAILABLE'),
    (@u_liuzj, @tomorrow, TIMESTAMP(@tomorrow, '08:00:00'), TIMESTAMP(@tomorrow, '18:00:00'), 'AVAILABLE'),
    (@u_lin, @tomorrow, TIMESTAMP(@tomorrow, '08:30:00'), TIMESTAMP(@tomorrow, '12:30:00'), 'AVAILABLE'),
    (@u_lv, @tomorrow, TIMESTAMP(@tomorrow, '13:00:00'), TIMESTAMP(@tomorrow, '18:30:00'), 'AVAILABLE'),
    (@u_li, @tomorrow, TIMESTAMP(@tomorrow, '08:30:00'), TIMESTAMP(@tomorrow, '18:00:00'), 'AVAILABLE'),
    (@u_wen, @tomorrow, TIMESTAMP(@tomorrow, '08:00:00'), TIMESTAMP(@tomorrow, '18:30:00'), 'AVAILABLE'),
    (@u_zhang, @tomorrow, TIMESTAMP(@tomorrow, '13:30:00'), TIMESTAMP(@tomorrow, '18:00:00'), 'AVAILABLE'),
    (@u_huang, @tomorrow, TIMESTAMP(@tomorrow, '09:00:00'), TIMESTAMP(@tomorrow, '12:00:00'), 'UNAVAILABLE');

INSERT INTO activity
    (name, activity_type, start_time, end_time, location, owner_id, owner_name, contact_phone, status)
VALUES
    ('DEMO-城市图书馆周末志愿服务', '公共文化服务', TIMESTAMP(@today, '08:30:00'), TIMESTAMP(@today, '18:30:00'),
     '岭南城市图书馆', @organizer_id, '陈老师', '19900001000', 'PUBLISHED'),
    ('DEMO-社区公益嘉年华保障活动', '社区公益活动', TIMESTAMP(@tomorrow, '08:30:00'), TIMESTAMP(@tomorrow, '18:30:00'),
     '岭南社区文化广场', @organizer_id, '陈老师', '19900001000', 'PUBLISHED');

SET @act_library = (SELECT id FROM activity WHERE name = 'DEMO-城市图书馆周末志愿服务');
SET @act_carnival = (SELECT id FROM activity WHERE name = 'DEMO-社区公益嘉年华保障活动');

INSERT INTO area (activity_id, name, gps, owner_id, owner_name, status)
VALUES
    (@act_library, '入口接待区', '113.3640000,22.9410000;113.3660000,22.9430000', @organizer_id, '陈老师', 'ENABLED'),
    (@act_library, '借阅服务区', '113.3660000,22.9420000;113.3680000,22.9440000', @organizer_id, '陈老师', 'ENABLED'),
    (@act_library, '阅读活动区', '113.3650000,22.9440000;113.3680000,22.9460000', @organizer_id, '陈老师', 'ENABLED'),
    (@act_carnival, '主舞台区', '113.3720000,22.9490000;113.3740000,22.9510000', @organizer_id, '陈老师', 'ENABLED'),
    (@act_carnival, '摊位互动区', '113.3740000,22.9490000;113.3760000,22.9510000', @organizer_id, '陈老师', 'ENABLED'),
    (@act_carnival, '后勤物资区', '113.3730000,22.9470000;113.3760000,22.9490000', @organizer_id, '陈老师', 'ENABLED');

SET @area_entry = (SELECT id FROM area WHERE activity_id = @act_library AND name = '入口接待区');
SET @area_borrow = (SELECT id FROM area WHERE activity_id = @act_library AND name = '借阅服务区');
SET @area_reading = (SELECT id FROM area WHERE activity_id = @act_library AND name = '阅读活动区');
SET @area_stage = (SELECT id FROM area WHERE activity_id = @act_carnival AND name = '主舞台区');
SET @area_booth = (SELECT id FROM area WHERE activity_id = @act_carnival AND name = '摊位互动区');
SET @area_logistics = (SELECT id FROM area WHERE activity_id = @act_carnival AND name = '后勤物资区');

INSERT INTO position
    (activity_id, area_id, name, position_type, need_count, start_time, end_time, skill_requirement, salary, status)
VALUES
    (@act_library, @area_entry, '入馆引导岗', 'GUIDE', 2, TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '12:00:00'), 'GUIDE', 30.00, 'OPEN'),
    (@act_library, @area_entry, '秩序维护岗', 'ORDER', 2, TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '12:00:00'), 'ORDER', 35.00, 'OPEN'),
    (@act_library, @area_borrow, '信息咨询岗', 'INFO', 1, TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '12:00:00'), 'INFO', 30.00, 'OPEN'),
    (@act_library, @area_reading, '亲子阅读协助岗', 'GUIDE', 2, TIMESTAMP(@today, '14:00:00'), TIMESTAMP(@today, '17:30:00'), 'GUIDE', 40.00, 'OPEN'),
    (@act_library, @area_reading, '摄影记录岗', 'PHOTO', 1, TIMESTAMP(@today, '14:00:00'), TIMESTAMP(@today, '17:30:00'), 'PHOTO', 45.00, 'OPEN'),
    (@act_library, @area_borrow, '物资整理岗', 'MATERIAL', 1, TIMESTAMP(@today, '14:00:00'), TIMESTAMP(@today, '17:30:00'), 'MATERIAL', 35.00, 'OPEN'),
    (@act_carnival, @area_stage, '主舞台秩序岗', 'ORDER', 3, TIMESTAMP(@tomorrow, '09:00:00'), TIMESTAMP(@tomorrow, '12:00:00'), 'ORDER', 35.00, 'OPEN'),
    (@act_carnival, @area_booth, '摊位咨询岗', 'INFO', 2, TIMESTAMP(@tomorrow, '09:00:00'), TIMESTAMP(@tomorrow, '12:00:00'), 'INFO', 30.00, 'OPEN'),
    (@act_carnival, @area_logistics, '物资补给岗', 'MATERIAL', 2, TIMESTAMP(@tomorrow, '14:00:00'), TIMESTAMP(@tomorrow, '17:30:00'), 'MATERIAL', 35.00, 'OPEN'),
    (@act_carnival, @area_stage, '活动摄影岗', 'PHOTO', 1, TIMESTAMP(@tomorrow, '14:00:00'), TIMESTAMP(@tomorrow, '17:30:00'), 'PHOTO', 45.00, 'OPEN'),
    (@act_carnival, @area_logistics, '医疗协助岗', 'MEDICAL', 1, TIMESTAMP(@tomorrow, '14:00:00'), TIMESTAMP(@tomorrow, '17:30:00'), 'MEDICAL', 45.00, 'OPEN');

SET @pos_lib_guide = (SELECT id FROM position WHERE activity_id = @act_library AND name = '入馆引导岗');
SET @pos_lib_order = (SELECT id FROM position WHERE activity_id = @act_library AND name = '秩序维护岗');
SET @pos_lib_info = (SELECT id FROM position WHERE activity_id = @act_library AND name = '信息咨询岗');
SET @pos_lib_reading = (SELECT id FROM position WHERE activity_id = @act_library AND name = '亲子阅读协助岗');
SET @pos_lib_photo = (SELECT id FROM position WHERE activity_id = @act_library AND name = '摄影记录岗');
SET @pos_lib_material = (SELECT id FROM position WHERE activity_id = @act_library AND name = '物资整理岗');
SET @pos_car_order = (SELECT id FROM position WHERE activity_id = @act_carnival AND name = '主舞台秩序岗');
SET @pos_car_info = (SELECT id FROM position WHERE activity_id = @act_carnival AND name = '摊位咨询岗');
SET @pos_car_material = (SELECT id FROM position WHERE activity_id = @act_carnival AND name = '物资补给岗');
SET @pos_car_photo = (SELECT id FROM position WHERE activity_id = @act_carnival AND name = '活动摄影岗');
SET @pos_car_medical = (SELECT id FROM position WHERE activity_id = @act_carnival AND name = '医疗协助岗');

INSERT INTO activity_signup (activity_id, position_id, user_id, signup_status, remark)
VALUES
    (@act_library, @pos_lib_guide, @u_wang, 'APPROVED', '熟悉图书馆入口动线，可承担引导工作'),
    (@act_library, @pos_lib_guide, @u_liuxy, 'APPROVED', '沟通表达较好，可协助入馆引导'),
    (@act_library, @pos_lib_order, @u_chen, 'APPROVED', '有大型活动秩序维护经验'),
    (@act_library, @pos_lib_order, @u_wen, 'APPROVED', '可配合现场安全提醒'),
    (@act_library, @pos_lib_info, @u_li, 'APPROVED', '熟悉借阅流程，可做咨询答疑'),
    (@act_library, @pos_lib_reading, @u_wang, 'APPROVED', '下午继续参与亲子阅读活动'),
    (@act_library, @pos_lib_photo, @u_lv, 'APPROVED', '摄影记录技能匹配'),
    (@act_library, @pos_lib_material, @u_liuzj, 'APPROVED', '物资整理经验充足'),
    (@act_library, @pos_lib_reading, @u_huang, 'PENDING', '新人首次报名，待确认实名认证'),
    (@act_carnival, @pos_car_order, @u_chen, 'APPROVED', '适合舞台区秩序维护'),
    (@act_carnival, @pos_car_order, @u_wen, 'APPROVED', '具备秩序维护技能'),
    (@act_carnival, @pos_car_info, @u_liuxy, 'APPROVED', '信息咨询能力较强'),
    (@act_carnival, @pos_car_info, @u_li, 'APPROVED', '熟悉现场答疑流程'),
    (@act_carnival, @pos_car_material, @u_liuzj, 'APPROVED', '后勤物资能力匹配'),
    (@act_carnival, @pos_car_photo, @u_lv, 'APPROVED', '摄影技能匹配'),
    (@act_carnival, @pos_car_medical, @u_lin, 'APPROVED', '具备医疗协助经验'),
    (@act_carnival, @pos_car_order, @u_huang, 'REJECTED', '实名认证未完成，暂不通过');

INSERT INTO schedule_plan (activity_id, plan_no, plan_name, plan_status, generated_by, published_time)
VALUES
    (@act_library, CONCAT('DEMO-SCH-LIB-', DATE_FORMAT(@today, '%Y%m%d')), '图书馆周末服务正式排班', 'PUBLISHED', @admin_id, DATE_SUB(@now, INTERVAL 2 HOUR)),
    (@act_carnival, CONCAT('DEMO-SCH-CAR-', DATE_FORMAT(@tomorrow, '%Y%m%d')), '社区公益嘉年华预排班', 'GENERATED', @admin_id, NULL);

SET @plan_library = (SELECT id FROM schedule_plan WHERE plan_no = CONCAT('DEMO-SCH-LIB-', DATE_FORMAT(@today, '%Y%m%d')));
SET @plan_carnival = (SELECT id FROM schedule_plan WHERE plan_no = CONCAT('DEMO-SCH-CAR-', DATE_FORMAT(@tomorrow, '%Y%m%d')));

INSERT INTO schedule_assignment
    (plan_id, activity_id, area_id, position_id, user_id, work_date, start_time, end_time, assignment_status)
VALUES
    (@plan_library, @act_library, @area_entry, @pos_lib_guide, @u_wang, @today, TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '12:00:00'), 'CONFIRMED'),
    (@plan_library, @act_library, @area_entry, @pos_lib_guide, @u_liuxy, @today, TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '12:00:00'), 'CONFIRMED'),
    (@plan_library, @act_library, @area_entry, @pos_lib_order, @u_chen, @today, TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '12:00:00'), 'CONFIRMED'),
    (@plan_library, @act_library, @area_entry, @pos_lib_order, @u_wen, @today, TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '12:00:00'), 'CONFIRMED'),
    (@plan_library, @act_library, @area_borrow, @pos_lib_info, @u_li, @today, TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '12:00:00'), 'CONFIRMED'),
    (@plan_library, @act_library, @area_reading, @pos_lib_reading, @u_wang, @today, TIMESTAMP(@today, '14:00:00'), TIMESTAMP(@today, '17:30:00'), 'CONFIRMED'),
    (@plan_library, @act_library, @area_reading, @pos_lib_photo, @u_lv, @today, TIMESTAMP(@today, '14:00:00'), TIMESTAMP(@today, '17:30:00'), 'CONFIRMED'),
    (@plan_library, @act_library, @area_borrow, @pos_lib_material, @u_liuzj, @today, TIMESTAMP(@today, '14:00:00'), TIMESTAMP(@today, '17:30:00'), 'CONFIRMED'),
    (@plan_carnival, @act_carnival, @area_stage, @pos_car_order, @u_chen, @tomorrow, TIMESTAMP(@tomorrow, '09:00:00'), TIMESTAMP(@tomorrow, '12:00:00'), 'WAIT_CONFIRM'),
    (@plan_carnival, @act_carnival, @area_stage, @pos_car_order, @u_wen, @tomorrow, TIMESTAMP(@tomorrow, '09:00:00'), TIMESTAMP(@tomorrow, '12:00:00'), 'WAIT_CONFIRM'),
    (@plan_carnival, @act_carnival, @area_booth, @pos_car_info, @u_liuxy, @tomorrow, TIMESTAMP(@tomorrow, '09:00:00'), TIMESTAMP(@tomorrow, '12:00:00'), 'WAIT_CONFIRM'),
    (@plan_carnival, @act_carnival, @area_booth, @pos_car_info, @u_li, @tomorrow, TIMESTAMP(@tomorrow, '09:00:00'), TIMESTAMP(@tomorrow, '12:00:00'), 'WAIT_CONFIRM'),
    (@plan_carnival, @act_carnival, @area_logistics, @pos_car_material, @u_liuzj, @tomorrow, TIMESTAMP(@tomorrow, '14:00:00'), TIMESTAMP(@tomorrow, '17:30:00'), 'WAIT_CONFIRM'),
    (@plan_carnival, @act_carnival, @area_stage, @pos_car_photo, @u_lv, @tomorrow, TIMESTAMP(@tomorrow, '14:00:00'), TIMESTAMP(@tomorrow, '17:30:00'), 'WAIT_CONFIRM'),
    (@plan_carnival, @act_carnival, @area_logistics, @pos_car_medical, @u_lin, @tomorrow, TIMESTAMP(@tomorrow, '14:00:00'), TIMESTAMP(@tomorrow, '17:30:00'), 'WAIT_CONFIRM');

SET @asg_wang_morning = (SELECT id FROM schedule_assignment WHERE plan_id = @plan_library AND position_id = @pos_lib_guide AND user_id = @u_wang);
SET @asg_liuxy_morning = (SELECT id FROM schedule_assignment WHERE plan_id = @plan_library AND position_id = @pos_lib_guide AND user_id = @u_liuxy);
SET @asg_chen_morning = (SELECT id FROM schedule_assignment WHERE plan_id = @plan_library AND position_id = @pos_lib_order AND user_id = @u_chen);
SET @asg_wen_morning = (SELECT id FROM schedule_assignment WHERE plan_id = @plan_library AND position_id = @pos_lib_order AND user_id = @u_wen);
SET @asg_li_morning = (SELECT id FROM schedule_assignment WHERE plan_id = @plan_library AND position_id = @pos_lib_info AND user_id = @u_li);
SET @asg_wang_afternoon = (SELECT id FROM schedule_assignment WHERE plan_id = @plan_library AND position_id = @pos_lib_reading AND user_id = @u_wang);
SET @asg_lv_afternoon = (SELECT id FROM schedule_assignment WHERE plan_id = @plan_library AND position_id = @pos_lib_photo AND user_id = @u_lv);
SET @asg_liuzj_afternoon = (SELECT id FROM schedule_assignment WHERE plan_id = @plan_library AND position_id = @pos_lib_material AND user_id = @u_liuzj);

INSERT INTO checkin_record
    (assignment_id, activity_id, position_id, user_id, checkin_type, checkin_status, checkin_time, longitude, latitude, qr_code)
VALUES
    (@asg_wang_morning, @act_library, @pos_lib_guide, @u_wang, 'CHECK_IN', 'NORMAL', TIMESTAMP(@today, '08:56:00'), 113.3651200, 22.9421100, 'DEMO-QR-LIB-GUIDE-IN'),
    (@asg_wang_morning, @act_library, @pos_lib_guide, @u_wang, 'CHECK_OUT', 'NORMAL', TIMESTAMP(@today, '12:04:00'), 113.3651800, 22.9421800, 'DEMO-QR-LIB-GUIDE-OUT'),
    (@asg_liuxy_morning, @act_library, @pos_lib_guide, @u_liuxy, 'CHECK_IN', 'NORMAL', TIMESTAMP(@today, '08:59:00'), 113.3651600, 22.9421500, 'DEMO-QR-LIB-GUIDE-IN'),
    (@asg_chen_morning, @act_library, @pos_lib_order, @u_chen, 'CHECK_IN', 'NORMAL', TIMESTAMP(@today, '08:54:00'), 113.3652300, 22.9420500, 'DEMO-QR-LIB-ORDER-IN'),
    (@asg_chen_morning, @act_library, @pos_lib_order, @u_chen, 'CHECK_OUT', 'NORMAL', TIMESTAMP(@today, '12:03:00'), 113.3652600, 22.9420900, 'DEMO-QR-LIB-ORDER-OUT'),
    (@asg_wen_morning, @act_library, @pos_lib_order, @u_wen, 'CHECK_IN', 'NORMAL', TIMESTAMP(@today, '09:02:00'), 113.3652500, 22.9421600, 'DEMO-QR-LIB-ORDER-IN'),
    (@asg_li_morning, @act_library, @pos_lib_info, @u_li, 'CHECK_IN', 'PENDING', TIMESTAMP(@today, '09:18:00'), 113.3670200, 22.9430200, 'DEMO-QR-LIB-INFO-IN'),
    (@asg_wang_afternoon, @act_library, @pos_lib_reading, @u_wang, 'CHECK_IN', 'NORMAL', TIMESTAMP(@today, '13:56:00'), 113.3665000, 22.9445000, 'DEMO-QR-LIB-READING-IN'),
    (@asg_lv_afternoon, @act_library, @pos_lib_photo, @u_lv, 'CHECK_IN', 'NORMAL', TIMESTAMP(@today, '13:58:00'), 113.3668000, 22.9447000, 'DEMO-QR-LIB-PHOTO-IN');

INSERT INTO location_record (activity_id, user_id, longitude, latitude, location_time, status)
VALUES
    (@act_library, @u_wang, 113.3665000, 22.9445000, DATE_SUB(@now, INTERVAL 4 MINUTE), 'ONLINE'),
    (@act_library, @u_chen, 113.3652600, 22.9420900, DATE_SUB(@now, INTERVAL 36 MINUTE), 'ONLINE'),
    (@act_library, @u_liuxy, 113.3651600, 22.9421500, DATE_SUB(@now, INTERVAL 18 MINUTE), 'ONLINE'),
    (@act_library, @u_wen, 113.3652500, 22.9421600, DATE_SUB(@now, INTERVAL 12 MINUTE), 'ONLINE'),
    (@act_library, @u_li, 113.3670200, 22.9430200, DATE_SUB(@now, INTERVAL 90 MINUTE), 'OFFLINE'),
    (@act_library, @u_lv, 113.3668000, 22.9447000, DATE_SUB(@now, INTERVAL 5 MINUTE), 'ONLINE'),
    (@act_library, @u_liuzj, 113.3669000, 22.9438000, DATE_SUB(@now, INTERVAL 50 MINUTE), 'ONLINE');

INSERT INTO user_realtime_status (activity_id, user_id, status, longitude, latitude, last_report_time)
VALUES
    (@act_library, @u_wang, 'WORKING', 113.3665000, 22.9445000, DATE_SUB(@now, INTERVAL 4 MINUTE)),
    (@act_library, @u_chen, 'FREE', 113.3652600, 22.9420900, DATE_SUB(@now, INTERVAL 36 MINUTE)),
    (@act_library, @u_liuxy, 'WORKING', 113.3651600, 22.9421500, DATE_SUB(@now, INTERVAL 18 MINUTE)),
    (@act_library, @u_wen, 'WORKING', 113.3652500, 22.9421600, DATE_SUB(@now, INTERVAL 12 MINUTE)),
    (@act_library, @u_li, 'OFFLINE', 113.3670200, 22.9430200, DATE_SUB(@now, INTERVAL 90 MINUTE)),
    (@act_library, @u_lv, 'WORKING', 113.3668000, 22.9447000, DATE_SUB(@now, INTERVAL 5 MINUTE)),
    (@act_library, @u_liuzj, 'FREE', 113.3669000, 22.9438000, DATE_SUB(@now, INTERVAL 50 MINUTE)),
    (@act_carnival, @u_zhang, 'FREE', 113.3740000, 22.9485000, DATE_SUB(@now, INTERVAL 15 MINUTE));

INSERT INTO dispatch_task
    (activity_id, area_id, position_id, required_count, reason, dispatch_status, created_by, finished_time)
VALUES
    (@act_carnival, @area_stage, @pos_car_order, 1, '主舞台秩序岗需求 3 人，目前预排 2 人，需要补员', 'PENDING', @admin_id, NULL),
    (@act_carnival, @area_logistics, @pos_car_material, 1, '物资补给岗需求 2 人，目前预排 1 人，建议追加后勤志愿者', 'PROCESSING', @admin_id, NULL);

SET @task_order = (SELECT id FROM dispatch_task WHERE activity_id = @act_carnival AND position_id = @pos_car_order ORDER BY id DESC LIMIT 1);
SET @task_material = (SELECT id FROM dispatch_task WHERE activity_id = @act_carnival AND position_id = @pos_car_material ORDER BY id DESC LIMIT 1);

INSERT INTO dispatch_recommendation (dispatch_task_id, user_id, distance_meter, match_score, recommend_status)
VALUES
    (@task_order, @u_wang, 420.50, 88.00, 'RECOMMENDED'),
    (@task_order, @u_huang, 310.00, 62.00, 'RECOMMENDED'),
    (@task_material, @u_zhang, 180.00, 91.00, 'RECOMMENDED'),
    (@task_material, @u_liuzj, 260.00, 84.00, 'RECOMMENDED');

INSERT INTO settlement_bill
    (bill_no, activity_id, user_id, total_work_minutes, base_amount, hour_amount, reward_amount, deduct_amount, total_amount, bill_status)
VALUES
    (CONCAT('DEMO-BILL-', DATE_FORMAT(@today, '%Y%m%d'), '-WANG'), @act_library, @u_wang, 398, 0.00, 70.00, 10.00, 0.00, 80.00, 'CONFIRMED'),
    (CONCAT('DEMO-BILL-', DATE_FORMAT(@today, '%Y%m%d'), '-CHEN'), @act_library, @u_chen, 189, 0.00, 35.00, 5.00, 0.00, 40.00, 'PAID');

SET @bill_wang = (SELECT id FROM settlement_bill WHERE bill_no = CONCAT('DEMO-BILL-', DATE_FORMAT(@today, '%Y%m%d'), '-WANG'));
SET @bill_chen = (SELECT id FROM settlement_bill WHERE bill_no = CONCAT('DEMO-BILL-', DATE_FORMAT(@today, '%Y%m%d'), '-CHEN'));

INSERT INTO settlement_detail (bill_id, assignment_id, position_id, work_minutes, amount, remark)
VALUES
    (@bill_wang, @asg_wang_morning, @pos_lib_guide, 188, 35.00, '上午入馆引导岗，按签到签退计算'),
    (@bill_wang, @asg_wang_afternoon, @pos_lib_reading, 210, 45.00, '下午亲子阅读协助岗，含现场奖励'),
    (@bill_chen, @asg_chen_morning, @pos_lib_order, 189, 40.00, '秩序维护岗已确认支付');

INSERT INTO payment_record (bill_id, pay_no, pay_channel, pay_amount, pay_status, pay_time)
VALUES
    (@bill_chen, CONCAT('DEMO-PAY-', DATE_FORMAT(@today, '%Y%m%d'), '-CHEN'), 'OFFLINE', 40.00, 'SUCCESS', DATE_SUB(@now, INTERVAL 30 MINUTE));

INSERT INTO message_notice
    (receiver_id, event_key, notice_type, title, content, send_channel, send_status, send_time, is_read)
VALUES
    (@u_wang, CONCAT('DEMO-NOTICE-SCHEDULE-', @plan_library, '-', @u_wang), 'SCHEDULE_PUBLISHED', '排班已发布', '你已被安排到城市图书馆周末志愿服务，请按时到入口接待区签到。', 'WECHAT', 'SUCCESS', DATE_SUB(@now, INTERVAL 2 HOUR), 1),
    (@u_li, CONCAT('DEMO-NOTICE-CHECKIN-PENDING-', @asg_li_morning), 'CHECKIN_REVIEW', '签到待复核', '你的信息咨询岗签到记录需要管理员复核定位信息。', 'WECHAT', 'SUCCESS', DATE_SUB(@now, INTERVAL 1 HOUR), 0),
    (@u_chen, CONCAT('DEMO-NOTICE-BILL-', @bill_chen), 'SETTLEMENT_PAID', '补贴已支付', '图书馆志愿服务补贴 40.00 元已登记为已支付。', 'SMS', 'SUCCESS', DATE_SUB(@now, INTERVAL 25 MINUTE), 1),
    (@u_zhang, CONCAT('DEMO-NOTICE-DISPATCH-', @task_material), 'DISPATCH_RECOMMEND', '调度推荐提醒', '社区公益嘉年华物资补给岗存在缺口，系统推荐你参与补位。', 'WECHAT', 'WAIT_SEND', NULL, 0);

INSERT INTO mq_event_log (topic, event_key, event_type, payload, event_status, retry_count, last_error)
VALUES
    ('schedule-topic', CONCAT('DEMO-MQ-SCHEDULE-', @plan_library), 'SCHEDULE_PUBLISHED',
     CONCAT('{\"planId\":', @plan_library, ',\"activityId\":', @act_library, '}'), 'SENT', 0, NULL),
    ('checkin-topic', CONCAT('DEMO-MQ-CHECKIN-', @asg_wang_morning), 'CHECKIN_COMPLETED',
     CONCAT('{\"assignmentId\":', @asg_wang_morning, ',\"userId\":', @u_wang, '}'), 'SENT', 0, NULL),
    ('dispatch-topic', CONCAT('DEMO-MQ-DISPATCH-', @task_order), 'DISPATCH_SHORTAGE_DETECTED',
     CONCAT('{\"dispatchTaskId\":', @task_order, ',\"requiredCount\":1}'), 'NEW', 0, NULL);

INSERT INTO ai_schedule_job (activity_id, job_no, job_type, job_status, input_payload, output_payload, error_message)
VALUES
    (@act_carnival, CONCAT('DEMO-AI-', DATE_FORMAT(@tomorrow, '%Y%m%d'), '-RISK'), 'RISK_PREDICTION', 'SUCCESS',
     CONCAT('{\"activityId\":', @act_carnival, ',\"source\":\"demo-data\"}'),
     '{\"shortageRisks\":2,\"attritionRisks\":1,\"areaRisks\":2}', NULL);

INSERT INTO risk_prediction
    (activity_id, area_id, position_id, risk_type, risk_level, risk_score, prediction_time, suggestion)
VALUES
    (@act_carnival, @area_stage, @pos_car_order, 'SHORTAGE', 'HIGH', 86.00, @now, '主舞台区秩序岗仍缺 1 人，建议优先联系具备 ORDER 技能且服务时间覆盖上午的志愿者。'),
    (@act_carnival, @area_logistics, @pos_car_material, 'SHORTAGE', 'MEDIUM', 72.00, @now, '物资补给岗下午缺 1 人，可从后勤保障或物资发放技能志愿者中补位。'),
    (@act_carnival, @area_logistics, NULL, 'AREA_RISK', 'HIGH', 81.00, @now, '后勤物资区存在岗位缺口和补给压力，建议安排现场管理员重点关注。'),
    (@act_library, @area_borrow, @pos_lib_info, 'ATTRITION', 'MEDIUM', 65.00, @now, '信息咨询岗存在一条待复核签到，建议签到管理员及时确认定位。');

SELECT
    'DEMO_FULL_FLOW_READY' AS result,
    @act_library AS library_activity_id,
    @act_carnival AS carnival_activity_id,
    @plan_library AS library_plan_id,
    @plan_carnival AS carnival_plan_id;
