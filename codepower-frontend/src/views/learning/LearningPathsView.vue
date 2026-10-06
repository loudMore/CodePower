<!-- 学习路线列表页面 -->
<template>
  <div class="learning-paths">
    <el-card class="banner-card" shadow="never" :body-style="{padding: '48px 56px 48px 60px', background: 'linear-gradient(90deg, #e3f0ff 60%, #fff 100%)', borderRadius: '20px'}">
      <div class="banner">
        <div class="banner-left">
          <h1>闯关学习</h1>
          <p class="intro">趣味闯关，系统掌握编程知识！<br>微课视频+闯关模式，助你高效进阶，开启你的编程冒险之旅。</p>
        </div>
        <div class="banner-right">
          <img class="banner-img" src="https://img.alicdn.com/imgextra/i4/O1CN01Qw6QwC1wQw6QwC1wQw6QwC.png" alt="闯关学习" loading="lazy" />
        </div>
      </div>
    </el-card>
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon stat-blue"><i class="el-icon-s-operation"></i></div>
        <div class="stat-value">{{ overview.totalPaths }}</div>
        <div class="stat-label">学习路径</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-green"><i class="el-icon-connection"></i></div>
        <div class="stat-value">{{ overview.totalStages }}</div>
        <div class="stat-label">总关卡数</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-orange"><i class="el-icon-user"></i></div>
        <div class="stat-value">{{ overview.activeLearners }}</div>
        <div class="stat-label">累计学习人数</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-red"><i class="el-icon-trophy"></i></div>
        <div class="stat-value">{{ overview.completedPasses }}</div>
        <div class="stat-label">累计闯关次数</div>
      </div>
    </div>
    <el-card class="charts-card" shadow="hover">
      <div class="charts-section">
        <div class="chart-box">
          <div class="chart-title">每月学习人数变化</div>
          <div ref="lineChartRef" class="echarts-box"></div>
        </div>
        <div class="chart-box">
          <div class="chart-title">最受欢迎学习路径TOP5</div>
          <div ref="barChartRef" class="echarts-box"></div>
        </div>
      </div>
    </el-card>

    <el-card class="charts-card" shadow="hover">
      <div class="chart-title main-title">个人能力画像</div>
      <div class="ability-profile-section">
        <div class="ability-profile-chart-box">
          <div ref="abilityRadarChartRef" class="ability-radar-chart"></div>
        </div>
        <div class="ability-profile-side">
          <div class="ability-panel">
            <div class="ability-panel-title">评估说明</div>
            <div class="ability-analysis-text">{{ abilityAnalysisBasis }}</div>
          </div>
          <div class="ability-panel">
            <div class="ability-panel-title">核心概览</div>
            <div class="ability-overview-grid">
              <div class="ability-overview-item">
                <span class="ability-overview-label">已通过题目</span>
                <strong>{{ abilityOverview.solvedCount }}</strong>
              </div>
              <div class="ability-overview-item">
                <span class="ability-overview-label">有效通过率</span>
                <strong>{{ abilityOverview.acceptanceRate }}%</strong>
              </div>
              <div class="ability-overview-item">
                <span class="ability-overview-label">覆盖标签</span>
                <strong>{{ abilityOverview.coveredTagCount }}</strong>
              </div>
              <div class="ability-overview-item">
                <span class="ability-overview-label">最高难度</span>
                <strong>{{ abilityOverview.highestSolvedDifficulty }}</strong>
              </div>
            </div>
          </div>
          <div class="ability-panel">
            <div class="ability-panel-title">优先补强</div>
            <div v-if="abilityWeaknesses.length" class="ability-weakness-list">
              <div v-for="item in abilityWeaknesses" :key="item.dimension" class="ability-weakness-item">
                <span>{{ item.dimension }}</span>
                <strong>{{ item.score }}</strong>
              </div>
            </div>
            <div v-else class="ability-empty-text">继续保持，当前暂无明显短板。</div>
          </div>
        </div>
      </div>
    </el-card>

    <section class="recommend-band">
      <div class="recommend-band-header">
        <div>
          <div class="section-kicker">智能推荐</div>
          <h2>当前最值得练的题目</h2>
          <p>这里沿用首页的个性化推荐结果，但在学习路径页改成更像侧栏清单的表达，方便你顺手切题。</p>
        </div>
        <el-tag type="success" effect="light">{{ recommendedProblems.length }} 道推荐</el-tag>
      </div>

      <template v-if="isLoggedIn">
        <div v-loading="recommendLoading" class="recommend-band-body">
          <div v-if="recommendedProblems.length" class="recommend-list">
            <article
              v-for="(item, index) in recommendedProblems"
              :key="item.id"
              class="recommend-row"
              :class="getRecommendStatusClass(item)"
            >
              <div class="recommend-rank">
                <span v-if="getRecommendStatusMark(item)">{{ getRecommendStatusMark(item) }}</span>
                <template v-else>{{ String(index + 1).padStart(2, '0') }}</template>
              </div>
              <div class="recommend-main">
                <div class="recommend-topline">
                  <router-link :to="`/problems/${item.id}`" class="recommend-title">
                    {{ item.title }}
                  </router-link>
                  <el-tag :type="getDifficultyType(item.difficulty)" size="small">
                    {{ item.difficulty }}
                  </el-tag>
                </div>
                <div class="recommend-meter">
                  <div class="recommend-meter-head">
                    <span>匹配度</span>
                    <strong>{{ item.matchScore }}%</strong>
                  </div>
                  <el-progress
                    :percentage="item.matchScore"
                    :stroke-width="8"
                    :show-text="false"
                    :color="item.matchScore >= 80 ? '#67C23A' : item.matchScore >= 60 ? '#409EFF' : '#E6A23C'"
                  />
                </div>
                <p class="recommend-reason">{{ item.reason }}</p>
                <div class="recommend-tags">
                  <el-tag
                    v-for="tag in (item.focusTags || []).slice(0, 4)"
                    :key="tag"
                    size="small"
                    effect="plain"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
              <div class="recommend-side">
                <div class="recommend-score-circle">
                  {{ item.matchScore }}<small>%</small>
                </div>
                <el-button type="primary" plain size="small" @click="router.push(`/problems/${item.id}`)">
                  开始练习
                </el-button>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无推荐，多做几道题后系统会在这里给你补题建议" />
        </div>
      </template>

      <div v-else class="recommend-login-hint">
        <div class="recommend-login-copy">
          <strong>登录后可看到个性化推荐</strong>
          <p>推荐会根据你的提交记录、标签覆盖和当前能力阶段动态调整。</p>
        </div>
        <el-button type="primary" @click="router.push('/auth/login')">去登录</el-button>
      </div>
    </section>

    <div class="language-grid">
      <div class="card-container" v-for="path in learningPaths" :key="path.id">
        <div class="flip-card" :class="{ 'is-flipped': flippedCards[path.id] }">
          <div class="lang-card card-front">
            <div class="custom-flip-btn" @click.stop="toggleCardFlip(path.id)">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4C7.58 4 4.01 7.58 4.01 12C4.01 16.42 7.58 20 12 20C15.73 20 18.84 17.45 19.73 14H17.65C16.83 16.33 14.61 18 12 18C8.69 18 6 15.31 6 12C6 8.69 8.69 6 12 6C13.66 6 15.14 6.69 16.22 7.78L13 11H20V4L17.65 6.35Z" fill="#1976d2"/>
              </svg>
            </div>
            <div class="icon-container">
              <div class="lang-icon-bg"></div>
              <div class="lang-icon" v-html="path.advancedIcon || path.icon"></div>
            </div>
            <div class="lang-title">{{ path.display }}</div>
            <el-button type="primary" size="large" class="start-btn" @click="$router.push(`/learning-paths/${path.id}`)">开始学习</el-button>
          </div>
          <div class="lang-card card-back">
            <div class="custom-flip-btn" @click.stop="toggleCardFlip(path.id)">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M20 11H7.83L13.42 5.41L12 4L4 12L12 20L13.41 18.59L7.83 13H20V11Z" fill="#1976d2"/>
              </svg>
            </div>
            <h3>{{ path.display }} - 课程统计</h3>
            <div class="stat-row">
              <div class="stat-label">学习人数：</div>
              <div class="stat-number">{{ path.stats.activeLearners }}</div>
            </div>
            <div class="stat-row">
              <div class="stat-label">课时数量：</div>
              <div class="stat-number">{{ path.stats.lessons }}课时</div>
            </div>
            <div class="stat-row">
              <div class="stat-label">累计通关：</div>
              <div class="stat-number">{{ path.stats.completedPasses }}</div>
            </div>
            <div class="stat-row">
              <div class="stat-label">平均完成率：</div>
              <div class="stat-number">{{ path.stats.completionRate }}%</div>
            </div>
            <div class="progress-container">
              <div class="progress-label">课程难度</div>
              <el-progress
                :percentage="path.stats.difficulty"
                :color="getDifficultyInfo(path.stats.difficulty).color"
                :show-text="false"/>
              <div class="difficulty-level" :style="{ color: getDifficultyInfo(path.stats.difficulty).color }">
                {{ getDifficultyInfo(path.stats.difficulty).level }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dataAnalysisApi } from '@/api/dataAnalysis'
