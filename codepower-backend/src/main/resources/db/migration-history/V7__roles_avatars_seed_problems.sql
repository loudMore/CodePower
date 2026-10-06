-- V7: Add SENIOR_USER role and role upgrade request table

-- Seed system author user for clean-database migrations
INSERT IGNORE INTO users (id, username, email, password, role, status)
VALUES (1, 'system', 'system@codepower.local', '$2a$10$TWct2em1cb719cWt9E57UOzmwb9zrSFjV0LfxfTsdSDyDm5waOR6m', 'ADMIN', 1);

-- Add SENIOR_USER role to dictionary
INSERT IGNORE INTO dict_user_role (code, name) VALUES ('SENIOR_USER', '高级用户');

-- Role upgrade request table
CREATE TABLE IF NOT EXISTS role_upgrade_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    requested_role VARCHAR(32) NOT NULL DEFAULT 'SENIOR_USER',
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    reviewer_id BIGINT UNSIGNED NULL,
    review_comment VARCHAR(500) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (reviewer_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Set default avatar for users without one
UPDATE user_profiles SET avatar_url = '/avatars/avatar-1.svg' WHERE avatar_url IS NULL OR avatar_url = '';

-- Set all seeded problems to PUBLIC visibility
UPDATE problems SET visibility = 'PUBLIC' WHERE author_id = 1 AND visibility = 'PRIVATE';

-- Seed initial problems (经典算法题)
INSERT INTO problems (title, description, difficulty, status, author_id, input_format, output_format, input_example, output_example, time_limit, memory_limit) VALUES
('两数之和', '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值 target 的那两个整数，并返回它们的数组下标。\n\n你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。\n\n你可以按任意顺序返回答案。', '简单', 1, 1, '第一行包含两个整数 n 和 target，表示数组长度和目标值。\n第二行包含 n 个整数，表示数组元素。', '输出两个整数，表示两个元素的下标（从0开始），用空格分隔。', '4 9\n2 7 11 15', '0 1', 1000, 256),

('反转链表', '给你单链表的头节点 head，请你反转链表，并返回反转后的链表。\n\n用数组模拟链表操作。', '简单', 1, 1, '第一行包含一个整数 n，表示链表节点数。\n第二行包含 n 个整数，表示链表节点的值。', '输出反转后的链表节点值，用空格分隔。', '5\n1 2 3 4 5', '5 4 3 2 1', 1000, 256),

('最大子数组和', '给你一个整数数组 nums，请你找出一个具有最大和的连续子数组（子数组最少包含一个元素），返回其最大和。', '普通', 1, 1, '第一行包含一个整数 n，表示数组长度。\n第二行包含 n 个整数，表示数组元素。', '输出一个整数，表示最大子数组和。', '9\n-2 1 -3 4 -1 2 1 -5 4', '6', 1000, 256),

('爬楼梯', '假设你正在爬楼梯。需要 n 阶你才能到达楼顶。\n\n每次你可以爬 1 或 2 个台阶。你有多少种不同的方法可以爬到楼顶呢？', '简单', 1, 1, '输入一个正整数 n (1 <= n <= 45)，表示楼梯的阶数。', '输出一个整数，表示爬到楼顶的方法数。', '3', '3', 1000, 256),

('有效的括号', '给定一个只包括 ''('', '')'', ''{'', ''}'', ''['', '']'' 的字符串 s，判断字符串是否有效。\n\n有效字符串需满足：\n1. 左括号必须用相同类型的右括号闭合。\n2. 左括号必须以正确的顺序闭合。\n3. 每个右括号都有一个对应的相同类型的左括号。', '简单', 1, 1, '输入一个字符串 s。', '如果有效输出 true，否则输出 false。', '()[]{}', 'true', 1000, 256),

('二分查找', '给定一个 n 个元素有序的（升序）整型数组 nums 和一个目标值 target，写一个函数搜索 nums 中的 target，如果目标值存在返回下标，否则返回 -1。', '简单', 1, 1, '第一行包含两个整数 n 和 target。\n第二行包含 n 个升序整数。', '输出目标值的下标，不存在则输出 -1。', '6 9\n-1 0 3 5 9 12', '4', 1000, 256),

('合并两个有序数组', '给你两个按非递减顺序排列的整数数组 nums1 和 nums2，请你将 nums2 合并到 nums1 中，使合并后的数组同样按非递减顺序排列。', '简单', 1, 1, '第一行包含两个整数 m 和 n。\n第二行包含 m 个整数（nums1）。\n第三行包含 n 个整数（nums2）。', '输出合并后的数组，用空格分隔。', '3 3\n1 2 3\n2 5 6', '1 2 2 3 5 6', 1000, 256),

('最长公共前缀', '编写一个函数来查找字符串数组中的最长公共前缀。如果不存在公共前缀，返回空字符串 ""。', '简单', 1, 1, '第一行包含一个整数 n，表示字符串数量。\n接下来 n 行，每行一个字符串。', '输出最长公共前缀字符串。', '3\nflower\nflow\nflight', 'fl', 1000, 256),

('买卖股票的最佳时机', '给定一个数组 prices，它的第 i 个元素 prices[i] 表示一支给定股票第 i 天的价格。\n\n你只能选择某一天买入这只股票，并选择在未来的某一个不同的日子卖出该股票。设计一个算法来计算你所能获取的最大利润。', '简单', 1, 1, '第一行包含一个整数 n。\n第二行包含 n 个整数表示每天的股价。', '输出最大利润，如果不能获取利润则输出 0。', '6\n7 1 5 3 6 4', '5', 1000, 256),

('无重复字符的最长子串', '给定一个字符串 s，请你找出其中不含有重复字符的最长子串的长度。', '普通', 1, 1, '输入一个字符串 s。', '输出一个整数，表示最长无重复字符子串的长度。', 'abcabcbb', '3', 1000, 256),

('三数之和', '给你一个整数数组 nums，判断是否存在三元组 [nums[i], nums[j], nums[k]] 满足 i != j、i != k 且 j != k，同时还满足 nums[i] + nums[j] + nums[k] == 0。\n\n请你返回所有和为 0 且不重复的三元组。', '普通', 1, 1, '第一行包含一个整数 n。\n第二行包含 n 个整数。', '每行输出一个三元组，元素用空格分隔。按字典序输出。', '6\n-1 0 1 2 -1 -4', '-1 -1 2\n-1 0 1', 1000, 256),

('最长回文子串', '给你一个字符串 s，找到 s 中最长的回文子串。', '普通', 1, 1, '输入一个字符串 s（1 <= s.length <= 1000）。', '输出最长回文子串。', 'babad', 'bab', 1000, 256),

('合并区间', '以数组 intervals 表示若干个区间的集合，其中单个区间为 intervals[i] = [starti, endi]。请你合并所有重叠的区间，并返回一个不重叠的区间数组。', '普通', 1, 1, '第一行包含一个整数 n，表示区间数量。\n接下来 n 行，每行两个整数表示一个区间。', '每行输出一个合并后的区间。', '4\n1 3\n2 6\n8 10\n15 18', '1 6\n8 10\n15 18', 1000, 256),

('二叉树的层序遍历', '给你二叉树的根节点 root，返回其节点值的层序遍历。（即逐层地，从左到右访问所有节点）。\n\n输入为二叉树的数组表示（-1表示空节点）。', '普通', 1, 1, '第一行包含一个整数 n，表示数组长度。\n第二行包含 n 个整数（-1 表示空节点）。', '每行输出一层的节点值，用空格分隔。', '7\n3 9 20 -1 -1 15 7', '3\n9 20\n15 7', 1000, 256),

('最小路径和', '给定一个包含非负整数的 m x n 网格 grid，请找出一条从左上角到右下角的路径，使得路径上的数字总和为最小。\n\n说明：每次只能向下或者向右移动一步。', '普通', 1, 1, '第一行包含两个整数 m 和 n。\n接下来 m 行，每行 n 个整数。', '输出最小路径和。', '3 3\n1 3 1\n1 5 1\n4 2 1', '7', 1000, 256),

('编辑距离', '给你两个单词 word1 和 word2，请返回将 word1 转换成 word2 所使用的最少操作数。\n\n你可以对一个单词进行如下三种操作：插入一个字符、删除一个字符、替换一个字符。', '困难', 1, 1, '第一行输入字符串 word1。\n第二行输入字符串 word2。', '输出最少操作数。', 'horse\nros', '3', 1000, 256),

('接雨水', '给定 n 个非负整数表示每个宽度为 1 的柱子的高度图，计算按此排列的柱子，下雨之后能接多少雨水。', '困难', 1, 1, '第一行包含一个整数 n。\n第二行包含 n 个非负整数。', '输出能接的雨水总量。', '12\n0 1 0 2 1 0 1 3 2 1 2 1', '6', 2000, 256),

('LRU缓存', '请你设计并实现一个满足 LRU (最近最少使用) 缓存约束的数据结构。\n\n实现 LRUCache 类：\n- LRUCache(int capacity) 以正整数作为容量初始化 LRU 缓存\n- int get(int key) 如果关键字 key 存在于缓存中，则返回关键字的值，否则返回 -1\n- void put(int key, int value) 如果关键字 key 已经存在，则变更其数据值；如果不存在，则向缓存中插入。当缓存容量达到上限时，删除最久未使用的数据。', '困难', 1, 1, '第一行两个整数 capacity 和 n（操作数）。\n接下来 n 行，每行格式为：\nget key\nput key value', '对于每个 get 操作，输出返回值。', '2 6\nput 1 1\nput 2 2\nget 1\nput 3 3\nget 2\nget 3', '1\n-1\n3', 1000, 256),

('最长递增子序列', '给你一个整数数组 nums，找到其中最长严格递增子序列的长度。', '普通', 1, 1, '第一行包含一个整数 n。\n第二行包含 n 个整数。', '输出最长递增子序列的长度。', '8\n10 9 2 5 3 7 101 18', '4', 1000, 256),

('岛屿数量', '给你一个由 ''1''（陆地）和 ''0''（水）组成的的二维网格，请你计算网格中岛屿的数量。\n\n岛屿总是被水包围，并且每座岛屿只能由水平方向和/或垂直方向上相邻的陆地连接形成。', '普通', 1, 1, '第一行包含两个整数 m 和 n。\n接下来 m 行，每行 n 个字符（0或1）。', '输出岛屿数量。', '4 5\n1 1 1 1 0\n1 1 0 1 0\n1 1 0 0 0\n0 0 0 0 0', '1', 1000, 256);

-- Add tags for the seeded problems
INSERT IGNORE INTO tags (name) VALUES ('数组'), ('链表'), ('动态规划'), ('字符串'), ('栈'), ('二分查找'), ('排序'), ('树'), ('图'), ('贪心'), ('双指针'), ('滑动窗口'), ('哈希表'), ('BFS'), ('DFS'), ('设计');

-- Link problems to tags (based on insert order, IDs may vary)
-- We use a safe approach: link by title match
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '两数之和' AND t.name IN ('数组', '哈希表');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '反转链表' AND t.name = '链表';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最大子数组和' AND t.name IN ('数组', '动态规划');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '爬楼梯' AND t.name = '动态规划';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '有效的括号' AND t.name IN ('字符串', '栈');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '二分查找' AND t.name IN ('数组', '二分查找');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '合并两个有序数组' AND t.name IN ('数组', '排序', '双指针');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最长公共前缀' AND t.name = '字符串';
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '买卖股票的最佳时机' AND t.name IN ('数组', '动态规划', '贪心');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '无重复字符的最长子串' AND t.name IN ('字符串', '滑动窗口', '哈希表');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '三数之和' AND t.name IN ('数组', '双指针', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最长回文子串' AND t.name IN ('字符串', '动态规划');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '合并区间' AND t.name IN ('数组', '排序');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '二叉树的层序遍历' AND t.name IN ('树', 'BFS');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最小路径和' AND t.name IN ('数组', '动态规划');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '编辑距离' AND t.name IN ('字符串', '动态规划');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '接雨水' AND t.name IN ('数组', '双指针', '栈');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = 'LRU缓存' AND t.name IN ('设计', '哈希表', '链表');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '最长递增子序列' AND t.name IN ('数组', '动态规划', '二分查找');
INSERT IGNORE INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t WHERE p.title = '岛屿数量' AND t.name IN ('图', 'DFS', 'BFS');
