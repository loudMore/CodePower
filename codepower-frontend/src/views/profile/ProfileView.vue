<!-- 个人主页 — 用户信息、做题统计、能力雷达图 -->
<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="8" :lg="6">
        <!-- 用户信息卡片 -->
        <el-card class="profile-card" shadow="hover">
          <div class="user-avatar">
            <el-avatar :size="100" :src="userInfo.avatar" />
            <div v-if="isSelfProfile" class="edit-avatar">
              <el-button type="primary" circle icon="el-icon-camera" size="small"></el-button>
            </div>
          </div>
          <div class="user-name">{{ userInfo.username || '未命名用户' }}</div>
          <div class="user-title">{{ userInfo.title || '码力新手' }}</div>
          <div class="user-bio">{{ userInfo.bio || '这个用户很懒，还没有填写个人简介' }}</div>

          <div class="user-statistics">
            <div class="stat-item">
              <div class="stat-value">{{ userInfo.problemSolved }}</div>
              <div class="stat-label">解决问题</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ userInfo.submissionCount }}</div>
              <div class="stat-label">提交次数</div>
            </div>
            <div class="stat-item clickable" @click="goToMessageCenter">
              <div class="stat-value">{{ followCounts.followingCount || 0 }}</div>
              <div class="stat-label">关注</div>
            </div>
            <div class="stat-item clickable" @click="goToMessageCenter">
              <div class="stat-value">{{ followCounts.followerCount || 0 }}</div>
              <div class="stat-label">粉丝</div>
            </div>
          </div>

          <div v-if="!isSelfProfile" class="other-user-tip">
            当前正在查看其他用户主页，可关注对方或直接发起私信。
          </div>
          <div v-if="isSelfProfile" class="user-contact">
            <div v-if="userInfo.email" class="contact-item">
              <el-icon><Message /></el-icon>
              <span>{{ userInfo.email }}</span>
            </div>
            <div v-if="userInfo.location" class="contact-item">
              <el-icon><Location /></el-icon>
              <span>{{ userInfo.location }}</span>
            </div>
          </div>
          
          <div v-if="isSelfProfile" class="edit-profile">
            <el-button type="primary" @click="showEditProfile = true">编辑个人资料</el-button>
          </div>
          <div v-else class="profile-actions">
            <el-button :type="followStatus.isFollowing ? 'default' : 'primary'" @click="toggleFollow">
              {{ followStatus.isFollowing ? '取消关注' : '关注' }}
            </el-button>
            <el-button type="success" @click="goToMessageCenter">私信</el-button>
          </div>
        </el-card>

        <!-- 等级与每日任务面板 -->
        <level-panel v-if="isSelfProfile" ref="levelPanelRef" />

        <!-- 技能标签卡片 -->
        <el-card class="skills-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>技能标签</span>
              <el-tag size="small" type="info" effect="plain">基于做题自动计算</el-tag>
            </div>
          </template>
          <div v-if="userSkills.length > 0" class="skills-container">
            <el-tag
              v-for="skill in userSkills"
              :key="skill.name"
              class="skill-tag"
              :type="skill.score >= 70 ? 'success' : skill.score >= 40 ? '' : 'info'"
              effect="plain"
            >
              {{ skill.name }} · {{ skill.score }}分
            </el-tag>
          </div>
          <el-empty v-else description="暂无做题数据，快去刷题吧" :image-size="60" />
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="24" :md="16" :lg="18">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="概览" name="overview">
            <el-row :gutter="20">
              <el-col :span="24">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-header">
                      <span>能力雷达图</span>
                    </div>
                  </template>
                  <div ref="radarChartRef" style="width: 100%; height: 360px;"></div>
                  <div v-if="abilityProfile.dimensions.length" class="ability-summary">
                    <div class="ability-score-grid">
                      <div
                        v-for="item in abilityProfile.dimensions"
                        :key="item.key"
                        class="ability-score-item"
                      >
                        <div class="ability-score-header">
                          <span>{{ item.name }}</span>
                          <span>{{ item.score }} 分</span>
                        </div>
                        <el-progress :percentage="item.score" :stroke-width="10" :show-text="false" />
                        <div class="ability-score-evidence">{{ item.evidence }}</div>
                      </div>
                    </div>

                    <div class="ability-highlight-grid">
                      <el-card shadow="never" class="ability-highlight-card">
                        <template #header>
                          <div class="card-header">
                            <span>优势亮点</span>
                          </div>
                        </template>
                        <div v-if="abilityProfile.strengths.length" class="ability-highlight-list">
                          <div
                            v-for="item in abilityProfile.strengths"
                            :key="item.name"
                            class="ability-highlight-item success"
                          >
                            <div class="ability-highlight-title">{{ item.name }} · {{ item.score }} 分</div>
                            <div class="ability-highlight-text">{{ item.highlight }}</div>
                            <div class="ability-highlight-evidence">{{ item.evidence }}</div>
                          </div>
                        </div>
                        <el-empty v-else description="暂无明显优势项" :image-size="60" />
                      </el-card>

                      <el-card shadow="never" class="ability-highlight-card">
                        <template #header>
                          <div class="card-header">
                            <span>近期短板</span>
                          </div>
                        </template>
                        <div v-if="abilityProfile.weaknesses.length" class="ability-highlight-list">
                          <div
                            v-for="item in abilityProfile.weaknesses"
                            :key="item.name"
                            class="ability-highlight-item warning"
                          >
                            <div class="ability-highlight-title">{{ item.name }} · {{ item.score }} 分</div>
                            <div class="ability-highlight-text">{{ item.highlight }}</div>
                            <div class="ability-highlight-evidence">{{ item.evidence }}</div>
                          </div>
                        </div>
                        <el-empty v-else description="暂无明显短板项" :image-size="60" />
                      </el-card>
                    </div>

                    <div class="ability-overview-grid">
                      <div class="overview-item">
                        <div class="overview-label">日常通过题数</div>
                        <div class="overview-value">{{ abilityProfile.overview.solvedCount }}</div>
                      </div>
                      <div class="overview-item">
                        <div class="overview-label">日常提交次数</div>
                        <div class="overview-value">{{ abilityProfile.overview.totalAttempts }}</div>
                      </div>
                      <div class="overview-item">
                        <div class="overview-label">日常通过率</div>
                        <div class="overview-value">{{ abilityProfile.overview.acceptanceRate }}%</div>
                      </div>
                      <div class="overview-item">
                        <div class="overview-label">覆盖标签数</div>
                        <div class="overview-value">{{ abilityProfile.overview.coveredTagCount }}</div>
                      </div>
                      <div class="overview-item">
                        <div class="overview-label">平均通过难度</div>
                        <div class="overview-value">{{ abilityProfile.overview.averageSolvedDifficulty }}</div>
                      </div>
                      <div class="overview-item">
                        <div class="overview-label">最高通过难度</div>
                        <div class="overview-value">{{ abilityProfile.overview.highestSolvedDifficulty }}</div>
                      </div>
                    </div>

                    <div v-if="abilityProfile.analysisBasis" class="ability-analysis-basis">
                      {{ abilityProfile.analysisBasis }}
                    </div>
                  </div>
                  <el-empty v-if="!hasAbilities" description="暂无做题数据，快去刷题吧" />
                </el-card>
              </el-col>

            </el-row>
          </el-tab-pane>
          
          <el-tab-pane v-if="isSelfProfile" label="提交记录" name="submissions">
            <el-card shadow="hover">
              <submission-history :submissions="submissions" />
            </el-card>
          </el-tab-pane>
          
          <el-tab-pane v-if="isSelfProfile" label="收藏题目" name="favorites">
            <el-card shadow="hover">
              <favorite-problems :problems="favoriteProblems" />
            </el-card>
          </el-tab-pane>
          
          <el-tab-pane v-if="isSelfProfile" label="考试记录" name="contests">
            <el-card shadow="hover">
              <contest-history :contests="contestRecords" />
            </el-card>
          </el-tab-pane>
          
          <el-tab-pane v-if="isSelfProfile" label="系统设置" name="settings">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header">
                  <span>账户设置</span>
                  <router-link to="/profile/settings">
                    <el-button type="primary">进入完整设置</el-button>
                  </router-link>
                </div>
              </template>
              
              <el-result
                icon="info"
                title="账户与系统设置"
                sub-title="点击上方按钮进入完整的设置页面，管理您的账户信息和系统偏好"
              >
                <template #extra>
                  <router-link to="/profile/settings">
                    <el-button type="primary">立即前往设置</el-button>
                  </router-link>
                </template>
              </el-result>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
    
    <!-- 编辑个人资料弹窗 -->
    <el-dialog
      v-if="isSelfProfile"
      v-model="showEditProfile"
      title="编辑个人资料"
      width="500px"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="头像">
          <div class="avatar-selector">
            <div v-for="(av, i) in builtinAvatars" :key="i"
                 class="avatar-option" :class="{ selected: editForm.avatar === av }"
                 @click="editForm.avatar = av">
              <el-avatar :size="50" :src="av" />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="所在地">
          <el-select
            v-model="editForm.location"
            filterable
            clearable
            placeholder="请选择省级地区"
            style="width: 100%;"
          >
            <el-option
              v-for="region in REGION_OPTIONS"
              :key="region.value"
              :label="region.label"
              :value="region.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditProfile = false">取消</el-button>
          <el-button type="primary" @click="updateProfile">保存</el-button>
        </span>
      </template>
    </el-dialog>
    
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, Location } from '@element-plus/icons-vue'
import FavoriteProblems from '@/components/profile/FavoriteProblems.vue';
import ContestHistory from '@/components/profile/ContestHistory.vue';
import SubmissionHistory from '@/components/profile/SubmissionHistory.vue';
import LevelPanel from '@/components/profile/LevelPanel.vue';
import { REGION_OPTIONS, normalizeRegionLabel } from '@/utils/regions'
import { levelApi } from '@/api/level'
import { userApi } from '@/api/user'
import { favoriteApi } from '@/api/favorite'
import { followApi } from '@/api/follow'
import api from '@/api'
import { checkTokenValid } from '@/api/index'
import { buildAbilityRadarOption, normalizeAbilityProfile } from '@/utils/common'

