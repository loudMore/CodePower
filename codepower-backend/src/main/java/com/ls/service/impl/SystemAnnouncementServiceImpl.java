/**
 * 文件说明：系统公告 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ls.domain.SystemAnnouncement;
import com.ls.mapper.SystemAnnouncementMapper;
import com.ls.service.RedisCacheService;
import com.ls.service.SystemAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/** 系统公告服务实现 */
@Service
@RequiredArgsConstructor
public class SystemAnnouncementServiceImpl extends ServiceImpl<SystemAnnouncementMapper, SystemAnnouncement> implements SystemAnnouncementService {

    private static final Duration ANNOUNCEMENT_LIST_CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration ANNOUNCEMENT_UNREAD_CACHE_TTL = Duration.ofSeconds(30);

    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public void createAnnouncement(String title, String content, String type, Long creatorId, Integer priority) {
        SystemAnnouncement announcement = new SystemAnnouncement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType(type);
        announcement.setCreatorId(creatorId);
        announcement.setPriority(priority);
        announcement.setStatus(1);
        announcement.setStartTime(LocalDateTime.now());
        save(announcement);
        evictAnnouncementCaches();

        // 不再给所有用户发通知，只在首页展示
    }

    @Override
    @SuppressWarnings("unchecked")
    public IPage<SystemAnnouncement> getActiveAnnouncements(int page, int size) {
        String cacheKey = buildAnnouncementListCacheKey(page, size);
        Page<SystemAnnouncement> cached = redisCacheService.get(cacheKey, Page.class);
        if (cached != null) {
            return cached;
        }

        Page<SystemAnnouncement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SystemAnnouncement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemAnnouncement::getStatus, 1)
                .le(SystemAnnouncement::getStartTime, LocalDateTime.now())
                .and(w -> w.isNull(SystemAnnouncement::getEndTime)
                        .or()
                        .ge(SystemAnnouncement::getEndTime, LocalDateTime.now()))
                .orderByDesc(SystemAnnouncement::getPriority)
                .orderByDesc(SystemAnnouncement::getCreatedAt);
        Page<SystemAnnouncement> result = (Page<SystemAnnouncement>) page(pageParam, wrapper);
        redisCacheService.set(cacheKey, result, ANNOUNCEMENT_LIST_CACHE_TTL);
        return result;
    }

    @Override
    public void deactivateAnnouncement(Long announcementId) {
        SystemAnnouncement announcement = getById(announcementId);
        if (announcement != null) {
            announcement.setStatus(0);
            updateById(announcement);
            evictAnnouncementCaches();
        }
    }

    @Override
    public void markAsRead(Long userId, Long announcementId) {
        baseMapper.insertRead(userId, announcementId);
        redisCacheService.delete(buildAnnouncementUnreadCacheKey(userId));
    }

    @Override
    public long getUnreadCount(Long userId) {
        String cacheKey = buildAnnouncementUnreadCacheKey(userId);
        Long cached = redisCacheService.get(cacheKey, Long.class);
        if (cached != null) {
            return cached;
        }
        long count = baseMapper.countUnread(userId);
        redisCacheService.set(cacheKey, count, ANNOUNCEMENT_UNREAD_CACHE_TTL);
        return count;
    }

    @Override
    public void evictAnnouncementCaches() {
        redisCacheService.deleteByPattern("announcement:list:*");
        redisCacheService.deleteByPattern("announcement:unread:*");
    }

    private String buildAnnouncementListCacheKey(int page, int size) {
        return "announcement:list:" + page + ":" + size;
    }

    private String buildAnnouncementUnreadCacheKey(Long userId) {
        return "announcement:unread:" + userId;
    }
}
