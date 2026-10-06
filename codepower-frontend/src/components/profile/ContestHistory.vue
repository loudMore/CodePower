<!-- 个人主页子组件 — 竞赛参赛记录 -->
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { apiDateTimeMs, formatApiDateTime } from '@/utils/datetime'

const router = useRouter()

// 父页面传入用户参与或创建过的竞赛，本组件负责排序和卡片展示。
const props = defineProps<{
  contests: Array<{
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
  }>
}>()

// 按开始/结束时间倒序展示，最近的竞赛排在前面。
const sortedContests = computed(() => {
  return [...(props.contests || [])].sort((a, b) => {
    const left = apiDateTimeMs(b.startTime || b.endTime)
    const right = apiDateTimeMs(a.startTime || a.endTime)
    return left - right
  })
})

const statusMap: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  UPCOMING: { label: '未开始', type: 'warning' },
  RUNNING: { label: '进行中', type: 'success' },
  ENDED: { label: '已结束', type: 'info' },
  DRAFT: { label: '草稿', type: 'danger' }
}

const typeMap: Record<string, string> = {
  EXAM: '考试',
  OFFICIAL: '正式赛',
  PRACTICE: '练习赛'
}

// 把竞赛状态码转成中文和标签颜色。
const getStatus = (status?: string) => {
  if (!status) return { label: '未知状态', type: 'info' as const }
  return statusMap[status] || { label: status, type: 'info' as const }
}

// 把竞赛类型码转成中文。
const getTypeLabel = (type?: string) => {
  if (!type) return '考试/竞赛'
  return typeMap[type] || type
}

// 格式化竞赛开始/结束时间。
const formatTime = (time?: string) => {
  return formatApiDateTime(time)
}

// 把分钟数转成“x 小时 y 分钟”。
const formatDuration = (minutes?: number) => {
  if (!minutes) return '-'
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const remainMinutes = minutes % 60
  return remainMinutes > 0 ? `${hours} 小时 ${remainMinutes} 分钟` : `${hours} 小时`
}

// 判断当前用户在竞赛中的身份：创建者、参赛者或仅可查看。
const getIdentityLabel = (contest: { creatorId?: number; registered?: boolean }) => {
  if (contest.creatorId) return contest.registered ? '我发起并参与' : '我发起'
  return contest.registered ? '我已参加' : '可查看'
}

// 点击竞赛进入详情页。
const openContest = (id: number) => {
  router.push(`/contests/${id}`)
}
</script>

<template>
  <div class="contest-history">
    <h3>考试与竞赛记录</h3>
    <div v-if="sortedContests.length === 0" class="empty-state">
      <el-empty description="暂无考试或竞赛记录" :image-size="80" />
    </div>
    <div v-else class="contest-list">
      <el-card
        v-for="contest in sortedContests"
        :key="contest.id"
        class="contest-item"
        shadow="hover"
      >
        <div class="contest-main">
          <div class="contest-header">
            <div class="contest-title-row">
              <el-link type="primary" @click="openContest(contest.id)">
                {{ contest.title || `考试 #${contest.id}` }}
              </el-link>
              <el-tag size="small" effect="plain">{{ getTypeLabel(contest.type) }}</el-tag>
              <el-tag :type="getStatus(contest.status).type" size="small">
                {{ getStatus(contest.status).label }}
              </el-tag>
            </div>
            <el-tag size="small" type="info" effect="plain">
              {{ getIdentityLabel(contest) }}
            </el-tag>
          </div>

          <div v-if="contest.description" class="contest-description">
            {{ contest.description }}
          </div>

          <div class="contest-meta">
            <div class="meta-item">
              <span class="meta-label">开始时间</span>
              <span>{{ formatTime(contest.startTime) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">结束时间</span>
              <span>{{ formatTime(contest.endTime) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">时长</span>
              <span>{{ formatDuration(contest.durationMinutes) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">题目数</span>
              <span>{{ contest.problemCount ?? 0 }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">参与人数</span>
              <span>{{ contest.participantCount ?? 0 }}</span>
            </div>
            <div class="meta-item" v-if="contest.creatorName">
              <span class="meta-label">发起人</span>
              <span>{{ contest.creatorName }}</span>
            </div>
          </div>

          <div class="contest-actions">
            <el-button type="primary" plain @click="openContest(contest.id)">查看详情</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.contest-history {
  margin-top: 10px;
}

.contest-history h3 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 16px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.contest-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contest-item {
  border-radius: 12px;
}

.contest-main {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.contest-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.contest-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.contest-description {
  color: #606266;
  line-height: 1.7;
}

.contest-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 10px;
  color: #303133;
}

.meta-label {
  font-size: 12px;
  color: #909399;
}

.contest-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .contest-header {
    flex-direction: column;
  }

  .contest-actions {
    justify-content: flex-start;
  }
}
</style>
