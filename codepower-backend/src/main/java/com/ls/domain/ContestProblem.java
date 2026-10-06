/**
 * 文件说明：竞赛题目关系 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("contest_problems")
/** 竞赛-题目关联实体 */
public class ContestProblem {
    private Long contestId;
    private Long problemId;
    private Integer sortOrder;
    private Integer score;

    @TableField(exist = false)
    private String problemTitle;
    @TableField(exist = false)
    private String difficulty;
}
