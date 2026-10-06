/**
 * 文件说明：等级积分 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 等级控制器 — 查询用户等级与经验信息 */
@RestController
@RequestMapping("/api/level")
@RequiredArgsConstructor
public class UserLevelController {

    private final UserLevelService userLevelService;
    private final UserMapper userMapper;

    /** 获取当前用户等级与经验值详情 */
    @GetMapping("/info")
    public Result<Map<String, Object>> getLevelInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(userLevelService.getUserLevelInfo(userId));
    }

    /** 获取全部等级配置表（等级-经验阈值映射） */
    @GetMapping("/config")
    public Result<List<Map<String, Object>>> getLevelConfig() {
        return Result.success(userLevelService.getLevelConfig());
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
