/**
 * 文件说明：测试点提交结果 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提交测试结果实体
 * @author ls
 * @since 2024-07-01
 */
@Data
@TableName("submission_results")
public class SubmissionResult {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long testCaseId;
    private String status;
    private Integer executionTime;
    private Integer memoryUsed;
    private String actualOutput;
    private String errorMessage;
    private LocalDateTime createdAt;
} 