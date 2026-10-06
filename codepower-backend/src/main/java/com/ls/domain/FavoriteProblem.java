/**
 * 文件说明：题目收藏 实体类，映射数据库表并承载业务数据。
 */
package com.ls.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.sql.Timestamp;

/**
 * 用户收藏问题实体
 * @author ls
 * @since 2025-07-01
 */
@Data
@TableName("favorite_problems")
public class FavoriteProblem {
    @TableField("user_id")
    private Long userId;
    
    @TableField("problem_id")
    private Long problemId;
    
    @TableField("created_at")
    private Timestamp createdAt;
} 