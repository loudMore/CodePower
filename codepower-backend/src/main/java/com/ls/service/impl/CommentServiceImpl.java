/**
 * 文件说明：评论 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.Comment;
import com.ls.domain.CommentLike;
import com.ls.domain.User;
import com.ls.domain.UserProfile;
import com.ls.mapper.CommentLikeMapper;
import com.ls.mapper.CommentMapper;
import com.ls.mapper.UserMapper;
import com.ls.mapper.UserProfileMapper;
import com.ls.service.CommentService;
import com.ls.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 评论服务实现 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final NotificationService notificationService;
    private final Cache<String, IPage<Comment>> commentPageCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(20000)
            .build();

    @Override
    public IPage<Comment> getComments(String targetType, Long targetId, int page, int size,
                                      Long currentUserId, String sort, String keyword, Boolean mineOnly) {
        Page<Comment> pageParam = new Page<>(page, size);
        if (Boolean.TRUE.equals(mineOnly) && currentUserId == null) {
            pageParam.setRecords(List.of());
            pageParam.setTotal(0);
            return pageParam;
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedSort = sort == null ? "latest" : sort.trim().toLowerCase();
        boolean cacheable = !Boolean.TRUE.equals(mineOnly) && normalizedKeyword.isBlank();
        String cacheKey = buildCommentPageCacheKey(targetType, targetId, page, size, normalizedSort);
        if (cacheable) {
            IPage<Comment> cached = commentPageCache.getIfPresent(cacheKey);
            if (cached != null) {
                return copyCommentPageWithLikedStatus(cached, currentUserId);
            }
        }

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .isNull(Comment::getParentId)
                .eq(Comment::getStatus, 1)
                .eq(Boolean.TRUE.equals(mineOnly) && currentUserId != null, Comment::getUserId, currentUserId)
                .like(!normalizedKeyword.isBlank(), Comment::getContent, normalizedKeyword);

        switch (normalizedSort) {
            case "hot" -> wrapper.orderByDesc(Comment::getLikesCount).orderByDesc(Comment::getCreatedAt);
            case "oldest" -> wrapper.orderByAsc(Comment::getCreatedAt);
            default -> wrapper.orderByDesc(Comment::getCreatedAt).orderByDesc(Comment::getLikesCount);
        }

        IPage<Comment> result = this.page(pageParam, wrapper);
        List<Long> commentIds = result.getRecords().stream().map(Comment::getId).collect(Collectors.toList());

        if (!commentIds.isEmpty()) {
            // 回复按发布时间顺序展示，保留对话上下文。
            List<Comment> replies = this.list(new LambdaQueryWrapper<Comment>()
                    .in(Comment::getParentId, commentIds)
                    .eq(Comment::getStatus, 1)
                    .orderByAsc(Comment::getCreatedAt));
            Map<Long, List<Comment>> replyMap = replies.stream()
                    .collect(Collectors.groupingBy(Comment::getParentId));
            fillUserInfo(replies);
            result.getRecords().forEach(c -> c.setChildren(replyMap.getOrDefault(c.getId(), new ArrayList<>())));
        }
        fillUserInfo(result.getRecords());
        if (cacheable) {
            commentPageCache.put(cacheKey, copyCommentPage(result));
            return copyCommentPageWithLikedStatus(result, currentUserId);
        }
        fillLikedStatusDeep(result.getRecords(), currentUserId);
        return result;
    }

    @Override
    @Transactional
    public Comment addComment(Long userId, String targetType, Long targetId, Long parentId, String content) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setTargetType(targetType);
        comment.setTargetId(targetId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setLikesCount(0);
        comment.setStatus(1);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        this.save(comment);
        evictCommentPageCache(targetType, targetId);
        fillUserInfo(List.of(comment));

        if (parentId != null) {
            Comment parent = this.getById(parentId);
            if (parent != null && !parent.getUserId().equals(userId)) {
                User replier = userMapper.selectById(userId);
                String replierName = replier != null ? replier.getUsername() : "有人";
                notificationService.createNotification(parent.getUserId(), "COMMENT_REPLY",
                        replierName + " 回复了你的评论",
                        "\"" + (content.length() > 50 ? content.substring(0, 50) + "..." : content) + "\"",
                        comment.getId());
            }
        }

        return comment;
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = this.getById(commentId);
        if (comment == null) throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        if (!comment.getUserId().equals(userId)) {
            User user = userMapper.selectById(userId);
            if (user == null || !"ADMIN".equals(user.getRole())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此评论");
            }
        }
        this.removeById(commentId);
        evictCommentPageCache(comment.getTargetType(), comment.getTargetId());
    }

    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long commentId) {
        CommentLike existing = commentLikeMapper.selectOne(
                new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getUserId, userId)
                        .eq(CommentLike::getCommentId, commentId));
        Comment comment = this.getById(commentId);
        if (comment == null) throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");

        if (existing != null) {
            commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getUserId, userId)
                    .eq(CommentLike::getCommentId, commentId));
            int currentLikes = comment.getLikesCount() == null ? 0 : comment.getLikesCount();
            comment.setLikesCount(Math.max(0, currentLikes - 1));
            this.updateById(comment);
            evictCommentPageCache(comment.getTargetType(), comment.getTargetId());
            return false;
        } else {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            like.setCreatedAt(LocalDateTime.now());
            commentLikeMapper.insert(like);
            int currentLikes = comment.getLikesCount() == null ? 0 : comment.getLikesCount();
            comment.setLikesCount(currentLikes + 1);
            this.updateById(comment);
            evictCommentPageCache(comment.getTargetType(), comment.getTargetId());
            return true;
        }
    }

    private IPage<Comment> copyCommentPageWithLikedStatus(IPage<Comment> source, Long currentUserId) {
        IPage<Comment> copy = copyCommentPage(source);
        fillLikedStatusDeep(copy.getRecords(), currentUserId);
        return copy;
    }

    private IPage<Comment> copyCommentPage(IPage<Comment> source) {
        Page<Comment> copy = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        copy.setRecords(copyComments(source.getRecords()));
        return copy;
    }

    private List<Comment> copyComments(List<Comment> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        return source.stream().map(this::copyComment).collect(Collectors.toList());
    }

    private Comment copyComment(Comment source) {
        Comment copy = new Comment();
        copy.setId(source.getId());
        copy.setTargetType(source.getTargetType());
        copy.setTargetId(source.getTargetId());
        copy.setUserId(source.getUserId());
        copy.setParentId(source.getParentId());
        copy.setContent(source.getContent());
        copy.setLikesCount(source.getLikesCount());
        copy.setStatus(source.getStatus());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setDeleted(source.getDeleted());
        copy.setUsername(source.getUsername());
        copy.setAvatarUrl(source.getAvatarUrl());
        copy.setLiked(source.getLiked());
        copy.setChildren(copyComments(source.getChildren()));
        return copy;
    }

    private void fillLikedStatusDeep(List<Comment> comments, Long currentUserId) {
        List<Comment> all = new ArrayList<>();
        collectComments(comments, all);
        fillLikedStatus(all, currentUserId);
    }

    private void collectComments(List<Comment> comments, List<Comment> collector) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        for (Comment comment : comments) {
            collector.add(comment);
            collectComments(comment.getChildren(), collector);
        }
    }

    private void evictCommentPageCache(String targetType, Long targetId) {
        String prefix = "comment:page:" + targetType + ":" + targetId + ":";
        commentPageCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    private String buildCommentPageCacheKey(String targetType, Long targetId, int page, int size, String sort) {
        return "comment:page:" + targetType + ":" + targetId + ":" + page + ":" + size + ":" + sort;
    }

    private void fillUserInfo(List<Comment> comments) {
        if (comments.isEmpty()) return;
        List<Long> userIds = comments.stream().map(Comment::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, UserProfile> profileMap = userProfileMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        comments.forEach(c -> {
            User u = userMap.get(c.getUserId());
            if (u != null) c.setUsername(u.getUsername());
            UserProfile p = profileMap.get(c.getUserId());
            if (p != null) c.setAvatarUrl(p.getAvatarUrl());
        });
    }

    private void fillLikedStatus(List<Comment> comments, Long currentUserId) {
        if (comments.isEmpty()) return;
        if (currentUserId == null) {
            comments.forEach(comment -> comment.setLiked(false));
            return;
        }

        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        Set<Long> likedIds = new HashSet<>(commentLikeMapper.selectList(
                        new LambdaQueryWrapper<CommentLike>()
                                .eq(CommentLike::getUserId, currentUserId)
                                .in(CommentLike::getCommentId, commentIds))
                .stream()
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet()));
        comments.forEach(comment -> comment.setLiked(likedIds.contains(comment.getId())));
    }
}
