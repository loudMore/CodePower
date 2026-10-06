/** 私信 API — 会话列表、发送消息、未读数 */
import api from './index'

export const messageApi = {
  /** 获取会话列表 */
  getConversations: () =>
    api.get('/api/messages/conversations'),

  /** 获取与某用户的聊天记录 */
  getConversation: (otherUserId: number, page = 1, size = 50) =>
    api.get(`/api/messages/conversation/${otherUserId}`, { params: { page, size } }),

  /** 发送私信 */
  sendMessage: (toUserId: number, content: string) =>
    api.post('/api/messages/send', { toUserId, content }, { skipGlobalError: true } as any),

  /** 标记与某用户的消息为已读 */
  markAsRead: (otherUserId: number) =>
    api.put(`/api/messages/read/${otherUserId}`),

  /** 获取未读私信数 */
  getUnreadCount: () =>
    api.get('/api/messages/unread-count')
}
