/**
 * 文件说明：系统公告 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ls.domain.SystemAnnouncement;

/** 系统公告服务 */
public interface SystemAnnouncementService extends IService<SystemAnnouncement> {

    /** 创建系统公告 */
    void createAnnouncement(String title, String content, String type, Long creatorId, Integer priority);

    /** 分页获取有效公告列表 */
    IPage<SystemAnnouncement> getActiveAnnouncements(int page, int size);

    /** 下架指定公告 */
    void deactivateAnnouncement(Long announcementId);

    /** 标记用户已读某条公告 */
    void markAsRead(Long userId, Long announcementId);

    /** 获取用户未读公告数量 */
    long getUnreadCount(Long userId);

    /** 清理公告相关缓存 */
    void evictAnnouncementCaches();
}
