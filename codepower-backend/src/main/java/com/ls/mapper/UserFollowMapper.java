/**
 * 文件说明：用户关注 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserFollow;
import org.apache.ibatis.annotations.Mapper;

/** 用户关注关系 Mapper */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
}
