/** 文件说明：第三方授权登录接口，处理 GitHub/Gitee 回调、绑定和注册。 */
import api from './index'

/** 第三方授权模式：login 表示授权登录，bind 表示在个人中心绑定账号。 */
export type OAuthMode = 'login' | 'bind'

/** 授权平台状态：用于登录页和账号设置页显示 GitHub/Gitee/QQ/微信是否可用、是否已绑定。 */
export interface OAuthProviderStatus {
  provider: string
  label: string
  enabled: boolean
  configured: boolean
  available: boolean
  message: string
  bound?: boolean
  providerUsername?: string
  providerNickname?: string
  providerAvatar?: string
  providerEmail?: string
  boundAt?: string
  lastLoginAt?: string
}

/**
 * OAuth 授权接口 — 第三方平台只负责确认外部身份，最终仍由本系统完成账号绑定和邮箱验证。
 * 未绑定的授权账号会进入“已有账号绑定 / 新账号注册”流程，而不是直接登录。
 */
export const oauthApi = {
  /** 获取各授权平台展示状态：是否启用、是否配置、当前用户是否已绑定。 */
  getProviders: () =>
    api.get('/api/oauth/providers'),

  /** 获取第三方平台授权地址，前端拿到 URL 后跳转到 GitHub/Gitee 等平台。 */
  getAuthorizeUrl: (provider: string, mode: OAuthMode, redirect = '/') =>
    api.post(`/api/oauth/${provider}/authorize-url`, { mode, redirect }),

  /** 授权回调后消费一次性登录会话，成功时拿到本系统 Token 和用户信息。 */
  consumeLoginSession: (loginId: string) =>
    api.get(`/api/oauth/login-sessions/${loginId}`),

  /** 授权平台未绑定账号时，读取后端临时绑定会话中的外部账号信息。 */
  getLinkSession: (sessionId: string) =>
    api.get(`/api/oauth/link-sessions/${sessionId}`),

  /** 已有账号绑定：用户输入本系统账号密码，通过后把外部身份绑定到该账号。 */
  bindExisting: (sessionId: string, data: { account: string; password: string }) =>
    api.post(`/api/oauth/link-sessions/${sessionId}/bind-existing`, data),

  /** 新账号注册并绑定：沿用注册逻辑，必须填写邮箱并完成邮箱验证码校验。 */
  createAccount: (sessionId: string, data: {
    username: string
    email: string
    region?: string
    verificationCode: string
    password: string
    confirmPassword?: string
  }) =>
    api.post(`/api/oauth/link-sessions/${sessionId}/create-account`, data),

  /** 个人中心查询当前账号已经绑定的第三方平台。 */
  getMyBindings: () =>
    api.get('/api/oauth/me/bindings'),

  /** 解绑指定第三方平台，解绑后该外部账号不能再直接登录本系统。 */
  unlink: (provider: string) =>
    api.delete(`/api/oauth/me/bindings/${provider}`)
}
