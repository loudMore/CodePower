/**
 * 文件说明：用户 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ls.domain.Submission;
import com.ls.domain.User;
import com.ls.domain.UserProfile;
import com.ls.domain.dto.UserProfileDto;
import com.ls.mapper.UserMapper;
import com.ls.mapper.UserProfileMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.service.UserService;
import com.ls.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 用户服务实现 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserProfileMapper userProfileMapper;
    
    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private RegionService regionService;

    @Override
    public ResponseEntity<?> getUserByUsername(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "用户不存在"));
        }
        
        // 不返回密码
        user.setPassword(null);
        
        // 获取用户资料和统计数据
        UserProfile profile = userProfileMapper.findByUserId(user.getId());
        Long submissionCount = submissionMapper.selectCount(
                new QueryWrapper<Submission>().eq("user_id", user.getId()).isNull("contest_id"));
        Integer solvedCount = submissionMapper.countAcceptedProblemsByUserId(user.getId());
        
        // 构建完整响应
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("profile", profile);
        response.put("stats", Map.of(
            "submissionCount", submissionCount,
            "solvedCount", solvedCount
        ));
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "用户不存在"));
        }
        
        // 不返回密码
        user.setPassword(null);
        // 公开主页不暴露登录邮箱，邮箱只在 /api/users/me 中返回给本人。
        user.setEmail(null);
        user.setDailyAiQuota(null);
        user.setDailyAiUsed(null);
        user.setBonusAiPoints(null);
        user.setQuotaResetDate(null);
        
        // 获取用户资料和统计数据
        UserProfile profile = userProfileMapper.findByUserId(id);
        Long submissionCount = submissionMapper.selectCount(
                new QueryWrapper<Submission>().eq("user_id", id).isNull("contest_id"));
        Integer solvedCount = submissionMapper.countAcceptedProblemsByUserId(id);
        
        // 构建完整响应
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("profile", profile);
        response.put("stats", Map.of(
            "submissionCount", submissionCount,
            "solvedCount", solvedCount
        ));
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userMapper.selectList(null);
        // 移除密码
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateUser(Long id, UserProfileDto profileDto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "用户不存在"));
        }
        
        // 更新用户基本信息
        if (profileDto.getUsername() != null && !profileDto.getUsername().isEmpty()) {
            // 检查用户名是否已存在
            User existingUser = userMapper.findByUsername(profileDto.getUsername());
            if (existingUser != null && !existingUser.getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
            }
            user.setUsername(profileDto.getUsername());
            userMapper.updateById(user);
        }
        
        // 更新用户资料信息
        UserProfile profile = userProfileMapper.findByUserId(id);
        boolean createProfile = false;
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(id);
            createProfile = true;
        }
        if (profileDto.getRegion() != null) {
            profile.setRegion(regionService.normalizeRegion(profileDto.getRegion()));
        }
        if (profileDto.getBio() != null) {
            profile.setBio(profileDto.getBio());
        }
        if (profileDto.getAvatarUrl() != null) {
            profile.setAvatarUrl(profileDto.getAvatarUrl());
        }
        if (createProfile) {
            if (profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) {
                profile.setAvatarUrl("/avatars/avatar-1.svg");
            }
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }
        
        return ResponseEntity.ok(Map.of("message", "用户信息已更新"));
    }

    @Override
    public ResponseEntity<?> deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "用户不存在"));
        }

        // 设置状态为已注销
        user.setStatus(0);
        userMapper.updateById(user);
        return ResponseEntity.ok(Map.of("message", "账户已注销"));
    }

    @Override
    public boolean isCurrentUserOrAdmin(Long userId, UserDetails userDetails) {
        User currentUser = userMapper.findByUsername(userDetails.getUsername());
        if (currentUser == null) {
            return false;
        }

        // 是本人或管理员
        return currentUser.getId().equals(userId) || "ADMIN".equals(currentUser.getRole());
    }
} 