const router = useRouter()
const route = useRoute()
const activeTab = ref('overview')
const currentUserId = ref(0)

type SubmissionHistoryItem = {
  id: number
  problemId: number
  problemTitle?: string
  status: string
  language?: string
  runtime?: number | null
  memory?: number | null
  submittedAt?: string
}

type ContestRecordItem = {
  id: number
  title?: string
  description?: string
  type?: string
  status?: string
  startTime?: string
  endTime?: string
  durationMinutes?: number
  creatorId?: number
  creatorName?: string
  problemCount?: number
  participantCount?: number
  registered?: boolean
}

type AbilityDimensionItem = {
  key: string
  name: string
  score: number
  evidence: string
}

type AbilityHighlightItem = {
  name: string
  score: number
  highlight: string
  evidence: string
}

type AbilityProfileState = {
  dimensions: AbilityDimensionItem[]
  strengths: AbilityHighlightItem[]
  weaknesses: AbilityHighlightItem[]
  overview: {
    solvedCount: number
    totalAttempts: number
    acceptanceRate: number
    coveredTagCount: number
    averageSolvedDifficulty: number | string
    highestSolvedDifficulty: string
  }
  analysisBasis: string
}

const builtinAvatars = [
  '/avatars/avatar-1.svg',
  '/avatars/avatar-2.svg',
  '/avatars/avatar-3.svg',
  '/avatars/avatar-4.svg',
  '/avatars/avatar-5.svg',
  '/avatars/avatar-6.svg',
]

