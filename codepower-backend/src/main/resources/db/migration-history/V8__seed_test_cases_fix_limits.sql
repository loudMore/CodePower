-- V8: Seed judge_test_cases for all 20 problems + fix memory_limit + add debug examples
-- memory_limit should be in KB: 256MB = 262144KB

-- Fix memory_limit for all problems (was 256 which is 256KB = 0.25MB, should be 262144 = 256MB)
UPDATE problems SET memory_limit = 262144 WHERE memory_limit = 256;

-- Set debug examples (same as input/output examples for now)
UPDATE problems SET debug_input_example = input_example, debug_output_example = output_example
WHERE debug_input_example IS NULL;

-- Problem 1: Two Sum
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(1, '4 9\n2 7 11 15', '0 1', 5, 0, 0),
(1, '3 6\n3 2 4', '1 2', 5, 1, 1),
(1, '2 6\n3 3', '0 1', 5, 1, 2),
(1, '5 8\n1 2 3 4 5', '2 4', 5, 1, 3);

-- Problem 2: Reverse Array
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(2, '5\n1 2 3 4 5', '5 4 3 2 1', 5, 0, 0),
(2, '1\n42', '42', 5, 1, 1),
(2, '3\n-1 0 1', '1 0 -1', 5, 1, 2),
(2, '6\n1 1 1 2 2 2', '2 2 2 1 1 1', 5, 1, 3);

-- Problem 3: Maximum Subarray Sum
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(3, '9\n-2 1 -3 4 -1 2 1 -5 4', '6', 5, 0, 0),
(3, '1\n1', '1', 5, 1, 1),
(3, '5\n5 4 -1 7 8', '23', 5, 1, 2),
(3, '3\n-1 -2 -3', '-1', 5, 1, 3);

-- Problem 4: Climbing Stairs
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(4, '3', '3', 5, 0, 0),
(4, '1', '1', 5, 1, 1),
(4, '2', '2', 5, 1, 2),
(4, '10', '89', 5, 1, 3);

-- Problem 5: Valid Parentheses
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(5, '()[]{}', 'true', 5, 0, 0),
(5, '(]', 'false', 5, 1, 1),
(5, '([)]', 'false', 5, 1, 2),
(5, '{[]}', 'true', 5, 1, 3);

-- Problem 6: Binary Search
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(6, '6 9\n-1 0 3 5 9 12', '4', 5, 0, 0),
(6, '6 2\n-1 0 3 5 9 12', '-1', 5, 1, 1),
(6, '1 5\n5', '0', 5, 1, 2),
(6, '5 1\n1 2 3 4 5', '0', 5, 1, 3);

-- Problem 7: Merge Sorted Lists/Arrays
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(7, '3 3\n1 2 3\n2 5 6', '1 2 2 3 5 6', 5, 0, 0),
(7, '1 3\n1\n2 3 4', '1 2 3 4', 5, 1, 1),
(7, '3 1\n1 2 3\n0', '0 1 2 3', 5, 1, 2),
(7, '0 3\n\n1 2 3', '1 2 3', 5, 1, 3);

-- Problem 8: Longest Common Prefix
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(8, '3\nflower\nflow\nflight', 'fl', 5, 0, 0),
(8, '3\ndog\nracecar\ncar', '', 5, 1, 1),
(8, '1\nhello', 'hello', 5, 1, 2),
(8, '2\ninterstellar\ninterface', 'inter', 5, 1, 3);

-- Problem 9: Best Time to Buy and Sell Stock
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(9, '6\n7 1 5 3 6 4', '5', 5, 0, 0),
(9, '5\n7 6 4 3 1', '0', 5, 1, 1),
(9, '2\n1 2', '1', 5, 1, 2),
(9, '4\n2 4 1 3', '2', 5, 1, 3);

-- Problem 10: Longest Substring Without Repeating Characters
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(10, 'abcabcbb', '3', 5, 0, 0),
(10, 'bbbbb', '1', 5, 1, 1),
(10, 'pwwkew', '3', 5, 1, 2),
(10, ' ', '1', 5, 1, 3);

-- Problem 11: Three Sum
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(11, '6\n-1 0 1 2 -1 -4', '-1 -1 2\n-1 0 1', 5, 0, 0),
(11, '3\n0 1 1', '', 5, 1, 1),
(11, '3\n0 0 0', '0 0 0', 5, 1, 2),
(11, '4\n-2 0 1 1', '-2 1 1', 5, 1, 3);

-- Problem 12: Longest Palindromic Substring
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(12, 'babad', 'bab', 5, 0, 0),
(12, 'cbbd', 'bb', 5, 1, 1),
(12, 'a', 'a', 5, 1, 2),
(12, 'ac', 'a', 5, 1, 3);

-- Problem 13: Merge Intervals
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(13, '4\n1 3\n2 6\n8 10\n15 18', '1 6\n8 10\n15 18', 5, 0, 0),
(13, '2\n1 4\n4 5', '1 5', 5, 1, 1),
(13, '1\n1 5', '1 5', 5, 1, 2),
(13, '3\n1 4\n0 4\n3 5', '0 5', 5, 1, 3);

