/**
 * 文件说明：竞赛 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.Contest;
import org.apache.ibatis.annotations.Mapper;

/** 竞赛 Mapper */
@Mapper
public interface ContestMapper extends BaseMapper<Contest> {
}
