-- V10: 等级经验体系 + AI配额 + 签到 + 每日任务

-- 1. users 表新增字段
ALTER TABLE users
  ADD COLUMN level INT NOT NULL DEFAULT 1,
  ADD COLUMN exp INT NOT NULL DEFAULT 0,
  ADD COLUMN daily_ai_quota INT NOT NULL DEFAULT 5,
  ADD COLUMN daily_ai_used INT NOT NULL DEFAULT 0,
  ADD COLUMN quota_reset_date DATE DEFAULT NULL;

-- 2. 等级配置表
CREATE TABLE IF NOT EXISTS level_config (
  level INT NOT NULL PRIMARY KEY,
  required_exp INT NOT NULL,
  base_ai_quota INT NOT NULL,
  title VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO level_config (level, required_exp, base_ai_quota, title) VALUES
  (1,  0,     5,  '初学者'),
  (2,  100,   8,  '入门选手'),
  (3,  300,   12, '进阶选手'),
  (4,  600,   16, '熟练选手'),
  (5,  1000,  20, '高手'),
  (6,  1600,  25, '资深高手'),
  (7,  2500,  30, '专家'),
  (8,  3500,  36, '大师'),
  (9,  5000,  43, '宗师'),
  (10, 7000,  50, '传奇');

-- 3. 签到表
CREATE TABLE IF NOT EXISTS user_check_ins (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  check_in_date DATE NOT NULL,
  streak INT NOT NULL DEFAULT 1,
  exp_earned INT NOT NULL DEFAULT 0,
  ai_bonus INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, check_in_date),
  CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 每日任务定义表
CREATE TABLE IF NOT EXISTS daily_tasks (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  task_key VARCHAR(50) NOT NULL UNIQUE,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  exp_reward INT NOT NULL DEFAULT 5,
  ai_reward INT NOT NULL DEFAULT 2,
  required_count INT NOT NULL DEFAULT 1,
  active TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO daily_tasks (task_key, title, description, exp_reward, ai_reward, required_count) VALUES
  ('submit_code',  '提交一次代码',   '提交任意一道题目的代码',       5,  2, 1),
  ('pass_problem', '通过一道题目',   'AC一道编程题',              15,  3, 1),
  ('pass_three',   '通过三道题目',   '累计AC三道不同的编程题',      30,  5, 3),
  ('ai_chat',      '使用AI助手',    '与AI助手进行一次对话',         3,  0, 1);

-- 5. 用户每日任务进度表
CREATE TABLE IF NOT EXISTS user_daily_task_progress (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  task_id BIGINT UNSIGNED NOT NULL,
  task_date DATE NOT NULL,
  current_count INT NOT NULL DEFAULT 0,
  completed TINYINT(1) NOT NULL DEFAULT 0,
  rewarded TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_task_date (user_id, task_id, task_date),
  CONSTRAINT fk_daily_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_daily_progress_task FOREIGN KEY (task_id) REFERENCES daily_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
