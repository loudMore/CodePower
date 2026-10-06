/**
 * 文件说明：AI 会话 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_conversations")
/** AI 对话实体 */
public class AiConversation {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private Long problemId;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
