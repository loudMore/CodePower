<!-- 竞赛列表页面 -->
<template>
  <div class="contests-container">
    <div class="contests-header">
      <h1>编程竞赛</h1>
      <p class="subtitle">参加编程竞赛，提升你的编程技能和解题能力</p>
      <div v-if="canCreateContent" class="contest-actions-header">
        <el-button type="success" @click="$router.push('/contests/create')" class="create-btn">创建竞赛</el-button>
      </div>
    </div>

    <!-- 范围筛选 -->
    <div class="scope-filter">
      <el-radio-group v-model="activeScope" size="large" @change="handleScopeChange">
        <el-radio-button label="all">全部竞赛</el-radio-button>
        <el-radio-button label="joined">我参加的</el-radio-button>
        <el-radio-button v-if="canCreateContent" label="created">我创建的</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 状态筛选 + 类型筛选 -->
    <div class="filter-section">
      <div class="filter-tabs">
        <el-radio-group v-model="activeStatus" size="large" @change="handleStatusChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="RUNNING">进行中</el-radio-button>
          <el-radio-button label="UPCOMING">即将开始</el-radio-button>
          <el-radio-button label="ENDED">已结束</el-radio-button>
        </el-radio-group>
        <el-radio-group v-model="activeType" size="large" style="margin-left: 16px" @change="handleTypeChange">
          <el-radio-button label="all">全部类型</el-radio-button>
          <el-radio-button label="EXAM">限时考试</el-radio-button>
          <el-radio-button label="PRACTICE">练习赛</el-radio-button>
        </el-radio-group>
        <el-checkbox v-model="officialOnly" style="margin-left: 16px" @change="handleOfficialChange">仅官方活动</el-checkbox>
      </div>
      <div class="search-box">
        <el-input v-model="searchKeyword" placeholder="搜索竞赛..." clearable @input="handleSearchChange" />
      </div>
    </div>

    <!-- 邀请码加入 -->
    <div class="invite-code-section">
      <span class="invite-label">邀请码加入</span>
      <el-input v-model="inviteCode" placeholder="输入邀请码快速加入竞赛..." class="invite-input">
        <template #append>
          <el-button @click="joinWithInviteCode" :loading="joiningByCode">加入</el-button>
        </template>
      </el-input>
    </div>

    <!-- 竞赛列表 -->
    <div class="contests-list" v-loading="loading">
      <el-empty v-if="contests.length === 0 && !loading" description="暂无符合条件的竞赛" />

      <el-card v-for="contest in contests" :key="contest.id" class="contest-card" shadow="hover"
               @click="enterContest(contest)">
        <!-- 状态角标 -->
        <div class="contest-status" :class="'status-' + contest.status">
          {{ statusTextMap[contest.status] || contest.status }}
        </div>

        <!-- 官方徽章 -->
        <div class="official-ribbon" v-if="contest.isOfficial === 1">
          <span>官方</span>
        </div>

        <div class="contest-content">
          <div class="contest-info">
            <h2 class="contest-title">{{ contest.title }}</h2>
            <div class="contest-tags">
              <el-tag size="small" effect="plain">{{ typeTextMap[contest.type] || contest.type }}</el-tag>
              <el-tag size="small" type="warning" v-if="contest.allowedLanguages">
                限制语言: {{ contest.allowedLanguages }}
              </el-tag>
              <el-tag size="small" type="danger" v-if="contest.isPublic === 0">非公开</el-tag>
              <el-tag size="small" type="danger" effect="plain" v-if="contest.password">需密码</el-tag>
              <el-tag
                v-if="getContestParticipationTag(contest)"
                size="small"
                :type="getContestParticipationTag(contest)?.type"
              >
                {{ getContestParticipationTag(contest)?.text }}
              </el-tag>
            </div>
            <p class="contest-desc">{{ contest.description }}</p>
            <div class="contest-meta">
              <span>创建者：{{ contest.creatorName || '-' }}</span>
              <span>开始：{{ formatDate(contest.startTime) }}</span>
              <span>结束：{{ formatEndDate(contest) }}</span>
              <span>时长：{{ formatDuration(contest) }}</span>
              <span>{{ contest.problemCount || 0 }} 题</span>
              <span>{{ contest.participantCount || 0 }} 人报名</span>
            </div>
          </div>

          <div class="contest-actions" @click.stop>
            <!-- 倒计时 -->
            <div class="countdown" v-if="contest.status === 'UPCOMING'">
              <div class="countdown-label">距离开始</div>
              <div class="countdown-time">{{ getCountdown(contest) }}</div>
            </div>
            <div class="countdown" v-else-if="contest.status === 'RUNNING'">
              <div class="countdown-label">距离结束</div>
              <div class="countdown-time">{{ getCountdown(contest) }}</div>
            </div>
            <el-button type="primary" size="large" @click.stop="enterContest(contest)">
              {{ getContestActionText(contest) }}
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <div class="pagination-container" v-if="total > pageSize">
      <el-pagination background layout="prev, pager, next" :total="total"
                     :page-size="pageSize" :current-page="currentPage" @current-change="handlePageChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contestApi } from '@/api/contest'
