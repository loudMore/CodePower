/**
 * 文件说明：通知 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.Notification;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 通知控制器 — 系统通知列表、未读数、标记已读 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserMapper userMapper;

    /** 分页获取当前用户的通知列表 */
    @GetMapping
    public Result<IPage<Notification>> getNotifications(@AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(userDetails);
        return Result.success(notificationService.getNotifications(userId, page, size));
    }

    /** 获取未读通知数量 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    /** 将指定通知标记为已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        notificationService.markAsRead(userId, id);
        return Result.success();
    }

    /** 将所有通知标记为已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        notificationService.markAllAsRead(userId);
        return Result.success();
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
