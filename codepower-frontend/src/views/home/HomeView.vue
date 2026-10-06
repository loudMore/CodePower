<!-- 首页 — 平台入口、功能导航 -->
<template>
  <div class="home">
    <!-- 主横幅 -->
    <el-card class="banner" :body-style="{padding: '60px 40px'}">
      <div class="hero-badge">在线评测 · 智能训练 · 题解社区</div>
      <h1>Code Power</h1>
      <p>面向算法学习、竞赛训练与能力成长的一体化编程实践平台</p>
      <div class="actions">
        <router-link to="/problems">
          <el-button type="primary" size="large">开始刷题</el-button>
        </router-link>
        <router-link to="/contests" class="ml-2">
          <el-button size="large">参加竞赛</el-button>
        </router-link>
      </div>
      <div class="hero-meta">
        <span>真实提交画像</span>
        <span>多语言沙箱评测</span>
        <span>社区题解沉淀</span>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <el-card class="stats-card" shadow="hover">
        <div class="stat-grid">
          <div class="stat-item clickable-stat" @click="router.push('/problems')">
            <div class="stat-icon stat-icon-problem">题</div>
            <div class="stat-body">
              <div class="stat-value">{{ platformStats.totalProblems }}</div>
              <div class="stat-label">算法题目</div>
            </div>
          </div>
          <div class="stat-item clickable-stat" @click="router.push('/contests')">
            <div class="stat-icon stat-icon-contest">赛</div>
            <div class="stat-body">
              <div class="stat-value">{{ platformStats.totalContests }}</div>
              <div class="stat-label">编程竞赛</div>
            </div>
          </div>
          <div class="stat-item clickable-stat" @click="router.push('/community')">
            <div class="stat-icon stat-icon-user">人</div>
            <div class="stat-body">
              <div class="stat-value">{{ platformStats.totalUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon stat-icon-submit">判</div>
            <div class="stat-body">
              <div class="stat-value">{{ platformStats.totalSubmissions }}</div>
              <div class="stat-label">判题次数</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

   

    <!-- AI特性 -->
    <div class="ai-features">
      <h2 class="section-title">AI赋能学习</h2>
      <el-row :gutter="20">
        <el-col :span="8" :xs="24" :sm="24" :md="8">
          <el-card class="ai-card clickable-card" shadow="hover" @click="router.push('/problems')">
            <div class="ai-icon">🤖</div>
            <h3>AI助手辅导</h3>
            <p>智能分析解题思路，提供个性化指导和提示，帮助你深入理解算法原理</p>
          </el-card>
        </el-col>
        <el-col :span="8" :xs="24" :sm="24" :md="8">
          <el-card class="ai-card clickable-card" shadow="hover" @click="scrollToRecommendations">
            <div class="ai-icon">📊</div>
            <h3>智能题目推荐</h3>
            <p>融合题目标签、难度阶梯和有效提交表现，推荐当前最值得训练的题目</p>
          </el-card>
        </el-col>
        <el-col :span="8" :xs="24" :sm="24" :md="8">
          <el-card class="ai-card clickable-card" shadow="hover" @click="router.push('/problems/create')">
            <div class="ai-icon">📝</div>
            <h3>AI出题助手</h3>
            <p>根据算法标签和难度要求生成多道候选题，出题者可预览、筛选并保存到题库</p>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 适应性训练 -->
    <el-card class="adaptive-card" shadow="hover" :body-style="{padding: '0'}">
      <div class="adaptive-content">
        <div>
          <div class="adaptive-kicker">多指标能力画像 · 有效训练评估</div>
          <h2>适应性训练系统</h2>
          <p>系统不会简单按积分判断水平，而是结合日常提交、已通过题目、标签覆盖、单题尝试次数、难度进阶和同题性能百分位，计算出可解释的能力画像，再把训练目标映射到合适的题目难度和知识标签上。</p>
          <el-space direction="vertical" alignment="start" size="small">
            <div class="adaptive-feature"><el-tag type="success" size="small">✓</el-tag> 关注真实掌握情况，避免反复提交影响能力判断</div>
            <div class="adaptive-feature"><el-tag type="success" size="small">✓</el-tag> 根据标签能力和弱项分布，定位优先补强知识点</div>
            <div class="adaptive-feature"><el-tag type="success" size="small">✓</el-tag> 结合平均难度、最高难度和稳定性，匹配下一阶段题目</div>
            <div class="adaptive-feature"><el-tag type="success" size="small">✓</el-tag> 对久未练习的薄弱标签进行间隔复习推荐</div>
          </el-space>
          <div class="mt-4 adaptive-actions">
            <el-button type="primary" :style="adaptiveButtonStyle" @click="router.push('/problems')">去刷题体验</el-button>
            <el-button text class="adaptive-link-btn" @click="scrollToRecommendations">查看智能推荐</el-button>
          </div>
        </div>
        <div class="adaptive-image">
          <div class="brain-orbit">
            <span class="orbit-pill orbit-pill-left">标签弱项</span>
            <span class="orbit-pill orbit-pill-right">难度阶梯</span>
            <span class="orbit-pill orbit-pill-bottom">同题百分位</span>
            <div class="brain-image">🧠</div>
          </div>
        </div>
      </div>
    </el-card>
     <!-- 功能区域 -->
    <el-row :gutter="20" class="features-section">
      <el-col :span="8" :xs="24" :sm="24" :md="8">
        <el-card class="feature-card" shadow="hover">
          <div class="feature-icon feature-icon-problem"><el-icon><List /></el-icon></div>
          <h3>刷题系统</h3>
          <p>提供丰富的编程题目，从简单到复杂，帮助你逐步提升解题能力和编程技巧。</p>
          <router-link to="/problems" class="link feature-link">
            <el-link type="primary">查看刷题 <el-icon><ArrowRight /></el-icon></el-link>
          </router-link>
        </el-card>
      </el-col>

      <el-col :span="8" :xs="24" :sm="24" :md="8">
        <el-card class="feature-card" shadow="hover">
          <div class="feature-icon feature-icon-contest"><el-icon><Trophy /></el-icon></div>
          <h3>竞赛练习</h3>
          <p>参加模拟比赛，在真实环境中提升解题能力</p>
          <router-link to="/contests" class="link feature-link">
            <el-link type="primary">查看竞赛<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-link>
          </router-link>
        </el-card>
      </el-col>

      <el-col :span="8" :xs="24" :sm="24" :md="8">
        <el-card class="feature-card" shadow="hover">
          <div class="feature-icon feature-icon-community"><el-icon><Comment /></el-icon></div>
          <h3>题解社区</h3>
          <p>浏览优质题解，与其他用户交流解题心得</p>
          <router-link to="/community" class="link feature-link">
            <el-link type="primary">查看社区<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-link>
          </router-link>
        </el-card>
      </el-col>
    </el-row>
 <!-- 热门题目 -->
    <div class="trending-section">
      <h2 class="section-title">热门题目</h2>
      <p class="section-subtitle">展示公开题库中的近期训练入口，点击题目即可进入对应做题页面。</p>
      <el-card class="trending-card" shadow="hover">
        <el-table :data="trendingProblems" stripe style="width: 100%" class="trending-table">
          <el-table-column width="100">
            <template #default="scope">
              <el-tag
                :type="getDifficultyType(scope.row.difficulty)"
                size="small"
                class="difficulty-tag"
                :class="{ 'difficulty-blue': scope.row.difficulty === '困难' }"
              >
                {{ scope.row.difficulty }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="title">
            <template #default="scope">
              <router-link :to="`/problems/${scope.row.id}`" class="problem-title">
                {{ scope.row.title }}
              </router-link>
              <div class="mt-2">
                <el-tag 
                  v-for="tag in scope.row.tags" 
                  :key="tag"
                  size="small"
                  effect="plain"
                  class="mr-1"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="passRate" label="通过率" width="100" align="center" />
        </el-table>
      </el-card>
    </div>

    <!-- 智能推荐 -->
    <div id="smart-recommend" class="recommend-section">
      <h2 class="section-title">智能推荐</h2>
      <p class="section-subtitle">推荐结果来自你的真实训练记录：弱标签优先补强，已掌握题目自动避开，难度按当前能力阶段动态匹配。</p>
      <template v-if="isLoggedIn">
        <el-row v-loading="recommendLoading" :gutter="16">
          <template v-if="recommendedProblems.length > 0">
            <el-col
              v-for="item in recommendedProblems"
              :key="item.id"
              :span="8"
              :xs="24"
              :sm="12"
              :md="8"
            >
              <el-card
                class="recommend-card"
                :class="getRecommendStatusClass(item)"
                shadow="hover"
                @click="router.push(`/problems/${item.id}`)"
              >
                <div class="recommend-header">
                  <span v-if="getRecommendStatusMark(item)" class="recommend-status-mark">
                    {{ getRecommendStatusMark(item) }}
                  </span>
                  <router-link :to="`/problems/${item.id}`" class="recommend-title">
                    {{ item.title }}
                  </router-link>
                  <el-tag
                    :type="getDifficultyType(item.difficulty)"
                    size="small"
                    :class="{ 'difficulty-blue': item.difficulty === '困难' }"
                  >
                    {{ item.difficulty }}
                  </el-tag>
                </div>
                <div class="recommend-score">
                  <span class="score-label">匹配度</span>
                  <el-progress
                    :percentage="item.matchScore"
                    :stroke-width="8"
                    :show-text="true"
                    :color="item.matchScore >= 80 ? '#67C23A' : item.matchScore >= 60 ? '#409EFF' : '#E6A23C'"
                    style="flex: 1"
                  />
                </div>
                <div class="recommend-reason">{{ item.reason }}</div>
                <div class="recommend-tags">
                  <el-tag
                    v-for="tag in (item.focusTags || []).slice(0, 3)"
                    :key="tag"
                    size="small"
                    effect="plain"
                    class="mr-1"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </el-card>
            </el-col>
          </template>
          <el-empty v-else-if="!recommendLoading" description="暂无推荐，多做几道题后系统将为你智能推荐" />
        </el-row>
      </template>
      <el-card v-else class="login-hint-card" shadow="hover">
        <div class="login-hint">
          <span>登录后查看个性化推荐</span>
          <router-link to="/auth/login">
            <el-button type="primary" size="small">去登录</el-button>
          </router-link>
        </div>
      </el-card>
    </div>

    <!-- 平台介绍/项目优势模块 -->
    <el-card class="platform-card" shadow="hover" :body-style="{padding: '0'}">
      <div class="platform-content">
        <div class="platform-info">
          <div class="platform-kicker">主站服务 · Judge0 评测 · 数据分析</div>
          <h2>平台技术与优势</h2>
          <p>Code Power 将用户、题库、社区、数据分析等主业务与代码评测服务解耦。提交进入评测队列后由 Judge0 沙箱执行，系统记录每次运行的状态、耗时、内存和测试点结果，用于反馈用户、更新题目统计，并为后续能力画像和智能推荐提供真实依据。</p>
          <div class="platform-badges">
            <span>沙箱隔离</span>
            <span>队列调度</span>
            <span>多语言支持</span>
            <span>真实数据统计</span>
          </div>
          <el-space direction="vertical" alignment="start" size="small">
            <div class="platform-feature"><el-tag type="success" size="small">✓</el-tag> 在线IDE，支持多语言编程、自动补全、在线运行</div>
            <div class="platform-feature"><el-tag type="success" size="small">✓</el-tag> 丰富社区，题解、讨论、点赞、收藏、关注</div>
            <div class="platform-feature"><el-tag type="success" size="small">✓</el-tag> 考试系统，一键组卷、自动判题、错题本</div>
            <div class="platform-feature"><el-tag type="success" size="small">✓</el-tag> 智能分析，通过多维度图表洞察个人能力，定位知识薄弱点</div>
            <div class="platform-feature"><el-tag type="success" size="small">✓</el-tag> AI辅助，智能推荐题目、自动生成题解</div>
          </el-space>
          <div class="mt-4">
            <el-alert title="平台已服务全国多地用户，持续升级中！" type="info" show-icon :closable="false" />
          </div>
        </div>
        <div class="platform-map">
          <div id="china-map" class="map-canvas"></div>
        </div>
      </div>
    </el-card>

    <!-- 数据分析模块 -->
    <el-card class="analysis-card" shadow="hover">
      <h2 class="section-title">平台数据</h2>
      <p class="section-subtitle">图表根据真实注册时间、公开题目和最近提交记录汇总生成，用于观察平台规模、题目结构和使用活跃度。</p>
      <div class="analysis-charts">
        <div class="chart-block">
          <div class="chart-title">注册用户总量趋势（人）</div>
          <div id="user-growth-chart" class="analysis-chart-canvas"></div>
        </div>
        <div class="chart-block">
          <div class="chart-title">题目难度分布（道）</div>
          <div id="problem-difficulty-pie" class="analysis-chart-canvas"></div>
        </div>
        <div class="chart-block">
          <div class="chart-title">近30天提交活跃时段（按小时段）</div>
          <div id="active-time-bar" class="analysis-chart-canvas"></div>
        </div>
      </div>
    </el-card>

   

   
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ArrowRight, Comment, List, Trophy } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import type { ECharts, EChartsCoreOption } from 'echarts'
import { dataAnalysisApi } from '@/api/dataAnalysis'
import { userApi } from '@/api/user'
import { submissionApi } from '@/api/submission'
import api, { checkTokenValid } from '@/api/index'
import { getDifficultyType } from '@/utils/common'
import { normalizeRegionLabel } from '@/utils/regions'

const router = useRouter()

const trendingProblems = ref<any[]>([])

/** 是否已登录 */
const isLoggedIn = ref(false)

/** 智能推荐题目列表 */
const recommendedProblems = ref<any[]>([])
const recommendLoading = ref(false)
const recommendationStatusMap = ref<Record<number, string | null>>({})

const platformStats = ref({
  totalProblems: 0,
  totalContests: 0,
  totalUsers: 0,
  totalSubmissions: 0
})

const loadPlatformStats = async () => {
  try {
    const res: any = await dataAnalysisApi.getPlatformStats()
    const data = res.data || res
    platformStats.value = {
      totalProblems: Number(data.totalProblems || 0),
      totalContests: Number(data.totalContests || 0),
      totalUsers: Number(data.totalUsers || 0),
      totalSubmissions: Number(data.totalSubmissions || 0)
    }
  } catch {
    platformStats.value = {
      totalProblems: 0,
      totalContests: 0,
      totalUsers: 0,
      totalSubmissions: 0
    }
  }
}

const loadTrendingProblems = async () => {
  try {
    const res: any = await api.get('/api/problems', {
      params: { visibility: 'PUBLIC', status: 1 }
    })
    const data = res.data || res
    const records = Array.isArray(data.records) ? data.records : []
    trendingProblems.value = records.slice(0, 6).map((p: any) => ({
      id: p.id,
      title: p.title,
      difficulty: p.difficulty,
      tags: Array.isArray(p.tags) ? p.tags.map((t: any) => t.name || t) : [],
      passRate: p.acceptRate != null ? `${p.acceptRate}%` : '0%'
    }))
  } catch {
    trendingProblems.value = []
  }
}

const getProblemId = (item: any) => Number(item?.id || item?.problemId || 0)

const attachRecommendationStatuses = async () => {
  if (!isLoggedIn.value || recommendedProblems.value.length === 0) {
    recommendationStatusMap.value = {}
    return
  }
  const ids = [...new Set(recommendedProblems.value.map(getProblemId).filter(id => id > 0))]
  if (ids.length === 0) return
  try {
    const res: any = await submissionApi.getUserProblemStatus(ids)
    const statusMap = res.data || res || {}
    recommendationStatusMap.value = Object.fromEntries(
      ids.map(id => [id, statusMap[id] || null])
    )
    recommendedProblems.value = recommendedProblems.value.map(item => ({
      ...item,
      practiceStatus: recommendationStatusMap.value[getProblemId(item)] || null
    }))
  } catch {
    recommendationStatusMap.value = {}
  }
}

const getRecommendStatus = (item: any) => (
  recommendationStatusMap.value[getProblemId(item)] || item.practiceStatus || null
)

const getRecommendStatusClass = (item: any) => {
  const status = getRecommendStatus(item)
  return {
    'status-accepted': status === 'ACCEPTED',
    'status-attempted': status === 'ATTEMPTED',
    'status-pending': status === 'PENDING' || status === 'RUNNING'
  }
}

const getRecommendStatusMark = (item: any) => {
  const status = getRecommendStatus(item)
  if (status === 'ACCEPTED') return '✓'
  if (status === 'ATTEMPTED') return '×'
  if (status === 'PENDING' || status === 'RUNNING') return '…'
  return ''
}

/** 加载个性化推荐题目（仅登录状态） */
const loadRecommendations = async () => {
  if (!isLoggedIn.value) return
  recommendLoading.value = true
  try {
    const res: any = await userApi.getRecommendations(6)
    const data = res?.data ?? res
    recommendedProblems.value = Array.isArray(data) ? data : []
    await attachRecommendationStatuses()
  } catch {
    recommendedProblems.value = []
    recommendationStatusMap.value = {}
  } finally {
    recommendLoading.value = false
  }
}

const scrollToRecommendations = async () => {
  await nextTick()
  const section = document.getElementById('smart-recommend')
  if (section) {
    section.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } else {
    router.push('/problems')
  }
}

const adaptiveButtonStyle = computed(() => ({
  background: 'linear-gradient(135deg, #409EFF 0%, #2c78d4 100%)',
  borderColor: 'transparent',
  padding: '12px 25px'
}))

let userGrowthChart: ECharts | null = null
let problemDifficultyPie: ECharts | null = null
let activeTimeBar: ECharts | null = null
let mapChart: ECharts | null = null
let resizeHandler: (() => void) | null = null

const extractDataPayload = (payload: any) => payload?.data ?? payload ?? {}

const normalizeMapData = (payload: any) => {
  const data = extractDataPayload(payload)
  const rawList = [
    data?.mapData,
    data?.data,
    data?.records,
    data?.list,
    data
  ].find(Array.isArray)

  if (!Array.isArray(rawList)) return []

  return rawList
    .map((item: any) => ({
      name: normalizeRegionLabel(item?.name || item?.province || item?.region || item?.label || ''),
      value: Number(item?.value ?? item?.count ?? item?.userCount ?? item?.total ?? 0)
    }))
    .filter((item: any) => item.name)
    .reduce((acc: Array<{ name: string; value: number }>, item) => {
      const existing = acc.find(entry => entry.name === item.name)
      if (existing) {
        existing.value += item.value
      } else {
        acc.push(item)
      }
      return acc
    }, [])
}

const getMapPieces = (mapData: Array<{ name: string; value: number }>) => {
  const maxValue = Math.max(...mapData.map(item => item.value), 0)
  if (maxValue <= 0) {
    return [{ min: 0, label: '暂无数据', color: '#e0f3ff' }]
  }

  const step = Math.max(1, Math.ceil(maxValue / 5))
  return [
    { min: step * 4, label: `${step * 4}+`, color: '#0057b7' },
    { min: step * 3, max: step * 4 - 1, label: `${step * 3}-${step * 4 - 1}`, color: '#409EFF' },
    { min: step * 2, max: step * 3 - 1, label: `${step * 2}-${step * 3 - 1}`, color: '#7ecfff' },
    { min: step, max: step * 2 - 1, label: `${step}-${step * 2 - 1}`, color: '#b3e5fc' },
    { min: 0, max: step - 1, label: `0-${step - 1}`, color: '#e0f3ff' }
  ]
}

const chartGrid = {
  left: 64,
  right: 28,
  top: 54,
  bottom: 52,
  containLabel: true
} as const

const valueAxis = (name: string) => ({
  type: 'value',
  name,
  nameLocation: 'middle' as const,
  nameGap: 42,
  axisLabel: {
    margin: 12,
    hideOverlap: true
  }
})

const categoryAxis = (data: any[], name?: string) => ({
  type: 'category',
  data,
  name,
  nameLocation: 'middle' as const,
  nameGap: 32,
  axisLabel: {
    margin: 12,
    hideOverlap: true
  }
})

const initChinaMap = async () => {
  try {
    const chartDom = document.getElementById('china-map')
    if (!chartDom) return

    // 按需加载 ECharts，避免首屏加载过大
    const echarts = await import('echarts')

    const response = await dataAnalysisApi.getUserDistributionMapData()
    const mapData = normalizeMapData(response)

    const geoJsonUrl = '/map/100000_full.json'
    const mapResponse = await fetch(geoJsonUrl)
    if (!mapResponse.ok) {
      throw new Error(`地图数据加载失败: ${mapResponse.status}`)
    }
    const chinaGeoJson = await mapResponse.json()

    echarts.registerMap('china', chinaGeoJson)
    mapChart?.dispose()
    mapChart = echarts.init(chartDom)

    mapChart.setOption({
      title: {
        text: '全国用户分布（人）',
        left: 'center',
        top: 12,
        textStyle: { fontSize: 16, fontWeight: 600 }
      },
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => `${params.name}<br/>用户数: ${Number(params.value || 0)}人`
      },
      visualMap: {
        type: 'piecewise',
        pieces: getMapPieces(mapData),
        left: 12,
        bottom: 10,
        show: true,
        textStyle: { color: '#606266' }
      },
      series: [{
        name: '用户数',
        type: 'map',
        map: 'china',
        roam: true,
        label: { show: false },
        emphasis: {
          label: { show: true, color: '#303133' },
          itemStyle: { areaColor: '#8ec5ff' }
        },
        data: mapData
      }]
    } as EChartsCoreOption)
  } catch (error) {
    console.error('初始化地图失败', error)
  }
}

const initCharts = async () => {
  try {
    // 按需加载 ECharts
    const echarts = await import('echarts')

    const [userGrowthResponse, difficultyResponse, activeTimeResponse] = await Promise.all([
      dataAnalysisApi.getUserGrowthTrendData(),
      dataAnalysisApi.getProblemDifficultyDistributionData(),
      dataAnalysisApi.getUserActiveHoursData()
    ])

    const userGrowthData = extractDataPayload(userGrowthResponse)
    const difficultyData = extractDataPayload(difficultyResponse)
    const activeTimeData = extractDataPayload(activeTimeResponse)

    const userGrowthDom = document.getElementById('user-growth-chart')
    if (userGrowthDom) {
      userGrowthChart?.dispose()
      userGrowthChart = echarts.init(userGrowthDom)
      userGrowthChart.setOption({
        tooltip: { trigger: 'axis', formatter: '{b}月<br/>累计注册用户: {c}人' },
        xAxis: categoryAxis(userGrowthData?.xAxisData || [], '月份'),
        yAxis: valueAxis('累计人数'),
        series: [{
          data: userGrowthData?.yAxisData || [],
          type: 'line',
          smooth: true,
          areaStyle: { color: '#e0f3ff' },
          lineStyle: { color: '#409EFF', width: 3 },
          symbol: 'circle',
          symbolSize: 8
        }],
        grid: chartGrid
      } as EChartsCoreOption)
    }

    const problemDifficultyDom = document.getElementById('problem-difficulty-pie')
    if (problemDifficultyDom) {
      problemDifficultyPie?.dispose()
      problemDifficultyPie = echarts.init(problemDifficultyDom)
      const pieSource = Array.isArray(difficultyData?.data)
        ? difficultyData.data
        : Array.isArray(difficultyData)
          ? difficultyData
          : []
      const pieData = pieSource.map((item: any) => ({
        value: Number(item?.value || 0),
        name: item?.name || '未知',
        itemStyle: item?.color ? { color: item.color } : undefined
      }))

      problemDifficultyPie.setOption({
        tooltip: { trigger: 'item', formatter: '{b}<br/>题目数: {c}道 ({d}%)' },
        legend: {
          orient: 'vertical',
          right: 8,
          top: 'center',
          itemWidth: 18,
          itemHeight: 12,
          textStyle: { fontSize: 13 },
          data: pieData.map(item => item.name)
        },
        series: [{
          name: '题目难度',
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['36%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold' } },
          labelLine: { show: false },
          data: pieData
        }]
      } as EChartsCoreOption)
    }

    const activeTimeDom = document.getElementById('active-time-bar')
    if (activeTimeDom) {
      activeTimeBar?.dispose()
      activeTimeBar = echarts.init(activeTimeDom)
      activeTimeBar.setOption({
        tooltip: { trigger: 'axis', formatter: '{b}<br/>活跃用户: {c}人' },
        xAxis: categoryAxis(activeTimeData?.xAxisData || [], '小时段'),
        yAxis: valueAxis('活跃用户数'),
        series: [{
          data: activeTimeData?.yAxisData || [],
          type: 'bar',
          itemStyle: { color: '#409EFF', borderRadius: [6, 6, 0, 0] },
          barWidth: 32
        }],
        grid: chartGrid
      } as EChartsCoreOption)
    }
  } catch (error) {
    console.error('初始化图表失败', error)
  }
}

