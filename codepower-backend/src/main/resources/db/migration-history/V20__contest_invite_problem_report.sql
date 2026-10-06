-- V20: 竞赛邀请码 + 题目报告机制

-- 1. contests 增加邀请码和公开/非公开字段
ALTER TABLE contests
  ADD COLUMN invite_code VARCHAR(16) NULL COMMENT '邀请码，非公开竞赛的唯一入口',
  ADD COLUMN is_public TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开(0:非公开 1:公开)';

-- 为已有竞赛生成邀请码（8位随机字符串）
UPDATE contests SET invite_code = UPPER(SUBSTRING(MD5(RAND()), 1, 8)) WHERE invite_code IS NULL;

-- 2. 题目报告表
CREATE TABLE IF NOT EXISTS problem_reports (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  problem_id BIGINT UNSIGNED NOT NULL,
  reporter_id BIGINT UNSIGNED NOT NULL,
  report_type VARCHAR(32) NOT NULL DEFAULT 'BUG' COMMENT 'BUG/WRONG_ANSWER/UNCLEAR/OTHER',
  content TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RESOLVED/DISMISSED',
  reply TEXT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_report_problem (problem_id),
  KEY idx_report_reporter (reporter_id),
  CONSTRAINT fk_report_problem FOREIGN KEY (problem_id) REFERENCES problems(id),
  CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
