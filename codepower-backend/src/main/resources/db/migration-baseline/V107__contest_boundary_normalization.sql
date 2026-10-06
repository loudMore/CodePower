-- V107: 竞赛边界数据归一化
-- 1. 管理员创建的历史竞赛补齐官方标记，保证列表排序与前端徽标一致。
UPDATE contests c
JOIN users u ON c.creator_id = u.id
SET c.is_official = 1
WHERE u.role = 'ADMIN'
  AND (c.is_official IS NULL OR c.is_official <> 1);

-- 2. 0 或负数参赛人数上限统一按不限制处理。
UPDATE contests
SET max_participants = NULL
WHERE max_participants IS NOT NULL
  AND max_participants <= 0;

-- 3. 历史空值补默认边界，避免旧数据绕过前后端交互语义。
UPDATE contests
SET allow_paste = 1
WHERE allow_paste IS NULL;

UPDATE contests
SET is_public = 1
WHERE is_public IS NULL;

-- 4. 长期开放的练习赛不再保存一个很大的时长，前端统一显示为“长期开放”。
UPDATE contests
SET duration_minutes = 0
WHERE type <> 'EXAM'
  AND end_time >= '2099-01-01 00:00:00'
  AND (duration_minutes IS NULL OR duration_minutes <> 0);
