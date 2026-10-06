/**
 * 文件说明：第三方账号绑定 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 第三方授权账号与 CodePower 用户的唯一绑定关系 */
@Data
@TableName("user_oauth_bindings")
public class UserOauthBinding {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String provider;
    private String providerUserId;
    private String providerUnionId;
    private String providerUsername;
    private String providerNickname;
    private String providerAvatar;
    private String providerEmail;
    private Integer emailVerified;
    private LocalDateTime boundAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
