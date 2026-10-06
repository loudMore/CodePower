/**
 * 文件说明：私信 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("private_messages")
/** 私信实体 */
public class PrivateMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromUserId;
    private Long toUserId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
