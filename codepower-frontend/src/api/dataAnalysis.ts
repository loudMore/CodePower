/** 文件说明：数据分析接口，提供图表页和后台统计所需的数据请求。 */
import api from './index';

/**
 * 数据分析 API — 管理后台和首页图表使用的统计数据入口。
 * 这些接口只做读取，后端负责聚合用户增长、活跃时间、题目难度和学习路线统计。
 */
export const dataAnalysisApi = {
  /** 用户增长趋势折线图 */
  getUserGrowthTrendData: () => api.get('/api/data-analysis/user-growth-trend'),
  /** 用户活跃时间热力图 */
  getUserActiveHoursData: () => api.get('/api/data-analysis/user-active-hours'),
  /** 用户地区分布地图 */
  getUserDistributionMapData: () => api.get('/api/data-analysis/user-distribution-map'),
  /** 题目难度分布饼图 */
  getProblemDifficultyDistributionData: () => api.get('/api/data-analysis/problem-difficulty-distribution'),
  /** 最受欢迎学习路径排行 */
  getPopularLearningPaths: () => api.get('/api/data-analysis/popular-learning-paths'),
  /** 每月活跃用户统计 */
  getMonthlyActiveUsers: () => api.get('/api/data-analysis/monthly-active-users'),
  /** 用户能力雷达图数据 */
  getUserRadarChartData: () => api.get('/api/data-analysis/user-radar-chart'),
  /** 学习路线总览数据 */
  getLearningPathOverview: () => api.get('/api/data-analysis/learning-path-overview'),

  // 兼容旧页面命名：实际访问的是同一组后端接口。
  getUserGrowthData: () => api.get('/api/data-analysis/user-growth-trend'),
  getUserActiveTimeData: () => api.get('/api/data-analysis/user-active-hours'),
  getUserDistributionData: () => api.get('/api/data-analysis/user-distribution-map'),
  getProblemDifficultyData: () => api.get('/api/data-analysis/problem-difficulty-distribution'),
  /** 平台首页统计卡片 */
  getPlatformStats: () => api.get('/api/data-analysis/platform-stats')
};

// 旧组件仍按单函数方式引入，暂时保留这些适配出口。
export const getUserDistribution = () => api.get('/api/data-analysis/user-distribution');
export const getSubmissionCount = () => api.get('/api/data-analysis/submission-count');
export const getProblemDifficultyCount = () => api.get('/api/data-analysis/problem-difficulty-count'); 
