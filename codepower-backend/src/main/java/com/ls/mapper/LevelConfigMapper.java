/**
 * 文件说明：L ev el 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.LevelConfig;
import org.apache.ibatis.annotations.Mapper;

/** 等级配置 Mapper */
@Mapper
public interface LevelConfigMapper extends BaseMapper<LevelConfig> {
}
