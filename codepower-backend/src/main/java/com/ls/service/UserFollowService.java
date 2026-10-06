/**
 * 文件说明：用户关注 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import java.util.List;
import java.util.Map;

/** 用户关注服务 */
public interface UserFollowService {
    /** 关注用户 */
    void follow(Long followerId, Long followingId);
    /** 取消关注用户 */
    void unfollow(Long followerId, Long followingId);
    /** 判断是否已关注目标用户 */
    boolean isFollowing(Long followerId, Long followingId);
    /** 判断两位用户是否互相关注 */
    boolean isMutualFollow(Long userA, Long userB);
    /** 获取用户的关注列表 */
    List<Map<String, Object>> getFollowingList(Long userId, int page, int size);
    /** 获取用户的粉丝列表 */
    List<Map<String, Object>> getFollowerList(Long userId, int page, int size);
    /** 获取用户的关注数与粉丝数 */
    Map<String, Long> getFollowCounts(Long userId);
    /** 批量查询当前用户对目标用户列表的关注状态 */
    Map<Long, Boolean> batchCheckFollowing(Long currentUserId, List<Long> targetUserIds);
}
