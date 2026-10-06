/**
 * 文件说明：用户资料 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

import java.io.Serializable;

/**
 * 用户详细信息扩展实体
 * @author ls
 * @since 2024-07-01
 */
@Data
@TableName("user_profiles")
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id")
    private Long userId;

    private String region;

    private String bio;

    private String avatarUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
} 