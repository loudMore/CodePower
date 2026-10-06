/** 关注 API — 关注/取关、粉丝列表 */
import api from './index'

export const followApi = {
  /** 关注用户 */
  follow: (userId: number) =>
    api.post(`/api/follows/${userId}`),

  /** 取消关注 */
  unfollow: (userId: number) =>
    api.delete(`/api/follows/${userId}`),

  /** 查询关注状态 */
  checkFollow: (userId: number) =>
    api.get(`/api/follows/check/${userId}`),

  /** 获取我的关注列表 */
  getFollowing: (params?: { page?: number; size?: number }) =>
    api.get('/api/follows/following', { params }),

  /** 获取我的粉丝列表 */
  getFollowers: (params?: { page?: number; size?: number }) =>
    api.get('/api/follows/followers', { params }),

  /** 获取指定用户的关注/粉丝数 */
  getCounts: (userId: number) =>
    api.get(`/api/follows/counts/${userId}`),

  /** 获取指定用户的关注列表 */
  getUserFollowing: (userId: number, params?: { page?: number; size?: number }) =>
    api.get(`/api/follows/${userId}/following`, { params }),

  /** 获取指定用户的粉丝列表 */
  getUserFollowers: (userId: number, params?: { page?: number; size?: number }) =>
    api.get(`/api/follows/${userId}/followers`, { params }),
}
