/** 管理后台接口 — 概览、审核、模型配置和评测集群 */
import api from './index'

export const adminApi = {
  /** 获取后台首页摘要数据 */
  getDashboard: () =>
    api.get('/api/admin/dashboard'),

  /** 获取平台统计概览 */
  getStatsOverview: () =>
    api.get('/api/admin/stats/overview'),

  /** 分页查询用户列表 */
  listUsers: (params?: { page?: number; size?: number; keyword?: string; role?: string }) =>
    api.get('/api/admin/users', { params }),

  /** 修改用户角色 */
  updateUserRole: (userId: number, role: string) =>
    api.put(`/api/admin/users/${userId}/role`, { role }),

  /** 修改用户启用状态 */
  updateUserStatus: (userId: number, status: number) =>
    api.put(`/api/admin/users/${userId}/status`, { status }),

  /** 分页查询题目列表 */
  listProblems: (params?: { page?: number; size?: number; keyword?: string; status?: number }) =>
    api.get('/api/admin/problems', { params }),

  /** 查询待审核题目 */
  listProblemsForReview: (params?: { page?: number; size?: number; keyword?: string }) =>
    api.get('/api/admin/problems/review', { params }),

  /** 修改题目发布状态 */
  updateProblemStatus: (problemId: number, status: number) =>
    api.put(`/api/admin/problems/${problemId}/status`, { status }),

  /** 分页查询题目反馈 */
  listProblemReports: (params?: { page?: number; size?: number; status?: string; keyword?: string }) =>
    api.get('/api/admin/problem-reports', { params }),

  /** 处理题目反馈 */
  handleProblemReport: (id: number, data: { status: string; reply?: string }) =>
    api.put(`/api/admin/problem-reports/${id}/handle`, data),

  /** 获取提交趋势统计 */
  getSubmissionStats: (days = 7) =>
    api.get('/api/admin/stats/submissions', { params: { days } }),

  /** 获取 Judge0 评测集群状态 */
  getJudgeCluster: () =>
    api.get('/api/admin/judge-cluster'),

  /** 触发答辩演示用评测压力任务 */
  triggerJudgeClusterDemo: (data?: {
    count?: number
    language?: string
    timeLimitMs?: number
    memoryLimitKb?: number
  }) =>
    api.post('/api/admin/judge-cluster/demo-load', data || {}),

  /** 查询角色升级申请 */
  listUpgradeRequests: (status?: string) =>
    api.get('/api/admin/upgrade-requests', { params: status ? { status } : {} }),

  /** 审核角色升级申请 */
  reviewUpgradeRequest: (id: number, action: string, comment?: string, reviewerId?: number) =>
    api.put(`/api/admin/upgrade-requests/${id}`, { action, comment, reviewerId }),

  /** 管理员重置用户密码 */
  resetUserPassword: (userId: number, password: string) =>
    api.put(`/api/admin/users/${userId}/password`, { password }),

  /** 调整用户经验值 */
  adjustUserExp: (userId: number, amount: number) =>
    api.put(`/api/admin/users/${userId}/exp`, { amount }),

  /** 调整用户 AI 积分 */
  adjustUserAiPoints: (userId: number, amount: number) =>
    api.put(`/api/admin/users/${userId}/ai-points`, { amount }),

  /** 查询 AI 模型提供商 */
  listAiProviders: () =>
    api.get('/api/admin/ai/providers'),

  /** 新增 AI 模型提供商 */
  createAiProvider: (data: any) =>
    api.post('/api/admin/ai/providers', data),

  /** 更新 AI 模型提供商 */
  updateAiProvider: (id: number, data: any) =>
    api.put(`/api/admin/ai/providers/${id}`, data),

  /** 删除 AI 模型提供商 */
  deleteAiProvider: (id: number) =>
    api.delete(`/api/admin/ai/providers/${id}`),

  /** 从远程提供商拉取模型列表 */
  fetchAiProviderModels: (id: number) =>
    api.post(`/api/admin/ai/providers/${id}/fetch-models`),

  /** 新增 AI 模型配置 */
  createAiModel: (providerId: number, data: any) =>
    api.post(`/api/admin/ai/providers/${providerId}/models`, data),

  /** 更新 AI 模型配置 */
  updateAiModel: (id: number, data: any) =>
    api.put(`/api/admin/ai/models/${id}`, data),

  /** 删除 AI 模型配置 */
  deleteAiModel: (id: number) =>
    api.delete(`/api/admin/ai/models/${id}`)
}