import { learningPathApi } from '@/api/learningPath'
import { userApi } from '@/api/user'
import { submissionApi } from '@/api/submission'
import { checkTokenValid } from '@/api/index'
import { buildAbilityRadarOption, getDifficultyType, normalizeAbilityProfile } from '@/utils/common'

let lineChart: any = null
let barChart: any = null
let abilityRadarChart: any = null
const router = useRouter()
const lineChartRef = ref()
const barChartRef = ref()
const abilityRadarChartRef = ref()
const flippedCards = reactive<Record<number, boolean>>({})
const learningPaths = ref<any[]>([])
const isLoggedIn = ref(checkTokenValid())
const recommendedProblems = ref<any[]>([])
const recommendLoading = ref(false)
const recommendationStatusMap = ref<Record<number, string | null>>({})
const overview = reactive({
  totalPaths: 0,
  totalStages: 0,
  activeLearners: 0,
  completedPasses: 0
})
const abilityProfile = reactive({
  dimensions: [] as Array<{ key: string; name: string; score: number; evidence: string }>,
  weaknesses: [] as Array<{ name: string; score: number; highlight: string; evidence: string }>,
  overview: {
    solvedCount: 0,
    totalAttempts: 0,
    acceptanceRate: 0,
    coveredTagCount: 0,
    averageSolvedDifficulty: 0,
    highestSolvedDifficulty: '暂无'
  },
  analysisBasis: '系统会结合最近一段时间的通过记录、标签覆盖、难度分布与提交稳定性，定期生成能力画像。'
})

