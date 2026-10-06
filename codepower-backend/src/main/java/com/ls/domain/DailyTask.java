/**
 * 文件说明：每日任务 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("daily_tasks")
/** 每日任务实体 */
public class DailyTask {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String taskKey;

    private String title;

    private String description;

    private Integer expReward;

    @TableField("ai_reward")
    private Integer aiReward;

    @TableField("required_count")
    private Integer requiredCount;

    private Boolean active;
}
