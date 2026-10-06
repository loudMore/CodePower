/**
 * 文件说明：用户签到 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserCheckIn;
import org.apache.ibatis.annotations.Mapper;

/** 用户签到记录 Mapper */
@Mapper
public interface UserCheckInMapper extends BaseMapper<UserCheckIn> {
}
