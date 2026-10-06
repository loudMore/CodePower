-- V17: 种子题目 - 排序/双指针/树/图 (20题，普通为主)
-- 新增标签
INSERT IGNORE INTO tags (name) VALUES ('拓扑排序'), ('并查集'), ('回溯');

-- ============ 排序与双指针 10题 ============
INSERT INTO problems (title, description, difficulty, status, author_id, visibility,
  input_format, output_format, input_example, output_example, hint, time_limit, memory_limit) VALUES

('归并排序',
 '使用归并排序算法对一个整数数组进行升序排列，并输出排序结果。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数（-10^9 <= a_i <= 10^9），用空格分隔。',
 '输出排序后的数组，元素间用空格分隔。',
 '6\n5 2 4 6 1 3', '1 2 3 4 5 6', NULL, 2000, 262144),

('第K大元素',
 '给定一个整数数组和一个正整数 K，找出数组中第 K 大的元素。\n\n注意：第 K 大是排序后从大到小的第 K 个。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 N 和 K（1 <= K <= N <= 10^5）。\n第二行包含 N 个整数（-10^9 <= a_i <= 10^9）。',
 '输出第 K 大的元素。',
 '6 2\n3 2 1 5 6 4', '5', '可使用快速选择算法达到平均 O(n) 时间。', 1000, 262144),

('搜索旋转排序数组',
 '整数数组 nums 按升序排列后，在某个未知点进行了旋转（例如 [0,1,2,4,5,6,7] 变为 [4,5,6,7,0,1,2]）。\n\n给定旋转后的数组和一个目标值，如果目标值在数组中则输出其下标，否则输出 -1。要求 O(log n) 时间复杂度。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个整数 N 和 target（1 <= N <= 10^5）。\n第二行包含 N 个不重复整数。',
 '输出目标值的下标（0-based），不存在输出 -1。',
 '7 0\n4 5 6 7 0 1 2', '4', '修改二分查找：判断哪一半是有序的。', 1000, 262144),

('逆序对计数',
 '给定一个整数数组，求数组中逆序对的数量。\n\n逆序对定义：如果 i < j 且 a[i] > a[j]，则 (a[i], a[j]) 构成一个逆序对。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数（-10^9 <= a_i <= 10^9）。',
 '输出逆序对的数量。',
 '5\n5 4 3 2 1', '10', '利用归并排序在合并时计数。', 2000, 262144),

('颜色分类',
 '给定一个包含 N 个元素的数组，其中元素值只有 0、1、2 三种，对数组进行排序（要求原地排序，不使用库函数）。\n\n这是经典的荷兰国旗问题。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个整数（值为 0、1 或 2）。',
 '输出排序后的数组，元素间用空格分隔。',
 '6\n2 0 2 1 1 0', '0 0 1 1 2 2', '使用三指针（荷兰国旗算法）。', 1000, 262144),

('盛水最多的容器',
 '给定 N 个非负整数 a1, a2, ..., aN，每个数代表坐标点 (i, ai) 处的一条竖线。找出两条线与 x 轴组成的容器，使其盛水量最多。\n\n盛水量 = min(a[i], a[j]) × (j - i)',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（2 <= N <= 10^5）。\n第二行包含 N 个非负整数（0 <= a_i <= 10^4）。',
 '输出最大盛水量。',
 '9\n1 8 6 2 5 4 8 3 7', '49', '使用双指针从两端向中间逼近。', 1000, 262144),

('三数之和最接近',
 '给定一个整数数组 nums 和一个目标值 target，找出数组中三个整数使得它们的和最接近 target，返回这个最接近的和。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个整数 N 和 target（3 <= N <= 1000）。\n第二行包含 N 个整数（-1000 <= a_i <= 1000）。',
 '输出最接近 target 的三数之和。',
 '4 1\n-1 2 1 -4', '2', '排序后使用双指针。', 1000, 262144),