const abilityAnalysisBasis = computed(() => abilityProfile.analysisBasis)
const abilityOverview = computed(() => abilityProfile.overview)
const abilityWeaknesses = computed(() => abilityProfile.weaknesses.map(item => ({
  dimension: item.name,
  score: item.score
})))

const levelColors = {
  basic: '#4CAF50',
  advanced: '#2196F3',
  master: '#FF5722'
}

const toggleCardFlip = (cardId: number) => {
  flippedCards[cardId] = !flippedCards[cardId]
}

const getDifficultyInfo = (difficulty: number) => {
  if (difficulty < 40) {
    return { level: '基础', color: levelColors.basic }
  }
  if (difficulty < 75) {
    return { level: '进阶', color: levelColors.advanced }
  }
  return { level: '大师', color: levelColors.master }
}

const langColors = ['#1976d2', '#67c23a', '#e6a23c', '#f56c6c', '#00599C', '#3776AB', '#E44D26', '#764ABC']
const langAbbr = (title: string) => {
  const map: Record<string, string> = {
    c: 'C',
    'c++': 'C++',
    cpp: 'C++',
    java: 'Java',
    python: 'Py',
    javascript: 'JS',
    '数据结构': 'DS',
    '算法': 'Algo'
  }
  const lower = title.toLowerCase()
  for (const [k, v] of Object.entries(map)) {
    if (lower.includes(k)) return v
  }
  return title.substring(0, 2).toUpperCase()
}

