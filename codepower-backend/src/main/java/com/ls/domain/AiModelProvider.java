/**
 * 文件说明：AI 模型提供商 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model_providers")
public class AiModelProvider {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String providerKey;
    private String name;
    private String apiType;
    private String baseUrl;
    private String apiKey;
    private Integer enabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
