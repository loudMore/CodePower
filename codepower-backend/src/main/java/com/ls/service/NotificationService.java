/**
 * 文件说明：通知 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.domain.Notification;

/** 系统通知服务 */
public interface NotificationService {
    /** 分页查询用户通知列表 */
    IPage<Notification> getNotifications(Long userId, int page, int size);
    /** 标记单条通知为已读 */
    void markAsRead(Long userId, Long notificationId);
    /** 标记用户所有通知为已读 */
    void markAllAsRead(Long userId);
    /** 获取用户未读通知数量 */
    long getUnreadCount(Long userId);
    /** 创建一条新通知 */
    void createNotification(Long userId, String type, String title, String content, Long relatedId);
    /** 清除用户通知缓存 */
    void evictUserNotificationCache(Long userId);
}
