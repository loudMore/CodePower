/**
 * 文件说明：评论 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.domain.Comment;

/** 评论服务 */
public interface CommentService {
    /** 分页查询指定目标的评论列表 */
    IPage<Comment> getComments(String targetType, Long targetId, int page, int size,
                               Long currentUserId, String sort, String keyword, Boolean mineOnly);
    /** 添加评论 */
    Comment addComment(Long userId, String targetType, Long targetId, Long parentId, String content);
    /** 删除评论 */
    void deleteComment(Long userId, Long commentId);
    /** 切换评论点赞状态，返回当前是否已点赞 */
    boolean toggleLike(Long userId, Long commentId);
}
