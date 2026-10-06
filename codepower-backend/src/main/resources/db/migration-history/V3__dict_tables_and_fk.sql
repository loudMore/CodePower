-- V3 dictionary tables and enum-to-code normalization

CREATE TABLE IF NOT EXISTS dict_user_role (
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dict_problem_difficulty (
  code VARCHAR(16) NOT NULL,
  name VARCHAR(64) NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dict_problem_visibility (
  code VARCHAR(16) NOT NULL,
  name VARCHAR(64) NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dict_submission_status (
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dict_submission_result_status (
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO dict_user_role (code, name) VALUES
('NORMAL_USER', '普通用户'),
('PREMIUM_USER', '高级用户'),
('ADMIN', '管理员')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_problem_difficulty (code, name) VALUES
('简单', '简单'),
('普通', '普通'),
('困难', '困难'),
('极限', '极限')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_problem_visibility (code, name) VALUES
('PUBLIC', '公开'),
('PRIVATE', '私有')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_submission_status (code, name) VALUES
('PENDING', '待评测'),
('ACCEPTED', '通过'),
('WRONG_ANSWER', '答案错误'),
('TIME_LIMIT_EXCEEDED', '超时'),
('MEMORY_LIMIT_EXCEEDED', '超内存'),
('COMPILATION_ERROR', '编译错误'),
('RUNTIME_ERROR', '运行时错误')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_submission_result_status (code, name) VALUES
('ACCEPTED', '通过'),
('WRONG_ANSWER', '答案错误'),
('TIME_LIMIT_EXCEEDED', '超时'),
('MEMORY_LIMIT_EXCEEDED', '超内存'),
('COMPILATION_ERROR', '编译错误'),
('RUNTIME_ERROR', '运行时错误')
ON DUPLICATE KEY UPDATE name = VALUES(name);

UPDATE users
SET role = CASE
  WHEN role IN ('NORMAL_USER', 'PREMIUM_USER', 'ADMIN') THEN role
  ELSE 'NORMAL_USER'
END;

UPDATE problems
SET difficulty = CASE
  WHEN difficulty IN ('简单', '普通', '困难', '极限') THEN difficulty
  WHEN difficulty = '中等' THEN '普通'
  ELSE '普通'
END;

UPDATE problems
SET visibility = CASE
  WHEN visibility IN ('PUBLIC', 'PRIVATE') THEN visibility
  ELSE 'PRIVATE'
END;

UPDATE submissions
SET status = CASE
  WHEN status IN ('PENDING', 'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED', 'MEMORY_LIMIT_EXCEEDED', 'COMPILATION_ERROR', 'RUNTIME_ERROR') THEN status
  ELSE 'PENDING'
END;

UPDATE submission_results
SET status = CASE
  WHEN status IN ('ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED', 'MEMORY_LIMIT_EXCEEDED', 'COMPILATION_ERROR', 'RUNTIME_ERROR') THEN status
  ELSE 'RUNTIME_ERROR'
END;

ALTER TABLE users
  MODIFY COLUMN role VARCHAR(32) NOT NULL DEFAULT 'NORMAL_USER';

ALTER TABLE problems
  MODIFY COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT '普通',
  MODIFY COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE';

ALTER TABLE submissions
  MODIFY COLUMN status VARCHAR(64) NOT NULL DEFAULT 'PENDING';

ALTER TABLE submission_results
  MODIFY COLUMN status VARCHAR(64) NOT NULL;

ALTER TABLE users
  ADD CONSTRAINT fk_users_role_code
    FOREIGN KEY (role) REFERENCES dict_user_role (code);

ALTER TABLE problems
  ADD CONSTRAINT fk_problems_difficulty_code
    FOREIGN KEY (difficulty) REFERENCES dict_problem_difficulty (code),
  ADD CONSTRAINT fk_problems_visibility_code
    FOREIGN KEY (visibility) REFERENCES dict_problem_visibility (code);

ALTER TABLE submissions
  ADD CONSTRAINT fk_submissions_status_code
    FOREIGN KEY (status) REFERENCES dict_submission_status (code);

ALTER TABLE submission_results
  ADD CONSTRAINT fk_submission_results_status_code
    FOREIGN KEY (status) REFERENCES dict_submission_result_status (code);
