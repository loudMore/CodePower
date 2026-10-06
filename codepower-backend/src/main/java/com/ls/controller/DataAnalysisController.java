/**
 * 文件说明：数据分析 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ls.domain.Contest;
import com.ls.domain.Problem;
import com.ls.domain.Submission;
import com.ls.domain.User;
import com.ls.mapper.ContestMapper;
import com.ls.mapper.ProblemMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.DataAnalysisService;
import com.ls.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据分析控制器 - 提供各类统计数据的API
 */
@RestController
@RequestMapping("/api/data-analysis")
public class DataAnalysisController {

    @Autowired
    private DataAnalysisService dataAnalysisService;
    @Autowired
    private RedisCacheService redisCacheService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private SubmissionMapper submissionMapper;
    @Autowired
    private ContestMapper contestMapper;

    /**
     * 获取月活跃用户数据
     * @return 月活跃用户统计数据
     */
    @GetMapping("/monthly-active-users")
    public Map<String, Object> getMonthlyActiveUsers() {
        return dataAnalysisService.getMonthlyActiveUsers();
    }

    /**
     * 获取热门学习路径
     * @return 热门学习路径统计数据
     */
    @GetMapping("/popular-learning-paths")
    public Map<String, Object> getPopularLearningPaths() {
        return dataAnalysisService.getPopularLearningPaths();
    }

    /**
     * 获取用户技能雷达图数据
     * @return 用户各项技能水平数据，用于雷达图展示
     */
    @GetMapping("/user-radar-chart")
    public Map<String, Object> getUserRadarChartData() {
        return dataAnalysisService.getUserRadarChartData();
    }

    /** 获取学习路线总览数据 */
    @GetMapping("/learning-path-overview")
    public Map<String, Object> getLearningPathOverview() {
        return dataAnalysisService.getLearningPathOverview();
    }

    /**
     * 获取用户地理分布数据
     * @return 用户地理位置分布统计数据，用于地图展示
     */
    @GetMapping("/user-distribution-map")
    public Map<String, Object> getUserDistributionMapData() {
        return dataAnalysisService.getUserDistributionMapData();
    }

    /**
     * 获取用户增长趋势数据
     * @return 用户数量随时间增长的趋势数据
     */
    @GetMapping("/user-growth-trend")
    public Map<String, Object> getUserGrowthTrendData() {
        return dataAnalysisService.getUserGrowthTrendData();
    }

    /**
     * 获取题目难度分布数据
     * @return 各难度等级题目分布统计
     */
    @GetMapping("/problem-difficulty-distribution")
    public Map<String, Object> getProblemDifficultyDistributionData() {
        return dataAnalysisService.getProblemDifficultyDistributionData();
    }

    /**
     * 获取用户活跃时段数据
     * @return 用户在一天中不同时段的活跃度统计
     */
    @GetMapping("/user-active-hours")
    public Map<String, Object> getUserActiveHoursData() {
        return dataAnalysisService.getUserActiveHoursData();
    }

    private static final Duration STATS_CACHE_TTL = Duration.ofMinutes(5);

    /** 获取平台核心统计指标（题目数、竞赛数、用户数、提交数），Redis缓存5分钟 */
    @GetMapping("/platform-stats")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPlatformStats() {
        String cacheKey = "stats:platform";
        Map<String, Object> cached = redisCacheService.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalProblems", problemMapper.selectCount(
                new LambdaQueryWrapper<Problem>().eq(Problem::getStatus, 1)));
        stats.put("totalContests", contestMapper.selectCount(null));
        stats.put("totalUsers", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)));
        stats.put("totalSubmissions", submissionMapper.selectCount(null));

        redisCacheService.set(cacheKey, stats, STATS_CACHE_TTL);
        return stats;
    }
} 