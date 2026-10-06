-- V18: 种子题目 - 动态规划/贪心/回溯 (20题，困难+极限为主)

INSERT INTO problems (title, description, difficulty, status, author_id, visibility,
  input_format, output_format, input_example, output_example, hint, time_limit, memory_limit) VALUES

('零钱兑换',
 '给定不同面额的硬币 coins 和一个总金额 amount，计算凑成该金额所需的最少硬币个数。如果无法凑成则返回 -1。\n\n每种硬币数量无限。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个整数 N 和 amount（1 <= N <= 12，0 <= amount <= 10^4）。\n第二行包含 N 个正整数表示硬币面额。',
 '输出最少硬币个数，无法凑成输出 -1。',
 '3 11\n1 2 5', '3', 'dp[i] = min(dp[i], dp[i-coin]+1)，完全背包变体。', 1000, 262144),

('打家劫舍',
 '一个小偷计划偷沿街的房屋。每间房内都有一定的现金，唯一限制是相邻的房屋装有互连的防盗系统——如果同一晚上相邻的两间房屋都被闯入，系统会自动报警。\n\n求在不触发报警的情况下能偷到的最大金额。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 100）。\n第二行包含 N 个非负整数（0 <= a_i <= 400），表示每间房的现金。',
 '输出能偷到的最大金额。',
 '5\n1 2 3 1 5', '9', 'dp[i] = max(dp[i-1], dp[i-2] + nums[i])', 1000, 262144),

('不同路径',
 '一个机器人位于 m × n 网格的左上角，每次只能向下或向右移动一步。求到达右下角有多少条不同的路径。',
 '普通', 1, 1, 'PUBLIC',
 '一行，包含两个正整数 m 和 n（1 <= m, n <= 100）。',
 '输出不同路径的数量。',
 '3 7', '28', 'dp[i][j] = dp[i-1][j] + dp[i][j-1]，可用一维数组优化空间。', 1000, 262144),

('跳跃游戏',
 '给定一个非负整数数组 nums，数组中的每个元素代表你在该位置可以跳跃的最大长度。判断你是否能从第一个位置到达最后一个位置。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^4）。\n第二行包含 N 个非负整数。',
 '如果能到达最后位置输出 true，否则输出 false。',
 '5\n2 3 1 1 4', 'true', '贪心：维护当前能到达的最远位置。', 1000, 262144),

('加油站',
 '在一条环路上有 N 个加油站，第 i 个加油站有汽油 gas[i] 升。从第 i 个加油站到第 i+1 个加油站需要消耗 cost[i] 升汽油。\n\n你的油箱无限大，初始为空。如果可以绕环路行驶一周，返回出发加油站的编号（0-based），否则返回 -1。保证答案唯一。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个非负整数，表示 gas。\n第三行包含 N 个非负整数，表示 cost。',
 '输出出发站编号（0-based），不能完成输出 -1。',
 '5\n1 2 3 4 5\n3 4 5 1 2', '3', NULL, 1000, 262144),

('分发糖果',
 'N 个孩子站成一排，每个孩子有一个评分。给每个孩子分配糖果，要求：\n1. 每个孩子至少分 1 颗糖果\n2. 相邻两个孩子中，评分更高的孩子获得更多糖果\n\n求最少需要多少颗糖果。',
 '困难', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 2×10^4）。\n第二行包含 N 个正整数，表示每个孩子的评分。',
 '输出最少需要的糖果总数。',
 '5\n1 0 2 1 3', '9', '从左到右扫描一次，再从右到左扫描一次。', 1000, 262144),

('0-1背包问题',
 '有 N 件物品和一个容量为 W 的背包。第 i 件物品的重量是 w[i]，价值是 v[i]。每件物品只能使用一次，求将哪些物品装入背包使价值总和最大。',
 '困难', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 N 和 W（1 <= N <= 100，1 <= W <= 10^4）。\n接下来 N 行，每行两个正整数 w_i 和 v_i（1 <= w_i, v_i <= 10^4）。',
 '输出最大价值。',
 '4 5\n1 2\n2 4\n3 4\n4 5', '8', 'dp[j] = max(dp[j], dp[j-w[i]] + v[i])，逆序遍历容量。', 1000, 262144),

