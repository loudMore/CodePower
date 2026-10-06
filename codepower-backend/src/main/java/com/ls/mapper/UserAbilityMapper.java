/**
 * 文件说明：用户标签能力 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserAbility;
import org.apache.ibatis.annotations.Mapper;

/** 用户能力值 Mapper */
@Mapper
public interface UserAbilityMapper extends BaseMapper<UserAbility> {
}
