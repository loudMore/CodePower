/**
 * 文件说明：私信 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 私信 Mapper */
@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    @Select("""
            SELECT
                t.other_user_id AS userId,
                u.username AS username,
                up.avatar_url AS avatar,
                t.content AS lastMessage,
                t.created_at AS lastMessageTime,
                COALESCE(unread.unread_count, 0) AS unreadCount
            FROM (
                SELECT pm1.id,
                       pm1.content,
                       pm1.created_at,
                       CASE
                           WHEN pm1.from_user_id = #{userId} THEN pm1.to_user_id
                           ELSE pm1.from_user_id
                       END AS other_user_id
                FROM private_messages pm1
                INNER JOIN (
                    SELECT MAX(id) AS latest_id
                    FROM private_messages
                    WHERE from_user_id = #{userId} OR to_user_id = #{userId}
                    GROUP BY CASE
                        WHEN from_user_id = #{userId} THEN to_user_id
                        ELSE from_user_id
                    END
                ) latest ON latest.latest_id = pm1.id
            ) t
            LEFT JOIN users u ON u.id = t.other_user_id
            LEFT JOIN user_profiles up ON up.user_id = t.other_user_id
            LEFT JOIN (
                SELECT from_user_id, COUNT(*) AS unread_count
                FROM private_messages
                WHERE to_user_id = #{userId} AND is_read = 0
                GROUP BY from_user_id
            ) unread ON unread.from_user_id = t.other_user_id
            ORDER BY t.created_at DESC
            """)
    List<Map<String, Object>> selectConversationList(@Param("userId") Long userId);
}