('完全背包问题',
 '有 N 种物品和一个容量为 W 的背包。每种物品有无限个可用。第 i 种物品的重量是 w[i]，价值是 v[i]。求能获得的最大价值。',
 '困难', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 N 和 W（1 <= N <= 100，1 <= W <= 10^4）。\n接下来 N 行，每行两个正整数 w_i 和 v_i。',
 '输出最大价值。',
 '3 7\n2 3\n3 4\n4 5', '10', '与0-1背包类似，但容量正序遍历。', 1000, 262144),

('最长有效括号',
 '给定一个只包含 ''('' 和 '')'' 的字符串，找出最长的有效（格式正确且连续）括号子串的长度。',
 '困难', 1, 1, 'PUBLIC',
 '一行，包含一个只含 ''('' 和 '')'' 的字符串 s（0 <= |s| <= 3×10^4）。',
 '输出最长有效括号子串的长度。',
 ')()())', '4', '可以用栈或 DP。', 1000, 262144),

('N皇后问题',
 '在 N×N 的棋盘上放置 N 个皇后，使得任何两个皇后都不在同一行、同一列或同一对角线上。\n\n输出所有合法方案数。',
 '困难', 1, 1, 'PUBLIC',
 '一行，包含一个正整数 N（1 <= N <= 13）。',
 '输出合法方案总数。',
 '4', '2', '回溯法：逐行放置，检查列和对角线冲突。', 5000, 262144),

('柱状图中最大矩形',
 '给定 N 个非负整数，表示柱状图中各柱子的高度，每个柱子的宽度为 1。求能勾勒出的最大矩形面积。',
 '困难', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个非负整数（0 <= h_i <= 10^4）。',
 '输出最大矩形面积。',
 '6\n2 1 5 6 2 3', '10', '使用单调栈。', 1000, 262144),

('滑动窗口最大值',
 '给定一个整数数组和一个大小为 K 的滑动窗口，窗口从数组最左边滑到最右边。输出每个窗口位置的最大值。',
 '困难', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 N 和 K（1 <= K <= N <= 10^5）。\n第二行包含 N 个整数（-10^4 <= a_i <= 10^4）。',
 '输出每个窗口的最大值，用空格分隔。',
 '8 3\n1 3 -1 -3 5 3 6 7', '3 3 5 5 6 7', '使用单调递减双端队列。', 1000, 262144),

('最小覆盖子串',
 '给定两个字符串 s 和 t，找出 s 中包含 t 所有字符的最短子串。如果不存在则输出空行。\n\n注意：t 中字符可能有重复，子串中必须包含对应数量。',
 '困难', 1, 1, 'PUBLIC',
 '第一行包含字符串 s（1 <= |s| <= 10^5）。\n第二行包含字符串 t（1 <= |t| <= 10^5）。',
 '输出最小覆盖子串，不存在输出空行。',
 'ADOBECODEBANC\nABC', 'BANC', '滑动窗口 + 字符计数。', 1000, 262144),

('打家劫舍II',
 '在"打家劫舍"基础上，所有房屋围成一圈（第一间和最后一间相邻）。同样不能偷相邻的两间。求能偷到的最大金额。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 100）。\n第二行包含 N 个非负整数（0 <= a_i <= 1000）。',
 '输出能偷到的最大金额。',
 '4\n1 2 3 1', '4', '分两种情况：偷第一间（不偷最后一间）或不偷第一间。取两者最大值。', 1000, 262144),

('单词搜索',
 '给定一个 m×n 的字符网格和一个字符串 word，判断 word 是否存在于网格中。\n\n单词可以由相邻（上下左右）的单元格中的字母构成，同一单元格不能重复使用。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 m 和 n（1 <= m, n <= 6）。\n接下来 m 行，每行一个长度为 n 的字符串。\n最后一行是要搜索的单词 word（1 <= |word| <= 15）。',
 '如果存在输出 true，否则输出 false。',
 '3 4\nABCE\nSFCS\nADEE\nABCCED', 'true', NULL, 2000, 262144),

