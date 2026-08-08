CREATE TABLE IF NOT EXISTS admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account VARCHAR(64) NOT NULL COMMENT '登录账号',
  password VARCHAR(128) NOT NULL COMMENT '登录密码',
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  real_name VARCHAR(64) NULL COMMENT '真实姓名',
  mobile VARCHAR(32) NULL COMMENT '手机号',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_user_account (account)
) COMMENT='后台管理员账号';

CREATE TABLE IF NOT EXISTS admin_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_role_code (role_code)
) COMMENT='后台角色';

CREATE TABLE IF NOT EXISTS admin_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_code VARCHAR(96) NOT NULL COMMENT '权限编码',
  permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
  parent_code VARCHAR(96) NULL COMMENT '父级权限编码',
  permission_type VARCHAR(32) NOT NULL DEFAULT 'MENU' COMMENT '权限类型',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_permission_code (permission_code)
) COMMENT='后台权限树';

CREATE TABLE IF NOT EXISTS admin_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_id BIGINT NOT NULL COMMENT '管理员ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_user_role (admin_id, role_id)
) COMMENT='后台账号角色关系';

CREATE TABLE IF NOT EXISTS admin_role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_role_permission (role_id, permission_id)
) COMMENT='后台角色权限关系';

INSERT INTO admin_user (account, password, username, real_name, mobile, status, is_deleted)
VALUES ('admin', 'change-me', 'admin', '超级管理员', NULL, 'ENABLED', 0)
ON DUPLICATE KEY UPDATE username = VALUES(username), real_name = VALUES(real_name), status = 'ENABLED', is_deleted = 0;

INSERT INTO admin_role (role_code, role_name, status, is_deleted) VALUES
('SUPER_ADMIN', '超级管理员', 'ENABLED', 0),
('ACTIVITY_MANAGER', '活动管理员', 'ENABLED', 0),
('SIGNUP_REVIEWER', '报名审核员', 'ENABLED', 0),
('SCHEDULE_MANAGER', '排班管理员', 'ENABLED', 0),
('CHECKIN_MANAGER', '签到管理员', 'ENABLED', 0),
('NOTICE_MANAGER', '通知管理员', 'ENABLED', 0),
('VIEWER', '只读查看', 'ENABLED', 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = 'ENABLED', is_deleted = 0;

INSERT INTO admin_permission (permission_code, permission_name, parent_code, permission_type, sort_no, status, is_deleted) VALUES
('dashboard:view', '调度总览', NULL, 'MENU', 10, 'ENABLED', 0),
('activity:manage', '活动管理', NULL, 'MENU', 20, 'ENABLED', 0),
('signup:review', '报名审核', 'activity:manage', 'MENU', 30, 'ENABLED', 0),
('area:manage', '服务区域', 'activity:manage', 'MENU', 40, 'ENABLED', 0),
('position:manage', '岗位管理', 'activity:manage', 'MENU', 50, 'ENABLED', 0),
('volunteer:manage', '志愿者管理', NULL, 'MENU', 60, 'ENABLED', 0),
('schedule:manage', '排班计划', NULL, 'MENU', 70, 'ENABLED', 0),
('dispatch:manage', '智能调度', NULL, 'MENU', 80, 'ENABLED', 0),
('checkin:manage', '签到管理', NULL, 'MENU', 90, 'ENABLED', 0),
('settlement:manage', '工时结算', NULL, 'MENU', 100, 'ENABLED', 0),
('message:manage', '通知中心', NULL, 'MENU', 110, 'ENABLED', 0),
('system:manage', '系统管理', NULL, 'MENU', 120, 'ENABLED', 0)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), parent_code = VALUES(parent_code),
permission_type = VALUES(permission_type), sort_no = VALUES(sort_no), status = 'ENABLED', is_deleted = 0;

INSERT INTO admin_user_role (admin_id, role_id, is_deleted)
SELECT u.id, r.id, 0
FROM admin_user u
INNER JOIN admin_role r ON r.role_code = 'SUPER_ADMIN'
WHERE u.account = 'admin'
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO admin_role_permission (role_id, permission_id, is_deleted)
SELECT r.id, p.id, 0
FROM admin_role r
INNER JOIN admin_permission p ON 1 = 1
WHERE r.role_code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO admin_role_permission (role_id, permission_id, is_deleted)
SELECT r.id, p.id, 0
FROM admin_role r
INNER JOIN admin_permission p ON p.permission_code IN ('dashboard:view', 'activity:manage', 'area:manage', 'position:manage')
WHERE r.role_code = 'ACTIVITY_MANAGER'
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO admin_role_permission (role_id, permission_id, is_deleted)
SELECT r.id, p.id, 0
FROM admin_role r
INNER JOIN admin_permission p ON p.permission_code IN ('dashboard:view', 'signup:review')
WHERE r.role_code = 'SIGNUP_REVIEWER'
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO admin_role_permission (role_id, permission_id, is_deleted)
SELECT r.id, p.id, 0
FROM admin_role r
INNER JOIN admin_permission p ON p.permission_code IN ('dashboard:view', 'schedule:manage', 'dispatch:manage')
WHERE r.role_code = 'SCHEDULE_MANAGER'
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO admin_role_permission (role_id, permission_id, is_deleted)
SELECT r.id, p.id, 0
FROM admin_role r
INNER JOIN admin_permission p ON p.permission_code IN ('dashboard:view', 'checkin:manage')
WHERE r.role_code = 'CHECKIN_MANAGER'
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO admin_role_permission (role_id, permission_id, is_deleted)
SELECT r.id, p.id, 0
FROM admin_role r
INNER JOIN admin_permission p ON p.permission_code IN ('dashboard:view', 'message:manage')
WHERE r.role_code = 'NOTICE_MANAGER'
ON DUPLICATE KEY UPDATE is_deleted = 0;

INSERT INTO admin_role_permission (role_id, permission_id, is_deleted)
SELECT r.id, p.id, 0
FROM admin_role r
INNER JOIN admin_permission p ON p.permission_code = 'dashboard:view'
WHERE r.role_code = 'VIEWER'
ON DUPLICATE KEY UPDATE is_deleted = 0;
