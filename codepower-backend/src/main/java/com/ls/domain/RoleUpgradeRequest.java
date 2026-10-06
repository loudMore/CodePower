/**
 * 文件说明：角色升级申请 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("role_upgrade_requests")
/** 角色升级申请实体 */
public class RoleUpgradeRequest {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String requestedRole;
    private String reason;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String email;
    @TableField(exist = false)
    private String currentRole;
}