('长度最小的子数组',
 '给定一个正整数数组和一个正整数 target，找出满足其和 >= target 的长度最小的连续子数组，返回其长度。如果不存在则返回 0。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个正整数 N 和 target（1 <= N <= 10^5，1 <= target <= 10^9）。\n第二行包含 N 个正整数（1 <= a_i <= 10^4）。',
 '输出最小子数组长度，不存在输出 0。',
 '6 7\n2 3 1 2 4 3', '2', '滑动窗口法。', 1000, 262144),

('移除有序数组重复项',
 '给定一个升序排列的整数数组，原地删除重复元素，使每个元素只出现一次，输出去重后的数组。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^5）。\n第二行包含 N 个升序整数。',
 '输出去重后的数组，元素间用空格分隔。',
 '7\n1 1 2 2 3 3 4', '1 2 3 4', NULL, 1000, 262144),

('全排列生成',
 '给定一个不包含重复数字的整数数组，输出其所有可能的全排列，按字典序排列。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 8）。\n第二行包含 N 个不同的整数（-10 <= a_i <= 10）。',
 '每行输出一个排列，元素间用空格分隔。按字典序输出。',
 '3\n1 2 3', '1 2 3\n1 3 2\n2 1 3\n2 3 1\n3 1 2\n3 2 1', NULL, 2000, 262144),

-- ============ 树/图 10题 ============

('二叉树中序遍历',
 '给定一棵二叉树（数组表示，-1 表示空节点），输出它的中序遍历序列。\n\n中序遍历顺序：左子树 → 根节点 → 右子树。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N，表示数组长度。\n第二行包含 N 个整数（-1 表示空节点）。',
 '输出中序遍历序列，元素间用空格分隔。',
 '7\n1 2 3 4 5 -1 -1', '4 2 5 1 3', NULL, 1000, 262144),

('对称二叉树判断',
 '给定一棵二叉树（数组表示，-1 表示空节点），判断它是否是对称的（即左右子树互为镜像）。',
 '简单', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N，表示数组长度。\n第二行包含 N 个整数（-1 表示空节点）。',
 '如果是对称二叉树输出 true，否则输出 false。',
 '7\n1 2 2 3 4 4 3', 'true', NULL, 1000, 262144),

('验证二叉搜索树',
 '给定一棵二叉树（数组表示，-1 表示空节点），判断它是否是有效的二叉搜索树（BST）。\n\nBST 性质：左子树所有节点值 < 根节点值 < 右子树所有节点值。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N，表示数组长度。\n第二行包含 N 个整数（-1 表示空节点，其余值不重复）。',
 '如果是有效 BST 输出 true，否则输出 false。',
 '5\n2 1 3 -1 -1', 'true', '中序遍历 BST 应得到严格递增序列。', 1000, 262144),

('二叉树的最近公共祖先',
 '给定一棵二叉树（数组表示）和两个节点的值 p、q，找出它们的最近公共祖先（LCA）的值。\n\n假设 p 和 q 一定存在于树中。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N，表示数组长度。\n第二行包含 N 个整数（-1 表示空节点）。\n第三行包含两个整数 p 和 q。',
 '输出最近公共祖先的值。',
 '7\n3 5 1 6 2 0 8\n5 1', '3', NULL, 1000, 262144),

('图的BFS最短路径',
 '给定一个无权无向图和一个起始节点，使用 BFS 求起始节点到所有其他节点的最短路径长度。\n\n不可达的节点输出 -1。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含三个整数 N、M、S（1 <= N <= 10^4，0 <= M <= 10^5，1 <= S <= N），分别为节点数、边数和起始节点。\n接下来 M 行，每行两个整数 u、v（1 <= u, v <= N），表示一条无向边。',
 '输出 N 个整数，分别是起始节点到节点 1, 2, ..., N 的最短距离，用空格分隔。',
 '5 4 1\n1 2\n2 3\n1 4\n4 5', '0 1 2 1 2', NULL, 1000, 262144),

