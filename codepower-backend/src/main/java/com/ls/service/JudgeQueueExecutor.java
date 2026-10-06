/**
 * 文件说明：评测队列执行器，负责异步提交排队、工作线程调度和后台监控统计。
 */
package com.ls.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步评测统一队列。
 * 所有异步提交先进入这里，再由固定数量的工作线程按进入顺序送去 Judge0 集群。
 */
@Slf4j
@Service
public class JudgeQueueExecutor {

    private static final AtomicInteger THREAD_ID = new AtomicInteger(1);
    private static final int RECENT_LIMIT = 30;

    @Value("${judge0.queue.worker-count:9}")
    private int configuredWorkerCount;

    @Value("${judge0.queue.capacity:1000}")
    private int configuredQueueCapacity;

    private ThreadPoolExecutor executor;
    private final AtomicLong submittedTasks = new AtomicLong();
    private final AtomicLong startedTasks = new AtomicLong();
    private final AtomicLong completedTasks = new AtomicLong();
    private final AtomicLong failedTasks = new AtomicLong();
    private final AtomicLong rejectedTasks = new AtomicLong();
    private final AtomicLong totalQueueWaitMs = new AtomicLong();
    private final AtomicLong totalRunMs = new AtomicLong();
    private final AtomicLong lastQueueWaitMs = new AtomicLong();
    private final AtomicLong lastRunMs = new AtomicLong();
    private final AtomicLong lastSubmittedAt = new AtomicLong();
    private final AtomicLong lastStartedAt = new AtomicLong();
    private final AtomicLong lastCompletedAt = new AtomicLong();
    private final ConcurrentHashMap<Long, TaskSnapshot> runningTasks = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<TaskSnapshot> recentTasks = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> recentCompletionTimes = new ConcurrentLinkedDeque<>();

    /** 初始化固定线程数的评测线程池，并提前启动工作线程。 */
    @PostConstruct
    void init() {
        int workers = Math.max(1, configuredWorkerCount);
        int capacity = Math.max(workers, configuredQueueCapacity);
        executor = new ThreadPoolExecutor(
                workers,
                workers,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(capacity),
                judgeThreadFactory(),
                (task, ignored) -> {
                    rejectedTasks.incrementAndGet();
                    throw new RejectedExecutionException("Judge queue is full");
                }
        );
        executor.prestartAllCoreThreads();
        log.info("Judge queue initialized, workers={}, capacity={}", workers, capacity);
    }

    /** 系统关闭时停止评测线程池。 */
    @PreDestroy
    void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    /** 提交一个异步评测任务，并记录它进入队列的时间。 */
    public void submit(Long submissionId, Runnable runnable) {
        if (executor == null) {
            throw new RejectedExecutionException("Judge queue is not initialized");
        }
        // 先记录入队时间，再交给线程池；后续可计算“排队等待多久”和“实际评测多久”。
        long queuedAtMillis = System.currentTimeMillis();
        submittedTasks.incrementAndGet();
        lastSubmittedAt.set(queuedAtMillis);
        executor.execute(new QueuedJudgeTask(submissionId, runnable, queuedAtMillis));
    }

    /**
     * 返回队列长度、工作线程、运行任务和最近任务，用于管理员后台实时监控。
     *
     * 返回 JSON 格式（前端 AdminView.vue 中 loadJudgeCluster() 定时刷新）:
     * {
     *   "initialized": true,
     *   "workerCount": 9,           ← 工作线程数
     *   "activeCount": 3,           ← 当前正在评测的任务数
     *   "queueSize": 12,            ← 排队中的任务数
     *   "submittedTasks": 1520,     ← 历史总提交数
     *   "completedTasks": 1505,     ← 已完成数
     *   "failedTasks": 3,           ← 失败数
     *   "rejectedTasks": 0,         ← 队列满被拒绝数
     *   "avgQueueWaitMs": 320,      ← 平均排队等待耗时(ms)
     *   "avgRunMs": 2800,           ← 平均评测耗时(ms)
     *   "completedLastMinute": 15,  ← 最近1分钟完成数
     *   "runningTasks": [...],      ← 当前运行中的任务
     *   "recentTasks": [...]        ← 最近完成的任务
     * }
     */
    public Map<String, Object> getStatus() {
        ThreadPoolExecutor current = executor;
        pruneRecentCompletionTimes();
        Map<String, Object> status = new LinkedHashMap<>();
        if (current == null) {
            status.put("initialized", false);
            return status;
        }

        // 管理后台需要的是实时快照，因此这里直接读取线程池和内存计数器，不再访问数据库。
        long started = Math.max(1, startedTasks.get());
        long completed = Math.max(1, completedTasks.get());
        status.put("initialized", true);
        status.put("workerCount", current.getCorePoolSize());
        status.put("poolSize", current.getPoolSize());
        status.put("activeCount", current.getActiveCount());
        status.put("queueSize", current.getQueue().size());
        status.put("queueRemainingCapacity", current.getQueue().remainingCapacity());
        status.put("submittedTasks", submittedTasks.get());
        status.put("startedTasks", startedTasks.get());
        status.put("completedTasks", completedTasks.get());
        status.put("failedTasks", failedTasks.get());
        status.put("rejectedTasks", rejectedTasks.get());
        status.put("largestPoolSize", current.getLargestPoolSize());
        status.put("avgQueueWaitMs", totalQueueWaitMs.get() / started);
        status.put("lastQueueWaitMs", lastQueueWaitMs.get());
        status.put("avgRunMs", totalRunMs.get() / completed);
        status.put("lastRunMs", lastRunMs.get());
        status.put("completedLastMinute", recentCompletionTimes.size());
        status.put("lastSubmittedAt", toDateTime(lastSubmittedAt.get()));
        status.put("lastStartedAt", toDateTime(lastStartedAt.get()));
        status.put("lastCompletedAt", toDateTime(lastCompletedAt.get()));
        status.put("runningTasks", runningTasks.values().stream()
                .sorted(Comparator.comparing(TaskSnapshot::startedAtMillis))
                .map(TaskSnapshot::toMap)
                .toList());
        status.put("recentTasks", recentTasks.stream()
                .limit(RECENT_LIMIT)
                .map(TaskSnapshot::toMap)
                .toList());
        return status;
    }

