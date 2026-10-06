<!-- 个人主页子组件 — 等级、经验、每日任务面板 -->
<template>
  <el-card class="level-panel" shadow="hover">
    <template #header>
      <div class="panel-header">
        <span>等级与任务</span>
        <el-tag :type="levelTagType" size="small" effect="dark">Lv.{{ levelInfo.level }} {{ levelInfo.title }}</el-tag>
      </div>
    </template>

    <!-- 等级进度 -->
    <div class="level-section">
      <div class="level-label">
        <span>经验值</span>
        <span class="level-exp">{{ levelInfo.exp }} / {{ levelInfo.nextLevelExp || '满级' }}</span>
      </div>
      <el-progress
        :percentage="expPercentage"
        :stroke-width="12"
        :color="levelColor"
        :format="() => levelInfo.maxLevel ? '满级' : `${expPercentage}%`"
      />
    </div>

    <!-- AI积分 -->
    <div class="ai-quota-section">
      <div class="level-label">
        <span>每日AI积分</span>
        <span class="ai-count">{{ levelInfo.dailyAiRemaining }} / {{ levelInfo.dailyAiQuota }}</span>
      </div>
      <el-progress
        :percentage="aiPercentage"
        :stroke-width="12"
        :color="aiPercentage > 30 ? '#67c23a' : aiPercentage > 0 ? '#e6a23c' : '#f56c6c'"
        :format="() => `剩余${levelInfo.dailyAiRemaining}积分`"
      />
      <div v-if="levelInfo.bonusAiPoints > 0" class="bonus-info">
        累积积分: <strong>{{ levelInfo.bonusAiPoints }}</strong>（不随每日重置）
      </div>
    </div>

    <!-- 签到 -->
    <div class="checkin-section">
      <el-button
        :type="checkInStatus.checkedInToday ? 'info' : 'success'"
        :disabled="checkInStatus.checkedInToday || checkingIn"
        :loading="checkingIn"
        round
        class="checkin-btn"
        @click="doCheckIn"
      >
        {{ checkInStatus.checkedInToday ? '已签到' : '签到 +10经验 +3AI' }}
      </el-button>
      <div class="streak-info" v-if="checkInStatus.streak > 0">
        连续签到 <strong>{{ checkInStatus.streak }}</strong> 天
        <span v-if="checkInStatus.nextStreakBonus" class="streak-tip">
          (再签{{ checkInStatus.nextStreakBonus - checkInStatus.streak }}天有额外奖励)
        </span>
      </div>
    </div>

    <!-- 每日任务 -->
    <div class="daily-tasks-section">
      <div class="tasks-title">每日任务</div>
      <div v-for="task in dailyTasks" :key="task.taskId" class="task-item">
        <div class="task-info">
          <span class="task-name">{{ task.title }}</span>
          <span class="task-reward">+{{ task.expReward }}exp{{ task.aiReward > 0 ? ` +${task.aiReward}AI` : '' }}</span>
        </div>
        <div class="task-progress-row">
          <el-progress
            :percentage="Math.min(100, (task.currentCount / task.requiredCount) * 100)"
            :stroke-width="8"
            :color="task.completed ? '#67c23a' : '#409EFF'"
            :format="() => `${task.currentCount}/${task.requiredCount}`"
            class="task-progress"
          />
          <el-button
            v-if="task.completed && !task.rewarded"
            type="warning"
            size="small"
            round
            :loading="claimingTask === task.taskId"
            @click="claimReward(task.taskId)"
          >领取</el-button>
          <el-tag v-else-if="task.rewarded" type="success" size="small" effect="plain">已领取</el-tag>
        </div>
      </div>
      <el-empty v-if="dailyTasks.length === 0" description="暂无每日任务" :image-size="60" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { levelApi } from '@/api/level'

// 等级和 AI 额度面板：展示经验进度、每日 AI 额度、签到和每日任务。
const levelInfo = ref<any>({
  level: 1, title: '初学者', exp: 0,
  currentLevelExp: 0, nextLevelExp: 100, maxLevel: false,
  dailyAiQuota: 5, dailyAiUsed: 0, dailyAiRemaining: 5, baseAiQuota: 5
})

const checkInStatus = ref<any>({
  checkedInToday: false, streak: 0, nextStreakBonus: 3
})

