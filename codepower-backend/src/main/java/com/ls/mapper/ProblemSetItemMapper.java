/**
 * 文件说明：题目集题目关系 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.ProblemSetItem;
import org.apache.ibatis.annotations.Mapper;

/** 题目集条目 Mapper */
@Mapper
public interface ProblemSetItemMapper extends BaseMapper<ProblemSetItem> {
}
