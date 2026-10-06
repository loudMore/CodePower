-- B3: beta1.0 完整性约束与触发器
-- 用途：提供答辩要求中的触发器与关键级联一致性。

ALTER TABLE user_profiles
    ADD CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_check_ins
    ADD CONSTRAINT fk_user_check_ins_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_daily_task_progress
    ADD CONSTRAINT fk_udtp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_udtp_task FOREIGN KEY (task_id) REFERENCES daily_tasks(id) ON DELETE CASCADE;

ALTER TABLE problems
    ADD CONSTRAINT fk_problems_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE problem_tags
    ADD CONSTRAINT fk_problem_tags_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_problem_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE;

ALTER TABLE judge_test_cases
    ADD CONSTRAINT fk_judge_test_cases_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE;

ALTER TABLE submissions
    ADD CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_submissions_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_submissions_contest FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE SET NULL;

ALTER TABLE submission_results
    ADD CONSTRAINT fk_submission_results_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_submission_results_case FOREIGN KEY (test_case_id) REFERENCES judge_test_cases(id) ON DELETE SET NULL;

ALTER TABLE problem_solutions
    ADD CONSTRAINT fk_problem_solutions_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_problem_solutions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE favorite_problems
    ADD CONSTRAINT fk_favorite_problems_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_favorite_problems_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE;

ALTER TABLE solution_codes
    ADD CONSTRAINT fk_solution_codes_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE;

ALTER TABLE comments
    ADD CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE;

ALTER TABLE comment_likes
    ADD CONSTRAINT fk_comment_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE;

ALTER TABLE learning_path_stages
    ADD CONSTRAINT fk_learning_path_stages_path FOREIGN KEY (path_id) REFERENCES learning_paths(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_learning_path_stages_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE SET NULL;

ALTER TABLE user_learning_progress
    ADD CONSTRAINT fk_user_learning_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_learning_progress_path FOREIGN KEY (path_id) REFERENCES learning_paths(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_learning_progress_stage FOREIGN KEY (stage_id) REFERENCES learning_path_stages(id) ON DELETE CASCADE;

ALTER TABLE contests
    ADD CONSTRAINT fk_contests_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE contest_problems
    ADD CONSTRAINT fk_contest_problems_contest FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_contest_problems_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE;

ALTER TABLE contest_registrations
    ADD CONSTRAINT fk_contest_registrations_contest FOREIGN KEY (contest_id) REFERENCES contests(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_contest_registrations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE ai_conversations
    ADD CONSTRAINT fk_ai_conversations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ai_conversations_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE SET NULL;

ALTER TABLE ai_messages
    ADD CONSTRAINT fk_ai_messages_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE;

ALTER TABLE user_abilities
    ADD CONSTRAINT fk_user_abilities_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_abilities_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE role_upgrade_requests
    ADD CONSTRAINT fk_role_upgrade_requests_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_role_upgrade_requests_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE private_messages
    ADD CONSTRAINT fk_private_messages_from FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_private_messages_to FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE system_announcements
    ADD CONSTRAINT fk_system_announcements_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE user_announcement_reads
    ADD CONSTRAINT fk_user_announcement_reads_announcement FOREIGN KEY (announcement_id) REFERENCES system_announcements(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_announcement_reads_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE problem_sets
    ADD CONSTRAINT fk_problem_sets_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE problem_set_items
    ADD CONSTRAINT fk_problem_set_items_set FOREIGN KEY (problem_set_id) REFERENCES problem_sets(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_problem_set_items_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE;

ALTER TABLE problem_reports
    ADD CONSTRAINT fk_problem_reports_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_problem_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_problem_reports_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE user_follows
    ADD CONSTRAINT fk_user_follows_follower FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_follows_following FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE;

DROP TRIGGER IF EXISTS trg_users_soft_delete_cascade;
DROP TRIGGER IF EXISTS trg_problems_soft_delete_cascade;
DROP TRIGGER IF EXISTS trg_problem_solutions_soft_delete_cleanup;
DROP TRIGGER IF EXISTS trg_learning_paths_soft_delete_cleanup;
DROP TRIGGER IF EXISTS trg_contests_soft_delete_cleanup;
DROP TRIGGER IF EXISTS trg_problem_sets_soft_delete_cleanup;

DELIMITER $$

CREATE TRIGGER trg_users_soft_delete_cascade
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        UPDATE user_profiles SET updated_at = CURRENT_TIMESTAMP WHERE user_id = NEW.id;
        UPDATE problem_solutions SET deleted = 1 WHERE user_id = NEW.id AND deleted = 0;
        UPDATE comments SET deleted = 1 WHERE user_id = NEW.id AND deleted = 0;
    END IF;
END$$

CREATE TRIGGER trg_problems_soft_delete_cascade
AFTER UPDATE ON problems
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        UPDATE judge_test_cases SET updated_at = CURRENT_TIMESTAMP WHERE problem_id = NEW.id;
        UPDATE problem_solutions SET deleted = 1 WHERE problem_id = NEW.id AND deleted = 0;
    END IF;
END$$

CREATE TRIGGER trg_problem_solutions_soft_delete_cleanup
AFTER UPDATE ON problem_solutions
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        UPDATE comments SET deleted = 1
        WHERE target_type = 'SOLUTION' AND target_id = NEW.id AND deleted = 0;
    END IF;
END$$

CREATE TRIGGER trg_learning_paths_soft_delete_cleanup
AFTER UPDATE ON learning_paths
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        DELETE FROM user_learning_progress WHERE path_id = NEW.id;
    END IF;
END$$

CREATE TRIGGER trg_contests_soft_delete_cleanup
AFTER UPDATE ON contests
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        DELETE FROM contest_registrations WHERE contest_id = NEW.id;
    END IF;
END$$

CREATE TRIGGER trg_problem_sets_soft_delete_cleanup
AFTER UPDATE ON problem_sets
FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
        DELETE FROM problem_set_items WHERE problem_set_id = NEW.id;
    END IF;
END$$

DELIMITER ;
