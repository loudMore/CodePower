/**
 * 文件说明：学习路线 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.LearningPath;
import org.apache.ibatis.annotations.Mapper;

/** 学习路线 Mapper */
@Mapper
public interface LearningPathMapper extends BaseMapper<LearningPath> {
}
