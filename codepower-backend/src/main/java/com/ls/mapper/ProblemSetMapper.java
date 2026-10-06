/**
 * 文件说明：题目集 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.ProblemSet;
import org.apache.ibatis.annotations.Mapper;

/** 题目集 Mapper */
@Mapper
public interface ProblemSetMapper extends BaseMapper<ProblemSet> {
}
