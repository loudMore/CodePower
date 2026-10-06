/**
 * 文件说明：评测测试点 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.sql.Timestamp;

/**
 * 测试用例实体类
 * @author ls
 */
@Data
@TableName("judge_test_cases")
public class JudgeTestCase {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 问题ID
     */
    private Long problemId;
    
    /**
     * 输入数据
     */
    private String input;
    
    /**
     * 期望输出
     */
    private String expectedOutput;
    
    /**
     * 分值
     */
    private Integer score;
    
    /**
     * 是否隐藏(1:隐藏不展示给用户)
     */
    private Boolean isHidden;
    
    /**
     * 顺序号
     */
    private Integer orderNum;
    
    /**
     * 创建时间
     */
    private Timestamp createdAt;
} 