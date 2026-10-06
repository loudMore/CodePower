-- V109: indexes for hot read paths after public problem list caching.
-- All DDL is guarded so it can run on databases created from different
-- historical baselines without failing on existing indexes or missing columns.

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND index_name = 'idx_submissions_user_contest_problem_status_time'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name IN ('user_id', 'contest_id', 'problem_id', 'status', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 5,
    'ALTER TABLE `submissions` ADD INDEX `idx_submissions_user_contest_problem_status_time` (`user_id`, `contest_id`, `problem_id`, `status`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND index_name = 'idx_submissions_user_problem_contest_time'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name IN ('user_id', 'problem_id', 'contest_id', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 4,
    'ALTER TABLE `submissions` ADD INDEX `idx_submissions_user_problem_contest_time` (`user_id`, `problem_id`, `contest_id`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'comments'
      AND index_name = 'idx_comments_target_root_latest'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'comments'
      AND column_name IN ('target_type', 'target_id', 'parent_id', 'status', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 5,
    'ALTER TABLE `comments` ADD INDEX `idx_comments_target_root_latest` (`target_type`, `target_id`, `parent_id`, `status`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'comments'
      AND index_name = 'idx_comments_target_root_hot'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'comments'
      AND column_name IN ('target_type', 'target_id', 'parent_id', 'status', 'likes_count', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 6,
    'ALTER TABLE `comments` ADD INDEX `idx_comments_target_root_hot` (`target_type`, `target_id`, `parent_id`, `status`, `likes_count`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_solutions'
      AND index_name = 'idx_problem_solutions_problem_hot'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_solutions'
      AND column_name IN ('problem_id', 'status', 'deleted', 'is_official', 'likes', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 6,
    'ALTER TABLE `problem_solutions` ADD INDEX `idx_problem_solutions_problem_hot` (`problem_id`, `status`, `deleted`, `is_official`, `likes`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_solutions'
      AND index_name = 'idx_problem_solutions_problem_hot_likes_count'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_solutions'
      AND column_name IN ('problem_id', 'status', 'deleted', 'is_official', 'likes_count', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 6,
    'ALTER TABLE `problem_solutions` ADD INDEX `idx_problem_solutions_problem_hot_likes_count` (`problem_id`, `status`, `deleted`, `is_official`, `likes_count`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
