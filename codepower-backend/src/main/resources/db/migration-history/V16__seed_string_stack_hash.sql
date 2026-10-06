-- V16: 种子题目 - 字符串 + 栈/队列/哈希 (20题，简单+普通)
-- 新增标签
INSERT IGNORE INTO tags (name) VALUES ('队列');

-- ============ 字符串 10题 ============
INSERT INTO problems (title, description, difficulty, status, author_id, visibility,
  input_format, output_format, input_example, output_example, hint, time_limit, memory_limit) VALUES

('统计字符种类数',
 '给定一个字符串，统计其中包含多少种不同的字符（区分大小写）。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个字符串 s（1 <= |s| <= 10^5），仅包含可打印 ASCII 字符。',
 '输出一个整数，表示不同字符的种类数。',
 'aAbBcC', '6', NULL, 1000, 262144),

('大小写转换',
 '给定一个字符串，将所有大写字母转换为小写字母，所有小写字母转换为大写字母，其他字符不变。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个字符串 s（1 <= |s| <= 10^5）。',
 '输出转换后的字符串。',
 'Hello World!', 'hELLO wORLD!', NULL, 1000, 262144),

('回文串判断',
 '给定一个字符串（只考虑字母和数字字符，忽略大小写），判断它是否是回文串。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个字符串 s（0 <= |s| <= 10^5）。',
 '如果是回文串输出 true，否则输出 false。',
 'A man, a plan, a canal: Panama', 'true', '空字符串视为回文串。', 1000, 262144),

('单词计数',
 '给定一行文本，统计其中包含多少个单词。单词由连续的非空白字符组成，单词之间由一个或多个空格分隔。',
 '简单', 1, 1, 'PUBLIC',
 '一行文本（长度不超过 10^5）。',
 '输出单词个数。',
 '  Hello   World  ', '2', '注意处理前导、尾随和连续空格。', 1000, 262144),

('字符串翻转',
 '给定一个句子，将句子中每个单词的字符顺序翻转，但单词顺序不变。\n\n例如 "hello world" 变为 "olleh dlrow"。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个句子（单词间用单个空格分隔，长度不超过 10^5）。',
 '输出翻转每个单词后的句子。',
 'hello world', 'olleh dlrow', NULL, 1000, 262144),

('Caesar密码',
 '凯撒密码是一种简单的加密方法：将每个字母在字母表中向后移动 K 个位置（循环）。\n\n给定一个字符串和偏移量 K，输出加密后的结果。只对英文字母加密，其他字符不变，保持大小写。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个整数 K（0 <= K <= 25）。\n第二行包含一个字符串 s（1 <= |s| <= 10^5）。',
 '输出加密后的字符串。',
 '3\nHello, World!', 'Khoor, Zruog!', NULL, 1000, 262144),

('罗马数字转整数',
 '给定一个罗马数字字符串，将其转换为整数。\n\n罗马数字规则：I=1, V=5, X=10, L=50, C=100, D=500, M=1000。\n小值在大值左边做减法（如 IV=4, IX=9），否则做加法。',
 '简单', 1, 1, 'PUBLIC',
 '一行，包含一个罗马数字字符串（1 <= |s| <= 15）。',
 '输出对应的整数。',
 'MCMXCIV', '1994', NULL, 1000, 262144),

('字符串压缩编码',
 '实现一个简单的字符串压缩：将连续重复的字符替换为字符加重复次数。如果压缩后的字符串不比原串短，则返回原串。\n\n例如 "aabcccccaaa" 压缩为 "a2b1c5a3"。',
 '普通', 1, 1, 'PUBLIC',
 '一行，包含一个字符串 s（1 <= |s| <= 10^5），仅包含英文字母。',
 '输出压缩后的字符串（如果更短），否则输出原字符串。',
 'aabcccccaaa', 'a2b1c5a3', NULL, 1000, 262144),

('最长公共子序列',
 '给定两个字符串 s1 和 s2，求它们的最长公共子序列（LCS）的长度。\n\n子序列是指从原串中删除一些（或不删除）字符后得到的串，不改变剩余字符的相对顺序。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含字符串 s1（1 <= |s1| <= 1000）。\n第二行包含字符串 s2（1 <= |s2| <= 1000）。',
 '输出最长公共子序列的长度。',
 'abcde\nace', '3', '使用二维 DP：dp[i][j] 表示 s1 前 i 个字符与 s2 前 j 个字符的 LCS 长度。', 2000, 262144),

