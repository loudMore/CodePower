/** 用户 API — 个人资料、头像、角色升级 */
import api from './index'
import { contestApi } from './contest'

const normalizeEmail = (email: string) => String(email || '').trim().toLowerCase()

export const userApi = {
  /** 获取当前用户资料 */
  getProfile: () =>
    api.get('/api/users/me'),

  /** 根据ID获取用户信息 */
  getUserById: (userId: number) =>
    api.get(`/api/users/${userId}`),

  /** 搜索可发起私信的用户 */
  searchUsers: (params: { keyword: string; size?: number }) =>
    api.get('/api/users/search', { params }),

  /** 更新个人资料 */
  updateProfile: (data: any) =>
    api.put('/api/users/me/profile', data),

  /** 获取当前用户能力雷达数据 */
  getAbilities: async () => {
    const response: any = await api.get('/api/users/me/abilities')
    return response?.data ?? response
  },

  /** 获取指定用户能力雷达数据 */
  getAbilitiesById: async (userId: number) => {
    const response: any = await api.get(`/api/users/${userId}/abilities`)
    return response?.data ?? response
  },

  /** 获取当前用户提交记录 */
  getSubmissions: (params?: { page?: number; size?: number; problemId?: number }) =>
    api.get('/api/users/me/submissions', { params }),

  /** 获取当前用户的竞赛记录（合并参与与创建） */
  getContestRecords: async () => {
    const [joinedRes, createdRes] = await Promise.all([
      contestApi.listMyContests({ filter: 'joined', page: 1, size: 100 }),
      contestApi.listMyContests({ filter: 'created', page: 1, size: 100 })
    ])

    const joinedPage = joinedRes.data?.data || joinedRes.data || joinedRes
    const createdPage = createdRes.data?.data || createdRes.data || createdRes
    const joinedRecords = (joinedPage.records || []) as any[]
    const createdRecords = (createdPage.records || []) as any[]
    const merged = [...joinedRecords, ...createdRecords]
    const uniqueMap = new Map<number, any>()

    merged.forEach((item) => {
      if (item?.id != null && !uniqueMap.has(item.id)) {
        uniqueMap.set(item.id, item)
      }
    })

    return Array.from(uniqueMap.values())
  },

  /** 修改密码 */
  changePassword: (currentPassword: string, newPassword: string) =>
    api.put('/api/users/me/password', { currentPassword, newPassword }),

  /** 发送换绑邮箱验证码 */
  sendChangeEmailCode: (newEmail: string, currentPassword: string) =>
    api.post('/api/users/me/send-email-code', { newEmail: normalizeEmail(newEmail), currentPassword }),

  /** 换绑邮箱 */
  changeEmail: (currentPassword: string, newEmail: string, verificationCode: string) =>
    api.put('/api/users/me/email', { currentPassword, newEmail: normalizeEmail(newEmail), verificationCode }),

  /** 上传头像 */
  uploadAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('avatar', file)
    return api.post('/api/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /** 提交角色升级申请 */
  submitUpgradeRequest: (reason: string) =>
    api.post('/api/users/me/upgrade-request', { reason }),

  /** 查询角色升级申请状态 */
  getUpgradeStatus: () =>
    api.get('/api/users/me/upgrade-status'),

  /** 获取个性化推荐题目 */
  getRecommendations: (size = 6) =>
    api.get('/api/users/me/recommend', { params: { size } })
}
