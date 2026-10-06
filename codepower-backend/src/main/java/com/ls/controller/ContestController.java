/**
 * 文件说明：竞赛 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.Contest;
import com.ls.domain.ContestProblem;
import com.ls.domain.ContestRegistration;
import com.ls.domain.Submission;
import com.ls.domain.SubmissionResult;
import com.ls.domain.User;
import com.ls.mapper.ContestRegistrationMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.SubmissionResultMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 竞赛控制器。
 * 负责竞赛列表、报名、题目导入、排名、成绩导出和前端异常排查接口。
 */
@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;
    private final UserMapper userMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionResultMapper submissionResultMapper;
    private final ContestRegistrationMapper contestRegistrationMapper;

    /** 获取竞赛列表（支持状态、类型筛选） */
    @GetMapping
    public Result<IPage<Contest>> listContests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long viewerId = userDetails != null ? getUserId(userDetails) : null;
        return Result.success(contestService.listContests(status, type, page, size, viewerId));
    }

    /** 获取我参加或创建的竞赛列表 */
    @GetMapping("/my")
    public Result<IPage<Contest>> listMyContests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "joined") String filter,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(userDetails);
        return Result.success(contestService.listMyContests(userId, filter, status, type, page, size));
    }

    /** 获取竞赛详情 */
    @GetMapping("/{id}")
    public Result<Contest> getContestDetail(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? getUserId(userDetails) : null;
        return Result.success(contestService.getContestDetail(id, userId));
    }

    /** 报名参加竞赛（可选密码验证） */
    @PostMapping("/{id}/register")
    public Result<Void> register(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long id,
                                  @RequestBody(required = false) Map<String, String> body) {
        Long userId = getUserId(userDetails);
        String password = body != null ? body.get("password") : null;
        contestService.register(userId, id, password);
        return Result.success();
    }

    /** 获取竞赛排名 */
    @GetMapping("/{id}/ranking")
    public Result<List<Map<String, Object>>> getRanking(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserDetails userDetails,
                                                        @RequestParam(defaultValue = "active") String filter) {
        Long userId = userDetails != null ? getUserId(userDetails) : null;
        return Result.success(contestService.getRanking(id, userId, filter));
    }

    /** 获取竞赛题目列表 */
    @GetMapping("/{id}/problems")
    public Result<List<ContestProblem>> getProblems(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = null;
        String userRole = null;
        if (userDetails != null) {
            User viewer = getCurrentUser(userDetails);
            userId = viewer.getId();
            userRole = viewer.getRole();
        }
        Contest contest = contestService.getContestDetail(id, userId);
        boolean canManage = userId != null
                && (Objects.equals(contest.getCreatorId(), userId) || "ADMIN".equals(userRole));
        if (!canManage && !Boolean.TRUE.equals(contest.getCanAccessWorkspace())) {
            return Result.success(List.of());
        }
        return Result.success(contestService.getContestProblems(id));
    }

    /** 导出竞赛成绩（创建者和管理员可用） */
    @GetMapping("/{id}/results/export")
    public ResponseEntity<byte[]> exportContestResults(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User viewer = getCurrentUser(userDetails);
        ContestResultData data = loadContestResultData(id, viewer);
        byte[] body = buildContestResultWorkbook(data);
        String filename = "contest-" + id + "-results.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /** 在线查看竞赛异常排查线索（创建者和管理员可用） */
    @GetMapping("/{id}/audit")
    public Result<Map<String, Object>> getContestAudit(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User viewer = getCurrentUser(userDetails);
        ContestResultData data = loadContestResultData(id, viewer);
        List<Map<String, Object>> auditItems = buildAuditItems(data);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", buildAuditSummary(auditItems, data));
        payload.put("items", auditItems);
        return Result.success(payload);
    }

    /** 创建竞赛 */
    @PostMapping
    public Result<Contest> createContest(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody Contest contest) {
        User creator = getCurrentUser(userDetails);
        if (!canCreateContest(creator)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账号没有创建竞赛的权限");
        }
        contest.setCreatorId(creator.getId());
        return Result.success(contestService.createContest(contest));
    }

    /** 更新竞赛信息 */
    @PutMapping("/{id}")
    public Result<Contest> updateContest(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody Contest contest) {
        assertContestOwnerOrAdmin(id, userDetails);
        return Result.success(contestService.updateContest(id, contest));
    }

    /** 删除竞赛（创建者或管理员可用） */
    @DeleteMapping("/{id}")
    public Result<Void> deleteContest(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        assertContestOwnerOrAdmin(id, userDetails);
        contestService.deleteContest(id);
        return Result.success();
    }

    /** 批量添加竞赛题目 */
    @PostMapping("/{id}/problems")
    public Result<Void> addProblems(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     @RequestBody List<ContestProblem> problems) {
        User viewer = getCurrentUser(userDetails);
        assertContestOwnerOrAdmin(id, viewer);
        contestService.addProblems(id, problems, viewer.getId(), viewer.getRole());
        return Result.success();
    }

    /** 批量移除竞赛题目 */
    @DeleteMapping("/{id}/problems")
    public Result<Void> removeProblems(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody Map<String, List<Long>> body) {
        assertContestOwnerOrAdmin(id, userDetails);
        contestService.removeProblems(id, body.get("problemIds"));
        return Result.success();
    }

    /** 从题目集批量导入题目到竞赛 */
    @PostMapping("/{id}/import-from-set/{setId}")
    public Result<Map<String, Object>> importFromProblemSet(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long setId) {
        User viewer = getCurrentUser(userDetails);
        assertContestOwnerOrAdmin(id, viewer);
        int count = contestService.importFromProblemSet(id, setId, viewer.getId(), viewer.getRole());
        return Result.success(Map.of("imported", count));
    }

    /** 更新竞赛状态 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      @RequestBody Map<String, String> body) {
        assertContestOwnerOrAdmin(id, userDetails);
        contestService.updateContestStatus(id, body.get("status"));
        return Result.success();
    }

    /** 通过邀请码快速加入竞赛 */
    @PostMapping("/join")
    public Result<Contest> joinByInviteCode(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody Map<String, String> body) {
        Long userId = getUserId(userDetails);
        String inviteCode = body.get("inviteCode");
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀请码不能为空");
        }
        return Result.success(contestService.joinByInviteCode(userId, inviteCode.trim().toUpperCase()));
    }

    private Long getUserId(UserDetails userDetails) {
        return getCurrentUser(userDetails).getId();
    }

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return user;
    }

    private boolean canCreateContest(User user) {
        return user != null && ("ADMIN".equals(user.getRole()) || "SENIOR_USER".equals(user.getRole()));
    }

    private Contest assertContestOwnerOrAdmin(Long contestId, UserDetails userDetails) {
        User viewer = getCurrentUser(userDetails);
        return assertContestOwnerOrAdmin(contestId, viewer);
    }

    private Contest assertContestOwnerOrAdmin(Long contestId, User viewer) {
        Contest contest = contestService.getContestDetail(contestId, viewer.getId());
        if (!Objects.equals(contest.getCreatorId(), viewer.getId()) && !"ADMIN".equals(viewer.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有竞赛创建者或管理员可以管理该竞赛");
        }
        return contest;
    }

    /** 汇总成绩导出和异常排查所需的竞赛、题目、排名、提交、测试点和用户数据。 */
    private ContestResultData loadContestResultData(Long contestId, User viewer) {
        Contest contest = assertContestOwnerOrAdmin(contestId, viewer);
        List<ContestProblem> problems = contestService.getContestProblems(contestId);
        List<Map<String, Object>> ranking = contestService.getRanking(contestId, viewer.getId(), "all");
        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getContestId, contestId)
                        .orderByAsc(Submission::getCreatedAt));
        List<Long> submissionIds = submissions.stream().map(Submission::getId).filter(Objects::nonNull).toList();
        List<SubmissionResult> submissionResults = submissionIds.isEmpty()
                ? List.of()
                : submissionResultMapper.selectList(
                        new LambdaQueryWrapper<SubmissionResult>()
                                .in(SubmissionResult::getSubmissionId, submissionIds)
                                .orderByAsc(SubmissionResult::getSubmissionId)
                                .orderByAsc(SubmissionResult::getTestCaseId));
        List<ContestRegistration> registrations = contestRegistrationMapper.selectList(
                new LambdaQueryWrapper<ContestRegistration>()
                        .eq(ContestRegistration::getContestId, contestId)
                        .orderByAsc(ContestRegistration::getRegisteredAt));
        Set<Long> userIds = new HashSet<>();
        registrations.forEach(reg -> userIds.add(reg.getUserId()));
        submissions.forEach(submission -> userIds.add(submission.getUserId()));
        if (contest.getCreatorId() != null) {
            userIds.add(contest.getCreatorId());
        }
        Map<Long, User> users = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Map<String, Object>> problemStats = buildProblemStats(problems, ranking);
        Map<Long, ContestProblem> problemMap = problems.stream()
                .collect(Collectors.toMap(ContestProblem::getProblemId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, ContestRegistration> registrationMap = registrations.stream()
                .collect(Collectors.toMap(ContestRegistration::getUserId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<Submission>> submissionsByUser = submissions.stream()
                .collect(Collectors.groupingBy(Submission::getUserId, LinkedHashMap::new, Collectors.toList()));
        return new ContestResultData(contest, problems, ranking, submissions, submissionResults, registrations,
                users, problemStats, problemMap, registrationMap, submissionsByUser);
    }

    @SuppressWarnings("unchecked")
    /** 生成多工作表 Excel，包含成绩、题目统计、提交历史、代码留痕、测试点和复核线索。 */
    private byte[] buildContestResultWorkbook(ContestResultData data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Map<String, CellStyle> styles = createWorkbookStyles(workbook);
            List<Map<String, Object>> auditItems = buildAuditItems(data);

            writeOverviewSheet(workbook, styles, data.contest(), data.problems(), data.ranking(), data.problemStats(), data.submissions(), auditItems);
            writeScoreSheet(workbook, styles, data.contest(), data.problems(), data.ranking(), data.users(), data.registrationMap(), data.submissionsByUser());
            writeProblemStatsSheet(workbook, styles, data.problemStats());
            writeSubmissionHistorySheet(workbook, styles, data.submissions(), data.users(), data.problemMap());
            writeCodeTraceSheet(workbook, styles, data.submissions(), data.users(), data.problemMap());
            writeTestResultSheet(workbook, styles, data.submissionResults(), data.submissions(), data.users(), data.problemMap());
            writeAuditSheet(workbook, styles, auditItems);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "生成成绩 Excel 失败");
        }
    }

    private record ContestResultData(Contest contest,
                                     List<ContestProblem> problems,
                                     List<Map<String, Object>> ranking,
                                     List<Submission> submissions,
                                     List<SubmissionResult> submissionResults,
                                     List<ContestRegistration> registrations,
                                     Map<Long, User> users,
                                     List<Map<String, Object>> problemStats,
                                     Map<Long, ContestProblem> problemMap,
                                     Map<Long, ContestRegistration> registrationMap,
                                     Map<Long, List<Submission>> submissionsByUser) {
    }

    private Map<String, CellStyle> createWorkbookStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        styles.put("title", title);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("header", header);

        Font warningFont = workbook.createFont();
        warningFont.setColor(IndexedColors.DARK_RED.getIndex());
        CellStyle warning = workbook.createCellStyle();
        warning.setFont(warningFont);
        styles.put("warning", warning);

        CellStyle normal = workbook.createCellStyle();
        styles.put("normal", normal);
        return styles;
    }

    /** 写入导出文件的概览工作表，帮助教师先看整体情况。 */
    private void writeOverviewSheet(Workbook workbook,
                                    Map<String, CellStyle> styles,
                                    Contest contest,
                                    List<ContestProblem> problems,
                                    List<Map<String, Object>> ranking,
                                    List<Map<String, Object>> problemStats,
                                    List<Submission> submissions,
                                    List<Map<String, Object>> auditItems) {
        Sheet sheet = workbook.createSheet("总览");
        int row = 0;
        row = writeRow(sheet, row, styles.get("title"), "竞赛成绩导出总览");
        row++;
        row = writeRow(sheet, row, styles.get("header"), "字段", "值");
        row = writeRow(sheet, row, null, "竞赛名称", contest.getTitle());
        row = writeRow(sheet, row, null, "竞赛类型", typeText(contest.getType()));
        row = writeRow(sheet, row, null, "竞赛状态", statusText(contest.getStatus()));
        row = writeRow(sheet, row, null, "是否官方", Objects.equals(contest.getIsOfficial(), 1) ? "是" : "否");
        row = writeRow(sheet, row, null, "开始时间", contest.getStartTime());
        row = writeRow(sheet, row, null, "结束时间", isOpenEndedContest(contest) ? "长期开放" : contest.getEndTime());
        row = writeRow(sheet, row, null, "考试时长(分钟)", contest.getDurationMinutes() != null && contest.getDurationMinutes() > 0 ? contest.getDurationMinutes() : "不限制");
        row = writeRow(sheet, row, null, "题目数量", problems.size());
        row = writeRow(sheet, row, null, "导出时间", LocalDateTime.now());

        int participantCount = ranking.size();
        long enteredCount = ranking.stream().filter(rowData -> Boolean.TRUE.equals(rowData.get("entered"))).count();
        long submittedCount = ranking.stream().filter(rowData -> Boolean.TRUE.equals(rowData.get("hasSubmission"))).count();
        long acceptedUserCount = ranking.stream().filter(rowData -> numberValue(rowData.get("solvedCount")) > 0).count();
        int totalAttempts = submissions.size();
        int highestScore = ranking.stream().mapToInt(rowData -> numberValue(rowData.get("totalScore"))).max().orElse(0);
        int averageScore = participantCount == 0 ? 0
                : (int) Math.round(ranking.stream().mapToInt(rowData -> numberValue(rowData.get("totalScore"))).average().orElse(0));

        row++;
        row = writeRow(sheet, row, styles.get("header"), "指标", "数值");
        row = writeRow(sheet, row, null, "报名人数", participantCount);
        row = writeRow(sheet, row, null, "入场人数", enteredCount);
        row = writeRow(sheet, row, null, "有提交人数", submittedCount);
        row = writeRow(sheet, row, null, "有通过人数", acceptedUserCount);
        row = writeRow(sheet, row, null, "总提交次数", totalAttempts);
        row = writeRow(sheet, row, null, "最高总分", highestScore);
        row = writeRow(sheet, row, null, "平均总分", averageScore);
        row = writeRow(sheet, row, null, "需人工关注题目数", problemStats.stream().filter(stat -> numberValue(stat.get("attemptCount")) > 0 && numberValue(stat.get("acceptedCount")) == 0).count());
        row = writeRow(sheet, row, null, "复核线索总数", auditItems.size());
        row = writeRow(sheet, row, null, "高优先级线索数", countAuditSeverity(auditItems, "HIGH"));
        row = writeRow(sheet, row, null, "中优先级线索数", countAuditSeverity(auditItems, "MEDIUM"));
        autoSize(sheet, 2);
    }

    @SuppressWarnings("unchecked")
    /** 写入成绩明细工作表，按每位选手展开各题得分。 */
    private void writeScoreSheet(Workbook workbook,
                                 Map<String, CellStyle> styles,
                                 Contest contest,
                                 List<ContestProblem> problems,
                                 List<Map<String, Object>> ranking,
                                 Map<Long, User> users,
                                 Map<Long, ContestRegistration> registrationMap,
                                 Map<Long, List<Submission>> submissionsByUser) {
        Sheet sheet = workbook.createSheet("成绩明细");
        int row = 0;
        List<Object> headers = new ArrayList<>(List.of(
                "排名", "用户ID", "用户名", "邮箱", "身份", "入场状态", "提交状态",
                "通过题数", "总罚时(分钟)", "总分", "报名时间", "首次入场", "最近提交"
        ));
        for (int i = 0; i < problems.size(); i++) {
            ContestProblem problem = problems.get(i);
            headers.add("第" + (i + 1) + "题-" + safeProblemTitle(problem));
        }
        row = writeRow(sheet, row, styles.get("header"), headers.toArray());

        for (Map<String, Object> item : ranking) {
            Long userId = longValue(item.get("userId"));
            User user = users.get(userId);
            ContestRegistration registration = registrationMap.get(userId);
            List<Object> values = new ArrayList<>();
            values.add(item.get("rank"));
            values.add(userId);
            values.add(item.get("username"));
            values.add(user != null ? user.getEmail() : "");
            values.add(Boolean.TRUE.equals(item.get("isCreator")) ? "创建者" : "参赛者");
            values.add(Boolean.TRUE.equals(item.get("entered")) ? "已入场" : "未入场");
            values.add(Boolean.TRUE.equals(item.get("hasSubmission")) ? "有提交" : "无提交");
            values.add(item.get("solvedCount"));
            values.add(item.get("totalPenalty"));
            values.add(item.get("totalScore"));
            values.add(registration != null ? registration.getRegisteredAt() : "");
            values.add(item.get("enteredAt"));
            values.add(item.get("lastSubmissionAt"));
            List<Map<String, Object>> cells = item.get("problems") instanceof List
                    ? (List<Map<String, Object>>) item.get("problems")
                    : List.of();
            for (int i = 0; i < problems.size(); i++) {
                Map<String, Object> cell = i < cells.size() ? cells.get(i) : Map.of();
                values.add(formatProblemResultCell(cell));
            }
            writeRow(sheet, row++, null, values.toArray());
        }
        sheet.createFreezePane(0, 1);
        autoSize(sheet, Math.min(13 + problems.size(), 35));
    }

    private void writeProblemStatsSheet(Workbook workbook,
                                        Map<String, CellStyle> styles,
                                        List<Map<String, Object>> problemStats) {
        Sheet sheet = workbook.createSheet("题目统计");
        int row = 0;
        row = writeRow(sheet, row, styles.get("header"),
                "题号", "题目ID", "题目名称", "分值", "难度", "尝试人数", "通过人数", "提交次数", "通过率");
        for (Map<String, Object> stat : problemStats) {
            row = writeRow(sheet, row, null,
                    stat.get("index"),
                    stat.get("problemId"),
                    stat.get("title"),
                    stat.get("score"),
                    normalizeDifficultyText(stat.get("difficulty")),
                    stat.get("attemptedCount"),
                    stat.get("acceptedCount"),
                    stat.get("attemptCount"),
                    stat.get("acceptedRate"));
        }
        sheet.createFreezePane(0, 1);
        autoSize(sheet, 9);
    }

    private void writeSubmissionHistorySheet(Workbook workbook,
                                             Map<String, CellStyle> styles,
                                             List<Submission> submissions,
                                             Map<Long, User> users,
                                             Map<Long, ContestProblem> problemMap) {
        Sheet sheet = workbook.createSheet("提交历史");
        int row = 0;
        row = writeRow(sheet, row, styles.get("header"),
                "提交ID", "用户ID", "用户名", "题目ID", "题目名称", "语言", "结果", "得分", "耗时(ms)", "内存(KB)", "提交时间", "错误信息");
        for (Submission submission : submissions) {
            User user = users.get(submission.getUserId());
            ContestProblem problem = problemMap.get(submission.getProblemId());
            row = writeRow(sheet, row, null,
                    submission.getId(),
                    submission.getUserId(),
                    user != null ? user.getUsername() : "",
                    submission.getProblemId(),
                    problem != null ? safeProblemTitle(problem) : "",
                    submission.getLanguage(),
                    statusTextForSubmission(submission.getStatus()),
                    submission.getScore(),
                    submission.getExecutionTime(),
                    submission.getMemoryUsed(),
                    submission.getCreatedAt(),
                    trimText(submission.getErrorMessage(), 500));
        }
        sheet.createFreezePane(0, 1);
        autoSize(sheet, 12);
    }

    private void writeCodeTraceSheet(Workbook workbook,
                                     Map<String, CellStyle> styles,
                                     List<Submission> submissions,
                                     Map<Long, User> users,
                                     Map<Long, ContestProblem> problemMap) {
        Sheet sheet = workbook.createSheet("代码留痕");
        int row = 0;
        row = writeRow(sheet, row, styles.get("header"),
                "提交ID", "用户ID", "用户名", "题目ID", "题目名称", "语言", "结果", "提交时间", "代码长度", "代码SHA-256", "代码内容");
        for (Submission submission : submissions) {
            User user = users.get(submission.getUserId());
            ContestProblem problem = problemMap.get(submission.getProblemId());
            String code = submission.getCode() == null ? "" : submission.getCode();
            row = writeRow(sheet, row, null,
                    submission.getId(),
                    submission.getUserId(),
                    user != null ? user.getUsername() : "",
                    submission.getProblemId(),
                    problem != null ? safeProblemTitle(problem) : "",
                    submission.getLanguage(),
                    statusTextForSubmission(submission.getStatus()),
                    submission.getCreatedAt(),
                    code.length(),
                    codeHash(code),
                    trimText(code, 32000));
        }
        sheet.createFreezePane(0, 1);
        autoSize(sheet, 10);
        sheet.setColumnWidth(10, 12000);
    }

    private void writeTestResultSheet(Workbook workbook,
                                      Map<String, CellStyle> styles,
                                      List<SubmissionResult> results,
                                      List<Submission> submissions,
                                      Map<Long, User> users,
                                      Map<Long, ContestProblem> problemMap) {
        Sheet sheet = workbook.createSheet("测试点明细");
        Map<Long, Submission> submissionMap = submissions.stream()
                .collect(Collectors.toMap(Submission::getId, Function.identity(), (a, b) -> a));
        int row = 0;
        row = writeRow(sheet, row, styles.get("header"),
                "提交ID", "用户ID", "用户名", "题目ID", "题目名称", "测试点ID", "结果", "耗时(ms)", "内存(KB)", "生成时间", "错误信息");
        for (SubmissionResult result : results) {
            Submission submission = submissionMap.get(result.getSubmissionId());
            User user = submission != null ? users.get(submission.getUserId()) : null;
            ContestProblem problem = submission != null ? problemMap.get(submission.getProblemId()) : null;
            row = writeRow(sheet, row, null,
                    result.getSubmissionId(),
                    submission != null ? submission.getUserId() : "",
                    user != null ? user.getUsername() : "",
                    submission != null ? submission.getProblemId() : "",
                    problem != null ? safeProblemTitle(problem) : "",
                    result.getTestCaseId(),
                    statusTextForSubmission(result.getStatus()),
                    result.getExecutionTime(),
                    result.getMemoryUsed(),
                    result.getCreatedAt(),
                    trimText(result.getErrorMessage(), 500));
        }
        sheet.createFreezePane(0, 1);
        autoSize(sheet, 11);
    }

    /**
     * 生成竞赛异常排查线索 —— 5 种复核线索，系统只提供证据不自动判定违规。
     *
     * 线索类型：
     *   SAME_ACCEPTED_CODE (HIGH, 92分)   — 不同用户同一题 AC 代码 SHA-256 完全一致
     *   AFTER_PERSONAL_DEADLINE (HIGH, 88) — 提交时间超过个人考试倒计时
     *   FAST_ACCEPTED (MEDIUM, 68分)      — 首次提交到 AC ≤ 2 分钟且提交 ≤ 2 次
     *   HIGH_ERROR_SUBMISSIONS (LOW, 38)  — 编译/运行错误 ≥ 5 次
     *   REGISTERED_WITHOUT_SUBMISSION (INFO, 12) — 报名但未提交
     *
     * 风险分 = 基础分 + min(8, 相关数量-1)，上限 100。
     */
    private List<Map<String, Object>> buildAuditItems(ContestResultData data) {
        List<Map<String, Object>> items = new ArrayList<>();

        record SameAcceptedCodeGroup(Long problemId, Set<Long> userIds, List<Submission> submissions, String hash) {
        }

        Map<String, List<Submission>> acceptedByCode = data.submissions().stream()
                .filter(submission -> "ACCEPTED".equals(submission.getStatus()))
                .filter(submission -> submission.getCode() != null && !submission.getCode().isBlank())
                .collect(Collectors.groupingBy(submission -> submission.getProblemId() + ":" + codeHash(submission.getCode())));
        Map<String, List<SameAcceptedCodeGroup>> acceptedGroupsByProblemUsers = new LinkedHashMap<>();
        for (Map.Entry<String, List<Submission>> entry : acceptedByCode.entrySet()) {
            Set<Long> distinctUsers = entry.getValue().stream().map(Submission::getUserId).collect(Collectors.toSet());
            if (distinctUsers.size() < 2) {
                continue;
            }
            Submission first = entry.getValue().get(0);
            String key = first.getProblemId() + ":" + userScopeKey(distinctUsers);
            acceptedGroupsByProblemUsers.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new SameAcceptedCodeGroup(first.getProblemId(), distinctUsers, entry.getValue(), codeHash(first.getCode())));
        }
        for (List<SameAcceptedCodeGroup> groups : acceptedGroupsByProblemUsers.values()) {
            if (groups.isEmpty()) {
                continue;
            }
            groups.sort(Comparator.comparing(group -> earliestSubmissionTime(group.submissions()),
                    Comparator.nullsLast(LocalDateTime::compareTo)));
            SameAcceptedCodeGroup firstGroup = groups.get(0);
            Set<Long> distinctUsers = firstGroup.userIds();
            ContestProblem problem = data.problemMap().get(firstGroup.problemId());
            String usernames = formatUserScope(distinctUsers, data.users());
            List<Submission> mergedSubmissions = groups.stream()
                    .flatMap(group -> group.submissions().stream())
                    .sorted(Comparator
                            .comparing(Submission::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                            .thenComparing(Submission::getId, Comparator.nullsLast(Long::compareTo)))
                    .toList();
            List<Long> ids = mergedSubmissions.stream()
                    .map(Submission::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            List<String> hashPrefixes = groups.stream()
                    .map(SameAcceptedCodeGroup::hash)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(hash -> trimText(hash, 12))
                    .toList();
            String variantText = groups.size() > 1
                    ? "共发现 " + groups.size() + " 组相同代码变体，"
                    : "";
            Map<String, Object> item = addAuditItem(items,
                    "SAME_ACCEPTED_CODE",
                    "相同通过代码",
                    "HIGH",
                    usernames,
                    firstGroup.problemId(),
                    problem != null ? safeProblemTitle(problem) : String.valueOf(firstGroup.problemId()),
                    "同一道题存在 " + distinctUsers.size() + " 名用户的 AC 源码哈希一致，"
                            + variantText + "涉及 " + ids.size() + " 次相关提交；哈希前缀："
                            + String.join("、", hashPrefixes) + "。",
                    "优先查看相关提交详情或 Excel「代码留痕」，按题目、用户和提交时间线合并复核，不需要逐条重复处理相同风险。",
                    ids,
                    earliestSubmissionTime(mergedSubmissions),
                    distinctUsers.size());
            item.put("codeVariantCount", groups.size());
            item.put("affectedUserCount", distinctUsers.size());
            item.put("hashPrefixes", hashPrefixes);
        }

        Map<Long, Map<Long, List<Submission>>> byUserProblem = data.submissions().stream()
                .collect(Collectors.groupingBy(Submission::getUserId, Collectors.groupingBy(Submission::getProblemId)));
        for (Map.Entry<Long, Map<Long, List<Submission>>> userEntry : byUserProblem.entrySet()) {
            Long userId = userEntry.getKey();
            User user = data.users().get(userId);
            for (Map.Entry<Long, List<Submission>> problemEntry : userEntry.getValue().entrySet()) {
                List<Submission> list = problemEntry.getValue().stream()
                        .sorted(Comparator.comparing(Submission::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                        .toList();
                if (list.isEmpty()) {
                    continue;
                }
                Submission first = list.get(0);
                Submission firstAccepted = list.stream().filter(submission -> "ACCEPTED".equals(submission.getStatus())).findFirst().orElse(null);
                if (firstAccepted == null || first.getCreatedAt() == null || firstAccepted.getCreatedAt() == null) {
                    continue;
                }
                long minutes = Duration.between(first.getCreatedAt(), firstAccepted.getCreatedAt()).toMinutes();
                if (minutes <= 2 && list.size() <= 2) {
                    ContestProblem problem = data.problemMap().get(problemEntry.getKey());
                    addAuditItem(items,
                            "FAST_ACCEPTED",
                            "短时间通过",
                            "MEDIUM",
                            formatUser(user, userId),
                            problemEntry.getKey(),
                            problem != null ? safeProblemTitle(problem) : String.valueOf(problemEntry.getKey()),
                            "首次提交到首次 AC 间隔 " + Math.max(0, minutes) + " 分钟，且该题提交次数为 " + list.size() + " 次。",
                            "可与题目难度、授课进度、考试现场情况和代码留痕交叉查看；该线索不单独作为违规结论。",
                            submissionIdList(firstAccepted.getId()),
                            firstAccepted.getCreatedAt(),
                            list.size());
                }
            }
        }

        Map<Long, Long> errorCountByUser = data.submissions().stream()
                .filter(submission -> Set.of("COMPILE_ERROR", "COMPILATION_ERROR", "RUNTIME_ERROR").contains(submission.getStatus()))
                .collect(Collectors.groupingBy(Submission::getUserId, Collectors.counting()));
        for (Map.Entry<Long, Long> entry : errorCountByUser.entrySet()) {
            if (entry.getValue() >= 5) {
                User user = data.users().get(entry.getKey());
                addAuditItem(items,
                        "HIGH_ERROR_SUBMISSIONS",
                        "高错误提交",
                        "LOW",
                        formatUser(user, entry.getKey()),
                        null,
                        "全部题目",
                        "编译或运行错误提交 " + entry.getValue() + " 次，可能与环境、语言选择、复制代码或练习过程有关。",
                        "优先查看错误信息和语言分布，必要时提醒学生检查编译环境、输入输出格式和运行时异常。",
                        List.of(),
                        null,
                        entry.getValue());
            }
        }

        if ("EXAM".equals(data.contest().getType()) && data.contest().getDurationMinutes() != null && data.contest().getDurationMinutes() > 0) {
            for (Submission submission : data.submissions()) {
                ContestRegistration registration = data.registrationMap().get(submission.getUserId());
                if (registration == null || registration.getEnteredAt() == null || submission.getCreatedAt() == null) {
                    continue;
                }
                LocalDateTime deadline = registration.getEnteredAt().plusMinutes(data.contest().getDurationMinutes());
                if (submission.getCreatedAt().isAfter(deadline)) {
                    User user = data.users().get(submission.getUserId());
                    ContestProblem problem = data.problemMap().get(submission.getProblemId());
                    addAuditItem(items,
                            "AFTER_PERSONAL_DEADLINE",
                            "超过个人考试时长",
                            "HIGH",
                            formatUser(user, submission.getUserId()),
                            submission.getProblemId(),
                            problem != null ? safeProblemTitle(problem) : String.valueOf(submission.getProblemId()),
                            "提交时间晚于个人考试截止时间：" + deadline.toString().replace('T', ' ') + "。",
                            "检查前端倒计时、服务端提交校验和历史数据，确认是否需要手工调整成绩口径。",
                            submissionIdList(submission.getId()),
                            submission.getCreatedAt(),
                            1);
                }
            }
        }

        Set<Long> submittedUsers = data.submissions().stream().map(Submission::getUserId).collect(Collectors.toSet());
        for (ContestRegistration registration : data.registrations()) {
            if (!submittedUsers.contains(registration.getUserId())) {
                User user = data.users().get(registration.getUserId());
                addAuditItem(items,
                        "REGISTERED_WITHOUT_SUBMISSION",
                        "报名未提交",
                        "INFO",
                        formatUser(user, registration.getUserId()),
                        null,
                        "全部题目",
                        "该用户已报名但没有竞赛提交记录。",
                        "录入成绩或课堂统计时可按缺考、未进入作答或未提交处理，并结合首次入场时间判断。",
                        List.of(),
                        registration.getRegisteredAt(),
                        0);
            }
        }

        items.sort(Comparator
                .comparingInt((Map<String, Object> item) -> auditSeverityWeight(String.valueOf(item.get("severity"))))
                .thenComparing(item -> String.valueOf(item.get("type")))
                .thenComparing(item -> String.valueOf(item.get("userScope"))));
        return items;
    }

    /** 汇总异常排查数量、风险等级和最高风险分，供前端和 Excel 概览展示。 */
    private Map<String, Object> buildAuditSummary(List<Map<String, Object>> items, ContestResultData data) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", items.size());
        summary.put("highCount", countAuditSeverity(items, "HIGH"));
        summary.put("mediumCount", countAuditSeverity(items, "MEDIUM"));
        summary.put("lowCount", countAuditSeverity(items, "LOW"));
        summary.put("infoCount", countAuditSeverity(items, "INFO"));
        summary.put("maxRiskScore", items.stream().mapToInt(item -> numberValue(item.get("riskScore"))).max().orElse(0));
        summary.put("averageRiskScore", items.isEmpty() ? 0
                : (int) Math.round(items.stream().mapToInt(item -> numberValue(item.get("riskScore"))).average().orElse(0)));
        summary.put("participantCount", data.ranking().size());
        summary.put("submissionCount", data.submissions().size());
        summary.put("generatedAt", LocalDateTime.now().toString());
        summary.put("note", "反作弊分析仅用于辅助创建者或管理员定位风险，不代表系统自动判定违规。");
        return summary;
    }

    private Map<String, Object> addAuditItem(List<Map<String, Object>> items,
                                             String type,
                                             String typeText,
                                             String severity,
                                             String userScope,
                                             Long problemId,
                                             String problemTitle,
                                             String description,
                                             String suggestion,
                                             List<Long> submissionIds,
                                             LocalDateTime occurredAt,
                                             long relatedCount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("typeText", typeText);
        item.put("severity", severity);
        item.put("severityText", auditSeverityText(severity));
        item.put("riskScore", auditRiskScore(type, severity, relatedCount));
        item.put("riskFactors", auditRiskFactors(type));
        item.put("antiCheatSummary", auditAntiCheatSummary(type));
        item.put("reviewAction", auditReviewAction(type));
        item.put("userScope", userScope);
        item.put("problemId", problemId);
        item.put("problemTitle", problemTitle);
        item.put("description", description);
        item.put("suggestion", suggestion);
        item.put("submissionIds", submissionIds == null ? List.of() : submissionIds);
        item.put("submissionIdText", submissionIds == null || submissionIds.isEmpty()
                ? ""
                : submissionIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        item.put("occurredAt", occurredAt == null ? "" : occurredAt.toString());
        item.put("relatedCount", relatedCount);
        items.add(item);
        return item;
    }

    private String userScopeKey(Set<Long> userIds) {
        return userIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private LocalDateTime earliestSubmissionTime(List<Submission> submissions) {
        return submissions.stream()
                .map(Submission::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String formatUserScope(Set<Long> userIds, Map<Long, User> users) {
        return userIds.stream()
                .sorted()
                .map(uid -> formatUser(users.get(uid), uid))
                .collect(Collectors.joining("、"));
    }

    private String formatUser(User user, Long userId) {
        return user != null ? user.getUsername() + "(" + userId + ")" : String.valueOf(userId);
    }

    private long countAuditSeverity(List<Map<String, Object>> items, String severity) {
        return items.stream().filter(item -> severity.equals(item.get("severity"))).count();
    }

    private int auditSeverityWeight(String severity) {
        if ("HIGH".equals(severity)) return 0;
        if ("MEDIUM".equals(severity)) return 1;
        if ("LOW".equals(severity)) return 2;
        return 3;
    }

    private String auditSeverityText(String severity) {
        if ("HIGH".equals(severity)) return "高";
        if ("MEDIUM".equals(severity)) return "中";
        if ("LOW".equals(severity)) return "低";
        return "提示";
    }

    private int auditRiskScore(String type, String severity, long relatedCount) {
        int base;
        if ("SAME_ACCEPTED_CODE".equals(type)) {
            base = 92;
        } else if ("AFTER_PERSONAL_DEADLINE".equals(type)) {
            base = 88;
        } else if ("FAST_ACCEPTED".equals(type)) {
            base = 68;
        } else if ("HIGH_ERROR_SUBMISSIONS".equals(type)) {
            base = 38;
        } else if ("REGISTERED_WITHOUT_SUBMISSION".equals(type)) {
            base = 12;
        } else if ("HIGH".equals(severity)) {
            base = 80;
        } else if ("MEDIUM".equals(severity)) {
            base = 60;
        } else if ("LOW".equals(severity)) {
            base = 30;
        } else {
            base = 10;
        }
        return Math.min(100, base + Math.max(0, Math.min(8, (int) relatedCount - 1)));
    }

    private List<String> auditRiskFactors(String type) {
        if ("SAME_ACCEPTED_CODE".equals(type)) {
            return List.of("代码相似性", "跨用户同题 AC", "源码哈希一致");
        }
        if ("FAST_ACCEPTED".equals(type)) {
            return List.of("作答速度", "低提交次数", "首次 AC 间隔短");
        }
        if ("AFTER_PERSONAL_DEADLINE".equals(type)) {
            return List.of("考试时长边界", "截止时间后提交", "成绩口径风险");
        }
        if ("HIGH_ERROR_SUBMISSIONS".equals(type)) {
            return List.of("错误模式", "环境或复制痕迹", "练习过程异常");
        }
        if ("REGISTERED_WITHOUT_SUBMISSION".equals(type)) {
            return List.of("参赛状态", "报名未作答", "成绩录入边界");
        }
        return List.of("人工复核");
    }

    private String auditAntiCheatSummary(String type) {
        if ("SAME_ACCEPTED_CODE".equals(type)) {
            return "反作弊重点：不同用户在同题 AC 源码哈希一致，应结合代码留痕、提交时间和现场情况复核来源。";
        }
        if ("FAST_ACCEPTED".equals(type)) {
            return "反作弊重点：极短时间内少量提交即 AC，适合与题目难度、课堂进度和代码来源交叉分析。";
        }
        if ("AFTER_PERSONAL_DEADLINE".equals(type)) {
            return "反作弊重点：提交晚于个人考试截止时间，优先确认前端倒计时和服务端校验是否正常。";
        }
        if ("HIGH_ERROR_SUBMISSIONS".equals(type)) {
            return "分析重点：错误提交集中出现，可能是环境不熟、复制代码未适配或调试过程异常。";
        }
        if ("REGISTERED_WITHOUT_SUBMISSION".equals(type)) {
            return "分析重点：报名但没有提交，主要用于缺考、未作答或成绩录入边界判断。";
        }
        return "分析重点：该线索仅用于人工复核。";
    }

    private String auditReviewAction(String type) {
        if ("SAME_ACCEPTED_CODE".equals(type)) {
            return "查看相关提交源码与提交时间线，必要时线下询问代码来源。";
        }
        if ("FAST_ACCEPTED".equals(type)) {
            return "查看该题提交详情，结合题目难度和学生平时记录判断是否正常。";
        }
        if ("AFTER_PERSONAL_DEADLINE".equals(type)) {
            return "核对个人进入时间、截止时间和服务端提交日志，确认是否调整成绩。";
        }
        if ("HIGH_ERROR_SUBMISSIONS".equals(type)) {
            return "查看错误信息和语言选择，判断是否需要教学提醒或环境排查。";
        }
        if ("REGISTERED_WITHOUT_SUBMISSION".equals(type)) {
            return "结合入场时间和课堂记录，按缺考、未作答或未提交处理。";
        }
        return "结合提交历史进行人工复核。";
    }

    private List<Long> submissionIdList(Long... ids) {
        if (ids == null || ids.length == 0) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Long id : ids) {
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    /** 写入异常排查工作表，方便教师离线复核提交行为。 */
    private void writeAuditSheet(Workbook workbook,
                                 Map<String, CellStyle> styles,
                                 List<Map<String, Object>> auditItems) {
        Sheet sheet = workbook.createSheet("异常排查");
        int row = 0;
        row = writeRow(sheet, row, styles.get("header"),
                "优先级", "风险分", "类型", "反作弊分析", "分析维度", "用户/范围", "题目", "线索说明", "处理建议", "相关提交ID", "相关数量", "发生时间");

        for (Map<String, Object> item : auditItems) {
            CellStyle style = "HIGH".equals(item.get("severity")) || "MEDIUM".equals(item.get("severity"))
                    ? styles.get("warning")
                    : null;
            row = writeRow(sheet, row, style,
                    item.get("severityText"),
                    item.get("riskScore"),
                    item.get("typeText"),
                    item.get("antiCheatSummary"),
                    item.get("riskFactors") instanceof List<?> factors ? factors.stream().map(String::valueOf).collect(Collectors.joining("、")) : "",
                    item.get("userScope"),
                    item.get("problemTitle"),
                    item.get("description"),
                    item.get("suggestion"),
                    item.get("submissionIdText"),
                    item.get("relatedCount"),
                    item.get("occurredAt"));
        }

        if (auditItems.isEmpty()) {
            writeRow(sheet, row, null, "无", "-", "-", "未按当前规则发现需要人工关注的记录。", "");
        }
        sheet.createFreezePane(0, 1);
        autoSize(sheet, 12);
    }

    private int writeRow(Sheet sheet, int rowIndex, CellStyle style, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object value = values[i];
            if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else if (value instanceof LocalDateTime time) {
                cell.setCellValue(time.toString().replace('T', ' '));
            } else {
                cell.setCellValue(value == null ? "" : String.valueOf(value));
            }
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
        return rowIndex + 1;
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(width + 800, 3000), 16000));
        }
    }

    private String safeProblemTitle(ContestProblem problem) {
        if (problem == null) {
            return "";
        }
        if (problem.getProblemTitle() != null && !problem.getProblemTitle().isBlank()) {
            return problem.getProblemTitle();
        }
        return "题目" + problem.getProblemId();
    }

    private String statusTextForSubmission(String status) {
        if ("ACCEPTED".equals(status)) return "通过";
        if ("WRONG_ANSWER".equals(status)) return "答案错误";
        if ("COMPILE_ERROR".equals(status) || "COMPILATION_ERROR".equals(status)) return "编译错误";
        if ("RUNTIME_ERROR".equals(status)) return "运行错误";
        if ("TIME_LIMIT_EXCEEDED".equals(status)) return "超时";
        if ("MEMORY_LIMIT_EXCEEDED".equals(status)) return "内存超限";
        if ("PENDING".equals(status)) return "等待中";
        if ("RUNNING".equals(status)) return "运行中";
        return status == null || status.isBlank() ? "-" : status;
    }

    private String normalizeDifficultyText(Object difficulty) {
        if (difficulty == null) return "-";
        String text = String.valueOf(difficulty);
        if ("EASY".equals(text)) return "简单";
        if ("MEDIUM".equals(text)) return "普通";
        if ("HARD".equals(text)) return "困难";
        if ("EXTREME".equals(text)) return "极限";
        return text;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 计算提交代码的 SHA-256 哈希，用于竞赛异常排查和导出 Excel 代码留痕。
     * 对源代码字符串做 UTF-8 字节编码后计算哈希，改一个字符哈希值就完全不同。
     * 异常排查时按 problemId + ":" + SHA256 分组，不同用户相同哈希 → 触发 SAME_ACCEPTED_CODE 线索。
     */
    private String codeHash(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String trimText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    @SuppressWarnings("unchecked")
    private String buildContestResultCsv(Contest contest, List<ContestProblem> problems, List<Map<String, Object>> ranking) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        List<Map<String, Object>> problemStats = buildProblemStats(problems, ranking);

        int participantCount = ranking.size();
        long enteredCount = ranking.stream().filter(row -> Boolean.TRUE.equals(row.get("entered"))).count();
        long submittedCount = ranking.stream().filter(row -> Boolean.TRUE.equals(row.get("hasSubmission"))).count();
        long acceptedUserCount = ranking.stream().filter(row -> numberValue(row.get("solvedCount")) > 0).count();
        int totalAttempts = problemStats.stream().mapToInt(row -> numberValue(row.get("attemptCount"))).sum();
        int highestScore = ranking.stream().mapToInt(row -> numberValue(row.get("totalScore"))).max().orElse(0);
        int averageScore = participantCount == 0 ? 0
                : (int) Math.round(ranking.stream().mapToInt(row -> numberValue(row.get("totalScore"))).average().orElse(0));

        csv.append("竞赛基本信息").append('\n');
        csv.append("字段,值").append('\n');
        csv.append("竞赛名称,").append(escapeCsv(contest.getTitle())).append('\n');
        csv.append("竞赛类型,").append(escapeCsv(typeText(contest.getType()))).append('\n');
        csv.append("竞赛状态,").append(escapeCsv(statusText(contest.getStatus()))).append('\n');
        csv.append("是否官方,").append(Objects.equals(contest.getIsOfficial(), 1) ? "是" : "否").append('\n');
        csv.append("开始时间,").append(escapeCsv(contest.getStartTime())).append('\n');
        csv.append("结束时间,").append(escapeCsv(isOpenEndedContest(contest) ? "长期开放" : contest.getEndTime())).append('\n');
        csv.append("考试时长(分钟),").append(escapeCsv(contest.getDurationMinutes() != null && contest.getDurationMinutes() > 0 ? contest.getDurationMinutes() : "不限制")).append('\n');
        csv.append("题目数量,").append(problems.size()).append('\n');
        csv.append("导出时间,").append(escapeCsv(LocalDateTime.now())).append('\n');
        csv.append('\n');

        csv.append("数据概览").append('\n');
        csv.append("指标,数值").append('\n');
        csv.append("报名人数,").append(participantCount).append('\n');
        csv.append("入场人数,").append(enteredCount).append('\n');
        csv.append("有提交人数,").append(submittedCount).append('\n');
        csv.append("有通过人数,").append(acceptedUserCount).append('\n');
        csv.append("总提交次数,").append(totalAttempts).append('\n');
        csv.append("最高总分,").append(highestScore).append('\n');
        csv.append("平均总分,").append(averageScore).append('\n');
        csv.append('\n');

        csv.append("题目统计").append('\n');
        csv.append("题号,题目ID,题目名称,分值,难度,尝试人数,通过人数,提交次数,通过率").append('\n');
        for (Map<String, Object> stat : problemStats) {
            csv.append(escapeCsv(stat.get("index"))).append(',')
                    .append(escapeCsv(stat.get("problemId"))).append(',')
                    .append(escapeCsv(stat.get("title"))).append(',')
                    .append(escapeCsv(stat.get("score"))).append(',')
                    .append(escapeCsv(stat.get("difficulty"))).append(',')
                    .append(escapeCsv(stat.get("attemptedCount"))).append(',')
                    .append(escapeCsv(stat.get("acceptedCount"))).append(',')
                    .append(escapeCsv(stat.get("attemptCount"))).append(',')
                    .append(escapeCsv(stat.get("acceptedRate"))).append('\n');
        }
        csv.append('\n');

        csv.append("成绩明细").append('\n');
        csv.append("排名,用户名,账号身份,入场状态,提交状态,通过题数,总罚时(分钟),总分,首次入场,最近提交");
        for (int i = 0; i < problems.size(); i++) {
            String title = problems.get(i).getProblemTitle();
            csv.append(',').append(escapeCsv("第" + (i + 1) + "题" + (title == null || title.isBlank() ? "" : "：" + title)));
        }
        csv.append('\n');

        for (Map<String, Object> row : ranking) {
            csv.append(escapeCsv(row.get("rank"))).append(',')
                    .append(escapeCsv(row.get("username"))).append(',')
                    .append(escapeCsv(Boolean.TRUE.equals(row.get("isCreator")) ? "创建者" : "参赛者")).append(',')
                    .append(escapeCsv(Boolean.TRUE.equals(row.get("entered")) ? "已入场" : "未入场")).append(',')
                    .append(escapeCsv(Boolean.TRUE.equals(row.get("hasSubmission")) ? "有提交" : "无提交")).append(',')
                    .append(escapeCsv(row.get("solvedCount"))).append(',')
                    .append(escapeCsv(row.get("totalPenalty"))).append(',')
                    .append(escapeCsv(row.get("totalScore"))).append(',')
                    .append(escapeCsv(row.get("enteredAt"))).append(',')
                    .append(escapeCsv(row.get("lastSubmissionAt")));

            List<Map<String, Object>> problemCells = row.get("problems") instanceof List
                    ? (List<Map<String, Object>>) row.get("problems")
                    : List.of();
            for (int i = 0; i < problems.size(); i++) {
                Map<String, Object> cell = i < problemCells.size() ? problemCells.get(i) : Map.of();
                csv.append(',').append(escapeCsv(formatProblemResultCell(cell)));
            }
            csv.append('\n');
        }
        return csv.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildProblemStats(List<ContestProblem> problems, List<Map<String, Object>> ranking) {
        List<Map<String, Object>> stats = new ArrayList<>();
        for (int i = 0; i < problems.size(); i++) {
            ContestProblem problem = problems.get(i);
            int attemptedCount = 0;
            int acceptedCount = 0;
            int attemptCount = 0;
            for (Map<String, Object> row : ranking) {
                List<Map<String, Object>> cells = row.get("problems") instanceof List
                        ? (List<Map<String, Object>>) row.get("problems")
                        : List.of();
                Map<String, Object> cell = i < cells.size() ? cells.get(i) : Map.of();
                int attempts = numberValue(cell.get("wrongAttempts")) + (Boolean.TRUE.equals(cell.get("accepted")) ? 1 : 0);
                if (attempts > 0) {
                    attemptedCount++;
                }
                if (Boolean.TRUE.equals(cell.get("accepted"))) {
                    acceptedCount++;
                }
                attemptCount += attempts;
            }
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("index", i + 1);
            stat.put("problemId", problem.getProblemId());
            stat.put("title", problem.getProblemTitle() == null || problem.getProblemTitle().isBlank()
                    ? "题目" + problem.getProblemId()
                    : problem.getProblemTitle());
            stat.put("score", problem.getScore() == null ? 100 : problem.getScore());
            stat.put("difficulty", problem.getDifficulty() == null ? "-" : problem.getDifficulty());
            stat.put("attemptedCount", attemptedCount);
            stat.put("acceptedCount", acceptedCount);
            stat.put("attemptCount", attemptCount);
            stat.put("acceptedRate", attemptedCount == 0 ? "-" : Math.round(acceptedCount * 100.0 / attemptedCount) + "%");
            stats.add(stat);
        }
        return stats;
    }

    private String formatProblemResultCell(Map<String, Object> cell) {
        if (Boolean.TRUE.equals(cell.get("accepted"))) {
            Object acTime = cell.get("acTime");
            Object wrongAttempts = cell.get("wrongAttempts");
            Object score = cell.get("score");
            return "通过 / " + acTime + "分钟 / 错误" + wrongAttempts + "次 / " + score + "分";
        }
        Object wrongAttempts = cell.get("wrongAttempts");
        int attempts = wrongAttempts instanceof Number ? ((Number) wrongAttempts).intValue() : 0;
        return attempts > 0 ? "未通过 / 提交" + attempts + "次" : "未提交";
    }

    private int numberValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean isOpenEndedContest(Contest contest) {
        return contest.getEndTime() == null || contest.getEndTime().getYear() >= 2099;
    }

    private String typeText(String type) {
        if ("EXAM".equals(type)) return "限时考试";
        if ("PRACTICE".equals(type)) return "练习赛";
        if ("OFFICIAL".equals(type)) return "官方活动";
        if ("RATED".equals(type)) return "积分赛";
        return type == null || type.isBlank() ? "-" : type;
    }

    private String statusText(String status) {
        if ("UPCOMING".equals(status)) return "即将开始";
        if ("RUNNING".equals(status)) return "进行中";
        if ("ENDED".equals(status)) return "已结束";
        if ("DRAFT".equals(status)) return "草稿";
        return status == null || status.isBlank() ? "-" : status;
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
