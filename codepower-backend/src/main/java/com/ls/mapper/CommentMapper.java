/**
 * 文件说明：评论 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.Comment;
import org.apache.ibatis.annotations.Mapper;

/** 评论 Mapper */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
