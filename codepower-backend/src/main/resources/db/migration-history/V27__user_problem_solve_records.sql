-- V27: 题目首通长期记忆
-- 用途：独立保存用户对题目的首通与最近一次通过信息，避免清理 submissions 后丢失首通判断。

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
    s.user_id,
    s.problem_id,
    (
        SELECT s1.id
        FROM submissions s1
        WHERE s1.user_id = s.user_id
          AND s1.problem_id = s.problem_id
          AND s1.status = 'ACCEPTED'
          AND s1.contest_id IS NULL
        ORDER BY s1.created_at ASC, s1.id ASC
        LIMIT 1
    ) AS first_accepted_submission_id,
    MIN(s.created_at) AS first_accepted_at,
    (
        SELECT s2.id
        FROM submissions s2
        WHERE s2.user_id = s.user_id
          AND s2.problem_id = s.problem_id
          AND s2.status = 'ACCEPTED'
          AND s2.contest_id IS NULL
        ORDER BY s2.created_at DESC, s2.id DESC
        LIMIT 1
    ) AS latest_accepted_submission_id,
    MAX(s.created_at) AS latest_accepted_at,
    COUNT(*) AS accepted_count
FROM submissions s
WHERE s.status = 'ACCEPTED'
  AND s.contest_id IS NULL
GROUP BY s.user_id, s.problem_id
ON DUPLICATE KEY UPDATE
first_accepted_submission_id = VALUES(first_accepted_submission_id),
first_accepted_at = VALUES(first_accepted_at),
latest_accepted_submission_id = VALUES(latest_accepted_submission_id),
latest_accepted_at = VALUES(latest_accepted_at),
accepted_count = VALUES(accepted_count);