('单词翻转顺序',
 '给定一个句子，将句子中单词的顺序翻转。\n\n例如 "the sky is blue" 变为 "blue is sky the"。',
 '简单', 1, 1, 'PUBLIC',
 '一行文本，单词间可能有多个空格，首尾可能有空格。',
 '输出翻转单词顺序后的句子，单词间用单个空格分隔，首尾无空格。',
 '  the sky is   blue  ', 'blue is sky the', NULL, 1000, 262144),

-- ============ 栈/队列/哈希 10题 ============

('栈的基本操作',
 '模拟一个整数栈的操作。支持以下操作：\n- push x：将整数 x 压入栈\n- pop：弹出栈顶元素并输出，栈空时输出 -1\n- top：输出栈顶元素但不弹出，栈空时输出 -1\n- size：输出栈中元素个数',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5），表示操作次数。\n接下来 N 行，每行一个操作。',
 '对于每个 pop、top、size 操作，输出一行结果。',
 '6\npush 1\npush 2\ntop\nsize\npop\npop', '2\n2\n2\n1', NULL, 1000, 262144),

('后缀表达式求值',
 '后缀表达式（逆波兰表达式）是一种不需要括号的算术表达式表示方法。\n\n给定一个后缀表达式，计算其值。操作符包括 +, -, *, /（整数除法，向零取整）。',
 '普通', 1, 1, 'PUBLIC',
 '一行，包含后缀表达式的各个元素（整数或运算符），用空格分隔。元素个数不超过 10^4。',
 '输出计算结果。',
 '2 1 + 3 *', '9', NULL, 1000, 262144),

('每日温度',
 '给定一个表示每天温度的整数数组，对于每一天，找出需要等多少天才能遇到更高的温度。如果之后没有更高温度，则对应输出 0。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数，表示每天的温度（30 <= T <= 100）。',
 '输出 N 个整数，用空格分隔。',
 '8\n73 74 75 71 69 72 76 73', '1 1 4 2 1 1 0 0', '使用单调递减栈。', 1000, 262144),

('最小栈设计',
 '设计一个支持 push、pop、top 和 getMin 操作的栈，其中 getMin 能在 O(1) 时间内返回栈中最小元素。\n\n操作：push x / pop / top / getMin',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5），表示操作次数。\n接下来 N 行，每行一个操作。',
 '对于每个 top 和 getMin 操作，输出一行结果。栈空时 pop/top/getMin 不输出。',
 '7\npush -2\npush 0\npush -3\ngetMin\npop\ntop\ngetMin', '-3\n0\n-2', NULL, 1000, 262144),

('用栈实现队列',
 '仅使用两个栈实现一个队列。支持以下操作：\n- enqueue x：入队\n- dequeue：出队并输出，队空时输出 -1\n- front：查看队头元素，队空时输出 -1',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5），表示操作次数。\n接下来 N 行，每行一个操作。',
 '对于每个 dequeue 和 front 操作，输出一行结果。',
 '6\nenqueue 1\nenqueue 2\nfront\ndequeue\nfront\ndequeue', '1\n1\n2\n2', NULL, 1000, 262144),

('两个数组的交集',
 '给定两个整数数组，找出它们的交集。结果中每个元素只出现一次，按升序输出。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数。\n第三行包含一个正整数 M（1 <= M <= 10^5）。\n第四行包含 M 个整数。',
 '输出交集元素，升序排列，用空格分隔。如果交集为空则输出 empty。',
 '4\n1 2 2 1\n3\n2 2 3', '2', NULL, 1000, 262144),

('同构字符串判断',
 '给定两个字符串 s 和 t，判断它们是否同构。\n\n两个字符串同构是指 s 中的每个字符都可以被替换为另一个字符以得到 t，且这种映射是一一对应的（双射）。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含字符串 s。\n第二行包含字符串 t。\n两个字符串长度相同（1 <= |s| <= 10^5），仅含小写字母。',
 '如果同构输出 true，否则输出 false。',
 'egg\nadd', 'true', NULL, 1000, 262144),

