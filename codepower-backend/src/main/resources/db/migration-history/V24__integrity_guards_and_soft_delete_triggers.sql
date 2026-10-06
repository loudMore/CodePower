-- V24: 数据库完整性修复与删除联动保护
-- 目标：补齐缺失外键/级联规则，并为软删除主表增加联动触发器，避免孤儿数据。

-- ===================== 1. 补齐缺失外键级联规则 =====================

-- comments.parent_id -> comments.id 改为级联删除，删除父评论时自动清理子评论
ALTER TABLE comments DROP FOREIGN KEY fk_comments_parent;
ALTER TABLE comments
  ADD CONSTRAINT fk_comments_parent
  FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE;

-- comment_likes.comment_id -> comments.id 改为级联删除，删除评论时自动清理点赞
ALTER TABLE comment_likes DROP FOREIGN KEY fk_comment_likes_comment;
ALTER TABLE comment_likes
  ADD CONSTRAINT fk_comment_likes_comment
  FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE;

-- notifications.user_id -> users.id 增加级联删除
ALTER TABLE notifications DROP FOREIGN KEY fk_notif_user;
ALTER TABLE notifications
  ADD CONSTRAINT fk_notif_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- private_messages 双向用户外键增加级联删除
ALTER TABLE private_messages DROP FOREIGN KEY fk_pm_from_user;
ALTER TABLE private_messages DROP FOREIGN KEY fk_pm_to_user;
ALTER TABLE private_messages
  ADD CONSTRAINT fk_pm_from_user
  FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_pm_to_user
  FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE;

-- system_announcements.creator_id 增加级联删除
ALTER TABLE system_announcements DROP FOREIGN KEY fk_announcement_creator;
ALTER TABLE system_announcements
  ADD CONSTRAINT fk_announcement_creator
  FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE;

-- user_announcement_reads.user_id 增加级联删除
ALTER TABLE user_announcement_reads DROP FOREIGN KEY fk_uar_user;
ALTER TABLE user_announcement_reads
  ADD CONSTRAINT fk_uar_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- user_learning_progress 三个外键统一为级联删除
