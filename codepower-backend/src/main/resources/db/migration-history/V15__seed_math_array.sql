-- V15: 种子题目 - 基础数学 + 数组 (20题，简单为主)
-- 新增标签
INSERT IGNORE INTO tags (name) VALUES ('数学'), ('递归'), ('模拟'), ('矩阵'), ('前缀和');

-- ============ 基础数学 10题 (简单) ============
INSERT INTO problems (title, description, difficulty, status, author_id, visibility,
  input_format, output_format, input_example, output_example, hint, time_limit, memory_limit) VALUES

('A+B 问题',
 '给定两个整数 A 和 B，计算 A + B 的值。\n\n这是一道经典的入门题，帮助你熟悉在线评测系统的输入输出格式。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含两个整数 A 和 B（-10^9 <= A, B <= 10^9），用空格分隔。',
 '输出一个整数，表示 A + B 的值。',
 '1 2', '3', NULL, 1000, 262144),

('判断奇偶',
 '给定一个整数 N，判断它是奇数还是偶数。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个整数 N（-10^9 <= N <= 10^9）。',
 '如果 N 是偶数，输出 even；如果 N 是奇数，输出 odd。',
 '3', 'odd', NULL, 1000, 262144),

('计算阶乘',
 '给定一个非负整数 N，计算 N 的阶乘 N!。\n\n定义：0! = 1，N! = 1 × 2 × 3 × ... × N。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个非负整数 N（0 <= N <= 20）。',
 '输出 N! 的值。',
 '5', '120', '注意 N=0 时结果为 1。20! 不超过 long long 范围。', 1000, 262144),

('斐波那契数列',
 '斐波那契数列定义如下：F(0)=0, F(1)=1, F(n)=F(n-1)+F(n-2)（n>=2）。\n\n给定一个非负整数 N，求 F(N) 的值。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个非负整数 N（0 <= N <= 45）。',
 '输出 F(N) 的值。',
 '10', '55', '注意：请使用迭代方式计算，避免递归超时。', 1000, 262144),

('判断素数',
 '给定一个正整数 N，判断它是否是素数。\n\n素数是指大于 1 的自然数中，只能被 1 和自身整除的数。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个正整数 N（1 <= N <= 10^9）。',
 '如果 N 是素数，输出 Yes；否则输出 No。',
 '7', 'Yes', '优化提示：只需检查到 sqrt(N) 即可。', 1000, 262144),

('最大公约数与最小公倍数',
 '给定两个正整数 A 和 B，求它们的最大公约数（GCD）和最小公倍数（LCM）。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含两个正整数 A 和 B（1 <= A, B <= 10^9）。',
 '输出两个整数，分别是 GCD 和 LCM，用空格分隔。',
 '12 18', '6 36', '使用辗转相除法求 GCD，LCM = A * B / GCD。注意溢出问题。', 1000, 262144),

('十进制转二进制',
 '给定一个非负整数 N，将其转换为二进制表示。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个非负整数 N（0 <= N <= 10^9）。',
 '输出 N 的二进制表示（无前导零，N=0 时输出 0）。',
 '10', '1010', NULL, 1000, 262144),

('数位之和',
 '给定一个非负整数 N，计算 N 的各位数字之和。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个非负整数 N（0 <= N <= 10^18）。',
 '输出 N 的各位数字之和。',
 '12345', '15', NULL, 1000, 262144),

('完美数判断',
 '一个正整数如果等于除自身外所有正因子之和，则称为完美数。例如 6 = 1 + 2 + 3。\n\n给定一个正整数 N，判断它是否是完美数。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个正整数 N（1 <= N <= 10^8）。',
 '如果 N 是完美数，输出 Yes；否则输出 No。',
 '28', 'Yes', '28 = 1 + 2 + 4 + 7 + 14。注意 1 不是完美数。', 1000, 262144),

