-- =====================================================================
-- V26: 性能索引增强
-- 用途：提升竞赛排行榜、用户提交历史、数据分析、私信未读统计等高频查询性能
-- =====================================================================

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND index_name = 'idx_submissions_contest_user_problem_time'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `submissions` ADD INDEX `idx_submissions_contest_user_problem_time` (`contest_id`, `user_id`, `problem_id`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND index_name = 'idx_submissions_user_created_at'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `submissions` ADD INDEX `idx_submissions_user_created_at` (`user_id`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND index_name = 'idx_submissions_problem_status_created_at'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `submissions` ADD INDEX `idx_submissions_problem_status_created_at` (`problem_id`, `status`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND index_name = 'idx_submissions_created_at_user'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `submissions` ADD INDEX `idx_submissions_created_at_user` (`created_at`, `user_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'private_messages'
      AND index_name = 'idx_pm_to_from_read'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `private_messages` ADD INDEX `idx_pm_to_from_read` (`to_user_id`, `from_user_id`, `is_read`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'private_messages'
      AND index_name = 'idx_pm_user_pair_latest'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `private_messages` ADD INDEX `idx_pm_user_pair_latest` (`to_user_id`, `created_at`, `id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'private_messages'
      AND index_name = 'idx_pm_from_pair_latest'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `private_messages` ADD INDEX `idx_pm_from_pair_latest` (`from_user_id`, `created_at`, `id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'contest_registrations'
      AND index_name = 'idx_cr_contest_user_registered'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `contest_registrations` ADD INDEX `idx_cr_contest_user_registered` (`contest_id`, `user_id`, `registered_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'contest_registrations'
      AND index_name = 'idx_cr_user_registered'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `contest_registrations` ADD INDEX `idx_cr_user_registered` (`user_id`, `registered_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'contest_problems'
      AND index_name = 'idx_cp_problem_contest'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `contest_problems` ADD INDEX `idx_cp_problem_contest` (`problem_id`, `contest_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'contest_problems'
      AND index_name = 'idx_cp_contest_sort'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `contest_problems` ADD INDEX `idx_cp_contest_sort` (`contest_id`, `sort_order`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notifications'
      AND index_name = 'idx_notif_user_created_at'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `notifications` ADD INDEX `idx_notif_user_created_at` (`user_id`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'notifications'
      AND index_name = 'idx_notif_user_read_created_at'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `notifications` ADD INDEX `idx_notif_user_read_created_at` (`user_id`, `is_read`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'system_announcements'
      AND index_name = 'idx_announcement_status_time'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `system_announcements` ADD INDEX `idx_announcement_status_time` (`status`, `start_time`, `end_time`, `priority`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_announcement_reads'
      AND index_name = 'idx_uar_announcement_user'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `user_announcement_reads` ADD INDEX `idx_uar_announcement_user` (`announcement_id`, `user_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'contest_registrations'
      AND column_name = 'entered_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `contest_registrations` ADD COLUMN `entered_at` DATETIME NULL COMMENT ''首次进入竞赛工作区时间'' AFTER `registered_at`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'contest_registrations'
      AND column_name = 'last_submission_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `contest_registrations` ADD COLUMN `last_submission_at` DATETIME NULL COMMENT ''最近一次竞赛提交时间'' AFTER `entered_at`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'contest_registrations'
      AND index_name = 'idx_cr_contest_entered'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `contest_registrations` ADD INDEX `idx_cr_contest_entered` (`contest_id`, `entered_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'contest_registrations'
      AND index_name = 'idx_cr_contest_last_submission'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `contest_registrations` ADD INDEX `idx_cr_contest_last_submission` (`contest_id`, `last_submission_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
