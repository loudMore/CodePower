/**
 * 文件说明：角色升级申请 数据访问接口，封装 MyBatis-Plus 的数据库操作。
 */
package com.ls.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ls.domain.RoleUpgradeRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 角色升级申请 Mapper */
@Mapper
public interface RoleUpgradeRequestMapper extends BaseMapper<RoleUpgradeRequest> {

    @Select("SELECT r.*, u.username, u.email, u.role as current_role " +
            "FROM role_upgrade_requests r " +
            "JOIN users u ON r.user_id = u.id " +
            "WHERE r.status = #{status} " +
            "ORDER BY r.created_at DESC")
    List<RoleUpgradeRequest> selectWithUserInfo(String status);

    @Select("SELECT r.*, u.username, u.email, u.role as current_role " +
            "FROM role_upgrade_requests r " +
            "JOIN users u ON r.user_id = u.id " +
            "ORDER BY r.created_at DESC")
    List<RoleUpgradeRequest> selectAllWithUserInfo();
}
