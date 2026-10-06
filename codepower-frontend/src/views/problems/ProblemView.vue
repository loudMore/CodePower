<!-- 题库列表 — 筛选、搜索、题目集 -->
<template>
  <div class="problem-page">
    <el-tabs v-model="activeTab" class="problem-tabs" type="card">
      <!-- 公共题库 -->
      <el-tab-pane label="公共题库" name="public">
        <el-card class="problem-filter-card" shadow="never">
          <div class="filter-section">
            <el-input
              v-model="searchText"
              placeholder="搜索题目..."
              prefix-icon="Search"
              clearable
              @input="handleSearch"
              class="search-input"
            />
            <el-select v-model="difficultyFilter" placeholder="难度" clearable class="filter-select">
              <el-option label="简单" value="简单" />
              <el-option label="普通" value="普通" />
              <el-option label="困难" value="困难" />
              <el-option label="极限" value="极限" />
            </el-select>
            <el-select
              v-model="tagFilter"
              placeholder="分类（可多选）"
              clearable
              class="filter-select"
              multiple filterable allow-create default-first-option
              style="width: 250px;"
              collapse-tags collapse-tags-tooltip :max-collapse-tags="2"
            >
              <el-option v-for="tag in tags" :key="tag" :label="tag" :value="tag" />
            </el-select>
          </div>
        </el-card>

        <el-table :data="pagedProblems" style="width: 100%" v-loading="loading" @row-click="handleRowClick" :row-class-name="tableRowClassName">
          <el-table-column label="" width="40" align="center">
            <template #default="scope">
              <span v-if="scope.row.solved === 'pass'" style="color:#67c23a;font-size:16px;">&#10004;</span>
              <span v-else-if="scope.row.solved === 'fail'" style="color:#f56c6c;font-size:14px;">&#10006;</span>
            </template>
          </el-table-column>
          <el-table-column prop="id" label="编号" width="80" align="center" />
          <el-table-column prop="title" label="题目" min-width="200">
            <template #default="scope">
              <div class="problem-title-cell">
                <span>{{ scope.row.title }}</span>
                <el-tag
                  v-if="scope.row.difficulty"
                  :type="getDifficultyType(scope.row.difficulty)"
                  size="small"
                  class="difficulty-tag"
                  :class="{'difficulty-blue': scope.row.difficulty === '困难'}"
                >
                  {{ scope.row.difficulty }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="author" label="作者" width="100" align="center" />
          <el-table-column prop="tags" label="标签" min-width="200">
            <template #default="scope">
              <el-tag v-for="tag in scope.row.tags" :key="tag" class="mx-1" size="small">{{ tag }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submitCount" label="提交次数" width="100" align="center" />
          <el-table-column prop="passRate" label="通过率" width="100" align="center" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button type="primary" link @click.stop="startProblem(scope.row)">开始挑战</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pagination" background layout="prev, pager, next"
          :total="totalProblems" :page-size="PUBLIC_PAGE_SIZE"
          v-model:current-page="currentPage"
        />
      </el-tab-pane>

      <!-- 我的题库 (仅出题者/管理员可见) -->
      <el-tab-pane v-if="canCreateContent" label="我的题库" name="my">
        <div class="my-problems-header">
          <h3>我创建的题目</h3>
          <el-button type="primary" @click="createNewProblem">
            <el-icon><plus /></el-icon> 创建新题目
          </el-button>
        </div>
        <el-card class="problem-filter-card" shadow="never" style="margin-bottom: 20px;">
          <div class="filter-section">
            <el-input v-model="mySearchText" placeholder="搜索题目..." prefix-icon="Search" clearable class="search-input" />
            <el-select v-model="myDifficultyFilter" placeholder="难度" clearable class="filter-select">
              <el-option label="简单" value="简单" />
              <el-option label="普通" value="普通" />
              <el-option label="困难" value="困难" />
              <el-option label="极限" value="极限" />
            </el-select>
            <el-select
              v-model="myTagFilter" placeholder="分类（可多选）" clearable class="filter-select"
              multiple filterable allow-create default-first-option
              style="width: 250px;" collapse-tags collapse-tags-tooltip :max-collapse-tags="2"
            >
              <el-option v-for="tag in tags" :key="tag" :label="tag" :value="tag" />
            </el-select>
          </div>
        </el-card>
        <el-empty v-if="myProblems.length === 0" description="您还没有创建题目">
          <el-button type="primary" @click="createNewProblem">现在创建</el-button>
        </el-empty>
        <el-table v-else :data="pagedMyProblems" style="width: 100%" :row-class-name="myTableRowClassName">
          <el-table-column prop="id" label="编号" width="80" align="center" />
          <el-table-column prop="title" label="题目" min-width="200">
            <template #default="scope">
              <el-link type="primary" @click.stop="startProblem(scope.row)">{{ scope.row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="tags" label="标签" min-width="200">
            <template #default="scope">
              <el-tag v-for="tag in scope.row.tags" :key="tag" class="mx-1" size="small">{{ tag }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getDifficultyType(scope.row.difficulty)" size="small" :class="{'difficulty-blue': scope.row.difficulty === '困难'}">
                {{ scope.row.difficulty }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.isPublic ? 'success' : 'info'" size="small">
                {{ scope.row.isPublic ? '公开' : '私有' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="scope">
              <el-button type="primary" link @click="editProblem(scope.row)">编辑</el-button>
              <el-button type="primary" link @click="toggleVisibility(scope.row)">
                {{ scope.row.isPublic ? '设为私有' : '公开' }}
              </el-button>
              <el-popconfirm title="确定要删除这个题目吗？" @confirm="deleteProblemHandler(scope.row)">
                <template #reference>
                  <el-button type="danger" link>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pagination" background layout="prev, pager, next"
          :total="filteredMyProblems.length" :page-size="12"
          v-model:current-page="myCurrentPage"
        />
      </el-tab-pane>

      <!-- 公开题目集 -->
      <el-tab-pane label="公开题目集" name="publicSets">
        <div class="sets-grid" v-loading="setsLoading">
          <el-empty v-if="publicSets.length === 0 && !setsLoading" description="暂无公开题目集" />
          <el-card
            v-for="s in publicSets" :key="s.id"
            class="set-card" shadow="hover"
            @click="openProblemSetDetail(s, 'publicSets')"
          >
            <div class="set-card-body">
              <div class="set-title">{{ s.title }}</div>
              <div class="set-desc">{{ s.description || '暂无描述' }}</div>
              <div class="set-meta">
                <el-tag size="small" :type="s.type === 'OFFICIAL' ? 'danger' : 'info'">
                  {{ s.type === 'OFFICIAL' ? '官方' : '用户' }}
                </el-tag>
                <span class="set-count">{{ s.problemCount }} 题</span>
                <span class="set-creator">{{ s.creatorName }}</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 我的题目集 (仅出题者/管理员可见) -->
      <el-tab-pane v-if="canCreateContent" label="我的题目集" name="mySets">
        <div class="my-problems-header">
          <h3>我的题目集</h3>
          <el-button type="primary" @click="showCreateSetDialog = true">
            <el-icon><plus /></el-icon> 创建题目集
          </el-button>
        </div>
        <div class="sets-grid" v-loading="mySetsLoading">
          <el-empty v-if="mySets.length === 0 && !mySetsLoading" description="您还没有创建题目集">
            <el-button type="primary" @click="showCreateSetDialog = true">现在创建</el-button>
          </el-empty>
          <el-card
            v-for="s in mySets" :key="s.id"
            class="set-card" shadow="hover"
            @click="openProblemSetDetail(s, 'mySets')"
          >
            <div class="set-card-body">
              <div class="set-title">{{ s.title }}</div>
              <div class="set-desc">{{ s.description || '暂无描述' }}</div>
              <div class="set-meta">
                <el-tag size="small" :type="s.type === 'OFFICIAL' ? 'danger' : 'info'">
                  {{ s.type === 'OFFICIAL' ? '官方' : '个人' }}
                </el-tag>
                <span class="set-count">{{ s.problemCount }} 题</span>
                <el-tag size="small" :type="s.isPublic ? 'success' : 'warning'">
                  {{ s.isPublic ? '公开' : '私有' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建题目集对话框 -->
    <el-dialog v-model="showCreateSetDialog" title="创建题目集" width="480px">
      <el-form :model="createSetForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="createSetForm.title" placeholder="题目集名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createSetForm.description" type="textarea" :rows="3" placeholder="题目集描述" />
        </el-form-item>
        <el-form-item v-if="isAdmin" label="类型">
          <el-radio-group v-model="createSetForm.type">
            <el-radio value="OFFICIAL">官方题目集</el-radio>
            <el-radio value="PRIVATE">个人题目集</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公开">
          <el-switch v-model="createSetForm.isPublicBool" active-text="公开" inactive-text="私有" />
          <div class="privacy-tip">
            私有题目集可放入竞赛；公开题目集只展示公开题，题目作者改私有后会自动从公开视图隐藏。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateSetDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSet" :loading="createSetLoading">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getProblems, getProblemsByAuthor, updateProblemVisibility, deleteProblem } from '@/api/problem'
import { getAllTags } from '@/api/problem'
import { submissionApi } from '@/api/submission'
import { problemSetApi } from '@/api/problemSet'
import { checkTokenValid } from '@/api/index'
import { getMemoryCache, setMemoryCache, deleteMemoryCacheByPrefix } from '@/utils/memoryCache'

// 题库列表页面：负责公共题库分页筛选、通过状态标识、我的题目和题目集入口。

const route = useRoute()
const router = useRouter()
const activeTab = ref('public')
const PROBLEM_LIST_CACHE_TTL = 30_000

// 角色判断
const getUserRole = () => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return u.role || ''
  } catch { return '' }
}
const canCreateContent = computed(() => {
  const role = getUserRole()
  return role === 'SENIOR_USER' || role === 'ADMIN'
})
const isAdmin = computed(() => getUserRole() === 'ADMIN')

// ========== 公共题库 ==========
const searchText = ref('')
const difficultyFilter = ref('')
const tagFilter = ref<string[]>([])
const currentPage = ref(1)
const loading = ref(false)
const problems = ref<any[]>([])
const totalProblems = ref(0)
const allTags = ref<any[]>([])
const PUBLIC_PAGE_SIZE = 12

// 加载公共题库列表，优先读前端短期缓存，未命中再向后端分页查询。
const loadProblems = async (force = false) => {
  const normalizedTags = [...tagFilter.value].map(tag => tag.trim()).filter(Boolean).sort()
  const cacheKey = `problem:list:${currentPage.value}:${PUBLIC_PAGE_SIZE}:${searchText.value}:${difficultyFilter.value}:${normalizedTags.join(',')}`
  if (!force) {
    const cached = getMemoryCache<{ records: any[]; total: number }>(cacheKey)
    if (cached) {
      problems.value = cached.records
      totalProblems.value = cached.total
      await loadUserStatus()
      return
    }
  }

  try {
    loading.value = true
    const params: any = { current: currentPage.value, size: PUBLIC_PAGE_SIZE }
    if (searchText.value) params.title = searchText.value
    if (difficultyFilter.value) params.difficulty = difficultyFilter.value
    if (normalizedTags.length > 0) params.tagNames = normalizedTags.join(',')
    const response = await getProblems(params)
    if (response && response.records) {
      problems.value = response.records.map((p: any) => ({
        id: p.id, title: p.title, difficulty: p.difficulty,
        tags: p.tags ? p.tags.map((t: any) => t.name) : [],
        submitCount: p.submitCount || 0,
        passRate: p.acceptRate != null ? p.acceptRate + '%' : '0%',
        author: p.authorName || '未知用户', solved: ''
      }))
      totalProblems.value = Number(response.total || 0)
      setMemoryCache(cacheKey, { records: problems.value, total: totalProblems.value }, PROBLEM_LIST_CACHE_TTL)
      await loadUserStatus()
    }
  } catch { ElMessage.error('加载题目列表失败') } finally { loading.value = false }
}

// 批量查询当前用户对本页题目的通过/尝试状态，用绿色和红色辅助刷题。
const loadUserStatus = async () => {
  if (!checkTokenValid() || problems.value.length === 0) return
  try {
    const ids = problems.value.map(p => p.id)
    const res: any = await submissionApi.getUserProblemStatus(ids)
    const statusMap = res.data || res
    if (statusMap) {
      problems.value.forEach(p => {
        const st = statusMap[p.id]
        p.solved = st === 'ACCEPTED' ? 'pass' : st === 'ATTEMPTED' ? 'fail' : ''
      })
    }
  } catch {}
}

const tags = computed(() => allTags.value.map((t: any) => t.name))

// 题库分页、筛选已由后端处理，这里保持 records 原样展示。
const filteredProblems = computed(() => {
  return problems.value
})

// 当前页题目数据直接来自后端分页结果，避免前端一次性拉全量题库。
const pagedProblems = computed(() => {
  return filteredProblems.value
})

// 搜索、难度、标签变化后回到第一页重新请求，避免页码越界。
const reloadProblemsFromFirstPage = () => {
  if (currentPage.value === 1) {
    loadProblems(true)
  } else {
    currentPage.value = 1
  }
}
// 搜索防抖 — 避免每次按键都触发 API 请求。
let searchTimer: number
const handleSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    reloadProblemsFromFirstPage()
  }, 300)
}

// ========== 我的题库 ==========
const myProblems = ref<any[]>([])
const mySearchText = ref('')
const myDifficultyFilter = ref('')
const myTagFilter = ref<string[]>([])
const myCurrentPage = ref(1)

// 加载当前用户创建的题目，供高级用户和管理员维护自己的题库。
const loadMyProblems = async () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (!userInfo.id) { myProblems.value = []; return }
    const response = await getProblemsByAuthor(userInfo.id)
    if (response) {
      myProblems.value = response.map((p: any) => ({
        id: p.id, title: p.title, difficulty: p.difficulty,
        tags: p.tags ? p.tags.map((t: any) => t.name) : [],
        isPublic: p.visibility === 'PUBLIC'
      }))
    }
  } catch { ElMessage.error('加载我的题目失败') }
}

const filteredMyProblems = computed(() => {
  return myProblems.value.filter(p => {
    const s = !mySearchText.value || p.title.toLowerCase().includes(mySearchText.value.toLowerCase())
    const d = !myDifficultyFilter.value || p.difficulty === myDifficultyFilter.value
    const t = !myTagFilter.value.length || (p.tags && myTagFilter.value.every((tag: string) => p.tags.includes(tag)))
    return s && d && t
  })
})

// 我的题目数据量通常较小，当前页先在前端做本地分页。
const pagedMyProblems = computed(() => {
  const start = (myCurrentPage.value - 1) * 12
  return filteredMyProblems.value.slice(start, start + 12)
})

// ========== 题目集 ==========
const publicSets = ref<any[]>([])
const mySets = ref<any[]>([])
const setsLoading = ref(false)
const mySetsLoading = ref(false)
const showCreateSetDialog = ref(false)
const createSetLoading = ref(false)
const createSetForm = ref({ title: '', description: '', type: 'PRIVATE', isPublicBool: true })

// 加载公开题目集列表。
const loadPublicSets = async () => {
  setsLoading.value = true
  try {
    const res: any = await problemSetApi.list({ onlyPublic: true, size: 200 })
    const data = res.data || res
    const list = data.records || []
    publicSets.value = Array.isArray(list) ? list : []
  } catch { publicSets.value = [] } finally { setsLoading.value = false }
}

// 加载当前用户创建的题目集列表。
const loadMySets = async () => {
  mySetsLoading.value = true
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (!userInfo.id) {
      mySets.value = []
      return
    }
    const res: any = await problemSetApi.list({ mine: true, size: 200 })
    const data = res.data || res
    const list = data.records || []
    mySets.value = Array.isArray(list) ? list : []
  } catch { mySets.value = [] } finally { mySetsLoading.value = false }
}

// 根据路由 query 恢复题库标签页，方便从题目集详情返回原页签。
const syncActiveTabFromRoute = () => {
  if (route.path !== '/problems') return
  const tab = route.query.tab
  if (tab === 'publicSets' || tab === 'mySets' || tab === 'public' || tab === 'my') {
    activeTab.value = tab
  }
}

// 题目集页签按需加载，避免每次进入题库都请求所有题目集。
const refreshProblemSetListsByTab = async (tab: unknown) => {
  if (route.path !== '/problems') return
  if (tab === 'publicSets') {
    await loadPublicSets()
    return
  }
  if (tab === 'mySets' && canCreateContent.value) {
    await loadMySets()
  }
}

// 进入题目集详情时带上来源页签，返回时能回到公开/我的题目集。
const openProblemSetDetail = (set: any, tab: 'publicSets' | 'mySets') => {
  router.push({
    path: `/problem-sets/${set.id}`,
    query: { fromTab: tab }
  })
}

// 创建题目集，前端把公开/私有布尔值转成后端需要的类型字段。
const handleCreateSet = async () => {
  if (!createSetForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  createSetLoading.value = true
  try {
    await problemSetApi.create({
      title: createSetForm.value.title.trim(),
      description: createSetForm.value.description,
      type: isAdmin.value ? createSetForm.value.type : 'PRIVATE',
      isPublic: createSetForm.value.isPublicBool ? 1 : 0
    })
    ElMessage.success('题目集创建成功')
    showCreateSetDialog.value = false
    createSetForm.value = { title: '', description: '', type: 'PRIVATE', isPublicBool: true }
    await loadMySets()
    await loadPublicSets()
    activeTab.value = 'mySets'
  } catch { ElMessage.error('创建失败') } finally { createSetLoading.value = false }
}

// 切换标签页时按需加载对应数据，并把当前页签写入 URL。
watch(activeTab, (tab, previousTab) => {
  if (route.path !== '/problems') return
  if (route.query.tab !== tab) {
    router.replace({ path: '/problems', query: { tab } })
  }
  if (tab !== previousTab) {
    refreshProblemSetListsByTab(tab)
  }
})

watch(() => [route.path, route.query.tab, route.query.refreshed], async ([path, tab]) => {
  if (path !== '/problems') return
  syncActiveTabFromRoute()
  await refreshProblemSetListsByTab(tab)
})

watch(currentPage, () => {
  loadProblems()
})

watch(difficultyFilter, () => {
  reloadProblemsFromFirstPage()
})

watch(tagFilter, () => {
  reloadProblemsFromFirstPage()
}, { deep: true })

// ========== 通用操作 ==========

// 加载题目标签，用于题库筛选和我的题目筛选。
const loadTags = async () => {
  try { const r = await getAllTags(); if (r) allTags.value = r } catch {}
}

// 切换题目公开/私有；公开题库缓存需要同步失效。
const toggleVisibility = async (problem: any) => {
  try {
    const v = problem.isPublic ? 'PRIVATE' : 'PUBLIC'
    await updateProblemVisibility(problem.id, v)
    problem.isPublic = !problem.isPublic
    deleteMemoryCacheByPrefix('problem:list:')
    if (activeTab.value === 'public') loadProblems(true)
    ElMessage.success(`题目已设为${problem.isPublic ? '公开' : '私有'}`)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.response?.data?.error || '更新失败')
  }
}

// 删除自己创建的题目，并清理题库列表缓存。
const deleteProblemHandler = async (problem: any) => {
  try {
    await deleteProblem(problem.id)
    myProblems.value = myProblems.value.filter(p => p.id !== problem.id)
    deleteMemoryCacheByPrefix('problem:list:')
    if (problem.isPublic) loadProblems(true)
    ElMessage.success(`题目已删除`)
  } catch { ElMessage.error('删除失败') }
}

// 将题目难度映射到 Element Plus 标签类型。
const getDifficultyType = (d: string) => {
  switch (d) {
    case '简单': return 'success'
    case '普通': return 'warning'
    case '困难': return 'extreme'
    case '极限': return 'danger'
    default: return 'info'
  }
}
// 题库列表行样式：绿色表示已通过，红色表示尝试过但未通过。
const tableRowClassName = ({ row }: { row: any }) => {
  if (row.solved === 'pass') return 'solved-row'
  if (row.solved === 'fail') return 'error-row'
  return 'default-row'
}
const myTableRowClassName = () => 'default-row'

// 点击题目行进入做题 IDE。
const handleRowClick = (row: any) => startProblem(row)
const startProblem = (p: any) => router.push(`/problems/${p.id}`)

// 高级用户/管理员进入出题和编辑页面。
const createNewProblem = () => router.push('/problems/create')
const editProblem = (p: any) => router.push(`/problems/${p.id}/edit`)

onMounted(() => {
  syncActiveTabFromRoute()
  loadProblems()
  if (canCreateContent.value) loadMyProblems()
  loadTags()
  if (activeTab.value === 'publicSets') loadPublicSets()
  if (activeTab.value === 'mySets' && canCreateContent.value) loadMySets()
})
</script>

<style scoped>
.problem-page { width: 100%; }
.problem-tabs { width: 100%; }
.problem-filter-card { margin-bottom: 20px; }
.filter-section { display: flex; gap: 15px; align-items: center; flex-wrap: wrap; }
.search-input { width: 300px; }
.filter-select { width: 150px; }
.difficulty-tag { margin-left: 10px; }
.problem-title-cell { display: flex; align-items: center; }
:deep(.el-table__row.solved-row) { background-color: #f0f9eb !important; }
:deep(.el-table__row.error-row) { background-color: #fff1f0 !important; }
:deep(.el-table__row.default-row) { background-color: #fff !important; }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
.my-problems-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.my-problems-header h3 { margin: 0; color: #303133; }
.difficulty-blue { background-color: #e6f0fa !important; color: #1976d2 !important; border-color: #b3d8fd !important; }

/* 题目集卡片网格 */
.sets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  min-height: 200px;
}
.set-card { cursor: pointer; transition: transform 0.2s; }
.set-card:hover { transform: translateY(-2px); }
.set-card-body { padding: 4px 0; }
.set-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.set-desc { font-size: 13px; color: #909399; margin-bottom: 12px; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.set-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.set-count { font-size: 12px; color: #606266; }
.set-creator { font-size: 12px; color: #909399; }
.privacy-tip {
  width: 100%;
  margin-top: 6px;
  color: #7a8b9a;
  font-size: 12px;
  line-height: 1.5;
}
</style>
