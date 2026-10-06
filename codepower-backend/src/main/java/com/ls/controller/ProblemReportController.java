/**
 * 文件说明：题目反馈 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.common.BusinessException;
import com.ls.common.Result;
import com.ls.common.ResultCode;
import com.ls.domain.Problem;
import com.ls.domain.ProblemReport;
import com.ls.domain.User;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.ProblemReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 题目反馈控制器 — 提交反馈、查看反馈、管理员处理 */
@RestController
@RequestMapping("/api/problem-reports")
@RequiredArgsConstructor
public class ProblemReportController {

    private final ProblemReportService reportService;
    private final ProblemMapper problemMapper;
    private final UserMapper userMapper;

    /** 提交题目反馈 */
    @PostMapping
    public Result<ProblemReport> submitReport(@AuthenticationPrincipal UserDetails userDetails,
                                               @RequestBody Map<String, Object> body) {
        Long userId = getUserId(userDetails);
        Long problemId = Long.valueOf(body.get("problemId").toString());
        String reportType = (String) body.getOrDefault("reportType", "BUG");
        String content = (String) body.get("content");
        return Result.success(reportService.submitReport(userId, problemId, reportType, content));
    }

    /** 查看指定题目的反馈列表 */
    @GetMapping("/problem/{problemId}")
    public Result<IPage<ProblemReport>> listByProblem(@AuthenticationPrincipal UserDetails userDetails,
                                                       @PathVariable Long problemId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(userDetails);
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        boolean isAdmin = hasAdminRole(userDetails);
        boolean isAuthor = userId != null && userId.equals(problem.getAuthorId());
        if (!isAdmin && !isAuthor) {
            throw new BusinessException(ResultCode.FORBIDDEN, "没有权限查看该题目的反馈");
        }
        return Result.success(reportService.listByProblem(problemId, page, size));
    }

    /** 查看我提交的反馈记录 */
    @GetMapping("/my")
    public Result<IPage<ProblemReport>> listMyReports(@AuthenticationPrincipal UserDetails userDetails,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(userDetails);
        return Result.success(reportService.listByReporter(userId, page, size));
    }

    /** 处理题目反馈（管理员） */
    @PutMapping("/{id}/handle")
    public Result<ProblemReport> handleReport(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        Long userId = getUserId(userDetails);
        String status = body.get("status");
        String reply = body.get("reply");
        return Result.success(reportService.handleReport(id, status, reply, userId));
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return user.getId();
    }

    private boolean hasAdminRole(UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
