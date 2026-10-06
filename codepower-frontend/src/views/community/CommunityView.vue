<!-- 社区页面 — 题解浏览与发布、评论互动 -->
<template>
  <div class="community">
    <!-- 页面头部：展示社区定位、当前讨论数量和排序状态 -->
    <div class="community-header">
      <div class="community-kicker">CodePower Community</div>
      <h1>社区讨论</h1>
      <p class="subtitle">沉淀题解、复盘踩坑、分享学习经验，让每一次讨论都有后来的同学接得住。</p>
      <div class="community-stats">
        <span>{{ total || comments.length }} 条讨论</span>
        <span>{{ sortModeLabel }}</span>
      </div>
    </div>

    <div class="community-layout">
      <main class="community-main">
        <!-- 讨论筛选工具栏：关键词、排序、只看我的和手动刷新 -->
        <section class="community-toolbar">
          <el-input
            v-model="keyword"
            class="discussion-search"
            placeholder="搜索讨论内容"
            clearable
            @keyup.enter="applyFilters"
            @clear="applyFilters"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-radio-group v-model="sortMode" class="sort-tabs" @change="handleFilterChange">
            <el-radio-button label="latest">最新</el-radio-button>
            <el-radio-button label="hot">点赞优先</el-radio-button>
            <el-radio-button label="oldest">最早</el-radio-button>
          </el-radio-group>
          <el-checkbox v-model="mineOnly" class="mine-filter" @change="handleFilterChange">
            只看我的
          </el-checkbox>
          <el-button :icon="RefreshRight" @click="refreshComments">刷新</el-button>
        </section>

        <div class="post-section">
          <!-- 发帖编辑器：Quill 富文本支持代码块和图片 -->
          <el-card shadow="never" class="composer-card">
            <div class="composer-head">
              <div>
                <h3>发起一个讨论</h3>
                <p>可以贴代码、写题解思路，也可以记录一个刚踩过的坑。</p>
              </div>
            </div>
            <QuillEditor
              v-model:content="newComment"
              contentType="html"
              :options="editorOptions"
              style="min-height: 120px"
              ref="quillRef"
            />
            <div class="post-actions">
              <input ref="imageInput" type="file" accept="image/*" style="display: none" @change="handleImageUpload" />
              <el-button @click="insertImage" :icon="PictureFilled">插入图片</el-button>
              <el-button type="primary" @click="postComment" :loading="posting" :disabled="isContentEmpty">
                发布
              </el-button>
            </div>
          </el-card>
        </div>

        <!-- 讨论列表：顶层评论 + 子回复 + 点赞/删除/分页 -->
        <div class="comments-section" v-loading="loading">
      <el-empty v-if="comments.length === 0 && !loading" description="暂无讨论，来发表第一条吧" />

      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <el-card shadow="never" class="discussion-card">
          <div class="comment-header">
            <div class="comment-user">
              <el-avatar
                :size="40"
                :src="comment.avatarUrl || '/avatars/avatar-1.svg'"
                class="clickable-avatar"
                @click="goToUserProfile(comment.userId)"
              />
              <div class="user-info">
                <span class="username clickable-user" @click="goToUserProfile(comment.userId)">{{ comment.username }}</span>
                <span class="time">{{ formatTime(comment.createdAt) }}</span>
              </div>
            </div>
            <el-dropdown v-if="isOwner(comment)" trigger="click">
              <el-button text><el-icon><MoreFilled /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="deleteComment(comment.id)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="comment-content rich-content" v-html="comment.content"></div>
          <div class="comment-footer">
            <el-button text class="like-btn" :class="{ active: comment.liked }" :type="comment.liked ? 'primary' : ''" @click="toggleLike(comment)">
              <el-icon><Pointer /></el-icon>
              {{ comment.liked ? '已点赞' : '点赞' }} {{ comment.likesCount || 0 }}
            </el-button>
            <el-button text @click="startReply(comment)">
              <el-icon><ChatDotRound /></el-icon>
              回复 {{ comment.children?.length || 0 }}
            </el-button>
          </div>

          <!-- 回复区：默认展示前3条 -->
          <div v-if="comment.children && comment.children.length > 0" class="replies-section">
            <div v-for="reply in visibleReplies(comment)" :key="reply.id" class="reply-item">
              <div class="reply-header">
                <el-avatar
                  :size="28"
                  :src="reply.avatarUrl || '/avatars/avatar-1.svg'"
                  class="clickable-avatar"
                  @click="goToUserProfile(reply.userId)"
                />
                <span class="reply-username clickable-user" @click="goToUserProfile(reply.userId)">{{ reply.username }}</span>
                <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
              </div>
              <div class="reply-body">
                <div class="reply-content rich-content" v-html="reply.content"></div>
                <div class="reply-actions">
                  <el-button text size="small" class="like-btn mini" :class="{ active: reply.liked }" :type="reply.liked ? 'primary' : ''" @click="toggleLike(reply)">
                    <el-icon><Pointer /></el-icon> {{ reply.liked ? '已赞' : '点赞' }} {{ reply.likesCount || 0 }}
                  </el-button>
                  <el-button text size="small" @click="startReply(comment, reply)">回复</el-button>
                </div>
              </div>
            </div>
            <el-button v-if="comment.children.length > 3 && !comment.showAllReplies"
                       text type="primary" size="small" class="expand-btn"
                       @click="comment.showAllReplies = true">
              展开全部 {{ comment.children.length }} 条回复
            </el-button>
            <el-button v-if="comment.showAllReplies && comment.children.length > 3"
                       text type="info" size="small" class="expand-btn"
                       @click="comment.showAllReplies = false">
              收起
            </el-button>
          </div>

          <!-- 回复输入框 -->
          <div v-if="comment.showReplyInput" class="reply-input">
            <el-input
              v-model="comment.replyText"
              :placeholder="comment.replyPlaceholder || '写下你的回复...'"
              @keyup.enter="postReply(comment)"
            >
              <template #append>
                <el-button @click="postReply(comment)" :disabled="!comment.replyText?.trim()">回复</el-button>
              </template>
            </el-input>
          </div>
        </el-card>
      </div>

      <div class="pagination" v-if="total > 0">
        <el-pagination background layout="prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="currentPage" @current-change="handlePageChange" />
      </div>
        </div>
      </main>

      <aside class="community-sidebar">
        <!-- 侧边栏：提供快捷筛选和本页统计，便于答辩演示社区沉淀效果 -->
        <el-card shadow="never" class="side-card">
          <div class="side-card-title">讨论筛选</div>
          <div class="side-actions">
            <el-button :type="sortMode === 'latest' && !mineOnly ? 'primary' : 'default'" @click="quickFilter('latest')">
              最新讨论
            </el-button>
            <el-button :type="sortMode === 'hot' && !mineOnly ? 'primary' : 'default'" @click="quickFilter('hot')">
              点赞优先
            </el-button>
            <el-button :type="mineOnly ? 'primary' : 'default'" @click="quickFilter('mine')">
              我的讨论
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="side-card">
          <div class="side-card-title">本页概览</div>
          <div class="metric-row">
            <span>讨论</span>
            <strong>{{ comments.length }}</strong>
          </div>
          <div class="metric-row">
            <span>回复</span>
            <strong>{{ replyTotal }}</strong>
          </div>
          <div class="metric-row">
            <span>点赞</span>
            <strong>{{ pageLikes }}</strong>
          </div>
          <div v-if="topComment" class="top-topic">
            <span>当前热帖</span>
            <p>{{ plainText(topComment.content) }}</p>
          </div>
        </el-card>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Pointer, ChatDotRound, MoreFilled, PictureFilled, RefreshRight, Search } from '@element-plus/icons-vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { commentApi } from '@/api/comment'
