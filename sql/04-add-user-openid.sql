ALTER TABLE `user_info`
    ADD COLUMN `openid` VARCHAR(128) DEFAULT NULL COMMENT 'wechat miniapp openid' AFTER `mobile`;

ALTER TABLE `user_info`
    ADD UNIQUE KEY `uk_openid` (`openid`);
