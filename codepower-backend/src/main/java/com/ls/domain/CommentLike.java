/**
 * 文件说明：评论点赞 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment_likes")
/** 评论点赞实体 */
public class CommentLike {
    private Long userId;
    private Long commentId;
    private LocalDateTime createdAt;
}
