/**
 * 文件说明：AI 模型配置 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model_configs")
public class AiModelConfig {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long providerId;
    private String modelId;
    private String displayName;
    private Integer enabled;
    private Double costMultiplier;
    private Integer contextWindow;
    private String capabilityTags;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String providerKey;

    @TableField(exist = false)
    private String providerName;
}
