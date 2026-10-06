/**
 * 文件说明：题目反馈 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.ProblemReport;
import org.apache.ibatis.annotations.Mapper;

/** 题目反馈 Mapper */
@Mapper
public interface ProblemReportMapper extends BaseMapper<ProblemReport> {
}
