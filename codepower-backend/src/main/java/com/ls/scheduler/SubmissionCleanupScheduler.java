/**
 * 文件说明：提交清理定时任务，负责按固定时间触发后台维护任务。
 */
package com.ls.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ls.domain.Submission;
import com.ls.domain.SubmissionResult;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.SubmissionResultMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定时清理旧提交记录
 *
 * 保留策略：
 *   - 30天内的提交：全部保留（为能力算法的时间衰减提供完整数据）
 *   - 30天外的提交：每道题为每个用户保留两条 —— 最新一次提交 + 最新一次通过提交
 *     （保证"草稿恢复"和"是否做对过"两个需求不丢失）
 *
 * 注意：user_problem_solve_records 表独立记录通过状态，不受此清理影响
 */
@Slf4j
@Component
public class SubmissionCleanupScheduler {

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private SubmissionResultMapper submissionResultMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldSubmissions() {
        log.info("开始清理旧提交记录...");
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

            // 只查询30天之前的提交
            List<Submission> oldSubmissions = submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>()
                            .select(Submission::getId, Submission::getUserId,
                                    Submission::getProblemId, Submission::getStatus,
                                    Submission::getCreatedAt)
                            .lt(Submission::getCreatedAt, thirtyDaysAgo)
                            .orderByDesc(Submission::getCreatedAt)
            );

            if (oldSubmissions.isEmpty()) {
                log.info("没有需要清理的旧提交记录");
                return;
            }

            // 按 (userId, problemId) 分组
            Map<String, List<Submission>> grouped = oldSubmissions.stream()
                    .collect(Collectors.groupingBy(
                            s -> s.getUserId() + "_" + s.getProblemId()
                    ));

            List<Long> toDelete = new ArrayList<>();
            for (List<Submission> subs : grouped.values()) {
                if (subs.size() <= 2) continue;

                subs.sort(Comparator.comparing(Submission::getCreatedAt).reversed());

                // 保留最新一条
                Set<Long> keepIds = new HashSet<>();
                keepIds.add(subs.get(0).getId());

                // 保留最新一条 ACCEPTED（如果存在且与最新提交不同）
                for (Submission s : subs) {
                    if ("ACCEPTED".equals(s.getStatus())) {
                        keepIds.add(s.getId());
                        break;
                    }
                }

                // 其余标记删除
                for (Submission s : subs) {
                    if (!keepIds.contains(s.getId())) {
                        toDelete.add(s.getId());
                    }
                }
            }

            if (toDelete.isEmpty()) {
                log.info("没有需要清理的提交记录");
                return;
            }

            // 分批删除
            int batchSize = 500;
            int deleted = 0;
            for (int i = 0; i < toDelete.size(); i += batchSize) {
                List<Long> batch = toDelete.subList(i, Math.min(i + batchSize, toDelete.size()));
                submissionResultMapper.delete(
                        new LambdaQueryWrapper<SubmissionResult>()
                                .in(SubmissionResult::getSubmissionId, batch)
                );
                submissionMapper.deleteBatchIds(batch);
                deleted += batch.size();
            }

            log.info("提交记录清理完成，共删除 {} 条旧记录（30天内提交已全部保留）", deleted);
        } catch (Exception e) {
            log.error("清理提交记录失败", e);
        }
    }
}
