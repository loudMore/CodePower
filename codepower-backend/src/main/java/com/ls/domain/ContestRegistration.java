/**
 * 文件说明：竞赛报名 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contest_registrations")
/** 竞赛报名实体 */
public class ContestRegistration {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long userId;
    private LocalDateTime registeredAt;
    private LocalDateTime enteredAt;
    private LocalDateTime lastSubmissionAt;
}