import { getMemoryCache, setMemoryCache, deleteMemoryCacheByPrefix } from '@/utils/memoryCache'
import { apiDateTimeMs, formatApiDateTime, parseApiDate } from '@/utils/datetime'

const router = useRouter()
const CONTEST_LIST_CACHE_TTL = 30_000

type ContestStatus = 'UPCOMING' | 'RUNNING' | 'ENDED' | 'DRAFT'
type ContestScope = 'all' | 'joined' | 'created'
type ContestType = 'all' | 'EXAM' | 'PRACTICE'

type ContestListItem = {
  id: number
  title?: string
  description?: string
  status: ContestStatus
  type?: string
  isOfficial?: number
  allowedLanguages?: string
  password?: string
  registered?: boolean
  canRegister?: boolean
  canAccessWorkspace?: boolean
  creatorName?: string
  startTime?: string
  endTime?: string
  durationMinutes?: number
  problemCount?: number
  participantCount?: number
  isPublic?: number
}

type ContestListResponse = {
  records?: ContestListItem[]
  total?: number
}

const loading = ref(false)
const contests = ref<ContestListItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeStatus = ref<'all' | ContestStatus>('all')
const activeType = ref<ContestType>('all')
const activeScope = ref<ContestScope>('all')
const searchKeyword = ref('')
const officialOnly = ref(false)
const inviteCode = ref('')
const joiningByCode = ref(false)

const statusTextMap: Record<string, string> = { UPCOMING: '即将开始', RUNNING: '进行中', ENDED: '已结束', DRAFT: '草稿' }
const typeTextMap: Record<string, string> = { PRACTICE: '练习赛', EXAM: '限时考试', OFFICIAL: '官方活动', RATED: '积分赛' }

const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const canCreateContent = computed(() => {
  return currentUser.value.role === 'SENIOR_USER' || currentUser.value.role === 'ADMIN'
})

const canRegisterContest = (contest: ContestListItem) => {
  return !!contest.canRegister && contest.status !== 'ENDED'
}

const canDirectEnterContest = (contest: ContestListItem) => {
  return contest.status === 'RUNNING' && !!contest.canAccessWorkspace
}

const isOpenEndedContest = (contest: ContestListItem) => {
  if (!contest.endTime) return !contest.durationMinutes || contest.durationMinutes <= 0
  const end = parseApiDate(contest.endTime)
  return !end || !Number.isFinite(end.getTime()) || end.getFullYear() >= 2099
}

const isOpenEndedPractice = (contest: ContestListItem) => {
  return contest.status === 'RUNNING' && isOpenEndedContest(contest)
}

