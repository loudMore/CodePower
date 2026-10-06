<!-- 个人主页子组件 — 收藏的题目列表 -->
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

// 父页面传入收藏题目；miniMode 用于个人主页右侧简略展示。
defineProps<{
  problems: any[]
  miniMode?: boolean
}>()

const difficultyMap: Record<string, { label: string; type: string }> = {
  EASY: { label: '简单', type: 'success' },
  MEDIUM: { label: '中等', type: 'warning' },
  HARD: { label: '困难', type: 'danger' },
}

// 把后端难度码转成中文和标签颜色。
const getDifficulty = (d: string) => difficultyMap[d] || { label: d, type: 'info' }

// 点击收藏题目进入做题 IDE。
const goToProblem = (id: number) => {
  router.push(`/problems/${id}`)
}
</script>

<template>
  <div class="favorite-problems">
    <div v-if="!problems || problems.length === 0" class="empty-state">
      <el-empty description="暂无收藏题目" :image-size="miniMode ? 50 : 80" />
    </div>
    <el-table v-else :data="problems" style="width: 100%" stripe :show-header="!miniMode">
      <el-table-column label="题目" min-width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="goToProblem(row.id)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column v-if="!miniMode" label="难度" width="100">
        <template #default="{ row }">
          <el-tag :type="getDifficulty(row.difficulty).type" size="small">{{ getDifficulty(row.difficulty).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!miniMode" label="通过率" width="100">
        <template #default="{ row }">
          {{ row.acceptRate != null ? (row.acceptRate * 100).toFixed(0) + '%' : '-' }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.empty-state { text-align: center; padding: 20px 0; }
</style>