('正则表达式匹配',
 '实现正则表达式匹配，支持 ''.'' 和 ''*''。\n\n''.'' 匹配任意单个字符。\n''*'' 匹配零个或多个前面的字符。\n\n匹配需要覆盖整个字符串（不是部分匹配）。',
 '极限', 1, 1, 'PUBLIC',
 '第一行包含字符串 s（0 <= |s| <= 20），仅含小写字母。\n第二行包含模式 p（0 <= |p| <= 30），含小写字母、''.'' 和 ''*''。',
 '如果匹配输出 true，否则输出 false。',
 'aab\nc*a*b', 'true', 'dp[i][j] 表示 s 的前 i 个字符与 p 的前 j 个字符是否匹配。', 1000, 262144),

('数独求解',
 '编写程序解一个 9×9 的数独。空格用 0 表示。\n\n规则：每行、每列、每个 3×3 宫格内数字 1-9 各出现一次。保证有唯一解。',
 '极限', 1, 1, 'PUBLIC',
 '9 行，每行 9 个整数（0 表示空格，1-9 表示已填数字），用空格分隔。',
 '输出填满后的 9×9 数独矩阵，每行 9 个数字用空格分隔。',
 '5 3 0 0 7 0 0 0 0\n6 0 0 1 9 5 0 0 0\n0 9 8 0 0 0 0 6 0\n8 0 0 0 6 0 0 0 3\n4 0 0 8 0 3 0 0 1\n7 0 0 0 2 0 0 0 6\n0 6 0 0 0 0 2 8 0\n0 0 0 4 1 9 0 0 5\n0 0 0 0 8 0 0 7 9',
 '5 3 4 6 7 8 9 1 2\n6 7 2 1 9 5 3 4 8\n1 9 8 3 4 2 5 6 7\n8 5 9 7 6 1 4 2 3\n4 2 6 8 5 3 7 9 1\n7 1 3 9 2 4 8 5 6\n9 6 1 5 3 7 2 8 4\n2 8 7 4 1 9 6 3 5\n3 4 5 2 8 6 1 7 9', NULL, 5000, 262144),

('通配符匹配',
 '实现通配符模式匹配，支持 ''?'' 和 ''*''。\n\n''?'' 匹配任意单个字符。\n''*'' 匹配任意字符序列（包括空序列）。\n\n匹配需要覆盖整个字符串。',
 '极限', 1, 1, 'PUBLIC',
 '第一行包含字符串 s（0 <= |s| <= 2000），仅含小写字母。\n第二行包含模式 p（0 <= |p| <= 2000），含小写字母、''?'' 和 ''*''。',
 '如果匹配输出 true，否则输出 false。',
 'adceb\n*a*b', 'true', 'DP 或贪心+回溯。', 1000, 262144),

('最大正方形',
 '在一个由 0 和 1 组成的 m×n 矩阵中，找到只包含 1 的最大正方形，输出其面积。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 m 和 n（1 <= m, n <= 300）。\n接下来 m 行，每行 n 个字符（0 或 1）。',
 '输出最大全1正方形的面积。',
 '4 5\n1 0 1 0 0\n1 0 1 1 1\n1 1 1 1 1\n1 0 0 1 0', '4', 'dp[i][j] = min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]) + 1', 1000, 262144),

('编辑距离应用',
 '给定两个字符串和一个操作限制数 K，判断是否能在最多 K 次编辑操作内将第一个字符串转换为第二个。\n\n允许的操作：插入一个字符、删除一个字符、替换一个字符。',
 '困难', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 K（0 <= K <= 100）。\n第二行包含字符串 s1。\n第三行包含字符串 s2。\n（字符串长度不超过 500）',
 '如果可以在 K 次操作内完成转换输出 true，否则输出 false。',
 '3\nhorse\nros', 'true', '先求编辑距离，再和 K 比较。', 1000, 262144);

