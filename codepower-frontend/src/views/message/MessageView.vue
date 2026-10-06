<!-- 信箱页面 — 私信、系统消息、关注/粉丝的统一入口 -->
<template>
  <div class="message-container">
    <el-row :gutter="0" class="message-layout">
      <!-- 左侧：标签切换 + 列表 -->
      <el-col :span="8" class="conversation-panel">
        <div class="panel-header">
          <el-radio-group v-model="leftTab" size="small">
            <el-radio-button value="chat">私信</el-radio-button>
            <el-radio-button value="following">关注</el-radio-button>
            <el-radio-button value="followers">粉丝</el-radio-button>
          </el-radio-group>
          <el-button size="small" type="primary" plain @click="openStartChatDialog">发私信</el-button>
        </div>

        <!-- 会话列表 -->
        <div v-show="leftTab === 'chat'" v-loading="convLoading" class="conversation-list">
          <!-- 系统消息固定在顶部 -->
          <div class="conv-item" :class="{ active: isSystemChat }" @click="openSystemChat">
            <el-avatar :size="40" style="background: #e6a23c; font-size: 18px;">系统</el-avatar>
            <div class="conv-info">
              <div class="conv-name">
                系统消息
                <el-badge v-if="systemUnread > 0" :value="systemUnread" :max="99" class="conv-badge" />
              </div>
              <div class="conv-preview">通知、评论回复等系统消息</div>
            </div>
          </div>
          <div v-if="conversations.length === 0" class="empty-conv" style="padding: 20px 0;">
            <div style="text-align: center; color: #909399; font-size: 13px;">暂无私信对话</div>
            <div style="text-align: center; margin-top: 10px;">
              <el-button size="small" type="primary" @click="openStartChatDialog">搜索用户发起聊天</el-button>
            </div>
          </div>
          <div v-for="conv in conversations" :key="conv.userId" class="conv-item"
               :class="{ active: activeUserId === conv.userId }" @click="selectConversation(conv)">
            <el-avatar
              :size="40"
              :src="conv.avatar || '/avatars/avatar-1.svg'"
              class="clickable-avatar"
              @click.stop="goToUserProfile(conv.userId)"
            />
            <div class="conv-info">
              <div class="conv-name">
                {{ conv.username }}
                <el-badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" :max="99" class="conv-badge" />
              </div>
              <div class="conv-preview">{{ conv.lastMessage }}</div>
            </div>
            <div class="conv-time">{{ formatTime(conv.lastMessageTime) }}</div>
          </div>
        </div>

        <!-- 关注列表 -->
        <div v-show="leftTab === 'following'" v-loading="followListLoading" class="conversation-list">
          <div v-if="followingList.length === 0" class="empty-conv">
            <el-empty description="还没有关注任何人" :image-size="60" />
          </div>
          <div v-for="u in followingList" :key="u.userId" class="conv-item"
               @click="openChatWith(u)">
            <el-avatar
              :size="40"
              :src="u.avatar || '/avatars/avatar-1.svg'"
              class="clickable-avatar"
              @click.stop="goToUserProfile(u.userId)"
            />
            <div class="conv-info">
              <div class="conv-name">
                {{ u.username }}
                <el-tag v-if="u.isMutual" size="small" type="success" class="mutual-tag">互关</el-tag>
              </div>
              <div class="conv-preview">Lv.{{ u.level || 0 }}</div>
            </div>
            <el-button size="small" text type="danger" @click.stop="handleUnfollow(u)">取消关注</el-button>
          </div>
        </div>

        <!-- 粉丝列表 -->
        <div v-show="leftTab === 'followers'" v-loading="followListLoading" class="conversation-list">
          <div v-if="followerList.length === 0" class="empty-conv">
            <el-empty description="还没有粉丝" :image-size="60" />
          </div>
          <div v-for="u in followerList" :key="u.userId" class="conv-item"
               @click="openChatWith(u)">
            <el-avatar
              :size="40"
              :src="u.avatar || '/avatars/avatar-1.svg'"
              class="clickable-avatar"
              @click.stop="goToUserProfile(u.userId)"
            />
            <div class="conv-info">
              <div class="conv-name">
                {{ u.username }}
                <el-tag v-if="u.isMutual" size="small" type="success" class="mutual-tag">互关</el-tag>
              </div>
              <div class="conv-preview">Lv.{{ u.level || 0 }}</div>
            </div>
            <el-button v-if="!u.isMutual" size="small" type="primary" @click.stop="handleFollowBack(u)">回关</el-button>
            <el-tag v-else size="small" type="info">已互关</el-tag>
          </div>
        </div>
      </el-col>

      <!-- 聊天区域 -->
      <el-col :span="16" class="chat-panel">
        <!-- 系统消息视图 -->
        <template v-if="isSystemChat">
          <div class="chat-header">
            <span>系统消息</span>
            <el-button size="small" text type="primary" @click="markAllNotificationsRead">全部已读</el-button>
          </div>
          <div class="system-readonly-hint">
            系统消息用于接收平台通知，不能直接回复；需要聊天请点击左侧“发私信”。
          </div>
          <div ref="chatBodyRef" class="chat-body" v-loading="chatLoading">
            <div v-for="n in notifications" :key="n.id" class="chat-message system-msg">
              <div class="msg-bubble system-bubble">
                <div class="system-title">{{ n.title }}</div>
                <div v-if="n.content" class="system-content">{{ n.content }}</div>
              </div>
              <div class="msg-time">{{ formatFullTime(n.createdAt) }}</div>
            </div>
            <el-empty v-if="notifications.length === 0" description="暂无系统消息" />
          </div>
        </template>
        <!-- 普通私信视图 -->
        <template v-else-if="activeUserId">
          <div class="chat-header">
            <span>{{ activeUsername }}</span>
            <div class="chat-header-actions">
              <el-tag v-if="followStatus.isMutual" size="small" type="success">互相关注</el-tag>
              <el-tag v-else-if="followStatus.isFollowing" size="small">已关注</el-tag>
              <el-button v-if="!followStatus.isFollowing" size="small" type="primary" @click="handleFollow">关注</el-button>
              <el-button v-else size="small" text type="info" @click="handleUnfollowChat">取消关注</el-button>
            </div>
          </div>
          <div ref="chatBodyRef" class="chat-body" v-loading="chatLoading">
            <div v-if="messageSendError" class="msg-limit-hint">
              <el-alert type="warning" :closable="true" @close="messageSendError = ''">
                {{ messageSendError }}
              </el-alert>
            </div>
            <div v-else-if="currentUserRole !== 'ADMIN' && !followStatus.isMutual && !hasOtherReplied" class="msg-limit-hint">
              <el-alert type="info" :closable="false">
                对方未回复你，最多可发送3条消息。对方回复或互相关注后可无限聊天。
              </el-alert>
            </div>
            <div v-for="msg in messages" :key="msg.id" class="chat-message"
                 :class="{ mine: msg.fromUserId === currentUserId }">
              <div class="msg-bubble">{{ msg.content }}</div>
              <div class="msg-time">{{ formatFullTime(msg.createdAt) }}</div>
            </div>
          </div>
          <div class="chat-input">
            <el-input v-model="inputText" type="textarea" :rows="2" placeholder="输入消息... (Ctrl+Enter 发送)" resize="none"
                      @keyup.ctrl.enter="sendMessage" />
            <el-button type="primary" :disabled="!inputText.trim()" :loading="sending" @click="sendMessage">发送</el-button>
          </div>
        </template>
        <template v-else>
          <div class="no-chat">
            <el-empty description="选择一个对话开始聊天，或搜索用户发起私信">
              <el-button type="primary" @click="openStartChatDialog">发起私信</el-button>
            </el-empty>
          </div>
        </template>
      </el-col>
    </el-row>

    <el-dialog v-model="startChatDialogVisible" title="发起私信" width="520px">
      <el-input
        v-model="userSearchKeyword"
        clearable
        placeholder="输入用户名搜索，例如 lisi"
        @input="scheduleUserSearch"
        @keyup.enter="searchChatUsers"
      />
      <div class="user-search-list" v-loading="userSearchLoading">
        <el-empty
          v-if="!userSearchLoading && userSearchResults.length === 0"
          :description="userSearchKeyword.trim() ? '没有找到匹配用户' : '输入用户名后即可搜索并发起聊天'"
          :image-size="64"
        />
        <div v-for="u in userSearchResults" :key="u.userId" class="user-search-item" @click="startChatWithUser(u)">
          <el-avatar
            :size="36"
            :src="u.avatar || '/avatars/avatar-1.svg'"
            class="clickable-avatar"
            @click.stop="goToUserProfile(u.userId)"
          />
          <div class="user-search-info">
            <strong>{{ u.username }}</strong>
            <span>Lv.{{ u.level || 0 }} · {{ formatRole(u.role) }}</span>
          </div>
          <el-button size="small" type="primary" text>聊天</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, onActivated, onBeforeUnmount, onDeactivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api/message'
