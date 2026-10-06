-- V22: 学习路径阶段关联真实题目（按顺序做题判断进度）

-- ===== C语言基础入门 (path 1) =====
-- Stage 2: 变量与数据类型 → 两数之和（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%两数之和%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'C语言基础入门' LIMIT 1)
  AND stage_order = 2 AND problem_id IS NULL;

-- Stage 3: 控制流语句 → 回文数（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%回文数%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'C语言基础入门' LIMIT 1)
  AND stage_order = 3 AND problem_id IS NULL;

-- Stage 4: 函数 → 阶乘（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%阶乘%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'C语言基础入门' LIMIT 1)
  AND stage_order = 4 AND problem_id IS NULL;

-- Stage 5: 数组与字符串 → 反转字符串（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%反转字符串%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'C语言基础入门' LIMIT 1)
  AND stage_order = 5 AND problem_id IS NULL;

-- Stage 6: 指针入门 → 数组中的最大值（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%最大值%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'C语言基础入门' LIMIT 1)
  AND stage_order = 6 AND problem_id IS NULL;

-- ===== Python编程入门 (path 2) =====
-- Stage 2: 控制流与循环 → FizzBuzz（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%FizzBuzz%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'Python编程入门' LIMIT 1)
  AND stage_order = 2 AND problem_id IS NULL;

-- Stage 3: 函数与模块 → 斐波那契数列（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%斐波那契%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'Python编程入门' LIMIT 1)
  AND stage_order = 3 AND problem_id IS NULL;

-- Stage 4: 列表、字典与集合 → 数组去重（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%去重%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'Python编程入门' LIMIT 1)
  AND stage_order = 4 AND problem_id IS NULL;

-- Stage 5: 文件操作与异常处理 → 字符串统计（简单）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%字符%计%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'Python编程入门' LIMIT 1)
  AND stage_order = 5 AND problem_id IS NULL;

-- Stage 6: 面向对象编程 → 矩阵转置（中等）
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%矩阵转置%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'Python编程入门' LIMIT 1)
  AND stage_order = 6 AND problem_id IS NULL;

-- ===== 数据结构与算法 (path 3) =====
-- Stage 1: 复杂度分析 → 两数之和
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%两数之和%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '数据结构与算法' LIMIT 1)
  AND stage_order = 1 AND problem_id IS NULL;

-- Stage 2: 链表 → 反转链表
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%反转链表%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '数据结构与算法' LIMIT 1)
  AND stage_order = 2 AND problem_id IS NULL;

-- Stage 3: 栈与队列 → 有效括号 / 栈相关
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%括号%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '数据结构与算法' LIMIT 1)
  AND stage_order = 3 AND problem_id IS NULL;

-- Stage 4: 树与二叉树 → 二叉树相关
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%二叉树%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '数据结构与算法' LIMIT 1)
  AND stage_order = 4 AND problem_id IS NULL;

-- Stage 5: 排序算法 → 排序相关
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%排序%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '数据结构与算法' LIMIT 1)
  AND stage_order = 5 AND problem_id IS NULL;

-- Stage 6: 搜索与回溯 → 全排列 / 回溯相关
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%全排列%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '数据结构与算法' LIMIT 1)
  AND stage_order = 6 AND problem_id IS NULL;

-- Stage 7: 动态规划 → 爬楼梯 / DP相关
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%爬楼梯%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '数据结构与算法' LIMIT 1)
  AND stage_order = 7 AND problem_id IS NULL;

-- ===== Java面向对象编程 (path 4) =====
-- Stage 2: 类与对象 → 两数之和
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%两数之和%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'Java面向对象编程' LIMIT 1)
  AND stage_order = 2 AND problem_id IS NULL;

-- Stage 3: 继承与多态 → 有效括号
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%括号%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'Java面向对象编程' LIMIT 1)
  AND stage_order = 3 AND problem_id IS NULL;

-- Stage 5: 集合框架 → 两个数组的交集
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%交集%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = 'Java面向对象编程' LIMIT 1)
  AND stage_order = 5 AND problem_id IS NULL;

-- ===== 算法竞赛入门 (path 5) =====
-- Stage 2: 数论基础 → 快速幂 / 素数
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%素数%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '算法竞赛入门' LIMIT 1)
  AND stage_order = 2 AND problem_id IS NULL;

-- Stage 3: 图论算法 → 图/最短路相关
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%最短%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '算法竞赛入门' LIMIT 1)
  AND stage_order = 3 AND problem_id IS NULL;

-- Stage 5: 动态规划进阶 → 最长递增子序列
UPDATE learning_path_stages SET problem_id = (
  SELECT id FROM problems WHERE title LIKE '%最长递增%' AND deleted = 0 LIMIT 1
) WHERE path_id = (SELECT id FROM learning_paths WHERE title = '算法竞赛入门' LIMIT 1)
  AND stage_order = 5 AND problem_id IS NULL;

-- 兜底：为仍无题目的阶段分配简单入门题
UPDATE learning_path_stages SET problem_id = (
  SELECT p.id FROM problems p WHERE p.difficulty = 'EASY' AND p.deleted = 0
  ORDER BY p.id LIMIT 1
) WHERE problem_id IS NULL;