-- ============ 标签关联 ============
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '零钱兑换' AND t.name IN ('动态规划');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '打家劫舍' AND t.name IN ('动态规划', '数组');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '不同路径' AND t.name IN ('动态规划', '数学');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '跳跃游戏' AND t.name IN ('数组', '贪心', '动态规划');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '加油站' AND t.name IN ('数组', '贪心');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '分发糖果' AND t.name IN ('数组', '贪心');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '0-1背包问题' AND t.name = '动态规划';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '完全背包问题' AND t.name = '动态规划';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最长有效括号' AND t.name IN ('字符串', '动态规划', '栈');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = 'N皇后问题' AND t.name = '回溯';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '柱状图中最大矩形' AND t.name IN ('数组', '栈');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '滑动窗口最大值' AND t.name IN ('数组', '滑动窗口', '队列');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最小覆盖子串' AND t.name IN ('字符串', '滑动窗口', '哈希表');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '打家劫舍II' AND t.name IN ('动态规划', '数组');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '单词搜索' AND t.name IN ('回溯', '矩阵');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '正则表达式匹配' AND t.name IN ('字符串', '动态规划', '递归');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '数独求解' AND t.name IN ('回溯', '矩阵');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '通配符匹配' AND t.name IN ('字符串', '动态规划', '贪心');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最大正方形' AND t.name IN ('动态规划', '矩阵');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '编辑距离应用' AND t.name IN ('字符串', '动态规划');

-- ============ 测试用例 ============

