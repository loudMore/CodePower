-- =====================================================================
-- V5: 补齐模块4-8所需的全部表 (评论、学习路线、竞赛、AI对话、用户能力、通知)
-- 执行日期: 2026-04-20
-- =====================================================================

-- ===================== 字典表补充 =====================

CREATE TABLE IF NOT EXISTS `dict_contest_type` (
    `code` VARCHAR(32) NOT NULL PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `dict_contest_type` (`code`, `name`) VALUES
('PRACTICE', '练习赛'),
('RATED', '积分赛'),
('EXAM', '考试');

CREATE TABLE IF NOT EXISTS `dict_contest_status` (
    `code` VARCHAR(32) NOT NULL PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `dict_contest_status` (`code`, `name`) VALUES
('DRAFT', '草稿'),
('UPCOMING', '即将开始'),
('RUNNING', '进行中'),
('ENDED', '已结束');

CREATE TABLE IF NOT EXISTS `dict_notification_type` (
    `code` VARCHAR(32) NOT NULL PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `dict_notification_type` (`code`, `name`) VALUES
('COMMENT_REPLY', '评论回复'),
('COMMENT_LIKE', '评论点赞'),
('SYSTEM', '系统通知'),
('CONTEST', '竞赛通知');

-- ===================== 模块4: 评论系统 =====================

CREATE TABLE `comments` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `target_type` VARCHAR(20) NOT NULL COMMENT '评论目标类型: SOLUTION/PROBLEM',
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT '评论目标ID',
    `user_id` BIGINT UNSIGNED NOT NULL,
    `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID, NULL表示顶级评论',
    `content` TEXT NOT NULL,
    `likes_count` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0:隐藏, 1:正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `comments`(`id`),
    INDEX `idx_comments_target` (`target_type`, `target_id`),
    INDEX `idx_comments_user` (`user_id`),
    INDEX `idx_comments_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comment_likes` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `comment_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `comment_id`),
    CONSTRAINT `fk_comment_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_comment_likes_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== 模块5: 学习路线 =====================

CREATE TABLE `learning_paths` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `difficulty` VARCHAR(20) NOT NULL DEFAULT '简单',
    `language` VARCHAR(30) NOT NULL DEFAULT '通用' COMMENT '编程语言方向',
    `cover_url` VARCHAR(500) DEFAULT NULL,
    `estimated_hours` INT DEFAULT NULL COMMENT '预计学习时长(小时)',
    `sort_order` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0:下架, 1:上架',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_learning_paths_difficulty` FOREIGN KEY (`difficulty`) REFERENCES `dict_problem_difficulty`(`code`),
    INDEX `idx_learning_paths_language` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `learning_path_stages` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `path_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `content` TEXT COMMENT '阶段学习内容(富文本/Markdown)',
    `stage_order` INT NOT NULL DEFAULT 0,
    `problem_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联题目(可选)',
    `resource_url` VARCHAR(500) DEFAULT NULL COMMENT '外部资源链接',
    `estimated_minutes` INT DEFAULT NULL COMMENT '预计学习时间(分钟)',
    CONSTRAINT `fk_stages_path` FOREIGN KEY (`path_id`) REFERENCES `learning_paths`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_stages_problem` FOREIGN KEY (`problem_id`) REFERENCES `problems`(`id`) ON DELETE SET NULL,
    INDEX `idx_stages_path_order` (`path_id`, `stage_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_learning_progress` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `path_id` BIGINT UNSIGNED NOT NULL,
    `stage_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/COMPLETED',
    `completed_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_progress_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_progress_path` FOREIGN KEY (`path_id`) REFERENCES `learning_paths`(`id`),
    CONSTRAINT `fk_progress_stage` FOREIGN KEY (`stage_id`) REFERENCES `learning_path_stages`(`id`),
    UNIQUE KEY `uk_user_stage` (`user_id`, `stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== 竞赛/考试 =====================

CREATE TABLE `contests` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `type` VARCHAR(32) NOT NULL DEFAULT 'PRACTICE',
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `duration_minutes` INT NOT NULL DEFAULT 120,
    `creator_id` BIGINT UNSIGNED NOT NULL,
    `password` VARCHAR(100) DEFAULT NULL COMMENT '加密的竞赛密码, NULL表示公开',
    `max_participants` INT DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_contests_creator` FOREIGN KEY (`creator_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_contests_type` FOREIGN KEY (`type`) REFERENCES `dict_contest_type`(`code`),
    CONSTRAINT `fk_contests_status` FOREIGN KEY (`status`) REFERENCES `dict_contest_status`(`code`),
    INDEX `idx_contests_status_time` (`status`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `contest_problems` (
    `contest_id` BIGINT UNSIGNED NOT NULL,
    `problem_id` BIGINT UNSIGNED NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '题目在竞赛中的顺序',
    `score` INT NOT NULL DEFAULT 100 COMMENT '该题总分',
    PRIMARY KEY (`contest_id`, `problem_id`),
    CONSTRAINT `fk_cp_contest` FOREIGN KEY (`contest_id`) REFERENCES `contests`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cp_problem` FOREIGN KEY (`problem_id`) REFERENCES `problems`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `contest_registrations` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `contest_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `registered_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_contest_user` (`contest_id`, `user_id`),
    CONSTRAINT `fk_cr_contest` FOREIGN KEY (`contest_id`) REFERENCES `contests`(`id`),
    CONSTRAINT `fk_cr_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== 模块6: AI 对话 =====================

CREATE TABLE `ai_conversations` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(200) DEFAULT '新对话',
    `problem_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联题目(可选)',
    `type` VARCHAR(20) NOT NULL DEFAULT 'CHAT' COMMENT 'CHAT/ANALYZE/GENERATE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ai_conv_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_ai_conv_problem` FOREIGN KEY (`problem_id`) REFERENCES `problems`(`id`) ON DELETE SET NULL,
    INDEX `idx_ai_conv_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ai_messages` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` BIGINT UNSIGNED NOT NULL,
    `role` VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    `content` TEXT NOT NULL,
    `tokens_used` INT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ai_msg_conv` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversations`(`id`) ON DELETE CASCADE,
    INDEX `idx_ai_msg_conv` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== 模块6/7: 用户能力模型 =====================

CREATE TABLE `user_abilities` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `tag_id` BIGINT UNSIGNED NOT NULL,
    `ability_score` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '能力分 0-100',
    `solved_count` INT NOT NULL DEFAULT 0 COMMENT '该标签下通过题数',
    `attempt_count` INT NOT NULL DEFAULT 0 COMMENT '该标签下尝试题数',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`),
    CONSTRAINT `fk_ability_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_ability_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== 通知系统 =====================

CREATE TABLE `notifications` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者',
    `type` VARCHAR(32) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT DEFAULT NULL,
    `related_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联资源ID(评论ID/竞赛ID等)',
    `is_read` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_notif_type` FOREIGN KEY (`type`) REFERENCES `dict_notification_type`(`code`),
    INDEX `idx_notif_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
