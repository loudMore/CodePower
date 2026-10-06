/**
 * 文件说明：管理后台 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.AiModelConfig;
import com.ls.domain.AiModelProvider;
import com.ls.domain.Problem;
import com.ls.domain.ProblemReport;
import com.ls.domain.RoleUpgradeRequest;
import com.ls.domain.Submission;
import com.ls.domain.User;
import com.ls.mapper.AiModelConfigMapper;
import com.ls.mapper.AiModelProviderMapper;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.RoleUpgradeRequestMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.CodeTestService;
import com.ls.service.JudgeQueueExecutor;
import com.ls.service.NotificationService;
import com.ls.service.ProblemReportService;
import com.ls.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.RejectedExecutionException;

/**
 * 管理后台控制器。
 * 负责用户管理、题目治理、反馈处理、AI模型配置、系统统计和评测集群监控。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Set<String> ALLOWED_USER_ROLES = Set.of("NORMAL_USER", "SENIOR_USER", "ADMIN");
    private static final Set<Integer> ALLOWED_USER_STATUS = Set.of(0, 1, 2);
    private static final Set<Integer> ALLOWED_PROBLEM_STATUS = Set.of(0, 1);
    private static final AtomicLong DEMO_TASK_SEQ = new AtomicLong(-1_000_000L);
    private static final int DEFAULT_DEMO_TASK_COUNT = 36;
    private static final int DEFAULT_DEMO_TIME_LIMIT_MS = 1200;
    private static final int DEFAULT_DEMO_MEMORY_LIMIT_KB = 262_144;

    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final RoleUpgradeRequestMapper roleUpgradeRequestMapper;
    private final AiModelProviderMapper aiModelProviderMapper;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final ProblemReportService problemReportService;
    private final CodeTestService codeTestService;
    private final JudgeQueueExecutor judgeQueueExecutor;
    private final NotificationService notificationService;
    private final UserLevelService userLevelService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${ai.api.key:}")
    private String fallbackAiApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** 获取管理后台仪表盘概览数据 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .eq(User::getStatus, 1));
        long totalProblems = problemMapper.selectCount(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getDeleted, 0)
                .eq(Problem::getStatus, 1));
        long todaySubmissions = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().ge(Submission::getCreatedAt, todayStart));
        long todayActiveUsers = submissionMapper.selectList(
                        new LambdaQueryWrapper<Submission>()
                                .select(Submission::getUserId)
                                .ge(Submission::getCreatedAt, todayStart)
                                .groupBy(Submission::getUserId))
                .size();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("totalProblems", totalProblems);
        data.put("todaySubmissions", todaySubmissions);
        data.put("todayActiveUsers", todayActiveUsers);
        long pendingProblemReports = countPendingProblemReports();
        data.put("pendingProblemReports", pendingProblemReports);
        data.put("pendingProblemReviews", pendingProblemReports);
        data.put("pendingUpgradeRequests", countPendingUpgradeRequests());
        return Result.success(data);
    }

    /** 获取平台详细统计数据（用户、题目、提交、通过率等） */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> statsOverview() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0));
        long enabledUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .eq(User::getStatus, 1));
        long disabledUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .eq(User::getStatus, 0));
        long totalProblems = problemMapper.selectCount(new LambdaQueryWrapper<Problem>().eq(Problem::getDeleted, 0));
        long publishedProblems = problemMapper.selectCount(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getDeleted, 0)
                .eq(Problem::getStatus, 1));
        long totalSubmissions = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>());
        long acceptedSubmissions = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, "ACCEPTED"));
        long todaySubmissions = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .ge(Submission::getCreatedAt, todayStart));
        long weeklyActiveUsers = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .select(Submission::getUserId)
                        .ge(Submission::getCreatedAt, weekStart)
                        .groupBy(Submission::getUserId))
                .size();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("enabledUsers", enabledUsers);
        data.put("disabledUsers", disabledUsers);
        data.put("totalProblems", totalProblems);
        data.put("publishedProblems", publishedProblems);
        long pendingProblemReports = countPendingProblemReports();
        data.put("pendingProblemReports", pendingProblemReports);
        data.put("pendingProblemReviews", pendingProblemReports);
        data.put("pendingUpgradeRequests", countPendingUpgradeRequests());
        data.put("totalSubmissions", totalSubmissions);
        data.put("acceptedSubmissions", acceptedSubmissions);
        data.put("todaySubmissions", todaySubmissions);
        data.put("weeklyActiveUsers", weeklyActiveUsers);
        data.put("acceptanceRate", totalSubmissions > 0 ? acceptedSubmissions * 100 / totalSubmissions : 0);
        return Result.success(data);
    }

    /** 分页查询用户列表，支持关键词和角色筛选 */
    @GetMapping("/users")
    public Result<IPage<User>> listUsers(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(User::getUsername, keyword)
                        .or().like(User::getEmail, keyword))
                .eq(role != null && !role.isBlank(), User::getRole, role)
                .orderByDesc(User::getCreatedAt);
        IPage<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    /** 修改指定用户的角色 */
    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = getUserOrThrow(id);
        String role = body.get("role");
        if (role == null || !ALLOWED_USER_ROLES.contains(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的角色类型");
        }
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        notificationService.createNotification(id, "ROLE_UPDATED",
                "账号角色已更新",
                "管理员已将你的角色调整为「" + role + "」。如有疑问请联系管理员。",
                id);
        return Result.success();
    }

    /** 修改指定用户的账号状态（启用/禁用） */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = getUserOrThrow(id);
        Integer status = body.get("status");
        if (status == null || !ALLOWED_USER_STATUS.contains(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的用户状态");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        notificationService.createNotification(id, "ACCOUNT_STATUS_UPDATED",
                "账号状态已更新",
                status == 1 ? "你的账号已恢复正常使用。" : "你的账号状态已被管理员调整，请联系管理员了解详情。",
                id);
        return Result.success();
    }

    /** 分页查询题目列表，支持关键词和状态筛选 */
    @GetMapping("/problems")
    public Result<IPage<Problem>> listProblems(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .eq(Problem::getDeleted, 0)
                .like(keyword != null && !keyword.isBlank(), Problem::getTitle, keyword)
                .eq(status != null, Problem::getStatus, status)
                .orderByDesc(Problem::getCreatedAt);
        return Result.success(problemMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 获取已下线题目列表 */
    @GetMapping("/problems/review")
    public Result<IPage<Problem>> listProblemsForReview(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .eq(Problem::getDeleted, 0)
                .eq(Problem::getStatus, 0)
                .like(keyword != null && !keyword.isBlank(), Problem::getTitle, keyword)
                .orderByDesc(Problem::getCreatedAt);
        return Result.success(problemMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 更新题目发布状态（上架/下架） */
    @PutMapping("/problems/{id}/status")
    public Result<Void> updateProblemStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null || Integer.valueOf(1).equals(problem.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        Integer status = body.get("status");
        if (status == null || !ALLOWED_PROBLEM_STATUS.contains(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的题目状态");
        }
        problemMapper.updateStatus(id, status);
        return Result.success();
    }

    /** 分页查询题目反馈处理队列 */
    @GetMapping("/problem-reports")
    public Result<IPage<ProblemReport>> listProblemReports(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String keyword) {
        return Result.success(problemReportService.listForAdmin(page, size, status, keyword));
    }

    /** 处理题目反馈（标记已处理或忽略） */
    @PutMapping("/problem-reports/{id}/handle")
    public Result<ProblemReport> handleProblemReport(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (!"RESOLVED".equals(status) && !"DISMISSED".equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的反馈处理状态");
        }
        String reply = body.get("reply");
        return Result.success(problemReportService.handleReport(id, status, reply, null));
    }

    /** 获取近N天的每日提交量统计 */
    @GetMapping("/stats/submissions")
    public Result<Map<String, Object>> submissionStats(@RequestParam(defaultValue = "7") int days) {
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.toString());
            long count = submissionMapper.selectCount(
                    new LambdaQueryWrapper<Submission>()
                            .ge(Submission::getCreatedAt, date.atStartOfDay())
                            .lt(Submission::getCreatedAt, date.plusDays(1).atStartOfDay()));
            counts.add(count);
        }
        return Result.success(Map.of("dates", dates, "counts", counts));
    }

    /** 获取评测集群健康状态、后端队列状态和最近提交，供后台实时监控。 */
    @GetMapping("/judge-cluster")
    public Result<Map<String, Object>> judgeCluster() {
        Map<String, Object> data = new LinkedHashMap<>(codeTestService.getJudgeClusterStatus());
        long pending = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, "PENDING"));
        long running = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, "RUNNING"));
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayTotal = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .ge(Submission::getCreatedAt, todayStart));
        long todayAccepted = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .ge(Submission::getCreatedAt, todayStart)
                .eq(Submission::getStatus, "ACCEPTED"));
        data.put("pendingSubmissions", pending);
        data.put("runningSubmissions", running);
        data.put("queuedSubmissions", pending + running);
        data.put("executor", judgeQueueExecutor.getStatus());
        data.put("recentSubmissions", latestJudgeSubmissions());
        data.put("todaySubmissions", todayTotal);
        data.put("todayAcceptedSubmissions", todayAccepted);
        data.put("checkedAt", LocalDateTime.now());
        return Result.success(data);
    }

    /**
     * 管理员演示压测入口：一次性向统一评测队列注入一批真实 Judge0 请求。
     * 该接口不会写入正常提交历史，仅用于后台演示和队列观察。
     */
    @PostMapping("/judge-cluster/demo-load")
    public Result<Map<String, Object>> judgeClusterDemoLoad(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> request = body == null ? Map.of() : body;
        int count = clampInt(request.get("count"), DEFAULT_DEMO_TASK_COUNT, 1, 100);
        String language = normalizeDemoLanguage(request.get("language"));
        int languageId = codeTestService.getJudge0LanguageId(language);
        int timeLimitMs = clampInt(request.get("timeLimitMs"), DEFAULT_DEMO_TIME_LIMIT_MS, 500, 5000);
        int memoryLimitKb = clampInt(request.get("memoryLimitKb"), DEFAULT_DEMO_MEMORY_LIMIT_KB, 65_536, 524_288);
        String demoCode = buildDemoJudgeCode(language);
        String encodedCode = Base64.getEncoder().encodeToString(demoCode.getBytes(StandardCharsets.UTF_8));

        List<Long> taskIds = new ArrayList<>(count);
        int accepted = 0;
        int rejected = 0;
        for (int i = 0; i < count; i++) {
            Long taskId = DEMO_TASK_SEQ.getAndDecrement();
            try {
                judgeQueueExecutor.submit(taskId, () -> {
                    try {
                        codeTestService.executeCodeWithJudge0(encodedCode, languageId, null, null, timeLimitMs, memoryLimitKb);
                    } catch (Exception e) {
                        log.warn("Judge cluster demo task failed, taskId={}, message={}", taskId, e.getMessage());
                    }
                });
                taskIds.add(taskId);
                accepted++;
            } catch (RejectedExecutionException e) {
                rejected++;
                log.warn("Judge cluster demo task rejected, taskId={}, queueBusy={}", taskId, e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestedCount", count);
        result.put("acceptedCount", accepted);
        result.put("rejectedCount", rejected);
        result.put("language", language);
        result.put("timeLimitMs", timeLimitMs);
        result.put("memoryLimitKb", memoryLimitKb);
        result.put("taskIds", taskIds);
        result.put("queueStatus", judgeQueueExecutor.getStatus());
        result.put("demoCode", demoCode);
        result.put("checkedAt", LocalDateTime.now());
        return Result.success(result);
    }

    /** 查询最近评测提交的关键字段，避免后台列表额外拉取完整代码造成性能浪费。 */
    private List<Map<String, Object>> latestJudgeSubmissions() {
        List<Submission> rows = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .select(Submission::getId, Submission::getProblemId, Submission::getUserId,
                        Submission::getLanguage, Submission::getStatus, Submission::getScore,
                        Submission::getContestId, Submission::getCreatedAt, Submission::getQueuedAt,
                        Submission::getStartedAt, Submission::getFinishedAt, Submission::getJudgeNode,
                        Submission::getQueueWaitMs, Submission::getJudgeDurationMs)
                .orderByDesc(Submission::getCreatedAt)
                .orderByDesc(Submission::getId)
                .last("LIMIT 12"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Submission row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("problemId", row.getProblemId());
            item.put("userId", row.getUserId());
            item.put("language", row.getLanguage());
            item.put("status", row.getStatus());
            item.put("score", row.getScore());
            item.put("contestId", row.getContestId());
            item.put("createdAt", row.getCreatedAt());
            item.put("queuedAt", row.getQueuedAt());
            item.put("startedAt", row.getStartedAt());
            item.put("finishedAt", row.getFinishedAt());
            item.put("judgeNode", row.getJudgeNode());
            item.put("queueWaitMs", row.getQueueWaitMs());
            item.put("judgeDurationMs", row.getJudgeDurationMs());
            result.add(item);
        }
        return result;
    }

    private int clampInt(Object raw, int defaultValue, int min, int max) {
        int value = defaultValue;
        if (raw instanceof Number number) {
            value = number.intValue();
        } else if (raw instanceof String text && !text.isBlank()) {
            try {
                value = Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                value = defaultValue;
            }
        }
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private String normalizeDemoLanguage(Object raw) {
        String language = raw == null ? "cpp" : raw.toString().trim().toLowerCase(Locale.ROOT);
        return switch (language) {
            case "python", "py" -> "python";
            case "java" -> "java";
            case "c" -> "c";
            case "cpp", "c++", "cc" -> "cpp";
            default -> "cpp";
        };
    }

    private String buildDemoJudgeCode(String language) {
        return switch (language) {
            case "python" -> """
                    while True:
                        pass
                    """;
            case "java" -> """
                    public class Main {
                        public static void main(String[] args) {
                            while (true) {
                            }
                        }
                    }
                    """;
            case "c" -> """
                    #include <stdio.h>
                    int main(void) {
                        volatile long long x = 0;
                        while (1) {
                            x++;
                        }
                        return 0;
                    }
                    """;
            default -> """
                    #include <bits/stdc++.h>
                    using namespace std;
                    int main() {
                        volatile long long x = 0;
                        while (true) {
                            x++;
                        }
                        return 0;
                    }
                    """;
        };
    }

    /** 查询角色升级申请列表，可按状态筛选 */
    @GetMapping("/upgrade-requests")
    public Result<List<RoleUpgradeRequest>> listUpgradeRequests(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            return Result.success(roleUpgradeRequestMapper.selectWithUserInfo(status));
        }
        return Result.success(roleUpgradeRequestMapper.selectAllWithUserInfo());
    }

    /** 审核角色升级申请（批准或拒绝） */
    @PutMapping("/upgrade-requests/{id}")
    public Result<Void> reviewUpgradeRequest(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        String action = body.get("action");
        String comment = body.get("comment");
        String reviewerIdText = body.get("reviewerId");
        Long reviewerId = null;
        if (reviewerIdText != null && !reviewerIdText.isBlank()) {
            try {
                reviewerId = Long.parseLong(reviewerIdText);
            } catch (NumberFormatException e) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "reviewerId 参数格式错误");
            }
        }

        RoleUpgradeRequest req = roleUpgradeRequestMapper.selectById(id);
        if (req == null) throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        if (!"PENDING".equals(req.getStatus())) {
            return Result.error("该申请已被处理");
        }
        req.setReviewerId(reviewerId);
        req.setUpdatedAt(LocalDateTime.now());
        if ("approve".equals(action)) {
            req.setStatus("APPROVED");
            req.setReviewComment(comment);
            roleUpgradeRequestMapper.updateById(req);
            User user = userMapper.selectById(req.getUserId());
            if (user != null) {
                user.setRole(req.getRequestedRole());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
            }
            notificationService.createNotification(req.getUserId(), "UPGRADE_APPROVED",
                    "角色升级申请已通过",
                    "恭喜！你的角色升级申请已通过，当前角色：" + req.getRequestedRole() + "。享受更多权限吧！",
                    id);
        } else if ("reject".equals(action)) {
            req.setStatus("REJECTED");
            req.setReviewComment(comment);
            roleUpgradeRequestMapper.updateById(req);
            notificationService.createNotification(req.getUserId(), "UPGRADE_REJECTED",
                    "角色升级申请被拒绝",
                    "你的角色升级申请未通过。" + (comment != null ? "原因：" + comment : "如有疑问请联系管理员。"),
                    id);
        } else {
            return Result.error("无效的操作");
        }
        return Result.success();
    }

    /** 重置指定用户的登录密码 */
    @PutMapping("/users/{id}/password")
    public Result<Void> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = getUserOrThrow(id);
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("密码长度不能小于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        notificationService.createNotification(id, "PASSWORD_RESET",
                "账号密码已重置",
                "你的账号密码已被管理员重置，请尽快使用新密码登录并修改为个人密码。",
                id);
        return Result.success();
    }

    /** 调整指定用户的经验值（可增可减） */
    @PutMapping("/users/{id}/exp")
    public Result<Void> adjustUserExp(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = getUserOrThrow(id);
        Integer amount = body.get("amount");
        if (amount == null || amount == 0) return Result.error("经验值不能为0");
        if (amount > 0) {
            userLevelService.addExp(id, amount);
        } else {
            user.setExp(Math.max(0, user.getExp() + amount));
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
        return Result.success();
    }

    /** 调整指定用户的AI积分（可增可减） */
    @PutMapping("/users/{id}/ai-points")
    public Result<Void> adjustUserAiPoints(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = getUserOrThrow(id);
        Integer amount = body.get("amount");
        if (amount == null || amount == 0) return Result.error("积分不能为0");
        if (amount > 0) {
            userLevelService.addAiBonus(id, amount);
        } else {
            int current = user.getBonusAiPoints() != null ? user.getBonusAiPoints() : 0;
            user.setBonusAiPoints(Math.max(0, current + amount));
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
        return Result.success();
    }

    /** 获取AI模型提供商与模型配置 */
    @GetMapping("/ai/providers")
    public Result<List<Map<String, Object>>> listAiProviders() {
        List<AiModelProvider> providers = aiModelProviderMapper.selectList(new LambdaQueryWrapper<AiModelProvider>()
                .orderByAsc(AiModelProvider::getSortOrder)
                .orderByAsc(AiModelProvider::getId));
        List<AiModelConfig> models = aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfig>()
                .orderByAsc(AiModelConfig::getSortOrder)
                .orderByAsc(AiModelConfig::getId));
        Map<Long, List<AiModelConfig>> modelsByProvider = models.stream()
                .collect(Collectors.groupingBy(AiModelConfig::getProviderId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (AiModelProvider provider : providers) {
            Map<String, Object> item = providerToMap(provider);
            item.put("models", modelsByProvider.getOrDefault(provider.getId(), List.of())
                    .stream().map(this::modelToMap).toList());
            item.put("modelCount", modelsByProvider.getOrDefault(provider.getId(), List.of()).size());
            result.add(item);
        }
        return Result.success(result);
    }

    /** 新增AI模型提供商 */
    @PostMapping("/ai/providers")
    public Result<AiModelProvider> createAiProvider(@RequestBody Map<String, Object> body) {
        String providerKey = stringValue(body.get("providerKey")).trim();
        if (providerKey.isBlank() || !providerKey.matches("[a-zA-Z0-9_-]{2,64}")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提供商ID只能包含字母、数字、下划线或横线，长度2-64");
        }
        AiModelProvider exists = aiModelProviderMapper.selectOne(new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getProviderKey, providerKey)
                .last("LIMIT 1"));
        if (exists != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提供商ID已存在");
        }
        AiModelProvider provider = new AiModelProvider();
        fillProvider(provider, body, true);
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());
        aiModelProviderMapper.insert(provider);
        provider.setApiKey(null);
        return Result.success(provider);
    }

    /** 更新AI模型提供商 */
    @PutMapping("/ai/providers/{id}")
    public Result<AiModelProvider> updateAiProvider(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AiModelProvider provider = getAiProviderOrThrow(id);
        fillProvider(provider, body, false);
        provider.setUpdatedAt(LocalDateTime.now());
        aiModelProviderMapper.updateById(provider);
        provider.setApiKey(null);
        return Result.success(provider);
    }

    /** 删除AI模型提供商 */
    @DeleteMapping("/ai/providers/{id}")
    public Result<Void> deleteAiProvider(@PathVariable Long id) {
        getAiProviderOrThrow(id);
        aiModelProviderMapper.deleteById(id);
        return Result.success();
    }

    /** 添加一个模型到指定提供商 */
    @PostMapping("/ai/providers/{providerId}/models")
    public Result<AiModelConfig> createAiModel(@PathVariable Long providerId, @RequestBody Map<String, Object> body) {
        getAiProviderOrThrow(providerId);
        String modelId = stringValue(body.get("modelId")).trim();
        if (modelId.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "模型ID不能为空");
        }
        AiModelConfig exists = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getProviderId, providerId)
                .eq(AiModelConfig::getModelId, modelId)
                .last("LIMIT 1"));
        if (exists != null) {
            fillModel(exists, body, false);
            exists.setUpdatedAt(LocalDateTime.now());
            aiModelConfigMapper.updateById(exists);
            return Result.success(exists);
        }
        AiModelConfig model = new AiModelConfig();
        model.setProviderId(providerId);
        fillModel(model, body, true);
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        aiModelConfigMapper.insert(model);
        return Result.success(model);
    }

    /** 更新AI模型配置 */
    @PutMapping("/ai/models/{id}")
    public Result<AiModelConfig> updateAiModel(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AiModelConfig model = getAiModelOrThrow(id);
        fillModel(model, body, false);
        model.setUpdatedAt(LocalDateTime.now());
        aiModelConfigMapper.updateById(model);
        return Result.success(model);
    }

    /** 删除AI模型配置 */
    @DeleteMapping("/ai/models/{id}")
    public Result<Void> deleteAiModel(@PathVariable Long id) {
        getAiModelOrThrow(id);
        aiModelConfigMapper.deleteById(id);
        return Result.success();
    }

    /** 从远程提供商拉取模型列表（OpenAI兼容 / Anthropic models接口） */
    @PostMapping("/ai/providers/{id}/fetch-models")
    public Result<List<Map<String, Object>>> fetchRemoteModels(@PathVariable Long id) {
        AiModelProvider provider = getAiProviderOrThrow(id);
        try {
            String apiKey = effectiveProviderApiKey(provider);
            String baseUrl = provider.getBaseUrl().replaceAll("/$", "");
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/models"))
                    .GET()
                    .timeout(Duration.ofSeconds(30));
            if ("ANTHROPIC".equalsIgnoreCase(provider.getApiType())) {
                builder.header("x-api-key", apiKey);
                builder.header("anthropic-version", "2023-06-01");
            } else {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("拉取远程模型失败: provider={}, status={}, body={}", provider.getProviderKey(), response.statusCode(), response.body());
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "拉取模型失败：" + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.has("data") ? root.get("data") : root;
            List<Map<String, Object>> models = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode item : data) {
                    String modelId = item.has("id") ? item.get("id").asText() : item.asText();
                    if (modelId == null || modelId.isBlank()) {
                        continue;
                    }
                    models.add(Map.of(
                            "modelId", modelId,
                            "displayName", modelId,
                            "providerId", provider.getId()
                    ));
                }
            }
            return Result.success(models);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("拉取远程AI模型异常: provider={}", provider.getProviderKey(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "拉取模型失败：" + e.getMessage());
        }
    }

    private User getUserOrThrow(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private AiModelProvider getAiProviderOrThrow(Long id) {
        AiModelProvider provider = aiModelProviderMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI提供商不存在");
        }
        return provider;
    }

    private AiModelConfig getAiModelOrThrow(Long id) {
        AiModelConfig model = aiModelConfigMapper.selectById(id);
        if (model == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI模型不存在");
        }
        return model;
    }

    private void fillProvider(AiModelProvider provider, Map<String, Object> body, boolean creating) {
        if (creating) {
            provider.setProviderKey(stringValue(body.get("providerKey")).trim());
        }
        provider.setName(defaultIfBlank(stringValue(body.get("name")), provider.getProviderKey()));
        provider.setApiType(defaultIfBlank(stringValue(body.get("apiType")), "OPENAI_COMPATIBLE"));
        provider.setBaseUrl(defaultIfBlank(stringValue(body.get("baseUrl")), "https://api.openai.com/v1").replaceAll("/$", ""));
        String apiKey = stringValue(body.get("apiKey"));
        if (creating || !apiKey.isBlank()) {
            provider.setApiKey(apiKey);
        }
        provider.setEnabled(boolInt(body.get("enabled"), 1));
        provider.setSortOrder(intValue(body.get("sortOrder"), provider.getSortOrder() == null ? 0 : provider.getSortOrder()));
    }

    private void fillModel(AiModelConfig model, Map<String, Object> body, boolean creating) {
        if (creating || body.containsKey("modelId")) {
            model.setModelId(stringValue(body.get("modelId")).trim());
        }
        model.setDisplayName(defaultIfBlank(stringValue(body.get("displayName")), model.getModelId()));
        model.setEnabled(boolInt(body.get("enabled"), model.getEnabled() == null ? 1 : model.getEnabled()));
        model.setCostMultiplier(doubleValue(body.get("costMultiplier"), model.getCostMultiplier() == null ? 1.0 : model.getCostMultiplier()));
        model.setContextWindow(intValue(body.get("contextWindow"), model.getContextWindow()));
        model.setCapabilityTags(stringValue(body.get("capabilityTags")));
        model.setSortOrder(intValue(body.get("sortOrder"), model.getSortOrder() == null ? 0 : model.getSortOrder()));
    }

    private Map<String, Object> providerToMap(AiModelProvider provider) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", provider.getId());
        map.put("providerKey", provider.getProviderKey());
        map.put("name", provider.getName());
        map.put("apiType", provider.getApiType());
        map.put("baseUrl", provider.getBaseUrl());
        map.put("enabled", provider.getEnabled());
        map.put("sortOrder", provider.getSortOrder());
        map.put("hasApiKey", provider.getApiKey() != null && !provider.getApiKey().isBlank());
        map.put("apiKeyMasked", maskKey(provider.getApiKey()));
        map.put("createdAt", provider.getCreatedAt());
        map.put("updatedAt", provider.getUpdatedAt());
        return map;
    }

    private Map<String, Object> modelToMap(AiModelConfig model) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", model.getId());
        map.put("providerId", model.getProviderId());
        map.put("modelId", model.getModelId());
        map.put("displayName", model.getDisplayName());
        map.put("enabled", model.getEnabled());
        map.put("costMultiplier", model.getCostMultiplier());
        map.put("contextWindow", model.getContextWindow());
        map.put("capabilityTags", model.getCapabilityTags());
        map.put("sortOrder", model.getSortOrder());
        return map;
    }

    private String effectiveProviderApiKey(AiModelProvider provider) {
        if (provider.getApiKey() != null && !provider.getApiKey().isBlank()) {
            return provider.getApiKey();
        }
        if ("siliconflow".equals(provider.getProviderKey())) {
            return fallbackAiApiKey;
        }
        return "";
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.length() <= 10) {
            return "******";
        }
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Double doubleValue(Object value, Double fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private Integer boolInt(Object value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        if (value instanceof Number number) {
            return number.intValue() == 0 ? 0 : 1;
        }
        String text = String.valueOf(value);
        return "false".equalsIgnoreCase(text) || "0".equals(text) ? 0 : 1;
    }

    private long countPendingProblemReports() {
        return problemReportService.countPendingReports();
    }

    private long countPendingUpgradeRequests() {
        return roleUpgradeRequestMapper.selectCount(new LambdaQueryWrapper<RoleUpgradeRequest>()
                .eq(RoleUpgradeRequest::getStatus, "PENDING"));
    }
}
