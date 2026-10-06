/**
 * 文件说明：学习路线 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("learning_paths")
/** 学习路线实体 */
public class LearningPath {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String difficulty;
    private String language;
    private String coverUrl;
    private Integer estimatedHours;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private java.util.List<LearningPathStage> stages;
    @TableField(exist = false)
    private Integer totalStages;
    @TableField(exist = false)
    private Integer completedStages;
}
