/**
 * 文件说明：用户 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 用户 Mapper */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM users WHERE LOWER(email) = LOWER(#{email})")
    User findByEmail(String email);
    
    @Update("UPDATE users SET password = #{password} WHERE id = #{id}")
    int updatePassword(Long id, String password);
} 