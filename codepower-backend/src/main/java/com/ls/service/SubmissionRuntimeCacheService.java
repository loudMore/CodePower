/**
 * 文件说明：提交运行态缓存 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ls.domain.Submission;
import com.ls.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 提交运行时缓存服务。
 * 缓存题目做题状态、提交详情和待刷新题目统计；Redis 异常时不影响主业务落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionRuntimeCacheService {

    private static final String USER_PROBLEM_STATUS_PREFIX = "user:problem-status:";
    private static final String DIRTY_PROBLEM_STATS_KEY = "problem:stats:dirty";
    private static final String SUBMISSION_DETAIL_PREFIX = "submission:detail:";
    private static final String STATUS_NONE = "NONE";
    private static final Duration USER_STATUS_TTL = Duration.ofDays(30);
    private static final Duration SUBMISSION_DETAIL_TTL = Duration.ofMinutes(10);
    private static final int DEFAULT_DIRTY_DRAIN_LIMIT = 500;

    private final RedisCacheService redisCacheService;
    private final SubmissionMapper submissionMapper;

    /** 批量查询用户题目状态，缓存未命中时由调用方从数据库加载。 */
    public Map<Long, String> getUserProblemStatuses(Long userId,
                                                    Collection<Long> problemIds,
                                                    Function<List<Long>, Map<Long, String>> dbLoader) {
        List<Long> ids = normalizeIds(problemIds);
        Map<Long, String> result = new LinkedHashMap<>();
        if (userId == null || ids.isEmpty()) {
            return result;
        }

        String key = userProblemStatusKey(userId);
        List<String> fields = ids.stream().map(String::valueOf).toList();
        List<Object> cachedValues = redisCacheService.hashMultiGet(key, fields);
        Set<Long> missedIds = new LinkedHashSet<>();

        for (int i = 0; i < ids.size(); i++) {
            Long problemId = ids.get(i);
            Object cached = i < cachedValues.size() ? cachedValues.get(i) : null;
            String status = cached == null ? null : String.valueOf(cached);
            if (status == null) {
                missedIds.add(problemId);
            } else {
                result.put(problemId, STATUS_NONE.equals(status) ? null : status);
            }
        }

        if (!missedIds.isEmpty()) {
            Map<Long, String> loaded = dbLoader.apply(List.copyOf(missedIds));
            Map<String, Object> cachePatch = new LinkedHashMap<>();
            for (Long problemId : missedIds) {
                String status = loaded == null ? null : loaded.get(problemId);
                result.put(problemId, status);
                cachePatch.put(String.valueOf(problemId), status == null ? STATUS_NONE : status);
            }
            redisCacheService.hashPutAll(key, cachePatch);
        }

        redisCacheService.expire(key, USER_STATUS_TTL.toSeconds(), TimeUnit.SECONDS);
        return result;
    }

    /** 日常提交完成后更新用户题目状态，历史 AC 不会被后续错误提交覆盖。 */
    public void updateUserProblemStatus(Long userId, Long problemId, String finalStatus, Long contestId) {
        if (userId == null || problemId == null || contestId != null) {
            return;
        }
        String nextStatus = "ACCEPTED".equals(finalStatus) ? "ACCEPTED" : "ATTEMPTED";
        String key = userProblemStatusKey(userId);
        Object currentValue = redisCacheService.hashGet(key, String.valueOf(problemId));
        String current = currentValue == null ? null : String.valueOf(currentValue);
        if ("ACCEPTED".equals(current) && !"ACCEPTED".equals(nextStatus)) {
            return;
        }
        if (current == null && !"ACCEPTED".equals(nextStatus) && hasAcceptedSubmission(userId, problemId)) {
            nextStatus = "ACCEPTED";
        }
        redisCacheService.hashPut(key, String.valueOf(problemId), nextStatus);
        redisCacheService.expire(key, USER_STATUS_TTL.toSeconds(), TimeUnit.SECONDS);
    }

    /** 标记题目统计需要刷新，定时任务会异步重算提交数和通过率。 */
    public void markProblemStatsDirty(Long problemId, Long contestId) {
        if (problemId == null || contestId != null) {
            return;
        }
        redisCacheService.setAdd(DIRTY_PROBLEM_STATS_KEY, String.valueOf(problemId));
        redisCacheService.expire(DIRTY_PROBLEM_STATS_KEY, 7, TimeUnit.DAYS);
    }

    public List<Long> drainDirtyProblemIds() {
        return drainDirtyProblemIds(DEFAULT_DIRTY_DRAIN_LIMIT);
    }

    /** 从 Redis 中取出一批待刷新题目 ID，供统计任务消费。 */
    public List<Long> drainDirtyProblemIds(int limit) {
        List<Object> values = redisCacheService.setPop(DIRTY_PROBLEM_STATS_KEY, Math.max(1, limit));
        return values.stream()
                .map(this::toLong)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** 缓存已完成提交的详情，减少前端轮询和重复打开详情时的数据库查询。 */
    public void cacheSubmissionDetail(Long submissionId, Map<String, Object> detail) {
        if (submissionId == null || detail == null || detail.isEmpty()) {
            return;
        }
        redisCacheService.set(submissionDetailKey(submissionId), detail, SUBMISSION_DETAIL_TTL);
    }

    /** 读取缓存中的提交详情。 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedSubmissionDetail(Long submissionId) {
        if (submissionId == null) {
            return null;
        }
        Object cached = redisCacheService.get(submissionDetailKey(submissionId));
        if (cached instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return null;
    }

    /** 提交状态变化时清理详情缓存，确保前端拿到最新结果。 */
    public void evictSubmissionDetail(Long submissionId) {
        if (submissionId != null) {
            redisCacheService.delete(submissionDetailKey(submissionId));
        }
    }

    private String userProblemStatusKey(Long userId) {
        return USER_PROBLEM_STATUS_PREFIX + userId;
    }

    private String submissionDetailKey(Long submissionId) {
        return SUBMISSION_DETAIL_PREFIX + submissionId;
    }

    private List<Long> normalizeIds(Collection<Long> problemIds) {
        if (problemIds == null) {
            return List.of();
        }
        return problemIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.debug("Ignore malformed dirty problem id from Redis: {}", value);
            return null;
        }
    }

    private boolean hasAcceptedSubmission(Long userId, Long problemId) {
        try {
            return submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getUserId, userId)
                    .eq(Submission::getProblemId, problemId)
                    .eq(Submission::getStatus, "ACCEPTED")
                    .isNull(Submission::getContestId)) > 0;
        } catch (Exception e) {
            log.debug("Check accepted submission failed, keep attempted status: {}", e.getMessage());
            return false;
        }
    }
}
