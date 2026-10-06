/** 评论 API — 获取评论、发布、点赞、删除 */
import api from './index'

export interface CommentQueryParams {
  page?: number
  size?: number
  sort?: 'latest' | 'hot' | 'oldest'
  keyword?: string
  mineOnly?: boolean
}

export const commentApi = {
  /** 获取评论列表（按目标类型和ID） */
  getComments: (targetType: string, targetId: number, pageOrParams: number | CommentQueryParams = 1, size = 20) => {
    const params = typeof pageOrParams === 'number'
      ? { page: pageOrParams, size }
      : {
          page: pageOrParams.page ?? 1,
          size: pageOrParams.size ?? size,
          sort: pageOrParams.sort,
          keyword: pageOrParams.keyword,
          mineOnly: pageOrParams.mineOnly
        }
    return api.get(`/api/comments/target/${targetType}/${targetId}`, { params })
  },

  /** 发布评论 */
  addComment: (data: { targetType: string; targetId: number; parentId?: number; content: string }) =>
    api.post('/api/comments', data),

  /** 切换评论点赞状态 */
  toggleLike: (commentId: number) =>
    api.post(`/api/comments/${commentId}/like`),

  /** 删除评论 */
  deleteComment: (commentId: number) =>
    api.delete(`/api/comments/${commentId}`)
}