// 用户信息
const userInfo = reactive({
  id: 0,
  username: '',
  email: '',
  avatar: builtinAvatars[0],
  title: '',
  bio: '',
  location: '',
  problemSolved: 0,
  submissionCount: 0,
})

// 页面数据
const submissions = ref<SubmissionHistoryItem[]>([])
const favoriteProblems = ref<any[]>([])
const contestRecords = ref<ContestRecordItem[]>([])
const radarChartRef = ref()
const levelPanelRef = ref()
const radarChartInstance = ref<any>(null)
const hasAbilities = ref(false)
const userSkills = ref<{ name: string; score: number }[]>([])
const abilityProfile = reactive<AbilityProfileState>({
  dimensions: [],
  strengths: [],
  weaknesses: [],
  overview: {
    solvedCount: 0,
    totalAttempts: 0,
    acceptanceRate: 0,
    coveredTagCount: 0,
    averageSolvedDifficulty: 0,
    highestSolvedDifficulty: '暂无'
  },
  analysisBasis: ''
})
const followCounts = ref<{ followingCount: number; followerCount: number }>({ followingCount: 0, followerCount: 0 })
const followStatus = ref<{ isFollowing: boolean; isMutual: boolean }>({ isFollowing: false, isMutual: false })
const viewedUserId = computed(() => {
  const id = Number(route.params.id)
  return Number.isFinite(id) && id > 0 ? id : null
})
const isSelfProfile = computed(() => !viewedUserId.value || viewedUserId.value === currentUserId.value)