import { followApi } from '@/api/follow'
import { notificationApi } from '@/api/notification'
import { userApi } from '@/api/user'
import { getMemoryCache, setMemoryCache, deleteMemoryCache } from '@/utils/memoryCache'
import { apiDateTimeMs, formatApiDate, formatApiDateTime, formatRelativeTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const MESSAGE_LIST_CACHE_TTL = 30_000
const MESSAGE_DETAIL_CACHE_TTL = 30_000
const MESSAGE_POLL_INTERVAL = 10000
const MESSAGE_SOCKET_RECONNECT_DELAY = 3000
const MESSAGE_PAGE_SIZE = 50
const emojiList = ['😀', '😁', '😂', '😊', '😍', '😎', '😭', '😡', '👍', '🙏', '🎉', '❤️', '🔥', '💡', '✅', '❌', '🤔', '🥳', '😅', '😴']
const conversations = ref<any[]>([])
const messages = ref<any[]>([])
const activeUserId = ref<number | null>(null)
const activeUsername = ref('')
const inputText = ref('')
const convLoading = ref(false)
const chatLoading = ref(false)
const sending = ref(false)
const chatBodyRef = ref<HTMLElement>()
const currentUserId = ref(0)
const currentUserRole = ref('')
const leftTab = ref('chat')
const followListLoading = ref(false)
const followingList = ref<any[]>([])
const followerList = ref<any[]>([])
const followStatus = ref<{ isFollowing: boolean; isMutual: boolean }>({ isFollowing: false, isMutual: false })
const hasOtherReplied = ref(false)
const messageSendError = ref('')
const isSystemChat = ref(false)
const notifications = ref<any[]>([])
const systemUnread = ref(0)
const messagePage = ref(1)
const messageHasMore = ref(false)
const loadingOlderMessages = ref(false)
const startChatDialogVisible = ref(false)
const userSearchKeyword = ref('')
const userSearchResults = ref<any[]>([])
const userSearchLoading = ref(false)
const realtimeConnected = ref(false)
let userSearchTimer: number | undefined
let messagePollTimer: number | undefined
let messageSocket: WebSocket | null = null
let messageSocketReconnectTimer: number | undefined
let messageSocketManualClose = false
let lastMessageSocketToken = ''

const isPageVisible = () => document.visibilityState === 'visible'

const emitUnreadChanged = () => {
  window.dispatchEvent(new Event('message-center-updated'))
}

const handleMessageCenterUpdated = async () => {
  await Promise.all([loadConversations(true, true), loadSystemUnread()])
}

onMounted(async () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr)
      currentUserId.value = userInfo.id
      currentUserRole.value = userInfo.role || ''
    } catch {}
  }
  await loadConversations()
  await loadSystemUnread()
  await syncConversationFromRoute()
  startMessagePolling()
  connectMessageSocket()
  window.addEventListener('user-login', handleUserLoginStateChanged)
})

