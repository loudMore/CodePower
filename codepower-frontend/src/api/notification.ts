/** 通知 API — 通知列表、未读数、标记已读 */
import api from './index'

export const notificationApi = {
  /** 获取通知列表（分页） */
  getNotifications: (page = 1, size = 20) =>
    api.get('/api/notifications', { params: { page, size } }),

  /** 获取未读通知数 */
  getUnreadCount: () =>
    api.get('/api/notifications/unread-count'),

  /** 标记通知为已读 */
  markAsRead: (id: number) =>
    api.put(`/api/notifications/${id}/read`),

  /** 标记全部通知为已读 */
  markAllAsRead: () =>
    api.put('/api/notifications/read-all')
}