('拓扑排序',
 '给定一个有向无环图（DAG），输出其拓扑排序结果。如果有多个合法结果，输出字典序最小的。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个整数 N 和 M（1 <= N <= 10^4，0 <= M <= 10^5），分别为节点数和边数。\n接下来 M 行，每行两个整数 u 和 v（1 <= u, v <= N），表示从 u 到 v 的有向边。',
 '输出拓扑排序结果，节点编号用空格分隔。',
 '4 4\n1 2\n1 3\n2 4\n3 4', '1 2 3 4', '使用 Kahn 算法（BFS）+ 优先队列可保证字典序最小。', 1000, 262144),

('并查集与连通分量',
 '给定 N 个节点和 M 条无向边，使用并查集求图中连通分量的个数。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含两个整数 N 和 M（1 <= N <= 10^5，0 <= M <= 10^5）。\n接下来 M 行，每行两个整数 u 和 v（1 <= u, v <= N），表示一条无向边。',
 '输出连通分量的个数。',
 '5 3\n1 2\n2 3\n4 5', '2', NULL, 1000, 262144),

('二叉树的前序与中序重建',
 '给定一棵二叉树的前序遍历和中序遍历序列，重建二叉树并输出其后序遍历。\n\n假设树中没有重复的值。',
 '困难', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（1 <= N <= 10^4）。\n第二行包含 N 个整数，表示前序遍历。\n第三行包含 N 个整数，表示中序遍历。',
 '输出后序遍历序列，元素间用空格分隔。',
 '5\n3 9 20 15 7\n9 3 15 20 7', '9 15 7 20 3', NULL, 1000, 262144),

('Dijkstra最短路径',
 '给定一个带权有向图、起始节点 S 和目标节点 T，使用 Dijkstra 算法求从 S 到 T 的最短路径长度。\n\n如果不可达输出 -1。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含四个整数 N、M、S、T（1 <= N <= 10^4，0 <= M <= 10^5，1 <= S, T <= N），分别为节点数、边数、起点和终点。\n接下来 M 行，每行三个整数 u、v、w（1 <= u, v <= N，1 <= w <= 10^4），表示从 u 到 v 权重为 w 的有向边。',
 '输出最短路径长度，不可达输出 -1。',
 '5 6 1 5\n1 2 2\n1 3 4\n2 3 1\n2 4 7\n3 5 3\n4 5 1', '7', NULL, 2000, 262144),

('子集生成',
 '给定一个不包含重复元素的整数集合，输出其所有子集（幂集），按字典序排列。\n\n空集用空行表示（第一行为空行）。',
 '普通', 1, 1, 'PUBLIC',
 '第一行包含一个正整数 N（0 <= N <= 10）。\n第二行包含 N 个不同的整数（-10 <= a_i <= 10），已按升序排列。',
 '每行输出一个子集，元素间用空格分隔。按子集大小排序，同大小按字典序。空集输出一个空行。',
 '3\n1 2 3', '\n1\n2\n3\n1 2\n1 3\n2 3\n1 2 3', NULL, 1000, 262144);

