/**
 * 文件说明：学习路线阶段 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("learning_path_stages")
/** 学习路线阶段实体 */
public class LearningPathStage {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long pathId;
    private String title;
    private String description;
    private String content;
    private Integer stageOrder;
    private Long problemId;
    private String resourceUrl;
    private Integer estimatedMinutes;

    @TableField(exist = false)
    private String progressStatus;
    @TableField(exist = false)
    private String problemTitle;
    @TableField(exist = false)
    private String problemDifficulty;
}
