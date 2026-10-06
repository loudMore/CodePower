-- V112: Persist queue timings and Judge0 node usage for cluster observability.

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name = 'queued_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `submissions` ADD COLUMN `queued_at` DATETIME NULL COMMENT ''评测入队时间'' AFTER `created_at`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name = 'started_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `submissions` ADD COLUMN `started_at` DATETIME NULL COMMENT ''评测开始时间'' AFTER `queued_at`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name = 'finished_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `submissions` ADD COLUMN `finished_at` DATETIME NULL COMMENT ''评测完成时间'' AFTER `started_at`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name = 'judge_node'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `submissions` ADD COLUMN `judge_node` VARCHAR(255) NULL COMMENT ''执行评测的Judge0节点'' AFTER `finished_at`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name = 'queue_wait_ms'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `submissions` ADD COLUMN `queue_wait_ms` INT NULL COMMENT ''入队等待毫秒'' AFTER `judge_node`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name = 'judge_duration_ms'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `submissions` ADD COLUMN `judge_duration_ms` INT NULL COMMENT ''评测执行毫秒'' AFTER `queue_wait_ms`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND index_name = 'idx_submissions_status_created_id'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `submissions` ADD INDEX `idx_submissions_status_created_id` (`status`, `created_at`, `id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND index_name = 'idx_submissions_judge_node_finished'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'submissions'
      AND column_name IN ('judge_node', 'finished_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 2,
    'ALTER TABLE `submissions` ADD INDEX `idx_submissions_judge_node_finished` (`judge_node`, `finished_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