('字母异位词分组',
 '给定一组字符串，将字母异位词（由相同字母重新排列组成的单词）分为一组。\n\n每组内的单词按输入顺序输出，各组按组内第一个单词在原数组中的出现顺序输出。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^4）。\n接下来 N 行，每行一个字符串（仅含小写字母，长度不超过 100）。',
 '每组占一行，同组单词用空格分隔。',
 '6\neat tea tan ate nat bat', 'eat tea ate\ntan nat\nbat', NULL, 1000, 262144),

('最长连续序列',
 '给定一个未排序的整数数组，找出最长的连续元素序列的长度。\n\n要求算法时间复杂度为 O(n)。\n\n例如：给定 [100, 4, 200, 1, 3, 2]，最长连续序列是 [1,2,3,4]，长度为 4。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数（-10^9 <= a_i <= 10^9）。',
 '输出最长连续序列的长度。',
 '6\n100 4 200 1 3 2', '4', '使用哈希集合。对每个可能是序列起点的数（即 num-1 不在集合中），向后扩展计数。', 1000, 262144),

('和为K的子数组',
 '给定一个整数数组和一个整数 K，找出数组中和为 K 的连续子数组的个数。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个整数 N 和 K（1 <= N <= 2×10^4，-10^9 <= K <= 10^9）。\n第二行包含 N 个整数（-1000 <= a_i <= 1000）。',
 '输出和为 K 的连续子数组个数。',
 '5 3\n1 1 1 2 3', '3', '使用前缀和 + 哈希表：记录每个前缀和出现的次数。', 1000, 262144);