const getContestParticipationTag = (contest: ContestListItem) => {
  if (contest.registered) return { type: 'success' as const, text: contest.status === 'RUNNING' ? '已参赛' : '已报名' }
  if (contest.canAccessWorkspace) return { type: 'warning' as const, text: '可进入' }
  return null
}

const getContestActionText = (contest: ContestListItem) => {
  if (contest.status === 'UPCOMING') {
    if (contest.registered) return '已报名'
    return canRegisterContest(contest) ? '立即报名' : '查看详情'
  }
  if (contest.status === 'RUNNING') {
    if (canDirectEnterContest(contest)) return '进入竞赛'
    return canRegisterContest(contest) ? '立即参赛' : '查看详情'
  }
  return '查看结果'
}

// 倒计时刷新定时器：竞赛开始/结束后需要刷新按钮和状态。
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 切换“全部/我参加/我创建”范围后回到第一页。
const handleScopeChange = () => {
  currentPage.value = 1
  loadContests()
}

// 切换竞赛状态筛选后重新请求列表。
const handleStatusChange = () => {
  currentPage.value = 1
  loadContests()
}

// 切换竞赛类型筛选后重新请求列表。
const handleTypeChange = () => {
  currentPage.value = 1
  loadContests()
}

// 搜索防抖 — 避免每次按键都触发 API 请求
let searchDebounceTimer: number
const handleSearchChange = () => {
  clearTimeout(searchDebounceTimer)
  searchDebounceTimer = window.setTimeout(() => {
    currentPage.value = 1
    loadContests()
  }, 300)
}

const handleOfficialChange = () => {
  currentPage.value = 1
  loadContests()
}

// 加载竞赛列表：分页参数交给后端，前端只做轻量搜索和官方活动过滤，并用短 TTL 缓存减少重复请求。
const loadContests = async (force = false) => {
  loading.value = true
  try {
    const cacheKey = `contest:list:${activeScope.value}:${activeStatus.value}:${activeType.value}:${officialOnly.value}:${currentPage.value}:${pageSize.value}:${searchKeyword.value}`
    if (!force) {
      const cached = getMemoryCache<ContestListResponse>(cacheKey)
      if (cached) {
        contests.value = cached.records || []
        total.value = cached.total || 0
        return
      }
    }

    const params: { page: number; size: number; status?: ContestStatus; type?: string; filter?: ContestScope } = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (activeStatus.value !== 'all') params.status = activeStatus.value
    if (activeType.value !== 'all') params.type = activeType.value

    let res
    if (activeScope.value === 'joined' || activeScope.value === 'created') {
      params.filter = activeScope.value
      res = await contestApi.listMyContests(params)
    } else {
      res = await contestApi.listContests(params)
    }
    const data = (res.data?.data || res.data || res) as ContestListResponse
    let records = data.records || []

    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase()
      records = records.filter((c) =>
        c.title?.toLowerCase().includes(kw) || c.description?.toLowerCase().includes(kw))
    }
    // 按 is_official 标记筛选官方活动。
    if (officialOnly.value) {
      records = records.filter((c) => c.isOfficial === 1)
    }
    records = sortContestRecords(records)
    contests.value = records
    total.value = data.total || records.length
    setMemoryCache(cacheKey, { records, total: total.value }, CONTEST_LIST_CACHE_TTL)
  } catch (e) {
    console.error('加载竞赛列表失败', e)
  } finally {
    loading.value = false
  }
}

// 分页切换后加载目标页。
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadContests()
}

