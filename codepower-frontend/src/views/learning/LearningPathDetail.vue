<!-- 学习路线详情 — 阶段列表与进度追踪 -->
<template>
  <div class="path-detail" v-loading="loading">
    <div class="path-header" v-if="path">
      <div class="path-info">
        <el-button text @click="$router.push('/learning-paths')" class="back-btn">
          <el-icon><ArrowLeft /></el-icon> 返回学习路径
        </el-button>
        <div class="title-row">
          <div>
            <h1>{{ path.title }}</h1>
            <p class="path-desc">{{ path.description }}</p>
          </div>
          <el-tag type="primary" effect="dark">拓扑闯关</el-tag>
        </div>
        <div class="path-meta">
          <el-tag>{{ path.difficulty }}</el-tag>
          <el-tag type="info">{{ path.language }}</el-tag>
          <span class="meta-text">预计 {{ path.estimatedHours }} 小时</span>
          <span class="meta-text">{{ totalNodeCount }} 个关卡节点</span>
          <span class="meta-text">已完成 {{ completedCount }} 题</span>
        </div>
        <el-progress
          :percentage="overallProgress"
          :stroke-width="12"
          :format="(p: number) => `${p}%`"
          class="overall-progress"
        />
      </div>
    </div>

    <el-empty v-if="!loading && !path" description="学习路径不存在" />

    <template v-if="path">
      <el-card shadow="hover" class="battlefield-card">
        <template #header>
          <div class="section-header">
            <div>
              <h2>闯关地图</h2>
              <p>主线优先推进，支线用于补强专题能力；每个节点都对应题库中的一道题。</p>
            </div>
            <el-tag type="success">当前解锁 {{ unlockedCount }} / {{ totalNodeCount }}</el-tag>
          </div>
        </template>

        <div class="battlefield-scroll">
          <div class="battlefield-map" :style="battlefieldStyle">
            <div
              v-for="link in nodeLinks"
              :key="`${link.fromId}-${link.toId}`"
              class="node-link"
              :class="{ active: link.active, completed: link.completed }"
              :style="link.style"
            />

            <button
              v-for="node in mapNodes"
              :key="node.id"
              type="button"
              class="path-node"
              :class="[
                `path-node--${node.branchType}`,
                {
                  active: selectedNode?.id === node.id,
                  completed: node.isCompleted,
                  current: node.isCurrent,
                  locked: !node.isUnlocked
                }
              ]"
              :style="node.style"
              @click="selectNode(node.id)"
            >
              <span class="node-index">{{ node.displayIndex }}</span>
              <span class="node-status">
                <el-icon v-if="node.isCompleted"><CircleCheckFilled /></el-icon>
                <el-icon v-else-if="node.isUnlocked"><Position /></el-icon>
                <el-icon v-else><Lock /></el-icon>
              </span>
              <span class="node-title">{{ node.problemTitle || node.title }}</span>
              <span class="node-branch">{{ node.branchLabel }}</span>
            </button>
          </div>
        </div>
      </el-card>

      <div class="content-grid">
        <el-card shadow="hover" class="node-panel" v-if="selectedNode">
          <template #header>
            <div class="content-header">
              <div>
                <div class="panel-kicker">{{ selectedNode.branchLabel }}</div>
                <h2>{{ selectedNode.problemTitle || selectedNode.title }}</h2>
              </div>
              <div class="panel-status">
                <el-tag v-if="selectedNode.isCompleted" type="success">已完成</el-tag>
                <el-tag v-else-if="selectedNode.isUnlocked" type="primary">可挑战</el-tag>
                <el-tag v-else type="info">未解锁</el-tag>
              </div>
            </div>
          </template>

          <div class="node-meta">
            <el-tag size="small" :type="getDifficultyTagType(selectedNode.problemDifficulty)">
              {{ getDifficultyLabel(selectedNode.problemDifficulty) }}
            </el-tag>
            <span v-if="selectedNode.estimatedMinutes">预计 {{ selectedNode.estimatedMinutes }} 分钟</span>
            <span>节点序号 {{ selectedNode.displayIndex }}</span>
          </div>

          <div class="content-body" v-html="renderedContent"></div>

          <div class="practice-section">
            <div class="practice-summary">
              <div>
                <h3>关联题目</h3>
                <p v-if="selectedNode.problemId">该节点会跳转到题库原题，完成后即可累计此路线进度。</p>
                <p v-else>当前节点暂无题目绑定，可先阅读本阶段内容。</p>
              </div>
              <el-button
                v-if="selectedNode.problemId"
                :type="selectedNode.isCompleted ? 'success' : 'primary'"
                :disabled="!selectedNode.isUnlocked"
                @click="$router.push(`/problems/${selectedNode.problemId}`)"
              >
                {{ selectedNode.isCompleted ? '再次挑战' : '前往做题' }}
              </el-button>
              <el-button
                v-else
                type="success"
                :disabled="!selectedNode.isUnlocked || selectedNode.isCompleted"
                :loading="completing"
                @click="completeStage"
              >
                标记为已完成
              </el-button>
            </div>

            <div class="resource-section" v-if="selectedNode.resourceUrl">
              <el-divider />
              <el-link :href="selectedNode.resourceUrl" target="_blank" type="primary">查看扩展资源</el-link>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="overview-panel">
          <template #header>
            <div class="section-header compact">
              <h2>路线总览</h2>
              <el-tag type="warning">推荐先走主线</el-tag>
            </div>
          </template>

          <div class="overview-stats">
            <div class="overview-item">
              <span class="label">主线节点</span>
              <strong>{{ mainNodeCount }}</strong>
            </div>
            <div class="overview-item">
              <span class="label">支线节点</span>
              <strong>{{ sideNodeCount }}</strong>
            </div>
            <div class="overview-item">
              <span class="label">已解锁</span>
              <strong>{{ unlockedCount }}</strong>
            </div>
            <div class="overview-item">
              <span class="label">完成率</span>
              <strong>{{ overallProgress }}%</strong>
            </div>
          </div>

          <el-divider />

          <div class="node-list">
            <button
              v-for="node in mapNodes"
              :key="`list-${node.id}`"
              type="button"
              class="node-list-item"
              :class="{
                active: selectedNode?.id === node.id,
                completed: node.isCompleted,
                locked: !node.isUnlocked
              }"
              @click="selectNode(node.id)"
            >
              <div>
                <div class="node-list-title">{{ node.displayIndex }}. {{ node.problemTitle || node.title }}</div>
                <div class="node-list-meta">{{ node.branchLabel }} · {{ getDifficultyLabel(node.problemDifficulty) }}</div>
              </div>
              <el-tag v-if="node.isCompleted" size="small" type="success">已完成</el-tag>
              <el-tag v-else-if="node.isUnlocked" size="small" type="primary">可挑战</el-tag>
              <el-tag v-else size="small" type="info">未解锁</el-tag>
            </button>
          </div>
        </el-card>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CircleCheckFilled, Lock, Position } from '@element-plus/icons-vue'