-- ============ 标签关联 ============
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '统计字符种类数' AND t.name IN ('字符串', '哈希表');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '大小写转换' AND t.name = '字符串';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '回文串判断' AND t.name IN ('字符串', '双指针');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '单词计数' AND t.name = '字符串';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '字符串翻转' AND t.name = '字符串';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = 'Caesar密码' AND t.name IN ('字符串', '模拟');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '罗马数字转整数' AND t.name IN ('字符串', '数学');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '字符串压缩编码' AND t.name = '字符串';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最长公共子序列' AND t.name IN ('字符串', '动态规划');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '单词翻转顺序' AND t.name = '字符串';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '栈的基本操作' AND t.name IN ('栈', '模拟');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '后缀表达式求值' AND t.name = '栈';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '每日温度' AND t.name IN ('栈', '数组');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最小栈设计' AND t.name IN ('栈', '设计');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '用栈实现队列' AND t.name IN ('栈', '队列', '设计');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '两个数组的交集' AND t.name IN ('数组', '哈希表');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '同构字符串判断' AND t.name IN ('字符串', '哈希表');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '字母异位词分组' AND t.name IN ('字符串', '哈希表', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最长连续序列' AND t.name IN ('数组', '哈希表');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '和为K的子数组' AND t.name IN ('数组', '哈希表', '前缀和');

-- ============ 测试用例 ============

-- 统计字符种类数
SET @pid = (SELECT id FROM problems WHERE title = '统计字符种类数' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'aAbBcC', '6', 4, 0, 0),
(@pid, 'aaaa', '1', 4, 1, 1),
(@pid, 'a', '1', 4, 1, 2),
(@pid, 'Hello World!', '8', 4, 1, 3),
(@pid, '11223344', '4', 4, 1, 4);

-- 大小写转换
SET @pid = (SELECT id FROM problems WHERE title = '大小写转换' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'Hello World!', 'hELLO wORLD!', 4, 0, 0),
(@pid, 'ABC', 'abc', 4, 1, 1),
(@pid, 'abc', 'ABC', 4, 1, 2),
(@pid, '123!@#', '123!@#', 4, 1, 3),
(@pid, 'aAbBcC', 'AaBbCc', 4, 1, 4);

-- 回文串判断
SET @pid = (SELECT id FROM problems WHERE title = '回文串判断' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'A man, a plan, a canal: Panama', 'true', 4, 0, 0),
(@pid, 'race a car', 'false', 4, 1, 1),
(@pid, '', 'true', 4, 1, 2),
(@pid, 'a', 'true', 4, 1, 3),
(@pid, '.,', 'true', 4, 1, 4);

-- 单词计数
SET @pid = (SELECT id FROM problems WHERE title = '单词计数' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '  Hello   World  ', '2', 4, 0, 0),
(@pid, 'one', '1', 4, 1, 1),
(@pid, '   ', '0', 4, 1, 2),
(@pid, 'a b c d e', '5', 4, 1, 3),
(@pid, '  multiple   spaces   between   words  ', '4', 4, 1, 4);

-- 字符串翻转
SET @pid = (SELECT id FROM problems WHERE title = '字符串翻转' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'hello world', 'olleh dlrow', 4, 0, 0),
(@pid, 'a', 'a', 4, 1, 1),
(@pid, 'ab cd ef', 'ba dc fe', 4, 1, 2),
(@pid, 'racecar', 'racecar', 4, 1, 3),
(@pid, 'abc def ghi', 'cba fed ihg', 4, 1, 4);

-- Caesar密码
SET @pid = (SELECT id FROM problems WHERE title = 'Caesar密码' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3\nHello, World!', 'Khoor, Zruog!', 4, 0, 0),
(@pid, '0\nabc', 'abc', 4, 1, 1),
(@pid, '25\na', 'z', 4, 1, 2),
(@pid, '1\nxyz XYZ', 'yza YZA', 4, 1, 3),
(@pid, '13\nThe Quick Brown Fox', 'Gur Dhvpx Oebja Sbk', 4, 1, 4);

-- 罗马数字转整数
SET @pid = (SELECT id FROM problems WHERE title = '罗马数字转整数' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'MCMXCIV', '1994', 4, 0, 0),
(@pid, 'III', '3', 4, 1, 1),
(@pid, 'IV', '4', 4, 1, 2),
(@pid, 'IX', '9', 4, 1, 3),
(@pid, 'MMMCMXCIX', '3999', 4, 1, 4);

-- 字符串压缩编码
SET @pid = (SELECT id FROM problems WHERE title = '字符串压缩编码' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'aabcccccaaa', 'a2b1c5a3', 4, 0, 0),
(@pid, 'abc', 'abc', 4, 1, 1),
(@pid, 'aaa', 'a3', 4, 1, 2),
(@pid, 'a', 'a', 4, 1, 3),
(@pid, 'aabbcc', 'aabbcc', 4, 1, 4);

-- 最长公共子序列
SET @pid = (SELECT id FROM problems WHERE title = '最长公共子序列' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'abcde\nace', '3', 4, 0, 0),
(@pid, 'abc\nabc', '3', 4, 1, 1),
(@pid, 'abc\ndef', '0', 4, 1, 2),
(@pid, 'a\na', '1', 4, 1, 3),
(@pid, 'abcbdab\nbdcaba', '4', 4, 1, 4);

-- 单词翻转顺序
SET @pid = (SELECT id FROM problems WHERE title = '单词翻转顺序' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '  the sky is   blue  ', 'blue is sky the', 4, 0, 0),
(@pid, 'hello', 'hello', 4, 1, 1),
(@pid, '  hello world  ', 'world hello', 4, 1, 2),
(@pid, 'a b c', 'c b a', 4, 1, 3),
(@pid, 'Alice loves Bob', 'Bob loves Alice', 4, 1, 4);

-- 栈的基本操作
SET @pid = (SELECT id FROM problems WHERE title = '栈的基本操作' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6\npush 1\npush 2\ntop\nsize\npop\npop', '2\n2\n2\n1', 4, 0, 0),
(@pid, '3\npop\ntop\nsize', '-1\n-1\n0', 4, 1, 1),
(@pid, '4\npush 5\npush 3\npop\ntop', '3\n5', 4, 1, 2),
(@pid, '2\npush 42\ntop', '42', 4, 1, 3),
(@pid, '5\npush 1\npush 2\npush 3\nsize\npop', '3\n3', 4, 1, 4);

-- 后缀表达式求值
SET @pid = (SELECT id FROM problems WHERE title = '后缀表达式求值' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '2 1 + 3 *', '9', 4, 0, 0),
(@pid, '4 13 5 / +', '6', 4, 1, 1),
(@pid, '10 6 9 3 + -11 * / * 17 + 5 +', '22', 4, 1, 2),
(@pid, '3 4 -', '-1', 4, 1, 3),
(@pid, '5', '5', 4, 1, 4);

-- 每日温度
SET @pid = (SELECT id FROM problems WHERE title = '每日温度' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '8\n73 74 75 71 69 72 76 73', '1 1 4 2 1 1 0 0', 4, 0, 0),
(@pid, '3\n30 40 50', '1 1 0', 4, 1, 1),
(@pid, '3\n30 30 30', '0 0 0', 4, 1, 2),
(@pid, '1\n50', '0', 4, 1, 3),
(@pid, '5\n90 80 70 60 50', '0 0 0 0 0', 4, 1, 4);

-- 最小栈设计
SET @pid = (SELECT id FROM problems WHERE title = '最小栈设计' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '7\npush -2\npush 0\npush -3\ngetMin\npop\ntop\ngetMin', '-3\n0\n-2', 4, 0, 0),
(@pid, '4\npush 1\npush 1\ngetMin\npop', '1', 4, 1, 1),
(@pid, '5\npush 3\npush 2\npush 1\ngetMin\npop', '1', 4, 1, 2),
(@pid, '6\npush 5\npush 3\npush 7\ngetMin\npop\ngetMin', '3\n3', 4, 1, 3),
(@pid, '3\npush 0\ngetMin\ntop', '0\n0', 4, 1, 4);

-- 用栈实现队列
SET @pid = (SELECT id FROM problems WHERE title = '用栈实现队列' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6\nenqueue 1\nenqueue 2\nfront\ndequeue\nfront\ndequeue', '1\n1\n2\n2', 4, 0, 0),
(@pid, '3\ndequeue\nfront\nenqueue 5', '-1\n-1', 4, 1, 1),
(@pid, '5\nenqueue 10\nenqueue 20\nenqueue 30\ndequeue\nfront', '10\n20', 4, 1, 2),
(@pid, '4\nenqueue 1\ndequeue\nenqueue 2\ndequeue', '1\n2', 4, 1, 3),
(@pid, '2\nenqueue 42\nfront', '42', 4, 1, 4);

-- 两个数组的交集
SET @pid = (SELECT id FROM problems WHERE title = '两个数组的交集' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '4\n1 2 2 1\n3\n2 2 3', '2', 4, 0, 0),
(@pid, '3\n4 9 5\n4\n9 4 9 8 4', '4 9', 4, 1, 1),
(@pid, '2\n1 2\n2\n3 4', 'empty', 4, 1, 2),
(@pid, '3\n1 1 1\n3\n1 1 1', '1', 4, 1, 3),
(@pid, '1\n5\n1\n5', '5', 4, 1, 4);

-- 同构字符串判断
SET @pid = (SELECT id FROM problems WHERE title = '同构字符串判断' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, 'egg\nadd', 'true', 4, 0, 0),
(@pid, 'foo\nbar', 'false', 4, 1, 1),
(@pid, 'paper\ntitle', 'true', 4, 1, 2),
(@pid, 'ab\naa', 'false', 4, 1, 3),
(@pid, 'a\na', 'true', 4, 1, 4);

-- 字母异位词分组
SET @pid = (SELECT id FROM problems WHERE title = '字母异位词分组' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6\neat tea tan ate nat bat', 'eat tea ate\ntan nat\nbat', 4, 0, 0),
(@pid, '1\na', 'a', 4, 1, 1),
(@pid, '3\nabc bca cab', 'abc bca cab', 4, 1, 2),
(@pid, '4\nlisten silent abc def', 'listen silent\nabc\ndef', 4, 1, 3),
(@pid, '2\nab ba', 'ab ba', 4, 1, 4);

-- 最长连续序列
SET @pid = (SELECT id FROM problems WHERE title = '最长连续序列' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6\n100 4 200 1 3 2', '4', 4, 0, 0),
(@pid, '1\n0', '1', 4, 1, 1),
(@pid, '9\n0 3 7 2 5 8 4 6 1', '9', 4, 1, 2),
(@pid, '4\n1 2 0 1', '3', 4, 1, 3),
(@pid, '5\n10 20 30 40 50', '1', 4, 1, 4);

-- 和为K的子数组
SET @pid = (SELECT id FROM problems WHERE title = '和为K的子数组' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5 3\n1 1 1 2 3', '3', 4, 0, 0),
(@pid, '3 2\n1 1 1', '2', 4, 1, 1),
(@pid, '5 0\n1 -1 1 -1 1', '4', 4, 1, 2),
(@pid, '1 0\n0', '1', 4, 1, 3),
(@pid, '3 5\n1 2 3', '1', 4, 1, 4);

-- 设置 debug 示例
UPDATE problems SET debug_input_example = input_example, debug_output_example = output_example
WHERE debug_input_example IS NULL;
