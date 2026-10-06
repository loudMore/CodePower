/**
 * 文件说明：评论 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.Comment;
import com.ls.domain.User;
import com.ls.mapper.CommentMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 评论控制器 — 发布评论、回复、点赞、删除 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;

    /** 获取指定目标的评论列表（分页） */
    @GetMapping("/target/{targetType}/{targetId}")
    public Result<IPage<Comment>> getComments(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") Boolean mineOnly,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = resolveUserId(userDetails);
        return Result.success(commentService.getComments(
                targetType, targetId, page, size, currentUserId, sort, keyword, mineOnly));
    }

    /** 发布评论或回复 */
    @PostMapping
    public Result<Comment> addComment(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody Map<String, Object> body) {
        Long userId = getUserId(userDetails);
        String targetType = (String) body.get("targetType");
        Long targetId = Long.valueOf(body.get("targetId").toString());
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        String content = (String) body.get("content");
        return Result.success(commentService.addComment(userId, targetType, targetId, parentId, content));
    }

    /** 切换评论点赞状态 */
    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> toggleLike(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        boolean liked = commentService.toggleLike(userId, id);
        Comment comment = commentMapper.selectById(id);
        int likesCount = comment == null || comment.getLikesCount() == null ? 0 : comment.getLikesCount();
        return Result.success(Map.of("liked", liked, "likesCount", likesCount));
    }

    /** 删除评论 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        commentService.deleteComment(userId, id);
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

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        return user == null ? null : user.getId();
    }
}