const buildIcon = (abbr: string, color: string) => {
  return `<svg width='64' height='64' viewBox='0 0 64 64'>
    <defs><linearGradient id="grad-${abbr}" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:${color};stop-opacity:1" />
      <stop offset="100%" style="stop-color:${color};stop-opacity:0.7" />
    </linearGradient></defs>
    <circle cx='32' cy='32' r='30' fill='url(#grad-${abbr})' stroke='#fff' stroke-width='1'/>
    <circle cx='32' cy='32' r='24' fill='${color}' opacity='0.7'/>
    <text x='32' y='40' text-anchor='middle' font-size='${abbr.length > 2 ? 18 : 24}' font-weight='bold' fill='#fff'>${abbr}</text>
  </svg>`
}

const difficultyToPercent = (difficulty?: string) => {
  const diffMap: Record<string, number> = {
    EASY: 30,
    简单: 30,
    MEDIUM: 55,
    普通: 55,
    HARD: 80,
    困难: 80,
    EXTREME: 95,
    极限: 95
  }
  return diffMap[difficulty || ''] || 50
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

const resizeHandler = () => {
  lineChart?.resize()
  barChart?.resize()
  abilityRadarChart?.resize()
}

const extractPayload = (payload: any) => payload?.data ?? payload ?? {}

const extractRecords = (payload: any) => {
  const data = extractPayload(payload)
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.data)) return data.data
  return []
}

const extractChartData = (payload: any) => {
  const data = extractPayload(payload)
  return data?.data ?? data
}

const loadLearningPaths = async () => {
  const [pathsRes, overviewRes] = await Promise.all([
    learningPathApi.listPaths(),
    dataAnalysisApi.getLearningPathOverview()
  ])
  const pathData = extractPayload(pathsRes)
  const overviewData = extractPayload(overviewRes)
  const list = extractRecords(pathData)
  const pathStats = Array.isArray(overviewData?.pathStats)
    ? overviewData.pathStats
    : Array.isArray(overviewData?.data?.pathStats)
      ? overviewData.data.pathStats
      : []
  const statMap = new Map<number, any>()
  ;pathStats.forEach((item: any) => {
    statMap.set(item.pathId, item)
  })

  overview.totalPaths = Number(overviewData.totalPaths || overviewData?.data?.totalPaths || list.length)
  overview.totalStages = Number(overviewData.totalStages || overviewData?.data?.totalStages || 0)
  overview.activeLearners = Number(overviewData.activeLearners || overviewData?.data?.activeLearners || 0)
  overview.completedPasses = Number(overviewData.completedPasses || overviewData?.data?.completedPasses || 0)

  learningPaths.value = list.map((p: any, i: number) => {
    const abbr = langAbbr(p.title || '')
    const color = langColors[i % langColors.length]
    const stats = statMap.get(p.id) || {}
    return {
      ...p,
      display: p.title,
      color,
      advancedIcon: buildIcon(abbr, color),
      icon: buildIcon(abbr, color),
      stats: {
        activeLearners: Number(stats.activeLearners || 0),
        lessons: Number(stats.totalStages || p.totalStages || 0),
        completedPasses: Number(stats.completedPasses || 0),
        completionRate: Number(stats.completionRate || 0),
        difficulty: difficultyToPercent(p.difficulty)
      }
    }
  })
}

