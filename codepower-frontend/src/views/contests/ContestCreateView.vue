<!-- 创建竞赛页面 -->
<template>
  <div class="contest-create-page">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">{{ isEditMode ? '编辑竞赛' : '创建竞赛' }}</span>
      </template>
    </el-page-header>

    <!-- 创建成功后展示邀请码 -->
    <el-result v-if="!isEditMode && createdContest" icon="success" title="竞赛创建成功">
      <template #sub-title>
        <div class="invite-code-section">
          <p>邀请码（分享给参赛者）：</p>
          <div class="invite-code-display">
            <span class="invite-code">{{ createdContest.inviteCode }}</span>
            <el-button type="primary" link @click="copyInviteCode">复制</el-button>
          </div>
          <p class="invite-hint" v-if="!createdContest.isPublic">
            非公开竞赛只能通过邀请码加入
          </p>
          <p class="invite-hint">
            参赛者可在竞赛列表的“邀请码加入”入口输入该码进入竞赛。
          </p>
        </div>
      </template>
      <template #extra>
        <el-button type="primary" @click="$router.push(`/contests/${createdContest.id}`)">查看竞赛</el-button>
        <el-button @click="$router.push('/contests')">返回列表</el-button>
      </template>
    </el-result>

    <el-form v-else ref="formRef" :model="form" :rules="rules" label-width="120px" class="contest-form">
      <el-form-item label="竞赛标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入竞赛标题" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="竞赛描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="竞赛说明、规则等" />
      </el-form-item>

      <el-form-item label="竞赛类型" prop="type">
        <el-radio-group v-model="form.type">
          <el-radio value="EXAM">限时考试</el-radio>
          <el-radio value="PRACTICE">练习赛</el-radio>
        </el-radio-group>
        <div class="contest-type-note">
          <div><strong>限时考试</strong>：适合模拟考试；若同时开启“不限时长”，竞赛长期开放，但每名参赛者从首次进入工作区开始按考试时长倒计时。</div>
          <div><strong>练习赛</strong>：适合日常训练；开启“不限时长”后没有个人倒计时，可长期反复进入。</div>
        </div>
      </el-form-item>

      <el-form-item label="是否公开">
        <el-switch v-model="publicSwitch" active-text="公开" inactive-text="非公开" />
        <span class="form-hint">非公开竞赛仅能通过邀请码加入</span>
      </el-form-item>

      <el-form-item v-if="isEditMode && !publicSwitch && editInviteCode" label="邀请码">
        <div class="invite-code-display edit-invite-code">
          <span class="invite-code">{{ editInviteCode }}</span>
          <el-button type="primary" link @click="copyEditInviteCode">复制</el-button>
          <span class="form-hint">创建者和管理员可见，用于邀请参赛者加入。</span>
        </div>
      </el-form-item>

      <el-form-item label="不限时长">
        <el-switch v-model="unlimitedDuration" active-text="不限时长" inactive-text="限时" />
        <span class="form-hint">{{ unlimitedDurationHint }}</span>
      </el-form-item>

      <el-form-item label="时间范围" prop="startTime">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          placeholder="开始时间"
          style="width: 220px; margin-right: 16px;"
        />
        <el-date-picker
          v-if="!unlimitedDuration"
          v-model="form.endTime"
          type="datetime"
          placeholder="结束时间"
          style="width: 220px;"
        />
        <el-tag v-else type="info" size="large" style="height: 32px; line-height: 32px;">长期开放</el-tag>
      </el-form-item>

      <el-form-item label="考试时长(分)" v-show="form.type === 'EXAM'">
        <el-input-number v-model="form.durationMinutes" :min="10" :max="600" :step="10" />
        <span class="form-hint">限时考试生效，参赛者从首次进入竞赛工作区开始计时</span>
      </el-form-item>

      <el-form-item label="最大参赛人数">
        <el-input-number v-model="form.maxParticipants" :min="0" :step="10" />
        <span class="form-hint">0 表示不限制</span>
      </el-form-item>

      <el-form-item label="加入密码">
        <el-input v-model="form.password" placeholder="留空则无需密码（公开竞赛可选）" />
      </el-form-item>

      <el-divider>高级设置</el-divider>

      <el-form-item label="允许粘贴代码">
        <el-switch v-model="allowPasteSwitch" active-text="允许" inactive-text="禁止" />
        <span class="form-hint">关闭后参赛者无法在编辑器中粘贴代码</span>
      </el-form-item>

      <el-form-item label="限制编程语言">
        <el-select v-model="form.allowedLanguageList" multiple placeholder="留空则允许全部语言" style="width: 100%">
          <el-option label="C++ (GCC 9.2.0)" value="cpp" />
          <el-option label="C (GCC 9.2.0)" value="c" />
          <el-option label="Java (OpenJDK 13.0.1)" value="java" />
          <el-option label="Python (3.8.1)" value="python" />
          <el-option label="JavaScript (Node.js)" value="javascript" />
          <el-option label="TypeScript" value="typescript" />
          <el-option label="Go" value="go" />
          <el-option label="Rust" value="rust" />
          <el-option label="C#" value="csharp" />
        </el-select>
      </el-form-item>

      <el-divider>选择题目</el-divider>

      <el-form-item label="从题目集导入">
        <el-select v-model="selectedSetId" placeholder="选择一个题目集" clearable style="width: 300px; margin-right: 12px;">
          <el-option v-for="s in problemSets" :key="s.id" :label="`${s.title}${s.isPublic ? '' : '（私有）'}`" :value="s.id" />
        </el-select>
        <el-button :disabled="!selectedSetId" @click="loadSetProblems">加载题目</el-button>
        <span class="form-hint">私有题目集可导入竞赛；学生只在竞赛授权范围内看到做题视图。</span>
      </el-form-item>

      <el-form-item label="已选题目">
        <el-table
          v-if="selectedProblems.length > 0"
          :data="selectedProblems"
          class="selected-problem-table"
          row-key="id"
          size="small"
          border
        >
          <el-table-column type="index" label="#" width="56" />
          <el-table-column prop="title" label="题目" min-width="220" show-overflow-tooltip />
          <el-table-column label="难度" width="100">
            <template #default="{ row }">
              <el-tag :type="difficultyTagType(row.difficulty)" size="small" :class="difficultyTagClass(row.difficulty)">
                {{ normalizeDifficultyLabel(row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分值" width="170">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="1" :max="1000" :step="5" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ $index }">
              <el-button text type="danger" @click="removeSelectedProblem($index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="尚未选择题目，可从题目集导入或从题库添加" :image-size="40" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" link @click="showProblemPicker = true">从题库手动添加题目</el-button>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEditMode ? '保存修改' : '创建竞赛' }}
        </el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>

    <!-- 题目选择弹窗 -->
    <el-dialog v-model="showProblemPicker" title="从题库选择题目" width="900px" @open="openProblemPicker">
      <div class="problem-picker-filters">
        <el-input v-model="pickerFilters.keyword" placeholder="搜索标题..." clearable @input="handlePickerFilterChange" />
        <el-input v-model="pickerFilters.id" placeholder="题目ID" clearable @input="handlePickerFilterChange" />
        <el-select v-model="pickerFilters.difficulty" placeholder="难度" clearable @change="handlePickerFilterChange">
          <el-option label="简单" value="简单" />
          <el-option label="普通" value="普通" />
          <el-option label="困难" value="困难" />
          <el-option label="极限" value="极限" />
        </el-select>
        <el-select v-model="pickerFilters.tags" placeholder="标签" clearable multiple class="tag-filter-select" @change="handlePickerFilterChange">
          <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
      </div>
      <el-table :data="pagedLibraryProblems" max-height="400" @selection-change="onPickerSelectionChange"
                ref="pickerTableRef" row-key="id" v-loading="pickerLoading">
        <el-table-column type="selection" width="40" :reserve-selection="true" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" />
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="tag in normalizeProblemTags(row)" :key="tag" size="small" class="picker-tag">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="difficultyTagType(row.difficulty)" size="small" :class="difficultyTagClass(row.difficulty)">
              {{ normalizeDifficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="problem-picker-footer">
        <span>已选 {{ pickerSelectedCount }} 题</span>
        <el-pagination
          background
          layout="prev, pager, next"
          :total="pickerTotal"
          :page-size="pickerPageSize"
          :current-page="pickerPage"
          @current-change="handlePickerPageChange"
        />
      </div>
      <template #footer>
        <el-button @click="showProblemPicker = false">取消</el-button>
        <el-button type="primary" @click="confirmPickerSelection">
          确认添加 ({{ pickerSelectedCount }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { contestApi } from '@/api/contest'
import { problemSetApi } from '@/api/problemSet'
import { getAllTags, getProblems } from '@/api/problem'
import { parseApiDate } from '@/utils/datetime'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const submitting = ref(false)
const createdContest = ref<any>(null)
const editInviteCode = ref('')
const isEditMode = computed(() => route.name === 'contest-edit')
const editContestId = computed(() => Number(route.params.id))
const existingContestProblemIds = ref<number[]>([])

const allowPasteSwitch = ref(true)
const publicSwitch = ref(true)
// 不限时长开关
const unlimitedDuration = ref(false)

const form = ref({
  title: '',
  description: '',
  type: 'PRACTICE',
  startTime: null as Date | null,
  endTime: null as Date | null,
  durationMinutes: 120,
  maxParticipants: 0,
  password: '',
  allowedLanguageList: [] as string[]
})

const rules = {
  title: [{ required: true, message: '请输入竞赛标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择竞赛类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }]
}

// 不限时长切换时自动设置结束时间和考试时长
watch(unlimitedDuration, (val) => {
  if (val) {
    form.value.endTime = new Date('2099-12-31T23:59:59')
  } else {
    form.value.endTime = null
    if (form.value.type === 'EXAM') form.value.durationMinutes = 120
  }
})

// 题目选择相关
const selectedProblems = ref<any[]>([])
const problemSets = ref<any[]>([])
const selectedSetId = ref<number | null>(null)
const showProblemPicker = ref(false)
const libraryProblems = ref<any[]>([])
const pickerTableRef = ref()
const pickerLoading = ref(false)
const pickerPage = ref(1)
const pickerPageSize = ref(10)
const pickerTotal = ref(0)
const pickerSelectedMap = ref<Record<number, any>>({})
const allTags = ref<string[]>([])
let restoringPickerSelection = false
let pickerSearchTimer: number | undefined
let pickerRequestSeq = 0
const pickerFilters = ref({
  keyword: '',
  id: '',
  difficulty: '',
  tags: [] as string[]
})

const filteredLibraryProblems = computed(() => {
  const existing = new Set(selectedProblems.value.map(p => p.id))
  return libraryProblems.value.filter(p => !existing.has(Number(p.id)))
})

const pagedLibraryProblems = computed(() => {
  return filteredLibraryProblems.value
})

const pickerSelectedCount = computed(() => Object.keys(pickerSelectedMap.value).length)
const unlimitedDurationHint = computed(() => {
  if (form.value.type === 'EXAM') {
    return '开启后竞赛长期开放，但每名参赛者仍按考试时长从首次进入起计时'
  }
  return '开启后竞赛无截止时间，可长期使用'
})

const normalizeDifficultyLabel = (difficulty?: string) => {
  const map: Record<string, string> = {
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
  return difficulty ? map[difficulty] || difficulty : '-'
}

const difficultyTagType = (difficulty?: string) => {
  const label = normalizeDifficultyLabel(difficulty)
  if (label === '简单') return 'success'
  if (label === '普通') return 'warning'
  if (label === '极限') return 'danger'
  return 'info'
}

const difficultyTagClass = (difficulty?: string) => {
  return { 'difficulty-blue': normalizeDifficultyLabel(difficulty) === '困难' }
}

const normalizeProblemTags = (problem: any) => {
  if (!problem?.tags) return []
  if (Array.isArray(problem.tags)) {
    return problem.tags.map((tag: any) => typeof tag === 'string' ? tag : tag?.name).filter(Boolean)
  }
  return String(problem.tags).split(/[,，、]/).map(tag => tag.trim()).filter(Boolean)
}

const restorePickerSelection = async () => {
  await nextTick()
  const table = pickerTableRef.value
  if (!table) return
  restoringPickerSelection = true
  table.clearSelection?.()
  pagedLibraryProblems.value.forEach(row => {
    if (pickerSelectedMap.value[Number(row.id)]) {
      table.toggleRowSelection?.(row, true)
    }
  })
  await nextTick()
  restoringPickerSelection = false
}

const loadPickerProblems = async (page = pickerPage.value) => {
  const seq = ++pickerRequestSeq
  pickerPage.value = page
  pickerLoading.value = true
  try {
    const params: any = {
      current: pickerPage.value,
      size: pickerPageSize.value
    }
    const keyword = pickerFilters.value.keyword.trim()
    const idKeyword = pickerFilters.value.id.trim()
    if (keyword) params.title = keyword
    if (idKeyword) {
      if (!/^\d+$/.test(idKeyword)) {
        libraryProblems.value = []
        pickerTotal.value = 0
        return
      }
      params.problemId = idKeyword
    }
    if (pickerFilters.value.difficulty) params.difficulty = pickerFilters.value.difficulty
    if (pickerFilters.value.tags.length > 0) params.tagNames = pickerFilters.value.tags.join(',')

    const res: any = await getProblems(params)
    if (seq !== pickerRequestSeq) return
    const data = res.data || res || {}
    const records = Array.isArray(data.records) ? data.records : []
    libraryProblems.value = records
    pickerTotal.value = Number(data.total || records.length)
    await restorePickerSelection()
  } catch {
    if (seq === pickerRequestSeq) {
      libraryProblems.value = []
      pickerTotal.value = 0
    }
  } finally {
    if (seq === pickerRequestSeq) pickerLoading.value = false
  }
}

const openProblemPicker = async () => {
  pickerSelectedMap.value = Object.fromEntries(selectedProblems.value.map(p => [Number(p.id), p]))
  await loadPickerProblems(1)
}

const handlePickerFilterChange = async () => {
  if (pickerSearchTimer !== undefined) {
    window.clearTimeout(pickerSearchTimer)
  }
  pickerSearchTimer = window.setTimeout(() => {
    loadPickerProblems(1)
  }, 250)
}

const handlePickerPageChange = async (page: number) => {
  await loadPickerProblems(page)
}

const loadSetProblems = async () => {
  if (!selectedSetId.value) return
  try {
    const res: any = await problemSetApi.getItems(selectedSetId.value)
    const items = res.data || res || []
    const existing = new Set(selectedProblems.value.map(p => p.id))
    let added = 0
    let skipped = 0
    for (const item of items) {
      if (item.accessStatus === 'INACCESSIBLE') {
        skipped++
        continue
      }
      const pid = item.problemId || item.id
      if (pid && !existing.has(pid)) {
        selectedProblems.value.push({
          id: pid,
          title: item.problemTitle || item.title || `题目#${pid}`,
          difficulty: item.difficulty || '普通',
          score: item.score || 100
        })
        existing.add(pid)
        added++
      }
    }
    if (skipped > 0) {
      ElMessage.warning(`已导入 ${added} 道题，跳过 ${skipped} 道无权访问的私有题`)
    } else {
      ElMessage.success(`已导入 ${added} 道题目`)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.response?.data?.error || '加载题目集失败')
  }
}

const removeSelectedProblem = (index: number) => {
  selectedProblems.value.splice(index, 1)
}

const onPickerSelectionChange = (selection: any[]) => {
  if (restoringPickerSelection) return
  const next = { ...pickerSelectedMap.value }
  pagedLibraryProblems.value.forEach(row => {
    delete next[Number(row.id)]
  })
  selection.forEach(row => {
    next[Number(row.id)] = {
      id: row.id,
      title: row.title,
      difficulty: row.difficulty,
      tags: normalizeProblemTags(row),
      score: row.score || 100
    }
  })
  pickerSelectedMap.value = next
}

const confirmPickerSelection = () => {
  selectedProblems.value = Object.values(pickerSelectedMap.value)
    .sort((a: any, b: any) => Number(a.id) - Number(b.id))
    .map((p: any) => ({
      id: p.id,
      title: p.title,
      difficulty: p.difficulty || '普通',
      tags: p.tags || [],
      score: p.score || 100
    }))
  showProblemPicker.value = false
}

const copyInviteCode = () => {
  if (createdContest.value?.inviteCode) {
    navigator.clipboard.writeText(createdContest.value.inviteCode)
    ElMessage.success('邀请码已复制')
  }
}

const copyEditInviteCode = () => {
  if (editInviteCode.value) {
    navigator.clipboard.writeText(editInviteCode.value)
    ElMessage.success('邀请码已复制')
  }
}

const isOpenEndedTime = (value?: string | Date | null) => {
  if (!value) return false
  const end = parseApiDate(value)
  return !end || !Number.isFinite(end.getTime()) || end.getFullYear() >= 2099
}

const applyContestToForm = async () => {
  if (!isEditMode.value || !Number.isFinite(editContestId.value)) return
  try {
    const [detailRes, problemRes]: any[] = await Promise.all([
      contestApi.getContestDetail(editContestId.value),
      contestApi.getContestProblems(editContestId.value)
    ])
    const contest = detailRes.data || detailRes
    const contestProblems = problemRes.data || problemRes || []

    form.value.title = contest.title || ''
    form.value.description = contest.description || ''
    form.value.type = contest.type || 'PRACTICE'
    form.value.startTime = parseApiDate(contest.startTime)
    form.value.endTime = parseApiDate(contest.endTime)
    form.value.durationMinutes = contest.durationMinutes || 120
    form.value.maxParticipants = contest.maxParticipants || 0
    form.value.password = contest.password || ''
    form.value.allowedLanguageList = contest.allowedLanguages
      ? String(contest.allowedLanguages).split(',').map((item: string) => item.trim()).filter(Boolean)
      : []
    publicSwitch.value = contest.isPublic !== 0
    editInviteCode.value = contest.inviteCode || ''
    allowPasteSwitch.value = contest.allowPaste !== 0
    unlimitedDuration.value = isOpenEndedTime(contest.endTime)

    existingContestProblemIds.value = contestProblems.map((item: any) => Number(item.problemId)).filter(Boolean)
    selectedProblems.value = contestProblems.map((item: any) => ({
      id: item.problemId,
      title: item.problemTitle || `题目#${item.problemId}`,
      difficulty: item.difficulty || '普通',
      score: item.score || 100
    }))
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载竞赛失败')
    router.push('/contests')
  }
}

const syncContestProblems = async (contestId: number) => {
  const oldIds = existingContestProblemIds.value
  if (oldIds.length > 0) {
    await contestApi.removeProblems(contestId, oldIds)
  }
  if (selectedProblems.value.length > 0) {
    const problems = selectedProblems.value.map((p, i) => ({
      problemId: p.id,
      sortOrder: i + 1,
      score: p.score || 100
    }))
    await contestApi.addProblems(contestId, problems)
  }
  existingContestProblemIds.value = selectedProblems.value.map(p => Number(p.id)).filter(Boolean)
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!unlimitedDuration.value && !form.value.endTime) {
    ElMessage.warning('请选择结束时间，或开启不限时长')
    return
  }
  if (!unlimitedDuration.value && form.value.endTime && form.value.startTime && form.value.endTime <= form.value.startTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  submitting.value = true
  try {
    const isExam = form.value.type === 'EXAM'
    const data: any = {
      title: form.value.title,
      description: form.value.description,
      type: form.value.type,
      startTime: form.value.startTime,
      endTime: unlimitedDuration.value ? new Date('2099-12-31T23:59:59') : form.value.endTime,
      durationMinutes: isExam ? form.value.durationMinutes : 0,
      maxParticipants: form.value.maxParticipants || 0,
      password: form.value.password || null,
      allowPaste: allowPasteSwitch.value ? 1 : 0,
      isPublic: publicSwitch.value ? 1 : 0,
      allowedLanguages: form.value.allowedLanguageList.length > 0
        ? form.value.allowedLanguageList.join(',')
        : null
    }

    if (isEditMode.value) {
      const contestId = editContestId.value
      await contestApi.updateContest(contestId, data)
      await syncContestProblems(contestId)
      ElMessage.success('竞赛已更新')
      router.push(`/contests/${contestId}`)
      return
    }

    const res: any = await contestApi.createContest(data)
    const contest = res.data || res
    const contestId = contest?.id
    if (contestId) {
      await syncContestProblems(contestId)
    }
    createdContest.value = contest
    ElMessage.success('竞赛创建成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 加载题目集列表
  try {
    const res: any = await problemSetApi.list({ size: 100 })
    const data = res.data || res
    problemSets.value = data.records || data || []
  } catch { /* 忽略竞赛详情加载失败 */ }

  try {
    const tagRes: any = await getAllTags()
    const tags = tagRes.data || tagRes || []
    allTags.value = tags.map((tag: any) => typeof tag === 'string' ? tag : tag.name).filter(Boolean)
  } catch { /* 忽略竞赛题目加载失败 */ }

  await applyContestToForm()
})
</script>

<style scoped>
.contest-create-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.contest-form {
  margin-top: 24px;
}

.form-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.contest-type-note {
  width: 100%;
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid #e6eef8;
  border-radius: 8px;
  background: #f8fbff;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

.contest-type-note strong {
  color: #303133;
}

.selected-problem-table {
  width: 100%;
}

.difficulty-blue {
  background-color: #e6f0fa !important;
  color: #1976d2 !important;
  border-color: #b3d8fd !important;
}

.problem-picker-filters {
  display: grid;
  grid-template-columns: 1.4fr 120px 140px 1.4fr;
  gap: 10px;
  margin-bottom: 12px;
}

.problem-picker-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.picker-tag {
  margin: 0 4px 4px 0;
}

@media (max-width: 768px) {
  .problem-picker-filters {
    grid-template-columns: 1fr;
  }
}

.invite-code-section {
  text-align: center;
  margin-top: 8px;
}

.invite-code-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 8px 0;
}

.edit-invite-code {
  justify-content: flex-start;
  flex-wrap: wrap;
  margin: 0;
}

.invite-code {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #409eff;
  font-family: 'Courier New', monospace;
}

.invite-hint {
  font-size: 13px;
  color: #909399;
}
</style>
