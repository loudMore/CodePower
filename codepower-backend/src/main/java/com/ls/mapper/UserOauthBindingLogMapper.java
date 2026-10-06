/**
 * 文件说明：第三方绑定日志 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserOauthBindingLog;
import org.apache.ibatis.annotations.Mapper;

/** 第三方授权账号绑定日志 Mapper */
@Mapper
public interface UserOauthBindingLogMapper extends BaseMapper<UserOauthBindingLog> {
}