import { formatRelativeTime } from '@/utils/datetime'

// 社区讨论页：负责评论列表、富文本发布、图片压缩上传、回复、点赞和删除。
const router = useRouter()

// 页面加载和发布状态。
const loading = ref(false)
const posting = ref(false)

// 顶层发帖编辑器、评论列表和分页状态。
const newComment = ref('')
const comments = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const quillRef = ref()
const imageInput = ref<HTMLInputElement>()

// 当前登录用户用于判断“我的讨论”和删除权限。
const currentUser = ref<string | null>(null)
const currentUserIdRef = ref<number | null>(null)

// 查询条件：关键词、排序方式和只看我的。
const keyword = ref('')
const sortMode = ref<'latest' | 'hot' | 'oldest'>('latest')
const mineOnly = ref(false)

// 当前排序文字，显示在页头统计区域。
const sortModeLabel = computed(() => {
  if (mineOnly.value) return '只看我的'
  if (sortMode.value === 'hot') return '点赞优先'
  if (sortMode.value === 'oldest') return '最早发布'
  return '最新发布'
})

// 本页回复总数，侧边栏用来快速展示互动规模。
const replyTotal = computed(() =>
  comments.value.reduce((sum, item) => sum + Number(item.children?.length || 0), 0)
)

// 本页点赞总数，包含顶层评论和回复。
const pageLikes = computed(() =>
  comments.value.reduce((sum, item) => {
    const replyLikes = (item.children || []).reduce((replySum: number, reply: any) => replySum + Number(reply.likesCount || 0), 0)
    return sum + Number(item.likesCount || 0) + replyLikes
  }, 0)
)

