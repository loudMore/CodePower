/**
 * 文件说明：用户学习路线进度 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserLearningProgress;
import org.apache.ibatis.annotations.Mapper;

/** 用户学习进度 Mapper */
@Mapper
public interface UserLearningProgressMapper extends BaseMapper<UserLearningProgress> {
}
