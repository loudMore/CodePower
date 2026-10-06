/**
 * 文件说明：用户 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.ls.domain.User;
import com.ls.domain.dto.UserProfileDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    ResponseEntity<?> getUserByUsername(String username);
    
    /**
     * 根据ID获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    ResponseEntity<?> getUserById(Long id);
    
    /**
     * 获取所有用户列表
     * @return 用户列表
     */
    ResponseEntity<List<User>> getAllUsers();
    
    /**
     * 更新用户信息
     * @param id 用户ID
     * @param profileDto 用户资料DTO
     * @return 更新结果
     */
    ResponseEntity<?> updateUser(Long id, UserProfileDto profileDto);
    
    /**
     * 删除用户（注销账户）
     * @param id 用户ID
     * @return 删除结果
     */
    ResponseEntity<?> deleteUser(Long id);
    
    /**
     * 检查是否为当前用户或管理员
     * @param userId 目标用户ID
     * @param userDetails 当前登录用户
     * @return 如果是当前用户或管理员则返回true
     */
    boolean isCurrentUserOrAdmin(Long userId, UserDetails userDetails);
} 