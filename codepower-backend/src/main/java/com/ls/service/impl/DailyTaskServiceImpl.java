/**
 * 文件说明：每日任务 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.DailyTask;
import com.ls.domain.Submission;
import com.ls.domain.UserDailyTaskProgress;
import com.ls.domain.UserProblemSolveRecord;
import com.ls.mapper.DailyTaskMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.UserDailyTaskProgressMapper;
import com.ls.mapper.UserProblemSolveRecordMapper;
import com.ls.service.DailyTaskService;
import com.ls.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** 每日任务服务实现 */
@Service
@RequiredArgsConstructor
public class DailyTaskServiceImpl implements DailyTaskService {

    private static final String TASK_SUBMIT_CODE = "submit_code";
    private static final String TASK_SUBMIT_THREE_TIMES = "submit_three_times";
    private static final String TASK_SOLVE_ONE_PROBLEM = "pass_problem";
    private static final String TASK_SOLVE_ONE_PROBLEM_LEGACY = "solve_one_problem";
    private static final String TASK_PASS_THREE = "pass_three";
    private static final String TASK_USE_AI_ONCE = "use_ai_once";
    private static final String TASK_AI_CHAT = "ai_chat";

    private final DailyTaskMapper dailyTaskMapper;
    private final UserDailyTaskProgressMapper progressMapper;
    private final UserLevelService userLevelService;
    private final SubmissionMapper submissionMapper;
    private final UserProblemSolveRecordMapper solveRecordMapper;

    @Override
    public List<Map<String, Object>> getDailyTasks(Long userId) {
        LocalDate today = LocalDate.now();

        List<DailyTask> tasks = dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>().eq(DailyTask::getActive, true));

        List<UserDailyTaskProgress> progresses = progressMapper.selectList(
                new LambdaQueryWrapper<UserDailyTaskProgress>()
                        .eq(UserDailyTaskProgress::getUserId, userId)
                        .eq(UserDailyTaskProgress::getTaskDate, today));

