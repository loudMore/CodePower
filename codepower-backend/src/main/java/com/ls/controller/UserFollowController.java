/**
 * 文件说明：用户关注 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 关注控制器 — 关注/取关、粉丝列表、关注列表 */
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService followService;
    private final UserMapper userMapper;

    /** 关注用户 */
    @PostMapping("/{userId}")
    public Result<Void> follow(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long userId) {
        Long myId = getUserId(userDetails);
        followService.follow(myId, userId);
        return Result.success();
    }

    /** 取消关注用户 */
    @DeleteMapping("/{userId}")
    public Result<Void> unfollow(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable Long userId) {
        Long myId = getUserId(userDetails);
        followService.unfollow(myId, userId);
        return Result.success();
    }

    /** 查询是否关注指定用户及互关状态 */
    @GetMapping("/check/{userId}")
    public Result<Map<String, Object>> checkFollow(@AuthenticationPrincipal UserDetails userDetails,
                                                   @PathVariable Long userId) {
        Long myId = getUserId(userDetails);
        boolean following = followService.isFollowing(myId, userId);
        boolean mutual = following && followService.isFollowing(userId, myId);
        return Result.success(Map.of("isFollowing", following, "isMutual", mutual));
    }

    /** 获取我的关注列表 */
    @GetMapping("/following")
    public Result<List<Map<String, Object>>> getFollowing(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        Long myId = getUserId(userDetails);
        return Result.success(followService.getFollowingList(myId, page, size));
    }

    /** 获取我的粉丝列表 */
    @GetMapping("/followers")
    public Result<List<Map<String, Object>>> getFollowers(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        Long myId = getUserId(userDetails);
        return Result.success(followService.getFollowerList(myId, page, size));
    }

    /** 获取指定用户的关注数与粉丝数 */
    @GetMapping("/counts/{userId}")
    public Result<Map<String, Long>> getCounts(@PathVariable Long userId) {
        return Result.success(followService.getFollowCounts(userId));
    }

    /** 获取指定用户的关注列表（公开） */
    @GetMapping("/{userId}/following")
    public Result<List<Map<String, Object>>> getUserFollowing(@PathVariable Long userId,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getFollowingList(userId, page, size));
    }

    /** 获取指定用户的粉丝列表（公开） */
    @GetMapping("/{userId}/followers")
    public Result<List<Map<String, Object>>> getUserFollowers(@PathVariable Long userId,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getFollowerList(userId, page, size));
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return user.getId();
    }
}
