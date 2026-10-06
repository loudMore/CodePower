-- V13: Contest enhancements + Problem Sets
-- 说明：早期该版本只保留了注释，占位但未真正落库。
-- 这里改为幂等 DDL，确保全新数据库按 Flyway 顺序初始化时不会在 V19/V21 之前缺表缺列。

-- 1. contests 补充竞赛增强字段
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'contests'
    AND column_name = 'allowed_languages'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE contests ADD COLUMN allowed_languages VARCHAR(255) DEFAULT NULL COMMENT ''允许提交的语言列表，逗号分隔'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'contests'
    AND column_name = 'is_official'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE contests ADD COLUMN is_official TINYINT NOT NULL DEFAULT 0 COMMENT ''是否官方赛事'' AFTER allowed_languages',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. submissions 增加 contest_id，允许赛内提交和赛外提交共存
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'submissions'
    AND column_name = 'contest_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE submissions ADD COLUMN contest_id BIGINT UNSIGNED NULL COMMENT ''所属竞赛ID，可为空'' AFTER test_results',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'submissions'
    AND index_name = 'idx_submissions_contest_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_submissions_contest_created ON submissions(contest_id, created_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 题目集主表
CREATE TABLE IF NOT EXISTS problem_sets (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  description TEXT NULL,
  type VARCHAR(32) NOT NULL DEFAULT 'TRAINING' COMMENT 'TRAINING/EXAM/COLLECTION',
  creator_id BIGINT UNSIGNED NOT NULL,
  is_public TINYINT NOT NULL DEFAULT 1,
  problem_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_problem_sets_creator (creator_id),
  KEY idx_problem_sets_public_deleted (is_public, deleted),
  CONSTRAINT fk_problem_sets_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目集';

-- 4. 题目集成员表
CREATE TABLE IF NOT EXISTS problem_set_items (
  set_id BIGINT UNSIGNED NOT NULL,
  problem_id BIGINT UNSIGNED NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  added_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (set_id, problem_id),
  KEY idx_problem_set_items_problem (problem_id),
  KEY idx_problem_set_items_set_sort (set_id, sort_order),
  CONSTRAINT fk_problem_set_items_set FOREIGN KEY (set_id) REFERENCES problem_sets(id) ON DELETE CASCADE,
  CONSTRAINT fk_problem_set_items_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目集-题目关联';
