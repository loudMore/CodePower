/**
 * 文件说明：题目 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 编程问题实体
 * @author ls
 * @since 2024-07-01
 */
@Data
@TableName("problems")
public class Problem {
    /**
     * 问题ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 题目标题
     */
    private String title;
    
    /**
     * 题目描述(富文本)
     */
    private String description;
    
    /**
     * 难度级别
     */
    private String difficulty;
    
    /**
     * 输入格式说明(富文本)
     */
    private String inputFormat;
    
    /**
     * 输出格式说明(富文本)
     */
    private String outputFormat;
    
    /**
     * 示例输入
     */
    private String inputExample;
    
    /**
     * 示例输出
     */
    private String outputExample;

    /**
     * 公开样例列表(JSON)，用于支持多组公开样例；inputExample/outputExample 保留为兼容字段
     */
    private String examples;
    
    /**
     * 兼容旧字段：第二个公开样例输入
     */
    private String debugInputExample;

    /**
     * 兼容旧字段：第二个公开样例输出
     */
    private String debugOutputExample;
    
    /**
     * 提示信息
     */
    private String hint;
    
    /**
     * 题解(富文本)
     */
    private String solution;
    
    /**
     * 时间限制(ms)
     */
    private Integer timeLimit;
    
    /**
     * 内存限制(KB)
     */
    private Integer memoryLimit;
    
    /**
     * 可见性(PUBLIC:公开,PRIVATE:私密)
     */
    private String visibility;
    
    /**
     * 作者ID
     */
    private Long authorId;
    
    /**
     * 状态 (0:禁用, 1:启用)
     */
    private Integer status;

    // 题目统计（由定时任务批量更新）
    private Integer submitCount;
    private Integer acceptCount;
    private java.math.BigDecimal acceptRate;

    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除标志 (0:未删除, 1:已删除)
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 关联的标签列表
     * 非数据库字段，需要手动查询关联
     */
    @TableField(exist = false)
    private List<Tag> tags;
    
    /**
     * 作者用户名
     * 非数据库字段，用于前端展示
     */
    @TableField(exist = false)
    private String authorName;
} 
