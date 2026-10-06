/** 题目集 API — 专题训练集增删改查 */
import api from './index'

export const problemSetApi = {
  /** 获取题目集列表（分页） */
  list: (params?: { type?: string; mine?: boolean; onlyPublic?: boolean; page?: number; size?: number }) =>
    api.get('/api/problem-sets', { params }),

  /** 获取题目集详情 */
  getDetail: (id: number) =>
    api.get(`/api/problem-sets/${id}`),

  /** 获取题目集中的题目列表 */
  getItems: (id: number) =>
    api.get(`/api/problem-sets/${id}/items`),

  /** 创建题目集 */
  create: (data: { title: string; description?: string; type: string; isPublic?: number }) =>
    api.post('/api/problem-sets', data),

  /** 更新题目集 */
  update: (id: number, data: any) =>
    api.put(`/api/problem-sets/${id}`, data),

  /** 删除题目集 */
  remove: (id: number) =>
    api.delete(`/api/problem-sets/${id}`),

  /** 添加题目到题目集 */
  addItems: (id: number, items: { problemId: number; sortOrder?: number }[]) =>
    api.post(`/api/problem-sets/${id}/items`, items),

  /** 从题目集移除题目 */
  removeItems: (id: number, problemIds: number[]) =>
    api.delete(`/api/problem-sets/${id}/items`, { data: { problemIds } })
}
