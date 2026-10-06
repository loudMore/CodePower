/** 收藏 API — 收藏/取消收藏题目 */
import api from './index'

export const favoriteApi = {
  /** 获取收藏列表 */
  getFavorites: () =>
    api.get('/api/favorites'),

  /** 收藏题目 */
  addFavorite: (problemId: number) =>
    api.post(`/api/favorites/${problemId}`),

  /** 取消收藏题目 */
  removeFavorite: (problemId: number) =>
    api.delete(`/api/favorites/${problemId}`),

  /** 检查题目是否已收藏 */
  checkFavorite: (problemId: number) =>
    api.get(`/api/favorites/check/${problemId}`)
}
