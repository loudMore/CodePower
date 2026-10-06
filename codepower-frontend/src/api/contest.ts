/** 竞赛 API — 竞赛列表、报名、排名、创建 */
import api from './index'

export type RankingFilter = 'active' | 'submitted' | 'all'

export const contestApi = {
  /** 获取竞赛列表（分页） */
  listContests: (params?: { status?: string; type?: string; page?: number; size?: number }) =>
    api.get('/api/contests', { params }),

  /** 获取我参与/创建的竞赛 */
  listMyContests: (params?: { filter?: string; status?: string; type?: string; page?: number; size?: number }) =>
    api.get('/api/contests/my', { params }),

  /** 获取竞赛详情 */
  getContestDetail: (id: number) =>
    api.get(`/api/contests/${id}`),

  /** 获取竞赛题目列表 */
  getContestProblems: (id: number) =>
    api.get(`/api/contests/${id}/problems`),

  /** 报名参加竞赛 */
  register: (contestId: number, password?: string) =>
    api.post(`/api/contests/${contestId}/register`, password ? { password } : {}),

  /** 获取竞赛排名 */
  getRanking: (contestId: number, filter: RankingFilter = 'active') =>
    api.get(`/api/contests/${contestId}/ranking`, { params: { filter } }),

  /** 导出竞赛成绩 */
  exportResults: (contestId: number) =>
    api.get(`/api/contests/${contestId}/results/export`, { responseType: 'blob' }),

  /** 在线查看竞赛异常排查线索 */
  getAudit: (contestId: number) =>
    api.get(`/api/contests/${contestId}/audit`),

  /** 创建竞赛 */
  createContest: (data: any) =>
    api.post('/api/contests', data),

  /** 更新竞赛信息 */
  updateContest: (id: number, data: any) =>
    api.put(`/api/contests/${id}`, data),

  /** 删除竞赛 */
  deleteContest: (id: number) =>
    api.delete(`/api/contests/${id}`),

  /** 向竞赛添加题目 */
  addProblems: (contestId: number, problems: any[]) =>
    api.post(`/api/contests/${contestId}/problems`, problems),

  /** 从竞赛移除题目 */
  removeProblems: (contestId: number, problemIds: number[]) =>
    api.delete(`/api/contests/${contestId}/problems`, { data: { problemIds } }),

  /** 从题目集导入题目到竞赛 */
  importFromProblemSet: (contestId: number, setId: number) =>
    api.post(`/api/contests/${contestId}/import-from-set/${setId}`),

  /** 通过邀请码加入竞赛 */
  joinByInviteCode: (inviteCode: string) =>
    api.post('/api/contests/join', { inviteCode }),

  /** 更新竞赛状态 */
  updateStatus: (contestId: number, status: string) =>
    api.put(`/api/contests/${contestId}/status`, { status })
}
