/**
 * 文件说明：提交记录 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.common.Result;
import com.ls.domain.Contest;
import com.ls.domain.ContestProblem;
import com.ls.domain.Submission;
import com.ls.domain.SubmissionResult;
import com.ls.domain.User;
import com.ls.domain.UserProfile;
import com.ls.mapper.ContestMapper;
import com.ls.mapper.ContestProblemMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.SubmissionResultMapper;
import com.ls.mapper.UserMapper;
import com.ls.mapper.UserProfileMapper;
import com.ls.service.SubmissionRuntimeCacheService;
import com.ls.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 提交记录控制器
 * 提供提交历史、详情、草稿恢复、用户题目状态等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private SubmissionResultMapper submissionResultMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private ContestMapper contestMapper;

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SubmissionRuntimeCacheService submissionRuntimeCacheService;

    /**
     * 查询当前用户在某道题的提交历史（分页）
     */
    @GetMapping("/problem/{problemId}")
    public Result<Map<String, Object>> getMySubmissions(
            @PathVariable Long problemId,
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        Page<Submission> p = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .select(Submission::getId, Submission::getProblemId, Submission::getUserId,
                        Submission::getLanguage, Submission::getStatus, Submission::getScore,
                        Submission::getExecutionTime, Submission::getMemoryUsed,
                        Submission::getContestId, Submission::getCreatedAt)
                .eq(Submission::getUserId, user.getId())
                .eq(Submission::getProblemId, problemId)
                .isNull(Submission::getContestId)
                .orderByDesc(Submission::getCreatedAt);

        IPage<Submission> result = submissionMapper.selectPage(p, wrapper);
        return Result.success(buildSubmissionPageData(result));
    }

    /**
     * 查询当前用户在某场竞赛某道题的提交历史（分页）
     */
    @GetMapping("/contest/{contestId}/problem/{problemId}")
    public Result<Map<String, Object>> getMyContestProblemSubmissions(
            @PathVariable Long contestId,
            @PathVariable Long problemId,
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        Page<Submission> p = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .select(Submission::getId, Submission::getProblemId, Submission::getUserId,
                        Submission::getLanguage, Submission::getStatus, Submission::getScore,
                        Submission::getExecutionTime, Submission::getMemoryUsed,
                        Submission::getContestId, Submission::getCreatedAt)
                .eq(Submission::getUserId, user.getId())
                .eq(Submission::getContestId, contestId)
                .eq(Submission::getProblemId, problemId)
                .orderByDesc(Submission::getCreatedAt);

        IPage<Submission> result = submissionMapper.selectPage(p, wrapper);
        return Result.success(buildSubmissionPageData(result));
    }

    /**
     * 批量获取当前用户在某场竞赛每道题的最近一次提交代码，用于竞赛 IDE 快速恢复。
     */
    @GetMapping("/contest/{contestId}/drafts")
    public Result<Map<Long, Map<String, Object>>> getMyContestLatestDrafts(
            @PathVariable Long contestId,
            @RequestHeader("Authorization") String auth) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) return Result.error(404, "竞赛不存在");

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getId, Submission::getProblemId, Submission::getUserId,
                                Submission::getCode, Submission::getLanguage, Submission::getStatus,
                                Submission::getScore, Submission::getContestId, Submission::getCreatedAt)
                        .eq(Submission::getUserId, user.getId())
                        .eq(Submission::getContestId, contestId)
                        .orderByDesc(Submission::getCreatedAt)
                        .orderByDesc(Submission::getId)
        );

        Map<Long, Map<String, Object>> drafts = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            if (submission.getProblemId() == null || drafts.containsKey(submission.getProblemId())) {
                continue;
            }
            drafts.put(submission.getProblemId(), buildContestDraftData(submission));
        }
        return Result.success(drafts);
    }

    /**
     * 查询竞赛最近提交（分页）。
     * 普通参赛者只看到自己的记录；创建者和管理员看到全场记录，便于考试监考与赛后统计。
     */
    @GetMapping("/contest/{contestId}")
    public Result<Map<String, Object>> getMyContestSubmissions(
            @PathVariable Long contestId,
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) return Result.error(404, "竞赛不存在");

        boolean canViewAll = "ADMIN".equals(user.getRole()) || Objects.equals(contest.getCreatorId(), user.getId());
        Page<Submission> p = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .select(Submission::getId, Submission::getProblemId, Submission::getUserId,
                        Submission::getLanguage, Submission::getStatus, Submission::getScore,
                        Submission::getExecutionTime, Submission::getMemoryUsed,
                        Submission::getContestId, Submission::getCreatedAt)
                .eq(!canViewAll, Submission::getUserId, user.getId())
                .eq(Submission::getContestId, contestId)
                .orderByDesc(Submission::getCreatedAt);

        IPage<Submission> result = submissionMapper.selectPage(p, wrapper);
        Map<String, Object> data = buildSubmissionPageData(result);
        data.put("scope", canViewAll ? "ALL" : "SELF");
        return Result.success(data);
    }

    /**
     * 查询竞赛中某位选手的提交历史（用于排行榜下钻）
     */
    @GetMapping("/contest/{contestId}/user/{userId}")
    public Result<Map<String, Object>> getContestUserSubmissions(
            @PathVariable Long contestId,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        User viewer = getUserFromAuth(auth);
        if (viewer == null) return Result.error(401, "用户未登录");

        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) return Result.error(404, "竞赛不存在");

        if (!canViewContestUserSubmissions(contest, viewer, userId)) {
            return Result.error(403, "无权查看该选手提交记录");
        }

        Page<Submission> p = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .select(Submission::getId, Submission::getProblemId, Submission::getUserId,
                        Submission::getLanguage, Submission::getStatus, Submission::getScore,
                        Submission::getExecutionTime, Submission::getMemoryUsed,
                        Submission::getContestId, Submission::getCreatedAt)
                .eq(Submission::getUserId, userId)
                .eq(Submission::getContestId, contestId)
                .orderByDesc(Submission::getCreatedAt);

        IPage<Submission> result = submissionMapper.selectPage(p, wrapper);
        Map<String, Object> data = buildSubmissionPageData(result);
        User targetUser = userMapper.selectById(userId);
        UserProfile profile = userProfileMapper.findByUserId(userId);
        data.put("user", buildContestUserSummary(targetUser, profile));
        return Result.success(data);
    }

    /**
     * 查询提交详情（含代码、每个测试点结果）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getSubmissionDetail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String auth) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        Submission submission = submissionMapper.selectById(id);
        if (submission == null) return Result.error(404, "提交记录不存在");

        if (!canViewSubmission(submission, user)) {
            return Result.error(403, "无权查看此提交记录");
        }

        Map<String, Object> cached = submissionRuntimeCacheService.getCachedSubmissionDetail(id);
        if (cached != null) {
            return Result.success(cached);
        }

        List<SubmissionResult> testResults = submissionResultMapper.selectList(
                new LambdaQueryWrapper<SubmissionResult>()
                        .eq(SubmissionResult::getSubmissionId, id)
                        .orderByAsc(SubmissionResult::getId)
        );

        Map<String, Object> data = buildSubmissionDetailData(submission, testResults);
        User owner = userMapper.selectById(submission.getUserId());
        UserProfile profile = userProfileMapper.findByUserId(submission.getUserId());
        data.put("user", buildContestUserSummary(owner, profile));

        if (!isPendingSubmissionStatus(submission.getStatus())) {
            submissionRuntimeCacheService.cacheSubmissionDetail(id, data);
        }

        return Result.success(data);
    }

    /**
     * 获取用户在某题的最后一次提交（用于草稿恢复）
     * 返回代码和语言，前端可直接填充到编辑器
     */
    @GetMapping("/last/{problemId}")
    public Result<Map<String, Object>> getLastSubmission(
            @PathVariable Long problemId,
            @RequestHeader("Authorization") String auth) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        Submission last = submissionMapper.selectOne(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getId, Submission::getCode, Submission::getLanguage,
                                Submission::getStatus, Submission::getCreatedAt)
                        .eq(Submission::getUserId, user.getId())
                        .eq(Submission::getProblemId, problemId)
                        .isNull(Submission::getContestId)
                        .orderByDesc(Submission::getCreatedAt)
                        .orderByDesc(Submission::getId)
                        .last("LIMIT 1")
        );

        if (last == null) {
            return Result.success(null);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", last.getCode());
        data.put("language", last.getLanguage());
        data.put("status", last.getStatus());
        data.put("createdAt", last.getCreatedAt());
        return Result.success(data);
    }

    /**
     * 查询当前用户最近一次提交使用的语言。
     * 当前题没有历史提交时，前端用它选择默认模板，避免每次都回到 Java。
     */
    @GetMapping("/preferred-language")
    public Result<Map<String, Object>> getPreferredLanguage(
            @RequestHeader("Authorization") String auth) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        Submission last = submissionMapper.selectOne(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getId, Submission::getProblemId, Submission::getContestId,
                                Submission::getLanguage, Submission::getStatus, Submission::getCreatedAt)
                        .eq(Submission::getUserId, user.getId())
                        .isNotNull(Submission::getLanguage)
                        .orderByDesc(Submission::getCreatedAt)
                        .orderByDesc(Submission::getId)
                        .last("LIMIT 1")
        );

        Map<String, Object> data = new LinkedHashMap<>();
        if (last != null) {
            data.put("language", last.getLanguage());
            data.put("submissionId", last.getId());
            data.put("problemId", last.getProblemId());
            data.put("contestId", last.getContestId());
            data.put("status", last.getStatus());
            data.put("createdAt", last.getCreatedAt());
        } else {
            data.put("language", null);
        }
        return Result.success(data);
    }

    /**
     * 批量查询用户对多道题的做题状态
     * 返回: { problemId -> "ACCEPTED" | "ATTEMPTED" | null }
     * ACCEPTED=已通过, ATTEMPTED=提交过但未通过, null=未尝试
     */
    @PostMapping("/user-status")
    public Result<Map<Long, String>> getUserProblemStatus(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String auth) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        @SuppressWarnings("unchecked")
        List<Number> problemIdNums = (List<Number>) request.get("problemIds");
        if (problemIdNums == null || problemIdNums.isEmpty()) {
            return Result.success(Collections.emptyMap());
        }
        List<Long> problemIds = problemIdNums.stream()
                .map(Number::longValue).collect(Collectors.toList());

        Map<Long, String> statusMap = submissionRuntimeCacheService.getUserProblemStatuses(
                user.getId(),
                problemIds,
                missIds -> {
                    List<Submission> submissions = submissionMapper.selectList(
                            new LambdaQueryWrapper<Submission>()
                                    .select(Submission::getProblemId, Submission::getStatus)
                                    .eq(Submission::getUserId, user.getId())
                                    .isNull(Submission::getContestId)
                                    .in(Submission::getProblemId, missIds)
                    );
                    return buildProblemStatusMap(missIds, submissions);
                }
        );

        return Result.success(statusMap);
    }

    /**
     * 批量查询用户在竞赛中的题目状态
     */
    @PostMapping("/contest/{contestId}/user-status")
    public Result<Map<Long, Map<String, Object>>> getUserContestProblemStatus(
            @PathVariable Long contestId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String auth) {
        User user = getUserFromAuth(auth);
        if (user == null) return Result.error(401, "用户未登录");

        @SuppressWarnings("unchecked")
        List<Number> problemIdNums = (List<Number>) request.get("problemIds");
        if (problemIdNums == null || problemIdNums.isEmpty()) {
            return Result.success(Collections.emptyMap());
        }
        List<Long> problemIds = problemIdNums.stream()
                .map(Number::longValue).collect(Collectors.toList());

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .select(Submission::getProblemId, Submission::getStatus, Submission::getScore, Submission::getCreatedAt)
                        .eq(Submission::getUserId, user.getId())
                        .eq(Submission::getContestId, contestId)
                        .in(Submission::getProblemId, problemIds)
        );

        List<ContestProblem> contestProblems = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .select(ContestProblem::getProblemId, ContestProblem::getScore)
                        .eq(ContestProblem::getContestId, contestId)
                        .in(ContestProblem::getProblemId, problemIds)
        );
        Map<Long, Integer> totalScoreMap = contestProblems.stream()
                .collect(Collectors.toMap(
                        ContestProblem::getProblemId,
                        item -> item.getScore() == null ? 100 : item.getScore(),
                        (a, b) -> a
                ));

        return Result.success(buildContestProblemProgressMap(problemIds, submissions, totalScoreMap));
    }

    private Map<String, Object> buildSubmissionPageData(IPage<Submission> result) {
        List<Long> userIds = result.getRecords().stream()
                .map(Submission::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = loadUserSummaryMap(userIds);
        Map<Long, UserProfile> profileMap = loadUserProfileMap(userIds);

        List<Map<String, Object>> records = result.getRecords().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("problemId", s.getProblemId());
            m.put("userId", s.getUserId());
            m.put("language", s.getLanguage());
            m.put("status", s.getStatus());
            m.put("score", s.getScore());
            m.put("executionTime", s.getExecutionTime());
            m.put("memoryUsed", s.getMemoryUsed());
            m.put("contestId", s.getContestId());
            m.put("createdAt", s.getCreatedAt());
            m.put("user", buildContestUserSummary(userMap.get(s.getUserId()), profileMap.get(s.getUserId())));
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    private Map<String, Object> buildContestDraftData(Submission submission) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", submission.getId());
        data.put("problemId", submission.getProblemId());
        data.put("contestId", submission.getContestId());
        data.put("language", submission.getLanguage());
        data.put("code", submission.getCode());
        data.put("status", submission.getStatus());
        data.put("score", submission.getScore());
        data.put("createdAt", submission.getCreatedAt());
        return data;
    }

    private Map<Long, User> loadUserSummaryMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(
                        new LambdaQueryWrapper<User>()
                                .select(User::getId, User::getUsername, User::getRole)
                                .in(User::getId, ids))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, UserProfile> loadUserProfileMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userProfileMapper.selectBatchIds(ids)
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));
    }

    private Map<Long, String> buildProblemStatusMap(List<Long> problemIds, List<Submission> submissions) {
        Map<Long, List<Submission>> grouped = submissions.stream()
                .collect(Collectors.groupingBy(Submission::getProblemId));

        Map<Long, String> statusMap = new LinkedHashMap<>();
        for (Long pid : problemIds) {
            List<Submission> subs = grouped.get(pid);
            if (subs == null || subs.isEmpty()) {
                statusMap.put(pid, null);
            } else {
                boolean accepted = subs.stream().anyMatch(s -> "ACCEPTED".equals(s.getStatus()));
                boolean pending = subs.stream().anyMatch(s -> isPendingSubmissionStatus(s.getStatus()));
                if (accepted) {
                    statusMap.put(pid, "ACCEPTED");
                } else if (pending) {
                    statusMap.put(pid, "PENDING");
                } else {
                    statusMap.put(pid, "ATTEMPTED");
                }
            }
        }
        return statusMap;
    }

    private Map<Long, Map<String, Object>> buildContestProblemProgressMap(List<Long> problemIds,
                                                                          List<Submission> submissions,
                                                                          Map<Long, Integer> totalScoreMap) {
        Map<Long, List<Submission>> grouped = submissions.stream()
                .filter(s -> s.getProblemId() != null)
                .collect(Collectors.groupingBy(Submission::getProblemId));

        Map<Long, Map<String, Object>> progressMap = new LinkedHashMap<>();
        for (Long pid : problemIds) {
            List<Submission> subs = grouped.getOrDefault(pid, Collections.emptyList());
            boolean accepted = subs.stream().anyMatch(s -> "ACCEPTED".equals(s.getStatus()));
            boolean pending = subs.stream().anyMatch(s -> isPendingSubmissionStatus(s.getStatus()));
            long finalAttempts = subs.stream().filter(s -> !isPendingSubmissionStatus(s.getStatus())).count();
            int totalScore = totalScoreMap.getOrDefault(pid, 100);
            int bestPercent = accepted ? 100 : subs.stream()
                    .filter(s -> !isPendingSubmissionStatus(s.getStatus()))
                    .map(Submission::getScore)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
            bestPercent = Math.max(0, Math.min(100, bestPercent));
            int earnedScore = Math.max(0, Math.min(totalScore, (int) Math.round(bestPercent * totalScore / 100.0)));

            String status = null;
            if (!subs.isEmpty()) {
                if (accepted) {
                    status = "ACCEPTED";
                } else if (pending) {
                    status = "PENDING";
                } else {
                    status = "ATTEMPTED";
                }
            }

            Submission latest = subs.stream()
                    .filter(s -> s.getCreatedAt() != null)
                    .max(Comparator.comparing(Submission::getCreatedAt))
                    .orElse(subs.isEmpty() ? null : subs.get(subs.size() - 1));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", status);
            item.put("accepted", accepted);
            item.put("attempts", subs.size());
            item.put("finalAttempts", finalAttempts);
            item.put("bestScorePercent", bestPercent);
            item.put("earnedScore", earnedScore);
            item.put("totalScore", totalScore);
            item.put("latestStatus", latest == null ? null : latest.getStatus());
            item.put("lastSubmissionAt", latest == null ? null : latest.getCreatedAt());
            progressMap.put(pid, item);
        }
        return progressMap;
    }

    private boolean isPendingSubmissionStatus(String status) {
        return "PENDING".equals(status) || "RUNNING".equals(status);
    }

    private boolean canViewSubmission(Submission submission, User viewer) {
        if (submission.getUserId().equals(viewer.getId())) {
            return true;
        }
        if ("ADMIN".equals(viewer.getRole())) {
            return true;
        }
        if (submission.getContestId() == null) {
            return false;
        }
        Contest contest = contestMapper.selectById(submission.getContestId());
        return contest != null && canViewContestUserSubmissions(contest, viewer, submission.getUserId());
    }

    private boolean canViewContestUserSubmissions(Contest contest, User viewer, Long targetUserId) {
        if (viewer == null || contest == null) {
            return false;
        }
        if (Objects.equals(viewer.getId(), targetUserId)) {
            return true;
        }
        if ("ADMIN".equals(viewer.getRole())) {
            return true;
        }
        if (Objects.equals(contest.getCreatorId(), viewer.getId())) {
            return true;
        }
        return false;
    }

    private Map<String, Object> buildSubmissionDetailData(Submission submission, List<SubmissionResult> testResults) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", submission.getId());
        data.put("problemId", submission.getProblemId());
        data.put("userId", submission.getUserId());
        data.put("language", submission.getLanguage());
        data.put("code", submission.getCode());
        data.put("status", submission.getStatus());
        data.put("score", submission.getScore());
        data.put("executionTime", submission.getExecutionTime());
        data.put("memoryUsed", submission.getMemoryUsed());
        data.put("errorMessage", submission.getErrorMessage());
        data.put("contestId", submission.getContestId());
        data.put("createdAt", submission.getCreatedAt());
        data.put("testResults", buildSafeSubmissionResultData(testResults));
        return data;
    }

    private List<Map<String, Object>> buildSafeSubmissionResultData(List<SubmissionResult> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (SubmissionResult result : testResults) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", result.getId());
            item.put("testCaseId", result.getTestCaseId());
            item.put("status", result.getStatus());
            item.put("executionTime", result.getExecutionTime());
            item.put("memoryUsed", result.getMemoryUsed());
            item.put("errorMessage", result.getErrorMessage());
            item.put("createdAt", result.getCreatedAt());
            data.add(item);
        }
        return data;
    }

    private Map<String, Object> buildContestUserSummary(User user, UserProfile profile) {
        if (user == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("avatar", profile != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()
                ? profile.getAvatarUrl()
                : "/avatars/avatar-1.svg");
        return data;
    }

    private User getUserFromAuth(String auth) {
        try {
            String token = auth.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            return userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, username)
            );
        } catch (Exception e) {
            return null;
        }
    }
}
