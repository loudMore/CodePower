-- Ensure the solve-record table exists in already-deployed databases.
-- Earlier environments may have skipped the V5 baseline migration while the
-- application code now relies on this table for durable accepted-problem stats.
CREATE TABLE IF NOT EXISTS user_problem_solve_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    problem_id BIGINT UNSIGNED NOT NULL,
    first_accepted_submission_id BIGINT UNSIGNED NOT NULL,
    first_accepted_at DATETIME NOT NULL,
    latest_accepted_submission_id BIGINT UNSIGNED NOT NULL,
    latest_accepted_at DATETIME NOT NULL,
    accepted_count INT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_problem_solve (user_id, problem_id),
    KEY idx_upsr_user_latest (user_id, latest_accepted_at),
    KEY idx_upsr_problem_latest (problem_id, latest_accepted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO user_problem_solve_records (
    user_id,
    problem_id,
    first_accepted_submission_id,
    first_accepted_at,
    latest_accepted_submission_id,
    latest_accepted_at,
    accepted_count
)
SELECT
    grouped.user_id,
    grouped.problem_id,
    first_submission.id AS first_accepted_submission_id,
    first_submission.created_at AS first_accepted_at,
    latest_submission.id AS latest_accepted_submission_id,
    latest_submission.created_at AS latest_accepted_at,
    grouped.accepted_count
FROM (
    SELECT
        user_id,
        problem_id,
        MIN(CONCAT(DATE_FORMAT(created_at, '%Y%m%d%H%i%s'), LPAD(id, 20, '0'))) AS first_key,
        MAX(CONCAT(DATE_FORMAT(created_at, '%Y%m%d%H%i%s'), LPAD(id, 20, '0'))) AS latest_key,
        COUNT(*) AS accepted_count
    FROM submissions
    WHERE status = 'ACCEPTED'
      AND contest_id IS NULL
      AND user_id IS NOT NULL
      AND problem_id IS NOT NULL
      AND created_at IS NOT NULL
    GROUP BY user_id, problem_id
) grouped
JOIN submissions first_submission
  ON first_submission.user_id = grouped.user_id
 AND first_submission.problem_id = grouped.problem_id
 AND first_submission.status = 'ACCEPTED'
 AND first_submission.contest_id IS NULL
 AND CONCAT(DATE_FORMAT(first_submission.created_at, '%Y%m%d%H%i%s'), LPAD(first_submission.id, 20, '0')) = grouped.first_key
JOIN submissions latest_submission
  ON latest_submission.user_id = grouped.user_id
 AND latest_submission.problem_id = grouped.problem_id
 AND latest_submission.status = 'ACCEPTED'
 AND latest_submission.contest_id IS NULL
 AND CONCAT(DATE_FORMAT(latest_submission.created_at, '%Y%m%d%H%i%s'), LPAD(latest_submission.id, 20, '0')) = grouped.latest_key
ON DUPLICATE KEY UPDATE
    first_accepted_submission_id = VALUES(first_accepted_submission_id),
    first_accepted_at = VALUES(first_accepted_at),
    latest_accepted_submission_id = VALUES(latest_accepted_submission_id),
    latest_accepted_at = VALUES(latest_accepted_at),
    accepted_count = VALUES(accepted_count);
