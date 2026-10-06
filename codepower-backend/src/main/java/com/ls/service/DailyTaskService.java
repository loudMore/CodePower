/**
 * 文件说明：每日任务 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import java.util.List;
import java.util.Map;

/** 每日任务服务 */
public interface DailyTaskService {
    /** 获取用户当日任务列表及进度 */
    List<Map<String, Object>> getDailyTasks(Long userId);

    /** 推进指定任务的完成进度 */
    void incrementTaskProgress(Long userId, String taskKey);

    /** 记录用户通过题目以推进相关任务 */
    void recordAcceptedProblem(Long userId, Long problemId);

    /** 领取已完成任务的奖励 */
    Map<String, Object> claimTaskReward(Long userId, Long taskId);
}
