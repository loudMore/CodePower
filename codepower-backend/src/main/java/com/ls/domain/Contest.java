/**
 * 文件说明：竞赛 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contests")
/** 竞赛实体 */
public class Contest {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Long creatorId;
    private String password;
    private Integer maxParticipants;
    private String status;
    private String allowedLanguages;
    private Integer isOfficial;
    private Integer allowPaste; // 是否允许粘贴代码(0:禁止 1:允许)
    private String inviteCode; // 邀请码
    private Integer isPublic; // 是否公开(0:非公开 1:公开)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String creatorName;
    @TableField(exist = false)
    private Integer problemCount;
    @TableField(exist = false)
    private Integer participantCount;
    @TableField(exist = false)
    private Boolean registered;
    @TableField(exist = false)
    private Boolean canRegister;
    @TableField(exist = false)
    private Boolean canAccessWorkspace;
    @TableField(exist = false)
    private Boolean privilegedParticipant;
    @TableField(exist = false)
    private Boolean participantTimeExpired;
    @TableField(exist = false)
    private LocalDateTime participantDeadline;
    @TableField(exist = false)
    private Long participantRemainingSeconds;
    @TableField(exist = false)
    private Boolean canManage;
}
