/**
 * 文件说明：题解 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题题解实体类
 * @author ls
 * @since 2024-07-04
 */
@Data
@TableName("problem_solutions")
public class ProblemSolution {
    
    /**
     * 题解ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 问题ID
     */
    private Long problemId;
    
    /**
     * 作者ID
     */
    private Long userId;
    
    /**
     * 编程语言
     */
    private String language;
    
    /**
     * 题解标题
     */
    private String title;
    
    /**
     * 题解描述(富文本)
     */
    private String description;
    
    /**
     * 代码内容
     */
    private String code;
    
    /**
     * 是否官方题解(0:用户题解, 1:官方题解)
     */
    private Integer isOfficial;
    
    /**
     * 点赞数
     */
    private Integer likes;

    /**
     * 题解对应的 AC 提交，用于判定奖励和性能基准
     */
    private Long sourceSubmissionId;

    /**
     * 该题解对应提交的运行时间（ms）
     */
    private Integer runtimeMs;

    /**
     * 该题解对应提交的内存占用（KB）
     */
    private Integer memoryKb;

    /**
     * 是否已经发放题解质量奖励
     */
    private Integer rewardGranted;

    /**
     * 奖励原因：FIRST_COMMUNITY / NEW_BEST 等
     */
    private String rewardReason;
    
    /**
     * 状态(0:禁用, 1:启用)
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除标志(0:未删除, 1:已删除)
     */
    @TableLogic
    private Integer deleted;

    /**
     * 作者用户名，仅接口展示使用
     */
    @TableField(exist = false)
    private String username;

    /**
     * 兼容前端旧字段 userName
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 作者头像，仅接口展示使用
     */
    @TableField(exist = false)
    private String avatarUrl;
} 