('温度转换',
 '给定一个摄氏温度 C，将其转换为华氏温度 F。\n\n转换公式：F = C × 9/5 + 32',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个浮点数 C（-273.15 <= C <= 10000），表示摄氏温度。',
 '输出对应的华氏温度，保留两位小数。',
 '100', '212.00', NULL, 1000, 262144),

-- ============ 数组基础 10题 (简单+普通) ============

('数组求和',
 '给定一个包含 N 个整数的数组，计算所有元素的总和。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数 a_i（-10^9 <= a_i <= 10^9），用空格分隔。',
 '输出数组元素的总和。',
 '5\n1 2 3 4 5', '15', '注意使用 long long 避免溢出。', 1000, 262144),

('数组最大最小值',
 '给定一个包含 N 个整数的数组，找出其中的最大值和最小值。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数（-10^9 <= a_i <= 10^9），用空格分隔。',
 '输出两个整数，分别是最大值和最小值，用空格分隔。',
 '5\n3 1 4 1 5', '5 1', NULL, 1000, 262144),

('数组去重并排序',
 '给定一个包含 N 个整数的数组，去除其中的重复元素并按升序输出。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数（-10^9 <= a_i <= 10^9），用空格分隔。',
 '输出去重后的升序数组，元素间用空格分隔。',
 '8\n3 1 4 1 5 9 2 6', '1 2 3 4 5 6 9', NULL, 1000, 262144),

('数组左旋转',
 '给定一个包含 N 个整数的数组和一个整数 K，将数组向左旋转 K 个位置。\n\n例如：数组 [1,2,3,4,5] 左旋转 2 位后变为 [3,4,5,1,2]。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 N 和 K（1 <= N <= 10^5，0 <= K <= 10^9）。\n第二行包含 N 个整数，用空格分隔。',
 '输出旋转后的数组，元素间用空格分隔。',
 '5 2\n1 2 3 4 5', '3 4 5 1 2', '提示：K 可能大于 N，需要取模。', 1000, 262144),

('查找缺失数字',
 '给定一个包含 N 个不重复整数的数组，这些整数取自 0, 1, 2, ..., N。找出数组中缺失的那个数字。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个不重复整数（取值范围 0 到 N）。',
 '输出缺失的数字。',
 '5\n3 0 1 4 2', '5', '提示：利用等差数列求和公式 N*(N+1)/2 减去数组元素之和。', 1000, 262144),

('移动零到末尾',
 '给定一个包含 N 个整数的数组，将所有零移动到数组末尾，同时保持非零元素的相对顺序。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数（-10^9 <= a_i <= 10^9），用空格分隔。',
 '输出处理后的数组，元素间用空格分隔。',
 '5\n0 1 0 3 12', '1 3 12 0 0', NULL, 1000, 262144),

('找众数',
 '给定一个包含 N 个整数的数组，找出其中出现次数超过 N/2 的元素（众数）。\n\n保证输入中一定存在众数。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5，N 为奇数）。\n第二行包含 N 个整数，用空格分隔。',
 '输出众数。',
 '7\n2 2 1 1 1 2 2', '2', '提示：可以使用 Boyer-Moore 投票算法实现 O(n) 时间 O(1) 空间。', 1000, 262144),

('前缀和与区间查询',
 '给定一个包含 N 个整数的数组和 Q 次查询，每次查询给出一个区间 [L, R]，求该区间内所有元素的和。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 N 和 Q（1 <= N, Q <= 10^5）。\n第二行包含 N 个整数 a_i（-10^9 <= a_i <= 10^9）。\n接下来 Q 行，每行两个整数 L 和 R（1 <= L <= R <= N），表示查询区间。',
 '输出 Q 行，每行一个整数表示区间和。',
 '5 3\n1 2 3 4 5\n1 3\n2 4\n1 5', '6\n9\n15', '使用前缀和数组可将每次查询降为 O(1)。', 1000, 262144),

