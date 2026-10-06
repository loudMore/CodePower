/**
 * 文件说明：第三方账号绑定 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserOauthBinding;
import org.apache.ibatis.annotations.Mapper;

/** 第三方授权账号绑定 Mapper */
@Mapper
public interface UserOauthBindingMapper extends BaseMapper<UserOauthBinding> {
}
