/**
 * 文件说明：提交记录 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提交记录实体
 * @author ls
 * @since 2024-07-01
 */
@Data
@TableName("submissions")
public class Submission {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long problemId;
    private Long userId;
    private String code;
    private String language;
    private String status;
    private Integer score;
    private Integer executionTime;
    private Integer memoryUsed;
    private String errorMessage;
    private Object testResults; // JSON数据
    private Long contestId;
    private LocalDateTime createdAt;
    private LocalDateTime queuedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String judgeNode;
    private Integer queueWaitMs;
    private Integer judgeDurationMs;
} 
