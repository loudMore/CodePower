-- V14: 题目统计字段 + 竞赛粘贴控制

-- 1. problems 增加提交/通过统计（定时批量更新，避免逐次写入）
ALTER TABLE problems
  ADD COLUMN submit_count INT NOT NULL DEFAULT 0 COMMENT '总提交次数',
  ADD COLUMN accept_count INT NOT NULL DEFAULT 0 COMMENT '通过人数(去重)',
  ADD COLUMN accept_rate  DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '通过率(百分比)';

-- 2. contests 增加粘贴控制（老师可禁止学生粘贴代码）
ALTER TABLE contests
  ADD COLUMN allow_paste TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许粘贴代码(0:禁止 1:允许)';

-- 3. 初始化已有题目的统计数据（一次性，后续由定时任务维护）
UPDATE problems p SET
  p.submit_count = COALESCE((SELECT COUNT(*) FROM submissions s WHERE s.problem_id = p.id), 0),
  p.accept_count = COALESCE((SELECT COUNT(DISTINCT s.user_id) FROM submissions s WHERE s.problem_id = p.id AND s.status = 'ACCEPTED'), 0);

UPDATE problems SET accept_rate = CASE WHEN submit_count > 0 THEN ROUND(accept_count * 100.0 / submit_count, 2) ELSE 0 END;
