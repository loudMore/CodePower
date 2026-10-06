/**
 * 文件说明：题目反馈 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.domain.ProblemReport;

/** 题目反馈服务 */
public interface ProblemReportService {
    /** 提交题目反馈（含频率限制） */
    ProblemReport submitReport(Long reporterId, Long problemId, String reportType, String content);

    /** 按题目查看反馈列表（题目作者/管理员） */
    IPage<ProblemReport> listByProblem(Long problemId, int page, int size);

    /** 查看当前用户提交的反馈列表 */
    IPage<ProblemReport> listByReporter(Long reporterId, int page, int size);

    /** 管理员查看反馈处理队列 */
    IPage<ProblemReport> listForAdmin(int page, int size, String status, String keyword);

    /** 当前待处理反馈数量 */
    long countPendingReports();

    /** 处理反馈（管理员回复） */
    ProblemReport handleReport(Long reportId, String status, String reply, Long operatorId);
}