const loadRecommendations = async () => {
  if (!isLoggedIn.value) {
    recommendedProblems.value = []
    return
  }

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

const renderEmptyRadar = (chart: any, title: string, message: string) => {
  chart.setOption({
    title: {
      text: title,
      left: 'center',
      top: '42%',
      textStyle: {
        fontSize: 16,
        color: '#666',
        fontWeight: 'normal'
      },
      subtext: message,
      subtextStyle: {
        fontSize: 12,
        color: '#999'
      }
    }
  })
}

const initLineChart = async (echarts: any) => {
  const monthlyLearningData = extractChartData(await dataAnalysisApi.getMonthlyActiveUsers())
  if (!lineChartRef.value) return
  lineChart = echarts.init(lineChartRef.value)
  lineChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>学习人数: {c}人' },
    xAxis: {
      type: 'category',
      data: monthlyLearningData.xAxisData || [],
      name: '月',
      axisLabel: { formatter: '{value}' }
    },
    yAxis: { type: 'value', name: '人' },
    series: [{
      data: monthlyLearningData.yAxisData || [],
      type: 'line',
      smooth: true,
      areaStyle: { color: '#e0f3ff' },
      lineStyle: { color: '#409EFF', width: 3 },
      symbol: 'circle',
      symbolSize: 8
    }],
    grid: { left: 40, right: 20, top: 40, bottom: 30 }
  })
}

const initBarChart = async (echarts: any) => {
  const popularPathsData = extractChartData(await dataAnalysisApi.getPopularLearningPaths())
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>累计通关: {c}次' },
    xAxis: {
      type: 'category',
      data: popularPathsData.xAxisData || [],
      axisLabel: { fontSize: 15 }
    },
    yAxis: { type: 'value', name: '次' },
    series: [{
      data: popularPathsData.yAxisData || [],
      type: 'bar',
      barWidth: 38,
      itemStyle: {
        color(params: any) {
          return (popularPathsData.colors && popularPathsData.colors[params.dataIndex]) || '#409EFF'
        },
        borderRadius: [8, 8, 0, 0]
      }
    }],
    grid: { left: 40, right: 20, top: 40, bottom: 30 }
  })
}

const initAbilityRadarChart = async (echarts: any) => {
  if (!abilityRadarChartRef.value) return
  await nextTick()
  if (abilityRadarChart) {
    abilityRadarChart.dispose()
  }
  abilityRadarChart = echarts.init(abilityRadarChartRef.value)

  try {
    const profile = await userApi.getAbilities()
    const normalizedProfile = normalizeAbilityProfile(profile)

    abilityProfile.dimensions = normalizedProfile.dimensions
    abilityProfile.weaknesses = normalizedProfile.weaknesses
    abilityProfile.overview = normalizedProfile.overview
    abilityProfile.analysisBasis = normalizedProfile.analysisBasis

    if (!normalizedProfile.hasData) {
      renderEmptyRadar(abilityRadarChart, '个人能力画像', '暂无个人做题数据')
      abilityRadarChart.resize()
      return
    }

    abilityRadarChart.setOption(buildAbilityRadarOption(normalizedProfile.indicator, normalizedProfile.values))
    await nextTick()
    abilityRadarChart.resize()
  } catch (error) {
    renderEmptyRadar(abilityRadarChart, '个人能力画像', '登录后可查看个人画像')
    abilityRadarChart.resize()
  }
}

