-- B2: beta1.0 最小必要种子数据
-- 用途：保证新环境可直接启动、登录、演示核心功能。

INSERT INTO dict_user_role (code, name) VALUES
('NORMAL_USER', '普通用户'),
('SENIOR_USER', '高级用户'),
('ADMIN', '管理员')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_problem_difficulty (code, name, sort_order) VALUES
('简单', '简单', 1),
('普通', '普通', 2),
('困难', '困难', 3)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order);

INSERT INTO dict_problem_visibility (code, name) VALUES
('PUBLIC', '公开'),
('PRIVATE', '私有')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_submission_status (code, name) VALUES
('PENDING', '等待中'),
('RUNNING', '运行中'),
('ACCEPTED', '通过'),
('WRONG_ANSWER', '答案错误'),
('TIME_LIMIT_EXCEEDED', '超时'),
('MEMORY_LIMIT_EXCEEDED', '超内存'),
('RUNTIME_ERROR', '运行错误'),
('COMPILE_ERROR', '编译错误'),
('SYSTEM_ERROR', '系统错误')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_submission_result_status (code, name) VALUES
('ACCEPTED', '通过'),
('WRONG_ANSWER', '答案错误'),
('TIME_LIMIT_EXCEEDED', '超时'),
('MEMORY_LIMIT_EXCEEDED', '超内存'),
('RUNTIME_ERROR', '运行错误'),
('COMPILE_ERROR', '编译错误'),
('SYSTEM_ERROR', '系统错误')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_contest_type (code, name) VALUES
('PRACTICE', '练习赛'),
('RATED', '积分赛'),
('EXAM', '考试')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_contest_status (code, name) VALUES
('UPCOMING', '未开始'),
('RUNNING', '进行中'),
('ENDED', '已结束')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO dict_notification_type (code, name) VALUES
('COMMENT_REPLY', '评论回复'),
('SYSTEM', '系统通知'),
('CONTEST', '竞赛通知')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO level_config (level, required_exp, base_ai_quota, title) VALUES
(1, 0, 5, '算法新手'),
(2, 100, 6, '初级探索者'),
(3, 250, 7, '勤学学徒'),
(4, 450, 8, '代码行者'),
(5, 700, 9, '解题达人'),
(6, 1000, 10, '进阶挑战者'),
(7, 1400, 11, '算法学者'),
(8, 1900, 12, '竞赛新锐'),
(9, 2500, 13, '逻辑大师'),
(10, 3200, 15, '代码专家'),
(11, 4000, 17, '系统构建者'),
(12, 4900, 19, '刷题精英'),
(13, 5900, 21, '算法分析师'),
(14, 7000, 23, '中级架构师'),
(15, 8200, 25, '高级挑战者'),
(16, 9500, 28, '竞赛强者'),
(17, 10900, 31, '解题宗师'),
(18, 12400, 34, '逻辑王者'),
(19, 14000, 37, '平台核心用户'),
(20, 15700, 40, '算法大师'),
(21, 17500, 44, '高级算法大师'),
(22, 19400, 48, '系统大师'),
(23, 21400, 52, '竞赛大师'),
(24, 23500, 56, '数据结构宗师'),
(25, 25700, 60, '题海领航者'),
(26, 28000, 68, '极限挑战者'),
(27, 30400, 76, '超凡工程师'),
(28, 32900, 84, '智慧贤者'),
(29, 35500, 92, '算法之王'),
(30, 90000, 100, '创世之神')
ON DUPLICATE KEY UPDATE
required_exp = VALUES(required_exp),
base_ai_quota = VALUES(base_ai_quota),
title = VALUES(title);

INSERT INTO daily_tasks (task_key, task_name, exp_reward, bonus_ai_points_reward, target_count, enabled, sort_order) VALUES
('solve_one_problem', '完成1道题目', 20, 1, 1, 1, 1),
('submit_three_times', '完成3次提交', 15, 1, 3, 1, 2),
('use_ai_once', '使用1次AI辅助', 10, 1, 1, 1, 3),
('check_in', '每日签到', 10, 0, 1, 1, 4)
ON DUPLICATE KEY UPDATE
task_name = VALUES(task_name),
exp_reward = VALUES(exp_reward),
bonus_ai_points_reward = VALUES(bonus_ai_points_reward),
target_count = VALUES(target_count),
enabled = VALUES(enabled),
sort_order = VALUES(sort_order);

INSERT IGNORE INTO users (id, username, email, password, role, status, level, exp, daily_ai_quota, daily_ai_used, quota_reset_date, bonus_ai_points, deleted)
VALUES (1, 'system', 'system@codepower.local', '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'ADMIN', 1, 30, 90000, 100, 0, NULL, 0, 0);

INSERT IGNORE INTO user_profiles (user_id, nickname, avatar_url, bio)
VALUES (1, '系统账号', '/avatars/avatar-1.svg', '系统内置账号');

UPDATE user_profiles
SET avatar_url = '/avatars/avatar-1.svg'
WHERE avatar_url IS NULL OR avatar_url = '';

INSERT INTO tags (name) VALUES
('数组'), ('链表'), ('动态规划'), ('字符串'), ('栈'), ('二分查找'), ('排序'), ('树'), ('图'), ('贪心'), ('双指针'), ('滑动窗口'), ('哈希表'), ('BFS'), ('DFS'), ('设计')
ON DUPLICATE KEY UPDATE name = VALUES(name);
