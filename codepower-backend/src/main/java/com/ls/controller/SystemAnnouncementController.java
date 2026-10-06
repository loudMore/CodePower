/**
 * 文件说明：系统公告 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.SystemAnnouncement;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.SystemAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 系统公告控制器 — 公告列表、未读数、发布公告 */
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class SystemAnnouncementController {

    private final SystemAnnouncementService announcementService;
    private final UserMapper userMapper;

    /** 分页获取有效的系统公告列表 */
    @GetMapping
    public Result<IPage<SystemAnnouncement>> getAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(announcementService.getActiveAnnouncements(page, size));
    }

    /** 获取未读公告数量 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(Map.of("count", announcementService.getUnreadCount(userId)));
    }

    /** 将指定公告标记为已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        announcementService.markAsRead(userId, id);
        return Result.success();
    }

    /** 发布新的系统公告 */
    @PostMapping
    public Result<Void> createAnnouncement(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, Object> body) {
        Long userId = getUserId(userDetails);
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String type = (String) body.getOrDefault("type", "INFO");
        Integer priority = body.get("priority") != null ? Integer.valueOf(body.get("priority").toString()) : 0;
        announcementService.createAnnouncement(title, content, type, userId, priority);
        return Result.success();
    }

    /** 停用指定公告 */
    @PutMapping("/{id}/deactivate")
    public Result<Void> deactivate(@PathVariable Long id) {
        announcementService.deactivateAnnouncement(id);
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