// 点击竞赛卡片主按钮：未开始可报名，进行中可进入，结束后查看结果。
const enterContest = async (contest: ContestListItem) => {
  if (contest.status === 'UPCOMING') {
    if (contest.registered) {
      ElMessage.info('你已报名该竞赛，等待开始即可')
      return
    }
    if (!canRegisterContest(contest)) {
      router.push(`/contests/${contest.id}`)
      return
    }
    try {
      if (contest.password) {
        const { value } = await ElMessageBox.prompt('请输入竞赛密码', '私密竞赛', { inputType: 'password' })
        await contestApi.register(contest.id, value)
      } else {
        await contestApi.register(contest.id)
      }
      contest.registered = true
      contest.canRegister = false
      contest.participantCount = (contest.participantCount || 0) + 1
      deleteMemoryCacheByPrefix('contest:list:')
      ElMessage.success(`已成功报名「${contest.title}」`)
    } catch (e: unknown) {
      if (e !== 'cancel') {
        const error = e as { response?: { data?: { message?: string } }; data?: { message?: string } }
        ElMessage.error(error?.response?.data?.message || error?.data?.message || '报名失败')
      }
    }
    return
  }

  if (contest.status === 'RUNNING') {
    if (canDirectEnterContest(contest)) {
      router.push(`/contests/${contest.id}`)
      return
    }
    if (!canRegisterContest(contest)) {
      router.push(`/contests/${contest.id}`)
      return
    }
    try {
      if (contest.password) {
        const { value } = await ElMessageBox.prompt('请输入竞赛密码', '私密竞赛', { inputType: 'password' })
        await contestApi.register(contest.id, value)
      } else {
        await contestApi.register(contest.id)
      }
      contest.registered = true
      contest.canRegister = false
      contest.canAccessWorkspace = true
      contest.participantCount = (contest.participantCount || 0) + 1
      deleteMemoryCacheByPrefix('contest:list:')
      ElMessage.success(`已成功加入「${contest.title}」`)
      router.push(`/contests/${contest.id}`)
    } catch (e: unknown) {
      if (e !== 'cancel') {
        const error = e as { response?: { data?: { message?: string } }; data?: { message?: string } }
        ElMessage.error(error?.response?.data?.message || error?.data?.message || '加入失败')
      }
    }
    return
  }

  router.push(`/contests/${contest.id}`)
}

const joinWithInviteCode = async () => {
  if (!inviteCode.value.trim()) {
    ElMessage.warning('请输入邀请码')
    return
  }
  joiningByCode.value = true
  try {
    const res = await contestApi.joinByInviteCode(inviteCode.value.trim())
    const joinedContest = res.data?.data || res.data || res
    deleteMemoryCacheByPrefix('contest:list:')
    ElMessage.success('加入成功！')
    inviteCode.value = ''
    if (joinedContest?.id) {
      router.push(`/contests/${joinedContest.id}`)
    } else {
      loadContests()
    }
  } catch (e: unknown) {
    const error = e as { response?: { data?: { message?: string } } }
    ElMessage.error(error?.response?.data?.message || '加入失败，请检查邀请码')
  } finally {
    joiningByCode.value = false
  }
}

const formatDate = (date?: string) => {
  return formatApiDateTime(date)
}

const formatEndDate = (contest: ContestListItem) => {
  return isOpenEndedContest(contest) ? '长期开放' : formatDate(contest.endTime)
}

const sortContestRecords = (records: ContestListItem[]) => {
  return [...records].sort((a, b) => {
    const endedA = a.status === 'ENDED' ? 1 : 0
    const endedB = b.status === 'ENDED' ? 1 : 0
    if (endedA !== endedB) return endedA - endedB
    const officialA = a.isOfficial === 1 ? 1 : 0
    const officialB = b.isOfficial === 1 ? 1 : 0
    if (officialA !== officialB) return officialB - officialA
    return Number(b.id || 0) - Number(a.id || 0)
  })
}

const formatDuration = (contest: ContestListItem) => {
  if (isOpenEndedContest(contest) && contest.type !== 'EXAM') return '长期开放'
  if (!contest.durationMinutes) return '-'
  const h = Math.floor(contest.durationMinutes / 60)
  const m = contest.durationMinutes % 60
  return h > 0 ? `${h}小时${m > 0 ? m + '分' : ''}` : `${m}分钟`
}