-- Problem 14: Binary Tree Level Order Traversal
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(14, '7\n3 9 20 -1 -1 15 7', '3\n9 20\n15 7', 5, 0, 0),
(14, '1\n1', '1', 5, 1, 1),
(14, '3\n1 -1 2', '1\n2', 5, 1, 2),
(14, '0', '', 5, 1, 3);

-- Problem 15: Minimum Path Sum
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(15, '3 3\n1 3 1\n1 5 1\n4 2 1', '7', 5, 0, 0),
(15, '2 3\n1 2 3\n4 5 6', '12', 5, 1, 1),
(15, '1 1\n5', '5', 5, 1, 2),
(15, '2 2\n1 2\n1 1', '3', 5, 1, 3);

-- Problem 16: Edit Distance
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(16, 'horse\nros', '3', 5, 0, 0),
(16, 'intention\nexecution', '5', 5, 1, 1),
(16, '\nabc', '3', 5, 1, 2),
(16, 'abc\nabc', '0', 5, 1, 3);

-- Problem 17: Trapping Rain Water
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(17, '12\n0 1 0 2 1 0 1 3 2 1 2 1', '6', 5, 0, 0),
(17, '6\n4 2 0 3 2 5', '9', 5, 1, 1),
(17, '3\n1 0 1', '1', 5, 1, 2),
(17, '1\n5', '0', 5, 1, 3);

-- Problem 18: LRU Cache
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(18, '2 6\nput 1 1\nput 2 2\nget 1\nput 3 3\nget 2\nget 3', '1\n-1\n3', 5, 0, 0),
(18, '1 3\nput 1 1\nget 1\nput 2 2', '1', 5, 1, 1),
(18, '2 4\nput 1 1\nput 2 2\nget 1\nget 2', '1\n2', 5, 1, 2),
(18, '1 4\nput 1 1\nput 2 2\nget 1\nget 2', '-1\n2', 5, 1, 3);

-- Problem 19: Longest Increasing Subsequence
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(19, '8\n10 9 2 5 3 7 101 18', '4', 5, 0, 0),
(19, '4\n0 1 0 3', '3', 5, 1, 1),
(19, '6\n7 7 7 7 7 7', '1', 5, 1, 2),
(19, '5\n1 2 3 4 5', '5', 5, 1, 3);

-- Problem 20: Number of Islands
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(20, '4 5\n1 1 1 1 0\n1 1 0 1 0\n1 1 0 0 0\n0 0 0 0 0', '1', 5, 0, 0),
(20, '4 5\n1 1 0 0 0\n1 1 0 0 0\n0 0 1 0 0\n0 0 0 1 1', '3', 5, 1, 1),
(20, '1 1\n0', '0', 5, 1, 2),
(20, '1 1\n1', '1', 5, 1, 3);

-- =====================================================
-- Part 2: New tags (competition + algorithm supplement)
-- Existing: 1=数组,2=链表,3=动态规划,4=字符串,5=栈,6=二分查找,7=排序,8=树,9=图,10=贪心,11=双指针,12=滑动窗口,13=哈希表,14=BFS,15=DFS,16=设计
-- =====================================================
ALTER TABLE tags AUTO_INCREMENT = 17;
INSERT INTO tags (id, name) VALUES
(17, '蓝桥杯'), (18, 'GPLT'), (19, 'CCPC'), (20, 'ICPC'),
(21, '模拟'), (22, '数学'), (23, '前缀和'), (24, '递归'),
(25, '回溯'), (26, '并查集'), (27, '拓扑排序'), (28, '单调栈');
-- Tag IDs: 17=蓝桥杯, 18=GPLT, 19=CCPC, 20=ICPC,
--          21=模拟, 22=数学, 23=前缀和, 24=递归,
--          25=回溯, 26=并查集, 27=拓扑排序, 28=单调栈

-- =====================================================
-- Part 3: 30 new problems (IDs 21-50)
-- =====================================================

