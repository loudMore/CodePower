<!-- 根组件 — 顶部导航栏、路由视图、页脚 -->
<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Setting,
  ArrowDown,
  User,
  SwitchButton,
  Bell,
  ChatDotRound
} from '@element-plus/icons-vue'
import { checkTokenValid } from './api/index'
import { notificationApi } from './api/notification'
import { messageApi } from './api/message'

const router = useRouter()

const isLoggedIn = ref(false)
const username = ref('')
const userAvatar = ref('')
const userRole = ref('')
const unreadNotifications = ref(0)
const unreadMessages = ref(0)
const keepAliveRouteNames = ['problem-detail', 'contest-detail', 'messages', 'message-conversation']
const keepAliveIncludes = computed(() => {
  const currentName = router.currentRoute.value.name
  return typeof currentName === 'string' && keepAliveRouteNames.includes(currentName)
    ? Array.from(new Set([...keepAliveRouteNames, currentName]))
    : keepAliveRouteNames
})
const isProblemWorkspace = computed(() => router.currentRoute.value.name === 'problem-detail')

const getRouteViewKey = (route: any) => {
  if (route.name === 'problem-detail') {
    const contestId = route.query?.contestId
    return contestId ? `problem-detail-contest-${contestId}` : 'problem-detail-practice'
  }
  return route.fullPath
}

const builtinAvatars = [
  '/avatars/avatar-1.svg',
  '/avatars/avatar-2.svg',
  '/avatars/avatar-3.svg',
  '/avatars/avatar-4.svg',
  '/avatars/avatar-5.svg',
  '/avatars/avatar-6.svg',
]

const updateLoginState = () => {
  const isTokenValid = checkTokenValid()
  const userInfoStr = localStorage.getItem('userInfo')

  isLoggedIn.value = isTokenValid

  if (userInfoStr && isTokenValid) {
    try {
      const userInfo = JSON.parse(userInfoStr)
      username.value = userInfo.username || ''
      userRole.value = userInfo.role || 'NORMAL_USER'
      userAvatar.value = userInfo.avatar || builtinAvatars[0]
    } catch (e) {
      userAvatar.value = builtinAvatars[0]
    }
  } else {
    username.value = ''
    userRole.value = ''
    userAvatar.value = builtinAvatars[0]
  }
}

let statusCheckInterval: number | null = null

const loadUnreadCounts = async () => {
  if (!isLoggedIn.value) {
    unreadNotifications.value = 0
    unreadMessages.value = 0
    return
  }
  try {
    const [notifRes, msgRes]: any[] = await Promise.all([
      notificationApi.getUnreadCount(),
      messageApi.getUnreadCount()
    ])
    unreadNotifications.value = (notifRes.data || notifRes)?.count || 0
    unreadMessages.value = (msgRes.data || msgRes)?.count || 0
  } catch {}
}

const handleMessageCenterUpdated = () => {
  loadUnreadCounts()
}

const logout = () => {
  localStorage.removeItem('authToken')
  localStorage.removeItem('userInfo')
  unreadNotifications.value = 0
  unreadMessages.value = 0
  updateLoginState()
  router.push('/auth/login')
}

onMounted(() => {
  updateLoginState()
  loadUnreadCounts()

  statusCheckInterval = window.setInterval(() => {
    updateLoginState()
    loadUnreadCounts()
  }, 60000)

  window.addEventListener('storage', handleStorageChange)
  window.addEventListener('user-login', handleUserLogin)
  window.addEventListener('message-center-updated', handleMessageCenterUpdated)
})

onBeforeUnmount(() => {
  if (statusCheckInterval !== null) {
    clearInterval(statusCheckInterval)
  }

  window.removeEventListener('storage', handleStorageChange)
  window.removeEventListener('user-login', handleUserLogin)
  window.removeEventListener('message-center-updated', handleMessageCenterUpdated)
})

const handleStorageChange = (event: StorageEvent) => {
  if (event.key === 'authToken' || event.key === 'userInfo') {
    updateLoginState()
    loadUnreadCounts()
  }
}

const handleUserLogin = () => {
  updateLoginState()
  loadUnreadCounts()
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'notifications':
      router.push('/notifications')
      break
    case 'messages':
      router.push('/messages')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'settings':
      router.push('/profile/settings')
      break
    case 'logout':
      logout()
      break
  }
}

const handleMenuSelect = async (path: string) => {
  if (!path || router.currentRoute.value.path === path) return
  try {
    await router.push(path)
  } catch {
    window.location.assign(path)
  }
}
</script>

