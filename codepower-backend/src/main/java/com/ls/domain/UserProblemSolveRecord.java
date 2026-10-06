/**
 * 文件说明：用户题目首通记录 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_problem_solve_records")
/** 用户做题记录实体 */
public class UserProblemSolveRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long problemId;
    private Long firstAcceptedSubmissionId;
    private LocalDateTime firstAcceptedAt;
    private Long latestAcceptedSubmissionId;
    private LocalDateTime latestAcceptedAt;
    private Integer acceptedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