import { learningPathApi } from '@/api/learningPath'

const route = useRoute()
const pathId = Number(route.params.id)

const loading = ref(true)
const completing = ref(false)
const path = ref<any>(null)
const stages = ref<any[]>([])
const selectedStageId = ref<number | null>(null)

const getDifficultyTagType = (difficulty?: string | null) => {
  switch (difficulty) {
    case 'HARD':
    case '困难':
      return 'info'
    case 'MEDIUM':
    case '普通':
    case '中等':
      return 'warning'
    case 'EXTREME':
    case '极限':
      return 'danger'
    case 'EASY':
    case '简单':
      return 'success'
    default:
      return 'info'
  }
}

const getDifficultyLabel = (difficulty?: string | null) => {
  const labels: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '普通',
    HARD: '困难',
    EXTREME: '极限',
    简单: '简单',
    普通: '普通',
    中等: '普通',
    困难: '困难',
    极限: '极限'
  }
  return labels[difficulty || ''] || '未定级'
}

const normalizeStage = (stage: any, index: number) => {
  const branchType = stage.branchType || (index % 4 === 2 ? 'side-up' : index % 4 === 3 ? 'side-down' : 'main')
  const isCompleted = (stage.progressStatus || stage.userStatus || 'PENDING') === 'COMPLETED'

  return {
    ...stage,
    displayIndex: index + 1,
    branchType,
    branchLabel: branchType === 'main' ? '主线' : '支线',
    userStatus: isCompleted ? 'COMPLETED' : 'PENDING',
    isCompleted,
    isUnlocked: false,
    isCurrent: false,
    problemTitle: stage.problemTitle || stage.title || '练习题',
    problemDifficulty: stage.problemDifficulty || stage.difficulty || null,
    predecessorIds: Array.isArray(stage.predecessorIds) ? stage.predecessorIds : []
  }
}

