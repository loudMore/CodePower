/**
 * 文件说明：通知 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ls.domain.Notification;
import com.ls.mapper.NotificationMapper;
import com.ls.service.NotificationService;
import com.ls.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/** 系统通知服务实现 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private static final Duration NOTIFICATION_LIST_CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration NOTIFICATION_UNREAD_CACHE_TTL = Duration.ofSeconds(30);

    private final RedisCacheService redisCacheService;

    @Override
    public IPage<Notification> getNotifications(Long userId, int page, int size) {
        String cacheKey = buildNotificationListCacheKey(userId, page, size);
        Page<Notification> cached = redisCacheService.get(cacheKey, Page.class);
        if (cached != null) {
            return cached;
        }

        Page<Notification> result = (Page<Notification>) this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt));
        redisCacheService.set(cacheKey, result, NOTIFICATION_LIST_CACHE_TTL);
        return result;
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = this.getById(notificationId);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(1);
            this.updateById(n);
            evictUserNotificationCache(userId);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        this.update(new Notification() {{ setIsRead(1); }},
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        evictUserNotificationCache(userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        String cacheKey = buildNotificationUnreadCacheKey(userId);
        Long cached = redisCacheService.get(cacheKey, Long.class);
        if (cached != null) {
            return cached;
        }

        long count = this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        redisCacheService.set(cacheKey, count, NOTIFICATION_UNREAD_CACHE_TTL);
        return count;
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String type, String title, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        this.save(notification);
        evictUserNotificationCache(userId);
    }

    @Override
    public void evictUserNotificationCache(Long userId) {
        redisCacheService.delete(buildNotificationUnreadCacheKey(userId));
        redisCacheService.deleteByPattern("notification:list:" + userId + ":*");
    }

    private String buildNotificationListCacheKey(Long userId, int page, int size) {
        return "notification:list:" + userId + ":" + page + ":" + size;
    }

    private String buildNotificationUnreadCacheKey(Long userId) {
        return "notification:unread:" + userId;
    }
}
