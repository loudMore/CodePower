-- V19: 预建题目集 (使用种子题目组织8个官方题目集)

-- 题目集1: 编程入门基础
INSERT INTO problem_sets (title, description, type, creator_id, is_public, problem_count) VALUES
('编程入门基础', '适合零基础学习者的入门题集，涵盖基本I/O、简单数学运算和条件判断。', 'OFFICIAL', 1, 1, 10);
SET @sid = LAST_INSERT_ID();
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 1 FROM problems p WHERE p.title = 'A+B 问题' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 2 FROM problems p WHERE p.title = '判断奇偶' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 3 FROM problems p WHERE p.title = '温度转换' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 4 FROM problems p WHERE p.title = '数位之和' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 5 FROM problems p WHERE p.title = '计算阶乘' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 6 FROM problems p WHERE p.title = '斐波那契数列' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 7 FROM problems p WHERE p.title = '判断素数' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 8 FROM problems p WHERE p.title = '最大公约数与最小公倍数' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 9 FROM problems p WHERE p.title = '完美数判断' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 10 FROM problems p WHERE p.title = '十进制转二进制' AND p.deleted = 0 LIMIT 1;

-- 题目集2: 数组与排序精选
INSERT INTO problem_sets (title, description, type, creator_id, is_public, problem_count) VALUES
('数组与排序精选', '数组操作与排序算法的经典题目，从基础到进阶。', 'OFFICIAL', 1, 1, 12);
SET @sid = LAST_INSERT_ID();
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 1 FROM problems p WHERE p.title = '数组求和' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 2 FROM problems p WHERE p.title = '数组最大最小值' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 3 FROM problems p WHERE p.title = '移动零到末尾' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 4 FROM problems p WHERE p.title = '数组去重并排序' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 5 FROM problems p WHERE p.title = '数组左旋转' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 6 FROM problems p WHERE p.title = '查找缺失数字' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 7 FROM problems p WHERE p.title = '找众数' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 8 FROM problems p WHERE p.title = '归并排序' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 9 FROM problems p WHERE p.title = '第K大元素' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 10 FROM problems p WHERE p.title = '颜色分类' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 11 FROM problems p WHERE p.title = '逆序对计数' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 12 FROM problems p WHERE p.title = '螺旋矩阵输出' AND p.deleted = 0 LIMIT 1;

-- 题目集3: 字符串处理专练
INSERT INTO problem_sets (title, description, type, creator_id, is_public, problem_count) VALUES
('字符串处理专练', '字符串操作的经典题目，包括翻转、匹配、编码等。', 'OFFICIAL', 1, 1, 10);
SET @sid = LAST_INSERT_ID();
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 1 FROM problems p WHERE p.title = '大小写转换' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 2 FROM problems p WHERE p.title = '回文串判断' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 3 FROM problems p WHERE p.title = '单词计数' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 4 FROM problems p WHERE p.title = '字符串翻转' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 5 FROM problems p WHERE p.title = 'Caesar密码' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 6 FROM problems p WHERE p.title = '罗马数字转整数' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 7 FROM problems p WHERE p.title = '单词翻转顺序' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 8 FROM problems p WHERE p.title = '字符串压缩编码' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 9 FROM problems p WHERE p.title = '最长公共前缀' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 10 FROM problems p WHERE p.title = '最长公共子序列' AND p.deleted = 0 LIMIT 1;

-- 题目集4: 栈与队列应用
INSERT INTO problem_sets (title, description, type, creator_id, is_public, problem_count) VALUES
('栈与队列应用', '栈和队列数据结构的经典应用题目。', 'OFFICIAL', 1, 1, 8);
SET @sid = LAST_INSERT_ID();
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 1 FROM problems p WHERE p.title = '栈的基本操作' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 2 FROM problems p WHERE p.title = '有效的括号' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 3 FROM problems p WHERE p.title = '后缀表达式求值' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 4 FROM problems p WHERE p.title = '最小栈设计' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 5 FROM problems p WHERE p.title = '用栈实现队列' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 6 FROM problems p WHERE p.title = '每日温度' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 7 FROM problems p WHERE p.title = '柱状图中最大矩形' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 8 FROM problems p WHERE p.title = '滑动窗口最大值' AND p.deleted = 0 LIMIT 1;