// 页面状态
const showEditProfile = ref(false)

// 编辑表单
const editForm = reactive({
  username: '',
  bio: '',
  location: '',
  avatar: builtinAvatars[0]
})

const syncEditForm = () => {
  editForm.username = userInfo.username
  editForm.bio = userInfo.bio
  editForm.location = normalizeRegionLabel(userInfo.location)
  editForm.avatar = userInfo.avatar || builtinAvatars[0]
}

const getUserTitle = (level?: number) => {
  if (!level || level <= 0) return '码力新手'
  if (level >= 25) return '算法宗师'
  if (level >= 18) return '刷题达人'
  if (level >= 10) return '进阶开发者'
  if (level >= 5) return '潜力选手'
  return '码力新手'
}

const normalizeSubmissionStatus = (status?: string) => status || 'UNKNOWN'

const loadProblemTitleMap = async (problemIds: number[]) => {
  const uniqueIds = Array.from(new Set(problemIds.filter(Boolean)))
  if (uniqueIds.length === 0) return new Map<number, string>()

  try {
    const requests = uniqueIds.map((id) => api.get(`/api/problems/${id}`))
    const responses = await Promise.all(requests)
    return responses.reduce((map, response, index) => {
      const payload = response.data || response
      const problem = payload?.data || payload
      const title = problem?.title || `题目 #${uniqueIds[index]}`
      map.set(uniqueIds[index], title)
      return map
    }, new Map<number, string>())
  } catch (error) {
    console.error('加载题目标题失败:', error)
    return uniqueIds.reduce((map, id) => {
      map.set(id, `题目 #${id}`)
      return map
    }, new Map<number, string>())
  }
}

const applyAbilityProfile = (profile: any) => {
  const normalizedProfile = normalizeAbilityProfile(profile)

  abilityProfile.dimensions = normalizedProfile.dimensions
  abilityProfile.strengths = normalizedProfile.strengths
  abilityProfile.weaknesses = normalizedProfile.weaknesses
  abilityProfile.overview = normalizedProfile.overview
  abilityProfile.analysisBasis = normalizedProfile.analysisBasis
}

const resetAbilityProfile = () => {
  abilityProfile.dimensions = []
  abilityProfile.strengths = []
  abilityProfile.weaknesses = []
  abilityProfile.overview = {
    solvedCount: 0,
    totalAttempts: 0,
    acceptanceRate: 0,
    coveredTagCount: 0,
    averageSolvedDifficulty: 0,
    highestSolvedDifficulty: '暂无'
  }
  abilityProfile.analysisBasis = ''
}

