/**
 * 文件说明：用户每日任务进度 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserDailyTaskProgress;
import org.apache.ibatis.annotations.Mapper;

/** 用户每日任务进度 Mapper */
@Mapper
public interface UserDailyTaskProgressMapper extends BaseMapper<UserDailyTaskProgress> {
}