('矩阵转置',
 '给定一个 M×N 的矩阵，输出它的转置矩阵（N×M）。\n\n转置矩阵是将原矩阵的行变为列，列变为行。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 M 和 N（1 <= M, N <= 100）。\n接下来 M 行，每行 N 个整数，用空格分隔。',
 '输出转置后的矩阵（N 行 M 列），每行元素用空格分隔。',
 '2 3\n1 2 3\n4 5 6', '1 4\n2 5\n3 6', NULL, 1000, 262144),

('螺旋矩阵输出',
 '给定一个正整数 N，生成一个 N×N 的矩阵，其元素按螺旋顺序（顺时针，从外向内）依次填入 1 到 N²。',
 '普通', 1, 1, 'PUBLIC',
 '一行，包含一个正整数 N（1 <= N <= 20）。',
 '输出 N×N 的螺旋矩阵，每行元素用空格分隔。',
 '3', '1 2 3\n8 9 4\n7 6 5', NULL, 1000, 262144);

-- ============ 标签关联 ============
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = 'A+B 问题' AND t.name = '数学';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '判断奇偶' AND t.name = '数学';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '计算阶乘' AND t.name IN ('数学', '递归');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '斐波那契数列' AND t.name IN ('数学', '动态规划');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '判断素数' AND t.name = '数学';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最大公约数与最小公倍数' AND t.name = '数学';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '十进制转二进制' AND t.name IN ('数学', '模拟');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '数位之和' AND t.name IN ('数学', '模拟');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '完美数判断' AND t.name = '数学';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '温度转换' AND t.name IN ('数学', '模拟');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '数组求和' AND t.name = '数组';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '数组最大最小值' AND t.name = '数组';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '数组去重并排序' AND t.name IN ('数组', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '数组左旋转' AND t.name = '数组';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '查找缺失数字' AND t.name IN ('数组', '数学');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '移动零到末尾' AND t.name IN ('数组', '双指针');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '找众数' AND t.name = '数组';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '前缀和与区间查询' AND t.name IN ('数组', '前缀和');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '矩阵转置' AND t.name IN ('数组', '矩阵');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '螺旋矩阵输出' AND t.name IN ('数组', '矩阵', '模拟');

-- ============ 测试用例 ============

-- A+B 问题
SET @pid = (SELECT id FROM problems WHERE title = 'A+B 问题' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '1 2', '3', 4, 0, 0),
(@pid, '0 0', '0', 4, 1, 1),
(@pid, '-1 1', '0', 4, 1, 2),
(@pid, '1000000000 1000000000', '2000000000', 4, 1, 3),
(@pid, '-1000000000 -1000000000', '-2000000000', 4, 1, 4);