ALTER TABLE user_learning_progress DROP FOREIGN KEY fk_progress_user;
ALTER TABLE user_learning_progress DROP FOREIGN KEY fk_progress_path;
ALTER TABLE user_learning_progress DROP FOREIGN KEY fk_progress_stage;
ALTER TABLE user_learning_progress
  ADD CONSTRAINT fk_progress_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_progress_path
  FOREIGN KEY (path_id) REFERENCES learning_paths(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_progress_stage
  FOREIGN KEY (stage_id) REFERENCES learning_path_stages(id) ON DELETE CASCADE;

-- contest_registrations 对竞赛/用户删除自动清理
ALTER TABLE contest_registrations DROP FOREIGN KEY fk_cr_contest;
ALTER TABLE contest_registrations DROP FOREIGN KEY fk_cr_user;
ALTER TABLE contest_registrations
  ADD CONSTRAINT fk_cr_contest
  FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_cr_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- contest_problems.problem_id 增加级联删除
ALTER TABLE contest_problems DROP FOREIGN KEY fk_cp_problem;
ALTER TABLE contest_problems
  ADD CONSTRAINT fk_cp_problem
  FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE;

-- ai_conversations.user_id 增加级联删除
ALTER TABLE ai_conversations DROP FOREIGN KEY fk_ai_conv_user;
ALTER TABLE ai_conversations
  ADD CONSTRAINT fk_ai_conv_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- user_abilities 两侧外键增加级联删除
ALTER TABLE user_abilities DROP FOREIGN KEY fk_ability_user;
ALTER TABLE user_abilities DROP FOREIGN KEY fk_ability_tag;
ALTER TABLE user_abilities
  ADD CONSTRAINT fk_ability_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_ability_tag
  FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE;

-- problem_reports 两侧外键增加级联删除
ALTER TABLE problem_reports DROP FOREIGN KEY fk_report_problem;
ALTER TABLE problem_reports DROP FOREIGN KEY fk_report_reporter;
ALTER TABLE problem_reports
  ADD CONSTRAINT fk_report_problem
  FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_report_reporter
  FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE;

-- user_follows 双向用户外键增加级联删除
ALTER TABLE user_follows DROP FOREIGN KEY user_follows_ibfk_1;
ALTER TABLE user_follows DROP FOREIGN KEY user_follows_ibfk_2;
ALTER TABLE user_follows
  ADD CONSTRAINT fk_user_follows_follower
  FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_user_follows_following
  FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE;

-- submissions.contest_id 补齐外键，竞赛删除时清空归属，保留提交历史
SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'submissions'
    AND constraint_name = 'fk_submissions_contest'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE submissions ADD CONSTRAINT fk_submissions_contest FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE SET NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================== 2. 软删除联动触发器 =====================

DROP TRIGGER IF EXISTS trg_users_soft_delete_cascade;
DELIMITER $$
CREATE TRIGGER trg_users_soft_delete_cascade
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        UPDATE user_profiles SET updated_at = CURRENT_TIMESTAMP WHERE user_id = NEW.id;
        UPDATE problems SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE author_id = NEW.id AND deleted = 0;
        UPDATE problem_solutions SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE user_id = NEW.id AND deleted = 0;
        DELETE FROM notifications WHERE user_id = NEW.id;
        DELETE FROM private_messages WHERE from_user_id = NEW.id OR to_user_id = NEW.id;
        DELETE FROM contest_registrations WHERE user_id = NEW.id;
        DELETE FROM user_learning_progress WHERE user_id = NEW.id;
        DELETE FROM user_abilities WHERE user_id = NEW.id;
        DELETE FROM comment_likes WHERE user_id = NEW.id;
        DELETE FROM comments WHERE user_id = NEW.id;
        DELETE FROM favorite_problems WHERE user_id = NEW.id;
        DELETE FROM user_follows WHERE follower_id = NEW.id OR following_id = NEW.id;
        DELETE FROM ai_conversations WHERE user_id = NEW.id;
        DELETE FROM user_announcement_reads WHERE user_id = NEW.id;
        DELETE FROM problem_reports WHERE reporter_id = NEW.id;
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_problems_soft_delete_cascade;
DELIMITER $$
CREATE TRIGGER trg_problems_soft_delete_cascade
AFTER UPDATE ON problems
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        UPDATE problem_solutions SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE problem_id = NEW.id AND deleted = 0;
        DELETE FROM comments WHERE target_type = 'PROBLEM' AND target_id = NEW.id;
        DELETE FROM judge_test_cases WHERE problem_id = NEW.id;
        DELETE FROM favorite_problems WHERE problem_id = NEW.id;
        DELETE FROM contest_problems WHERE problem_id = NEW.id;
        UPDATE learning_path_stages SET problem_id = NULL WHERE problem_id = NEW.id;
        UPDATE ai_conversations SET problem_id = NULL WHERE problem_id = NEW.id;
        DELETE FROM problem_reports WHERE problem_id = NEW.id;
        DELETE FROM problem_tags WHERE problem_id = NEW.id;
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_problem_solutions_soft_delete_cleanup;
DELIMITER $$
CREATE TRIGGER trg_problem_solutions_soft_delete_cleanup
AFTER UPDATE ON problem_solutions
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        DELETE FROM comments WHERE target_type = 'SOLUTION' AND target_id = NEW.id;
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_learning_paths_soft_delete_cleanup;
DELIMITER $$
CREATE TRIGGER trg_learning_paths_soft_delete_cleanup
AFTER UPDATE ON learning_paths
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        DELETE FROM user_learning_progress WHERE path_id = NEW.id;
        DELETE FROM learning_path_stages WHERE path_id = NEW.id;
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_contests_soft_delete_cleanup;
DELIMITER $$
CREATE TRIGGER trg_contests_soft_delete_cleanup
AFTER UPDATE ON contests
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        DELETE FROM contest_registrations WHERE contest_id = NEW.id;
        DELETE FROM contest_problems WHERE contest_id = NEW.id;
        UPDATE submissions SET contest_id = NULL WHERE contest_id = NEW.id;
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_problem_sets_soft_delete_cleanup;
DELIMITER $$
CREATE TRIGGER trg_problem_sets_soft_delete_cleanup
AFTER UPDATE ON problem_sets
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        DELETE FROM problem_set_items WHERE set_id = NEW.id;
    END IF;
END$$
DELIMITER ;
