/**
 * 文件说明：用户资料 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

/** 用户档案 Mapper */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    /**
     * 根据用户ID查询用户资料
     */
    @Select("SELECT * FROM user_profiles WHERE user_id = #{userId}")
    UserProfile findByUserId(@Param("userId") Long userId);
} 