const currentStageIndex = computed(() => {
  const idx = stages.value.findIndex(stage => !stage.isCompleted)
  return idx === -1 ? stages.value.length - 1 : idx
})

const enrichNodes = (rawNodes: any[]) => {
  const currentIndex = rawNodes.findIndex(node => !node.isCompleted)

  return rawNodes.map((node, index) => {
    const unlockedByOrder = currentIndex === -1 || index <= currentIndex
    const unlockedByDependency = node.predecessorIds.length === 0 || node.predecessorIds.every((id: number) => {
      const prev = rawNodes.find(item => item.id === id)
      return prev?.isCompleted
    })
    const isUnlocked = node.isCompleted || (unlockedByOrder && unlockedByDependency)

    return {
      ...node,
      isUnlocked,
      isCurrent: !node.isCompleted && index === currentIndex
    }
  })
}

const mapNodes = computed(() => {
  const baseX = 110
  const stepX = 168
  const mainY = 188
  const offsetY = 104

  return enrichNodes(stages.value).map((node, index) => {
    const laneY = node.branchType === 'side-up' ? mainY - offsetY : node.branchType === 'side-down' ? mainY + offsetY : mainY
    return {
      ...node,
      style: {
        left: `${baseX + index * stepX}px`,
        top: `${laneY}px`
      }
    }
  })
})

const nodeLinks = computed(() => {
  const nodes = mapNodes.value
  return nodes.slice(1).map((node, index) => {
    const prev = nodes[index]
    const x1 = 110 + index * 168 + 120
    const y1 = Number.parseInt(prev.style.top, 10) + 48
    const x2 = 110 + (index + 1) * 168
    const y2 = Number.parseInt(node.style.top, 10) + 48
    const width = Math.hypot(x2 - x1, y2 - y1)
    const angle = Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI

    return {
      fromId: prev.id,
      toId: node.id,
      active: prev.isUnlocked && node.isUnlocked,
      completed: prev.isCompleted && node.isCompleted,
      style: {
        left: `${x1}px`,
        top: `${y1}px`,
        width: `${width}px`,
        transform: `rotate(${angle}deg)`
      }
    }
  })
})

const selectedNode = computed(() => {
  const nodes = mapNodes.value
  return nodes.find(node => node.id === selectedStageId.value) || nodes[currentStageIndex.value] || null
})

const renderedContent = computed(() => {
  if (!selectedNode.value?.content) return '<p>暂无内容</p>'
  return selectedNode.value.content
    .replace(/\n/g, '<br>')
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
})

const totalNodeCount = computed(() => mapNodes.value.length)
const completedCount = computed(() => mapNodes.value.filter(node => node.isCompleted).length)
const unlockedCount = computed(() => mapNodes.value.filter(node => node.isUnlocked).length)
const mainNodeCount = computed(() => mapNodes.value.filter(node => node.branchType === 'main').length)
const sideNodeCount = computed(() => mapNodes.value.filter(node => node.branchType !== 'main').length)

const overallProgress = computed(() => {
  if (totalNodeCount.value === 0) return 0
  return Math.round((completedCount.value / totalNodeCount.value) * 100)
})

const battlefieldStyle = computed(() => ({
  width: `${Math.max(680, mapNodes.value.length * 168 + 180)}px`
}))

const selectNode = (id: number) => {
  selectedStageId.value = id
}

const loadPathDetail = async () => {
  loading.value = true
  try {
    const res: any = await learningPathApi.getPathDetail(pathId)
    const data = res.data || res
    path.value = data
    const normalized = Array.isArray(data.stages)
      ? data.stages.map((stage: any, index: number) => normalizeStage(stage, index))
      : []
    stages.value = normalized
    selectedStageId.value = normalized[currentStageIndex.value]?.id ?? normalized[0]?.id ?? null
  } catch (e) {
    console.error('加载学习路径失败', e)
    path.value = null
    stages.value = []
    selectedStageId.value = null
  } finally {
    loading.value = false
  }
}

const completeStage = async () => {
  if (!selectedNode.value || selectedNode.value.problemId) return
  completing.value = true
  try {
    await learningPathApi.completeStage(pathId, selectedNode.value.id)
    const target = stages.value.find(stage => stage.id === selectedNode.value?.id)
    if (target) {
      target.userStatus = 'COMPLETED'
      target.isCompleted = true
    }
    ElMessage.success('节点已完成，继续推进下一关')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    completing.value = false
  }
}

