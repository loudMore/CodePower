/**
 * 文件说明：等级配置 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("level_config")
/** 等级配置实体（经验阈值与每日积分） */
public class LevelConfig {
    @TableId(value = "level", type = IdType.INPUT)
    private Integer level;
    private Integer requiredExp;
    private Integer baseAiQuota;
    private String title;
}