-- 判断奇偶
SET @pid = (SELECT id FROM problems WHERE title = '判断奇偶' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3', 'odd', 4, 0, 0),
(@pid, '4', 'even', 4, 1, 1),
(@pid, '0', 'even', 4, 1, 2),
(@pid, '-7', 'odd', 4, 1, 3),
(@pid, '1000000000', 'even', 4, 1, 4);

-- 计算阶乘
SET @pid = (SELECT id FROM problems WHERE title = '计算阶乘' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5', '120', 4, 0, 0),
(@pid, '0', '1', 4, 1, 1),
(@pid, '1', '1', 4, 1, 2),
(@pid, '10', '3628800', 4, 1, 3),
(@pid, '20', '2432902008176640000', 4, 1, 4);

-- 斐波那契数列
SET @pid = (SELECT id FROM problems WHERE title = '斐波那契数列' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '10', '55', 4, 0, 0),
(@pid, '0', '0', 4, 1, 1),
(@pid, '1', '1', 4, 1, 2),
(@pid, '2', '1', 4, 1, 3),
(@pid, '45', '1134903170', 4, 1, 4);

-- 判断素数
SET @pid = (SELECT id FROM problems WHERE title = '判断素数' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '7', 'Yes', 4, 0, 0),
(@pid, '1', 'No', 4, 1, 1),
(@pid, '2', 'Yes', 4, 1, 2),
(@pid, '4', 'No', 4, 1, 3),
(@pid, '999999937', 'Yes', 4, 1, 4);

-- 最大公约数与最小公倍数
SET @pid = (SELECT id FROM problems WHERE title = '最大公约数与最小公倍数' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '12 18', '6 36', 4, 0, 0),
(@pid, '1 1', '1 1', 4, 1, 1),
(@pid, '7 13', '1 91', 4, 1, 2),
(@pid, '100 25', '25 100', 4, 1, 3),
(@pid, '36 48', '12 144', 4, 1, 4);

-- 十进制转二进制
SET @pid = (SELECT id FROM problems WHERE title = '十进制转二进制' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '10', '1010', 4, 0, 0),
(@pid, '0', '0', 4, 1, 1),
(@pid, '1', '1', 4, 1, 2),
(@pid, '255', '11111111', 4, 1, 3),
(@pid, '1024', '10000000000', 4, 1, 4);

-- 数位之和
SET @pid = (SELECT id FROM problems WHERE title = '数位之和' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '12345', '15', 4, 0, 0),
(@pid, '0', '0', 4, 1, 1),
(@pid, '9', '9', 4, 1, 2),
(@pid, '999999999999999999', '162', 4, 1, 3),
(@pid, '100000', '1', 4, 1, 4);

-- 完美数判断
SET @pid = (SELECT id FROM problems WHERE title = '完美数判断' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '28', 'Yes', 4, 0, 0),
(@pid, '6', 'Yes', 4, 1, 1),
(@pid, '1', 'No', 4, 1, 2),
(@pid, '12', 'No', 4, 1, 3),
(@pid, '8128', 'Yes', 4, 1, 4);

-- 温度转换
SET @pid = (SELECT id FROM problems WHERE title = '温度转换' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '100', '212.00', 4, 0, 0),
(@pid, '0', '32.00', 4, 1, 1),
(@pid, '-40', '-40.00', 4, 1, 2),
(@pid, '37.5', '99.50', 4, 1, 3),
(@pid, '-273.15', '-459.67', 4, 1, 4);

-- 数组求和
SET @pid = (SELECT id FROM problems WHERE title = '数组求和' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n1 2 3 4 5', '15', 4, 0, 0),
(@pid, '1\n42', '42', 4, 1, 1),
(@pid, '3\n-1 0 1', '0', 4, 1, 2),
(@pid, '4\n1000000000 1000000000 1000000000 1000000000', '4000000000', 4, 1, 3),
(@pid, '3\n-1000000000 -1000000000 -1000000000', '-3000000000', 4, 1, 4);

-- 数组最大最小值
SET @pid = (SELECT id FROM problems WHERE title = '数组最大最小值' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n3 1 4 1 5', '5 1', 4, 0, 0),
(@pid, '1\n42', '42 42', 4, 1, 1),
(@pid, '3\n-5 -2 -8', '-2 -8', 4, 1, 2),
(@pid, '4\n7 7 7 7', '7 7', 4, 1, 3),
(@pid, '5\n-1000000000 0 1000000000 -999999999 999999999', '1000000000 -1000000000', 4, 1, 4);

-- 数组去重并排序
SET @pid = (SELECT id FROM problems WHERE title = '数组去重并排序' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '8\n3 1 4 1 5 9 2 6', '1 2 3 4 5 6 9', 4, 0, 0),
(@pid, '1\n5', '5', 4, 1, 1),
(@pid, '5\n1 1 1 1 1', '1', 4, 1, 2),
(@pid, '6\n-3 -1 0 -1 2 -3', '-3 -1 0 2', 4, 1, 3),
(@pid, '5\n5 4 3 2 1', '1 2 3 4 5', 4, 1, 4);

-- 数组左旋转
SET @pid = (SELECT id FROM problems WHERE title = '数组左旋转' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5 2\n1 2 3 4 5', '3 4 5 1 2', 4, 0, 0),
(@pid, '5 0\n1 2 3 4 5', '1 2 3 4 5', 4, 1, 1),
(@pid, '5 5\n1 2 3 4 5', '1 2 3 4 5', 4, 1, 2),
(@pid, '5 7\n1 2 3 4 5', '3 4 5 1 2', 4, 1, 3),
(@pid, '1\n42', '42', 3, 1, 4);

-- 查找缺失数字
SET @pid = (SELECT id FROM problems WHERE title = '查找缺失数字' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n3 0 1 4 2', '5', 4, 0, 0),
(@pid, '1\n0', '1', 4, 1, 1),
(@pid, '1\n1', '0', 4, 1, 2),
(@pid, '3\n0 1 3', '2', 4, 1, 3),
(@pid, '9\n9 6 4 2 3 5 7 0 1', '8', 4, 1, 4);

-- 移动零到末尾
SET @pid = (SELECT id FROM problems WHERE title = '移动零到末尾' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n0 1 0 3 12', '1 3 12 0 0', 4, 0, 0),
(@pid, '1\n0', '0', 4, 1, 1),
(@pid, '4\n1 2 3 4', '1 2 3 4', 4, 1, 2),
(@pid, '4\n0 0 0 0', '0 0 0 0', 4, 1, 3),
(@pid, '6\n0 0 1 0 0 2', '1 2 0 0 0 0', 4, 1, 4);

-- 找众数
SET @pid = (SELECT id FROM problems WHERE title = '找众数' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '7\n2 2 1 1 1 2 2', '2', 4, 0, 0),
(@pid, '1\n1', '1', 4, 1, 1),
(@pid, '3\n3 3 3', '3', 4, 1, 2),
(@pid, '5\n1 2 1 2 1', '1', 4, 1, 3),
(@pid, '9\n5 5 5 5 5 1 2 3 4', '5', 4, 1, 4);

-- 前缀和与区间查询
SET @pid = (SELECT id FROM problems WHERE title = '前缀和与区间查询' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5 3\n1 2 3 4 5\n1 3\n2 4\n1 5', '6\n9\n15', 4, 0, 0),
(@pid, '1 1\n42\n1 1', '42', 4, 1, 1),
(@pid, '5 2\n-1 -2 -3 -4 -5\n1 5\n3 3', '-15\n-3', 4, 1, 2),
(@pid, '4 3\n1000000000 1000000000 1000000000 1000000000\n1 4\n1 1\n2 3', '4000000000\n1000000000\n2000000000', 4, 1, 3),
(@pid, '3 2\n0 0 0\n1 3\n2 2', '0\n0', 4, 1, 4);

-- 矩阵转置
SET @pid = (SELECT id FROM problems WHERE title = '矩阵转置' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '2 3\n1 2 3\n4 5 6', '1 4\n2 5\n3 6', 4, 0, 0),
(@pid, '1 1\n42', '42', 4, 1, 1),
(@pid, '3 3\n1 2 3\n4 5 6\n7 8 9', '1 4 7\n2 5 8\n3 6 9', 4, 1, 2),
(@pid, '1 4\n1 2 3 4', '1\n2\n3\n4', 4, 1, 3),
(@pid, '3 1\n10\n20\n30', '10 20 30', 4, 1, 4);

-- 螺旋矩阵输出
SET @pid = (SELECT id FROM problems WHERE title = '螺旋矩阵输出' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3', '1 2 3\n8 9 4\n7 6 5', 4, 0, 0),
(@pid, '1', '1', 4, 1, 1),
(@pid, '2', '1 2\n4 3', 4, 1, 2),
(@pid, '4', '1 2 3 4\n12 13 14 5\n11 16 15 6\n10 9 8 7', 4, 1, 3),
(@pid, '5', '1 2 3 4 5\n16 17 18 19 6\n15 24 25 20 7\n14 23 22 21 8\n13 12 11 10 9', 4, 1, 4);

-- 更新统计字段
UPDATE problems SET submit_count = 0, accept_count = 0, accept_rate = 0 WHERE submit_count IS NULL;
-- 设置 debug 示例
UPDATE problems SET debug_input_example = input_example, debug_output_example = output_example
WHERE debug_input_example IS NULL;
