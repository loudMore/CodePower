/** 题目反馈 API */
import api from './index'

export const problemReportApi = {
  /** 提交题目反馈 */
  submit: (data: { problemId: number; reportType: string; content: string }) =>
    api.post('/api/problem-reports', data),

  /** 查看某题目的反馈列表 */
  listByProblem: (problemId: number, params?: { page?: number; size?: number }) =>
    api.get(`/api/problem-reports/problem/${problemId}`, { params }),

  /** 查看我的反馈记录 */
  listMy: (params?: { page?: number; size?: number }) =>
    api.get('/api/problem-reports/my', { params }),

  /** 处理反馈 */
  handle: (id: number, data: { status: string; reply?: string }) =>
    api.put(`/api/problem-reports/${id}/handle`, data)
}
