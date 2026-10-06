/** 等级与签到 API — 等级信息、签到、每日任务 */
import api from './index'

export const levelApi = {
  /** 获取当前用户等级信息 */
  getLevelInfo: () => api.get('/api/level/info'),
  /** 获取等级配置表 */
  getLevelConfig: () => api.get('/api/level/config'),
  /** 每日签到 */
  checkIn: () => api.post('/api/check-in'),
  /** 获取今日签到状态 */
  getCheckInStatus: () => api.get('/api/check-in/status'),
  /** 获取月度签到记录 */
  getMonthlyCheckIns: (year?: number, month?: number) =>
    api.get('/api/check-in/monthly', { params: { year, month } }),
  /** 获取每日任务列表 */
  getDailyTasks: () => api.get('/api/daily-tasks'),
  /** 领取任务奖励 */
  claimTaskReward: (taskId: number) => api.post(`/api/daily-tasks/${taskId}/claim`),
}
