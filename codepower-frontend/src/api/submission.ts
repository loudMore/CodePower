/** 文件说明：提交记录接口，查询代码提交历史和测试点结果。 */
import api from './index'

/**
 * 提交记录 API — 做题 IDE、竞赛详情、个人主页都会通过这里读取提交历史和测试点详情。
 * 普通训练提交和竞赛提交分开查询，避免竞赛数据污染日常能力画像。
 */
export const submissionApi = {
  /** 查询当前用户在某道普通题上的提交历史，用于 IDE 右侧历史列表和题库状态。 */
  getMySubmissions: (problemId: number, page = 1, size = 20) =>
    api.get(`/api/submissions/problem/${problemId}`, { params: { page, size } }),

  /** 查询当前用户在某场竞赛某题上的提交历史，用于竞赛模式 IDE。 */
  getMyContestProblemSubmissions: (contestId: number, problemId: number, page = 1, size = 20) =>
    api.get(`/api/submissions/contest/${contestId}/problem/${problemId}`, { params: { page, size } }),

  /** 查询当前用户在整场竞赛中的提交历史，用于竞赛页面和排名下钻。 */
  getMyContestSubmissions: (contestId: number, page = 1, size = 50) =>
    api.get(`/api/submissions/contest/${contestId}`, { params: { page, size } }),

  /** 批量获取当前用户在某场竞赛每题最近一次提交代码，用于切题后恢复草稿。 */
  getMyContestDrafts: (contestId: number) =>
    api.get(`/api/submissions/contest/${contestId}/drafts`),

  /** 查询竞赛中某位选手的提交历史，教师/管理员复盘时查看代码留痕。 */
  getContestUserSubmissions: (contestId: number, userId: number, page = 1, size = 50) =>
    api.get(`/api/submissions/contest/${contestId}/user/${userId}`, { params: { page, size } }),

  /** 查询单次提交详情，包含源码、总状态、测试点明细、耗时和内存。 */
  getDetail: (id: number) =>
    api.get(`/api/submissions/${id}`),

  /** 获取当前题最后一次提交，用于非竞赛做题页面恢复上次代码。 */
  getLastSubmission: (problemId: number) =>
    api.get(`/api/submissions/last/${problemId}`),

  /** 获取当前用户最近一次提交使用的语言，用于新题默认选择语言模板。 */
  getPreferredLanguage: () =>
    api.get('/api/submissions/preferred-language'),

  /** 批量查询用户对多道题的做题状态，题库列表用它显示已通过/已尝试。 */
  getUserProblemStatus: (problemIds: number[]) =>
    api.post('/api/submissions/user-status', { problemIds }),

  /** 批量查询用户在竞赛中多道题的状态和得分，竞赛题目列表用它显示当前分数。 */
  getUserContestProblemStatus: (contestId: number, problemIds: number[]) =>
    api.post(`/api/submissions/contest/${contestId}/user-status`, { problemIds }),
}
