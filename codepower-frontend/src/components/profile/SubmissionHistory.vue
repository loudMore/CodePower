<!-- 个人主页子组件 — 提交记录列表 -->
<script setup lang="ts">
import { useRouter } from 'vue-router'
import { formatApiDateTime } from '@/utils/datetime'

const router = useRouter()

// 父页面传入最近提交记录，本组件只负责表格展示和状态格式化。
defineProps<{
  submissions: Array<{
    id: number
    problemId: number
    problemTitle?: string
    status: string
    language?: string
    runtime?: number | null
    memory?: number | null
    submittedAt?: string
    contestId?: number | null
  }>
}>()

const statusMap: Record<string, { label: string; type: string }> = {
  ACCEPTED: { label: '通过', type: 'success' },
  WRONG_ANSWER: { label: '答案错误', type: 'danger' },
  TIME_LIMIT_EXCEEDED: { label: '超时', type: 'warning' },
  COMPILATION_ERROR: { label: '编译错误', type: 'info' },
  RUNTIME_ERROR: { label: '运行错误', type: 'danger' },
  SYSTEM_ERROR: { label: '系统错误', type: 'danger' }
}

// 把后端提交状态码转成中文标签和颜色。
const getStatus = (status: string) => statusMap[status] || { label: status || '未知', type: 'info' }

// 统一格式化后端提交时间。
const formatTime = (time?: string) => {
  return formatApiDateTime(time)
}

// 后端内存以 KB 为主，超过 1024KB 时显示为 MB。
const formatMemory = (memory?: number | null) => {
  if (memory == null) return '-'
  if (memory >= 1024) return `${(memory / 1024).toFixed(1)} MB`
  return `${memory} KB`
}

// 点击题目标题进入做题 IDE。
const goToProblem = (id: number) => {
  router.push(`/problems/${id}`)
}
</script>

<template>
  <div class="submission-history">
    <h3>日常训练提交历史</h3>
    <div v-if="!submissions || submissions.length === 0" class="empty-state">
      <el-empty description="暂无日常训练提交记录" :image-size="80" />
    </div>
    <el-table v-else :data="submissions" style="width: 100%" stripe>
      <el-table-column label="题目" min-width="220">
        <template #default="{ row }">
          <el-link type="primary" @click="goToProblem(row.problemId)">{{ row.problemTitle || `题目 #${row.problemId}` }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="language" label="语言" width="120" />
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="getStatus(row.status).type" size="small">{{ getStatus(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="120">
        <template #default="{ row }">
          {{ row.runtime != null ? `${row.runtime}ms` : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="内存" width="120">
        <template #default="{ row }">
          {{ formatMemory(row.memory) }}
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="190">
        <template #default="{ row }">
          {{ formatTime(row.submittedAt) }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.submission-history { margin-top: 10px; }
.submission-history h3 { font-size: 16px; color: #303133; margin-bottom: 16px; }
.empty-state { text-align: center; padding: 40px 0; }
</style>
