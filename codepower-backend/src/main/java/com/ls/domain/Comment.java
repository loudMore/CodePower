/**
 * 文件说明：评论 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comments")
/** 评论实体 */
public class Comment {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String targetType;
    private Long targetId;
    private Long userId;
    private Long parentId;
    private String content;
    private Integer likesCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatarUrl;
    @TableField(exist = false)
    private java.util.List<Comment> children;
    @TableField(exist = false)
    private Boolean liked;
}
