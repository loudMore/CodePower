/**
 * 文件说明：签到 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 签到控制器 — 每日签到、签到状态、签到记录 */
@RestController
@RequestMapping("/api/check-in")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;
    private final UserMapper userMapper;

    /** 执行每日签到 */
    @PostMapping
    public Result<Map<String, Object>> checkIn(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(checkInService.checkIn(userId));
    }

    /** 获取当日签到状态及连续签到天数 */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(checkInService.getCheckInStatus(userId));
    }

    /** 获取指定月份的签到日期列表 */
    @GetMapping("/monthly")
    public Result<List<Integer>> getMonthlyCheckIns(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = getUserId(userDetails);
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();
        return Result.success(checkInService.getMonthlyCheckIns(userId, year, month));
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
