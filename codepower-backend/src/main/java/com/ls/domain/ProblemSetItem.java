/**
 * 文件说明：题目集题目关系 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("problem_set_items")
/** 题目集条目实体 */
public class ProblemSetItem {
    private Long setId;
    private Long problemId;
    private Integer sortOrder;
    private LocalDateTime addedAt;

    @TableField(exist = false)
    private String problemTitle;
    @TableField(exist = false)
    private String difficulty;
    @TableField(exist = false)
    private String visibility; // 题目可见性（PUBLIC/PRIVATE），前端据此显示锁定图标
    @TableField(exist = false)
    private Long problemAuthorId;
    @TableField(exist = false)
    private String accessStatus; // AVAILABLE/INACCESSIBLE，用于标记题目集中的失效引用
    @TableField(exist = false)
    private String invalidReason;
}
