/** 文件说明：账号认证接口，封装注册、登录、邮箱验证码和人机验证请求。 */
import api from './index';

const normalizeEmail = (email: string) => String(email || '').trim().toLowerCase();

/**
 * 账号认证接口 — 注册、登录、邮箱验证码、密码找回和登录前编程题验证。
 * 这里的函数只负责把页面表单数据发给后端，Token 保存和 401 跳转由 api/index.ts 统一处理。
 */

/**
 * 用户注册：提交用户名、邮箱、密码、地区和邮箱验证码。
 * @param {object} registrationData - 注册页整理出的表单数据
 */
export const registerUser = (registrationData: object) => {
  const payload: any = { ...(registrationData as any) };
  if (payload.email) {
    payload.email = normalizeEmail(payload.email);
  }
  return api.post('/api/auth/register', payload);
};

/**
 * 发送注册邮箱验证码：注册和第三方授权创建新账号都会走邮箱验证。
 * @param {string} email - 目标邮箱地址
 */
export const sendEmailCode = (email: string) => {
  return api.post('/api/auth/send-code', { email: normalizeEmail(email) });
};

/**
 * 用户登录：账号密码验证前必须先通过编程题人机验证。
 * @param {object} loginData - 包含账号、密码和 verificationToken
 */
export const loginUser = (loginData: object) => {
  const payload: any = { ...(loginData as any) };
  if (payload.username && String(payload.username).includes('@')) {
    payload.username = normalizeEmail(payload.username);
  }
  return api.post('/api/auth/login', payload);
};

/**
 * 请求密码重置：先校验登录前编程题，再向邮箱发送重置验证码。
 * @param {string} email - 用户邮箱
 * @param {string} verificationToken - 交互式验证成功后获得的票据
 */
export const requestPasswordReset = (email: string, verificationToken: string) => {
  return api.post('/api/auth/request-password-reset', { email: normalizeEmail(email), verificationToken });
};

/**
 * 使用邮箱验证码重置密码。
 * @param {object} resetData - 包含邮箱、验证码和新密码
 */
export const resetPassword = (resetData: object) => {
  const payload: any = { ...(resetData as any) };
  if (payload.email) {
    payload.email = normalizeEmail(payload.email);
  }
  return api.post('/api/auth/reset-password', payload);
};

/**
 * 检查用户名是否已存在
 * @param {string} username - 要检查的用户名
 */
export const checkUsernameExists = (username: string) => {
  return api.get(`/api/auth/check-username?username=${encodeURIComponent(username)}`);
};

/**
 * 检查邮箱是否已被注册
 * @param {string} email - 要检查的邮箱
 */
export const checkEmailExists = (email: string) => {
  return api.get(`/api/auth/check-email?email=${encodeURIComponent(normalizeEmail(email))}`);
};

// ===================================================================
// 交互式人机验证接口
// ===================================================================

/**
 * 获取登录前编程题验证挑战：后端返回题目、选项和 challengeId。
 */
export const getVerificationChallenge = async () => {
  try {
    const response = await api.get('/api/verification/challenge');
    console.log('验证响应原始数据:', response);
    // 直接返回 response，因为 api 拦截器已经提取了 data 部分。
    return response;
  } catch (error) {
    console.error('获取验证码挑战失败:', error);
    throw error;
  }
};

/**
 * 提交编程题验证答案：成功后返回 verificationToken，供登录或找回密码接口使用。
 * @param {string} challengeId - 挑战ID
 * @param {string[]} selection - 用户选择的答案数组
 */
export const submitVerification = async (challengeId: string, selection: string[]) => {
  try {
    const response = await api.post('/api/verification/verify', { challengeId, selection });
    // 直接返回 response，因为 api 拦截器已经提取了 data 部分。
    return response;
  } catch (error) {
    console.error('提交验证答案失败:', error);
    throw error;
  }
}; 
