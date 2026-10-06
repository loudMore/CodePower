/**
 * 文件说明：第三方绑定日志 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 第三方账号绑定操作日志 */
@Data
@TableName("user_oauth_binding_logs")
public class UserOauthBindingLog {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String provider;
    private String providerUserId;
    private String action;
    private String message;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
