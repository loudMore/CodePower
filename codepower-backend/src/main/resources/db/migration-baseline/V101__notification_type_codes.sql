-- 补齐系统通知类型字典，避免 notifications.type 外键约束导致通知写入失败。
INSERT INTO dict_notification_type (code, name) VALUES
('WELCOME', '欢迎通知'),
('LEVEL_UP', '等级升级'),
('CHECK_IN_MILESTONE', '签到里程碑'),
('COMMENT_REPLY', '评论回复'),
('CONTEST_REGISTER', '竞赛报名'),
('ROLE_UPDATED', '角色变更'),
('ACCOUNT_STATUS_UPDATED', '账号状态变更'),
('UPGRADE_APPROVED', '角色升级通过'),
('UPGRADE_REJECTED', '角色升级拒绝'),
('UPGRADE_REQUEST_SUBMITTED', '角色升级申请已提交'),
('UPGRADE_REQUEST_CREATED', '新的角色升级申请'),
('PASSWORD_RESET', '管理员重置密码'),
('PASSWORD_CHANGED', '用户修改密码'),
('PASSWORD_RESET_SELF', '邮箱重置密码'),
('EMAIL_CHANGED', '邮箱变更'),
('PROBLEM_REPORT', '题目报告'),
('REPORT_HANDLED', '题目报告处理')
ON DUPLICATE KEY UPDATE name = VALUES(name);