// 当前页点赞数最高的一条讨论，作为“热帖”摘要展示。
const topComment = computed(() => {
  if (!comments.value.length) return null
  return [...comments.value].sort((a, b) => Number(b.likesCount || 0) - Number(a.likesCount || 0))[0]
})

// Quill 编辑器配置：保留常用格式和代码块，避免工具栏过重。
const editorOptions = {
  placeholder: '分享你的想法... 支持代码块、格式排版',
  theme: 'snow',
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      ['code-block', 'blockquote'],
      [{ 'header': [1, 2, 3, false] }],
      [{ 'list': 'ordered' }, { 'list': 'bullet' }],
      ['link'],
      ['clean']
    ]
  }
}

// 富文本去掉标签后为空时，禁止发布。
const isContentEmpty = computed(() => {
  if (!newComment.value) return true
  return newComment.value.replace(/<[^>]*>/g, '').trim().length === 0
})

// 回复默认只展示前 3 条，避免讨论较多时页面被撑得过长。
const visibleReplies = (comment: any) => {
  if (!comment.children) return []
  return comment.showAllReplies ? comment.children : comment.children.slice(0, 3)
}

// 打开某条评论的回复输入框，若是回复子评论则带上 @ 用户提示。
const startReply = (comment: any, reply?: any) => {
  comment.showReplyInput = true
  comment.replyPlaceholder = reply ? `回复 @${reply.username}` : '写下你的回复...'
}

// 点击头像/昵称跳转个人主页，自己的主页走 /profile。
const goToUserProfile = (userId?: number) => {
  if (!userId) return
  router.push(userId === getCurrentUserId() ? '/profile' : `/users/${userId}`)
}

// 从 localStorage 读取当前用户 ID，供跳转和权限判断使用。
const getCurrentUserId = () => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    return userInfo ? Number(JSON.parse(userInfo).id) : null
  } catch {
    return null
  }
}

// 给富文本中的代码块注入“复制”按钮，解决 v-html 内容无法直接绑定 Vue 事件的问题。
const injectCopyButtons = () => {
  nextTick(() => {
    document.querySelectorAll('.rich-content pre').forEach(pre => {
      if (pre.querySelector('.copy-btn')) return
      const btn = document.createElement('button')
      btn.className = 'copy-btn'
      btn.textContent = '复制'
      btn.onclick = () => {
        const code = pre.querySelector('code')?.textContent || pre.textContent || ''
        navigator.clipboard.writeText(code).then(() => {
          btn.textContent = '已复制'
          setTimeout(() => { btn.textContent = '复制' }, 1500)
        })
      }
      ;(pre as HTMLElement).style.position = 'relative'
      pre.appendChild(btn)
    })
  })
}

// 图片进入富文本前先用 canvas 压缩，避免 base64 图片过大导致评论内容膨胀。
const compressImage = (file: File, maxWidth = 1200, maxSize = 500 * 1024): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (file.size <= maxSize) {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result as string)
      reader.onerror = reject
      reader.readAsDataURL(file)
      return
    }
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      const canvas = document.createElement('canvas')
      let w = img.width, h = img.height
      if (w > maxWidth) { h = Math.round(h * maxWidth / w); w = maxWidth }
      canvas.width = w; canvas.height = h
      const ctx = canvas.getContext('2d')!
      ctx.drawImage(img, 0, 0, w, h)
      let quality = 0.8
      let result = canvas.toDataURL('image/jpeg', quality)
      while (result.length > maxSize * 1.37 && quality > 0.3) {
        quality -= 0.1
        result = canvas.toDataURL('image/jpeg', quality)
      }
      resolve(result)
    }
    img.onerror = reject
    img.src = url
  })
}

