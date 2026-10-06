/**
 * 文件说明：题目统计定时任务，负责按固定时间触发后台维护任务。
 */
package com.ls.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ls.domain.Problem;
import com.ls.domain.Submission;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.service.SubmissionRuntimeCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Refreshes problem submission statistics. Hot-path runs consume dirty problem ids;
 * the nightly full pass is a safety net for missed cache events or manual data repair.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemStatsScheduler {

    private static final int DIRTY_BATCH_SIZE = 500;

    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionRuntimeCacheService submissionRuntimeCacheService;

    @Scheduled(fixedRate = 300_000, initialDelay = 30_000)
    public void updateDirtyProblemStats() {
        List<Long> problemIds = submissionRuntimeCacheService.drainDirtyProblemIds(DIRTY_BATCH_SIZE);
        if (problemIds.isEmpty()) {
            log.debug("No dirty problem stats to refresh");
            return;
        }
        try {
            int updated = refreshProblemStats(problemIds);
            log.info("Refreshed dirty problem stats, dirtyIds={}, updated={}", problemIds.size(), updated);
        } catch (Exception e) {
            problemIds.forEach(problemId -> submissionRuntimeCacheService.markProblemStatsDirty(problemId, null));
            log.error("Refresh dirty problem stats failed, ids returned to dirty set", e);
        }
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void updateAllProblemStatsAtLowPeak() {
        try {
            List<Long> problemIds = problemMapper.selectList(
                            new LambdaQueryWrapper<Problem>()
                                    .select(Problem::getId)
                                    .eq(Problem::getDeleted, 0))
                    .stream()
                    .map(Problem::getId)
                    .collect(Collectors.toList());
            int updated = refreshProblemStats(problemIds);
            log.info("Nightly full problem stats refresh completed, updated={}", updated);
        } catch (Exception e) {
            log.error("Nightly full problem stats refresh failed", e);
        }
    }

    private int refreshProblemStats(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return 0;
        }

        List<Long> existingProblemIds = problemMapper.selectList(
                        new LambdaQueryWrapper<Problem>()
                                .select(Problem::getId)
                                .eq(Problem::getDeleted, 0)
                                .in(Problem::getId, problemIds))
                .stream()
                .map(Problem::getId)
                .distinct()
                .collect(Collectors.toList());
        if (existingProblemIds.isEmpty()) {
            return 0;
        }

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getProblemId, Submission::getUserId, Submission::getStatus)
                        .in(Submission::getProblemId, existingProblemIds)
                        .isNull(Submission::getContestId)
        );

        Map<Long, List<Submission>> grouped = submissions.stream()
                .collect(Collectors.groupingBy(Submission::getProblemId));

        int updated = 0;
        for (Long problemId : existingProblemIds) {
            List<Submission> subs = grouped.get(problemId);
            int submitCount = subs != null ? subs.size() : 0;
            long acceptCount = subs != null
                    ? subs.stream()
                    .filter(s -> "ACCEPTED".equals(s.getStatus()))
                    .map(Submission::getUserId)
                    .distinct()
                    .count()
                    : 0;
            BigDecimal acceptRate = submitCount > 0
                    ? BigDecimal.valueOf(acceptCount * 100.0 / submitCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Problem update = new Problem();
            update.setId(problemId);
            update.setSubmitCount(submitCount);
            update.setAcceptCount((int) acceptCount);
            update.setAcceptRate(acceptRate);
            problemMapper.updateById(update);
            updated++;
        }
        return updated;
    }
}