<template>
  <el-config-provider>
    <el-container class="app">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="container header-container">
          <div class="logo">
            <router-link to="/">Code Power</router-link>
          </div>
          
          <el-menu
            mode="horizontal"
            :ellipsis="false"
            class="main-menu"
            :default-active="$route.path"
            @select="handleMenuSelect"
          >
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/learning-paths">闯关学习</el-menu-item>
            <el-menu-item index="/problems">刷题</el-menu-item>
            <el-menu-item index="/contests">竞赛</el-menu-item>
            <el-menu-item index="/community">社区</el-menu-item>
          </el-menu>
          
          <div class="user">
            <template v-if="isLoggedIn">
              <router-link to="/messages" class="header-icon">
                <el-badge :value="unreadNotifications + unreadMessages" :hidden="(unreadNotifications + unreadMessages) === 0" :max="99">
                  <el-icon :size="22"><chat-dot-round /></el-icon>
                </el-badge>
              </router-link>
              <el-dropdown @command="handleCommand" trigger="click">
                <div class="user-info">
                  <el-avatar :size="32" :src="userAvatar"></el-avatar>
                  <span class="username">{{ username }}</span>
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><user /></el-icon>个人中心
                    </el-dropdown-item>
                    <el-dropdown-item command="messages">
                      <el-icon><chat-dot-round /></el-icon>信箱
                    </el-dropdown-item>
                    <el-dropdown-item v-if="userRole === 'ADMIN'" command="admin">
                      <el-icon><setting /></el-icon>管理后台
                    </el-dropdown-item>
                    <el-dropdown-item command="settings">
                      <el-icon><setting /></el-icon>账户设置
                    </el-dropdown-item>
                    <el-dropdown-item divided command="logout">
                      <el-icon><switch-button /></el-icon>退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <router-link to="/auth/login">
                <el-button type="primary">登录</el-button>
              </router-link>
              <router-link to="/auth/register" class="ml-2">
                <el-button>注册</el-button>
              </router-link>
            </template>
          </div>
        </div>
      </el-header>
      
      <!-- 主内容区域 -->
      <el-main class="main">
        <div class="container content" :class="{ 'problem-workspace-content': isProblemWorkspace }">
          <router-view v-slot="{ Component, route }">
            <keep-alive :include="keepAliveIncludes">
              <component :is="Component" :key="getRouteViewKey(route)" />
            </keep-alive>
          </router-view>
        </div>
      </el-main>
      
      <!-- 页脚 -->
      <el-footer class="footer">
        <div class="container">
          <p class="copyright">&copy; {{ new Date().getFullYear() }} Code Power - 算法训练平台</p>
        </div>
      </el-footer>
    </el-container>
  </el-config-provider>
</template>

<style>
/* 重置样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
  font-size: 16px;
  line-height: 1.5;
  color: #333;
  background-color: #f5f7fa;
}

a {
  text-decoration: none;
  color: inherit;
}

.container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Element Plus 覆盖样式 */
.el-menu {
  border-bottom: none !important;
  background-color: transparent !important;
}

.el-menu--horizontal>.el-menu-item {
  height: 60px;
  line-height: 60px;
  font-size: 16px;
}

.el-menu--horizontal>.el-menu-item.is-active {
  color: #409EFF;
  border-bottom-color: #409EFF;
}

.el-dropdown-menu__item i {
  margin-right: 8px;
}

.el-header, .el-footer {
  padding: 0;
}

.el-main {
  padding: 30px 0;
}

/* 布局样式 */
.app {
  min-height: 100vh;
}

/* 头部样式 */
.header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  height: 60px;
}

.header-container {
  display: flex;
  align-items: center;
  height: 60px;
  justify-content: space-between;
}

.logo a {
  font-size: 22px;
  font-weight: bold;
  color: #409EFF;
}

.main-menu {
  margin-left: 20px;
  display: flex;
  flex-grow: 0;
}

.user {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-left: auto;
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #606266;
  cursor: pointer;
  transition: background-color 0.3s, color 0.3s;
}

.header-icon:hover {
  background-color: #f0f2f5;
  color: #409EFF;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 20px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f0f2f5;
}

.username {
  margin-left: 8px;
  margin-right: 5px;
  color: #333;
}

/* 主内容区域样式 */
.main {
  flex: 1;
  width: 100%;
}

.content {
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.10);
  padding: 28px 36px;
  width: 96%;
  min-width: 900px;
  max-width: 1600px;
  margin: 0 auto;
}

.content.problem-workspace-content {
  width: min(98vw, 1880px);
  max-width: 1880px;
  padding: 20px 24px;
}

/* 页脚样式 */
.footer {
  background-color: #2c3e50;
  color: #fff;
  padding: 20px 0;
  text-align: center;
}

.copyright {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

/* 间距辅助类 */
.ml-2 {
  margin-left: 8px;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .header-container {
    padding: 0 10px;
  }
  
  .username {
    display: none;
  }
  
  .main {
    padding: 15px 0;
  }
  
  .content {
    padding: 15px;
    border-radius: 4px;
  }
}

@media (max-width: 480px) {
  .logo a {
    font-size: 18px;
  }
}

@media (max-width: 1100px) {
  .content {
    min-width: 0;
    width: 99%;
    padding: 10px 2px;
  }
}
</style>
