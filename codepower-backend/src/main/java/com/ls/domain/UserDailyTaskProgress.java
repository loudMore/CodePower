/**
 * 文件说明：用户每日任务进度 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("user_daily_task_progress")
/** 用户每日任务进度实体 */
public class UserDailyTaskProgress {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long taskId;

    @TableField("task_date")
    private LocalDate taskDate;

    private Integer currentCount;
    private Boolean completed;
    private Boolean rewarded;
}
