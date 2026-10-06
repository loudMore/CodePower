/**
 * 文件说明：用户关注 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.UserFollow;
import com.ls.domain.User;
import com.ls.domain.UserProfile;
import com.ls.mapper.UserFollowMapper;
import com.ls.mapper.UserMapper;
import com.ls.mapper.UserProfileMapper;
import com.ls.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 用户关注服务实现 */
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowMapper followMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能关注自己");
        }
        if (isFollowing(followerId, followingId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已经关注过了");
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setCreatedAt(LocalDateTime.now());
        followMapper.insert(follow);
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        followMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId));
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId)) > 0;
    }

    @Override
    public boolean isMutualFollow(Long userA, Long userB) {
        return isFollowing(userA, userB) && isFollowing(userB, userA);
    }

    @Override
    public List<Map<String, Object>> getFollowingList(Long userId, int page, int size) {
        Page<UserFollow> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .orderByDesc(UserFollow::getCreatedAt);
        List<UserFollow> records = followMapper.selectPage(pageParam, wrapper).getRecords();
        List<Long> ids = records.stream().map(UserFollow::getFollowingId).collect(Collectors.toList());
        return buildUserInfoList(ids, userId);
    }

    @Override
    public List<Map<String, Object>> getFollowerList(Long userId, int page, int size) {
        Page<UserFollow> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, userId)
                .orderByDesc(UserFollow::getCreatedAt);
        List<UserFollow> records = followMapper.selectPage(pageParam, wrapper).getRecords();
        List<Long> ids = records.stream().map(UserFollow::getFollowerId).collect(Collectors.toList());
        return buildUserInfoList(ids, userId);
    }

    @Override
    public Map<String, Long> getFollowCounts(Long userId) {
        long followingCount = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId));
        long followerCount = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, userId));
        Map<String, Long> map = new HashMap<>();
        map.put("followingCount", followingCount);
        map.put("followerCount", followerCount);
        return map;
    }

    @Override
    public Map<Long, Boolean> batchCheckFollowing(Long currentUserId, List<Long> targetUserIds) {
        if (currentUserId == null || targetUserIds == null || targetUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserFollow> follows = followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, currentUserId)
                .in(UserFollow::getFollowingId, targetUserIds));
        Set<Long> followedSet = follows.stream().map(UserFollow::getFollowingId).collect(Collectors.toSet());
        Map<Long, Boolean> result = new HashMap<>();
        for (Long id : targetUserIds) {
            result.put(id, followedSet.contains(id));
        }
        return result;
    }

    // 构建用户信息列表（含是否互关）
    private List<Map<String, Object>> buildUserInfoList(List<Long> userIds, Long currentUserId) {
        if (userIds.isEmpty()) return Collections.emptyList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, UserProfile> profileMap = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds)
        ).stream().collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        return userIds.stream().map(uid -> {
            User u = userMap.get(uid);
            UserProfile p = profileMap.get(uid);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", uid);
            item.put("username", u != null ? u.getUsername() : "未知");
            item.put("avatar", p != null ? p.getAvatarUrl() : null);
            item.put("level", u != null ? u.getLevel() : 0);
            item.put("isMutual", isFollowing(uid, currentUserId));
            return item;
        }).collect(Collectors.toList());
    }
}
