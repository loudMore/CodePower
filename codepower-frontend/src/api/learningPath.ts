/** 学习路线 API — 路线列表、详情、阶段完成 */
import api from './index'

export const learningPathApi = {
  /** 获取学习路线列表（分页） */
  listPaths: (params?: { difficulty?: string; language?: string; page?: number; size?: number }) =>
    api.get('/api/learning-paths', { params }),

  /** 获取学习路线详情 */
  getPathDetail: (id: number) =>
    api.get(`/api/learning-paths/${id}`),

  /** 完成学习路线阶段 */
  completeStage: (pathId: number, stageId: number) =>
    api.post(`/api/learning-paths/${pathId}/stages/${stageId}/complete`),

  /** 获取我的学习进度 */
  getMyProgress: () =>
    api.get('/api/learning-paths/my/progress'),

  /** 创建学习路线 */
  createPath: (data: any) =>
    api.post('/api/learning-paths', data),

  /** 更新学习路线 */
  updatePath: (id: number, data: any) =>
    api.put(`/api/learning-paths/${id}`, data),

  /** 添加学习阶段 */
  addStage: (pathId: number, data: any) =>
    api.post(`/api/learning-paths/${pathId}/stages`, data),

  /** 删除学习阶段 */
  deleteStage: (pathId: number, stageId: number) =>
    api.delete(`/api/learning-paths/${pathId}/stages/${stageId}`)
}
