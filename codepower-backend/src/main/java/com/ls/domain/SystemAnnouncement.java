/**
 * 文件说明：系统公告 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_announcements")
/** 系统公告实体 */
public class SystemAnnouncement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String type;
    private Long creatorId;
    private Integer status;
    private Integer priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