onMounted(() => {
  loadPathDetail()
})
</script>

<style scoped>
.path-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px 48px;
}

.back-btn {
  margin-bottom: 12px;
  font-size: 14px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.path-header h1 {
  font-size: 2rem;
  color: #333;
  margin-bottom: 12px;
}

.path-desc {
  color: #666;
  font-size: 1rem;
  margin-bottom: 16px;
  line-height: 1.6;
}

.path-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.meta-text {
  color: #909399;
  font-size: 14px;
}

.overall-progress {
  max-width: 460px;
}

.battlefield-card,
.node-panel,
.overview-panel {
  border-radius: 18px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-header h2,
.content-header h2 {
  margin: 0;
  color: #303133;
}

.section-header p {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.section-header.compact {
  align-items: center;
}

.battlefield-scroll {
  overflow-x: auto;
  padding-bottom: 6px;
}

.battlefield-map {
  position: relative;
  min-height: 380px;
  padding: 12px 0 24px;
  background:
    radial-gradient(circle at top, rgba(64, 158, 255, 0.12), transparent 38%),
    linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  border-radius: 18px;
}

.node-link {
  position: absolute;
  height: 4px;
  border-radius: 999px;
  background: rgba(191, 203, 217, 0.8);
  transform-origin: left center;
}

.node-link.active {
  background: rgba(64, 158, 255, 0.5);
}

.node-link.completed {
  background: linear-gradient(90deg, #67c23a 0%, #95d475 100%);
}

.path-node {
  position: absolute;
  width: 120px;
  min-height: 96px;
  padding: 12px 10px 10px;
  border-radius: 20px;
  border: 2px solid #d9ecff;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  text-align: left;
  box-shadow: 0 12px 24px rgba(64, 158, 255, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.path-node:not(.locked) {
  cursor: pointer;
}

.path-node:hover:not(.locked),
.path-node.active {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 16px 32px rgba(64, 158, 255, 0.18);
}

.path-node--main {
  border-color: #c6e2ff;
}

.path-node--side-up,
.path-node--side-down {
  border-style: dashed;
}

.path-node.completed {
  border-color: #95d475;
  background: linear-gradient(180deg, #f7fff0 0%, #eef9e8 100%);
}

.path-node.current {
  border-color: #409eff;
}

.path-node.locked {
  opacity: 0.7;
  box-shadow: none;
}

.node-index,
.node-branch {
  font-size: 12px;
  color: #909399;
}

.node-status {
  font-size: 18px;
  color: #409eff;
}

.path-node.completed .node-status {
  color: #67c23a;
}

.path-node.locked .node-status {
  color: #c0c4cc;
}

.node-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.35;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 20px;
  margin-top: 20px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.panel-kicker {
  margin-bottom: 6px;
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
}

.node-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  color: #909399;
  font-size: 13px;
  margin-bottom: 18px;
}

.content-body {
  line-height: 1.85;
  color: #333;
  font-size: 15px;
}

.content-body :deep(pre) {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 10px;
  overflow-x: auto;
}

.content-body :deep(code) {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.practice-section {
  margin-top: 24px;
}

.practice-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.practice-summary h3 {
  margin: 0 0 8px;
  color: #303133;
}

.practice-summary p {
  margin: 0;
  color: #909399;
  line-height: 1.6;
}

.resource-section {
  margin-top: 12px;
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.overview-item {
  background: #f8fbff;
  border-radius: 14px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview-item .label {
  color: #909399;
  font-size: 13px;
}

.overview-item strong {
  color: #303133;
  font-size: 22px;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.node-list-item {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #fff;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
  cursor: pointer;
}

.node-list-item.active {
  border-color: #409eff;
  background: #f5f9ff;
}

.node-list-item.completed {
  border-color: #95d475;
}

.node-list-item.locked {
  opacity: 0.72;
}

.node-list-title {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.node-list-meta {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

@media (max-width: 992px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .title-row,
  .section-header,
  .content-header,
  .practice-summary {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .path-detail {
    padding: 20px 12px 36px;
  }

  .battlefield-map {
    min-height: 420px;
  }

  .overview-stats {
    grid-template-columns: 1fr;
  }
}
</style>
