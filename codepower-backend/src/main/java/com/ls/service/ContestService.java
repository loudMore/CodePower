/**
 * 文件说明：竞赛 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ls.domain.Contest;
import com.ls.domain.ContestProblem;

import java.util.List;
import java.util.Map;

/** 竞赛服务 */
public interface ContestService {
    /** 分页查询竞赛列表 */
    IPage<Contest> listContests(String status, String type, int page, int size, Long viewerId);
    /** 获取竞赛详情（含当前用户报名状态） */
    Contest getContestDetail(Long contestId, Long userId);
    /** 用户报名参加竞赛 */
    void register(Long userId, Long contestId, String password);
    /** 获取竞赛排行榜 */
    List<Map<String, Object>> getRanking(Long contestId, Long currentUserId, String filter);
    /** 创建竞赛 */
    Contest createContest(Contest contest);
    /** 更新竞赛信息 */
    Contest updateContest(Long contestId, Contest contest);
    /** 删除竞赛 */
    void deleteContest(Long contestId);
    /** 向竞赛中添加题目 */
    void addProblems(Long contestId, List<ContestProblem> problems, Long userId, String userRole);
    /** 从竞赛中移除题目 */
    void removeProblems(Long contestId, List<Long> problemIds);
    /** 更新竞赛状态 */
    void updateContestStatus(Long contestId, String status);
    /** 校验用户是否可以在竞赛中提交代码 */
    void validateContestSubmission(Long contestId, Long userId, Long problemId, String language);
    /** 获取竞赛题目列表 */
    List<ContestProblem> getContestProblems(Long contestId);
    /** 清除竞赛相关缓存 */
    void evictContestCaches(Long contestId);

    /** 检查用户是否在某个进行中的竞赛中，且该竞赛包含指定题目 */
    boolean isUserInRunningContestWithProblem(Long userId, Long problemId);

    /** 从题目集批量导入题目到竞赛（自动规避重复导入） */
    int importFromProblemSet(Long contestId, Long problemSetId, Long userId, String userRole);

    /** 通过邀请码加入竞赛 */
    Contest joinByInviteCode(Long userId, String inviteCode);

    /** 判断用户是否有权访问竞赛工作区 */
    boolean canAccessContestWorkspace(Contest contest, Long userId);

    /** 根据竞赛ID判断用户是否有权访问竞赛工作区 */
    boolean canAccessContestWorkspace(Long contestId, Long userId);

    /** 分页查询我参加的或我创建的竞赛 */
    IPage<Contest> listMyContests(Long userId, String filter, String status, String type, int page, int size);
}
