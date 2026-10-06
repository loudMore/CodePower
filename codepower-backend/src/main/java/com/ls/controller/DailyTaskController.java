/**
 * 文件说明：每日任务 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 每日任务控制器 — 任务列表、领取奖励 */
@RestController
@RequestMapping("/api/daily-tasks")
@RequiredArgsConstructor
public class DailyTaskController {

    private final DailyTaskService dailyTaskService;
    private final UserMapper userMapper;

    /** 获取当日任务列表及完成进度 */
    @GetMapping
    public Result<List<Map<String, Object>>> getDailyTasks(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(dailyTaskService.getDailyTasks(userId));
    }

    /** 领取已完成任务的奖励 */
    @PostMapping("/{taskId}/claim")
    public Result<Map<String, Object>> claimReward(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        Long userId = getUserId(userDetails);
        return Result.success(dailyTaskService.claimTaskReward(userId, taskId));
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
