-- V25: 课程要求型数据库对象（视图 / 函数 / 存储过程 / 游标）
-- 说明：本版本主要用于满足数据库课程对高级对象的要求。
-- 这些对象尽量贴近现有业务建模，但默认不强制由后端直接调用，避免影响现有功能与性能。

-- ===================== 1. 视图 =====================

DROP VIEW IF EXISTS vw_contest_overview;
CREATE VIEW vw_contest_overview AS
SELECT
    c.id,
    c.title,
    c.type,
    c.status,
    c.creator_id,
    c.max_participants,
    c.start_time,
    c.end_time,
    c.allow_paste,
    c.is_public,
    c.invite_code,
    c.deleted,
    COUNT(DISTINCT cp.problem_id) AS problem_count,
    COUNT(DISTINCT cr.user_id) AS participant_count
FROM contests c
LEFT JOIN contest_problems cp ON cp.contest_id = c.id
LEFT JOIN contest_registrations cr ON cr.contest_id = c.id
GROUP BY
    c.id, c.title, c.type, c.status, c.creator_id, c.max_participants,
    c.start_time, c.end_time, c.allow_paste, c.is_public, c.invite_code, c.deleted;

DROP VIEW IF EXISTS vw_problem_set_overview;
CREATE VIEW vw_problem_set_overview AS
SELECT
    ps.id,
    ps.title,
    ps.type,
    ps.creator_id,
    ps.is_public,
    ps.problem_count AS cached_problem_count,
    COUNT(psi.problem_id) AS actual_problem_count,
    ps.deleted,
    ps.created_at,
    ps.updated_at
FROM problem_sets ps
LEFT JOIN problem_set_items psi ON psi.set_id = ps.id
GROUP BY
    ps.id, ps.title, ps.type, ps.creator_id, ps.is_public,
    ps.problem_count, ps.deleted, ps.created_at, ps.updated_at;

DROP VIEW IF EXISTS vw_user_inbox_overview;
CREATE VIEW vw_user_inbox_overview AS
SELECT
    u.id AS user_id,
    u.username,
    COALESCE(pm_stats.unread_private_count, 0) AS unread_private_count,
    COALESCE(notif_stats.unread_notification_count, 0) AS unread_notification_count,
    COALESCE(follower_stats.follower_count, 0) AS follower_count,
    COALESCE(following_stats.following_count, 0) AS following_count
FROM users u
LEFT JOIN (
    SELECT to_user_id, COUNT(*) AS unread_private_count
    FROM private_messages
    WHERE is_read = 0
    GROUP BY to_user_id
) pm_stats ON pm_stats.to_user_id = u.id
LEFT JOIN (
    SELECT user_id, COUNT(*) AS unread_notification_count
    FROM notifications
    WHERE is_read = 0
    GROUP BY user_id
) notif_stats ON notif_stats.user_id = u.id
LEFT JOIN (
    SELECT following_id, COUNT(*) AS follower_count
    FROM user_follows
    GROUP BY following_id
) follower_stats ON follower_stats.following_id = u.id
LEFT JOIN (
    SELECT follower_id, COUNT(*) AS following_count
    FROM user_follows
    GROUP BY follower_id
) following_stats ON following_stats.follower_id = u.id
WHERE u.deleted = 0;

-- ===================== 2. 函数 =====================

DROP FUNCTION IF EXISTS fn_contest_participant_count;
DELIMITER $$
CREATE FUNCTION fn_contest_participant_count(p_contest_id BIGINT UNSIGNED)
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_count
    FROM contest_registrations
    WHERE contest_id = p_contest_id;

    RETURN COALESCE(v_count, 0);
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_problem_set_actual_count;
DELIMITER $$
CREATE FUNCTION fn_problem_set_actual_count(p_set_id BIGINT UNSIGNED)
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_count
    FROM problem_set_items
    WHERE set_id = p_set_id;

    RETURN COALESCE(v_count, 0);
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_user_total_inbox_unread;
DELIMITER $$
CREATE FUNCTION fn_user_total_inbox_unread(p_user_id BIGINT UNSIGNED)
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_pm_unread INT DEFAULT 0;
    DECLARE v_notification_unread INT DEFAULT 0;

    SELECT COUNT(*) INTO v_pm_unread
    FROM private_messages
    WHERE to_user_id = p_user_id AND is_read = 0;

    SELECT COUNT(*) INTO v_notification_unread
    FROM notifications
    WHERE user_id = p_user_id AND is_read = 0;

    RETURN COALESCE(v_pm_unread, 0) + COALESCE(v_notification_unread, 0);
END$$
DELIMITER ;

-- ===================== 3. 存储过程（含游标） =====================

DROP PROCEDURE IF EXISTS sp_refresh_problem_set_problem_count;
DELIMITER $$
CREATE PROCEDURE sp_refresh_problem_set_problem_count()
BEGIN
    DECLARE v_set_id BIGINT UNSIGNED;
    DECLARE v_count INT;
    DECLARE done INT DEFAULT 0;

    DECLARE cur_problem_sets CURSOR FOR
        SELECT id
        FROM problem_sets
        WHERE deleted = 0;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur_problem_sets;

    refresh_loop: LOOP
        FETCH cur_problem_sets INTO v_set_id;
        IF done = 1 THEN
            LEAVE refresh_loop;
        END IF;

        SELECT COUNT(*) INTO v_count
        FROM problem_set_items
        WHERE set_id = v_set_id;

        UPDATE problem_sets
        SET problem_count = COALESCE(v_count, 0),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = v_set_id;
    END LOOP;

    CLOSE cur_problem_sets;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_refresh_problem_accept_stats;
DELIMITER $$
CREATE PROCEDURE sp_refresh_problem_accept_stats()
BEGIN
    DECLARE v_problem_id BIGINT UNSIGNED;
    DECLARE v_submit_count INT;
    DECLARE v_accept_count INT;
    DECLARE v_accept_rate DECIMAL(5,2);
    DECLARE done INT DEFAULT 0;

    DECLARE cur_problems CURSOR FOR
        SELECT id
        FROM problems
        WHERE deleted = 0;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur_problems;

    problem_loop: LOOP
        FETCH cur_problems INTO v_problem_id;
        IF done = 1 THEN
            LEAVE problem_loop;
        END IF;

        SELECT COUNT(*) INTO v_submit_count
        FROM submissions
        WHERE problem_id = v_problem_id;

        SELECT COUNT(DISTINCT user_id) INTO v_accept_count
        FROM submissions
        WHERE problem_id = v_problem_id
          AND status = 'ACCEPTED';

        SET v_accept_rate = CASE
            WHEN COALESCE(v_submit_count, 0) = 0 THEN 0
            ELSE ROUND(v_accept_count * 100.0 / v_submit_count, 2)
        END;

        UPDATE problems
        SET submit_count = COALESCE(v_submit_count, 0),
            accept_count = COALESCE(v_accept_count, 0),
            accept_rate = COALESCE(v_accept_rate, 0),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = v_problem_id;
    END LOOP;

    CLOSE cur_problems;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_soft_delete_user_with_audit;
DELIMITER $$
CREATE PROCEDURE sp_soft_delete_user_with_audit(IN p_user_id BIGINT UNSIGNED)
BEGIN
    -- 该过程主要作为教学型对象保留：
    -- 1. 调用时执行逻辑删除
    -- 2. 由 V24 中的触发器完成后续联动清理
    UPDATE users
    SET deleted = 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id
      AND deleted = 0;
END$$
DELIMITER ;