const handleRadarResize = () => {
  radarChartInstance.value?.resize()
}

const goToMessageCenter = () => {
  const targetUserId = viewedUserId.value
  router.push(targetUserId && !isSelfProfile.value ? `/messages/${targetUserId}` : '/messages')
}

const loadFollowCounts = async (targetUserId: number) => {
  try {
    const res: any = await followApi.getCounts(targetUserId)
    followCounts.value = res.data || res || { followingCount: 0, followerCount: 0 }
  } catch (e) {
    console.error('获取关注数失败:', e)
    followCounts.value = { followingCount: 0, followerCount: 0 }
  }
}

const loadFollowStatus = async (targetUserId: number) => {
  if (isSelfProfile.value) {
    followStatus.value = { isFollowing: false, isMutual: false }
    return
  }
  try {
    const res: any = await followApi.checkFollow(targetUserId)
    followStatus.value = res.data || res || { isFollowing: false, isMutual: false }
  } catch (e) {
    console.error('获取关注状态失败:', e)
    followStatus.value = { isFollowing: false, isMutual: false }
  }
}

const toggleFollow = async () => {
  const targetUserId = viewedUserId.value
  if (!targetUserId || isSelfProfile.value) return
  try {
    if (followStatus.value.isFollowing) {
      await followApi.unfollow(targetUserId)
      ElMessage.success('已取消关注')
    } else {
      await followApi.follow(targetUserId)
      ElMessage.success('关注成功')
    }
    await Promise.all([loadFollowCounts(targetUserId), loadFollowStatus(targetUserId)])
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

// 更新个人资料
const updateProfile = async () => {
  try {
    const normalizedRegion = normalizeRegionLabel(editForm.location)
    await userApi.updateProfile({
      username: editForm.username,
      bio: editForm.bio,
      region: normalizedRegion,
      avatarUrl: editForm.avatar,
    })
    userInfo.username = editForm.username
    userInfo.bio = editForm.bio
    userInfo.location = normalizedRegion
    userInfo.avatar = editForm.avatar
    // 同步到 localStorage
    const stored = localStorage.getItem('userInfo')
    if (stored) {
      try {
        const obj = JSON.parse(stored)
        obj.username = editForm.username
        obj.avatar = editForm.avatar
        localStorage.setItem('userInfo', JSON.stringify(obj))
        window.dispatchEvent(new Event('user-login'))
      } catch {}
    }
    ElMessage.success('个人资料更新成功')
    showEditProfile.value = false
  } catch (e: any) {
    ElMessage.error('更新失败: ' + (e.response?.data?.message || '请稍后重试'))
  }
}

// 加载能力雷达图
const loadRadarChart = async (profile: any) => {
  const normalizedProfile = normalizeAbilityProfile(profile)
  if (!radarChartRef.value || !normalizedProfile.hasData) return
  hasAbilities.value = true
  const echarts = await import('echarts')
  radarChartInstance.value?.dispose()
  radarChartInstance.value = echarts.init(radarChartRef.value)
  radarChartInstance.value.setOption(buildAbilityRadarOption(normalizedProfile.indicator, normalizedProfile.values))
}

const loadAbilityProfile = async (targetUserId: number) => {
  hasAbilities.value = false
  userSkills.value = []
  resetAbilityProfile()
  radarChartInstance.value?.dispose()
  radarChartInstance.value = null

  try {
    const profileRes = isSelfProfile.value
      ? await userApi.getAbilities()
      : await userApi.getAbilitiesById(targetUserId)
    const profile = profileRes?.data || profileRes
    const tagSnapshot = Array.isArray(profile?.tagSnapshot) ? profile.tagSnapshot : []
    const dimensions = Array.isArray(profile?.dimensions) ? profile.dimensions : []

    if (dimensions.length > 0) {
      userSkills.value = tagSnapshot.map((item: any) => ({
        name: item.tagName || '未知',
        score: Math.round(item.score || 0)
      }))
      applyAbilityProfile(profile)
      await nextTick()
      await loadRadarChart(profile)
    }
  } catch (e) {
    console.error('获取能力数据失败:', e)
  }
}

// 检查登录状态
const checkLoginStatus = () => {
  if (!checkTokenValid()) {
    ElMessage.warning('请先登录')
    const redirectPath = viewedUserId.value ? `/users/${viewedUserId.value}` : '/profile'
    router.push(`/auth/login?redirect=${encodeURIComponent(redirectPath)}`)
    return false
  }
  return true
}

const syncLevelTitle = async () => {
  if (!isSelfProfile.value) return

  try {
    const levelRes: any = await levelApi.getLevelInfo()
    const levelData = levelRes.data || levelRes
    userInfo.title = levelData?.title || getUserTitle(levelData?.level)
    await levelPanelRef.value?.loadData?.()
  } catch (e) {
    console.error('获取等级信息失败:', e)
  }
}

const loadProfilePage = async () => {
  const userInfoStr = localStorage.getItem('userInfo')
  currentUserId.value = 0
  if (userInfoStr) {
    try {
      const userData = JSON.parse(userInfoStr)
      currentUserId.value = userData.id || 0
      if (!viewedUserId.value) {
        userInfo.id = userData.id || 0
        userInfo.username = userData.username || ''
        userInfo.email = userData.email || ''
        userInfo.avatar = userData.avatar || builtinAvatars[0]
      }
      syncEditForm()
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }

  const targetUserId = viewedUserId.value || currentUserId.value
  if (!targetUserId) return

  try {
    const profileRes: any = viewedUserId.value ? await userApi.getUserById(targetUserId) : await userApi.getProfile()
    const rawProfileData = profileRes.data || profileRes
    const profileData = rawProfileData?.data || rawProfileData
    const user = profileData?.user || {}
    const profile = profileData?.profile || {}
    const stats = profileData?.stats || {}
    if (profileData) {
      userInfo.id = user.id || targetUserId
      userInfo.username = user.username || ''
      userInfo.email = viewedUserId.value ? '' : (user.email || '')
      userInfo.avatar = profile.avatarUrl || builtinAvatars[0]
      userInfo.bio = profile.bio || ''
      userInfo.location = normalizeRegionLabel(profile.region)
      userInfo.problemSolved = stats.solvedCount || 0
      userInfo.submissionCount = stats.submissionCount || 0
      userInfo.title = viewedUserId.value ? getUserTitle(user.level) : ''
      if (isSelfProfile.value) {
        syncEditForm()
      }
    }
  } catch (e) {
    console.error('获取用户资料失败:', e)
  }

  activeTab.value = 'overview'
  await loadAbilityProfile(targetUserId)

  if (isSelfProfile.value) {
    await syncLevelTitle()

    try {
      const subRes: any = await userApi.getSubmissions({ page: 1, size: 20 })
      const subData = subRes.data || subRes
      const rawSubmissions = (subData.records || subData || []) as any[]
      const titleMap = await loadProblemTitleMap(rawSubmissions.map((s) => s.problemId))
      submissions.value = rawSubmissions.map((s: any) => ({
        id: s.id,
        problemId: s.problemId,
        problemTitle: titleMap.get(s.problemId) || `题目 #${s.problemId}`,
        status: normalizeSubmissionStatus(s.status),
        language: s.language,
        runtime: s.executionTime,
        memory: s.memoryUsed,
        submittedAt: s.createdAt
      }))
    } catch (e) {
      console.error('获取提交记录失败:', e)
    }

    try {
      const records = await userApi.getContestRecords()
      contestRecords.value = records
        .filter((item: any) => item?.type === 'EXAM' || item?.registered || item?.creatorId === currentUserId.value)
        .sort((a: any, b: any) => new Date(b.startTime || b.createdAt || 0).getTime() - new Date(a.startTime || a.createdAt || 0).getTime())
    } catch (e) {
      console.error('获取考试记录失败:', e)
    }

    try {
      const favRes: any = await favoriteApi.getFavorites()
      favoriteProblems.value = favRes.data || favRes || []
    } catch (e) {
      console.error('获取收藏题目失败:', e)
    }
  } else {
    submissions.value = []
    contestRecords.value = []
    favoriteProblems.value = []
  }

  await Promise.all([
    loadFollowCounts(targetUserId),
    loadFollowStatus(targetUserId)
  ])
}

// 初始化数据
onMounted(async () => {
  window.addEventListener('resize', handleRadarResize)
  if (!checkLoginStatus()) return
  await loadProfilePage()
})

watch(() => route.params.id, async () => {
  if (!checkLoginStatus()) return
  await loadProfilePage()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleRadarResize)
  radarChartInstance.value?.dispose()
  radarChartInstance.value = null
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.profile-card {
  margin-bottom: 20px;
  text-align: center;
}

.user-avatar {
  position: relative;
  display: inline-block;
  margin-bottom: 15px;
}

.edit-avatar {
  position: absolute;
  right: 0;
  bottom: 0;
}

.user-name {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 5px;
}

.user-title {
  font-size: 14px;
  color: #409EFF;
  margin-bottom: 10px;
}

.user-bio {
  color: #606266;
  margin-bottom: 20px;
  padding: 0 10px;
}

.user-statistics {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
  border-top: 1px solid #EBEEF5;
  border-bottom: 1px solid #EBEEF5;
  padding: 15px 0;
}

.stat-item {
  text-align: center;
}

.stat-item.clickable { cursor: pointer; }
.stat-item.clickable:hover .stat-value { color: #409eff; }

.stat-value {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.user-contact {
  margin-bottom: 20px;
}

.contact-item {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 5px;
  color: #606266;
}

.contact-item .el-icon {
  margin-right: 5px;
}

.edit-profile {
  margin-bottom: 10px;
}

.profile-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-bottom: 10px;
}

.other-user-tip {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f4f4f5;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.skills-card {
  margin-bottom: 20px;
}

.skills-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mt-20 {
  margin-top: 20px;
}

.profile-tabs {
  margin-top: 10px;
}

.ability-summary {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ability-score-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.ability-score-item {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid #e4e7ed;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.ability-score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.ability-score-evidence {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  min-height: 42px;
}

.ability-highlight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.ability-highlight-card {
  border-radius: 14px;
}

.ability-highlight-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ability-highlight-item {
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid transparent;
}

.ability-highlight-item.success {
  background: #f0f9eb;
  border-color: #c2e7b0;
}

.ability-highlight-item.warning {
  background: #fdf6ec;
  border-color: #f3d19e;
}

.ability-highlight-title {
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #303133;
}

.ability-highlight-text {
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
}

.ability-highlight-evidence {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #606266;
}

.ability-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}

.overview-item {
  padding: 16px;
  border-radius: 12px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
}

.overview-label {
  font-size: 13px;
  color: #909399;
}

.overview-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.ability-analysis-basis {
  padding: 14px 16px;
  border-radius: 12px;
  background: #ecf5ff;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  border: 1px solid #d9ecff;
}

.settings-form {
  max-width: 600px;
}

.settings-value {
  display: inline-block;
  margin-right: 10px;
}

.skill-tag.suggested {
  cursor: pointer;
  margin-right: 8px;
  margin-bottom: 8px;
}

.avatar-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.avatar-option {
  cursor: pointer;
  border: 3px solid transparent;
  border-radius: 50%;
  padding: 2px;
  transition: border-color 0.2s;
}

.avatar-option:hover {
  border-color: #c0c4cc;
}

.avatar-option.selected {
  border-color: #409EFF;
}

/* 适配小屏幕 */
@media (max-width: 768px) {
  .user-statistics {
    flex-wrap: wrap;
  }

  .stat-item {
    width: 33%;
    margin-bottom: 10px;
  }

  .ability-highlight-grid {
    grid-template-columns: 1fr;
  }

  .ability-score-grid,
  .ability-overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