    private ThreadFactory judgeThreadFactory() {
        return task -> {
            Thread thread = new Thread(task, "codepower-judge-queue-" + THREAD_ID.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    private void recordRecent(TaskSnapshot snapshot) {
        recentTasks.addFirst(snapshot);
        while (recentTasks.size() > RECENT_LIMIT) {
            recentTasks.pollLast();
        }
        recentCompletionTimes.addLast(snapshot.finishedAtMillis());
        pruneRecentCompletionTimes();
    }

    private void pruneRecentCompletionTimes() {
        long cutoff = System.currentTimeMillis() - 60_000L;
        Long value;
        while ((value = recentCompletionTimes.peekFirst()) != null && value < cutoff) {
            recentCompletionTimes.pollFirst();
        }
    }

    private LocalDateTime toDateTime(long millis) {
        if (millis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault());
    }

    private final class QueuedJudgeTask implements Runnable {
        private final Long submissionId;
        private final Runnable delegate;
        private final long queuedAtMillis;

        private QueuedJudgeTask(Long submissionId, Runnable delegate, long queuedAtMillis) {
            this.submissionId = submissionId;
            this.delegate = delegate;
            this.queuedAtMillis = queuedAtMillis;
        }

        /**
         * 工作线程取出任务并执行。
         *
         * 这里的 delegate.run() 最终调用的是 CodeTestController.executeAsyncJudgement()，
         * 也就是 judgeAllTestCases() → executeCodeWithJudge0() → acquireJudgeNode() → executeCodeOnNode()。
         *
         * 执行前记录 queueWaitMs（排队耗时），执行后记录 runMs（评测耗时），
         * 用于管理员后台监控队列健康状态。
         */
        @Override
        public void run() {
            long startedAtMillis = System.currentTimeMillis();
            long queueWaitMs = Math.max(0, startedAtMillis - queuedAtMillis);
            lastQueueWaitMs.set(queueWaitMs);
            totalQueueWaitMs.addAndGet(queueWaitMs);
            startedTasks.incrementAndGet();
            lastStartedAt.set(startedAtMillis);

            TaskSnapshot running = new TaskSnapshot(submissionId, "RUNNING", queuedAtMillis,
                    startedAtMillis, 0L, queueWaitMs, 0L, null);
            if (submissionId != null) {
                runningTasks.put(submissionId, running);
            }

            String status = "SUCCESS";
            String error = null;
            try {
                delegate.run();
            } catch (Throwable t) {
                status = "FAILED";
                error = t.getMessage();
                failedTasks.incrementAndGet();
                throw t;
            } finally {
                long finishedAtMillis = System.currentTimeMillis();
                long runMs = Math.max(0, finishedAtMillis - startedAtMillis);
                lastRunMs.set(runMs);
                totalRunMs.addAndGet(runMs);
                completedTasks.incrementAndGet();
                lastCompletedAt.set(finishedAtMillis);
                if (submissionId != null) {
                    runningTasks.remove(submissionId);
                }
                recordRecent(new TaskSnapshot(submissionId, status, queuedAtMillis,
                        startedAtMillis, finishedAtMillis, queueWaitMs, runMs, error));
            }
        }
    }

    /** 管理后台展示用的任务快照，不直接影响评测结果。 */
    private record TaskSnapshot(Long submissionId,
                                String status,
                                long queuedAtMillis,
                                long startedAtMillis,
                                long finishedAtMillis,
                                long queueWaitMs,
                                long runMs,
                                String error) {
        private Map<String, Object> toMap() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("submissionId", submissionId);
            item.put("status", status);
            item.put("queuedAt", toDateTimeValue(queuedAtMillis));
            item.put("startedAt", toDateTimeValue(startedAtMillis));
            item.put("finishedAt", toDateTimeValue(finishedAtMillis));
            item.put("queueWaitMs", queueWaitMs);
            item.put("runMs", runMs);
            item.put("error", error);
            return item;
        }

        private static LocalDateTime toDateTimeValue(long millis) {
            if (millis <= 0) {
                return null;
            }
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault());
        }
    }
}
