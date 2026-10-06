/**
 * 文件说明：私信 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.PrivateMessage;
import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import com.ls.service.PrivateMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 私信控制器 — 会话列表、发送消息、未读数 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService messageService;
    private final UserMapper userMapper;

    /** 获取会话列表 */
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(messageService.getConversationList(userId));
    }

    /** 分页获取与指定用户的聊天记录 */
    @GetMapping("/conversation/{otherUserId}")
    public Result<IPage<PrivateMessage>> getConversation(@AuthenticationPrincipal UserDetails userDetails,
                                                          @PathVariable Long otherUserId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "50") int size) {
        Long userId = getUserId(userDetails);
        return Result.success(messageService.getConversation(userId, otherUserId, page, size));
    }

    /** 发送私信 */
    @PostMapping("/send")
    public Result<PrivateMessage> sendMessage(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody Map<String, Object> body) {
        Long userId = getUserId(userDetails);
        Object toUserIdValue = body.get("toUserId");
        if (toUserIdValue == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少接收用户");
        }
        String content = body.get("content") == null ? null : body.get("content").toString();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }
        Long toUserId = Long.valueOf(toUserIdValue.toString());
        PrivateMessage message = messageService.sendMessage(userId, toUserId, content.trim());
        return Result.success(message);
    }

    /** 将与指定用户的私信标记为已读 */
    @PutMapping("/read/{otherUserId}")
    public Result<Void> markAsRead(@AuthenticationPrincipal UserDetails userDetails,
                                    @PathVariable Long otherUserId) {
        Long userId = getUserId(userDetails);
        messageService.markAsRead(userId, otherUserId);
        return Result.success();
    }

    /** 获取私信未读总数 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Result.success(Map.of("count", messageService.getUnreadCount(userId)));
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