// 触发隐藏的文件选择框，让用户插入本地图片。
const insertImage = () => { imageInput.value?.click() }

// 校验图片类型和大小，压缩后插入 Quill 当前光标位置。
const handleImageUpload = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('图片不能超过5MB'); return }
  try {
    const dataUrl = await compressImage(file)
    const quill = quillRef.value?.getQuill()
    if (quill) {
      const range = quill.getSelection(true)
      quill.insertEmbed(range.index, 'image', dataUrl)
      quill.setSelection(range.index + 1)
      newComment.value = quill.root.innerHTML
    }
  } catch {
    ElMessage.error('图片处理失败')
  }
  if (imageInput.value) imageInput.value.value = ''
}

onMounted(() => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      const parsed = JSON.parse(userInfo)
      currentUser.value = parsed.username
      currentUserIdRef.value = Number(parsed.id) || null
    }
  } catch {}
  loadComments()
})

// 调用评论 API 加载社区讨论，并把后端返回的 children/liked/likesCount 规范成页面需要的结构。
const loadComments = async () => {
  loading.value = true
  try {
    const res: any = await commentApi.getComments('COMMUNITY', 0, {
      page: currentPage.value,
      size: pageSize.value,
      sort: sortMode.value,
      keyword: keyword.value.trim() || undefined,
      mineOnly: mineOnly.value
    })
    const data = res.data || res
    comments.value = (data.records || []).map((c: any) => ({
      ...c,
      showAllReplies: false,
      showReplyInput: false,
      replyText: '',
      replyPlaceholder: '',
      liked: Boolean(c.liked),
      likesCount: Number(c.likesCount || 0),
      children: (c.children || []).map((r: any) => ({
        ...r,
        liked: Boolean(r.liked),
        likesCount: Number(r.likesCount || 0)
      }))
    }))
    total.value = data.total || 0
    injectCopyButtons()
  } catch (e) {
    console.error('加载评论失败', e)
  } finally { loading.value = false }
}

// 应用筛选条件时回到第一页，避免上一页页码在新条件下越界。
const applyFilters = () => {
  currentPage.value = 1
  loadComments()
}

// Element Plus 筛选控件 change 回调统一走 applyFilters。
const handleFilterChange = () => {
  applyFilters()
}

// 手动刷新当前筛选条件下的讨论列表。
const refreshComments = () => {
  loadComments()
}

// 侧边栏快捷筛选：最新、热门、我的讨论。
const quickFilter = (mode: 'latest' | 'hot' | 'mine') => {
  if (mode === 'mine') {
    mineOnly.value = !mineOnly.value
    if (mineOnly.value) sortMode.value = 'latest'
  } else {
    sortMode.value = mode
    mineOnly.value = false
  }
  applyFilters()
}

// 发布顶层讨论，后端保存成功后清空 Quill 并刷新第一页。
const postComment = async () => {
  if (isContentEmpty.value) return
  posting.value = true
  try {
    await commentApi.addComment({ targetType: 'COMMUNITY', targetId: 0, content: newComment.value })
    newComment.value = ''
    const quill = quillRef.value?.getQuill()
    if (quill) quill.setContents([])
    ElMessage.success('发布成功')
    currentPage.value = 1
    await loadComments()
  } catch {
    ElMessage.error('发布失败')
  } finally { posting.value = false }
}

// 点赞/取消点赞：优先使用后端返回的最新点赞数，接口没返回时前端做一次兜底修正。
const toggleLike = async (comment: any) => {
  try {
    const wasLiked = Boolean(comment.liked)
    const res: any = await commentApi.toggleLike(comment.id)
    const data = res.data || res
    const nextLiked = Boolean(data.liked)
    comment.liked = nextLiked
    if (typeof data.likesCount === 'number') {
      comment.likesCount = data.likesCount
    } else if (wasLiked !== nextLiked) {
      comment.likesCount = nextLiked ? Number(comment.likesCount || 0) + 1 : Math.max(0, Number(comment.likesCount || 0) - 1)
    }
  } catch {
    ElMessage.error('点赞失败，请稍后重试')
  }
}

