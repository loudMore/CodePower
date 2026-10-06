-- =====================================================================
-- V12: 通知系统扩展 + 私信功能 + 系统公告
-- 执行日期: 2026-04-21
-- =====================================================================

-- ===================== 扩展通知类型字典 =====================

INSERT INTO `dict_notification_type` (`code`, `name`) VALUES
('WELCOME', '欢迎通知'),
('LEVEL_UP', '升级祝贺'),
('CHECK_IN_MILESTONE', '签到里程碑'),
('AC_MILESTONE', 'AC里程碑'),
('CONTEST_REGISTER', '竞赛报名'),
('CONTEST_REMINDER', '竞赛提醒'),
('CONTEST_END', '竞赛结束'),
('UPGRADE_APPROVED', '升级申请通过'),
('UPGRADE_REJECTED', '升级申请拒绝'),
('SYSTEM_ANNOUNCEMENT', '系统公告')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ===================== 私信系统 =====================

CREATE TABLE `private_messages` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `from_user_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者',
    `to_user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0:未读, 1:已读',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_pm_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_pm_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `users`(`id`),
    INDEX `idx_pm_to_user_read` (`to_user_id`, `is_read`),
    INDEX `idx_pm_conversation` (`from_user_id`, `to_user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信表';

-- ===================== 系统公告 =====================

CREATE TABLE `system_announcements` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `type` VARCHAR(20) NOT NULL DEFAULT 'INFO' COMMENT 'INFO/WARNING/URGENT',
    `creator_id` BIGINT UNSIGNED NOT NULL COMMENT '发布者(管理员)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0:下架, 1:发布',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级，数字越大越靠前',
    `start_time` DATETIME DEFAULT NULL COMMENT '生效时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '失效时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_announcement_creator` FOREIGN KEY (`creator_id`) REFERENCES `users`(`id`),
    INDEX `idx_announcement_status_priority` (`status`, `priority` DESC, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- ===================== 用户已读公告记录 =====================

CREATE TABLE `user_announcement_reads` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `announcement_id` BIGINT UNSIGNED NOT NULL,
    `read_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `announcement_id`),
    CONSTRAINT `fk_uar_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_uar_announcement` FOREIGN KEY (`announcement_id`) REFERENCES `system_announcements`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户已读公告记录';
