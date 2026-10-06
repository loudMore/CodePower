/**
 * 文件说明：用户签到 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_check_ins")
/** 用户签到记录实体 */
public class UserCheckIn {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate checkInDate;
    private Integer streak;
    private Integer expEarned;
    private Integer aiBonus;
    private LocalDateTime createdAt;
}