onMounted(async () => {
  try {
    isLoggedIn.value = checkTokenValid()
    await Promise.all([loadLearningPaths(), loadRecommendations()])
    const echarts = await import('echarts')
    await Promise.all([
      initLineChart(echarts),
      initBarChart(echarts),
      initAbilityRadarChart(echarts)
    ])
    window.addEventListener('resize', resizeHandler)
  } catch (e) {
    console.error('加载学习路线页面失败:', e)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  lineChart?.dispose()
  barChart?.dispose()
  abilityRadarChart?.dispose()
})
</script>

<style scoped>
.learning-paths {
  padding: 36px 0 40px 0;
}
.banner-card {
  margin-bottom: 48px;
  border-radius: 20px;
  background: none;
  box-shadow: none;
}
.banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 220px;
}
.banner-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.banner-left h1 {
  font-size: 2.7em;
  font-weight: 800;
  color: #409EFF;
  margin-bottom: 18px;
}
.intro {
  color: #444;
  font-size: 1.18em;
  margin-bottom: 0;
  line-height: 1.7;
}
.banner-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 60px;
}
.banner-img {
  width: 120px;
  height: 120px;
  border-radius: 16px;
  box-shadow: 0 4px 24px #dbeafe;
  background: #fff;
}
.stats-cards {
  display: flex;
  gap: 36px;
  margin: 36px 0 32px 0;
  justify-content: center;
}
.stat-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px #f0f1f2;
  padding: 32px 36px 24px 36px;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: box-shadow 0.2s, transform 0.2s;
}
.stat-card:hover {
  box-shadow: 0 6px 24px #e3e8f7;
  transform: translateY(-4px) scale(1.04);
}
.stat-icon {
  font-size: 32px;
  margin-bottom: 12px;
}
.stat-blue { color: #409EFF; }
.stat-green { color: #67c23a; }
.stat-orange { color: #e6a23c; }
.stat-red { color: #f56c6c; }
.stat-value {
  font-size: 2.3em;
  font-weight: 900;
  color: #222;
  margin-bottom: 6px;
}
.stat-label {
  color: #666;
  font-size: 1.18em;
  margin-top: 2px;
  font-weight: 500;
}
.charts-card {
  margin-bottom: 40px;
  border-radius: 16px;
}
.recommend-band {
  margin: 0 0 40px 0;
  padding: 24px 28px 26px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  border: 1px solid #e6edf7;
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.06);
}
.recommend-band-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.section-kicker {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  color: #409eff;
  letter-spacing: 0;
  margin-bottom: 6px;
}
.recommend-band-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #18324a;
}
.recommend-band-header p {
  margin: 8px 0 0;
  color: #63778c;
  line-height: 1.7;
  max-width: 760px;
}
.recommend-band-body {
  min-height: 120px;
}
.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.recommend-row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 120px;
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #edf3fb;
}
.recommend-row.status-accepted {
  border-color: #d5efc8;
  background: #f3fbef;
}
.recommend-row.status-attempted {
  border-color: #ffd6d6;
  background: #fff1f0;
}
.recommend-row.status-pending {
  border-color: #faecd8;
  background: #fff8ec;
}
.recommend-rank {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
  color: #1d4ed8;
  background: #eef6ff;
}
.recommend-row.status-accepted .recommend-rank {
  color: #67c23a;
  background: #ffffff;
}
.recommend-row.status-attempted .recommend-rank {
  color: #f56c6c;
  background: #ffffff;
}
.recommend-row.status-pending .recommend-rank {
  color: #e6a23c;
  background: #ffffff;
}
.recommend-main {
  min-width: 0;
}
.recommend-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.recommend-title {
  color: #17324a;
  font-size: 18px;
  font-weight: 800;
  text-decoration: none;
  line-height: 1.4;
}
.recommend-title:hover {
  color: #409eff;
}
.recommend-meter {
  margin-top: 10px;
}
.recommend-meter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  color: #5e7185;
  font-size: 13px;
}
.recommend-meter-head strong {
  color: #18324a;
}
.recommend-reason {
  margin: 10px 0 0;
  color: #5e7185;
  line-height: 1.7;
  font-size: 13px;
}
.recommend-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.recommend-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.recommend-score-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 6px solid #e4f0ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  font-size: 22px;
  font-weight: 800;
  color: #409eff;
  background: #f8fbff;
}
.recommend-score-circle small {
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}
.recommend-login-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 22px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px dashed #d9e6f6;
}
.recommend-login-copy strong {
  display: block;
  color: #18324a;
  font-size: 16px;
  margin-bottom: 6px;
}
.recommend-login-copy p {
  margin: 0;
  color: #63778c;
  line-height: 1.7;
}
.ability-profile-section {
  display: grid;
  grid-template-columns: minmax(360px, 1.15fr) minmax(320px, 0.9fr);
  gap: 24px;
  align-items: stretch;
  padding: 20px 12px 4px;
}
.ability-profile-chart-box {
  min-height: 380px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at top left, rgba(64, 158, 255, 0.08), transparent 42%), #fff;
  border: 1px solid #eef3fb;
  border-radius: 18px;
  padding: 14px;
}
.ability-radar-chart {
  width: 100%;
  height: 360px;
  min-height: 360px;
}
.ability-profile-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ability-panel {
  border: 1px solid #edf2fb;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.06);
  padding: 16px 18px;
}
.ability-panel-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 700;
  color: #23425f;
}
.ability-analysis-text {
  color: #5e7185;
  line-height: 1.75;
  font-size: 13px;
}
.ability-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.ability-overview-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: #f7fbff;
  border: 1px solid #e6f0fb;
}
.ability-overview-label {
  display: block;
  color: #7b8ca0;
  font-size: 12px;
  margin-bottom: 6px;
}
.ability-overview-item strong {
  color: #19324a;
  font-size: 20px;
}
.ability-weakness-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ability-weakness-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid #e9f1fb;
}
.ability-weakness-item strong {
  color: #409eff;
}
.ability-empty-text {
  color: #7b8ca0;
  font-size: 13px;
  line-height: 1.7;
}
.charts-section {
  display: flex;
  gap: 40px;
  justify-content: center;
  align-items: flex-start;
  padding: 36px 0 18px 0;
}
.radar-compare-section {
  align-items: stretch;
  flex-wrap: wrap;
}
.main-title {
  font-size: 1.5em;
  font-weight: 700;
  text-align: center;
  margin-top: 16px;
  margin-bottom: 0;
}
.chart-box {
  width: 380px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.radar-compare-box {
  width: 520px;
  max-width: 100%;
}
.radar-compare-chart {
  width: 100%;
  height: 420px;
}
.chart-title {
  font-size: 1.18em;
  font-weight: 700;
  margin-bottom: 12px;
  color: #409EFF;
}
.echarts-box {
  width: 340px;
  height: 220px;
}
.progress-card {
  margin-bottom: 40px;
  border-radius: 16px;
}
.progress-title {
  font-size: 1.15em;
  font-weight: 700;
  margin-bottom: 18px;
  color: #409EFF;
}
.progress-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 600px;
  margin: 0 auto;
}
.progress-item {
  display: flex;
  align-items: center;
  gap: 18px;
}
.progress-label {
  width: 90px;
  font-size: 1.08em;
  color: #333;
  font-weight: 500;
}
.path-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 36px;
  margin: 36px 0 32px 0;
  justify-content: center;
}
.path-card {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 2px 12px #f0f1f2;
  padding: 36px 32px 28px 32px;
  width: 320px;
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  transition: box-shadow 0.2s, transform 0.2s;
}
.path-card:hover {
  box-shadow: 0 6px 24px #e3e8f7;
  transform: translateY(-4px) scale(1.04);
}
.path-card-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 18px;
  gap: 8px;
}
.path-progress {
  margin-right: 10px;
}
.path-icon {
  margin-right: 8px;
  width: 38px;
  height: 38px;
  display: inline-block;
}
.info-icon {
  color: #409EFF;
  vertical-align: middle;
}
.language-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 40px;
  margin-top: 10px;
  justify-content: center;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

