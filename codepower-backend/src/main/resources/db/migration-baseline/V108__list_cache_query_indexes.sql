-- V108: indexes for high-frequency list pages.
-- These checks keep the migration safe on databases created from either the
-- historical scripts or the current baseline schema.

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problems'
      AND index_name = 'idx_problems_public_list'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `problems` ADD INDEX `idx_problems_public_list` (`visibility`, `status`, `deleted`, `id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problems'
      AND index_name = 'idx_problems_public_difficulty'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `problems` ADD INDEX `idx_problems_public_difficulty` (`visibility`, `status`, `deleted`, `difficulty`, `id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_tags'
      AND index_name = 'idx_problem_tags_tag_problem'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE `problem_tags` ADD INDEX `idx_problem_tags_tag_problem` (`tag_id`, `problem_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_sets'
      AND index_name = 'idx_problem_sets_public_created'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_sets'
      AND column_name IN ('is_public', 'deleted', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 3,
    'ALTER TABLE `problem_sets` ADD INDEX `idx_problem_sets_public_created` (`is_public`, `deleted`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_sets'
      AND index_name = 'idx_problem_sets_creator_created'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_sets'
      AND column_name IN ('creator_id', 'deleted', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 3,
    'ALTER TABLE `problem_sets` ADD INDEX `idx_problem_sets_creator_created` (`creator_id`, `deleted`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_sets'
      AND index_name = 'idx_problem_sets_type_created'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_sets'
      AND column_name IN ('type', 'deleted', 'created_at')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 3,
    'ALTER TABLE `problem_sets` ADD INDEX `idx_problem_sets_type_created` (`type`, `deleted`, `created_at`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_set_items'
      AND index_name = 'idx_problem_set_items_set_sort_v2'
);
SET @cols_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'problem_set_items'
      AND column_name IN ('set_id', 'sort_order')
);
SET @sql := IF(@idx_exists = 0 AND @cols_exists = 2,
    'ALTER TABLE `problem_set_items` ADD INDEX `idx_problem_set_items_set_sort_v2` (`set_id`, `sort_order`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
