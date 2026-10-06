/**
 * 文件说明：题解代码 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 题解代码实体类
 * @author ls
 */
@Data
@TableName("solution_codes")
public class SolutionCode {
    
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
     * 编程语言
     */
    private String language;
    
    /**
     * 代码内容
     */
    private String code;
    
    /**
     * 是否官方题解
     */
    private Boolean isOfficial;
    
    /**
     * 创建时间
     */
    private Timestamp createdAt;
} 