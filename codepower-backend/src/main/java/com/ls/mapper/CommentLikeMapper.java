/**
 * 文件说明：评论点赞 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.CommentLike;
import org.apache.ibatis.annotations.Mapper;

/** 评论点赞 Mapper */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
}
