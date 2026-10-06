/** 文件说明：前端通用展示工具，负责难度标签、状态颜色和能力雷达图配置。 */
/**
 * 获取难度对应的标签类型
 * @param difficulty 难度级别
 */
export const getDifficultyType = (difficulty: string): string => {
  switch (difficulty) {
    case '简单':
      return 'success'
    case '普通':
    case '中等':
      return 'warning'
    case '困难':
      return 'info'
    case '极限':
      return 'danger'
    default:
      return 'info'
  }
}

const abilityScoreFieldMap: Record<string, string> = {
  知识覆盖: 'knowledgeCoverage',
  掌握深度: 'proficiency',
  难度进阶: 'difficultyMastery',
  解题稳定: 'stability',
  运行效率: 'runtimeEfficiency',
  空间效率: 'memoryEfficiency',
  成长潜力: 'growthPotential',
  学习习惯: 'habit'
}

const defaultAbilityAnalysisBasis = '系统会结合最近一段时间的通过记录、标签覆盖、难度分布与提交稳定性，定期生成能力画像。'

// 从后端能力画像 scores 中取出某个维度分数，并四舍五入成雷达图需要的整数。
const getAbilityScore = (scores: Record<string, any>, dimension: string) => {
  const field = abilityScoreFieldMap[dimension]
  return Math.round(Number((field ? scores?.[field] : 0) || 0))
}

/**
 * 标准化能力画像数据。
 * 后端负责计算标签能力、八维雷达分和证据说明；前端把这些数据整理成 ECharts 雷达图和卡片需要的结构。
 */
export const normalizeAbilityProfile = (profile: any) => {
  const dimensions = Array.isArray(profile?.dimensions) ? profile.dimensions : []
  const scores = profile?.scores || {}
  const indicator = dimensions.map((name: string) => ({ name, max: 100 }))
  const values = dimensions.map((name: string) => getAbilityScore(scores, name))
  const evidenceMap = new Map<string, string>()

  ;[...(profile?.strengths || []), ...(profile?.weaknesses || [])].forEach((item: any) => {
    if (item?.name) {
      evidenceMap.set(item.name, item.evidence || '')
    }
  })

  return {
    hasData: indicator.length > 0 && values.length > 0,
    dimensions: dimensions.map((name: string) => ({
      key: abilityScoreFieldMap[name] || name,
      name,
      score: getAbilityScore(scores, name),
      evidence: evidenceMap.get(name) || ''
    })),
    indicator,
    values,
    strengths: Array.isArray(profile?.strengths)
      ? profile.strengths.map((item: any) => ({
          name: item?.name || '未命名维度',
          score: Math.round(Number(item?.score || 0)),
          highlight: item?.highlight || '',
          evidence: item?.evidence || ''
        }))
      : [],
    weaknesses: Array.isArray(profile?.weaknesses)
      ? profile.weaknesses.map((item: any) => ({
          name: item?.name || '未命名维度',
          score: Math.round(Number(item?.score || 0)),
          highlight: item?.highlight || '',
          evidence: item?.evidence || ''
        }))
      : [],
    overview: {
      solvedCount: Number(profile?.overview?.solvedCount || 0),
      totalAttempts: Number(profile?.overview?.totalAttempts || 0),
      attemptedProblemCount: Number(profile?.overview?.attemptedProblemCount || 0),
      acceptanceRate: Math.round(Number(profile?.overview?.acceptanceRate || 0)),
      coveredTagCount: Number(profile?.overview?.coveredTagCount || 0),
      averageSolvedDifficulty: profile?.overview?.averageSolvedDifficulty || 0,
      highestSolvedDifficulty: profile?.overview?.highestSolvedDifficulty || '暂无'
    },
    analysisBasis: profile?.analysisBasis || defaultAbilityAnalysisBasis,
    tagSnapshot: Array.isArray(profile?.tagSnapshot) ? profile.tagSnapshot : []
  }
}

/** 构建能力雷达图配置，indicator 是八个维度，values 是对应 0-100 分。 */
export const buildAbilityRadarOption = (indicator: Array<{ name: string; max: number }>, values: number[]) => ({
  tooltip: {
    trigger: 'item',
    formatter(params: any) {
      return `<strong>${params.name}</strong><br/>${params.marker} ${params.value}`
    }
  },
  radar: {
    indicator,
    center: ['50%', '55%'],
    radius: '65%',
    shape: 'circle',
    splitNumber: 4,
    name: {
      textStyle: {
        color: '#333',
        fontSize: 14,
        fontWeight: 'bold'
      }
    },
    splitArea: {
      areaStyle: {
        color: ['#f7f8fc', '#e9edf7', '#d9e1f6', '#c9d5f5']
      }
    }
  },
  series: [{
    type: 'radar',
    data: [{
      value: values,
      name: '我的能力',
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#409EFF' },
      areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
      label: { show: true, formatter: '{c}', fontSize: 12, fontWeight: 'bold' }
    }]
  }]
})
