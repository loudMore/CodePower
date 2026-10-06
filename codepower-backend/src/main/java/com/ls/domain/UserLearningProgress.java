/**
 * 文件说明：用户学习路线进度 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_learning_progress")
/** 用户学习进度实体 */
public class UserLearningProgress {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long pathId;
    private Long stageId;
    private String status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
