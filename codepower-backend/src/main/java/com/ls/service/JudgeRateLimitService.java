/**
 * 文件说明：评测限频 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 评测提交限频服务。
 * 普通用户按配置限制提交频率，管理员压测和运维场景不受限制。
 */
@Service
@RequiredArgsConstructor
public class JudgeRateLimitService {

    private static final String KEY_PREFIX = "rate:judge-submit:";
    private static final long FALLBACK_WINDOW_MS = 1000L;

    private final RedisCacheService redisCacheService;
    private final Map<Long, Long> fallbackLastSubmitAt = new ConcurrentHashMap<>();

    @Value("${judge0.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${judge0.rate-limit.window-ms:1000}")
    private long windowMs;

    @Value("${judge0.rate-limit.max-requests:1}")
    private int maxRequests;

    /** 检查当前用户是否超过提交频率，优先使用 Redis，Redis 不可用时使用本机内存兜底。 */
    public void check(User user) {
        if (!enabled || user == null || "ADMIN".equals(user.getRole())) {
            return;
        }
        Long userId = user.getId();
        if (userId == null) {
            return;
        }

        long effectiveWindowMs = Math.max(200L, windowMs);
        int effectiveMax = Math.max(1, maxRequests);
        String key = KEY_PREFIX + userId + ":" + (System.currentTimeMillis() / effectiveWindowMs);
        Long count = redisCacheService.incrementAndExpire(key, 1L,
                Math.max(1L, effectiveWindowMs + 500L), TimeUnit.MILLISECONDS);

        if (count != null && count > 0) {
            if (count > effectiveMax) {
                throw tooManyRequests(effectiveWindowMs);
            }
            return;
        }

        cleanupFallback();
        long now = System.currentTimeMillis();
        Long previous = fallbackLastSubmitAt.put(userId, now);
        if (previous != null && now - previous < Math.max(FALLBACK_WINDOW_MS, effectiveWindowMs)) {
            fallbackLastSubmitAt.put(userId, previous);
            throw tooManyRequests(effectiveWindowMs);
        }
    }

    /** 构造统一的限频异常提示。 */
    private BusinessException tooManyRequests(long effectiveWindowMs) {
        long seconds = Math.max(1L, (long) Math.ceil(effectiveWindowMs / 1000.0));
        return new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                "提交太频繁了，请至少间隔 " + seconds + " 秒再试");
    }

    /** 清理本机兜底限频记录，避免长时间运行后内存无限增长。 */
    private void cleanupFallback() {
        long cutoff = System.currentTimeMillis() - 60_000L;
        Iterator<Map.Entry<Long, Long>> iterator = fallbackLastSubmitAt.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < cutoff) {
                iterator.remove();
            }
        }
    }
}
