<!-- 题目集详情页面 -->
<template>
  <div class="set-detail-container" v-loading="loading">
    <el-button text @click="goBack" class="back-btn">
      <el-icon><ArrowLeft /></el-icon> 返回题目集
    </el-button>

    <div class="set-header" v-if="detail">
      <div class="title-row">
        <h1>{{ detail.title }}</h1>
        <el-tag :type="detail.type === 'OFFICIAL' ? 'warning' : ''" size="large">
          {{ detail.type === 'OFFICIAL' ? '官方题库' : '个人题库' }}
        </el-tag>
      </div>
      <p class="set-desc" v-if="detail.description">{{ detail.description }}</p>
      <div class="set-meta">
        <span>创建者：{{ detail.creatorName || '-' }}</span>
        <span>共 {{ detail.problemCount || 0 }} 题</span>
      </div>
    </div>

    <!-- 管理区域（仅创建者/管理员可见） -->
    <div class="manage-area" v-if="canManage">
      <el-tag size="small" :type="detail?.isPublic ? 'success' : 'warning'">当前：{{ visibilityText }}</el-tag>
      <el-button type="warning" size="small" plain @click="toggleSetVisibility">
        {{ detail?.isPublic ? '设为私有' : '设为公开' }}
      </el-button>
      <el-button type="primary" size="small" @click="showAddDialog">添加题目</el-button>
      <el-button type="danger" size="small" plain @click="handleDeleteSet">删除题目集</el-button>
    </div>
    <p class="set-privacy-tip" v-if="canManage">
      私有题目集可导入竞赛；非作者学生只在对应竞赛中可做题，适合考前保护原创题。
    </p>
    <el-alert
      v-if="canManage && isPublicSet && privateItemCount > 0"
      class="set-stale-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="`当前公开题目集有 ${privateItemCount} 道题已转为私有，普通用户不会看到这些题。`"
      description="题目可见性由题目作者控制；可移除这些题，或将本题目集改为私有后用于竞赛。"
    />

    <!-- 题目列表 -->
    <el-table :data="items" stripe class="items-table">
      <el-table-column type="index" label="#" width="60" />
      <el-table-column label="题目">
        <template #default="{ row }">
          <div class="problem-cell">
            <!-- 私有题目显示锁定图标 -->
            <el-icon v-if="row.visibility === 'PRIVATE' || row.accessStatus === 'INACCESSIBLE'" class="lock-icon"><Lock /></el-icon>
            <el-link :type="row.accessStatus === 'INACCESSIBLE' ? 'info' : 'primary'"
                     @click="goToProblem(row)"
                     :disabled="!canAccessProblem(row)">
              {{ row.problemTitle || '未命名题目' }}
            </el-link>
            <el-tag v-if="row.accessStatus === 'INACCESSIBLE'" size="small" type="danger" style="margin-left: 6px">失效</el-tag>
            <el-tag v-else-if="row.visibility === 'PRIVATE'" size="small" type="info" style="margin-left: 6px">私有</el-tag>
            <span v-if="row.accessStatus === 'INACCESSIBLE'" class="invalid-reason">
              {{ row.invalidReason || '当前账号无权访问' }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="100">
        <template #default="{ row }">
          <el-tag :type="diffMap[row.difficulty] || 'info'" size="small">{{ row.difficulty || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" v-if="canManage">
        <template #default="{ row }">
          <el-button type="danger" size="small" text @click="removeItem(row.problemId)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="items.length === 0 && !loading" description="题目集暂无题目" />

    <!-- 添加题目对话框 -->
    <el-dialog v-model="addVisible" width="860px" class="add-problem-dialog" :show-close="false">
      <template #header>
        <div class="add-dialog-header">
          <div>
            <div class="add-dialog-kicker">{{ addDialogKicker }}</div>
            <h3>添加题目到「{{ detail?.title || '题目集' }}」</h3>
            <p>{{ addDialogTip }}</p>
          </div>
          <el-button text circle @click="addVisible = false">×</el-button>
        </div>
      </template>

      <div class="add-search-panel">
        <el-radio-group v-model="candidateSource" size="small">
          <el-radio-button label="public">公开题库</el-radio-button>
          <el-radio-button label="mine">我的题目</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="candidateKeyword"
          clearable
          placeholder="搜索题目标题，例如：A+B、动态规划、最短路"
          :prefix-icon="Search"
          @keyup.enter="loadCandidateProblems(1)"
          @clear="loadCandidateProblems(1)"
        />
        <el-select v-model="candidateDifficulty" clearable placeholder="全部难度" style="width: 140px" @change="loadCandidateProblems(1)">
          <el-option label="简单" value="简单" />
          <el-option label="普通" value="普通" />
          <el-option label="困难" value="困难" />
          <el-option label="极限" value="极限" />
        </el-select>
        <el-button type="primary" @click="loadCandidateProblems(1)" :loading="candidateLoading">
          搜索
        </el-button>
      </div>

      <div class="candidate-summary">
        <span>已选择 {{ selectedCandidates.length }} 题</span>
        <span>已自动隐藏当前题集已有的 {{ existingProblemIds.size }} 题</span>
      </div>

      <el-table
        :data="candidateProblems"
        v-loading="candidateLoading"
        class="candidate-table"
        height="390"
        @selection-change="handleCandidateSelectionChange"
      >
        <el-table-column type="selection" width="46" :selectable="isCandidateSelectable" />
        <el-table-column label="题目" min-width="260">
          <template #default="{ row }">
            <div class="candidate-title">
              <span class="candidate-id">#{{ row.id }}</span>
              <el-link type="primary" @click.stop="router.push(`/problems/${row.id}`)">{{ row.title }}</el-link>
              <el-tag v-if="normalizeVisibility(row) === 'PRIVATE'" size="small" type="info">私有</el-tag>
            </div>
            <div class="candidate-meta">
              <span>出题人：{{ row.authorName || '未知' }}</span>
              <span v-if="row.acceptRate != null">通过率：{{ row.acceptRate }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="tag in (row.tags || []).slice(0, 3)" :key="tag.id || tag.name || tag" size="small" effect="plain" class="tag-gap">
              {{ tag.name || tag }}
            </el-tag>
            <span v-if="!row.tags || row.tags.length === 0" class="muted">暂无标签</span>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="diffMap[row.difficulty] || 'info'" size="small">{{ formatDifficulty(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="candidate-footer">
        <el-pagination
          small
          background
          layout="prev, pager, next"
          :total="candidateTotal"
          :page-size="candidatePageSize"
          :current-page="candidatePage"
          @current-change="loadCandidateProblems"
        />
      </div>

      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddItem" :loading="adding" :disabled="selectedCandidates.length === 0">
          添加 {{ selectedCandidates.length || '' }} 题
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Lock, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { problemSetApi } from '@/api/problemSet'
import { problemApi } from '@/api'

const route = useRoute()
const router = useRouter()
const setId = Number(route.params.id)

const loading = ref(true)
const adding = ref(false)
const detail = ref<any>(null)
const items = ref<any[]>([])
const addVisible = ref(false)
const candidateKeyword = ref('')
const candidateDifficulty = ref('')
const candidateSource = ref<'public' | 'mine'>('public')
const candidateProblems = ref<any[]>([])
const selectedCandidates = ref<any[]>([])
const candidateLoading = ref(false)
const candidatePage = ref(1)
const candidatePageSize = 8
const candidateTotal = ref(0)

let candidateSearchTimer: number | undefined

const diffMap: Record<string, string> = {
  EASY: 'success',
  MEDIUM: 'warning',
  HARD: 'danger',
  EXTREME: 'danger',
  简单: 'success',
  普通: 'warning',
  困难: 'danger',
  极限: 'danger'
}
const diffTextMap: Record<string, string> = { EASY: '简单', MEDIUM: '普通', HARD: '困难', EXTREME: '极限' }

const userInfo = computed(() => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} }
})

const canManage = computed(() => {
  if (!detail.value) return false
  if (userInfo.value.role === 'ADMIN') return true
  return detail.value.creatorId === userInfo.value.id
})

const visibilityText = computed(() => detail.value?.isPublic ? '公开' : '私有')
const isPublicSet = computed(() => detail.value?.isPublic === 1 || detail.value?.isPublic === true)
const existingProblemIds = computed(() => new Set(items.value.map(item => Number(item.problemId))))
const privateItemCount = computed(() => items.value.filter(item => item.visibility === 'PRIVATE').length)
const addDialogKicker = computed(() => candidateSource.value === 'mine' ? '我的题目' : '公开题库检索')
const addDialogTip = computed(() => {
  if (isPublicSet.value) return '公开题目集只能添加公开题；如需放入私有题，请先将题目集设为私有。'
  return '可添加公开题或自己创建的私有题，私有题随题目集导入竞赛后仅授权参赛者可做题。'
})

const goBack = () => {
  const fromTab = route.query.fromTab === 'mySets' ? 'mySets' : 'publicSets'
  const refreshed = typeof route.query.refreshed === 'string' ? route.query.refreshed : undefined
  router.push({
    path: '/problems',
    query: refreshed ? { tab: fromTab, refreshed } : { tab: fromTab }
  })
}

const canAccessProblem = (row: any) => {
  if (!row || row.accessStatus === 'INACCESSIBLE') return false
  if (row.visibility === 'PUBLIC') return true
  if (userInfo.value.role === 'ADMIN') return true
  return Number(row.problemAuthorId) === Number(userInfo.value.id)
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res: any = await problemSetApi.getDetail(setId)
    detail.value = res.data || res
    const itemsRes: any = await problemSetApi.getItems(setId)
    items.value = itemsRes.data || itemsRes || []
  } catch (e) {
    console.error('加载题目集失败', e)
  } finally {
    loading.value = false
  }
}

watch(() => route.query.fromTab, () => {
  if (!route.query.fromTab) {
    router.replace({
      path: route.path,
      query: { fromTab: 'publicSets' }
    })
  }
}, { immediate: true })

const goToProblem = (row: any) => {
  if (!canAccessProblem(row)) {
    ElMessage.warning(row.invalidReason || '该题目当前不可访问')
    return
  }
  router.push(`/problems/${row.problemId}`)
}

const formatDifficulty = (difficulty?: string) => diffTextMap[difficulty || ''] || difficulty || '-'

const showAddDialog = () => {
  candidateKeyword.value = ''
  candidateDifficulty.value = ''
  candidateSource.value = isPublicSet.value ? 'public' : 'mine'
  selectedCandidates.value = []
  addVisible.value = true
  loadCandidateProblems(1)
}

const normalizeVisibility = (row: any) => row?.visibility || (row?.isPublic ? 'PUBLIC' : 'PRIVATE')

const isCandidateSelectable = (row: any) => {
  if (existingProblemIds.value.has(Number(row.id))) return false
  if (isPublicSet.value && normalizeVisibility(row) !== 'PUBLIC') return false
  return true
}

const handleCandidateSelectionChange = (rows: any[]) => {
  selectedCandidates.value = rows.filter(row => isCandidateSelectable(row))
}

const loadCandidateProblems = async (page = candidatePage.value) => {
  candidatePage.value = page
  candidateLoading.value = true
  try {
    if (candidateSource.value === 'mine' && userInfo.value.id) {
      const res: any = await problemApi.getProblemsByAuthor(Number(userInfo.value.id))
      const records = Array.isArray(res.data || res) ? (res.data || res) : []
      const keyword = candidateKeyword.value.trim().toLowerCase()
      const filtered = records
        .filter((row: any) => !existingProblemIds.value.has(Number(row.id)))
        .filter((row: any) => !isPublicSet.value || normalizeVisibility(row) === 'PUBLIC')
        .filter((row: any) => !keyword || String(row.title || '').toLowerCase().includes(keyword))
        .filter((row: any) => !candidateDifficulty.value || row.difficulty === candidateDifficulty.value)
      candidateTotal.value = filtered.length
      const start = (candidatePage.value - 1) * candidatePageSize
      candidateProblems.value = filtered.slice(start, start + candidatePageSize)
    } else {
      const params: any = {
        current: candidatePage.value,
        size: candidatePageSize
      }
      if (candidateKeyword.value.trim()) params.title = candidateKeyword.value.trim()
      if (candidateDifficulty.value) params.difficulty = candidateDifficulty.value
      const res: any = await problemApi.getProblems(params)
      const data = res.data || res || {}
      const records = Array.isArray(data.records) ? data.records : []
      candidateProblems.value = records.filter((row: any) => !existingProblemIds.value.has(Number(row.id)))
      candidateTotal.value = Number(data.total || 0)
    }
  } catch (e) {
    console.error('加载候选题目失败', e)
    candidateProblems.value = []
    candidateTotal.value = 0
  } finally {
    candidateLoading.value = false
  }
}

watch(candidateKeyword, () => {
  if (!addVisible.value) return
  if (candidateSearchTimer) window.clearTimeout(candidateSearchTimer)
  candidateSearchTimer = window.setTimeout(() => loadCandidateProblems(1), 300)
})

watch(candidateSource, () => {
  if (!addVisible.value) return
  selectedCandidates.value = []
  loadCandidateProblems(1)
})

const getErrorMessage = (e: any, fallback: string) => (
  e?.response?.data?.message || e?.response?.data?.error || e?.data?.message || e?.message || fallback
)

const handleAddItem = async () => {
  const problemIds = selectedCandidates.value
    .map(item => Number(item.id))
    .filter(id => id > 0 && !existingProblemIds.value.has(id))
  if (problemIds.length === 0) { ElMessage.warning('请选择要添加的题目'); return }
  adding.value = true
  try {
    await problemSetApi.addItems(setId, problemIds.map(problemId => ({ problemId })))
    ElMessage.success(`已添加 ${problemIds.length} 道题`)
    addVisible.value = false
    await loadDetail()
  } catch (e: any) {
    ElMessage.error(getErrorMessage(e, '添加失败'))
  } finally {
    adding.value = false
  }
}

const removeItem = async (problemId: number) => {
  try {
    await ElMessageBox.confirm('确定要从题目集中移除此题目吗？', '确认', { type: 'warning' })
    await problemSetApi.removeItems(setId, [problemId])
    ElMessage.success('已移除')
    loadDetail()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const toggleSetVisibility = async () => {
  if (!detail.value) return
  try {
    const nextIsPublic = detail.value.isPublic ? 0 : 1
    if (nextIsPublic === 1) {
      const privateItems = items.value.filter(item => item.visibility === 'PRIVATE')
      if (privateItems.length > 0) {
        await ElMessageBox.alert(
          `当前题目集还有 ${privateItems.length} 道私有题。公开前请先到对应题目中逐题改为公开，系统不会自动替你修改题目可见性。`,
          '暂不能公开',
          { type: 'warning', confirmButtonText: '我知道了' }
        )
        return
      }
      await ElMessageBox.confirm(
        '公开后其他用户可以查看这个题目集。请确认其中不包含考试前需要保密的题目。',
        '确认公开题目集',
        { type: 'warning', confirmButtonText: '确认公开', cancelButtonText: '取消' }
      )
    }
    await problemSetApi.update(setId, { isPublic: nextIsPublic })
    detail.value = { ...detail.value, isPublic: nextIsPublic }
    ElMessage.success(`题目集已设为${nextIsPublic ? '公开' : '私有'}`)
    const fromTab = route.query.fromTab === 'mySets' ? 'mySets' : 'publicSets'
    router.replace({
      path: route.path,
      query: { fromTab, refreshed: String(Date.now()) }
    })
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, '更新失败'))
  }
}

const handleDeleteSet = async () => {
  try {
    await ElMessageBox.confirm('确定要删除此题目集吗？此操作不可恢复。', '确认删除', { type: 'warning' })
    await problemSetApi.remove(setId)
    ElMessage.success('题目集已删除')
    goBack()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.set-detail-container { max-width: 980px; margin: 0 auto; padding: 30px 20px; }
.back-btn { margin-bottom: 12px; }
.title-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.title-row h1 { font-size: 1.8rem; color: #333; margin: 0; }
.set-desc { color: #666; margin-bottom: 12px; }
.set-meta { display: flex; gap: 16px; color: #909399; font-size: 14px; margin-bottom: 20px; }
.manage-area { margin-bottom: 6px; display: flex; gap: 8px; flex-wrap: wrap; }

.set-privacy-tip {
  margin: 0 0 16px;
  color: #7a8b9a;
  font-size: 13px;
}

.set-stale-alert {
  margin: 0 0 16px;
}

.items-table { margin-bottom: 20px; }
.problem-cell { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.lock-icon { color: #C0C4CC; font-size: 14px; }
.invalid-reason {
  width: 100%;
  margin-left: 22px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.add-dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-right: 4px;
}

.add-dialog-kicker {
  display: inline-flex;
  margin-bottom: 6px;
  padding: 4px 9px;
  border-radius: 999px;
  background: #f0f7ff;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

.add-dialog-header h3 {
  margin: 0;
  color: #1f2d3d;
  font-size: 20px;
}

.add-dialog-header p {
  margin: 6px 0 0;
  color: #7a8b9a;
  font-size: 13px;
}

.add-search-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border: 1px solid #e4edf8;
  border-radius: 14px;
  background: linear-gradient(135deg, #f8fbff 0%, #ffffff 100%);
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.06);
}

.candidate-summary {
  display: flex;
  justify-content: space-between;
  margin: 12px 2px 10px;
  color: #7a8b9a;
  font-size: 13px;
}

.candidate-table {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
}

.candidate-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-weight: 600;
}

.candidate-id {
  color: #909399;
  font-family: Consolas, Monaco, monospace;
}

.candidate-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}

.candidate-footer {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.tag-gap {
  margin: 2px 4px 2px 0;
}

.muted {
  color: #c0c4cc;
  font-size: 13px;
}

@media (max-width: 760px) {
  .add-search-panel {
    flex-direction: column;
  }
}
</style>
