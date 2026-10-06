/**
 * 文件说明：AI 消息 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_messages")
/** AI 消息实体 */
public class AiMessage {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private Integer tokensUsed;
    private LocalDateTime createdAt;
}