// 回复某条顶层评论，回复成功后重新加载以保证子回复顺序和点赞状态一致。
const postReply = async (comment: any) => {
  if (!comment.replyText?.trim()) return
  try {
    await commentApi.addComment({
      targetType: 'COMMUNITY', targetId: 0,
      parentId: comment.id, content: comment.replyText.trim()
    })
    comment.replyText = ''
    comment.showReplyInput = false
    await loadComments()
  } catch { ElMessage.error('回复失败') }
}

// 删除自己的评论或回复，删除后刷新列表。
const deleteComment = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除这条评论？', '提示', { type: 'warning' })
    await commentApi.deleteComment(id)
    ElMessage.success('已删除')
    await loadComments()
  } catch {}
}

// 判断当前登录用户是否是评论作者，用于展示删除菜单。
const isOwner = (comment: any) => {
  if (currentUserIdRef.value && Number(comment.userId) === currentUserIdRef.value) return true
  return currentUser.value && comment.username === currentUser.value
}

// 分页切换后加载对应页讨论。
const handlePageChange = (page: number) => { currentPage.value = page; loadComments() }

// 后端时间统一转成“几分钟前”这类相对时间。
const formatTime = (time: string) => {
  return formatRelativeTime(time)
}

// 富文本摘要：去掉 HTML 标签和实体，只保留侧边栏热帖需要的一小段纯文本。
const plainText = (html: string) => {
  return (html || '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 76) || '暂无正文摘要'
}
</script>

<style scoped>
.community {
  max-width: 1360px;
  margin: 0 auto;
  padding: 30px 24px 44px;
}

.community-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  align-items: start;
  gap: 22px;
}

.community-main {
  min-width: 0;
}

.community-sidebar {
  position: sticky;
  top: 72px;
  display: grid;
  gap: 16px;
}

.community-header {
  position: relative;
  overflow: hidden;
  margin-bottom: 24px;
  padding: 28px 34px;
  border: 1px solid #dcecff;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8fbff 0%, #ffffff 58%, #eef7ff 100%);
  box-shadow: 0 12px 28px rgba(31, 45, 61, 0.08);
}

.community-header::after {
  display: none;
}

.community-kicker {
  position: relative;
  z-index: 1;
  display: inline-flex;
  padding: 5px 12px;
  border-radius: 999px;
  background: #ecf5ff;
  color: #2c78d4;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.community-header h1 {
  position: relative;
  z-index: 1;
  margin: 12px 0 8px;
  color: #1f2d3d;
  font-size: 34px;
  line-height: 1.15;
  letter-spacing: -0.03em;
}

.subtitle {
  position: relative;
  z-index: 1;
  max-width: 660px;
  margin: 0;
  color: #637587;
  font-size: 15px;
  line-height: 1.8;
}

.community-stats {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.community-stats span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 5px 12px;
  border: 1px solid #dcecff;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: #456175;
  font-size: 13px;
  font-weight: 700;
}

.post-section {
  margin-bottom: 22px;
}

.community-toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto auto auto;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid #e6eef8;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(31, 45, 61, 0.06);
}

.discussion-search {
  min-width: 0;
}

.sort-tabs {
  white-space: nowrap;
}

.mine-filter {
  margin-right: 2px;
  white-space: nowrap;
}

.composer-card,
.discussion-card {
  border: 1px solid #e6eef8;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(31, 45, 61, 0.06);
}

.composer-card :deep(.el-card__body),
.discussion-card :deep(.el-card__body) {
  padding: 22px;
}

.composer-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.composer-head h3 {
  margin: 0;
  color: #1f2d3d;
  font-size: 20px;
}

.composer-head p {
  margin: 6px 0 0;
  color: #7a8b9a;
  font-size: 13px;
}

.post-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 14px;
  gap: 10px;
}

.comments-section {
  min-height: 180px;
}

.comment-item {
  margin-bottom: 18px;
}

.discussion-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.discussion-card:hover {
  border-color: #cfe5ff;
  box-shadow: 0 12px 30px rgba(64, 158, 255, 0.09);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.comment-user {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 3px;
}

.username {
  color: #1f2d3d;
  font-weight: 800;
}

.time {
  color: #9aa9b5;
  font-size: 12px;
}

.comment-content {
  margin-bottom: 14px;
  color: #2f3f4f;
  line-height: 1.78;
  word-break: break-word;
}

.comment-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 2px;
}

