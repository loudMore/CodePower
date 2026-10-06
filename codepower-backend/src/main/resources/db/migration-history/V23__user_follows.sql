-- V23: 用户关注/粉丝系统
CREATE TABLE IF NOT EXISTS user_follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT UNSIGNED NOT NULL COMMENT '关注者',
    following_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id),
    INDEX idx_following (following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id),
    FOREIGN KEY (following_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系';

-- 给 private_messages 加 type 字段区分普通私信和系统消息
ALTER TABLE private_messages ADD COLUMN msg_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/SYSTEM' AFTER content;
