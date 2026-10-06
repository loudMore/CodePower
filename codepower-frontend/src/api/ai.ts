/** AI 智能辅助 API — 对话、题目分析、AI出题、AI题解 */
import api from './index'

type ChatStreamPayload = {
  conversationId: number
  message: string
  model?: string
  userCode?: string
  language?: string
  testContext?: string
}

type ChatStreamHandlers = {
  onOpen?: (headers: Headers) => void
  onState?: (message: string) => void
  onDelta?: (content: string) => void
  onDone?: (data: any) => void
  onError?: (message: string) => void
}

// 解析后端返回的 SSE 数据块：AI 流式回答会分成 state/delta/done/error 多种事件。
const parseSseBlock = (block: string) => {
  const event = block.split('\n').find(line => line.startsWith('event:'))?.replace(/^event:\s*/, '').trim() || 'message'
  const dataText = block
    .split('\n')
    .filter(line => line.startsWith('data:'))
    .map(line => line.replace(/^data:\s?/, ''))
    .join('\n')
  let data: any = dataText
  try {
    data = dataText ? JSON.parse(dataText) : null
  } catch {}
  return { event, data }
}

export const aiApi = {
  /** 创建AI对话 */
  createConversation: (data: { title?: string; problemId?: number; type?: string }) =>
    api.post('/api/ai/conversations', data),

  /** 获取AI对话列表 */
  getConversations: (page = 1, size = 20) =>
    api.get('/api/ai/conversations', { params: { page, size } }),

  /** 获取对话的消息记录 */
  getMessages: (conversationId: number) =>
    api.get(`/api/ai/conversations/${conversationId}/messages`),

  /** 发送AI聊天消息 */
  chat: (conversationId: number, message: string, model?: string, userCode?: string, language?: string) =>
    api.post('/api/ai/chat', { conversationId, message, model, userCode, language }),

  /** 发送AI聊天消息（真实流式输出） */
  chatStream: async (payload: ChatStreamPayload, handlers: ChatStreamHandlers = {}, signal?: AbortSignal) => {
    const token = localStorage.getItem('authToken')
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(payload),
      signal
    })

    if (!response.ok || !response.body) {
      let message = `AI请求失败（${response.status}）`
      try {
        const data = await response.json()
        message = data?.message || message
      } catch {}
      throw new Error(message)
    }

    handlers.onOpen?.(response.headers)

    // fetch 原生读取流式响应，逐块解析 SSE，页面可以边生成边展示。
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    const handleBlock = (block: string) => {
      if (!block.trim()) return
      const { event, data } = parseSseBlock(block)
      if (event === 'state') {
        handlers.onState?.(data?.message || '')
      } else if (event === 'delta') {
        handlers.onDelta?.(data?.content || '')
      } else if (event === 'done') {
        handlers.onDone?.(data)
      } else if (event === 'error') {
        const message = data?.message || 'AI服务调用失败'
        handlers.onError?.(message)
        throw new Error(message)
      }
    }

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() || ''
      for (const block of blocks) {
        handleBlock(block)
      }
    }
    if (buffer.trim()) {
      handleBlock(buffer)
    }
  },

  /** 获取可用AI模型列表 */
  getModels: () =>
    api.get('/api/ai/models'),

  /** 删除AI对话 */
  deleteConversation: (conversationId: number) =>
    api.delete(`/api/ai/conversations/${conversationId}`),

  /** AI分析题目 */
  analyzeProblem: (problemId: number) =>
    api.post(`/api/ai/analyze/${problemId}`),

  /** AI生成题目（旧同步接口，保留兼容；当前出题页优先使用异步任务接口） */
  generateProblem: (data: { tags?: string; difficulty?: string; language?: string; model?: string; prompt?: string; exampleCount?: number; testCaseCount?: number; totalScore?: number }) =>
    api.post('/api/ai/generate-problem', data, { timeout: 120000 }),

  /** AI生成多道题目候选（旧同步接口，保留兼容；长耗时场景建议走 async job） */
  generateProblems: (data: { tags?: string; difficulty?: string; language?: string; model?: string; prompt?: string; exampleCount?: number; testCaseCount?: number; totalScore?: number; count?: number }) =>
    api.post('/api/ai/generate-problems', data, { timeout: 180000 }),

  /** 创建AI出题异步任务，避免长耗时请求被反代超时切断 */
  startProblemGeneration: (data: { tags?: string; difficulty?: string; language?: string; model?: string; prompt?: string; exampleCount?: number; testCaseCount?: number; totalScore?: number; count?: number }) =>
    api.post('/api/ai/generate-problems/async', data, { timeout: 30000 }),

  /** 查询AI出题异步任务结果 */
  getProblemGenerationJob: (jobId: string) =>
    api.get(`/api/ai/generate-problems/jobs/${jobId}`, { timeout: 30000 }),

  /** AI生成题解 */
  generateSolution: (problemId: number, prompt: string) =>
    api.post(`/api/ai/generate-solution/${problemId}`, { prompt }),

  /** AI生成题解草稿（出题时预览） */
  generateSolutionDraft: (data: { title?: string; difficulty?: string; description?: string; inputFormat?: string; outputFormat?: string; prompt?: string }) =>
    api.post('/api/ai/generate-solution-draft', data)
}