-- Problem 21: 斐波那契数列 (简单, 蓝桥杯/数学/递归)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(21, '斐波那契数列', '给定一个正整数 n，求斐波那契数列的第 n 项。\n\n斐波那契数列定义：F(1)=1, F(2)=1, F(n)=F(n-1)+F(n-2) (n≥3)。\n\n结果对 1000000007 取模。', '简单',
'一个正整数 n (1 ≤ n ≤ 100000)', '一个整数，表示 F(n) mod 1000000007',
'10', '55', '10', '55', '注意大数取模，避免溢出。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 22: 回文数判断 (简单, 蓝桥杯/数学/模拟)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(22, '回文数判断', '给定一个整数，判断它是否是回文数。\n\n回文数是指正序和倒序读都一样的整数。负数不是回文数。', '简单',
'一个整数 n (-2^31 ≤ n ≤ 2^31-1)', '如果是回文数输出 true，否则输出 false',
'121', 'true', '121', 'true', '不要将整数转为字符串来判断，尝试用数学方法。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 23: 计数质数 (简单, 蓝桥杯/数学)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(23, '计数质数', '给定一个正整数 n，统计小于 n 的质数的数量。', '简单',
'一个正整数 n (2 ≤ n ≤ 5000000)', '小于 n 的质数个数',
'10', '4', '10', '4', '使用埃拉托色尼筛法可以高效求解。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 24: 最大公约数与最小公倍数 (简单, 蓝桥杯/数学/GPLT)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(24, '最大公约数与最小公倍数', '给定两个正整数 a 和 b，求它们的最大公约数和最小公倍数。', '简单',
'两个正整数 a 和 b，以空格分隔 (1 ≤ a,b ≤ 10^9)', '两个整数，分别是最大公约数和最小公倍数，以空格分隔',
'12 18', '6 36', '12 18', '6 36', '使用辗转相除法求最大公约数，lcm = a * b / gcd(a, b)。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 25: 字符串反转 (简单, GPLT/字符串)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(25, '字符串反转', '给定一个字符串，将其反转后输出。', '简单',
'一行字符串 s (1 ≤ |s| ≤ 100000)，只包含英文字母和数字', '反转后的字符串',
'hello', 'olleh', 'hello', 'olleh', NULL, 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 26: 矩阵转置 (简单, 蓝桥杯/模拟)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(26, '矩阵转置', '给定一个 m×n 的矩阵，输出其转置矩阵。', '简单',
'第一行两个整数 m 和 n (1 ≤ m,n ≤ 100)\n接下来 m 行，每行 n 个整数', '转置后的矩阵，n 行 m 列',
'2 3\n1 2 3\n4 5 6', '1 4\n2 5\n3 6', '2 3\n1 2 3\n4 5 6', '1 4\n2 5\n3 6', NULL, 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 27: 进制转换 (简单, 蓝桥杯/模拟/数学)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(27, '进制转换', '给定一个十进制非负整数 n 和目标进制 b，将 n 转换为 b 进制表示。\n\n对于大于 9 的位用大写字母 A-F 表示。', '简单',
'两个整数 n 和 b (0 ≤ n ≤ 10^9, 2 ≤ b ≤ 16)', 'n 的 b 进制表示',
'255 16', 'FF', '255 16', 'FF', '短除法：反复除以进制取余数。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 28: 约瑟夫环 (普通, 蓝桥杯/模拟/数学)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(28, '约瑟夫环', '有 n 个人围成一圈，从第 1 个人开始报数，报到 m 的人出列，下一个人继续从 1 开始报数。求最后剩下的人的编号（编号从 1 开始）。', '普通',
'两个正整数 n 和 m (1 ≤ n ≤ 100000, 1 ≤ m ≤ 10^9)', '最后剩下的人的编号',
'5 3', '4', '5 3', '4', '递推公式：f(n,m) = (f(n-1,m) + m) % n，f(1,m)=0，最后加1。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 29: 螺旋矩阵 (普通, 蓝桥杯/模拟)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(29, '螺旋矩阵', '给定一个正整数 n，生成一个 n×n 的螺旋矩阵，从 1 到 n² 按顺时针螺旋填充。', '普通',
'一个正整数 n (1 ≤ n ≤ 100)', '按行输出螺旋矩阵，每行数字之间用空格分隔',
'3', '1 2 3\n8 9 4\n7 6 5', '3', '1 2 3\n8 9 4\n7 6 5', '模拟螺旋过程，用方向数组控制填充方向。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 30: 区间合并 (普通, CCPC/排序/贪心)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(30, '区间合并计数', '给定 n 个区间 [l, r]，合并所有重叠的区间，输出合并后的区间数量。', '普通',
'第一行一个整数 n (1 ≤ n ≤ 100000)\n接下来 n 行，每行两个整数 l 和 r (0 ≤ l ≤ r ≤ 10^9)', '合并后的区间数量',
'5\n1 3\n2 6\n8 10\n15 18\n17 20', '3', '5\n1 3\n2 6\n8 10\n15 18\n17 20', '3', '先按左端点排序，再依次合并。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 31: 前缀和 (简单, 蓝桥杯/前缀和)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(31, '区间求和', '给定一个长度为 n 的数组和 q 个查询，每次查询给出 l 和 r，求数组中第 l 到第 r 个元素的和（下标从 1 开始）。', '简单',
'第一行两个整数 n 和 q (1 ≤ n,q ≤ 100000)\n第二行 n 个整数表示数组 (-10^9 ≤ a[i] ≤ 10^9)\n接下来 q 行，每行两个整数 l 和 r (1 ≤ l ≤ r ≤ n)', '每个查询输出一行结果',
'5 3\n1 2 3 4 5\n1 3\n2 4\n1 5', '6\n9\n15', '5 3\n1 2 3 4 5\n1 3\n2 4\n1 5', '6\n9\n15', '使用前缀和数组，可以 O(1) 回答每个查询。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 32: 移除元素 (简单, 双指针)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(32, '移除元素', '给定一个数组和一个值 val，原地移除所有等于 val 的元素，输出移除后数组的长度和剩余元素（保持相对顺序）。', '简单',
'第一行两个整数 n 和 val (0 ≤ n ≤ 100000, 0 ≤ val ≤ 100)\n第二行 n 个整数', '第一行输出移除后的长度\n第二行输出剩余元素，用空格分隔',
'7 3\n3 2 2 3 4 3 5', '3\n2 2 4 5', '7 3\n3 2 2 3 4 3 5', '3\n2 2 4 5', '使用双指针，一个读一个写。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 33: 字符串匹配 (普通, ICPC/字符串)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(33, '字符串匹配', '给定一个文本串 s 和模式串 p，找出 p 在 s 中所有出现的位置（从 0 开始）。', '普通',
'第一行是文本串 s (1 ≤ |s| ≤ 1000000)\n第二行是模式串 p (1 ≤ |p| ≤ |s|)', '输出所有匹配位置，以空格分隔。若无匹配输出 -1',
'ababcababd\nabab', '0 5', 'ababcababd\nabab', '0 5', '可以使用 KMP 算法实现 O(n+m) 的字符串匹配。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 34: 栈的应用——表达式求值 (普通, 蓝桥杯/栈)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(34, '简单表达式求值', '给定一个只包含非负整数和加减乘除运算符的表达式字符串，计算其结果。\n\n只有 +、-、*、/ 四种运算符，保证除法为整数除法（向零取整），且不会除以零。', '普通',
'一行表达式字符串 (1 ≤ 长度 ≤ 100000)，数字在 int 范围内', '表达式的计算结果',
'3+2*2', '7', '3+2*2', '7', '使用双栈（操作数栈和运算符栈），或先处理乘除再处理加减。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 35: 全排列 (普通, 蓝桥杯/回溯)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(35, '全排列', '给定一个不含重复数字的数组，返回其所有可能的全排列。按字典序输出，每行一个排列。', '普通',
'第一行一个整数 n (1 ≤ n ≤ 8)\n第二行 n 个不同的整数', '每行输出一个排列，元素用空格分隔，按字典序排列',
'3\n1 2 3', '1 2 3\n1 3 2\n2 1 3\n2 3 1\n3 1 2\n3 2 1', '3\n1 2 3', '1 2 3\n1 3 2\n2 1 3\n2 3 1\n3 1 2\n3 2 1', '使用回溯法，通过交换或标记已用元素来生成排列。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 36: N 皇后计数 (普通, ICPC/回溯)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(36, 'N皇后计数', '在 N×N 的棋盘上放置 N 个皇后，使得彼此不能互相攻击（任意两个皇后不在同行、同列、同对角线）。求方案总数。', '普通',
'一个正整数 N (1 ≤ N ≤ 13)', '方案总数',
'8', '92', '8', '92', '经典回溯法，逐行放置皇后，检查列和对角线冲突。', 5000, 262144, 'PUBLIC', 1, 1);

-- Problem 37: 二叉树的前序遍历 (简单, GPLT/树)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(37, '二叉树的前序遍历', '给定一棵二叉树的层序序列（-1 表示空节点），输出其前序遍历结果。', '简单',
'第一行一个整数 n 表示序列长度 (1 ≤ n ≤ 1000)\n第二行 n 个整数表示层序序列', '前序遍历结果，用空格分隔',
'7\n1 2 3 4 5 -1 6', '1 2 4 5 3 6', '7\n1 2 3 4 5 -1 6', '1 2 4 5 3 6', '先根据层序序列建树，再递归前序遍历。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 38: 二叉搜索树的验证 (普通, 树/递归)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(38, '验证二叉搜索树', '给定一棵二叉树的层序序列（-1 表示空节点），判断它是否是有效的二叉搜索树（BST）。\n\n有效的 BST：左子树所有节点值 < 根节点值 < 右子树所有节点值，且左右子树也是 BST。', '普通',
'第一行一个整数 n (1 ≤ n ≤ 1000)\n第二行 n 个整数表示层序序列', '是 BST 输出 true，否则输出 false',
'7\n5 1 8 -1 -1 6 10', 'true', '7\n5 1 8 -1 -1 6 10', 'true', '使用中序遍历检查是否严格递增，或递归传递上下界。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 39: 快速排序 (普通, 蓝桥杯/排序)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(39, '排序', '给定一个整数数组，将其按升序排列后输出。', '普通',
'第一行一个整数 n (1 ≤ n ≤ 100000)\n第二行 n 个整数 (-10^9 ≤ a[i] ≤ 10^9)', '升序排列后的数组，元素用空格分隔',
'5\n3 1 4 1 5', '1 1 3 4 5', '5\n3 1 4 1 5', '1 1 3 4 5', '可以使用快速排序、归并排序等 O(n log n) 排序算法。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 40: 堆排序——第 K 大元素 (普通, CCPC/排序)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(40, '第K大元素', '给定一个未排序的整数数组，找到其中第 K 大的元素。', '普通',
'第一行两个整数 n 和 k (1 ≤ k ≤ n ≤ 100000)\n第二行 n 个整数 (-10^9 ≤ a[i] ≤ 10^9)', '第 K 大的元素',
'6 2\n3 2 1 5 6 4', '5', '6 2\n3 2 1 5 6 4', '5', '可以使用堆排序、快速选择算法，或排序后取值。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 41: 图的 BFS——最短路径 (普通, GPLT/BFS/图)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(41, '无权图最短路径', '给定一个 n 个节点 m 条边的无向无权图，求从节点 1 到节点 n 的最短路径长度。如果不可达，输出 -1。', '普通',
'第一行两个整数 n 和 m (2 ≤ n ≤ 100000, 1 ≤ m ≤ 200000)\n接下来 m 行，每行两个整数 u 和 v 表示一条无向边', '从节点 1 到节点 n 的最短距离',
'5 5\n1 2\n2 3\n3 5\n1 4\n4 5', '2', '5 5\n1 2\n2 3\n3 5\n1 4\n4 5', '2', '使用 BFS 求无权图最短路径。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 42: 并查集——连通分量 (普通, ICPC/并查集)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(42, '连通分量个数', '给定 n 个节点和 m 条无向边，求图中连通分量的个数。', '普通',
'第一行两个整数 n 和 m (1 ≤ n ≤ 100000, 0 ≤ m ≤ 200000)\n接下来 m 行，每行两个整数 u 和 v (1 ≤ u,v ≤ n)', '连通分量的个数',
'5 3\n1 2\n3 4\n4 5', '2', '5 3\n1 2\n3 4\n4 5', '2', '使用并查集，每次合并两个集合，最后统计不同根的个数。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 43: 01背包 (普通, CCPC/动态规划)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(43, '01背包问题', '有 n 件物品和一个容量为 W 的背包。第 i 件物品的体积为 w[i]，价值为 v[i]。每件物品只能用一次。求背包能装下的最大总价值。', '普通',
'第一行两个整数 n 和 W (1 ≤ n ≤ 1000, 1 ≤ W ≤ 1000)\n接下来 n 行，每行两个整数 w[i] 和 v[i] (1 ≤ w[i],v[i] ≤ 1000)', '最大总价值',
'4 5\n1 2\n2 4\n3 4\n4 5', '8', '4 5\n1 2\n2 4\n3 4\n4 5', '8', '经典 DP：dp[j] = max(dp[j], dp[j-w[i]] + v[i])，倒序遍历 j。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 44: 最长公共子序列 (普通, ICPC/动态规划/字符串)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(44, '最长公共子序列', '给定两个字符串，求它们的最长公共子序列（LCS）的长度。', '普通',
'两行，分别是字符串 s1 和 s2 (1 ≤ |s1|,|s2| ≤ 1000)，只包含小写字母', 'LCS 的长度',
'abcde\nace', '3', 'abcde\nace', '3', 'dp[i][j] 表示 s1 前 i 个字符与 s2 前 j 个字符的 LCS 长度。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 45: 硬币找零 (简单, 蓝桥杯/动态规划)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(45, '硬币找零', '给定不同面额的硬币和一个总金额，求凑成总金额所需的最少硬币数。如果无法凑出，输出 -1。\n\n每种硬币的数量是无限的。', '简单',
'第一行两个整数 n 和 amount (1 ≤ n ≤ 12, 0 ≤ amount ≤ 10000)\n第二行 n 个正整数表示硬币面额', '最少硬币数，无法凑出则输出 -1',
'3 11\n1 5 2', '3', '3 11\n1 5 2', '3', '完全背包变形，dp[i] 表示凑出金额 i 所需最少硬币数。', 1000, 262144, 'PUBLIC', 1, 1);

-- Problem 46: 单调栈——下一个更大元素 (普通, 单调栈)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(46, '下一个更大元素', '给定一个数组，对于每个元素，找到它右边第一个比它大的元素。如果不存在则为 -1。', '普通',
'第一行一个整数 n (1 ≤ n ≤ 100000)\n第二行 n 个整数 (-10^9 ≤ a[i] ≤ 10^9)', '每个元素的下一个更大元素，用空格分隔',
'4\n1 3 2 4', '3 4 4 -1', '4\n1 3 2 4', '3 4 4 -1', '使用单调递减栈，从右往左或从左往右扫描。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 47: 滑动窗口最大值 (困难, CCPC/滑动窗口/单调栈)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(47, '滑动窗口最大值', '给定一个数组和一个窗口大小 k，返回每个窗口内的最大值。', '困难',
'第一行两个整数 n 和 k (1 ≤ k ≤ n ≤ 100000)\n第二行 n 个整数 (-10^9 ≤ a[i] ≤ 10^9)', '每个窗口的最大值，用空格分隔',
'8 3\n1 3 -1 -3 5 3 6 7', '3 3 5 5 6 7', '8 3\n1 3 -1 -3 5 3 6 7', '3 3 5 5 6 7', '使用单调递减双端队列，维护窗口内的候选最大值。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 48: Dijkstra 最短路 (困难, ICPC/图)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(48, '单源最短路径', '给定一个 n 个节点 m 条边的有向加权图，求从节点 1 到所有其他节点的最短路径。若不可达输出 -1。', '困难',
'第一行两个整数 n 和 m (1 ≤ n ≤ 100000, 1 ≤ m ≤ 200000)\n接下来 m 行，每行三个整数 u, v, w 表示一条从 u 到 v 权值为 w 的边 (1 ≤ w ≤ 10^9)', 'n-1 个整数，分别是从节点 1 到节点 2,3,...,n 的最短距离，不可达输出 -1',
'5 6\n1 2 2\n1 3 4\n2 3 1\n2 4 7\n3 5 3\n4 5 1', '2 3 9 6', '5 6\n1 2 2\n1 3 4\n2 3 1\n2 4 7\n3 5 3\n4 5 1', '2 3 9 6', '使用优先队列优化的 Dijkstra 算法，时间复杂度 O((n+m)logn)。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 49: 拓扑排序 (普通, CCPC/拓扑排序/图)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(49, '拓扑排序', '给定一个有向无环图（DAG），输出其拓扑排序结果。如果存在多种合法排序，输出字典序最小的。', '普通',
'第一行两个整数 n 和 m (1 ≤ n ≤ 100000, 0 ≤ m ≤ 200000)\n接下来 m 行，每行两个整数 u 和 v 表示一条从 u 到 v 的有向边', '拓扑排序结果，用空格分隔',
'4 4\n1 2\n1 3\n2 4\n3 4', '1 2 3 4', '4 4\n1 2\n1 3\n2 4\n3 4', '1 2 3 4', '使用 BFS（Kahn 算法）+ 优先队列保证字典序最小。', 2000, 262144, 'PUBLIC', 1, 1);

-- Problem 50: 字符串哈希——子串判重 (普通, ICPC/字符串/哈希表)
INSERT INTO problems (id, title, description, difficulty, input_format, output_format, input_example, output_example, debug_input_example, debug_output_example, hint, time_limit, memory_limit, visibility, author_id, status) VALUES
(50, '不同子串个数', '给定一个字符串 s，求其所有不同子串的个数。', '普通',
'一行字符串 s (1 ≤ |s| ≤ 1000)，只包含小写字母', '不同子串的个数',
'aab', '5', 'aab', '5', 'aab 的不同子串有：a, aa, aab, ab, b，共 5 个。可以用集合或字符串哈希。', 2000, 262144, 'PUBLIC', 1, 1);

-- =====================================================
-- Part 4: Tag associations for new problems
-- =====================================================
INSERT INTO problem_tags (problem_id, tag_id) VALUES
-- Problem 21: 斐波那契数列 -> 蓝桥杯, 数学, 递归(24)
(21, 17), (21, 22), (21, 24),
-- Problem 22: 回文数判断 -> 蓝桥杯, 数学, 模拟
(22, 17), (22, 22), (22, 21),
-- Problem 23: 计数质数 -> 蓝桥杯, 数学
(23, 17), (23, 22),
-- Problem 24: 最大公约数与最小公倍数 -> 蓝桥杯, GPLT, 数学
(24, 17), (24, 18), (24, 22),
-- Problem 25: 字符串反转 -> GPLT, 字符串
(25, 18), (25, 4),
-- Problem 26: 矩阵转置 -> 蓝桥杯, 模拟
(26, 17), (26, 21),
-- Problem 27: 进制转换 -> 蓝桥杯, 模拟, 数学
(27, 17), (27, 21), (27, 22),
-- Problem 28: 约瑟夫环 -> 蓝桥杯, 模拟, 数学
(28, 17), (28, 21), (28, 22),
-- Problem 29: 螺旋矩阵 -> 蓝桥杯, 模拟
(29, 17), (29, 21),
-- Problem 30: 区间合并计数 -> CCPC, 排序, 贪心
(30, 19), (30, 7), (30, 10),
-- Problem 31: 区间求和 -> 蓝桥杯, 前缀和
(31, 17), (31, 23),
-- Problem 32: 移除元素 -> 双指针
(32, 11),
-- Problem 33: 字符串匹配 -> ICPC, 字符串
(33, 20), (33, 4),
-- Problem 34: 表达式求值 -> 蓝桥杯, 栈
(34, 17), (34, 5),
-- Problem 35: 全排列 -> 蓝桥杯, 回溯
(35, 17), (35, 25),
-- Problem 36: N皇后计数 -> ICPC, 回溯
(36, 20), (36, 25),
-- Problem 37: 二叉树前序遍历 -> GPLT, 树
(37, 18), (37, 8),
-- Problem 38: 验证BST -> 树, 递归(24)
(38, 8), (38, 24),
-- Problem 39: 排序 -> 蓝桥杯, 排序
(39, 17), (39, 7),
-- Problem 40: 第K大元素 -> CCPC, 排序
(40, 19), (40, 7),
-- Problem 41: 无权图最短路径 -> GPLT, BFS, 图
(41, 18), (41, 14), (41, 9),
-- Problem 42: 连通分量个数 -> ICPC, 并查集
(42, 20), (42, 26),
-- Problem 43: 01背包 -> CCPC, 动态规划
(43, 19), (43, 3),
-- Problem 44: 最长公共子序列 -> ICPC, 动态规划, 字符串
(44, 20), (44, 3), (44, 4),
-- Problem 45: 硬币找零 -> 蓝桥杯, 动态规划
(45, 17), (45, 3),
-- Problem 46: 下一个更大元素 -> 单调栈
(46, 28),
-- Problem 47: 滑动窗口最大值 -> CCPC, 滑动窗口(12), 单调栈
(47, 19), (47, 12), (47, 28),
-- Problem 48: 单源最短路径 -> ICPC, 图
(48, 20), (48, 9),
-- Problem 49: 拓扑排序 -> CCPC, 拓扑排序, 图
(49, 19), (49, 27), (49, 9),
-- Problem 50: 不同子串个数 -> ICPC, 字符串, 哈希表
(50, 20), (50, 4), (50, 13);

-- =====================================================
-- Part 5: Test cases for new problems (21-50)
-- =====================================================

-- Problem 21: 斐波那契数列
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(21, '10', '55', 5, 0, 0),
(21, '1', '1', 5, 1, 1),
(21, '2', '1', 5, 1, 2),
(21, '50', '586268941', 5, 1, 3);

-- Problem 22: 回文数判断
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(22, '121', 'true', 5, 0, 0),
(22, '-121', 'false', 5, 1, 1),
(22, '10', 'false', 5, 1, 2),
(22, '12321', 'true', 5, 1, 3);

-- Problem 23: 计数质数
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(23, '10', '4', 5, 0, 0),
(23, '2', '0', 5, 1, 1),
(23, '100', '25', 5, 1, 2),
(23, '1000000', '78498', 5, 1, 3);

-- Problem 24: 最大公约数与最小公倍数
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(24, '12 18', '6 36', 5, 0, 0),
(24, '7 13', '1 91', 5, 1, 1),
(24, '100 25', '25 100', 5, 1, 2),
(24, '1 1', '1 1', 5, 1, 3);

-- Problem 25: 字符串反转
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(25, 'hello', 'olleh', 5, 0, 0),
(25, 'a', 'a', 5, 1, 1),
(25, 'abcdef', 'fedcba', 5, 1, 2),
(25, '12345', '54321', 5, 1, 3);

-- Problem 26: 矩阵转置
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(26, '2 3\n1 2 3\n4 5 6', '1 4\n2 5\n3 6', 5, 0, 0),
(26, '1 1\n42', '42', 5, 1, 1),
(26, '3 2\n1 2\n3 4\n5 6', '1 3 5\n2 4 6', 5, 1, 2),
(26, '1 3\n7 8 9', '7\n8\n9', 5, 1, 3);

-- Problem 27: 进制转换
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(27, '255 16', 'FF', 5, 0, 0),
(27, '10 2', '1010', 5, 1, 1),
(27, '0 8', '0', 5, 1, 2),
(27, '100 8', '144', 5, 1, 3);

-- Problem 28: 约瑟夫环
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(28, '5 3', '4', 5, 0, 0),
(28, '1 1', '1', 5, 1, 1),
(28, '10 2', '5', 5, 1, 2),
(28, '6 7', '4', 5, 1, 3);

-- Problem 29: 螺旋矩阵
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(29, '3', '1 2 3\n8 9 4\n7 6 5', 5, 0, 0),
(29, '1', '1', 5, 1, 1),
(29, '2', '1 2\n4 3', 5, 1, 2),
(29, '4', '1 2 3 4\n12 13 14 5\n11 16 15 6\n10 9 8 7', 5, 1, 3);

-- Problem 30: 区间合并计数
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(30, '5\n1 3\n2 6\n8 10\n15 18\n17 20', '3', 5, 0, 0),
(30, '1\n1 5', '1', 5, 1, 1),
(30, '3\n1 4\n4 5\n6 8', '2', 5, 1, 2),
(30, '4\n1 10\n2 3\n4 5\n6 7', '1', 5, 1, 3);

-- Problem 31: 区间求和
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(31, '5 3\n1 2 3 4 5\n1 3\n2 4\n1 5', '6\n9\n15', 5, 0, 0),
(31, '1 1\n100\n1 1', '100', 5, 1, 1),
(31, '3 2\n-1 0 1\n1 2\n1 3', '-1\n0', 5, 1, 2),
(31, '4 1\n1000000000 1000000000 1000000000 1000000000\n1 4', '4000000000', 5, 1, 3);

-- Problem 32: 移除元素
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(32, '7 3\n3 2 2 3 4 3 5', '3\n2 2 4 5', 5, 0, 0),
(32, '4 2\n2 2 2 2', '0\n', 5, 1, 1),
(32, '5 1\n2 3 4 5 6', '5\n2 3 4 5 6', 5, 1, 2),
(32, '0 1', '0\n', 5, 1, 3);

-- Problem 33: 字符串匹配
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(33, 'ababcababd\nabab', '0 5', 5, 0, 0),
(33, 'aaaa\naa', '0 1 2', 5, 1, 1),
(33, 'hello\nworld', '-1', 5, 1, 2),
(33, 'abcabc\nabc', '0 3', 5, 1, 3);

-- Problem 34: 简单表达式求值
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(34, '3+2*2', '7', 5, 0, 0),
(34, '10-3+2', '9', 5, 1, 1),
(34, '2*3*4', '24', 5, 1, 2),
(34, '100/10/2', '5', 5, 1, 3);

-- Problem 35: 全排列
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(35, '3\n1 2 3', '1 2 3\n1 3 2\n2 1 3\n2 3 1\n3 1 2\n3 2 1', 5, 0, 0),
(35, '1\n1', '1', 5, 1, 1),
(35, '2\n1 2', '1 2\n2 1', 5, 1, 2),
(35, '3\n0 1 2', '0 1 2\n0 2 1\n1 0 2\n1 2 0\n2 0 1\n2 1 0', 5, 1, 3);

-- Problem 36: N皇后计数
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(36, '8', '92', 5, 0, 0),
(36, '1', '1', 5, 1, 1),
(36, '4', '2', 5, 1, 2),
(36, '10', '724', 5, 1, 3);

-- Problem 37: 二叉树前序遍历
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(37, '7\n1 2 3 4 5 -1 6', '1 2 4 5 3 6', 5, 0, 0),
(37, '1\n1', '1', 5, 1, 1),
(37, '3\n1 2 3', '1 2 3', 5, 1, 2),
(37, '5\n1 -1 2 -1 3', '1 2 3', 5, 1, 3);

-- Problem 38: 验证BST
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(38, '7\n5 1 8 -1 -1 6 10', 'true', 5, 0, 0),
(38, '5\n5 1 4 -1 -1', 'false', 5, 1, 1),
(38, '1\n1', 'true', 5, 1, 2),
(38, '7\n10 5 15 -1 -1 6 20', 'false', 5, 1, 3);

-- Problem 39: 排序
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(39, '5\n3 1 4 1 5', '1 1 3 4 5', 5, 0, 0),
(39, '1\n42', '42', 5, 1, 1),
(39, '6\n-3 -1 -4 -1 -5 -9', '-9 -5 -4 -3 -1 -1', 5, 1, 2),
(39, '4\n1 1 1 1', '1 1 1 1', 5, 1, 3);

-- Problem 40: 第K大元素
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(40, '6 2\n3 2 1 5 6 4', '5', 5, 0, 0),
(40, '1 1\n1', '1', 5, 1, 1),
(40, '5 3\n3 2 3 1 2', '2', 5, 1, 2),
(40, '4 4\n7 6 5 4', '4', 5, 1, 3);

-- Problem 41: 无权图最短路径
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(41, '5 5\n1 2\n2 3\n3 5\n1 4\n4 5', '2', 5, 0, 0),
(41, '2 1\n1 2', '1', 5, 1, 1),
(41, '3 0', '-1', 5, 1, 2),
(41, '4 4\n1 2\n2 3\n3 4\n1 4', '1', 5, 1, 3);

-- Problem 42: 连通分量个数
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(42, '5 3\n1 2\n3 4\n4 5', '2', 5, 0, 0),
(42, '3 0', '3', 5, 1, 1),
(42, '4 3\n1 2\n2 3\n3 4', '1', 5, 1, 2),
(42, '6 2\n1 2\n3 4', '4', 5, 1, 3);

-- Problem 43: 01背包
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(43, '4 5\n1 2\n2 4\n3 4\n4 5', '8', 5, 0, 0),
(43, '1 1\n2 3', '0', 5, 1, 1),
(43, '3 10\n5 10\n4 8\n6 12', '18', 5, 1, 2),
(43, '2 3\n1 1\n3 5', '5', 5, 1, 3);

-- Problem 44: 最长公共子序列
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(44, 'abcde\nace', '3', 5, 0, 0),
(44, 'abc\nabc', '3', 5, 1, 1),
(44, 'abc\ndef', '0', 5, 1, 2),
(44, 'abcbdab\nbdcaba', '4', 5, 1, 3);

-- Problem 45: 硬币找零
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(45, '3 11\n1 5 2', '3', 5, 0, 0),
(45, '1 0\n1', '0', 5, 1, 1),
(45, '2 3\n2 5', '-1', 5, 1, 2),
(45, '3 27\n1 5 10', '4', 5, 1, 3);

-- Problem 46: 下一个更大元素
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(46, '4\n1 3 2 4', '3 4 4 -1', 5, 0, 0),
(46, '3\n3 2 1', '-1 -1 -1', 5, 1, 1),
(46, '5\n1 2 3 4 5', '2 3 4 5 -1', 5, 1, 2),
(46, '1\n42', '-1', 5, 1, 3);

-- Problem 47: 滑动窗口最大值
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(47, '8 3\n1 3 -1 -3 5 3 6 7', '3 3 5 5 6 7', 5, 0, 0),
(47, '1 1\n1', '1', 5, 1, 1),
(47, '5 5\n1 2 3 4 5', '5', 5, 1, 2),
(47, '6 2\n1 -1 1 -1 1 -1', '1 1 1 1 1', 5, 1, 3);

-- Problem 48: 单源最短路径
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(48, '5 6\n1 2 2\n1 3 4\n2 3 1\n2 4 7\n3 5 3\n4 5 1', '2 3 9 6', 5, 0, 0),
(48, '2 1\n1 2 5', '5', 5, 1, 1),
(48, '3 1\n1 2 3', '3 -1', 5, 1, 2),
(48, '4 5\n1 2 1\n1 3 10\n2 3 2\n2 4 5\n3 4 1', '1 3 4', 5, 1, 3);

-- Problem 49: 拓扑排序
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(49, '4 4\n1 2\n1 3\n2 4\n3 4', '1 2 3 4', 5, 0, 0),
(49, '3 0', '1 2 3', 5, 1, 1),
(49, '2 1\n2 1', '2 1', 5, 1, 2),
(49, '5 4\n5 1\n4 2\n5 2\n4 1', '4 5 1 2 3', 5, 1, 3);

-- Problem 50: 不同子串个数
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(50, 'aab', '5', 5, 0, 0),
(50, 'a', '1', 5, 1, 1),
(50, 'abc', '6', 5, 1, 2),
(50, 'aaaa', '4', 5, 1, 3);

-- =====================================================
-- Part 6: Add tags to existing 20 problems for competition association
-- =====================================================
-- Add 蓝桥杯 tag to some easy existing problems
INSERT INTO problem_tags (problem_id, tag_id) VALUES
(1, 17), (2, 17), (4, 17), (5, 17), (8, 17),
-- Add GPLT tag
(3, 18), (6, 18), (9, 18),
-- Add CCPC tag
(13, 19), (17, 19), (19, 19),
-- Add ICPC tag
(16, 20), (18, 20), (20, 20);
