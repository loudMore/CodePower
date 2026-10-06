/**
 * 文件说明：每日任务 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.DailyTask;
import org.apache.ibatis.annotations.Mapper;

/** 每日任务 Mapper */
@Mapper
public interface DailyTaskMapper extends BaseMapper<DailyTask> {
}
