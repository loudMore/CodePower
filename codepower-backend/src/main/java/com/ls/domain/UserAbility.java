/**
 * 文件说明：用户标签能力 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_abilities")
/** 用户能力值实体（雷达图数据源） */
public class UserAbility {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long tagId;
    private BigDecimal abilityScore;
    private Integer solvedCount;
    private Integer attemptCount;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String tagName;
}
