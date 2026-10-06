/**
 * 文件说明：题目反馈 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("problem_reports")
/** 题目反馈实体 */
public class ProblemReport {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long problemId;
    private Long reporterId;
    private String reportType; // BUG / WRONG_ANSWER / UNCLEAR / OTHER
    private String content;
    private String status; // PENDING / RESOLVED / DISMISSED
    private String reply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String problemTitle;
    @TableField(exist = false)
    private String reporterName;
}
