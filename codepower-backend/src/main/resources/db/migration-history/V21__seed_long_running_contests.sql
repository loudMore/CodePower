-- V21: 预置长期竞赛（永久赛 / 月赛 / 周赛）

SET @admin_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);
SET @senior_id = (SELECT id FROM users WHERE username = 'wangwu' LIMIT 1);

-- 1. 每日练习赛（永久进行中）
INSERT INTO contests (title, description, type, start_time, end_time, duration_minutes, creator_id, status, is_official, is_public, invite_code, created_at, updated_at)
VALUES (
  '每日算法挑战',
  '长期开放的算法练习赛，包含各难度基础题目，适合日常刷题。无排名压力，随时加入随时练习！',
  'PRACTICE',
  '2025-01-01 00:00:00',
  '2099-12-31 23:59:59',
  0,
  @admin_id,
  'RUNNING',
  1,
  1,
  UPPER(SUBSTRING(MD5(RAND()), 1, 8)),
  NOW(), NOW()
);
SET @contest1 = LAST_INSERT_ID();

-- 2. 入门编程马拉松
INSERT INTO contests (title, description, type, start_time, end_time, duration_minutes, creator_id, status, is_official, is_public, invite_code, created_at, updated_at)
VALUES (
  '入门编程马拉松',
  '面向编程初学者的长期赛事，涵盖数组、字符串、循环等基础题型。完成全部题目即可获得结业证书。',
  'PRACTICE',
  '2025-01-01 00:00:00',
  '2099-12-31 23:59:59',
  0,
  @admin_id,
  'RUNNING',
  1,
  1,
  UPPER(SUBSTRING(MD5(RAND()), 1, 8)),
  NOW(), NOW()
);
SET @contest2 = LAST_INSERT_ID();

-- 3. 进阶算法挑战赛
INSERT INTO contests (title, description, type, start_time, end_time, duration_minutes, creator_id, status, is_official, is_public, invite_code, created_at, updated_at)
VALUES (
  '进阶算法挑战赛',
  '中高难度算法题集，涵盖动态规划、图论、贪心等高级算法。适合有一定基础的选手进阶提升。',
  'PRACTICE',
  '2025-03-01 00:00:00',
  '2099-12-31 23:59:59',
  0,
  @senior_id,
  'RUNNING',
  1,
  1,
  UPPER(SUBSTRING(MD5(RAND()), 1, 8)),
  NOW(), NOW()
);
SET @contest3 = LAST_INSERT_ID();

-- 4. 2025春季限时考试（即将开始的赛事示例）
INSERT INTO contests (title, description, type, start_time, end_time, duration_minutes, creator_id, status, is_official, is_public, invite_code, created_at, updated_at)
VALUES (
  '2025春季编程能力测试',
  '官方限时编程能力测评，2小时内完成全部题目。成绩将记入排名，前10%获得荣誉证书。',
  'EXAM',
  DATE_ADD(NOW(), INTERVAL 7 DAY),
  DATE_ADD(NOW(), INTERVAL 7 + 0.0833 DAY),
  120,
  @admin_id,
  'UPCOMING',
  1,
  1,
  UPPER(SUBSTRING(MD5(RAND()), 1, 8)),
  NOW(), NOW()
);
SET @contest4 = LAST_INSERT_ID();

-- 5. 数据结构专项训练（长期）
INSERT INTO contests (title, description, type, start_time, end_time, duration_minutes, creator_id, status, is_official, is_public, invite_code, created_at, updated_at)
VALUES (
  '数据结构专项训练',
  '系统性训练栈、队列、树、图等数据结构题目，从易到难逐步提升。长期有效，随时参与。',
  'PRACTICE',
  '2025-02-01 00:00:00',
  '2099-12-31 23:59:59',
  0,
  @senior_id,
  'RUNNING',
  1,
  1,
  UPPER(SUBSTRING(MD5(RAND()), 1, 8)),
  NOW(), NOW()
);
SET @contest5 = LAST_INSERT_ID();

