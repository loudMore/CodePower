/**
 * 文件说明：数据分析 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

import java.util.Map;

/** 数据统计与分析服务 */
public interface DataAnalysisService {

    /** 获取月活跃用户统计数据 */
    Map<String, Object> getMonthlyActiveUsers();

    /** 获取热门学习路径统计数据 */
    Map<String, Object> getPopularLearningPaths();

    /** 获取用户技能雷达图数据 */
    Map<String, Object> getUserRadarChartData();

    /** 获取学习路线总览数据 */
    Map<String, Object> getLearningPathOverview();

    /** 获取用户地理分布数据 */
    Map<String, Object> getUserDistributionMapData();

    /** 获取用户增长趋势数据 */
    Map<String, Object> getUserGrowthTrendData();

    /** 获取题目难度分布数据 */
    Map<String, Object> getProblemDifficultyDistributionData();

    /** 获取用户活跃时段统计数据 */
    Map<String, Object> getUserActiveHoursData();
} 