const getCountdown = (contest: ContestListItem) => {
  if (contest.status === 'RUNNING' && isOpenEndedContest(contest)) return '长期开放'
  const targetDate = contest.status === 'UPCOMING' ? contest.startTime : contest.endTime
  if (!targetDate) return '-'
  const diff = Math.max(0, Math.floor((apiDateTimeMs(targetDate) - Date.now()) / 1000))
  const d = Math.floor(diff / 86400)
  const h = Math.floor((diff % 86400) / 3600)
  const m = Math.floor((diff % 3600) / 60)
  if (d > 0) return `${d}天 ${h}小时`
  if (h > 0) return `${h}小时 ${m}分`
  return `${m}分钟`
}

onMounted(() => {
  loadContests()
  // 每 30 秒刷新倒计时（触发重新渲染）
  countdownTimer = setInterval(() => {
    contests.value = [...contests.value]
  }, 30000)
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.contests-container { max-width: 1200px; margin: 0 auto; padding: 30px 20px; }
.contests-header { text-align: center; margin-bottom: 40px; position: relative; }
.contests-header h1 { font-size: 2.5rem; color: #333; margin-bottom: 12px; }
.subtitle { font-size: 1.1rem; color: #666; }
.contest-actions-header { position: absolute; top: 0; right: 0; }

.filter-section { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.scope-filter { margin-bottom: 16px; }
.filter-tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.search-box { width: 260px; }

.invite-code-section { margin-bottom: 24px; display: flex; align-items: center; gap: 10px; }
.invite-label { font-weight: 600; color: #303133; white-space: nowrap; }
.invite-input { max-width: 400px; }

.contests-list { min-height: 300px; }
.contest-card { margin-bottom: 20px; position: relative; overflow: hidden; border-radius: 8px; cursor: pointer; transition: transform 0.2s; }
.contest-card:hover { transform: translateY(-3px); }

.contest-status { position: absolute; top: 16px; right: 0; padding: 4px 14px; color: #fff; font-size: 13px; font-weight: 600; border-radius: 4px 0 0 4px; z-index: 2; }
.status-UPCOMING { background: #409EFF; }
.status-RUNNING { background: #67C23A; }
.status-ENDED { background: #909399; }
.status-DRAFT { background: #E6A23C; }

.official-ribbon { position: absolute; top: -10px; left: -10px; width: 110px; height: 110px; overflow: hidden; z-index: 1; }
.official-ribbon span { position: absolute; display: block; width: 160px; padding: 6px 0; background: #E6A23C; box-shadow: 0 3px 10px rgba(0,0,0,.2); color: #fff; font-size: 13px; font-weight: bold; text-align: center; transform: rotate(-45deg); top: 26px; left: -38px; }

.contest-content { display: flex; padding: 20px 15px; }
.contest-info { flex: 1; padding-right: 20px; }
.contest-title { font-size: 1.4rem; margin-bottom: 10px; color: #333; }
.contest-tags { margin-bottom: 12px; display: flex; gap: 6px; flex-wrap: wrap; }
.contest-desc { color: #666; margin-bottom: 16px; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.contest-meta { display: flex; flex-wrap: wrap; gap: 16px; color: #909399; font-size: 13px; }

.contest-actions { width: 200px; display: flex; flex-direction: column; justify-content: center; align-items: center; border-left: 1px solid #eee; padding-left: 20px; }
.countdown { text-align: center; margin-bottom: 16px; }
.countdown-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.countdown-time { font-size: 17px; font-weight: 700; color: #409EFF; }

.pagination-container { margin-top: 30px; display: flex; justify-content: center; }

@media (max-width: 768px) {
  .filter-section { flex-direction: column; align-items: stretch; }
  .search-box { width: 100%; }
  .contest-content { flex-direction: column; }
  .contest-info { padding-right: 0; margin-bottom: 16px; }
  .contest-actions { width: 100%; border-left: none; border-top: 1px solid #eee; padding-left: 0; padding-top: 16px; }
  .contest-actions-header { position: relative; top: auto; right: auto; margin-top: 12px; }
}
</style>
