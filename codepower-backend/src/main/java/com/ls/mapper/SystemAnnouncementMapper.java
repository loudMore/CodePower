/**
 * 文件说明：系统公告 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.SystemAnnouncement;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 系统公告 Mapper */
@Mapper
public interface SystemAnnouncementMapper extends BaseMapper<SystemAnnouncement> {

    @Insert("INSERT INTO user_announcement_reads (user_id, announcement_id) VALUES (#{userId}, #{announcementId}) ON DUPLICATE KEY UPDATE user_id=user_id")
    void insertRead(@Param("userId") Long userId, @Param("announcementId") Long announcementId);

    @Select("SELECT COUNT(*) FROM system_announcements sa " +
            "WHERE sa.status = 1 " +
            "AND sa.start_time <= NOW() " +
            "AND (sa.end_time IS NULL OR sa.end_time >= NOW()) " +
            "AND NOT EXISTS (SELECT 1 FROM user_announcement_reads uar WHERE uar.user_id = #{userId} AND uar.announcement_id = sa.id)")
    long countUnread(@Param("userId") Long userId);
}