-- 6. 已结束的赛事示例
INSERT INTO contests (title, description, type, start_time, end_time, duration_minutes, creator_id, status, is_official, is_public, invite_code, created_at, updated_at)
VALUES (
  '2025元旦算法邀请赛',
  '元旦特别赛事，已结束。可查看排名和题解。',
  'EXAM',
  '2025-01-01 09:00:00',
  '2025-01-01 12:00:00',
  180,
  @admin_id,
  'ENDED',
  1,
  1,
  UPPER(SUBSTRING(MD5(RAND()), 1, 8)),
  '2025-01-01 00:00:00', '2025-01-01 12:00:00'
);
SET @contest6 = LAST_INSERT_ID();

-- ========== 为各竞赛绑定题目 ==========

-- 每日算法挑战：简单+中等混合（取前10道题）
INSERT INTO contest_problems (contest_id, problem_id, sort_order, score)
SELECT @contest1, p.id, (@row1 := @row1 + 1), 100
FROM problems p, (SELECT @row1 := 0) r
WHERE p.difficulty IN ('EASY', 'MEDIUM') AND p.deleted = 0
ORDER BY p.id
LIMIT 10;

-- 入门编程马拉松：简单题（取8道）
INSERT INTO contest_problems (contest_id, problem_id, sort_order, score)
SELECT @contest2, p.id, (@row2 := @row2 + 1), 100
FROM problems p, (SELECT @row2 := 0) r
WHERE p.difficulty = 'EASY' AND p.deleted = 0
ORDER BY p.id
LIMIT 8;

-- 进阶算法挑战赛：中等+困难（取8道）
INSERT INTO contest_problems (contest_id, problem_id, sort_order, score)
SELECT @contest3, p.id, (@row3 := @row3 + 1), IF(p.difficulty = 'HARD', 150, 100)
FROM problems p, (SELECT @row3 := 0) r
WHERE p.difficulty IN ('MEDIUM', 'HARD') AND p.deleted = 0
ORDER BY p.id
LIMIT 8;

-- 数据结构专项训练（取6道）
INSERT INTO contest_problems (contest_id, problem_id, sort_order, score)
SELECT @contest5, p.id, (@row5 := @row5 + 1), 100
FROM problems p, (SELECT @row5 := 0) r
WHERE p.deleted = 0
ORDER BY RAND()
LIMIT 6;

-- 春季考试（取5道混合难度）
INSERT INTO contest_problems (contest_id, problem_id, sort_order, score)
SELECT @contest4, p.id, (@row4 := @row4 + 1),
  CASE p.difficulty WHEN 'EASY' THEN 80 WHEN 'MEDIUM' THEN 100 ELSE 150 END
FROM problems p, (SELECT @row4 := 0) r
WHERE p.deleted = 0
ORDER BY p.difficulty, p.id
LIMIT 5;

-- 元旦邀请赛（取4道）
INSERT INTO contest_problems (contest_id, problem_id, sort_order, score)
SELECT @contest6, p.id, (@row6 := @row6 + 1), 100
FROM problems p, (SELECT @row6 := 0) r
WHERE p.deleted = 0
ORDER BY p.id DESC
LIMIT 4;

-- ========== 模拟一些报名数据 ==========
INSERT INTO contest_registrations (contest_id, user_id, registered_at)
SELECT @contest1, u.id, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM users u WHERE u.username IN ('zhangsan', 'lisi', 'wangwu', 'zhaoliu', 'sunqi', 'codeMaster');

INSERT INTO contest_registrations (contest_id, user_id, registered_at)
SELECT @contest2, u.id, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY)
FROM users u WHERE u.username IN ('wujiu', 'zhengshi', 'algoKing', 'zhangsan');

INSERT INTO contest_registrations (contest_id, user_id, registered_at)
SELECT @contest3, u.id, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 20) DAY)
FROM users u WHERE u.username IN ('wangwu', 'codeMaster', 'zhouba');

INSERT INTO contest_registrations (contest_id, user_id, registered_at)
SELECT @contest5, u.id, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 45) DAY)
FROM users u WHERE u.username IN ('zhangsan', 'lisi', 'sunqi', 'wujiu', 'algoKing');