-- 零钱兑换
SET @pid = (SELECT id FROM problems WHERE title = '零钱兑换' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3 11\n1 2 5', '3', 4, 0, 0),
(@pid, '1 0\n1', '0', 4, 1, 1),
(@pid, '1 3\n2', '-1', 4, 1, 2),
(@pid, '3 6\n1 3 4', '2', 4, 1, 3),
(@pid, '2 100\n1 50', '2', 4, 1, 4);

-- 打家劫舍
SET @pid = (SELECT id FROM problems WHERE title = '打家劫舍' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n1 2 3 1 5', '9', 4, 0, 0),
(@pid, '4\n2 7 9 3', '12', 4, 1, 1),
(@pid, '1\n5', '5', 4, 1, 2),
(@pid, '2\n1 2', '2', 4, 1, 3),
(@pid, '6\n2 1 1 2 1 1', '4', 4, 1, 4);

-- 不同路径
SET @pid = (SELECT id FROM problems WHERE title = '不同路径' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3 7', '28', 4, 0, 0),
(@pid, '1 1', '1', 4, 1, 1),
(@pid, '3 2', '3', 4, 1, 2),
(@pid, '3 3', '6', 4, 1, 3),
(@pid, '10 10', '48620', 4, 1, 4);

-- 跳跃游戏
SET @pid = (SELECT id FROM problems WHERE title = '跳跃游戏' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n2 3 1 1 4', 'true', 4, 0, 0),
(@pid, '5\n3 2 1 0 4', 'false', 4, 1, 1),
(@pid, '1\n0', 'true', 4, 1, 2),
(@pid, '2\n0 1', 'false', 4, 1, 3),
(@pid, '3\n1 1 0', 'true', 4, 1, 4);

-- 加油站
SET @pid = (SELECT id FROM problems WHERE title = '加油站' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n1 2 3 4 5\n3 4 5 1 2', '3', 4, 0, 0),
(@pid, '5\n2 3 4 3 2\n3 4 3 2 3', '-1', 4, 1, 1),
(@pid, '1\n5\n3', '0', 4, 1, 2),
(@pid, '3\n3 1 1\n1 1 3', '0', 4, 1, 3),
(@pid, '4\n1 2 3 4\n2 3 4 1', '3', 4, 1, 4);

-- 分发糖果
SET @pid = (SELECT id FROM problems WHERE title = '分发糖果' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n1 0 2 1 3', '9', 4, 0, 0),
(@pid, '3\n1 2 2', '4', 4, 1, 1),
(@pid, '1\n5', '1', 4, 1, 2),
(@pid, '4\n1 2 3 4', '10', 4, 1, 3),
(@pid, '4\n4 3 2 1', '10', 4, 1, 4);

-- 0-1背包问题
SET @pid = (SELECT id FROM problems WHERE title = '0-1背包问题' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '4 5\n1 2\n2 4\n3 4\n4 5', '8', 4, 0, 0),
(@pid, '1 1\n2 3', '0', 4, 1, 1),
(@pid, '3 10\n5 10\n4 8\n6 12', '18', 4, 1, 2),
(@pid, '2 3\n1 1\n2 2', '2', 4, 1, 3),
(@pid, '3 50\n10 60\n20 100\n30 120', '220', 4, 1, 4);

-- 完全背包问题
SET @pid = (SELECT id FROM problems WHERE title = '完全背包问题' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3 7\n2 3\n3 4\n4 5', '10', 4, 0, 0),
(@pid, '1 0\n1 1', '0', 4, 1, 1),
(@pid, '1 5\n2 3', '6', 4, 1, 2),
(@pid, '2 10\n3 5\n4 7', '17', 4, 1, 3),
(@pid, '2 1\n2 3\n3 5', '0', 4, 1, 4);

-- 最长有效括号
SET @pid = (SELECT id FROM problems WHERE title = '最长有效括号' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, ')()())', '4', 4, 0, 0),
(@pid, '(()', '2', 4, 1, 1),
(@pid, '', '0', 4, 1, 2),
(@pid, '()()', '4', 4, 1, 3),
(@pid, '()(())', '6', 4, 1, 4);

-- N皇后问题
SET @pid = (SELECT id FROM problems WHERE title = 'N皇后问题' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '4', '2', 4, 0, 0),
(@pid, '1', '1', 4, 1, 1),
(@pid, '8', '92', 4, 1, 2),
(@pid, '5', '10', 4, 1, 3),
(@pid, '6', '4', 4, 1, 4);

-- 柱状图中最大矩形
SET @pid = (SELECT id FROM problems WHERE title = '柱状图中最大矩形' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6\n2 1 5 6 2 3', '10', 4, 0, 0),
(@pid, '1\n2', '2', 4, 1, 1),
(@pid, '2\n2 4', '4', 4, 1, 2),
(@pid, '5\n1 1 1 1 1', '5', 4, 1, 3),
(@pid, '3\n6 7 5', '15', 4, 1, 4);

-- 滑动窗口最大值
SET @pid = (SELECT id FROM problems WHERE title = '滑动窗口最大值' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '8 3\n1 3 -1 -3 5 3 6 7', '3 3 5 5 6 7', 4, 0, 0),
(@pid, '1 1\n1', '1', 4, 1, 1),
(@pid, '5 5\n1 2 3 4 5', '5', 4, 1, 2),
(@pid, '5 1\n5 4 3 2 1', '5 4 3 2 1', 4, 1, 3),
(@pid, '5 3\n-1 -2 -3 -4 -5', '-1 -2 -3', 4, 1, 4);

-- 最小覆盖子串
SET @pid = (SELECT id FROM problems WHERE title = '最小覆盖子串' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'ADOBECODEBANC\nABC', 'BANC', 4, 0, 0),
(@pid, 'a\na', 'a', 4, 1, 1),
(@pid, 'a\naa', '', 4, 1, 2),
(@pid, 'abc\nac', 'abc', 4, 1, 3),
(@pid, 'aa\naa', 'aa', 4, 1, 4);

-- 打家劫舍II
SET @pid = (SELECT id FROM problems WHERE title = '打家劫舍II' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '4\n1 2 3 1', '4', 4, 0, 0),
(@pid, '3\n2 3 2', '3', 4, 1, 1),
(@pid, '1\n5', '5', 4, 1, 2),
(@pid, '3\n1 2 3', '3', 4, 1, 3),
(@pid, '5\n1 2 3 4 5', '8', 4, 1, 4);

-- 单词搜索
SET @pid = (SELECT id FROM problems WHERE title = '单词搜索' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3 4\nABCE\nSFCS\nADEE\nABCCED', 'true', 4, 0, 0),
(@pid, '3 4\nABCE\nSFCS\nADEE\nSEE', 'true', 4, 1, 1),
(@pid, '3 4\nABCE\nSFCS\nADEE\nABCB', 'false', 4, 1, 2),
(@pid, '1 1\nA\nA', 'true', 4, 1, 3),
(@pid, '1 1\nA\nB', 'false', 4, 1, 4);

-- 正则表达式匹配
SET @pid = (SELECT id FROM problems WHERE title = '正则表达式匹配' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'aab\nc*a*b', 'true', 4, 0, 0),
(@pid, 'aa\na', 'false', 4, 1, 1),
(@pid, 'aa\na*', 'true', 4, 1, 2),
(@pid, 'ab\n.*', 'true', 4, 1, 3),
(@pid, 'mississippi\nmis*is*p*.', 'false', 4, 1, 4);

-- 数独求解
SET @pid = (SELECT id FROM problems WHERE title = '数独求解' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid,
'5 3 0 0 7 0 0 0 0\n6 0 0 1 9 5 0 0 0\n0 9 8 0 0 0 0 6 0\n8 0 0 0 6 0 0 0 3\n4 0 0 8 0 3 0 0 1\n7 0 0 0 2 0 0 0 6\n0 6 0 0 0 0 2 8 0\n0 0 0 4 1 9 0 0 5\n0 0 0 0 8 0 0 7 9',
'5 3 4 6 7 8 9 1 2\n6 7 2 1 9 5 3 4 8\n1 9 8 3 4 2 5 6 7\n8 5 9 7 6 1 4 2 3\n4 2 6 8 5 3 7 9 1\n7 1 3 9 2 4 8 5 6\n9 6 1 5 3 7 2 8 4\n2 8 7 4 1 9 6 3 5\n3 4 5 2 8 6 1 7 9',
5, 0, 0),
(@pid,
'0 0 0 2 6 0 7 0 1\n6 8 0 0 7 0 0 9 0\n1 9 0 0 0 4 5 0 0\n8 2 0 1 0 0 0 4 0\n0 0 4 6 0 2 9 0 0\n0 5 0 0 0 3 0 2 8\n0 0 9 3 0 0 0 7 4\n0 4 0 0 5 0 0 3 6\n7 0 3 0 1 8 0 0 0',
'4 3 5 2 6 9 7 8 1\n6 8 2 5 7 1 4 9 3\n1 9 7 8 3 4 5 6 2\n8 2 6 1 9 5 3 4 7\n3 7 4 6 8 2 9 1 5\n9 5 1 7 4 3 6 2 8\n5 1 9 3 2 6 8 7 4\n2 4 8 9 5 7 1 3 6\n7 6 3 4 1 8 2 5 9',
5, 1, 1),
(@pid,
'0 0 0 6 0 0 4 0 0\n7 0 0 0 0 3 6 0 0\n0 0 0 0 9 1 0 8 0\n0 0 0 0 0 0 0 0 0\n0 5 0 1 8 0 0 0 3\n0 0 0 3 0 6 0 4 5\n0 4 0 2 0 0 0 6 0\n9 0 3 0 0 0 0 0 0\n0 2 0 0 0 0 1 0 0',
'5 8 1 6 7 2 4 3 9\n7 9 2 8 4 3 6 5 1\n3 6 4 5 9 1 7 8 2\n4 3 8 9 5 7 2 1 6\n2 5 6 1 8 4 9 7 3\n1 7 9 3 2 6 8 4 5\n8 4 5 2 1 9 3 6 7\n9 1 3 7 6 8 5 2 4\n6 2 7 4 3 5 1 9 8',
5, 1, 2);

-- 通配符匹配
SET @pid = (SELECT id FROM problems WHERE title = '通配符匹配' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'adceb\n*a*b', 'true', 4, 0, 0),
(@pid, 'acdcb\na*c?b', 'false', 4, 1, 1),
(@pid, '\n*', 'true', 4, 1, 2),
(@pid, 'aa\n*', 'true', 4, 1, 3),
(@pid, 'cb\n?a', 'false', 4, 1, 4);

-- 最大正方形
SET @pid = (SELECT id FROM problems WHERE title = '最大正方形' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '4 5\n1 0 1 0 0\n1 0 1 1 1\n1 1 1 1 1\n1 0 0 1 0', '4', 4, 0, 0),
(@pid, '1 1\n0', '0', 4, 1, 1),
(@pid, '1 1\n1', '1', 4, 1, 2),
(@pid, '2 2\n1 1\n1 1', '4', 4, 1, 3),
(@pid, '3 3\n1 1 0\n1 1 0\n0 0 0', '4', 4, 1, 4);

-- 编辑距离应用
SET @pid = (SELECT id FROM problems WHERE title = '编辑距离应用' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3\nhorse\nros', 'true', 4, 0, 0),
(@pid, '0\nabc\nabc', 'true', 4, 1, 1),
(@pid, '0\nabc\ndef', 'false', 4, 1, 2),
(@pid, '1\na\nab', 'true', 4, 1, 3),
(@pid, '2\nintention\nexecution', 'false', 4, 1, 4);

-- 设置 debug 示例
UPDATE problems SET debug_input_example = input_example, debug_output_example = output_example
WHERE debug_input_example IS NULL;