-- ============ 标签关联 ============
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '归并排序' AND t.name IN ('数组', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '第K大元素' AND t.name IN ('数组', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '搜索旋转排序数组' AND t.name IN ('数组', '二分查找');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '逆序对计数' AND t.name IN ('数组', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '颜色分类' AND t.name IN ('数组', '双指针', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '盛水最多的容器' AND t.name IN ('数组', '双指针', '贪心');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '三数之和最接近' AND t.name IN ('数组', '双指针', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '长度最小的子数组' AND t.name IN ('数组', '双指针', '滑动窗口');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '移除有序数组重复项' AND t.name IN ('数组', '双指针');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '全排列生成' AND t.name IN ('数组', '回溯');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '二叉树中序遍历' AND t.name IN ('树', 'DFS');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '对称二叉树判断' AND t.name IN ('树', 'DFS');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '验证二叉搜索树' AND t.name IN ('树', 'DFS');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '二叉树的最近公共祖先' AND t.name IN ('树', 'DFS');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '图的BFS最短路径' AND t.name IN ('图', 'BFS');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '拓扑排序' AND t.name IN ('图', '拓扑排序', 'BFS');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '并查集与连通分量' AND t.name IN ('图', '并查集');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '二叉树的前序与中序重建' AND t.name IN ('树', '递归');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = 'Dijkstra最短路径' AND t.name IN ('图', '贪心');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '子集生成' AND t.name IN ('数组', '回溯');

-- ============ 测试用例 ============

-- 归并排序
SET @pid = (SELECT id FROM problems WHERE title = '归并排序' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6\n5 2 4 6 1 3', '1 2 3 4 5 6', 4, 0, 0),
(@pid, '1\n1', '1', 4, 1, 1),
(@pid, '5\n5 4 3 2 1', '1 2 3 4 5', 4, 1, 2),
(@pid, '5\n1 2 3 4 5', '1 2 3 4 5', 4, 1, 3),
(@pid, '4\n-3 0 -1 2', '-3 -1 0 2', 4, 1, 4);

-- 第K大元素
SET @pid = (SELECT id FROM problems WHERE title = '第K大元素' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6 2\n3 2 1 5 6 4', '5', 4, 0, 0),
(@pid, '9 4\n3 2 3 1 2 4 5 5 6', '4', 4, 1, 1),
(@pid, '1 1\n1', '1', 4, 1, 2),
(@pid, '5 5\n1 2 3 4 5', '1', 4, 1, 3),
(@pid, '5 1\n-1 -2 -3 -4 -5', '-1', 4, 1, 4);

-- 搜索旋转排序数组
SET @pid = (SELECT id FROM problems WHERE title = '搜索旋转排序数组' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '7 0\n4 5 6 7 0 1 2', '4', 4, 0, 0),
(@pid, '7 3\n4 5 6 7 0 1 2', '-1', 4, 1, 1),
(@pid, '1 0\n1', '-1', 4, 1, 2),
(@pid, '1 1\n1', '0', 4, 1, 3),
(@pid, '5 1\n3 4 5 1 2', '3', 4, 1, 4);

-- 逆序对计数
SET @pid = (SELECT id FROM problems WHERE title = '逆序对计数' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n5 4 3 2 1', '10', 4, 0, 0),
(@pid, '5\n1 2 3 4 5', '0', 4, 1, 1),
(@pid, '2\n2 1', '1', 4, 1, 2),
(@pid, '1\n1', '0', 4, 1, 3),
(@pid, '6\n1 3 2 3 1 5', '4', 4, 1, 4);

-- 颜色分类
SET @pid = (SELECT id FROM problems WHERE title = '颜色分类' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6\n2 0 2 1 1 0', '0 0 1 1 2 2', 4, 0, 0),
(@pid, '3\n2 0 1', '0 1 2', 4, 1, 1),
(@pid, '1\n0', '0', 4, 1, 2),
(@pid, '5\n0 0 0 0 0', '0 0 0 0 0', 4, 1, 3),
(@pid, '6\n1 0 2 0 1 2', '0 0 1 1 2 2', 4, 1, 4);

-- 盛水最多的容器
SET @pid = (SELECT id FROM problems WHERE title = '盛水最多的容器' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '9\n1 8 6 2 5 4 8 3 7', '49', 4, 0, 0),
(@pid, '2\n1 1', '1', 4, 1, 1),
(@pid, '5\n4 3 2 1 4', '16', 4, 1, 2),
(@pid, '3\n1 2 1', '2', 4, 1, 3),
(@pid, '6\n1 1 1 1 1 1', '5', 4, 1, 4);

-- 三数之和最接近
SET @pid = (SELECT id FROM problems WHERE title = '三数之和最接近' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '4 1\n-1 2 1 -4', '2', 4, 0, 0),
(@pid, '3 0\n0 0 0', '0', 4, 1, 1),
(@pid, '4 100\n1 2 3 4', '9', 4, 1, 2),
(@pid, '4 -1\n-1 0 1 2', '0', 4, 1, 3),
(@pid, '6 12\n1 2 4 8 16 32', '12', 4, 1, 4);

-- 长度最小的子数组
SET @pid = (SELECT id FROM problems WHERE title = '长度最小的子数组' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '6 7\n2 3 1 2 4 3', '2', 4, 0, 0),
(@pid, '3 11\n1 2 3', '0', 4, 1, 1),
(@pid, '1 4\n4', '1', 4, 1, 2),
(@pid, '8 15\n1 2 3 4 5 6 7 8', '2', 4, 1, 3),
(@pid, '5 5\n1 1 1 1 5', '1', 4, 1, 4);

-- 移除有序数组重复项
SET @pid = (SELECT id FROM problems WHERE title = '移除有序数组重复项' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '7\n1 1 2 2 3 3 4', '1 2 3 4', 4, 0, 0),
(@pid, '1\n1', '1', 4, 1, 1),
(@pid, '5\n1 1 1 1 1', '1', 4, 1, 2),
(@pid, '5\n1 2 3 4 5', '1 2 3 4 5', 4, 1, 3),
(@pid, '6\n-3 -3 0 0 5 5', '-3 0 5', 4, 1, 4);

-- 全排列生成
SET @pid = (SELECT id FROM problems WHERE title = '全排列生成' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3\n1 2 3', '1 2 3\n1 3 2\n2 1 3\n2 3 1\n3 1 2\n3 2 1', 4, 0, 0),
(@pid, '1\n1', '1', 4, 1, 1),
(@pid, '2\n0 1', '0 1\n1 0', 4, 1, 2),
(@pid, '2\n-1 1', '-1 1\n1 -1', 4, 1, 3),
(@pid, '3\n0 1 2', '0 1 2\n0 2 1\n1 0 2\n1 2 0\n2 0 1\n2 1 0', 4, 1, 4);

-- 二叉树中序遍历
SET @pid = (SELECT id FROM problems WHERE title = '二叉树中序遍历' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '7\n1 2 3 4 5 -1 -1', '4 2 5 1 3', 4, 0, 0),
(@pid, '1\n1', '1', 4, 1, 1),
(@pid, '3\n1 -1 2', '1 2', 4, 1, 2),
(@pid, '3\n1 2 -1', '2 1', 4, 1, 3),
(@pid, '7\n4 2 6 1 3 5 7', '1 2 3 4 5 6 7', 4, 1, 4);

-- 对称二叉树判断
SET @pid = (SELECT id FROM problems WHERE title = '对称二叉树判断' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '7\n1 2 2 3 4 4 3', 'true', 4, 0, 0),
(@pid, '5\n1 2 2 -1 3 -1 3', 'false', 4, 1, 1),
(@pid, '1\n1', 'true', 4, 1, 2),
(@pid, '3\n1 2 2', 'true', 4, 1, 3),
(@pid, '3\n1 2 3', 'false', 4, 1, 4);

-- 验证二叉搜索树
SET @pid = (SELECT id FROM problems WHERE title = '验证二叉搜索树' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n2 1 3 -1 -1', 'true', 4, 0, 0),
(@pid, '5\n5 1 4 -1 -1', 'false', 4, 1, 1),
(@pid, '1\n1', 'true', 4, 1, 2),
(@pid, '7\n5 3 7 2 4 6 8', 'true', 4, 1, 3),
(@pid, '3\n1 1 -1', 'false', 4, 1, 4);

-- 二叉树的最近公共祖先
SET @pid = (SELECT id FROM problems WHERE title = '二叉树的最近公共祖先' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '7\n3 5 1 6 2 0 8\n5 1', '3', 4, 0, 0),
(@pid, '7\n3 5 1 6 2 0 8\n5 6', '5', 4, 1, 1),
(@pid, '3\n1 2 3\n2 3', '1', 4, 1, 2),
(@pid, '1\n1\n1 1', '1', 4, 1, 3),
(@pid, '7\n3 5 1 6 2 0 8\n6 8', '3', 4, 1, 4);

-- 图的BFS最短路径
SET @pid = (SELECT id FROM problems WHERE title = '图的BFS最短路径' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5 4 1\n1 2\n2 3\n1 4\n4 5', '0 1 2 1 2', 4, 0, 0),
(@pid, '1 0 1', '0', 4, 1, 1),
(@pid, '3 0 1', '0 -1 -1', 4, 1, 2),
(@pid, '4 4 2\n1 2\n2 3\n3 4\n1 4', '1 0 1 2', 4, 1, 3),
(@pid, '3 3 1\n1 2\n2 3\n1 3', '0 1 1', 4, 1, 4);

-- 拓扑排序
SET @pid = (SELECT id FROM problems WHERE title = '拓扑排序' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '4 4\n1 2\n1 3\n2 4\n3 4', '1 2 3 4', 4, 0, 0),
(@pid, '1 0', '1', 4, 1, 1),
(@pid, '3 2\n3 1\n3 2', '3 1 2', 4, 1, 2),
(@pid, '3 0', '1 2 3', 4, 1, 3),
(@pid, '5 4\n5 1\n5 2\n1 3\n2 4', '5 1 2 3 4', 4, 1, 4);

-- 并查集与连通分量
SET @pid = (SELECT id FROM problems WHERE title = '并查集与连通分量' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5 3\n1 2\n2 3\n4 5', '2', 4, 0, 0),
(@pid, '1 0', '1', 4, 1, 1),
(@pid, '5 0', '5', 4, 1, 2),
(@pid, '4 6\n1 2\n2 3\n3 4\n1 3\n2 4\n1 4', '1', 4, 1, 3),
(@pid, '6 3\n1 2\n3 4\n5 6', '3', 4, 1, 4);

-- 二叉树的前序与中序重建
SET @pid = (SELECT id FROM problems WHERE title = '二叉树的前序与中序重建' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5\n3 9 20 15 7\n9 3 15 20 7', '9 15 7 20 3', 4, 0, 0),
(@pid, '1\n1\n1', '1', 4, 1, 1),
(@pid, '2\n1 2\n2 1', '2 1', 4, 1, 2),
(@pid, '3\n1 2 3\n2 1 3', '2 3 1', 4, 1, 3),
(@pid, '7\n1 2 4 5 3 6 7\n4 2 5 1 6 3 7', '4 5 2 6 7 3 1', 4, 1, 4);

-- Dijkstra最短路径
SET @pid = (SELECT id FROM problems WHERE title = 'Dijkstra最短路径' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '5 6 1 5\n1 2 2\n1 3 4\n2 3 1\n2 4 7\n3 5 3\n4 5 1', '7', 4, 0, 0),
(@pid, '2 0 1 2', '-1', 4, 1, 1),
(@pid, '1 0 1 1', '0', 4, 1, 2),
(@pid, '3 3 1 3\n1 2 1\n2 3 1\n1 3 10', '2', 4, 1, 3),
(@pid, '4 5 1 4\n1 2 1\n2 3 2\n3 4 3\n1 3 10\n1 4 100', '6', 4, 1, 4);

-- 子集生成
SET @pid = (SELECT id FROM problems WHERE title = '子集生成' AND author_id = 1 ORDER BY id DESC LIMIT 1);
INSERT INTO judge_test_cases (problem_id, input, expected_output, score, is_hidden, order_num) VALUES
(@pid, '3\n1 2 3', '\n1\n2\n3\n1 2\n1 3\n2 3\n1 2 3', 4, 0, 0),
(@pid, '0\n', '', 4, 1, 1),
(@pid, '1\n0', '\n0', 4, 1, 2),
(@pid, '2\n1 2', '\n1\n2\n1 2', 4, 1, 3),
(@pid, '2\n-1 1', '\n-1\n1\n-1 1', 4, 1, 4);

-- 设置 debug 示例
UPDATE problems SET debug_input_example = input_example, debug_output_example = output_example
WHERE debug_input_example IS NULL;