const dailyTasks = ref<any[]>([])
const checkingIn = ref(false)
const claimingTask = ref<number | null>(null)

// 计算当前等级经验进度条百分比。
const expPercentage = computed(() => {
  if (levelInfo.value.maxLevel) return 100
  const current = levelInfo.value.exp - levelInfo.value.currentLevelExp
  const needed = (levelInfo.value.nextLevelExp || levelInfo.value.exp) - levelInfo.value.currentLevelExp
  return needed > 0 ? Math.min(100, Math.round((current / needed) * 100)) : 100
})

// 计算今日剩余 AI 额度百分比。
const aiPercentage = computed(() => {
  const quota = levelInfo.value.dailyAiQuota || 1
  return Math.round((levelInfo.value.dailyAiRemaining / quota) * 100)
})

// 根据等级选择展示颜色。
const levelColor = computed(() => {
  const lv = levelInfo.value.level
  if (lv >= 28) return '#ff4500'
  if (lv >= 25) return '#f56c6c'
  if (lv >= 20) return '#e6a23c'
  if (lv >= 15) return '#409EFF'
  if (lv >= 8) return '#67c23a'
  return '#909399'
})

// 根据等级选择 Element Plus 标签类型。
const levelTagType = computed(() => {
  const lv = levelInfo.value.level
  if (lv >= 25) return 'danger'
  if (lv >= 20) return 'warning'
  if (lv >= 15) return ''
  if (lv >= 8) return 'success'
  return 'info'
})

// 用户签到：后端发放经验和 AI 奖励，成功后刷新面板。
const doCheckIn = async () => {
  checkingIn.value = true
  try {
    const res: any = await levelApi.checkIn()
    const data = res.data || res
    ElMessage.success(`签到成功！+${data.expEarned}经验 +${data.aiBonus}AI积分${data.hasStreakBonus ? ' ' + data.streakBonusMsg : ''}`)
    checkInStatus.value.checkedInToday = true
    checkInStatus.value.streak = data.streak
    await loadData()
  } catch (e: any) {
    const msg = e.response?.data?.message || e.message || '签到失败'
    ElMessage.warning(msg)
  } finally {
    checkingIn.value = false
  }
}

// 领取每日任务奖励，完成后重新加载任务状态和额度。
const claimReward = async (taskId: number) => {
  claimingTask.value = taskId
  try {
    const res: any = await levelApi.claimTaskReward(taskId)
    const data = res.data || res
    ElMessage.success(data.message || '奖励已领取')
    await loadData()
  } catch (e: any) {
    ElMessage.warning(e.response?.data?.message || '领取失败')
  } finally {
    claimingTask.value = null
  }
}

// 并行加载等级信息、签到状态和每日任务。
const loadData = async () => {
  try {
    const [lvRes, statusRes, tasksRes]: any[] = await Promise.all([
      levelApi.getLevelInfo(),
      levelApi.getCheckInStatus(),
      levelApi.getDailyTasks()
    ])
    levelInfo.value = lvRes.data || lvRes
    checkInStatus.value = statusRes.data || statusRes
    dailyTasks.value = (tasksRes.data || tasksRes) || []
  } catch (e) {
    console.error('加载等级数据失败:', e)
  }
}

defineExpose({ loadData })

onMounted(() => loadData())
</script>

<style scoped>
.level-panel {
  margin-bottom: 20px;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
.level-section, .ai-quota-section {
  margin-bottom: 16px;
}
.level-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}
.level-exp, .ai-count {
  font-weight: bold;
  color: #303133;
}
.checkin-section {
  text-align: center;
  margin: 16px 0;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}
.checkin-btn {
  width: 80%;
  font-size: 14px;
}
.streak-info {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.streak-tip {
  color: #e6a23c;
}
.daily-tasks-section {
  margin-top: 12px;
}
.tasks-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}
.task-item {
  margin-bottom: 12px;
}
.task-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 4px;
}
.task-name {
  color: #303133;
}
.task-reward {
  color: #e6a23c;
  font-size: 12px;
}
.task-progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.task-progress {
  flex: 1;
}
.bonus-info {
  margin-top: 6px;
  font-size: 12px;
  color: #e6a23c;
}
</style>