.like-btn {
  padding: 7px 12px;
  border-radius: 999px;
  color: #637587;
  font-weight: 700;
}

.like-btn:hover {
  background: #f0f7ff;
  color: #409eff;
}

.like-btn.active {
  background: linear-gradient(135deg, #ecf5ff 0%, #dcecff 100%);
  color: #1677d2;
  box-shadow: inset 0 0 0 1px #b9dcff;
}

.like-btn.mini {
  padding: 4px 9px;
  font-size: 12px;
}

.replies-section {
  margin-top: 16px;
  padding: 14px 14px 8px;
  border: 1px solid #eef3f8;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fbff 100%);
}

.reply-item {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #eef3f8;
}

.reply-item:last-of-type {
  border-bottom: none;
}

.reply-header {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 7px;
}

.reply-username {
  overflow: hidden;
  color: #455a6f;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-time {
  flex: none;
  color: #a6b3bf;
  font-size: 12px;
}

.reply-body {
  min-width: 0;
}

.reply-content {
  color: #506273;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
}

.reply-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.expand-btn {
  margin-top: 6px;
}

.reply-input {
  margin-top: 14px;
}

.side-card {
  border: 1px solid #e6eef8;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(31, 45, 61, 0.06);
}

.side-card :deep(.el-card__body) {
  padding: 16px;
}

.side-card-title {
  margin-bottom: 12px;
  color: #1f2d3d;
  font-size: 15px;
  font-weight: 800;
}

.side-actions {
  display: grid;
  gap: 10px;
}

.side-actions .el-button {
  justify-content: flex-start;
  width: 100%;
  margin-left: 0;
}

.metric-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #eef3f8;
  color: #637587;
  font-size: 13px;
}

.metric-row strong {
  color: #1f2d3d;
  font-size: 18px;
}

.top-topic {
  margin-top: 14px;
  padding: 12px;
  border-radius: 8px;
  background: #f7fbff;
}

.top-topic span {
  color: #637587;
  font-size: 12px;
  font-weight: 700;
}

.top-topic p {
  margin: 7px 0 0;
  color: #2f3f4f;
  font-size: 13px;
  line-height: 1.55;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.clickable-avatar {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.clickable-avatar:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.22);
}

.clickable-user {
  cursor: pointer;
}

.clickable-user:hover {
  color: #409eff;
}

@media (max-width: 1120px) {
  .community-layout {
    grid-template-columns: 1fr;
  }

  .community-sidebar {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .community-toolbar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .community {
    padding: 22px 12px 34px;
  }

  .community-header {
    padding: 26px 22px;
    border-radius: 22px;
  }

  .community-header h1 {
    font-size: 28px;
  }

  .reply-item {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .community-toolbar,
  .community-sidebar {
    grid-template-columns: 1fr;
  }

  .post-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

<style>
.rich-content img { max-width: 100%; border-radius: 8px; margin: 8px 0; }
.rich-content pre {
  background: #f5f7fa; border-radius: 6px; padding: 12px 16px; overflow-x: auto;
  margin: 8px 0; font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; line-height: 1.5;
  position: relative;
}
.rich-content pre .copy-btn {
  position: absolute; top: 6px; right: 6px; padding: 2px 10px; font-size: 12px;
  background: #e4e7ed; border: none; border-radius: 4px; cursor: pointer; color: #606266;
  opacity: 0; transition: opacity 0.2s;
}
.rich-content pre:hover .copy-btn { opacity: 1; }
.rich-content pre .copy-btn:hover { background: #409eff; color: #fff; }
.rich-content code { background: #f0f2f5; padding: 2px 6px; border-radius: 3px; font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; }
.rich-content pre code { background: none; padding: 0; }
.rich-content blockquote { border-left: 4px solid #409EFF; padding: 8px 16px; margin: 8px 0; color: #666; background: #f9f9f9; }
.rich-content h1, .rich-content h2, .rich-content h3 { margin: 12px 0 8px; }
.ql-toolbar.ql-snow { border-radius: 6px 6px 0 0; border-color: #dcdfe6; }
.ql-container.ql-snow { border-radius: 0 0 6px 6px; border-color: #dcdfe6; }
</style>
