/**
 * 文件说明：题目标签关系 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 问题-标签关联实体类
 * @author ls
 * @since 2024-07-01
 */
@Data
@TableName("problem_tags")
public class ProblemTag {
    /**
     * 问题ID (联合主键的一部分)
     */
    private Long problemId;
    
    /**
     * 标签ID (联合主键的一部分)
     */
    private Long tagId;
    
    public ProblemTag() {
    }
    
    public ProblemTag(Long problemId, Long tagId) {
        this.problemId = problemId;
        this.tagId = tagId;
    }
} 