const resizeCharts = () => {
  userGrowthChart?.resize()
  problemDifficultyPie?.resize()
  activeTimeBar?.resize()
  mapChart?.resize()
}

onMounted(async () => {
  isLoggedIn.value = checkTokenValid()
  await Promise.all([loadPlatformStats(), loadTrendingProblems(), loadRecommendations()])
  await nextTick()
  await Promise.all([initCharts(), initChinaMap()])
  resizeHandler = () => resizeCharts()
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  userGrowthChart?.dispose()
  problemDifficultyPie?.dispose()
  activeTimeBar?.dispose()
  mapChart?.dispose()
  userGrowthChart = null
  problemDifficultyPie = null
  activeTimeBar = null
  mapChart = null
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 30px;
  position: relative;
  padding-bottom: 8px;
  background:
    radial-gradient(circle at top right, rgba(64, 158, 255, 0.08), transparent 28%),
    radial-gradient(circle at left center, rgba(44, 120, 212, 0.05), transparent 25%);
}

.banner {
  background: linear-gradient(135deg, #409EFF 0%, #2c78d4 100%);
  color: white;
  border: none;
  border-radius: 18px;
  overflow: hidden;
  text-align: center;
  position: relative;
  box-shadow: 0 18px 36px rgba(44, 120, 212, 0.16) !important;
}

.banner :deep(.el-card__body) {
  position: relative;
  z-index: 1;
}

.banner::before,
.banner::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.banner::before {
  width: 220px;
  height: 220px;
  right: -70px;
  top: -90px;
  background: rgba(255, 255, 255, 0.08);
}

.banner::after {
  width: 150px;
  height: 150px;
  left: -60px;
  bottom: -80px;
  background: rgba(255, 255, 255, 0.06);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  margin-bottom: 18px;
  padding: 7px 14px;
  border-radius: 999px;
  color: #eaf5ff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.24);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  backdrop-filter: blur(10px);
}

.banner h1 {
  font-size: 36px;
  margin-bottom: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.banner p {
  font-size: 18px;
  margin-bottom: 30px;
  opacity: 0.9;
  line-height: 1.8;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.hero-meta {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.hero-meta span {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
}

.stats-section {
  margin-top: -15px;
  position: relative;
  z-index: 2;
}

.stats-card {
  border-radius: 18px;
  border: 1px solid #e1ecf9;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 34px rgba(44, 120, 212, 0.10) !important;
  backdrop-filter: blur(12px);
}

.stats-card :deep(.el-card__body) {
  padding: 18px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 16px 18px;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #edf4ff;
  text-align: left;
}

.stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 800;
}

.stat-icon-problem {
  color: #2c78d4;
  background: #edf6ff;
}

.stat-icon-contest {
  color: #b7791f;
  background: #fff7e6;
}

.stat-icon-user {
  color: #2f855a;
  background: #ebf9f1;
}

.stat-icon-submit {
  color: #c05621;
  background: #fff1e8;
}

.stat-body {
  min-width: 0;
}

.stat-value {
  font-size: 30px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 4px;
  line-height: 1;
}

.stat-label {
  color: #64748b;
  font-size: 14px;
}

.section-title {
  font-size: 24px;
  color: #1f2937;
  margin-bottom: 16px;
  padding-left: 14px;
  border-left: 4px solid #409EFF;
  line-height: 1.1;
  letter-spacing: 0.02em;
}

.section-subtitle {
  margin: -4px 0 18px;
  color: #64748b;
  line-height: 1.75;
  font-size: 14px;
}

/* AI特性部分样式 */
.ai-features {
  margin-bottom: 0;
}

.ai-card {
  text-align: center;
  height: 100%;
  border-radius: 16px;
  border: 1px solid #e6eef8;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 10px 24px rgba(44, 120, 212, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  position: relative;
  overflow: hidden;
}

.ai-card::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: linear-gradient(90deg, #409EFF 0%, #6bc2ff 100%);
}

.ai-card:hover {
  border-color: #b8d8ff;
  box-shadow: 0 16px 30px rgba(44, 120, 212, 0.12);
  transform: translateY(-4px);
}

.ai-icon {
  font-size: 42px;
  margin-bottom: 16px;
  display: inline-block;
  background: linear-gradient(135deg, #409EFF 0%, #2c78d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.ai-card h3 {
  font-size: 20px;
  margin-bottom: 15px;
  color: #1f2937;
}

.ai-card p {
  color: #5f6b7a;
  line-height: 1.6;
}

/* 适应性训练部分样式 */
.adaptive-card {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #dbeafe;
  box-shadow: 0 16px 34px rgba(44, 120, 212, 0.09) !important;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.adaptive-content {
  display: flex;
  align-items: center;
  gap: 0;
}

.adaptive-content > div {
  flex: 1;
  padding: 40px;
}

.adaptive-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2c78d4;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.adaptive-content h2 {
  font-size: 28px;
  color: #1f2937;
  margin-bottom: 14px;
  letter-spacing: 0.01em;
}

.adaptive-content p {
  color: #5f6b7a;
  line-height: 1.7;
  margin-bottom: 20px;
}

.adaptive-feature {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #24364b;
  background: rgba(245, 249, 255, 0.82);
  border: 1px solid #edf4ff;
  border-radius: 10px;
  padding: 9px 12px;
  line-height: 1.5;
}

.adaptive-feature .el-tag {
  flex-shrink: 0;
}

.adaptive-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.adaptive-link-btn {
  color: #2c78d4;
  padding-left: 0;
}

.adaptive-image {
  flex: 1;
  background:
    radial-gradient(circle at 28% 28%, rgba(64, 158, 255, 0.18), transparent 28%),
    radial-gradient(circle at 75% 70%, rgba(44, 120, 212, 0.12), transparent 20%),
    linear-gradient(135deg, #f4f9ff 0%, #e7f2ff 100%);
  height: 100%;
  min-height: 350px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.adaptive-content > .adaptive-image {
  padding: 0;
}

.adaptive-image::before,
.adaptive-image::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.adaptive-image::before {
  width: 250px;
  height: 250px;
  border: 1px dashed rgba(64, 158, 255, 0.28);
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.adaptive-image::after {
  width: 180px;
  height: 180px;
  background: rgba(255, 255, 255, 0.36);
  filter: blur(14px);
}

.brain-orbit {
  position: relative;
  width: min(380px, 100%);
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.brain-orbit::before,
.brain-orbit::after {
  content: '';
  position: absolute;
  inset: 18px;
  border-radius: 50%;
}

.brain-orbit::before {
  border: 1px solid rgba(64, 158, 255, 0.22);
}

.brain-orbit::after {
  inset: 42px;
  border: 1px dashed rgba(44, 120, 212, 0.18);
}

.brain-image {
  font-size: 118px;
  animation: float 3s ease-in-out infinite;
  filter: drop-shadow(0 18px 24px rgba(44, 120, 212, 0.18));
  z-index: 2;
}

.orbit-pill {
  position: absolute;
  z-index: 3;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #2c78d4;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(44, 120, 212, 0.12);
  backdrop-filter: blur(10px);
}

.orbit-pill-left {
  left: 6px;
  top: 60px;
}

.orbit-pill-right {
  right: 6px;
  top: 70px;
}

.orbit-pill-bottom {
  left: 50%;
  bottom: 20px;
  transform: translateX(-50%);
}

@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
  100% { transform: translateY(0px); }
}

/* 功能卡片 */
.feature-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  border: 1px solid #e6eef8;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 10px 24px rgba(44, 120, 212, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.feature-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 26px 24px 24px;
}

.feature-card:hover {
  transform: translateY(-4px);
  border-color: #b8d8ff;
  box-shadow: 0 16px 30px rgba(44, 120, 212, 0.12);
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  border-radius: 16px;
  font-size: 22px;
}

.feature-icon-problem {
  color: #2c78d4;
  background: #edf6ff;
}

.feature-icon-contest {
  color: #b7791f;
  background: #fff7e6;
}

.feature-icon-community {
  color: #2f855a;
  background: #ebf9f1;
}

.feature-card h2,
.feature-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #1f2937;
}

.feature-card p {
  color: #5f6b7a;
  line-height: 1.6;
  margin-bottom: 20px;
  flex-grow: 1;
}

.feature-link {
  margin-top: auto;
}

/* 热门题目 */
.trending-card {
  border-radius: 16px;
  border: 1px solid #e3edf9;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 12px 28px rgba(44, 120, 212, 0.08) !important;
}

.trending-card :deep(.el-card__body) {
  padding: 6px 0;
}

.trending-table :deep(.el-table__header-wrapper th) {
  background: #f4f8ff;
  color: #475569;
  font-weight: 700;
}

.trending-table :deep(.el-table__row) {
  transition: background-color 0.18s ease;
}

.trending-table :deep(.el-table__row:hover td) {
  background: #f7fbff !important;
}

.difficulty-tag {
  border-radius: 999px;
  font-weight: 700;
}

.difficulty-blue {
  color: #2c78d4 !important;
  background: #edf6ff !important;
  border-color: #b8d8ff !important;
}

.problem-title {
  color: #1f2937;
  font-weight: 700;
  text-decoration: none;
}

.problem-title:hover {
  color: #409EFF;
}

/* 辅助样式 */
.ml-2 {
  margin-left: 8px;
}

.mt-2 {
  margin-top: 8px;
}

.mt-4 {
  margin-top: 16px;
}

.mr-1 {
  margin-right: 4px;
}

/* 可点击卡片 */
.clickable-card {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.clickable-card:hover {
  transform: translateY(-4px);
}

/* 可点击统计项 */
.clickable-stat {
  cursor: pointer;
  transition: transform 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.clickable-stat:hover {
  transform: translateY(-3px);
  border-color: #cfe5ff;
  background: #f6fbff;
  box-shadow: 0 10px 22px rgba(44, 120, 212, 0.08);
}

/* 智能推荐区域 */
.recommend-section {
  margin-bottom: 0;
  padding-top: 4px;
}

.recommend-card {
  cursor: pointer;
  margin-bottom: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  height: 100%;
  border-radius: 16px;
  border: 1px solid #e3edf9;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 10px 24px rgba(44, 120, 212, 0.06);
}

.recommend-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
}

.recommend-card:hover {
  transform: translateY(-4px);
  border-color: #b8d8ff;
  box-shadow: 0 16px 30px rgba(44, 120, 212, 0.12);
}

.recommend-card.status-accepted {
  border-color: #d5efc8;
  background: #f3fbef;
}

.recommend-card.status-attempted {
  border-color: #ffd6d6;
  background: #fff1f0;
}

.recommend-card.status-pending {
  border-color: #faecd8;
  background: #fff8ec;
}

.recommend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}

.recommend-status-mark {
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  color: #67c23a;
  background: #ffffff;
}

.recommend-card.status-attempted .recommend-status-mark {
  color: #f56c6c;
}

.recommend-card.status-pending .recommend-status-mark {
  color: #e6a23c;
}

.recommend-title {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.recommend-title:hover {
  color: #409EFF;
}

.recommend-score {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.score-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.recommend-reason {
  font-size: 13px;
  color: #5f6b7a;
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}

.recommend-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: auto;
}

.login-hint-card {
  text-align: center;
  border-radius: 14px;
}

.login-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px 0;
  font-size: 15px;
  color: #666;
}

@media (max-width: 768px) {
  .home {
    gap: 22px;
  }

  .adaptive-content {
    flex-direction: column;
  }
  
  .adaptive-image {
    width: 100%;
    min-height: 200px;
  }
  
  .brain-orbit {
    height: 180px;
    width: min(100%, 280px);
  }

  .orbit-pill {
    display: none;
  }

  .brain-image {
    font-size: 80px;
  }
  
  .actions {
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }
  
  .banner h1 {
    font-size: 28px;
  }
  
  .banner p {
    font-size: 16px;
  }
  
  .stat-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .section-title {
    font-size: 22px;
  }

  .adaptive-content > div {
    padding: 28px 22px;
  }
}

@media (min-width: 768px) and (max-width: 992px) {
  .features-section .el-col:last-child {
    margin-top: 20px;
  }
}

#china-map.map-canvas {
  width: 420px;
  height: 320px;
  max-width: 100%;
}

.analysis-chart-canvas {
  width: 100%;
  min-width: 320px;
  height: 220px;
}

.platform-card {
  margin: 40px 0 32px 0;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #e1ecf9;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 16px 32px rgba(44, 120, 212, 0.08) !important;
}

.platform-card :deep(.el-card__body) {
  padding: 0;
}

.platform-content {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 38px 32px 34px 32px;
}
.platform-info {
  flex: 1;
  min-width: 260px;
}
.platform-kicker {
  display: inline-flex;
  align-items: center;
  margin-bottom: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2c78d4;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.platform-info h2 {
  margin: 0 0 14px;
  font-size: 26px;
  color: #1f2937;
}
.platform-info p {
  color: #55657a;
  line-height: 1.8;
}
.platform-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0 10px;
}
.platform-badges span {
  padding: 6px 12px;
  border-radius: 999px;
  background: #f5faff;
  border: 1px solid #e3edf9;
  color: #36516f;
  font-size: 13px;
}
.platform-map {
  flex-shrink: 0;
  min-width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.platform-feature {
  font-size: 16px;
  color: #31465a;
  line-height: 1.6;
  display: flex;
  align-items: center;
  gap: 8px;
}
@media (max-width: 1100px) {
  .platform-content {
    flex-direction: column;
    gap: 18px;
    padding: 18px 8px 18px 8px;
  }
  .platform-map {
    min-width: 0;
    width: 100%;
    justify-content: flex-start;
  }

  #china-map.map-canvas {
    width: 100%;
    min-height: 320px;
  }
}

.analysis-card {
  margin: 40px 0 32px 0;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #e1ecf9;
  box-shadow: 0 16px 32px rgba(44, 120, 212, 0.08) !important;
  padding: 24px 32px 32px 32px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}
.analysis-charts {
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
  justify-content: space-between;
  align-items: flex-start;
}
.chart-block {
  background:
    radial-gradient(circle at 20% 10%, rgba(64, 158, 255, 0.08), transparent 32%),
    #f8fbff;
  border: 1px solid #e6eef8;
  border-radius: 16px;
  box-shadow: 0 12px 26px rgba(44, 120, 212, 0.06);
  padding: 18px 16px 12px 16px;
  min-width: 320px;
  flex: 1 1 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 10px;
}
@media (max-width: 1100px) {
  .analysis-charts {
    flex-direction: column;
    gap: 18px;
  }
  .chart-block {
    min-width: 0;
    width: 100%;
    padding: 12px 8px 10px 8px;
  }

  .analysis-chart-canvas {
    min-width: 0;
    height: 220px;
  }
}

</style>
