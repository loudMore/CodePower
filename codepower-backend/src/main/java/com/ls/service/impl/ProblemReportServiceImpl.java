/**
 * 文件说明：题目反馈 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ls.common.BusinessException;
import com.ls.common.ResultCode;
import com.ls.domain.Problem;
import com.ls.domain.ProblemReport;
import com.ls.domain.User;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.ProblemReportMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.NotificationService;
import com.ls.service.ProblemReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 题目反馈服务实现 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemReportServiceImpl extends ServiceImpl<ProblemReportMapper, ProblemReport>
        implements ProblemReportService {

    private static final String NOTIFICATION_PROBLEM_REPORT = "PROBLEM_REPORT";
    private static final String NOTIFICATION_PROBLEM_REPORT_PENDING = "PROBLEM_REPORT_PENDING";
    private static final String NOTIFICATION_REPORT_HANDLED = "REPORT_HANDLED";
    private static final String NOTIFICATION_PROBLEM_REPORT_HANDLED = "PROBLEM_REPORT_HANDLED";
    private static final int RATE_LIMIT_HOURS = 24;

    private final ProblemMapper problemMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ProblemReport submitReport(Long reporterId, Long problemId, String reportType, String content) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }

        // 频率限制：同一用户+同一题目，24h内只能提交一次
        LocalDateTime cutoff = LocalDateTime.now().minusHours(RATE_LIMIT_HOURS);
        long recentCount = this.count(new LambdaQueryWrapper<ProblemReport>()
                .eq(ProblemReport::getReporterId, reporterId)
                .eq(ProblemReport::getProblemId, problemId)
                .ge(ProblemReport::getCreatedAt, cutoff));
        if (recentCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "你已在24小时内对该题目提交过反馈，请勿频繁操作");
        }

        ProblemReport report = new ProblemReport();
        report.setProblemId(problemId);
        report.setReporterId(reporterId);
        report.setReportType(reportType);
        report.setContent(content);
        report.setStatus("PENDING");
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        this.save(report);

        // 通知出题人和管理员
        User reporter = userMapper.selectById(reporterId);
        String reporterName = reporter != null ? reporter.getUsername() : "用户";
        if (problem.getAuthorId() != null && !problem.getAuthorId().equals(reporterId)) {
            createNotificationSafely(
                    problem.getAuthorId(), NOTIFICATION_PROBLEM_REPORT,
                    "题目收到反馈：" + problem.getTitle(),
                    reporterName + " 对你的题目「" + problem.getTitle() + "」提交了反馈，类型：" + reportType + "，请到后台查看处理。",
                    problemId);
        }
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .eq(User::getStatus, 1)
                .eq(User::getRole, "ADMIN"));
        for (User admin : admins) {
            createNotificationSafely(
                    admin.getId(), NOTIFICATION_PROBLEM_REPORT_PENDING,
                    "有新的题目反馈待处理",
                    "题目「" + problem.getTitle() + "」收到了一条新的反馈，请到后台查看并处理。",
                    problemId);
        }

        return report;
    }

    @Override
    public IPage<ProblemReport> listByProblem(Long problemId, int page, int size) {
        IPage<ProblemReport> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<ProblemReport>()
                        .eq(ProblemReport::getProblemId, problemId)
                        .orderByDesc(ProblemReport::getCreatedAt));
        fillExtraInfo(result.getRecords());
        return result;
    }

    @Override
    public IPage<ProblemReport> listByReporter(Long reporterId, int page, int size) {
        IPage<ProblemReport> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<ProblemReport>()
                        .eq(ProblemReport::getReporterId, reporterId)
                        .orderByDesc(ProblemReport::getCreatedAt));
        fillExtraInfo(result.getRecords());
        return result;
    }

    @Override
    public IPage<ProblemReport> listForAdmin(int page, int size, String status, String keyword) {
        LambdaQueryWrapper<ProblemReport> wrapper = new LambdaQueryWrapper<ProblemReport>()
                .eq(status != null && !status.isBlank(), ProblemReport::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(ProblemReport::getContent, keyword)
                        .or().like(ProblemReport::getReportType, keyword))
                .orderByDesc(ProblemReport::getCreatedAt);
        IPage<ProblemReport> result = this.page(new Page<>(page, size), wrapper);
        fillExtraInfo(result.getRecords());
        return result;
    }

    @Override
    public long countPendingReports() {
        return this.count(new LambdaQueryWrapper<ProblemReport>()
                .eq(ProblemReport::getStatus, "PENDING"));
    }

    @Override
    @Transactional
    public ProblemReport handleReport(Long reportId, String status, String reply, Long operatorId) {
        ProblemReport report = this.getById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "反馈不存在");
        }

        // 反馈处理权限在 Controller / Security 层统一校验
        report.setStatus(status);
        report.setReply(reply);
        report.setUpdatedAt(LocalDateTime.now());
        this.updateById(report);

        // 通知反馈者处理结果
        Problem problem = problemMapper.selectById(report.getProblemId());
        String title = problem != null ? problem.getTitle() : "未知题目";
        String statusText = "RESOLVED".equals(status) ? "已处理" : "已忽略";
        createNotificationSafely(
                report.getReporterId(), NOTIFICATION_REPORT_HANDLED,
                "你的题目反馈已处理",
                "你对「" + title + "」的反馈已被" + statusText + "。" + (reply != null && !reply.isBlank() ? " 处理说明：" + reply : ""),
                report.getProblemId());
        if (problem != null && problem.getAuthorId() != null && !problem.getAuthorId().equals(report.getReporterId())) {
            createNotificationSafely(
                    problem.getAuthorId(), NOTIFICATION_PROBLEM_REPORT_HANDLED,
                    "题目反馈已处理",
                    "你题目「" + title + "」收到的反馈已被" + statusText + "。" + (reply != null && !reply.isBlank() ? " 处理说明：" + reply : ""),
                    report.getProblemId());
        }

        return report;
    }

    private void createNotificationSafely(Long userId, String type, String title, String content, Long relatedId) {
        try {
            notificationService.createNotification(userId, type, title, content, relatedId);
        } catch (Exception e) {
            log.warn("Create problem report notification failed, userId={}, type={}, relatedId={}",
                    userId, type, relatedId, e);
        }
    }

    // 填充题目标题和报告者名称
    private void fillExtraInfo(List<ProblemReport> reports) {
        if (reports.isEmpty()) return;
        Set<Long> problemIds = reports.stream().map(ProblemReport::getProblemId).collect(Collectors.toSet());
        Set<Long> userIds = reports.stream().map(ProblemReport::getReporterId).collect(Collectors.toSet());
        Map<Long, String> problemTitleMap = problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle));
        Map<Long, String> userNameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        for (ProblemReport r : reports) {
            r.setProblemTitle(problemTitleMap.get(r.getProblemId()));
            r.setReporterName(userNameMap.get(r.getReporterId()));
        }
    }
}
