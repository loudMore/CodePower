-- V4 indexes and cleanup

UPDATE problem_solutions
SET title = CONCAT('官方', language, '题解')
WHERE is_official = 1
  AND (title IS NULL OR TRIM(title) = '');

UPDATE problem_solutions ps
JOIN (
  SELECT problem_id, language, MIN(id) AS keep_id
  FROM problem_solutions
  WHERE is_official = 1 AND deleted = 0
  GROUP BY problem_id, language
  HAVING COUNT(*) > 1
) d ON d.problem_id = ps.problem_id
   AND d.language = ps.language
   AND ps.id <> d.keep_id
SET ps.deleted = 1,
    ps.updated_at = CURRENT_TIMESTAMP;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'problem_solutions'
    AND index_name = 'idx_problem_solutions_problem_official'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_problem_solutions_problem_official ON problem_solutions(problem_id, is_official, deleted)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'problem_solutions'
    AND index_name = 'idx_problem_solutions_user_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_problem_solutions_user_created ON problem_solutions(user_id, deleted, created_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'submissions'
    AND index_name = 'idx_submissions_user_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_submissions_user_created ON submissions(user_id, created_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'submissions'
    AND index_name = 'idx_submissions_problem_status_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_submissions_problem_status_created ON submissions(problem_id, status, created_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'judge_test_cases'
    AND index_name = 'idx_judge_test_cases_problem_hidden_order'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_judge_test_cases_problem_hidden_order ON judge_test_cases(problem_id, is_hidden, order_num)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE solution_codes COMMENT = 'DEPRECATED: use problem_solutions as the single source of truth.';
