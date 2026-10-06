/**
 * 文件说明：学习路线阶段 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.LearningPathStage;
import org.apache.ibatis.annotations.Mapper;

/** 学习路线阶段 Mapper */
@Mapper
public interface LearningPathStageMapper extends BaseMapper<LearningPathStage> {
}