/* 卡片翻转容器 */
.card-container {
  perspective: 1000px;
  width: 320px;
  height: 380px;
  margin-bottom: 20px;
}

/* 翻转卡片 */
.flip-card {
  width: 100%;
  height: 100%;
  position: relative;
  transition: transform 0.8s;
  transform-style: preserve-3d;
}

.flip-card.is-flipped {
  transform: rotateY(180deg);
}

/* 卡片正反面公共样式 */
.card-front, .card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 30px;
}

/* 卡片正面 */
.card-front {
  background: white;
}

/* 卡片背面 */
.card-back {
  background: white;
  transform: rotateY(180deg);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding-top: 40px;
}

.card-back h3 {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 24px;
  color: #333;
  align-self: center;
}

/* 自定义翻转按钮 */
.custom-flip-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #e6f1ff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.custom-flip-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  background-color: #d0e5ff;
}

/* 隐藏原来的翻转按钮 */
.flip-btn {
  display: none;
}

/* 语言卡片样式 */
.lang-card {
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.icon-container {
  position: relative;
  width: 70px;
  height: 70px;
  margin-bottom: 20px;
  margin-top: 20px;
}

.lang-icon-bg {
  position: absolute;
  top: -5px;
  left: -5px;
  width: 80px;
  height: 80px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 50%;
  z-index: 1;
}

.lang-icon {
  position: relative;
  z-index: 2;
  transform: scale(1.3);
}

.lang-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 30px;
  color: #333;
}

