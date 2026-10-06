INSERT INTO dict_notification_type (code, name) VALUES
('PROBLEM_REPORT_PENDING', '题目反馈待处理'),
('PROBLEM_REPORT_HANDLED', '题目反馈处理完成')
ON DUPLICATE KEY UPDATE name = VALUES(name);