onBeforeUnmount(() => {
  stopMessagePolling()
  disconnectMessageSocket()
  window.removeEventListener('message-center-updated', handleMessageCenterUpdated)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('user-login', handleUserLoginStateChanged)
  if (userSearchTimer) window.clearTimeout(userSearchTimer)
})

onActivated(async () => {
  window.addEventListener('message-center-updated', handleMessageCenterUpdated)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  startMessagePolling()
  connectMessageSocket()
  await loadConversations(true, true)
  await loadSystemUnread()
  await syncConversationFromRoute()
  if (!route.params.userId && activeUserId.value && !isSystemChat.value) {
    await loadMessages(activeUserId.value, true, { silent: true, keepPosition: true })
  }
})

onDeactivated(() => {
  stopMessagePolling()
  window.removeEventListener('message-center-updated', handleMessageCenterUpdated)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

watch(() => route.params.userId, async () => {
  await loadConversations(true, true)
  await syncConversationFromRoute()
})

watch(leftTab, async (tab) => {
  if (tab === 'following') await loadFollowingList()
  else if (tab === 'followers') await loadFollowerList()
})

const getCurrentToken = () => localStorage.getItem('authToken') || ''

const buildMessageSocketUrl = () => {
  const token = getCurrentToken()
  if (!token) return ''
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/messages?token=${encodeURIComponent(token)}`
}

const connectMessageSocket = () => {
  const token = getCurrentToken()
  if (!token) {
    disconnectMessageSocket()
    return
  }

  const isAlive = messageSocket
    && (messageSocket.readyState === WebSocket.OPEN || messageSocket.readyState === WebSocket.CONNECTING)
    && lastMessageSocketToken === token
  if (isAlive) return

  messageSocketManualClose = false
  if (messageSocketReconnectTimer !== undefined) {
    window.clearTimeout(messageSocketReconnectTimer)
    messageSocketReconnectTimer = undefined
  }
  closeCurrentMessageSocket()

  const url = buildMessageSocketUrl()
  if (!url) return
  lastMessageSocketToken = token

  const socket = new WebSocket(url)
  messageSocket = socket
  socket.onopen = () => {
    if (messageSocket !== socket) return
    realtimeConnected.value = true
  }
  socket.onmessage = (event) => {
    void handleRealtimeEvent(event.data)
  }
  socket.onclose = () => {
    if (messageSocket !== socket) return
    messageSocket = null
    realtimeConnected.value = false
    if (!messageSocketManualClose && getCurrentToken()) {
      scheduleMessageSocketReconnect()
    }
  }
  socket.onerror = () => {
    realtimeConnected.value = false
    if (messageSocket === socket && socket.readyState !== WebSocket.CLOSED) {
      socket.close()
    }
  }
}

const scheduleMessageSocketReconnect = () => {
  if (messageSocketReconnectTimer !== undefined) return
  messageSocketReconnectTimer = window.setTimeout(() => {
    messageSocketReconnectTimer = undefined
    connectMessageSocket()
  }, MESSAGE_SOCKET_RECONNECT_DELAY)
}

const disconnectMessageSocket = () => {
  messageSocketManualClose = true
  if (messageSocketReconnectTimer !== undefined) {
    window.clearTimeout(messageSocketReconnectTimer)
    messageSocketReconnectTimer = undefined
  }
  closeCurrentMessageSocket()
  lastMessageSocketToken = ''
}

const closeCurrentMessageSocket = () => {
  const socket = messageSocket
  messageSocket = null
  realtimeConnected.value = false
  if (!socket) return
  socket.onopen = null
  socket.onmessage = null
  socket.onclose = null
  socket.onerror = null
  if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
    socket.close()
  }
}

const handleUserLoginStateChanged = () => {
  if (getCurrentToken()) {
    connectMessageSocket()
  } else {
    disconnectMessageSocket()
  }
}

const startMessagePolling = () => {
  if (messagePollTimer !== undefined) return
  messagePollTimer = window.setInterval(() => {
    void refreshMessageCenterSilently()
  }, MESSAGE_POLL_INTERVAL)
}

const stopMessagePolling = () => {
  if (messagePollTimer !== undefined) {
    window.clearInterval(messagePollTimer)
    messagePollTimer = undefined
  }
}

const handleVisibilityChange = () => {
  if (isPageVisible()) {
    void refreshMessageCenterSilently()
  }
}

const refreshMessageCenterSilently = async () => {
  if (!isPageVisible()) return
  await loadConversations(true, true)
  if (leftTab.value === 'following') {
    await loadFollowingList(true)
  } else if (leftTab.value === 'followers') {
    await loadFollowerList(true)
  }
  if (isSystemChat.value) {
    await loadSystemUnread()
    await loadSystemMessages(true, true, false)
    return
  }
  if (activeUserId.value) {
    await loadMessages(activeUserId.value, true, { silent: true, keepPosition: true })
    try {
      await messageApi.markAsRead(activeUserId.value)
    } catch {}
  } else {
    await loadSystemUnread()
  }
}

const loadConversations = async (force = false, silent = false) => {
  if (!silent) convLoading.value = true
  try {
    const cacheKey = 'message:conversations'
    if (!force) {
      const cached = getMemoryCache<any[]>(cacheKey)
      if (cached) {
        conversations.value = cached
        return
      }
    }
    const res: any = await messageApi.getConversations()
    conversations.value = res.data || res || []
    setMemoryCache(cacheKey, conversations.value, MESSAGE_LIST_CACHE_TTL)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载会话失败')
  } finally {
    if (!silent) convLoading.value = false
  }
}

const syncConversationFromRoute = async () => {
  const routeUserId = Number(route.params.userId)
  if (!Number.isFinite(routeUserId) || routeUserId <= 0) return

  isSystemChat.value = false
  leftTab.value = 'chat'
  const existing = conversations.value.find(c => c.userId === routeUserId)
  if (existing) {
    await selectConversation(existing)
    return
  }

  activeUserId.value = routeUserId
  activeUsername.value = '用户 #' + routeUserId
  await loadMessages(routeUserId, true)
  await loadFollowStatus(routeUserId)
}

const selectConversation = async (conv: any) => {
  isSystemChat.value = false
  messageSendError.value = ''
  activeUserId.value = conv.userId
  activeUsername.value = conv.username
  await loadMessages(conv.userId, true)
  await loadFollowStatus(conv.userId)
  if (conv.unreadCount > 0) {
    await messageApi.markAsRead(conv.userId)
    conv.unreadCount = 0
    emitUnreadChanged()
  }
}

const openChatWith = async (u: any) => {
  isSystemChat.value = false
  messageSendError.value = ''
  activeUserId.value = u.userId
  activeUsername.value = u.username
  leftTab.value = 'chat'
  if (route.params.userId !== String(u.userId)) {
    router.replace(`/messages/${u.userId}`)
  }
  await loadMessages(u.userId, true)
  await loadFollowStatus(u.userId)
}

const openStartChatDialog = () => {
  startChatDialogVisible.value = true
  if (!userSearchKeyword.value.trim()) {
    userSearchResults.value = []
  }
}

const scheduleUserSearch = () => {
  if (userSearchTimer) window.clearTimeout(userSearchTimer)
  userSearchTimer = window.setTimeout(() => {
    searchChatUsers()
  }, 300)
}

const searchChatUsers = async () => {
  const keyword = userSearchKeyword.value.trim()
  if (!keyword) {
    userSearchResults.value = []
    return
  }
  userSearchLoading.value = true
  try {
    const res: any = await userApi.searchUsers({ keyword, size: 10 })
    userSearchResults.value = res.data || res || []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '搜索用户失败')
  } finally {
    userSearchLoading.value = false
  }
}

const startChatWithUser = async (u: any) => {
  startChatDialogVisible.value = false
  await openChatWith(u)
}

const loadMessages = async (
  otherUserId: number,
  force = false,
  options: { silent?: boolean; keepPosition?: boolean } = {}
) => {
  const shouldKeepPosition = options.keepPosition ?? false
  const shouldScrollToBottom = !shouldKeepPosition || isChatNearBottom()
  if (!options.silent) chatLoading.value = true
  try {
    const cacheKey = `message:conversation:${otherUserId}`
    if (!force) {
      const cached = getMemoryCache<any[]>(cacheKey)
      if (cached) {
        messages.value = cached
        hasOtherReplied.value = cached.some((m: any) => m.fromUserId === otherUserId)
        await nextTick()
        if (shouldScrollToBottom) scrollToBottom()
        return
      }
    }
    const res: any = await messageApi.getConversation(otherUserId)
    const d = res.data || res
    const records = d.records || []
    messages.value = records.reverse()
    hasOtherReplied.value = records.some((m: any) => m.fromUserId === otherUserId)
    setMemoryCache(cacheKey, messages.value, MESSAGE_DETAIL_CACHE_TTL)
    await nextTick()
    if (shouldScrollToBottom) scrollToBottom()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载消息失败')
  } finally {
    if (!options.silent) chatLoading.value = false
  }
}

const appendMessageIfMissing = (message: any, peerUserId?: number) => {
  if (!message || message.id == null) return false
  const normalizedId = Number(message.id)
  if (messages.value.some((item: any) => Number(item.id) === normalizedId)) {
    return false
  }

  messages.value.push(message)
  messages.value.sort((a: any, b: any) => {
    const left = apiDateTimeMs(a.createdAt)
    const right = apiDateTimeMs(b.createdAt)
    if (left !== right) return left - right
    return Number(a.id || 0) - Number(b.id || 0)
  })
  const cacheKey = `message:conversation:${peerUserId ?? activeUserId.value ?? 0}`
  if (peerUserId || activeUserId.value) {
    setMemoryCache(cacheKey, messages.value, MESSAGE_DETAIL_CACHE_TTL)
  }
  return true
}

const handleRealtimeEvent = async (rawData: string) => {
  let payload: any
  try {
    payload = JSON.parse(rawData)
  } catch {
    return
  }

  if (!payload || payload.type !== 'private_message' || !payload.message) {
    return
  }

  const message = payload.message
  const peerUserId = Number(payload.peerUserId || (Number(message.fromUserId) === Number(currentUserId.value) ? message.toUserId : message.fromUserId))
  if (!peerUserId) {
    return
  }

  deleteMemoryCache('message:conversations')
  deleteMemoryCache(`message:conversation:${peerUserId}`)

  const isCurrentConversation = !isSystemChat.value && Number(activeUserId.value) === peerUserId
  const shouldScroll = isCurrentConversation && isChatNearBottom()

  if (isCurrentConversation) {
    appendMessageIfMissing(message, peerUserId)
    hasOtherReplied.value = messages.value.some((item: any) => Number(item.fromUserId) === peerUserId)
    await nextTick()
    if (shouldScroll) {
      scrollToBottom()
    }
    if (Number(message.fromUserId) !== Number(currentUserId.value)) {
      try {
        await messageApi.markAsRead(peerUserId)
      } catch {}
    }
  }

  await loadConversations(true, true)
  emitUnreadChanged()
}

const loadFollowStatus = async (userId: number) => {
  try {
    const res: any = await followApi.checkFollow(userId)
    followStatus.value = res.data || res || { isFollowing: false, isMutual: false }
  } catch {
    followStatus.value = { isFollowing: false, isMutual: false }
  }
}

const sendMessage = async () => {
  if (!inputText.value.trim() || !activeUserId.value) return
  sending.value = true
  messageSendError.value = ''
  const content = inputText.value.trim()
  try {
    const res: any = await messageApi.sendMessage(activeUserId.value, content)
    const data = res.data || res
    const savedMessage = data?.id ? data : data?.data || data
    if (savedMessage?.id) {
      appendMessageIfMissing(savedMessage, activeUserId.value)
    }
    inputText.value = ''
    emitUnreadChanged()
    await nextTick()
    scrollToBottom()
    deleteMemoryCache('message:conversations')
    await loadConversations(true, true)
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.data?.message || '发送失败'
    messageSendError.value = msg
  } finally { sending.value = false }
}

const handleFollow = async () => {
  if (!activeUserId.value) return
  try {
    await followApi.follow(activeUserId.value)
    await loadFollowStatus(activeUserId.value)
    ElMessage.success('关注成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '关注失败')
  }
}

const handleUnfollowChat = async () => {
  if (!activeUserId.value) return
  try {
    await followApi.unfollow(activeUserId.value)
    await loadFollowStatus(activeUserId.value)
    ElMessage.success('已取消关注')
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleUnfollow = async (u: any) => {
  try {
    await followApi.unfollow(u.userId)
    u.isMutual = false
    await loadFollowingList()
    ElMessage.success('已取消关注')
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleFollowBack = async (u: any) => {
  try {
    await followApi.follow(u.userId)
    u.isMutual = true
    ElMessage.success('已回关')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '关注失败')
  }
}

const loadFollowingList = async (silent = false) => {
  if (!silent) followListLoading.value = true
  try {
    const res: any = await followApi.getFollowing({ size: 100 })
    followingList.value = res.data || res || []
  } catch {
    ElMessage.error('加载关注列表失败')
  } finally {
    if (!silent) followListLoading.value = false
  }
}

const loadFollowerList = async (silent = false) => {
  if (!silent) followListLoading.value = true
  try {
    const res: any = await followApi.getFollowers({ size: 100 })
    followerList.value = res.data || res || []
  } catch {
    ElMessage.error('加载粉丝列表失败')
  } finally {
    if (!silent) followListLoading.value = false
  }
}

const loadSystemMessages = async (force = false, silent = false, markRead = false) => {
  isSystemChat.value = true
  activeUserId.value = null
  activeUsername.value = ''
  if (!silent) chatLoading.value = true
  try {
    const cacheKey = 'message:system'
    const cached = !force && !silent ? getMemoryCache<any[]>(cacheKey) : null
    if (cached) {
      notifications.value = cached
    } else {
      const res: any = await notificationApi.getNotifications(1, 50)
      const data = res.data || res
      notifications.value = data.records || data || []
      setMemoryCache(cacheKey, notifications.value, MESSAGE_DETAIL_CACHE_TTL)
    }
    if (markRead) {
      systemUnread.value = 0
      await notificationApi.markAllAsRead()
      emitUnreadChanged()
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载系统消息失败')
  } finally {
    if (!silent) chatLoading.value = false
  }
}

const openSystemChat = async (silent = false) => {
  await loadSystemMessages(true, silent, true)
}

const loadSystemUnread = async () => {
  try {
    const res: any = await notificationApi.getUnreadCount()
    const data = res.data || res
    systemUnread.value = data.count || 0
  } catch {}
}

const markAllNotificationsRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    systemUnread.value = 0
    notifications.value.forEach(n => n.isRead = 1)
    deleteMemoryCache('message:system')
    emitUnreadChanged()
    ElMessage.success('已全部标记为已读')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

const scrollToBottom = () => {
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

const isChatNearBottom = () => {
  if (!chatBodyRef.value) return true
  const { scrollTop, scrollHeight, clientHeight } = chatBodyRef.value
  return scrollHeight - scrollTop - clientHeight < 96
}

const goToUserProfile = (userId: number) => {
  if (!userId) return
  router.push(userId === currentUserId.value ? '/profile' : `/users/${userId}`)
}

const formatTime = (time: string) => {
  const result = formatRelativeTime(time)
  return result || formatApiDate(time)
}

const formatFullTime = (time: string) => {
  return formatApiDateTime(time)
}

const formatRole = (role?: string) => {
  if (role === 'ADMIN') return '管理员'
  if (role === 'SENIOR_USER') return '高级用户'
  return '普通用户'
}
</script>

<style scoped>
.message-container { max-width: 1000px; margin: 0 auto; padding: 0; }
.message-layout { height: 600px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; }
.conversation-panel { border-right: 1px solid #e4e7ed; display: flex; flex-direction: column; height: 100%; }
.panel-header { padding: 10px 12px; border-bottom: 1px solid #e4e7ed; display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.conversation-list { flex: 1; overflow-y: auto; }
.conv-item { display: flex; align-items: center; gap: 10px; padding: 12px 16px; cursor: pointer; transition: background 0.2s; border-bottom: 1px solid #f2f3f5; }
.conv-item:hover { background: #f5f7fa; }
.conv-item.active { background: #ecf5ff; }
.clickable-avatar { cursor: pointer; transition: transform 0.18s ease, box-shadow 0.18s ease; }
.clickable-avatar:hover { transform: translateY(-1px); box-shadow: 0 8px 18px rgba(64, 158, 255, 0.22); }
.conv-info { flex: 1; min-width: 0; }
.conv-name { font-size: 14px; font-weight: 500; color: #303133; display: flex; align-items: center; gap: 6px; }
.conv-preview { font-size: 12px; color: #909399; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
.conv-time { font-size: 11px; color: #c0c4cc; flex-shrink: 0; }
.conv-badge { margin-left: 4px; }
.mutual-tag { margin-left: 4px; }
.empty-conv { padding: 40px 0; }
.chat-panel { display: flex; flex-direction: column; height: 100%; }
.chat-header { padding: 12px 20px; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; justify-content: space-between; }
.chat-header span { font-weight: 600; font-size: 15px; color: #303133; }
.chat-header-actions { display: flex; align-items: center; gap: 8px; }
.chat-body { flex: 1; overflow-y: auto; padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; }
.msg-limit-hint { margin-bottom: 8px; }
.chat-message { display: flex; flex-direction: column; max-width: 70%; }
.chat-message.mine { align-self: flex-end; }
.msg-bubble { padding: 10px 14px; border-radius: 12px; background: #f0f2f5; color: #303133; font-size: 14px; line-height: 1.5; word-break: break-word; }
.chat-message.mine .msg-bubble { background: #409EFF; color: #fff; }
.msg-time { font-size: 11px; color: #c0c4cc; margin-top: 4px; }
.chat-message.mine .msg-time { text-align: right; }
.chat-input { display: flex; gap: 10px; padding: 12px 16px; border-top: 1px solid #e4e7ed; align-items: flex-end; }
.chat-input .el-input { flex: 1; }
.no-chat { display: flex; align-items: center; justify-content: center; height: 100%; }
.system-readonly-hint { padding: 10px 20px; background: #fff7e6; border-bottom: 1px solid #faecd8; color: #8a5a12; font-size: 13px; }
.system-msg { max-width: 90%; align-self: flex-start; }
.system-bubble { background: #fdf6ec !important; border: 1px solid #faecd8; }
.system-title { font-weight: 600; color: #e6a23c; font-size: 14px; }
.system-content { color: #606266; font-size: 13px; margin-top: 4px; }
.user-search-list { min-height: 220px; margin-top: 14px; }
.user-search-item { display: flex; align-items: center; gap: 10px; padding: 12px 8px; border-bottom: 1px solid #f0f2f5; cursor: pointer; border-radius: 8px; transition: background 0.2s; }
.user-search-item:hover { background: #f5f7fa; }
.user-search-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.user-search-info strong { color: #303133; font-size: 14px; }
.user-search-info span { color: #909399; font-size: 12px; }
</style>