.start-btn {
  width: 160px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  border-radius: 10px;
  height: 48px;
  margin-top: auto;
}

.detail-btn {
  width: 140px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  height: 42px;
  align-self: center;
  margin-top: 20px;
}

/* 统计信息样式 */
.stat-row {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 16px;
}

.stat-row .stat-label {
  font-size: 16px;
  color: #606266;
  min-width: 120px;
}

.stat-row .stat-number {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.progress-container {
  width: 100%;
  margin-top: 10px;
  margin-bottom: 20px;
}

.progress-container .progress-label {
  font-size: 16px;
  color: #606266;
  margin-bottom: 8px;
}

.difficulty-level {
  text-align: right;
  margin-top: 5px;
  font-size: 16px;
  font-weight: 600;
  color: #409EFF;
}

/* 响应式布局 */
@media (max-width: 1100px) {
  .language-grid {
    gap: 30px;
  }
  .card-container {
    width: 300px;
    height: 360px;
  }
  .ability-profile-section {
    grid-template-columns: 1fr;
  }
  .recommend-row {
    grid-template-columns: 44px minmax(0, 1fr);
  }
  .recommend-side {
    grid-column: 1 / -1;
    flex-direction: row;
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .language-grid {
    gap: 20px;
  }
  .card-container {
    width: 280px;
    height: 340px;
  }
  .start-btn {
    width: 140px;
  height: 44px;
  }
  .ability-profile-section {
    padding: 16px 0 0;
  }
  .ability-profile-chart-box {
    min-height: 320px;
  }
  .ability-radar-chart {
    height: 300px;
    min-height: 300px;
  }
  .ability-overview-grid {
    grid-template-columns: 1fr;
  }
  .recommend-band {
    padding: 20px 16px;
  }
  .recommend-band-header,
  .recommend-login-hint {
    flex-direction: column;
    align-items: flex-start;
  }
  .recommend-row {
    padding: 14px;
  }
  .recommend-rank {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    font-size: 16px;
  }
  .recommend-title {
    font-size: 16px;
  }
}

.knowledge-stats {
  width: 480px;
  padding-top: 10px;
}
.knowledge-title {
  font-size: 1.18em;
  font-weight: 700;
  margin-bottom: 22px;
  color: #409EFF;
  text-align: center;
}
.knowledge-cards {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 10px;
  padding-left: 5px;
}
.knowledge-card {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 16px;
  position: relative;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 1px solid #ebeef5;
}
.knowledge-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}
.knowledge-detail {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  color: #303133;
  font-size: 15px;
}
.knowledge-detail strong {
  font-weight: 600;
  color: #333;
}
.progress-value {
  font-size: 14px;
  font-weight: 600;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  margin: 6px 0;
}
.detail-label {
  font-weight: 500;
  color: #333;
}
.detail-value {
  font-weight: 600;
  color: #1a1a1a;
}
</style> 
