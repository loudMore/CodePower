/** 文件说明：前端 API 基础实例，统一处理请求、响应、Token 和缓存。 */
import axios from 'axios';
import router from '@/router';
import { ElMessage } from 'element-plus';
import { getMemoryCache, setMemoryCache, deleteMemoryCacheByPrefix } from '@/utils/memoryCache';

/**
 * 全局唯一的 API 请求实例。
 * 开发环境通过 vite.config.ts 把 /api 代理到本地后端；线上由 OpenResty/Nginx 反向代理到 Spring Boot。
 */
const api = axios.create({
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// JWT Token 过期检查：前端先做一次轻量判断，真正权限仍以后端 Spring Security 为准。
function isTokenExpired(token: string): boolean {
  if (!token) return true;
  try {
    const payload = token.split('.')[1];
    if (!payload) return true;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);
    const decodedPayload = JSON.parse(atob(padded));
    const expTime = decodedPayload.exp * 1000;
    return Date.now() >= expTime;
  } catch (e) {
    console.error('Token解析错误:', e);
    return true;
  }
}

/**
 * 检查当前登录令牌是否有效（供路由守卫、App.vue 等使用）
 */
export const checkTokenValid = () => {
  const token = localStorage.getItem('authToken');
  return !!token && !isTokenExpired(token);
};

let isRedirectingToLogin = false;

const normalizeBearerToken = (authorization?: unknown) => {
  if (typeof authorization !== 'string') return '';
  return authorization.startsWith('Bearer ') ? authorization.substring(7) : authorization;
};

// 处理登录过期：清理本地登录态，通知顶层 App 更新头像/菜单，并带 redirect 回登录页。
const handleUnauthorized = (requestAuthorization?: unknown) => {
  const currentToken = localStorage.getItem('authToken') || '';
  const requestToken = normalizeBearerToken(requestAuthorization);
  if (requestToken && currentToken && requestToken !== currentToken && !isTokenExpired(currentToken)) {
    return;
  }
  localStorage.removeItem('authToken');
  localStorage.removeItem('userInfo');
  window.dispatchEvent(new Event('user-login'));

  if (isRedirectingToLogin) return;

  isRedirectingToLogin = true;
  ElMessage.error('登录已过期，请重新登录');
  const currentPath = router.currentRoute.value.fullPath;
  if (currentPath !== '/auth/login') {
    router.push({ path: '/auth/login', query: { redirect: currentPath } });
  }
  setTimeout(() => {
    isRedirectingToLogin = false;
  }, 2000);
};

// 请求拦截器 — 每次调用后端 API 前自动附加 Bearer Token。
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('authToken');
    if (token && !isTokenExpired(token)) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器 — 统一剥离 response.data，并处理 401/403/500 等通用错误。
api.interceptors.response.use(
  response => {
    return response.data;
  },
  error => {
    const skipGlobalError = (error.config as any)?.skipGlobalError
    if (error.response) {
      switch (error.response.status) {
        case 401:
          handleUnauthorized(error.config?.headers?.Authorization);
          break;
        case 403:
          if (!skipGlobalError) ElMessage.error('您没有权限执行此操作');
          break;
        case 500:
          console.error('服务器错误:', error.config?.url);
          if (!skipGlobalError) ElMessage.error(error.response.data?.message || '服务器开小差了');
          break;
        default:
          if (!skipGlobalError) ElMessage.error(error.response.data?.message || '请求失败');
          break;
      }
    } else {
      if (!skipGlobalError) ElMessage.error('网络错误，请检查网络连接');
    }
    return Promise.reject(error);
  }
);

// 题目相关 API — 做题、出题、评测提交的主入口。
export const problemApi = {
  getProblems: (params: any) => api.get('/api/problems', { params }),
  getProblemById: (id: number) => api.get(`/api/problems/${id}`),
  getProblemForSolving: (id: number) => api.get(`/api/problems/${id}/view`),
  getProblemsByAuthor: (authorId: number) => api.get(`/api/problems/author/${authorId}`),
  createProblem: (data: any) => api.post('/api/problems', data),
  updateProblem: (id: number, data: any) => api.put(`/api/problems/${id}`, data),
  deleteProblem: (id: number) => api.delete(`/api/problems/${id}`),
  testCode: (problemId: number, data: any) => api.post(`/api/code/test/${problemId}`, data),
  submitCode: (problemId: number, data: any) => api.post(`/api/code/submit/${problemId}`, data),
  submitCodeAsync: (problemId: number, data: any) => api.post(`/api/code/submit/${problemId}/async`, data)
};

// 题解相关 API — 官方题解、社区题解和 AC 绑定题解发布。
export const solutionApi = {
  getSolutions: (problemId: number) => api.get(`/api/solutions/problem/${problemId}`),
  getOfficialSolutions: (problemId: number) => api.get(`/api/solutions/problem/${problemId}/official`),
  getOfficialSolutionCode: (problemId: number) => api.get(`/api/solutions/problem/${problemId}/official-code`),
  getCategorizedSolutions: (problemId: number) => api.get(`/api/solutions/problem/${problemId}/categorized`),
  createSolution: (data: any) => api.post('/api/solutions', data)
};

// ========== GET 请求内存缓存 ==========
const DEFAULT_CACHE_TTL = 30_000 // 默认缓存 30 秒

/**
 * 带内存缓存的 GET 请求。
 * 相同 URL + params 在 TTL 内直接返回缓存，避免重复网络请求。
 */
export async function cachedGet<T = any>(
  url: string,
  params?: Record<string, any>,
  ttlMs: number = DEFAULT_CACHE_TTL
): Promise<T> {
  const cacheKey = `api:get:${url}:${JSON.stringify(params ?? {})}`
  const cached = getMemoryCache<T>(cacheKey)
  if (cached !== null) return cached
  const result = await api.get(url, { params }) as T
  setMemoryCache(cacheKey, result, ttlMs)
  return result
}

/**
 * 按 URL 模式清除缓存（支持前缀匹配）。
 * 例如 invalidateCache('/api/problems') 会清除所有以该路径开头的缓存。
 */
export function invalidateCache(pattern: string): void {
  deleteMemoryCacheByPrefix(`api:get:${pattern}`)
}

export default api;