        Map<Long, UserDailyTaskProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(UserDailyTaskProgress::getTaskId, p -> p));

        return tasks.stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(task -> {
                    UserDailyTaskProgress progress = progressMap.get(task.getId());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("taskId", task.getId());
                    item.put("taskKey", task.getTaskKey());
                    item.put("title", taskTitle(task));
                    item.put("description", taskDescription(task));
                    item.put("expReward", Optional.ofNullable(task.getExpReward()).orElse(0));
                    item.put("aiReward", Optional.ofNullable(task.getAiReward()).orElse(0));
                    item.put("requiredCount", Optional.ofNullable(task.getRequiredCount()).orElse(1));
                    item.put("currentCount", progress != null ? Optional.ofNullable(progress.getCurrentCount()).orElse(0) : 0);
                    item.put("completed", progress != null && Boolean.TRUE.equals(progress.getCompleted()));
                    item.put("rewarded", progress != null && Boolean.TRUE.equals(progress.getRewarded()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementTaskProgress(Long userId, String taskKey) {
        incrementTaskProgress(userId, taskKey, 1);
    }

    @Override
    @Transactional
    public void recordAcceptedProblem(Long userId, Long problemId) {
        if (userId == null || problemId == null) {
            return;
        }

        Submission latestAcceptedSubmission = submissionMapper.selectOne(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .eq(Submission::getProblemId, problemId)
                        .eq(Submission::getStatus, "ACCEPTED")
                        .isNull(Submission::getContestId)
                        .orderByDesc(Submission::getCreatedAt)
                        .orderByDesc(Submission::getId)
                        .last("LIMIT 1"));
        if (latestAcceptedSubmission == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        UserProblemSolveRecord existingRecord = solveRecordMapper.selectOne(
                new LambdaQueryWrapper<UserProblemSolveRecord>()
                        .eq(UserProblemSolveRecord::getUserId, userId)
                        .eq(UserProblemSolveRecord::getProblemId, problemId)
                        .last("LIMIT 1"));

        int inserted = solveRecordMapper.insertIgnore(
                userId,
                problemId,
                latestAcceptedSubmission.getId(),
                latestAcceptedSubmission.getCreatedAt());

        if (inserted > 0) {
            incrementTaskProgress(userId, TASK_SOLVE_ONE_PROBLEM, 1, today);
            incrementTaskProgress(userId, TASK_PASS_THREE, 1, today);
            return;
        }

        if (existingRecord == null) {
            return;
        }

        boolean shouldTouch = existingRecord.getLatestAcceptedSubmissionId() == null
                || latestAcceptedSubmission.getId() > existingRecord.getLatestAcceptedSubmissionId();
        if (shouldTouch) {
            solveRecordMapper.touchAccepted(
                    userId,
                    problemId,
                    latestAcceptedSubmission.getId(),
                    latestAcceptedSubmission.getCreatedAt());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> claimTaskReward(Long userId, Long taskId) {
        LocalDate today = LocalDate.now();
        UserDailyTaskProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<UserDailyTaskProgress>()
                        .eq(UserDailyTaskProgress::getUserId, userId)
                        .eq(UserDailyTaskProgress::getTaskId, taskId)
                        .eq(UserDailyTaskProgress::getTaskDate, today));

        if (progress == null || !Boolean.TRUE.equals(progress.getCompleted())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "任务未完成");
        }
        if (Boolean.TRUE.equals(progress.getRewarded())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "奖励已领取");
        }

        DailyTask task = dailyTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }

        progress.setRewarded(true);
        progressMapper.updateById(progress);

        int expReward = Optional.ofNullable(task.getExpReward()).orElse(0);
        int aiReward = Optional.ofNullable(task.getAiReward()).orElse(0);
        userLevelService.addExp(userId, expReward);
        if (aiReward > 0) {
            userLevelService.addAiBonus(userId, aiReward);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expReward", expReward);
        result.put("aiReward", aiReward);
        result.put("message", "获得 " + expReward + " 经验" +
                (aiReward > 0 ? " + " + aiReward + " AI积分" : ""));
        return result;
    }

    private void incrementTaskProgress(Long userId, String taskKey, int delta) {
        incrementTaskProgress(userId, taskKey, delta, LocalDate.now());
    }

    private void incrementTaskProgress(Long userId, String taskKey, int delta, LocalDate taskDate) {
        if (delta <= 0) {
            return;
        }

        DailyTask task = findActiveTask(taskKey);
        if (task == null) {
            return;
        }

        UserDailyTaskProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<UserDailyTaskProgress>()
                        .eq(UserDailyTaskProgress::getUserId, userId)
                        .eq(UserDailyTaskProgress::getTaskId, task.getId())
                        .eq(UserDailyTaskProgress::getTaskDate, taskDate));

        int requiredCount = Optional.ofNullable(task.getRequiredCount()).orElse(1);
        if (progress == null) {
            progress = new UserDailyTaskProgress();
            progress.setUserId(userId);
            progress.setTaskId(task.getId());
            progress.setTaskDate(taskDate);
            progress.setCurrentCount(delta);
            progress.setCompleted(delta >= requiredCount);
            progress.setRewarded(false);
            progressMapper.insert(progress);
            return;
        }

        if (Boolean.TRUE.equals(progress.getCompleted())) {
            return;
        }

        int nextCount = Optional.ofNullable(progress.getCurrentCount()).orElse(0) + delta;
        progress.setCurrentCount(nextCount);
        progress.setCompleted(nextCount >= requiredCount);
        progressMapper.updateById(progress);
    }

    private DailyTask findActiveTask(String taskKey) {
        DailyTask task = dailyTaskMapper.selectOne(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getTaskKey, taskKey)
                        .eq(DailyTask::getActive, true));
        if (task != null) {
            return task;
        }
        String alias = normalizeTaskKey(taskKey);
        if (Objects.equals(alias, taskKey)) {
            return null;
        }
        return dailyTaskMapper.selectOne(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getTaskKey, alias)
                        .eq(DailyTask::getActive, true));
    }

    private String normalizeTaskKey(String taskKey) {
        if (TASK_SUBMIT_CODE.equals(taskKey)) {
            return TASK_SUBMIT_THREE_TIMES;
        }
        if (TASK_AI_CHAT.equals(taskKey)) {
            return TASK_USE_AI_ONCE;
        }
        if (TASK_PASS_THREE.equals(taskKey)) {
            return TASK_SOLVE_ONE_PROBLEM;
        }
        if (TASK_SOLVE_ONE_PROBLEM.equals(taskKey)) {
            return TASK_SOLVE_ONE_PROBLEM_LEGACY;
        }
        if (TASK_SOLVE_ONE_PROBLEM_LEGACY.equals(taskKey)) {
            return TASK_SOLVE_ONE_PROBLEM;
        }
        return taskKey;
    }

    private String taskTitle(DailyTask task) {
        if (task.getTitle() != null && !task.getTitle().isBlank()) {
            return task.getTitle();
        }
        return task.getTaskKey();
    }

    private String taskDescription(DailyTask task) {
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            return task.getDescription();
        }
        return taskTitle(task);
    }
}
