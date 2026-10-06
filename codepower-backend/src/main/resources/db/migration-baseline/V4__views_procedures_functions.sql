-- B4: beta1.0 视图 / 函数 / 存储过程 / 游标
-- 用途：满足数据库课程对象要求，并服务答辩展示。

DROP VIEW IF EXISTS vw_contest_overview;
CREATE VIEW vw_contest_overview AS
SELECT
    c.id,
    c.title,
    c.type,
    c.status,
    c.start_time,
    c.end_time,
    c.duration_minutes,
    c.is_public,
    c.is_official,
    c.creator_id,
    COUNT(DISTINCT cr.user_id) AS participant_count,
    COUNT(DISTINCT cp.problem_id) AS problem_count
FROM contests c
LEFT JOIN contest_registrations cr ON cr.contest_id = c.id
LEFT JOIN contest_problems cp ON cp.contest_id = c.id
WHERE c.deleted = 0
GROUP BY c.id, c.title, c.type, c.status, c.start_time, c.end_time, c.duration_minutes, c.is_public, c.is_official, c.creator_id;

DROP VIEW IF EXISTS vw_problem_set_overview;
CREATE VIEW vw_problem_set_overview AS
SELECT
    ps.id,
    ps.title,
    ps.difficulty,
    ps.status,
    ps.creator_id,
    ps.problem_count,
    COUNT(psi.id) AS actual_problem_count,
    ps.created_at,
    ps.updated_at
FROM problem_sets ps
LEFT JOIN problem_set_items psi ON psi.problem_set_id = ps.id
WHERE ps.deleted = 0
GROUP BY ps.id, ps.title, ps.difficulty, ps.status, ps.creator_id, ps.problem_count, ps.created_at, ps.updated_at;

DROP VIEW IF EXISTS vw_user_inbox_overview;
CREATE VIEW vw_user_inbox_overview AS
SELECT
    u.id AS user_id,
    u.username,
    COALESCE(pm.unread_private_count, 0) AS unread_private_count,
    COALESCE(notif.unread_notification_count, 0) AS unread_notification_count,
    COALESCE(ann.unread_announcement_count, 0) AS unread_announcement_count,
    COALESCE(pm.unread_private_count, 0) + COALESCE(notif.unread_notification_count, 0) + COALESCE(ann.unread_announcement_count, 0) AS total_unread_count
FROM users u
LEFT JOIN (
    SELECT to_user_id AS user_id, COUNT(*) AS unread_private_count
    FROM private_messages
    WHERE is_read = 0
    GROUP BY to_user_id
) pm ON pm.user_id = u.id
LEFT JOIN (
    SELECT user_id, COUNT(*) AS unread_notification_count
    FROM notifications
    WHERE is_read = 0
    GROUP BY user_id
) notif ON notif.user_id = u.id
LEFT JOIN (
    SELECT u2.id AS user_id, COUNT(sa.id) AS unread_announcement_count
    FROM users u2
    JOIN system_announcements sa
      ON sa.status = 'PUBLISHED'
     AND (sa.start_time IS NULL OR sa.start_time <= NOW())
     AND (sa.end_time IS NULL OR sa.end_time >= NOW())
    LEFT JOIN user_announcement_reads uar
      ON uar.announcement_id = sa.id AND uar.user_id = u2.id
    WHERE uar.id IS NULL
    GROUP BY u2.id
) ann ON ann.user_id = u.id
WHERE u.deleted = 0;

DROP FUNCTION IF EXISTS fn_contest_participant_count;
DELIMITER $$
CREATE FUNCTION fn_contest_participant_count(p_contest_id BIGINT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(*) INTO v_count
    FROM contest_registrations
    WHERE contest_id = p_contest_id;
    RETURN v_count;
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_problem_set_actual_count;
DELIMITER $$
CREATE FUNCTION fn_problem_set_actual_count(p_problem_set_id BIGINT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(*) INTO v_count
    FROM problem_set_items
    WHERE problem_set_id = p_problem_set_id;
    RETURN v_count;
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_user_total_inbox_unread;
DELIMITER $$
CREATE FUNCTION fn_user_total_inbox_unread(p_user_id BIGINT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total INT DEFAULT 0;
    SELECT total_unread_count INTO v_total
    FROM vw_user_inbox_overview
    WHERE user_id = p_user_id;
    RETURN COALESCE(v_total, 0);
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_refresh_problem_set_problem_count;
DELIMITER $$
CREATE PROCEDURE sp_refresh_problem_set_problem_count(IN p_problem_set_id BIGINT)
BEGIN
    UPDATE problem_sets
    SET problem_count = (
        SELECT COUNT(*) FROM problem_set_items WHERE problem_set_id = p_problem_set_id
    )
    WHERE id = p_problem_set_id;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_refresh_problem_accept_stats;
DELIMITER $$
CREATE PROCEDURE sp_refresh_problem_accept_stats()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_problem_id BIGINT;
    DECLARE v_submit_count INT;
    DECLARE v_accept_count INT;
    DECLARE cur CURSOR FOR SELECT id FROM problems WHERE deleted = 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_problem_id;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SELECT COUNT(*), SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END)
        INTO v_submit_count, v_accept_count
        FROM submissions
        WHERE problem_id = v_problem_id;

        UPDATE problems
        SET submit_count = COALESCE(v_submit_count, 0),
            accept_count = COALESCE(v_accept_count, 0),
            accept_rate = CASE
                WHEN COALESCE(v_submit_count, 0) = 0 THEN 0
                ELSE ROUND(COALESCE(v_accept_count, 0) * 100.0 / v_submit_count, 2)
            END
        WHERE id = v_problem_id;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_soft_delete_user_with_audit;
DELIMITER $$
CREATE PROCEDURE sp_soft_delete_user_with_audit(IN p_user_id BIGINT)
BEGIN
    UPDATE users
    SET deleted = 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id AND deleted = 0;

    INSERT INTO notifications (user_id, type, title, content, related_id, is_read)
    VALUES (p_user_id, 'SYSTEM', '账号状态变更', '您的账号已被系统执行逻辑删除。', p_user_id, 0);
END$$
DELIMITER ;
