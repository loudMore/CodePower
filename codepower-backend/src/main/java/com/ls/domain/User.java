/**
 * 文件说明：用户 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户认证核心实体
 * @author ls
 * @since 2025-07-01
 */
@Data
@TableName("users")
public class User {
    /**
     * 用户ID，自动增长
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String password;
    private String role;
    /**
     * 用户状态 (0:已注销, 1:正常, 2:未验证)
     * 使用逻辑删除，已注销的用户不会被物理删除
     */
    private Integer status;
    private Integer level;
    private Integer exp;
    private Integer dailyAiQuota;
    private Integer dailyAiUsed;
    private Integer bonusAiPoints;
    private java.time.LocalDate quotaResetDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标志 (0:未删除, 1:已删除)
     */
    @TableLogic
    private Integer deleted;
} 