-- 题目集5: 动态规划入门到进阶
INSERT INTO problem_sets (title, description, type, creator_id, is_public, problem_count) VALUES
('动态规划入门到进阶', '从简单的爬楼梯到背包问题，系统学习动态规划。', 'OFFICIAL', 1, 1, 12);
SET @sid = LAST_INSERT_ID();
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 1 FROM problems p WHERE p.title = '爬楼梯' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 2 FROM problems p WHERE p.title = '不同路径' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 3 FROM problems p WHERE p.title = '最大子数组和' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 4 FROM problems p WHERE p.title = '打家劫舍' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 5 FROM problems p WHERE p.title = '打家劫舍II' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 6 FROM problems p WHERE p.title = '买卖股票的最佳时机' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 7 FROM problems p WHERE p.title = '最长递增子序列' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 8 FROM problems p WHERE p.title = '零钱兑换' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 9 FROM problems p WHERE p.title = '最小路径和' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 10 FROM problems p WHERE p.title = '0-1背包问题' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 11 FROM problems p WHERE p.title = '完全背包问题' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 12 FROM problems p WHERE p.title = '编辑距离' AND p.deleted = 0 LIMIT 1;

-- 题目集6: 树与图专题
INSERT INTO problem_sets (title, description, type, creator_id, is_public, problem_count) VALUES
('树与图专题', '二叉树遍历、BST验证、图的BFS/DFS、最短路径等经典题目。', 'OFFICIAL', 1, 1, 10);
SET @sid = LAST_INSERT_ID();
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 1 FROM problems p WHERE p.title = '二叉树中序遍历' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 2 FROM problems p WHERE p.title = '二叉树的层序遍历' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 3 FROM problems p WHERE p.title = '对称二叉树判断' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 4 FROM problems p WHERE p.title = '验证二叉搜索树' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 5 FROM problems p WHERE p.title = '二叉树的最近公共祖先' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 6 FROM problems p WHERE p.title = '岛屿数量' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 7 FROM problems p WHERE p.title = '图的BFS最短路径' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 8 FROM problems p WHERE p.title = '拓扑排序' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 9 FROM problems p WHERE p.title = '并查集与连通分量' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 10 FROM problems p WHERE p.title = 'Dijkstra最短路径' AND p.deleted = 0 LIMIT 1;

-- 题目集7: 经典面试算法30题
INSERT INTO problem_sets (title, description, type, creator_id, is_public, problem_count) VALUES
('经典面试算法30题', '互联网大厂高频面试算法题精选，覆盖数组、字符串、链表、树、DP等核心考点。', 'OFFICIAL', 1, 1, 15);
SET @sid = LAST_INSERT_ID();
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 1 FROM problems p WHERE p.title = '两数之和' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 2 FROM problems p WHERE p.title = '反转链表' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 3 FROM problems p WHERE p.title = '无重复字符的最长子串' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 4 FROM problems p WHERE p.title = '三数之和' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 5 FROM problems p WHERE p.title = '合并区间' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 6 FROM problems p WHERE p.title = '最长回文子串' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 7 FROM problems p WHERE p.title = '盛水最多的容器' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 8 FROM problems p WHERE p.title = '最长连续序列' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 9 FROM problems p WHERE p.title = '全排列生成' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 10 FROM problems p WHERE p.title = '接雨水' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 11 FROM problems p WHERE p.title = 'LRU缓存' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 12 FROM problems p WHERE p.title = '搜索旋转排序数组' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 13 FROM problems p WHERE p.title = '最小覆盖子串' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 14 FROM problems p WHERE p.title = '编辑距离' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 15 FROM problems p WHERE p.title = 'N皇后问题' AND p.deleted = 0 LIMIT 1;

-- 题目集8: GPLT天梯赛模拟
INSERT INTO problem_sets (title, description, type, creator_id, is_public, problem_count) VALUES
('GPLT天梯赛模拟', '模拟团体程序设计天梯赛题目组合，包含简单(8题)、普通(4题)、困难(3题)的梯度分布。', 'OFFICIAL', 1, 1, 15);
SET @sid = LAST_INSERT_ID();
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 1 FROM problems p WHERE p.title = 'A+B 问题' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 2 FROM problems p WHERE p.title = '数组最大最小值' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 3 FROM problems p WHERE p.title = '回文串判断' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 4 FROM problems p WHERE p.title = '统计字符种类数' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 5 FROM problems p WHERE p.title = '二分查找' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 6 FROM problems p WHERE p.title = '矩阵转置' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 7 FROM problems p WHERE p.title = '同构字符串判断' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 8 FROM problems p WHERE p.title = '移除有序数组重复项' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 9 FROM problems p WHERE p.title = '前缀和与区间查询' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 10 FROM problems p WHERE p.title = '字母异位词分组' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 11 FROM problems p WHERE p.title = '长度最小的子数组' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 12 FROM problems p WHERE p.title = '最大正方形' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 13 FROM problems p WHERE p.title = '二叉树的前序与中序重建' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 14 FROM problems p WHERE p.title = '柱状图中最大矩形' AND p.deleted = 0 LIMIT 1;
INSERT INTO problem_set_items (set_id, problem_id, sort_order)
SELECT @sid, p.id, 15 FROM problems p WHERE p.title = '数独求解' AND p.deleted = 0 LIMIT 1;
