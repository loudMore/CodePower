<!-- 在线做题 IDE — 代码编辑、评测、题解查看 -->
<template>
  <div
    class="problem-ide-container"
    :class="{
      'contest-mode': isContestMode,
      'contest-header-compact': contestHeaderCompact,
      'contest-sidebar-collapsed': contestSidebarCollapsed
    }"
  >
    <div v-if="isContestMode" class="contest-workspace-header">
      <div class="contest-workspace-main">
        <div class="contest-workspace-summary">
          <div class="contest-workspace-title-row">
            <el-button class="contest-back-btn contest-header-pill" @click="router.push(`/contests/${contestId}`)">← 返回竞赛</el-button>
            <div class="contest-workspace-title">{{ contestInfo?.title || '竞赛考试' }}</div>
            <el-tag :type="contestStatusTagType" effect="light">{{ contestStatusText }}</el-tag>
          </div>
          <div class="contest-workspace-meta">
            <span>当前题目：{{ problem.title || '-' }}</span>
            <span>当前得分：{{ contestCurrentScore }}</span>
            <span>已通过：{{ contestAcceptedCount }}/{{ contestProblems.length }}</span>
            <span v-if="contestCountdownText">{{ contestCountdownLabel }}：{{ contestCountdownText }}</span>
          </div>
        </div>
        <div class="contest-top-metrics">
          <div class="contest-top-metric">
            <span>名次</span>
            <strong>{{ contestCurrentRank ? `#${contestCurrentRank}` : '未上榜' }}</strong>
          </div>
          <div class="contest-top-metric">
            <span>进度</span>
            <strong>{{ contestAcceptedCount }}/{{ contestProblems.length }}</strong>
          </div>
          <div class="contest-top-metric">
            <span>得分</span>
            <strong>{{ contestCurrentScore }}</strong>
          </div>
        </div>
        <div class="contest-workspace-actions">
          <el-button class="contest-header-pill" size="small" plain @click="contestHeaderCompact = !contestHeaderCompact">
            {{ contestHeaderCompact ? '展开蓝条' : '收起蓝条' }}
          </el-button>
          <el-button class="contest-header-pill" size="small" plain @click="contestSidebarCollapsed = !contestSidebarCollapsed">
            {{ contestSidebarCollapsed ? '展开题栏' : '收起题栏' }}
          </el-button>
        </div>
      </div>
    </div>
    <aside v-if="isContestMode" class="contest-nav-card" :class="{ collapsed: contestSidebarCollapsed }">
      <div class="contest-side-tabs">
        <button
          v-for="tab in contestWorkspaceTabs"
          :key="tab.name"
          type="button"
          class="contest-side-tab"
          :class="{ active: contestWorkspaceTab === tab.name }"
          @click="contestWorkspaceTab = tab.name"
        >
          <span class="contest-side-tab-icon">{{ tab.icon }}</span>
          <span class="contest-side-tab-label">{{ tab.label }}</span>
        </button>
        <div class="contest-quick-nav" v-if="contestProblems.length > 1">
          <button
            type="button"
            class="contest-quick-nav-btn"
            :disabled="!contestPrevProblem"
            :title="contestPrevProblem ? `上一题：${contestPrevProblem.problemTitle || '题目'}` : '已经是第一题'"
            @click="goToAdjacentContestProblem(-1)"
          >
            ↑
          </button>
          <button
            type="button"
            class="contest-quick-nav-btn"
            :disabled="!contestNextProblem"
            :title="contestNextProblem ? `下一题：${contestNextProblem.problemTitle || '题目'}` : '已经是最后一题'"
            @click="goToAdjacentContestProblem(1)"
          >
            ↓
          </button>
        </div>
        <button
          type="button"
          class="contest-sidebar-toggle"
          :title="contestSidebarCollapsed ? '展开题栏' : '收起题栏'"
          @click="contestSidebarCollapsed = !contestSidebarCollapsed"
        >
          <span class="contest-side-tab-icon">{{ contestSidebarCollapsed ? '>' : '<' }}</span>
        </button>
      </div>
      <div v-show="!contestSidebarCollapsed" class="contest-side-panel">
        <div class="contest-side-head">
          <strong>题目</strong>
          <span>{{ contestAcceptedCount }}/{{ contestProblems.length }}</span>
        </div>
        <div class="contest-number-grid">
          <button
            v-for="(item, index) in contestProblems"
            :key="item.problemId"
            class="contest-number-cell"
            :class="getContestProblemButtonClass(item.problemId)"
            :title="item.problemTitle || `题目 ${index + 1}`"
            @click="goToContestProblem(item.problemId)"
          >
            <span>{{ index + 1 }}</span>
          </button>
        </div>
        <div class="contest-side-legend">
          <span><i class="legend-dot pending"></i>评测中</span>
          <span><i class="legend-dot accepted"></i>通过</span>
          <span><i class="legend-dot attempted"></i>尝试</span>
          <span><i class="legend-dot active"></i>当前</span>
        </div>
        <div class="contest-side-current" v-if="contestWorkspaceTab === 'answer'">
          <div class="contest-side-current-label">当前题目</div>
          <strong>{{ problem.title || '-' }}</strong>
          <span>{{ contestCurrentScore }} 分 · {{ contestStatusText }}</span>
        </div>
      </div>
    </aside>
    <div v-show="!isContestMode || contestWorkspaceTab === 'answer'" class="problem-layout">
      <!-- 左侧题目描述区域 -->
      <div class="problem-card">
        <el-card class="problem-description" shadow="never">
          <template #header>
            <div class="problem-header">
              <div class="problem-title">
                <h2>{{ problem.id }}. {{ problem.title }}</h2>
                <div class="problem-author" v-if="problem.authorName || problem.authorId">
                  <span>出题人</span>
                  <button class="author-link" @click="goToAuthorProfile">
                    {{ problem.authorName || `用户 #${problem.authorId}` }}
                  </button>
                </div>
                <div class="problem-tags">
                  <el-tag :style="{transition: 'none'}" :type="getDifficultyType(problem.difficulty)" size="small">
                    {{ problem.difficulty }}
                  </el-tag>
                  <el-tag
                    v-for="tag in problem.tags"
                    :key="tag.id"
                    size="small"
                    effect="plain"
                    :style="{transition: 'none'}"
                    class="category-tag"
                  >
                    {{ tag.name }}
                  </el-tag>
                </div>
              </div>
              <div class="problem-stats">
                <el-tooltip v-if="!isContestMode" :content="isFavorited ? '取消收藏' : '收藏题目'" placement="top">
                  <el-button class="problem-action-btn" :type="isFavorited ? 'warning' : ''" text circle @click="toggleFavorite">
                    <el-icon :size="18"><StarFilled v-if="isFavorited" /><Star v-else /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip v-if="!isContestMode" content="反馈题目问题" placement="top">
                  <el-button class="problem-action-btn" text circle @click="showReportDialog = true">
                    <el-icon :size="18"><WarningFilled /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="通过率" placement="top">
                  <div class="stat-item">
                    <span class="stat-icon success"><el-icon><CircleCheckFilled /></el-icon></span>
                    <span>{{ problem.acceptRate != null ? problem.acceptRate + '%' : '--' }}</span>
                  </div>
                </el-tooltip>
                <el-tooltip content="提交次数" placement="top">
                  <div class="stat-item">
                    <span class="stat-icon"><el-icon><Document /></el-icon></span>
                    <span>{{ problem.submitCount ?? '--' }}</span>
                  </div>
                </el-tooltip>
                <el-tooltip content="内存限制" placement="top">
                  <div class="stat-item">
                    <span class="stat-icon text">存</span>
                    <span>{{ problem.memoryLimit ? (problem.memoryLimit / 1024).toFixed(0) + ' MB' : '--' }}</span>
                  </div>
                </el-tooltip>
                <el-tooltip content="时间限制" placement="top">
                  <div class="stat-item">
                    <span class="stat-icon text">时</span>
                    <span>{{ problem.timeLimit ? problem.timeLimit + ' ms' : '--' }}</span>
                  </div>
                </el-tooltip>
              </div>
            </div>
          </template>
          
          <el-tabs v-model="activeDescriptionTab" class="problem-tabs">
            <el-tab-pane label="题目描述" name="description">
              <div class="problem-content problem-rich-content">
                <div class="problem-section problem-description-block">
                  <div class="section-title">题目描述</div>
                  <div class="section-content" v-html="problem.description"></div>
                </div>
                <div class="problem-section problem-format-block">
                  <div class="section-title">输入格式</div>
                  <div class="section-content" v-html="problem.inputFormat"></div>
                </div>
                <div class="problem-section problem-format-block">
                  <div class="section-title">输出格式</div>
                  <div class="section-content" v-html="problem.outputFormat"></div>
                </div>
                <div class="problem-section problem-limits-block">
                  <div class="section-title">限制条件</div>
                  <div class="section-content">
                    <ul>
                      <li>时间限制：{{ problem.timeLimit || '--' }} ms</li>
                      <li>内存限制：{{ problem.memoryLimit ? (problem.memoryLimit / 1024).toFixed(0) + ' MB' : '--' }}</li>
                    </ul>
                  </div>
                </div>
                <div class="problem-section problem-example-block">
                  <div class="section-title">示例</div>
                  <div
                    v-for="sample in displayExamples"
                    :key="sample.key"
                    class="example-group"
                  >
                    <div class="example-title">{{ sample.label }}</div>
                    <div class="example-label">输入</div>
                    <pre class="example-pre">{{ sample.input }}</pre>
                    <div class="example-label">输出</div>
                    <pre class="example-pre">{{ sample.expected }}</pre>
                    <template v-if="sample.explanation">
                      <div class="example-label">解释</div>
                      <div class="example-explain">{{ sample.explanation }}</div>
                    </template>
                  </div>
                  <el-empty v-if="displayExamples.length === 0" description="暂无公开样例" />
                </div>
                <div class="problem-section problem-hint-block" v-if="problem.hint">
                  <div class="section-title">提示</div>
                  <div class="section-content" v-html="problem.hint"></div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane v-if="!isContestMode" label="题解" name="solution">
              <div class="solution-tabs">
                <el-tabs type="border-card">
                  <!-- 官方题解 -->
                  <el-tab-pane v-if="!isContestMode" label="官方题解">
                    <div v-if="officialSolutions.length === 0" class="solution-empty">
                      <el-empty description="暂无官方题解" />
                    </div>
                    <div class="solution-content" v-else-if="showSolution">
                      <el-card v-for="solution in officialSolutions" :key="solution.id" class="solution-card" shadow="never">
                        <template #header>
                          <div class="solution-header">
                            <div class="solution-title">{{ solution.title || `${getLanguageLabel(solution.language)} 官方题解` }}</div>
                            <el-tag size="small" type="success">{{ getLanguageLabel(solution.language) }}</el-tag>
                          </div>
                        </template>
                        <div class="solution-thinking" v-if="solution.description">
                          <h3>解题思路</h3>
                          <div class="solution-body markdown-body" v-html="renderRichContent(solution.description)"></div>
                        </div>
                        <div class="solution-code" v-if="solution.code">
                          <h3>参考代码</h3>
                          <pre class="code-preview">{{ solution.code }}</pre>
                        </div>
                      </el-card>
                      <div v-if="officialSolutions.every(item => !item.description && !item.code)" class="solution-empty">
                        <el-empty description="暂无官方题解内容" />
                      </div>
                    </div>
                    <div v-else class="solution-locked">
                      <el-empty description="题解需要解锁">
                        <el-button type="primary" @click="unlockSolution">
                          解锁题解
                        </el-button>
                      </el-empty>
                    </div>
                  </el-tab-pane>
                  
                  <!-- 社区题解 -->
                  <el-tab-pane v-if="!isContestMode" label="社区讨论">
                    <div class="community-solutions" v-if="communitySolutions.length > 0">
                      <el-card v-for="solution in communitySolutions" :key="solution.id" class="solution-card community-solution-post" shadow="hover">
                        <div class="solution-post-header">
                          <div class="solution-post-author" @click="goToUserProfile(solution.userId)">
                            <el-avatar :size="36" :src="solution.avatarUrl || '/avatars/avatar-1.svg'" class="clickable-user-avatar">
                              {{ getSolutionAuthorName(solution).slice(0, 1) }}
                            </el-avatar>
                            <div>
                              <div class="solution-author-name">{{ getSolutionAuthorName(solution) }}</div>
                              <div class="solution-date">{{ formatSubmissionTime(solution.createdAt) }}</div>
                            </div>
                          </div>
                          <el-tag size="small" effect="plain">{{ getLanguageLabel(solution.language) }}</el-tag>
                        </div>
                        <h3 class="solution-post-title">{{ solution.title }}</h3>
                        <div class="solution-body markdown-body" v-html="renderRichContent(solution.description || '')"></div>
                        <div class="solution-code" v-if="solution.code">
                          <h3>参考代码</h3>
                          <div class="code-block-wrapper">
                            <el-button class="copy-code-btn" size="small" @click="copyText(solution.code, '题解代码已复制')">
                              复制代码
                            </el-button>
                            <pre class="code-preview">{{ solution.code }}</pre>
                          </div>
                        </div>
                      </el-card>
                    </div>
                    <el-empty v-else description="暂无社区题解" />
                    
                    <div class="add-solution">
                      <el-button type="primary" @click="openSolutionEditor">分享我的解法</el-button>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </el-tab-pane>
            <el-tab-pane label="提交记录" name="submissions">
              <el-table :data="userSubmissions" v-loading="submissionsLoading" class="submission-table" style="width: 100%">
                <el-table-column label="提交时间" min-width="130">
                  <template #default="scope">
                    {{ formatSubmissionTime(scope.row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" min-width="92">
                  <template #default="scope">
                    <el-tag :type="getSubmissionStatusType(scope.row.status)">
                      {{ formatStatus(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="语言" min-width="120" show-overflow-tooltip>
                  <template #default="scope">
                    {{ getLanguageLabel(scope.row.language) }}
                  </template>
                </el-table-column>
                <el-table-column label="运行时间" min-width="88">
                  <template #default="scope">
                    {{ scope.row.executionTime != null ? scope.row.executionTime + ' ms' : '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="内存" min-width="88">
                  <template #default="scope">
                    {{ scope.row.memoryUsed != null ? (scope.row.memoryUsed / 1024).toFixed(1) + ' MB' : '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="score" label="得分" min-width="64" />
                <el-table-column label="操作" width="70">
                  <template #default="scope">
                    <el-button type="primary" link @click="viewSubmissionDetail(scope.row)">
                      详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
      
      <!-- 右侧代码编辑区域 -->
      <div class="code-card">
        <el-card shadow="never" class="code-editor-card">
          <template #header>
            <div class="editor-header">
              <div class="language-select">
                <span>编程语言:</span>
                <el-select v-model="selectedLanguage" size="small" style="width: 260px">
                  <el-option v-for="lang in languages" :key="lang.value" :label="lang.label" :value="lang.value" />
                </el-select>
              </div>
              <div class="editor-controls">
                <el-button type="default" @click="openSettingsMenu" plain size="small">
                  设置
                </el-button>
              </div>
            </div>
          </template>
          
          <div class="code-editor">
            <!-- 真实ACE编辑器 -->
            <div id="editor" class="ace-editor-container"></div>
          </div>
          
          <!-- 按钮区域 -->
          <div class="editor-actions">
            <el-button-group>
              <el-button type="primary" @click="openTestDialog" :loading="testRunning">
                <el-icon><CaretRight /></el-icon> 运行测试
              </el-button>
              <el-button type="success" @click="runCode">
                <el-icon><Check /></el-icon> 提交代码
              </el-button>
              <el-button v-if="!isContestMode" type="info" @click="openAiDrawer">
                <el-icon><ChatDotRound /></el-icon> AI求助
              </el-button>
            </el-button-group>
          </div>

          <!-- AI助手抽屉 -->
          <el-drawer
            v-if="!isContestMode"
            v-model="showAiDrawer"
            title="AI求助"
            size="min(680px, 96vw)"
            :with-header="true"
            direction="rtl"
            class="ai-drawer"
          >
            <template #header>
              <div class="ai-drawer-header">
                <div class="ai-drawer-brand">
                  <div class="ai-brand-mark">
                    <svg class="ai-brand-svg" viewBox="0 0 32 32" aria-hidden="true">
                      <rect x="5" y="7" width="22" height="17" rx="7" />
                      <path d="M11 15.5h10M12.5 20h7M12 7V4.5M20 7V4.5" />
                      <circle cx="12.5" cy="14" r="1.2" />
                      <circle cx="19.5" cy="14" r="1.2" />
                    </svg>
                  </div>
                  <div class="ai-drawer-title-group">
                    <div class="ai-drawer-kicker">AI求助</div>
                    <div class="ai-drawer-subtitle">结合题目、样例和当前代码给提示</div>
                  </div>
                </div>
                <div class="ai-drawer-meta">
                  <el-tag v-if="aiQuotaInfo" :type="aiTotalAvailable > 0 ? 'success' : 'danger'" size="small" effect="plain" round>
                    可用 {{ aiTotalAvailable }} · 今日 {{ aiQuotaInfo.remaining }}/{{ aiQuotaInfo.quota }}<template v-if="aiQuotaInfo.bonus"> · 累积 {{ aiQuotaInfo.bonus }}</template>
                  </el-tag>
                </div>
              </div>
            </template>
            <div class="ai-assistant-container">
              <div class="ai-session-card">
                <div class="ai-session-context">
                  <div class="ai-context-main">
                    <span class="context-ready-dot"></span>
                    <span class="ai-context-title">{{ problem.id }}. {{ problem.title || '当前题目' }}</span>
                  </div>
                  <div class="ai-context-meta">
                    <span>{{ getLanguageLabel(selectedLanguage) }}</span>
                    <span>{{ aiCodeStats.hasCode ? `${aiCodeStats.lines} 行代码` : '尚未写有效代码' }}</span>
                  </div>
                </div>
                <div class="ai-session-row">
                  <div class="ai-session-label">推理模型</div>
                  <el-select v-model="selectedModel" placeholder="选择模型" size="small" class="ai-model-select" @change="aiModelTouched = true">
                    <el-option v-for="m in aiModels" :key="m.id" :value="m.id">
                      <span class="model-option-name">{{ m.name }}</span>
                      <span class="model-option-cost">×{{ m.cost }}</span>
                    </el-option>
                  </el-select>
                  <span v-if="selectedModelCost" class="ai-cost-hint">本次约 ×{{ selectedModelCostText }}</span>
                </div>
                <div class="ai-context-strip">
                  <div class="context-chip strong">当前题目</div>
                  <div class="context-chip">{{ getLanguageLabel(selectedLanguage) }}</div>
                  <div class="context-chip">{{ aiCodeStats.hasCode ? `${aiCodeStats.lines} 行代码` : '代码仍是模板' }}</div>
                  <div class="context-chip soft">公开样例手算</div>
                  <div class="context-chip soft">结合当前代码</div>
                </div>
              </div>

              <div class="ai-messages" ref="aiMessagesRef">
                <transition-group name="ai-msg-fade">
                <div v-for="(message, index) in aiMessages" :key="index" :class="['ai-message', message.role]">
                    <div class="msg-avatar">
                      <span v-if="message.role === 'user'">我</span>
                      <svg v-else-if="message.role === 'assistant'" class="ai-avatar-svg" viewBox="0 0 32 32" aria-hidden="true">
                        <rect x="5" y="8" width="22" height="16" rx="7" />
                        <path d="M11 16h10M13 20h6M12 8V5M20 8V5" />
                        <circle cx="12.5" cy="14" r="1.2" />
                        <circle cx="19.5" cy="14" r="1.2" />
                      </svg>
                      <span v-else>CP</span>
                    </div>
                    <div class="msg-bubble">
                      <div v-if="message.role === 'assistant'" class="message-content markdown-body">
                        <div v-if="message.content" v-html="renderMarkdown(message.content)"></div>
                        <div v-else-if="aiLoading && index === aiMessages.length - 1" class="ai-stream-placeholder">
                          <span>{{ aiStreamStatus || (aiStreaming ? '正在流式输出' : '等待模型响应') }}</span>
                          <span class="stream-cursor">▍</span>
                        </div>
                      </div>
                      <div v-else class="message-content">{{ message.content }}</div>
                    </div>
                  </div>
                </transition-group>
                <div v-if="aiLoading && !aiStreaming && aiMessages[aiMessages.length - 1]?.role !== 'assistant'" class="ai-typing-indicator">
                  <div class="msg-avatar">
                    <svg class="ai-avatar-svg" viewBox="0 0 32 32" aria-hidden="true">
                      <rect x="5" y="8" width="22" height="16" rx="7" />
                      <path d="M11 16h10M13 20h6M12 8V5M20 8V5" />
                      <circle cx="12.5" cy="14" r="1.2" />
                      <circle cx="19.5" cy="14" r="1.2" />
                    </svg>
                  </div>
                  <div class="msg-bubble typing-bubble">
                    <span class="typing-copy">{{ aiStreamStatus || '正在等待模型首段内容' }}</span>
                    <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                  </div>
                </div>
              </div>

              <div class="ai-suggestions">
                <div class="suggestions-head">
                  <div class="suggestions-title">快速提问</div>
                </div>
                <div class="suggestions-grid">
                  <button
                    v-for="(suggestion, index) in dynamicAiSuggestions"
                    :key="index"
                    class="suggestion-chip"
                    :disabled="aiLoading"
                    @click="useAiSuggestion(suggestion)"
                  >
                    {{ suggestion }}
                  </button>
                </div>
              </div>

              <div class="ai-input-bar">
                <el-input
                  v-model="aiQuestion"
                  placeholder="像问老师一样描述卡点，例如：按样例跑我当前代码，找最先出错的位置"
                  :disabled="aiLoading"
                  @keydown.enter.exact.prevent="askAi"
                  :autosize="{ minRows: 1, maxRows: 3 }"
                  type="textarea"
                  resize="none"
                />
                <el-button
                  v-if="aiLoading"
                  type="danger"
                  plain
                  circle
                  @click="stopAiStream"
                  style="margin-left:8px;flex-shrink:0"
                >
                  停
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  :disabled="!aiQuestion.trim()"
                  circle
                  @click="askAi"
                  style="margin-left:8px;flex-shrink:0"
                >
                  <el-icon><Position /></el-icon>
                </el-button>
              </div>
            </div>
          </el-drawer>
        </el-card>
      </div>
    </div>

    <div v-if="isContestMode && contestWorkspaceTab === 'ranking'" class="contest-tab-panel">
      <div class="contest-rank-dashboard contest-rank-dashboard-panel">
        <div class="contest-rank-main-card">
          <div class="contest-rank-main-top">
            <div>
              <div class="contest-rank-card-label">我的名次</div>
              <div class="contest-rank-main-value" :class="getContestRankLabelClass(contestCurrentRank)">
                <span class="contest-rank-main-icon">{{ contestRankIcon(contestCurrentRank) }}</span>
                <span>{{ contestCurrentRank ? `第 ${contestCurrentRank} 名` : '暂未上榜' }}</span>
              </div>
            </div>
            <el-tag :type="contestCurrentRankCardType" effect="dark" round>
              {{ contestRankToneText }}
            </el-tag>
          </div>
          <div class="contest-rank-main-stats">
            <div class="contest-rank-stat-card">
              <span class="contest-rank-stat-label">超越选手</span>
              <strong>{{ contestRankPercent }}%</strong>
            </div>
            <div class="contest-rank-stat-card">
              <span class="contest-rank-stat-label">题目进度</span>
              <strong>{{ contestProgressPercent }}%</strong>
            </div>
          </div>
          <div class="contest-rank-gap-row">
            <span>{{ contestAheadDistanceText || '继续提交以冲击更高排名' }}</span>
            <span>{{ contestBehindDistanceText || '当前后方暂无直接追赶者' }}</span>
          </div>
        </div>
        <div class="contest-rank-side-card">
          <div class="contest-rank-card-label">冲榜快照</div>
          <div class="contest-rank-avatar-lane">
            <div
              v-for="item in contestAvatarLane"
              :key="item.userId || item.id || item.rank"
              class="contest-rank-lane-item"
              :class="getContestLaneItemClass(item)"
              :style="getContestLaneItemStyle(item)"
            >
              <div class="contest-rank-lane-rank">{{ contestRankIcon(item.rank) }} {{ item.rank || '-' }}</div>
              <el-avatar :size="38" :src="item.avatar">{{ getContestAvatarFallback(item) }}</el-avatar>
              <div class="contest-rank-lane-name">{{ getContestRankDisplayName(item) }}</div>
              <div class="contest-rank-lane-score">{{ item.totalScore ?? 0 }} 分</div>
            </div>
          </div>
        </div>
      </div>
      <el-card shadow="never" class="contest-ranking-table-card">
        <template #header>
          <div class="contest-tab-header">
            <span>排行榜</span>
            <span class="contest-tab-subtitle">只展示已入场或有提交的选手</span>
          </div>
        </template>
        <el-table :data="contestRanking" stripe :row-class-name="rankRowClass" v-if="contestRanking.length > 0">
          <el-table-column prop="rank" label="排名" width="80">
            <template #default="{ row }">
              <span class="rank-number" :class="`rank-number-${row.rank <= 3 ? row.rank : 'normal'}`">#{{ row.rank }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="220">
            <template #default="{ row }">
              <button
                type="button"
                class="ranking-user-btn"
                :class="{ disabled: !canViewContestSubmission(row) }"
                @click="canViewContestSubmission(row) && openUserSubmissions(row)"
              >
                <el-avatar :src="row.avatar" :size="36">{{ row.username?.slice(0, 1) }}</el-avatar>
                <div class="ranking-user-meta">
                  <div class="ranking-user-name-row">
                    <span class="ranking-user-name">{{ row.username }}</span>
                    <el-tag v-if="row.isCurrentUser" size="small" type="danger">我</el-tag>
                    <el-tag v-else-if="row.isCreator" size="small" type="warning">创建者</el-tag>
                  </div>
                  <div class="ranking-user-sub">
                    <span v-if="row.entered">已入场</span>
                    <span v-else-if="row.hasSubmission">有提交</span>
                    <span v-else>仅报名</span>
                    <span :class="movementClass(row.rankDelta)">{{ movementText(row.rankDelta) }}</span>
                  </div>
                </div>
              </button>
            </template>
          </el-table-column>
          <el-table-column prop="solvedCount" label="通过题数" width="110" />
          <el-table-column prop="totalPenalty" label="罚时(分钟)" width="120" />
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column v-for="(p, idx) in contestProblems" :key="p.problemId" :label="'#' + (idx + 1)" width="90" align="center">
            <template #default="{ row }">
              <div v-if="row.problems && row.problems[idx]">
                <span v-if="row.problems[idx].accepted" class="ac-cell">
                  ✓ {{ row.problems[idx].acTime }}'
                  <span v-if="row.problems[idx].wrongAttempts > 0" class="wrong-count">(-{{ row.problems[idx].wrongAttempts }})</span>
                </span>
                <span v-else-if="row.problems[idx].wrongAttempts > 0" class="wa-cell">
                  -{{ row.problems[idx].wrongAttempts }}
                </span>
                <span v-else class="untried-cell">-</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button v-if="canViewContestSubmission(row)" text type="primary" @click="openUserSubmissions(row)">查看记录</el-button>
              <span v-else class="contest-table-muted">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无排名数据" />
      </el-card>
    </div>

    <div v-if="isContestMode && contestWorkspaceTab === 'submissions'" class="contest-tab-panel">
      <el-card shadow="never" class="contest-ranking-table-card">
        <template #header>
          <div class="contest-tab-header">
            <span>提交记录</span>
            <span class="contest-tab-subtitle">{{ contestSubmissionScopeText }}</span>
          </div>
        </template>
        <el-table :data="contestSubmissions" stripe v-loading="submissionsLoading" class="submission-table" style="width: 100%">
          <el-table-column v-if="canViewAllContestSubmissions" label="选手" min-width="130" show-overflow-tooltip>
            <template #default="scope">
              <div class="contest-submission-user">
                <el-avatar :size="24" :src="scope.row.user?.avatar">
                  {{ getContestSubmissionUserName(scope.row).slice(0, 1) }}
                </el-avatar>
                <span>{{ getContestSubmissionUserName(scope.row) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="130">
            <template #default="scope">
              {{ formatSubmissionTime(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="92">
            <template #default="scope">
              <el-tag :type="getSubmissionStatusType(scope.row.status)">
                {{ formatStatus(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="题目" min-width="160" show-overflow-tooltip>
            <template #default="scope">
              {{ getContestProblemLabel(scope.row.problemId) }}
            </template>
          </el-table-column>
          <el-table-column label="语言" min-width="120" show-overflow-tooltip>
            <template #default="scope">
              {{ getLanguageLabel(scope.row.language) }}
            </template>
          </el-table-column>
          <el-table-column label="运行时间" min-width="88">
            <template #default="scope">
              {{ scope.row.executionTime != null ? scope.row.executionTime + ' ms' : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="内存" min-width="88">
            <template #default="scope">
              {{ scope.row.memoryUsed != null ? (scope.row.memoryUsed / 1024).toFixed(1) + ' MB' : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" min-width="64" />
          <el-table-column label="操作" width="70">
            <template #default="scope">
              <el-button type="primary" link @click="viewSubmissionDetail(scope.row)">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="contestSubmissions.length === 0" description="暂无提交记录" />
      </el-card>
    </div>

    <div v-if="isContestMode && contestWorkspaceTab === 'details'" class="contest-tab-panel">
      <el-card shadow="never" class="contest-ranking-table-card">
        <template #header>
          <div class="contest-tab-header">
            <span>比赛详情</span>
            <span class="contest-tab-subtitle">比赛规则、时间与题目概览</span>
          </div>
        </template>
        <div class="contest-detail-grid">
          <div class="contest-detail-meta-card">
            <span>比赛名称</span>
            <strong>{{ contestInfo?.title || '-' }}</strong>
          </div>
          <div class="contest-detail-meta-card">
            <span>状态</span>
            <strong>{{ contestStatusText }}</strong>
          </div>
          <div class="contest-detail-meta-card">
            <span>类型</span>
            <strong>{{ contestInfo?.type || '-' }}</strong>
          </div>
          <div class="contest-detail-meta-card">
            <span>当前得分</span>
            <strong>{{ contestCurrentScore }}</strong>
          </div>
          <div class="contest-detail-meta-card">
            <span>已通过</span>
            <strong>{{ contestAcceptedCount }}/{{ contestProblems.length }}</strong>
          </div>
          <div class="contest-detail-meta-card">
            <span>剩余时间</span>
            <strong>{{ contestCountdownText || '暂无' }}</strong>
          </div>
        </div>
        <el-alert
          v-if="contestInfo?.description"
          :title="contestInfo.description"
          type="info"
          :closable="false"
          show-icon
          style="margin: 16px 0"
        />
        <el-table :data="contestProblems" stripe>
          <el-table-column label="#" width="60">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="题目" min-width="220">
            <template #default="{ row }">
              <button type="button" class="ranking-user-btn" @click="goToContestProblem(row.problemId)">
                <div class="ranking-user-meta">
                  <div class="ranking-user-name-row">
                    <span class="ranking-user-name">{{ row.problemTitle || `题目 ${row.problemId}` }}</span>
                  </div>
                  <div class="ranking-user-sub">
                    <span>{{ getContestProblemStatusText(row.problemId) }}</span>
                    <span v-if="getContestProblemAttempts(row.problemId) > 0">
                      当前 {{ getContestProblemEarnedScore(row) }}/{{ getContestProblemTotalScore(row) }} 分
                    </span>
                  </div>
                </div>
              </button>
            </template>
          </el-table-column>
          <el-table-column label="得分/总分" width="120">
            <template #default="{ row }">
              {{ getContestProblemEarnedScore(row) }} / {{ getContestProblemTotalScore(row) }}
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="100">
            <template #default="{ row }">
              <el-tag :type="diffMap[row.difficulty] || 'info'" size="small">{{ row.difficulty || '-' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog
      v-model="contestUserSubmissionsVisible"
      width="min(980px, 94vw)"
      destroy-on-close
      :title="contestSelectedUser ? `${getContestRankDisplayName(contestSelectedUser)} 的提交记录` : '选手提交记录'"
    >
      <el-table :data="contestUserSubmissions" stripe v-loading="contestUserSubmissionsLoading">
        <el-table-column label="提交时间" min-width="150">
          <template #default="scope">
            {{ formatSubmissionTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="题目" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            {{ getContestProblemLabel(scope.row.problemId) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getSubmissionStatusType(scope.row.status)" size="small">
              {{ formatStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="语言" width="130" show-overflow-tooltip>
          <template #default="scope">
            {{ getLanguageLabel(scope.row.language) }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="80" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="viewSubmissionDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!contestUserSubmissionsLoading && contestUserSubmissions.length === 0" description="暂无提交记录" />
    </el-dialog>

    <!-- 测试代码对话框 -->
    <el-dialog v-model="testDialogVisible" title="代码测试" width="60%" destroy-on-close>
      <div class="test-code-body">
        <div class="test-input-section">
          <div class="test-section-heading">
            <h4 class="test-section-title">输入样例</h4>
            <div v-if="testSamples.length > 1" class="test-sample-switcher">
              <span class="test-sample-label">测试样例</span>
              <el-button-group>
                <el-button
                  v-for="sample in testSamples"
                  :key="sample.key"
                  size="small"
                  :type="selectedTestSampleKey === sample.key ? 'primary' : 'default'"
                  @click="selectTestSample(sample.key)"
                >
                  {{ sample.label }}
                </el-button>
              </el-button-group>
            </div>
          </div>
          <div class="test-code">{{ testInput }}</div>
        </div>
        <div class="test-output-section">
          <h4 class="test-section-title">输出结果</h4>
          <div v-if="testResultMessage" 
            :class="[
              'test-result-header',
              testResultMessage.includes('测试通过') ? 'success' : 'error'
            ]"
          >
            <span>{{ testResultMessage }}</span>
          </div>
          <div class="test-results-container">
            <div class="test-result-box">
              <div class="test-result-title">期望输出:</div>
              <pre class="test-result-content diff-content" v-html="testOutputDiff.expectedHtml"></pre>
            </div>
            <div class="test-result-box">
              <div class="test-result-title">您的输出:</div>
              <pre
                class="test-result-content diff-content"
                :class="actualOutputIsError ? 'error-message' : ''"
                v-html="actualOutputIsError ? escapeOutputHtml(actualOutput || '无输出') : testOutputDiff.actualHtml"
              ></pre>
            </div>
          </div>
          <div v-if="testOutputDiff.message" class="diff-info">
            <strong>差异定位：</strong>{{ testOutputDiff.message }}
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="testDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="runTestWithFixedError" :loading="testRunning">
            重新测试
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 测试结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      title="代码运行结果"
      width="min(960px, 92vw)"
      class="result-dialog"
      destroy-on-close
    >
      <div v-if="testRunning || submitting" class="test-running">
        <el-skeleton :rows="5" animated />
        <div class="running-text">{{ submitting ? '正在提交代码...' : '正在运行测试...' }}</div>
      </div>
      <div v-else class="result-dialog-body">
        <div class="result-summary">
          <div class="summary-item">
            <div class="summary-value" :class="resultStatusClass" :style="{ color: resultStatusColor }">
              {{ resultStatusText }}
            </div>
            <div class="summary-label">提交状态</div>
          </div>
          <div class="summary-item">
            <div class="summary-value">{{ testResult.score }}</div>
            <div class="summary-label">分数</div>
          </div>
          <div class="summary-item">
            <div class="summary-value">{{ testResult.memory }}</div>
            <div class="summary-label">内存消耗</div>
          </div>
        </div>

        <div v-if="isContestResultPending" class="contest-judging-notice">
          <span class="contest-judging-spinner"></span>
          <div>
            <strong>{{ resultStatusText }}</strong>
            <p>评测任务已经进入后台队列，关闭弹窗后题目状态和排名仍会自动刷新。</p>
          </div>
        </div>

        <div v-if="testResult.success && testResult.acceptedInsights" class="accepted-insights-card">
          <div class="accepted-insights-head">
            <div>
              <div class="accepted-kicker">通过表现</div>
              <h3>这次提交通过了，表现已纳入本题和全站统计</h3>
              <p>
                统计口径：{{ testResult.acceptedInsights.cohortScope === 'same_language' ? '同语言历史通过提交' : '全语言历史通过提交' }}，按每位用户当前最佳表现进行比较。
              </p>
            </div>
            <el-tag type="success" effect="dark" round>AC</el-tag>
          </div>
          <div class="accepted-metrics-grid">
            <div class="accepted-metric-card">
              <span>运行用时</span>
              <strong>{{ testResult.runtime }}</strong>
              <small>优于 {{ formatPercent(testResult.acceptedInsights.runtimeBeatPercent) }}% 历史通过提交</small>
            </div>
            <div class="accepted-metric-card">
              <span>内存消耗</span>
              <strong>{{ testResult.memory }}</strong>
              <small>优于 {{ formatPercent(testResult.acceptedInsights.memoryBeatPercent) }}% 历史通过提交</small>
            </div>
            <div class="accepted-metric-card">
              <span>题内排名</span>
              <strong>#{{ testResult.acceptedInsights.submissionRank || '-' }}</strong>
              <small>共 {{ testResult.acceptedInsights.submissionRankTotal || 0 }} 位用户最佳提交参与比较</small>
            </div>
            <div class="accepted-metric-card global">
              <span>全站排名</span>
              <strong>#{{ testResult.acceptedInsights.globalRank?.rank || '-' }}</strong>
              <small>已通过 {{ testResult.acceptedInsights.globalRank?.acceptedCount || 0 }} 题 · Lv.{{ testResult.acceptedInsights.globalRank?.level || 1 }}</small>
            </div>
          </div>
          <div
            v-if="testResult.acceptedInsights.solutionInvite?.visible"
            class="solution-invite-card"
            :class="{ recommended: testResult.acceptedInsights.solutionInvite?.recommended }"
          >
            <div>
              <strong>{{ testResult.acceptedInsights.solutionInvite?.recommended ? '这份解法值得被更多人看到' : '可以顺手整理成题解' }}</strong>
              <p>
                {{ testResult.acceptedInsights.solutionInvite?.reason }}
                {{ testResult.acceptedInsights.solutionInvite?.rewardEligible ? '完成发布后可获得' : '若后续补充更完整的思路，可能会触发题解奖励。' }}
                {{ testResult.acceptedInsights.solutionInvite?.expReward || 30 }} 经验 +
                {{ testResult.acceptedInsights.solutionInvite?.aiPointsReward || 3 }} AI积分。
              </p>
            </div>
            <el-button type="primary" plain @click="openSolutionEditorFromAcceptedInvite">
              去分享题解
            </el-button>
          </div>
        </div>

        <el-collapse v-if="testResult.code" class="submitted-code-collapse">
          <el-collapse-item name="submitted-code">
            <template #title>
              <div class="submitted-code-title">
                <span>本次提交代码</span>
                <el-tag size="small">{{ getLanguageLabel(testResult.language) }}</el-tag>
              </div>
            </template>
            <div class="code-block-wrapper">
              <el-button class="copy-code-btn" size="small" @click="copyText(testResult.code, '提交代码已复制')">
                复制代码
              </el-button>
              <pre class="submitted-code-pre">{{ testResult.code }}</pre>
            </div>
          </el-collapse-item>
        </el-collapse>
        
        <div class="test-case-list">
          <div class="result-header">
            <span>通过 {{ testResult.passedTests }} / {{ testResult.totalTests }} 个测试点</span>
            <el-tag v-if="isContestResultPending" size="small" type="warning" effect="plain">自动刷新中</el-tag>
          </div>
          <div v-for="(tc, idx) in testResult.testCases" :key="idx"
               class="test-case" :class="tc.status === 'ACCEPTED' ? 'success' : 'error'">
            <h4>
              测试点 {{ idx + 1 }}
              <el-tag size="small" :type="getSubmissionStatusType(tc.status)">
                {{ formatStatus(tc.status) }}
              </el-tag>
            </h4>
            <div class="test-case-metrics">
              <span>执行用时: {{ tc.executionTime != null ? tc.executionTime + ' ms' : '-' }}</span>
              <span>内存消耗: {{ tc.memoryUsed != null ? (tc.memoryUsed / 1024).toFixed(1) + ' MB' : '-' }}</span>
            </div>
            <pre v-if="tc.errorMessage" class="test-case-error">{{ tc.errorMessage }}</pre>
            <div v-if="tc.input || tc.expectedOutput || tc.stdout || tc.actualOutput" class="test-case-io-grid">
              <div v-if="tc.input" class="test-case-io-item">
                <strong>输入</strong>
                <pre>{{ tc.input }}</pre>
              </div>
              <div v-if="tc.expectedOutput" class="test-case-io-item">
                <strong>期望输出</strong>
                <pre>{{ tc.expectedOutput }}</pre>
              </div>
              <div v-if="tc.stdout || tc.actualOutput" class="test-case-io-item">
                <strong>实际输出</strong>
                <pre>{{ tc.stdout || tc.actualOutput }}</pre>
              </div>
            </div>
          </div>
          <div v-if="isContestResultPending && testResult.testCases.length === 0" class="test-case judging-placeholder">
            <h4>正在等待测试点结果</h4>
            <p>评测完成后会自动显示每个测试点的状态、耗时和内存。</p>
          </div>
          <div v-else-if="testResult.testCases.length === 0" class="test-case">
            <h4>暂无测试点详情</h4>
          </div>
        </div>
      </div>
    </el-dialog>
    
    <!-- 编辑器设置菜单 -->
    <el-dialog v-model="settingsVisible" title="编辑器设置" width="400px" destroy-on-close>
      <div class="settings-item">
        <span class="settings-label">编辑器主题</span>
        <el-select v-model="editorTheme" @change="changeTheme" style="width: 100%">
          <el-option-group label="深色主题">
            <el-option label="Monokai" value="ace/theme/monokai" />
            <el-option label="Tomorrow Night" value="ace/theme/tomorrow_night" />
            <el-option label="Dracula" value="ace/theme/dracula" />
            <el-option label="One Dark" value="ace/theme/one_dark" />
            <el-option label="Nord Dark" value="ace/theme/nord_dark" />
            <el-option label="Cobalt" value="ace/theme/cobalt" />
            <el-option label="Terminal" value="ace/theme/terminal" />
            <el-option label="Twilight" value="ace/theme/twilight" />
          </el-option-group>
          <el-option-group label="浅色主题">
            <el-option label="TextMate" value="ace/theme/textmate" />
            <el-option label="GitHub" value="ace/theme/github" />
            <el-option label="Chrome" value="ace/theme/chrome" />
            <el-option label="Eclipse" value="ace/theme/eclipse" />
            <el-option label="Xcode" value="ace/theme/xcode" />
            <el-option label="Solarized Light" value="ace/theme/solarized_light" />
            <el-option label="Clouds" value="ace/theme/clouds" />
          </el-option-group>
        </el-select>
      </div>
      <div class="settings-item">
        <span class="settings-label">字体大小</span>
        <el-slider v-model="fontSize" :min="12" :max="36" :step="1" show-input @change="changeFontSize" />
      </div>
      <div class="settings-item">
        <span class="settings-label">字体样式</span>
        <el-select v-model="fontFamily" @change="changeFontFamily" style="width: 100%">
          <el-option label="Consolas" value="'Consolas', monospace" />
          <el-option label="Monaco" value="'Monaco', monospace" />
          <el-option label="Fira Code" value="'Fira Code', monospace" />
          <el-option label="JetBrains Mono" value="'JetBrains Mono', monospace" />
          <el-option label="Source Code Pro" value="'Source Code Pro', monospace" />
          <el-option label="Cascadia Code" value="'Cascadia Code', monospace" />
          <el-option label="Ubuntu Mono" value="'Ubuntu Mono', monospace" />
        </el-select>
      </div>
      <div class="settings-item">
        <span class="settings-label">行间距</span>
        <el-select v-model="lineHeight" @change="changeLineHeight" style="width: 100%">
          <el-option label="紧凑 (1.2)" value="1.2" />
          <el-option label="默认 (1.5)" value="1.5" />
          <el-option label="舒适 (1.8)" value="1.8" />
          <el-option label="宽松 (2.2)" value="2.2" />
        </el-select>
      </div>
    </el-dialog>
    
    <!-- 提交代码确认对话框 -->
    <el-dialog v-model="submitDialogVisible" title="提交代码" width="30%">
      <p>确定要提交当前代码吗？</p>
      <p class="submit-warning">提交后将记录在您的提交历史中</p>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="submitDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCode" :loading="submitting">
            确认提交
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 题目反馈弹窗 -->
    <el-dialog v-model="showReportDialog" title="反馈题目问题" width="480px">
      <el-form label-width="80px">
        <el-form-item label="问题类型">
          <el-radio-group v-model="reportForm.reportType">
            <el-radio value="BUG">题面错误</el-radio>
            <el-radio value="WRONG_ANSWER">参考答案有误</el-radio>
            <el-radio value="UNCLEAR">描述不清</el-radio>
            <el-radio value="OTHER">其他问题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="reportForm.content" type="textarea" :rows="4"
                    placeholder="请详细描述你遇到的问题，方便管理员和出题人接手处理..." maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="primary" @click="submitProblemReport" :loading="reportSubmitting">提交反馈</el-button>
      </template>
    </el-dialog>

    <!-- 分享题解编辑器 -->
    <el-dialog
      v-model="solutionEditorVisible"
      title="分享我的解法"
      width="min(840px, 94vw)"
      class="solution-editor-dialog"
      destroy-on-close
    >
      <div class="solution-editor-top">
        <el-input v-model="solutionForm.title" maxlength="120" show-word-limit placeholder="给你的题解起个清楚的标题" />
        <el-select v-model="solutionForm.language" class="solution-language-select" :disabled="!!solutionForm.sourceSubmissionId">
            <el-option v-for="lang in languages" :key="lang.value" :label="lang.label" :value="lang.value" />
          </el-select>
      </div>
      <div class="solution-outline-hint">
        这里不用死板套模板，直接写你的关键思路就行。需要时也可以一键填入一个轻量提纲。
      </div>
      <div class="solution-quill-wrap">
        <QuillEditor
          v-model:content="solutionForm.description"
          contentType="html"
          :options="solutionEditorOptions"
          ref="solutionQuillRef"
        />
      </div>
      <div class="solution-editor-actions">
        <input ref="solutionImageInput" type="file" accept="image/*" style="display: none" @change="handleSolutionImageUpload" />
        <el-button @click="insertSolutionImage" :icon="PictureFilled">插入图片</el-button>
        <el-button @click="applySolutionOutline">填入提纲</el-button>
        <el-button text @click="solutionForm.description = ''">清空说明</el-button>
        <el-button @click="syncCurrentCodeToSolution">同步AC代码</el-button>
      </div>
      <el-form label-width="72px" class="solution-editor-form">
        <el-form-item label="参考代码">
          <div class="solution-code-source">
            <el-alert
              v-if="solutionForm.sourceSubmissionId"
              type="success"
              :closable="false"
              show-icon
              class="solution-source-alert"
            >
              <template #title>
                已绑定 AC 提交 #{{ solutionForm.sourceSubmissionId }}，发布时后端会重新校验并使用这份已通过代码。
                <span v-if="solutionForm.sourceRuntime || solutionForm.sourceMemory">
                  用时 {{ solutionForm.sourceRuntime || '-' }}，内存 {{ solutionForm.sourceMemory || '-' }}
                </span>
              </template>
            </el-alert>
            <el-alert
              v-else
              type="warning"
              :closable="false"
              show-icon
              class="solution-source-alert"
              title="请先提交并通过本题，再分享题解。"
            />
            <el-input
              v-model="solutionForm.code"
              type="textarea"
              :rows="8"
              resize="vertical"
              readonly
              placeholder="这里会展示已通过提交的代码；如果想换代码，请先重新提交并通过。"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="solutionEditorVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSolution" :loading="solutionSubmitting">发布题解</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { loadAceCore, loadAceExt, loadAceMode, loadAceTheme } from '@/utils/aceLoader'
import { marked } from 'marked'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import {
  CircleCheckFilled,
  Document,
  ChatDotRound,
  Position,
  CaretRight,
  Check,
  Star,
  StarFilled,
  WarningFilled,
  PictureFilled
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { problemApi, solutionApi } from '../../api'
import { aiApi } from '@/api/ai'
import { userApi } from '@/api/user'
import { favoriteApi } from '@/api/favorite'
import { submissionApi } from '@/api/submission'
import { contestApi } from '@/api/contest'
import { problemReportApi } from '@/api/problemReport'

// 做题 IDE 页面：负责题目加载、Ace 编辑器、样例运行、正式提交、竞赛异步评测、历史恢复和 AI 辅导。

// 路由
const route = useRoute()
const router = useRouter()

// 题目数据
const problem = ref<any>({})
const loading = ref(true)
const officialSolutions = ref<any[]>([])
const communitySolutions = ref<any[]>([])
const isFavorited = ref(false)

// 题目反馈
const showReportDialog = ref(false)
const reportForm = ref({ reportType: 'BUG', content: '' })
const reportSubmitting = ref(false)

// 提交题目反馈，供管理员在后台处理题面错误、样例问题或描述不清等情况。
const submitProblemReport = async () => {
  if (!reportForm.value.content.trim()) {
    ElMessage.warning('请填写反馈内容')
    return
  }
  reportSubmitting.value = true
  try {
    await problemReportApi.submit({
      problemId: problemId.value,
      reportType: reportForm.value.reportType,
      content: reportForm.value.content
    })
    ElMessage.success('反馈已提交，感谢说明！')
    showReportDialog.value = false
    reportForm.value = { reportType: 'BUG', content: '' }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交反馈失败')
  } finally {
    reportSubmitting.value = false
  }
}
// 获取路由参数
const problemId = ref(Number(route.params.id) || 0)
// 竞赛上下文：从竞赛详情页跳转过来时会携带 contestId
const contestId = computed(() => {
  const value = route.query.contestId
  if (value == null || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
})
const isContestMode = computed(() => contestId.value != null)

// 点击题目作者，跳转作者主页；如果作者就是当前用户则进入自己的个人中心。
const goToAuthorProfile = () => {
  if (!problem.value?.authorId) return
  let currentUserId = 0
  try {
    currentUserId = Number(JSON.parse(localStorage.getItem('userInfo') || '{}')?.id)
  } catch {}
  router.push(currentUserId && currentUserId === Number(problem.value.authorId) ? '/profile' : `/users/${problem.value.authorId}`)
}

// 获取做题视图题目数据：隐藏测试点和官方代码，只保留学生答题需要的信息。
const fetchProblemData = async () => {
  try {
    loading.value = true
    officialSolutions.value = []
    communitySolutions.value = []
    showSolution.value = false
    solutionsLoadedForProblem.value = null
    // 使用做题专用 API 获取题面数据，不下发隐藏测试点和官方参考代码。
    const data = await problemApi.getProblemForSolving(problemId.value)
    problem.value = data
    
    // 如果题目不存在，跳转到404页面
    if (!data) {
      router.push('/not-found')
      return
    }
    
    // 根据题目标签推断初始语言；没有语言标签时保持用户最近使用语言。
    if (problem.value.tags && problem.value.tags.length > 0) {
      // 如果题目标签中有特定语言标签，设置为该语言
      const languageTag = problem.value.tags.find(tag => 
        ['Java', 'C', 'C++', 'Python', 'JavaScript'].includes(tag.name)
      )
      if (languageTag) {
        selectedLanguage.value = languageTag.name.toLowerCase()
        if (languageTag.name === 'C++') selectedLanguage.value = 'cpp'
      }
    }
    
    selectedTestSampleKey.value = testSamples.value[0]?.key || 'public'
    applySelectedTestSample(true)
    
    console.log('加载的题目数据:', problem.value)
  } catch (error) {
    console.error('获取题目数据失败:', error)
    ElMessage.error('获取题目数据失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

// 查询当前题目是否已收藏，用于右上角收藏按钮状态。
const checkFavoriteStatus = async () => {
  try {
    const res: any = await favoriteApi.checkFavorite(problemId.value)
    isFavorited.value = (res.data || res)?.isFavorite || false
  } catch {}
}

// 收藏或取消收藏当前题目，成功后立即更新按钮状态。
const toggleFavorite = async () => {
  try {
    if (isFavorited.value) {
      await favoriteApi.removeFavorite(problemId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteApi.addFavorite(problemId.value)
      isFavorited.value = true
      ElMessage.success('已收藏')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}
// 获取题解数据：竞赛模式不展示题解，防止比赛过程中泄露答案。
const fetchSolutionData = async () => {
  if (!problemId.value || isContestMode.value) {
    officialSolutions.value = []
    communitySolutions.value = []
    showSolution.value = false
    solutionsLoadedForProblem.value = null
    return
  }
  if (solutionsLoadedForProblem.value === problemId.value) {
    return
  }

  try {
    const res: any = await solutionApi.getCategorizedSolutions(problemId.value)
    const data = res.data || res || {}
    officialSolutions.value = Array.isArray(data.official) ? data.official : []
    communitySolutions.value = Array.isArray(data.community) ? data.community : []
    showSolution.value = false

    const firstOfficial = officialSolutions.value.find(item => item?.language)
    if (firstOfficial?.language) {
      activeCodeTab.value = firstOfficial.language
    }
    solutionsLoadedForProblem.value = problemId.value
  } catch (error) {
    console.error('获取题解数据失败:', error)
    officialSolutions.value = []
    communitySolutions.value = []
    showSolution.value = false
  }
}

// 题目加载完成后恢复编辑器：竞赛恢复本场草稿，普通训练恢复最近提交。
const restoreEditorForWorkspace = async () => {
  await nextTick()
  applyContestRestrictions()
  if (isContestMode.value) {
    await restoreContestEditorState()
  } else {
    await restoreLastDraft()
  }
}

// 重新加载做题工作区：题面先加载，提交历史、收藏、竞赛信息等并行加载。
const reloadProblemWorkspace = async (restoreEditor = true) => {
  await fetchProblemData()
  const secondaryTasks = [
    loadSubmissions(),
    checkFavoriteStatus(),
    loadContestInfo()
  ]
  if (activeDescriptionTab.value === 'solution') {
    secondaryTasks.push(fetchSolutionData())
  }
  const secondaryLoads = Promise.all(secondaryTasks)
  startContestClock()

  if (restoreEditor) nextTick(() => {
    setTimeout(() => {
      restoreEditorForWorkspace()
    }, 100)
  })

  await secondaryLoads
}

// 组件挂载：清理旧竞赛草稿、读取编辑器偏好、初始化 Ace Editor 并恢复代码。
onMounted(() => {
  purgeLegacyContestEditorStorage()
  loadEditorSettings()
  const workspaceReady = reloadProblemWorkspace(false)

  // 初始化 Ace Editor，编辑器准备好后再恢复草稿，避免 setValue 时实例未创建。
  nextTick(async () => {
    initAceEditor()
    await workspaceReady
    // 等编辑器初始化完成后恢复草稿
    setTimeout(() => {
      restoreEditorForWorkspace()
    }, 100)
  })
})

onUnmounted(() => {
  removeContestPasteGuard()
  stopContestClock()
  stopContestSubmissionPolling()
})

type ContestRankingItem = {
  id?: number
  userId?: number
  username?: string
  avatar?: string
  rank?: number
  totalScore?: number
  solvedCount?: number
  totalPenalty?: number
  entered?: boolean
  hasSubmission?: boolean
  isCurrentUser?: boolean
  isCreator?: boolean
  rankDelta?: number
  problems?: Array<{
    accepted?: boolean
    acTime?: number
    wrongAttempts?: number
    score?: number
  }>
}

type ContestSubmissionItem = {
  id: number
  problemId: number
  language?: string
  createdAt?: string
  status: string
  score?: number
  executionTime?: number
  memoryUsed?: number
  userId?: number
  user?: {
    id?: number
    username?: string
    avatar?: string
  }
}

type ContestProblemProgress = {
  status?: string | null
  accepted?: boolean
  attempts?: number
  finalAttempts?: number
  bestScorePercent?: number
  earnedScore?: number
  totalScore?: number
  latestStatus?: string | null
  lastSubmissionAt?: string | null
}

const userSubmissions = ref<any[]>([])
const submissionsLoading = ref(false)
// 竞赛上下文信息
const contestInfo = ref<any>(null)
const contestProblems = ref<any[]>([])
const contestSubmissions = ref<ContestSubmissionItem[]>([])
const contestSubmissionScope = ref<'SELF' | 'ALL'>('SELF')
const contestProblemStatusMap = ref<Record<number, ContestProblemProgress | string | null>>({})
const contestRanking = ref<ContestRankingItem[]>([])
const contestUserSubmissionsVisible = ref(false)
const contestUserSubmissionsLoading = ref(false)
const contestSelectedUser = ref<ContestRankingItem | null>(null)
const contestUserSubmissions = ref<any[]>([])
const contestPreviousRankMap = ref<Record<number, number>>({})
const contestNow = ref(Date.now())
let contestClockTimer: number | null = null
let contestRefreshTimer: number | null = null
let contestSubmissionPollTimer: number | null = null
let contestSubmissionPolling = false
let contestDraftHydrating = false
let contestRouteHydrating = false
let suppressLanguageTemplateReset = false
const pendingContestSubmissionIds = ref<number[]>([])
const CONTEST_SERVER_DRAFT_CACHE_TTL = 30 * 60 * 1000
const contestSubmissionDraftCache = new Map<string, ContestServerDraft>()

const isPendingSubmissionStatus = (status?: string | null) => {
  const value = String(status || '').toUpperCase()
  return value === 'PENDING' || value === 'RUNNING'
}

type ContestServerDraft = {
  code: string
  language: string
  updatedAt: number
  contestId: number
  problemId: number
  submissionId?: number | null
  status?: string | null
}

const isKnownLanguage = (language?: string | null) => (
  !!language && languages.value.some(item => item.value === language)
)

const contestServerDraftCacheKey = (cid = contestId.value, pid = problemId.value) => (
  cid && pid ? `${cid}:${pid}` : ''
)

const getCachedContestSubmissionDraft = (
  pid = problemId.value,
  cid = contestId.value
): ContestServerDraft | null => {
  const key = contestServerDraftCacheKey(cid, pid)
  if (!key) return null
  const cached = contestSubmissionDraftCache.get(key)
  if (!cached) return null
  if (Date.now() - cached.updatedAt > CONTEST_SERVER_DRAFT_CACHE_TTL) {
    contestSubmissionDraftCache.delete(key)
    return null
  }
  return cached
}

const cacheContestSubmissionDraft = (draft?: ContestServerDraft | null) => {
  if (!draft?.contestId || !draft.problemId || typeof draft.code !== 'string') return
  contestSubmissionDraftCache.set(contestServerDraftCacheKey(draft.contestId, draft.problemId), {
    ...draft,
    updatedAt: Date.now()
  })
}

const cacheContestSubmissionDrafts = (drafts: any) => {
  const source = drafts?.data || drafts || {}
  Object.entries(source).forEach(([problemKey, value]) => {
    const draft = value as any
    if (!draft?.code) return
    cacheContestSubmissionDraft({
      code: draft.code,
      language: draft.language || selectedLanguage.value,
      updatedAt: Date.now(),
      contestId: Number(draft.contestId || contestId.value),
      problemId: Number(draft.problemId || problemKey),
      submissionId: draft.id != null ? Number(draft.id) : null,
      status: draft.status || null
    })
  })
}

const purgeLegacyContestEditorStorage = () => {
  try {
    const keys: string[] = []
    for (let index = 0; index < localStorage.length; index++) {
      const key = localStorage.key(index)
      if (key?.startsWith('codepower_contest_editor_')) keys.push(key)
    }
    keys.forEach(key => localStorage.removeItem(key))
  } catch {}
}

const trackPendingContestSubmissionsFromRecords = (records: any[]) => {
  if (!isContestMode.value || !Array.isArray(records)) return
  const ids = records
    .filter(item => isPendingSubmissionStatus(item?.status))
    .map(item => Number(item?.id))
    .filter(id => Number.isFinite(id) && id > 0)
  if (ids.length === 0) return
  const merged = new Set([...pendingContestSubmissionIds.value, ...ids])
  pendingContestSubmissionIds.value = [...merged]
  persistPendingContestSubmissionIds()
  startContestSubmissionPolling()
}

const cacheContestDraftFromSubmissionDetail = (detail: any) => {
  if (!detail?.code || !detail?.contestId || !detail?.problemId) return
  cacheContestSubmissionDraft({
    code: detail.code,
    language: detail.language || selectedLanguage.value,
    updatedAt: Date.now(),
    contestId: Number(detail.contestId),
    problemId: Number(detail.problemId),
    submissionId: detail.id != null ? Number(detail.id) : null,
    status: detail.status || null
  })
}

const cacheContestDraftFromSubmit = (
  code: string,
  language: string,
  pid: number,
  cid: number,
  submissionId?: number | null,
  status?: string | null
) => {
  if (!code || !pid || !cid) return
  cacheContestSubmissionDraft({
    code,
    language,
    updatedAt: Date.now(),
    contestId: Number(cid),
    problemId: Number(pid),
    submissionId: submissionId != null ? Number(submissionId) : null,
    status: status || null
  })
}

const invalidateContestSubmissionDraftCache = (pid = problemId.value, cid = contestId.value) => {
  const key = contestServerDraftCacheKey(cid, pid)
  if (key) {
    contestSubmissionDraftCache.delete(key)
  }
}

const restoreContestEditorFromCache = async (
  pid = problemId.value,
  cid = contestId.value
) => {
  const cached = getCachedContestSubmissionDraft(pid, cid)
  if (!cached?.code || !editor.value) return false
  await applyEditorCode(cached.code, cached.language)
  return true
}

const pendingContestStorageKey = () => contestId.value ? `codepower_contest_pending_submissions_${contestId.value}` : ''

const persistPendingContestSubmissionIds = () => {
  const key = pendingContestStorageKey()
  if (!key) return
  try {
    sessionStorage.setItem(key, JSON.stringify(pendingContestSubmissionIds.value))
  } catch {}
}

const restorePendingContestSubmissionIds = () => {
  const key = pendingContestStorageKey()
  if (!key) {
    pendingContestSubmissionIds.value = []
    return
  }
  try {
    const parsed = JSON.parse(sessionStorage.getItem(key) || '[]')
    pendingContestSubmissionIds.value = Array.isArray(parsed)
      ? parsed.map((item: any) => Number(item)).filter((item: number) => Number.isFinite(item) && item > 0)
      : []
  } catch {
    pendingContestSubmissionIds.value = []
  }
}

const upsertSubmissionRecord = (list: any[], record: any) => {
  if (!record?.id) return list
  return [record, ...list.filter(item => Number(item.id) !== Number(record.id))]
}

const buildSubmissionRecord = (detail: any) => ({
  id: detail.id,
  problemId: detail.problemId,
  userId: detail.userId,
  user: detail.user,
  language: detail.language,
  status: detail.status,
  score: detail.score,
  executionTime: detail.executionTime,
  memoryUsed: detail.memoryUsed,
  contestId: detail.contestId ?? contestId.value ?? null,
  createdAt: detail.createdAt || new Date().toISOString()
})

const applySubmissionDetailToResult = (detail: any) => {
  const status = detail.status || testResult.status || ''
  const cases = Array.isArray(detail.testResults) ? detail.testResults : []
  const pending = isPendingSubmissionStatus(status)
  testResult.status = status
  testResult.success = status === 'ACCEPTED'
  testResult.score = detail.score || 0
  testResult.output = status === 'ACCEPTED' ? '全部通过' : formatStatus(status)
  testResult.runtime = !pending && detail.executionTime != null ? `${detail.executionTime} ms` : '-'
  testResult.memory = !pending && detail.memoryUsed != null ? `${(detail.memoryUsed / 1024).toFixed(1)} MB` : '-'
  testResult.language = detail.language || selectedLanguage.value
  testResult.code = detail.code || testResult.code || ''
  testResult.createdAt = detail.createdAt || testResult.createdAt || new Date().toLocaleString()
  testResult.submissionId = detail.id || testResult.submissionId || null
  testResult.acceptedInsights = detail.acceptedInsights || null
  testResult.testCases = cases
  testResult.passedTests = Number(detail.passedTests ?? cases.filter(tc => tc.status === 'ACCEPTED').length)
  testResult.totalTests = Number(detail.totalTests ?? (cases.length > 0 ? cases.length : testResult.totalTests) ?? 0)
}

const prepareContestPendingResult = (
  code: string,
  language: string,
  submissionId?: number | null,
  status = 'PENDING'
) => {
  testResult.status = status
  testResult.success = false
  testResult.score = 0
  testResult.passedTests = 0
  testResult.totalTests = 0
  testResult.output = formatStatus(status)
  testResult.runtime = '-'
  testResult.memory = '-'
  testResult.language = language
  testResult.code = code
  testResult.createdAt = new Date().toLocaleString()
  testResult.submissionId = submissionId != null ? Number(submissionId) : null
  testResult.acceptedInsights = null
  testResult.testCases = []
}

// 加载当前题目的提交历史：普通训练查日常提交，竞赛模式查本场本题提交。
const loadSubmissions = async () => {
  if (!problemId.value) return
  submissionsLoading.value = true
  try {
    const res: any = isContestMode.value
      ? await submissionApi.getMyContestProblemSubmissions(contestId.value as number, problemId.value, 1, 50)
      : await submissionApi.getMySubmissions(problemId.value, 1, 50)
    const data = res.data || res
    userSubmissions.value = data.records || (Array.isArray(data) ? data : [])
    if (isContestMode.value) {
      trackPendingContestSubmissionsFromRecords(userSubmissions.value)
      const latest = userSubmissions.value[0]
      const cached = getCachedContestSubmissionDraft(problemId.value, contestId.value)
      if (!latest?.id || Number(cached?.submissionId) !== Number(latest.id)) {
        invalidateContestSubmissionDraftCache(problemId.value, contestId.value)
      }
    }
  } catch {
    userSubmissions.value = []
  } finally {
    submissionsLoading.value = false
  }
}

// 加载竞赛工作区：竞赛详情、题目、提交、排名和每题最近提交草稿一起拉取。
const loadContestWorkspace = async () => {
  if (!contestId.value) return
  try {
    const [detailRes, problemsRes, submissionsRes, rankingRes, draftsRes] = await Promise.all([
      contestApi.getContestDetail(contestId.value),
      contestApi.getContestProblems(contestId.value),
      submissionApi.getMyContestSubmissions(contestId.value, 1, 50),
      contestApi.getRanking(contestId.value),
      submissionApi.getMyContestDrafts(contestId.value)
    ])

    contestInfo.value = detailRes.data || detailRes
    contestProblems.value = problemsRes.data || problemsRes || []
    cacheContestSubmissionDrafts(draftsRes)
    const submissionData = submissionsRes.data || submissionsRes
    contestSubmissions.value = submissionData.records || []
    trackPendingContestSubmissionsFromRecords(contestSubmissions.value)
    contestSubmissionScope.value = submissionData.scope === 'ALL' ? 'ALL' : 'SELF'
    const rawRanking = (rankingRes.data || rankingRes || []) as ContestRankingItem[]
    const nextRankMap: Record<number, number> = {}
    contestRanking.value = rawRanking.map((item) => {
      const uid = Number(item.userId || item.id)
      const previousRank = contestPreviousRankMap.value[uid]
      if (uid && item.rank) nextRankMap[uid] = item.rank
      return {
        ...item,
        rankDelta: previousRank && item.rank ? previousRank - item.rank : 0
      }
    })
    contestPreviousRankMap.value = nextRankMap

    if (contestProblems.value.length > 0) {
      const statusRes: any = await submissionApi.getUserContestProblemStatus(
        contestId.value,
        contestProblems.value.map((item: any) => item.problemId)
      )
      const statusData = statusRes.data || statusRes || {}
      contestProblemStatusMap.value = Object.fromEntries(
        Object.entries(statusData).map(([key, value]) => [Number(key), normalizeContestProblemProgress(value as any)])
      )
    } else {
      contestProblemStatusMap.value = {}
    }
  } catch (error) {
    console.error('加载竞赛工作区失败:', error)
  }
}

// 停止竞赛提交轮询，避免切出竞赛页面后继续发请求。
const stopContestSubmissionPolling = () => {
  if (contestSubmissionPollTimer) {
    window.clearInterval(contestSubmissionPollTimer)
    contestSubmissionPollTimer = null
  }
}

// 用提交详情刷新竞赛提交列表、当前题历史和题目状态色。
const updateContestSubmissionSnapshot = (detail: any) => {
  if (!detail?.id) return
  cacheContestDraftFromSubmissionDetail(detail)
  const record = buildSubmissionRecord(detail)
  contestSubmissions.value = upsertSubmissionRecord(contestSubmissions.value, record)
  if (Number(record.problemId) === Number(problemId.value)) {
    userSubmissions.value = upsertSubmissionRecord(userSubmissions.value, record)
  }
  if (record.problemId) {
    const pid = Number(record.problemId)
    const currentProgress = getContestProblemProgress(pid)
    const totalScore = getContestProblemTotalScore(pid)
    const nextStatus = record.status === 'ACCEPTED'
      ? 'ACCEPTED'
      : currentProgress.status === 'ACCEPTED'
        ? 'ACCEPTED'
        : isPendingSubmissionStatus(record.status)
          ? 'PENDING'
          : 'ATTEMPTED'
    const bestScorePercent = nextStatus === 'ACCEPTED'
      ? 100
      : Math.max(Number(currentProgress.bestScorePercent || 0), Number(record.score || 0))
    mergeContestProblemProgress(pid, {
      status: nextStatus,
      accepted: nextStatus === 'ACCEPTED',
      attempts: Math.max(Number(currentProgress.attempts || 0), 1),
      bestScorePercent,
      earnedScore: nextStatus === 'ACCEPTED'
        ? totalScore
        : Math.round(bestScorePercent * totalScore / 100),
      latestStatus: record.status,
      lastSubmissionAt: record.createdAt
    })
  }
}

// 异步评测完成后给用户明确反馈，而不是只停留在黄色等待状态。
const notifyContestSubmissionFinished = (detail: any) => {
  const title = getContestProblemLabel(Number(detail.problemId))
  const statusText = formatStatus(detail.status)
  if (detail.status === 'ACCEPTED') {
    ElMessage.success(`${title} 评测完成：通过`)
  } else {
    const firstError = detail.errorMessage || (Array.isArray(detail.testResults)
      ? detail.testResults.find((item: any) => item?.errorMessage)?.errorMessage
      : '')
    const reason = firstError ? `：${String(firstError).split('\n').find(Boolean)?.slice(0, 48)}` : ''
    ElMessage.warning(`${title} 评测完成：${statusText}${reason}`)
  }
}

// 轮询仍在 PENDING/RUNNING 的竞赛提交，完成后同步弹窗、榜单和题目状态。
const pollPendingContestSubmissions = async () => {
  if (!isContestMode.value || pendingContestSubmissionIds.value.length === 0 || contestSubmissionPolling) {
    if (pendingContestSubmissionIds.value.length === 0) stopContestSubmissionPolling()
    return
  }

  contestSubmissionPolling = true
  let hasFinished = false
  const ids = [...pendingContestSubmissionIds.value]
  try {
    await Promise.all(ids.map(async (id) => {
      try {
        const res: any = await submissionApi.getDetail(id)
        const detail = res.data || res
        updateContestSubmissionSnapshot(detail)
        if (Number(testResult.submissionId) === Number(id)) {
          applySubmissionDetailToResult(detail)
        }

        if (!isPendingSubmissionStatus(detail.status)) {
          hasFinished = true
          pendingContestSubmissionIds.value = pendingContestSubmissionIds.value.filter(item => Number(item) !== Number(id))
          notifyContestSubmissionFinished(detail)
        }
      } catch (error) {
        console.warn('轮询竞赛提交结果失败:', id, error)
      }
    }))

    persistPendingContestSubmissionIds()
    if (hasFinished) {
      await Promise.all([loadSubmissions(), loadContestWorkspace()])
    }
  } finally {
    contestSubmissionPolling = false
    if (pendingContestSubmissionIds.value.length === 0) {
      stopContestSubmissionPolling()
    }
  }
}

// 开启竞赛提交轮询；页面关闭结果弹窗后也会继续更新状态。
const startContestSubmissionPolling = () => {
  if (!isContestMode.value || pendingContestSubmissionIds.value.length === 0 || contestSubmissionPollTimer) return
  contestSubmissionPollTimer = window.setInterval(() => {
    pollPendingContestSubmissions()
  }, 2500)
  pollPendingContestSubmissions()
}

// 记录一个待轮询的异步提交编号。
const trackPendingContestSubmission = (submissionId?: number | null) => {
  const id = Number(submissionId)
  if (!isContestMode.value || !Number.isFinite(id) || id <= 0) return
  if (!pendingContestSubmissionIds.value.includes(id)) {
    pendingContestSubmissionIds.value = [...pendingContestSubmissionIds.value, id]
  }
  persistPendingContestSubmissionIds()
  startContestSubmissionPolling()
}

const currentUserId = computed(() => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return u.id ?? null
  } catch {
    return null
  }
})

const currentUserRole = computed(() => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return u.role ?? ''
  } catch {
    return ''
  }
})

const currentRankingEntry = computed(() => {
  if (!currentUserId.value) return null
  return contestRanking.value.find((item) => Number(item.userId) === Number(currentUserId.value)) || null
})

const normalizeContestProblemProgress = (value: ContestProblemProgress | string | null | undefined): ContestProblemProgress => {
  if (!value) return { status: null, attempts: 0, finalAttempts: 0, bestScorePercent: 0, earnedScore: 0 }
  if (typeof value === 'string') {
    return {
      status: value,
      accepted: value === 'ACCEPTED',
      attempts: value ? 1 : 0,
      finalAttempts: value && value !== 'PENDING' && value !== 'RUNNING' ? 1 : 0,
      bestScorePercent: value === 'ACCEPTED' ? 100 : 0,
      earnedScore: 0
    }
  }
  return {
    ...value,
    status: value.status ?? null,
    attempts: Number(value.attempts || 0),
    finalAttempts: Number(value.finalAttempts || 0),
    bestScorePercent: Number(value.bestScorePercent || 0),
    earnedScore: Number(value.earnedScore || 0)
  }
}

const getCurrentRankingProblemCell = (problemIdValue: number) => {
  const index = contestProblems.value.findIndex((item: any) => Number(item.problemId) === Number(problemIdValue))
  if (index < 0) return null
  return currentRankingEntry.value?.problems?.[index] || null
}

const getContestProblemProgress = (problemIdValue: number): ContestProblemProgress => {
  const pid = Number(problemIdValue)
  const progress = normalizeContestProblemProgress(contestProblemStatusMap.value[pid])
  if (progress.status || Number(progress.attempts || 0) > 0) return progress

  const rankingCell = getCurrentRankingProblemCell(pid)
  if (!rankingCell) return progress
  const attempts = Number(rankingCell.wrongAttempts || 0) + (rankingCell.accepted ? 1 : 0)
  return {
    status: rankingCell.accepted ? 'ACCEPTED' : attempts > 0 ? 'ATTEMPTED' : null,
    accepted: !!rankingCell.accepted,
    attempts,
    finalAttempts: attempts,
    earnedScore: Number(rankingCell.score || 0),
    bestScorePercent: undefined
  }
}

const getContestProblemTotalScore = (rowOrProblemId: any) => {
  const row = typeof rowOrProblemId === 'object'
    ? rowOrProblemId
    : contestProblems.value.find((item: any) => Number(item.problemId) === Number(rowOrProblemId))
  const progress = row?.problemId ? getContestProblemProgress(Number(row.problemId)) : null
  return Number(progress?.totalScore ?? row?.score ?? 100)
}

const getContestProblemEarnedScore = (rowOrProblemId: any) => {
  const pid = Number(typeof rowOrProblemId === 'object' ? rowOrProblemId.problemId : rowOrProblemId)
  const progress = getContestProblemProgress(pid)
  const totalScore = getContestProblemTotalScore(rowOrProblemId)
  if (progress.status === 'ACCEPTED') return totalScore
  if (Number.isFinite(Number(progress.earnedScore))) {
    return Math.max(0, Math.min(totalScore, Math.round(Number(progress.earnedScore || 0))))
  }
  const percent = Math.max(0, Math.min(100, Number(progress.bestScorePercent || 0)))
  return Math.max(0, Math.min(totalScore, Math.round(percent * totalScore / 100)))
}

const getContestProblemAttempts = (problemIdValue: number) => Number(getContestProblemProgress(problemIdValue).attempts || 0)
const getContestProblemStatus = (problemIdValue: number) => getContestProblemProgress(problemIdValue).status || null
const getContestProblemStatusText = (problemIdValue: number) => {
  const progress = getContestProblemProgress(problemIdValue)
  const status = progress.status
  if (status === 'ACCEPTED') return '已通过'
  if (status === 'PENDING' || status === 'RUNNING') return '评测中'
  if (status === 'ATTEMPTED') return '已提交'
  return '未尝试'
}

const mergeContestProblemProgress = (problemIdValue: number, patch: ContestProblemProgress) => {
  const pid = Number(problemIdValue)
  const current = normalizeContestProblemProgress(contestProblemStatusMap.value[pid])
  contestProblemStatusMap.value = {
    ...contestProblemStatusMap.value,
    [pid]: normalizeContestProblemProgress({
      ...current,
      ...patch
    })
  }
}

const canViewAllContestSubmissions = computed(() => {
  if (!isContestMode.value) return false
  if (contestSubmissionScope.value === 'ALL') return true
  return currentUserRole.value === 'ADMIN'
    || Number(contestInfo.value?.creatorId) === Number(currentUserId.value)
})
const contestSubmissionScopeText = computed(() => (
  canViewAllContestSubmissions.value
    ? '本场比赛全部提交'
    : '我的比赛提交'
))

const contestCurrentScore = computed(() => currentRankingEntry.value?.totalScore ?? 0)
const contestAcceptedCount = computed(() => contestProblems.value.filter((item: any) => getContestProblemStatus(Number(item.problemId)) === 'ACCEPTED').length)
const contestActiveRanking = computed(() => contestRanking.value.filter(item => item.entered !== false))
const contestCurrentRank = computed(() => currentRankingEntry.value?.rank ?? null)
const contestCurrentUserName = computed(() => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return u.username || u.nickname || '我'
  } catch {
    return '我'
  }
})
const contestTopPlayers = computed(() => contestActiveRanking.value.slice(0, 3))
const contestAroundMe = computed(() => {
  const activeList = contestActiveRanking.value
  if (activeList.length === 0) return []
  const currentRank = contestCurrentRank.value
  if (!currentRank) return activeList.slice(0, Math.min(5, activeList.length))
  const index = activeList.findIndex(item => item.rank === currentRank)
  if (index === -1) return activeList.slice(0, Math.min(5, activeList.length))
  const start = Math.max(0, index - 2)
  const end = Math.min(activeList.length, index + 3)
  return activeList.slice(start, end)
})
const contestAheadDistanceText = computed(() => {
  const currentRank = contestCurrentRank.value
  if (!currentRank || currentRank <= 1) return ''
  const currentScore = contestCurrentScore.value
  const ahead = contestActiveRanking.value.find(item => item.rank === currentRank - 1)
  if (!ahead) return ''
  const gap = Math.max(0, (ahead.totalScore ?? 0) - currentScore)
  return gap > 0 ? `距离前一名还差 ${gap} 分` : '已追平前一名，继续冲刺'
})
const contestBehindDistanceText = computed(() => {
  const currentRank = contestCurrentRank.value
  if (!currentRank) return ''
  const currentScore = contestCurrentScore.value
  const behind = contestActiveRanking.value.find(item => item.rank === currentRank + 1)
  if (!behind) return ''
  const gap = Math.max(0, currentScore - (behind.totalScore ?? 0))
  return gap > 0 ? `领先后一名 ${gap} 分` : '后一名紧追中'
})
const contestRankToneText = computed(() => {
  const currentRank = contestCurrentRank.value
  if (!currentRank) return '提交后即可进入实时榜单'
  if (currentRank === 1) return '你现在领跑全场，稳住节奏'
  if (currentRank <= 3) return '你正在领奖台区域，继续保持'
  if (currentRank <= 10) return '你已进入前十，冲击领奖台'
  return '榜单还很长，下一次通过就可能起飞'
})
const contestCurrentRankCardType = computed(() => {
  const currentRank = contestCurrentRank.value
  if (!currentRank) return 'info'
  if (currentRank === 1) return 'warning'
  if (currentRank <= 3) return 'success'
  return 'primary'
})
const contestProgressPercent = computed(() => {
  if (!contestProblems.value.length) return 0
  return Math.round((contestAcceptedCount.value / contestProblems.value.length) * 100)
})
const contestCurrentProblemIndex = computed(() => {
  if (!contestProblems.value.length) return -1
  return contestProblems.value.findIndex((item: any) => Number(item.problemId) === Number(problemId.value))
})
const contestPrevProblem = computed(() => {
  const index = contestCurrentProblemIndex.value
  if (index <= 0) return null
  return contestProblems.value[index - 1] || null
})
const contestNextProblem = computed(() => {
  const index = contestCurrentProblemIndex.value
  if (index < 0 || index >= contestProblems.value.length - 1) return null
  return contestProblems.value[index + 1] || null
})
const contestRankPercent = computed(() => {
  const activeCount = contestActiveRanking.value.length
  const currentRank = contestCurrentRank.value
  if (!activeCount || !currentRank) return 0
  return Math.max(0, Math.round(((activeCount - currentRank + 1) / activeCount) * 100))
})
const contestAvatarLane = computed(() => {
  const activeList = contestActiveRanking.value
  if (activeList.length <= 6) return activeList
  const top = activeList.slice(0, 3)
  const currentRank = contestCurrentRank.value
  const me = currentRankingEntry.value && currentRank && currentRank > 3
    ? [currentRankingEntry.value]
    : []
  const tail = activeList.slice(-2).filter(item => !me.some(meItem => meItem.userId === item.userId))
  return [...top, ...me, ...tail]
})
const contestRankIcon = (rank?: number | null) => {
  if (rank === 1) return '👑'
  if (rank === 2) return '🥈'
  if (rank === 3) return '🥉'
  return '#'
}
const getContestAvatarFallback = (item: ContestRankingItem) => {
  const name = item.username || ''
  return name ? name.slice(0, 1).toUpperCase() : '?'
}
const getContestLaneItemClass = (item: ContestRankingItem) => {
  return {
    current: Number(item.userId) === Number(currentUserId.value),
    champion: item.rank === 1,
    podium: !!item.rank && item.rank <= 3
  }
}
const getContestLaneItemStyle = (item: ContestRankingItem) => {
  if (item.rank === 1) return { '--lane-color': '#f59e0b' }
  if (item.rank === 2) return { '--lane-color': '#94a3b8' }
  if (item.rank === 3) return { '--lane-color': '#d97706' }
  if (Number(item.userId) === Number(currentUserId.value)) return { '--lane-color': '#2563eb' }
  return { '--lane-color': '#6366f1' }
}
const getContestRankLabelClass = (rank?: number | null) => {
  if (rank === 1) return 'champion'
  if (rank === 2) return 'silver'
  if (rank === 3) return 'bronze'
  return ''
}
const getContestRankDisplayName = (item: ContestRankingItem) => {
  if (Number(item.userId) === Number(currentUserId.value)) return `${item.username || contestCurrentUserName.value}（我）`
  return item.username || `选手${item.userId || ''}`
}
const getContestRankSummaryText = (item: ContestRankingItem) => {
  const solved = item.solvedCount ?? 0
  const score = item.totalScore ?? 0
  return `${solved} 题 / ${score} 分`
}
const getContestProblemButtonClass = (problemIdValue: number) => {
  const status = getContestProblemStatus(problemIdValue)
  return {
    active: Number(problemIdValue) === Number(problemId.value),
    accepted: status === 'ACCEPTED',
    pending: status === 'PENDING' || status === 'RUNNING',
    attempted: status === 'ATTEMPTED'
  }
}
const contestStatusText = computed(() => {
  const map: Record<string, string> = { UPCOMING: '即将开始', RUNNING: '进行中', ENDED: '已结束', DRAFT: '草稿' }
  return map[contestInfo.value?.status] || contestInfo.value?.status || '-'
})
const contestStatusTagType = computed(() => {
  const map: Record<string, string> = { UPCOMING: 'warning', RUNNING: 'success', ENDED: 'info', DRAFT: 'info' }
  return map[contestInfo.value?.status] || 'info'
})
const diffMap: Record<string, string> = {
  简单: 'success',
  普通: 'warning',
  困难: 'danger',
  极限: 'danger',
  EASY: 'success',
  MEDIUM: 'warning',
  HARD: 'danger',
  EXTREME: 'danger'
}

const movementText = (rankDelta?: number) => {
  if (!rankDelta) return '—'
  return rankDelta > 0 ? `↑${rankDelta}` : `↓${Math.abs(rankDelta)}`
}

const movementClass = (rankDelta?: number) => {
  if (!rankDelta) return 'movement-flat'
  return rankDelta > 0 ? 'movement-up' : 'movement-down'
}

const rankRowClass = ({ row }: { row: ContestRankingItem }) => {
  const classes = []
  if (row.rank === 1) classes.push('rank-gold')
  if (row.rank === 2) classes.push('rank-silver')
  if (row.rank === 3) classes.push('rank-bronze')
  if (row.isCurrentUser) classes.push('rank-self')
  return classes.join(' ')
}

const canViewContestSubmission = (row: ContestRankingItem) => {
  if (!row) return false
  if (Number(row.userId) === Number(currentUserId.value)) return true
  if (currentUserRole.value === 'ADMIN') return true
  return Number(contestInfo.value?.creatorId) === Number(currentUserId.value)
}

const getContestProblemMarker = (problemIdValue: number) => {
  const status = getContestProblemStatus(problemIdValue)
  if (status === 'ACCEPTED') return 'OK'
  if (status === 'PENDING' || status === 'RUNNING') return '...'
  if (status === 'ATTEMPTED') return 'WA'
  return '-'
}

const getContestProblemLabel = (problemIdValue: number) => {
  const item = contestProblems.value.find((p: any) => Number(p.problemId) === Number(problemIdValue))
  return item?.problemTitle || `题目 ${problemIdValue}`
}

const getContestSubmissionUserName = (row: ContestSubmissionItem) => {
  if (!row) return '-'
  if (row.user?.username) return row.user.username
  if (row.user?.id) return `用户 #${row.user.id}`
  if (row.userId) return `用户 #${row.userId}`
  return '-'
}

const goToContestProblem = (targetProblemId: number) => {
  contestWorkspaceTab.value = 'answer'
  activeDescriptionTab.value = 'description'
  if (Number(targetProblemId) === Number(problemId.value)) return
  router.push({ path: `/problems/${targetProblemId}`, query: { contestId: String(contestId.value) } })
}

const goToAdjacentContestProblem = (delta: number) => {
  const index = contestCurrentProblemIndex.value
  const target = contestProblems.value[index + delta]
  if (!target) return
  goToContestProblem(Number(target.problemId))
}

const formatSubmissionTime = (value: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const contestRemainingMs = computed(() => {
  if (!contestInfo.value) return 0
  const target = getContestCountdownTarget()
  if (!target || !Number.isFinite(target)) return 0
  return Math.max(0, target - contestNow.value)
})

const isOpenEndedContest = () => {
  const info = contestInfo.value
  if (!info?.endTime) return !info?.durationMinutes || info.durationMinutes <= 0
  const end = new Date(info.endTime)
  return !Number.isFinite(end.getTime()) || end.getFullYear() >= 2099
}

const getContestCountdownTarget = () => {
  const info = contestInfo.value
  if (!info) return null
  if (info.status === 'UPCOMING') return new Date(info.startTime).getTime()
  if (info.status !== 'RUNNING') return null
  if (info.participantDeadline) return new Date(info.participantDeadline).getTime()
  if (isOpenEndedContest()) return null
  return new Date(info.endTime).getTime()
}

const contestCountdownLabel = computed(() => {
  if (contestInfo.value?.status === 'UPCOMING') return '距离开始'
  return contestInfo.value?.participantDeadline ? '个人剩余' : '距离结束'
})
const contestCountdownText = computed(() => {
  if (!isContestMode.value || !contestInfo.value || !['UPCOMING', 'RUNNING'].includes(contestInfo.value.status)) return ''
  if (contestInfo.value.status === 'RUNNING' && isOpenEndedContest() && !contestInfo.value.participantDeadline) return '长期开放'
  const totalSeconds = Math.floor(contestRemainingMs.value / 1000)
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const parts: string[] = []
  if (days > 0) parts.push(`${days}天`)
  if (days > 0 || hours > 0) parts.push(`${hours}小时`)
  if (days > 0 || hours > 0 || minutes > 0) parts.push(`${minutes}分钟`)
  parts.push(`${seconds}秒`)
  return parts.join(' ')
})

const startContestClock = () => {
  if (!isContestMode.value) return
  if (contestClockTimer) window.clearInterval(contestClockTimer)
  if (contestRefreshTimer) window.clearInterval(contestRefreshTimer)
  contestNow.value = Date.now()
  contestClockTimer = window.setInterval(() => {
    contestNow.value = Date.now()
  }, 1000)
  contestRefreshTimer = window.setInterval(() => {
    if (contestInfo.value?.status === 'RUNNING') {
      loadContestWorkspace()
    }
  }, 30000)
}

const stopContestClock = () => {
  if (contestClockTimer) {
    window.clearInterval(contestClockTimer)
    contestClockTimer = null
  }
  if (contestRefreshTimer) {
    window.clearInterval(contestRefreshTimer)
    contestRefreshTimer = null
  }
}

// 草稿恢复：加载上次提交的代码和编译器（非竞赛模式）
const restoreLastDraft = async () => {
  if (contestId.value || !problemId.value) return
  try {
    const res: any = await submissionApi.getLastSubmission(problemId.value)
    const data = res.data || res
    if (data && data.code) {
      rememberUserPreferredLanguage(data.language)
      await applyEditorCode(data.code, data.language)
      return
    }
  } catch {
    // 继续走用户最近语言兜底，避免偶发历史接口失败时卡在 Java。
  }

  const preferredLanguage = await loadPreferredSubmissionLanguage()
  await applyPreferredLanguageTemplate(preferredLanguage)
}

const applyEditorCode = async (code: string, language?: string | null) => {
  if (!editor.value) return
  contestDraftHydrating = true
  suppressLanguageTemplateReset = true
  try {
    if (isKnownLanguage(language)) {
      selectedLanguage.value = language as string
    }
    await nextTick()
    await setEditorLanguage()
    editor.value.setValue(code, -1)
    refreshAiCodeStats()
  } finally {
    await nextTick()
    contestDraftHydrating = false
    suppressLanguageTemplateReset = false
  }
}

const loadPreferredSubmissionLanguage = async () => {
  try {
    const res: any = await submissionApi.getPreferredLanguage()
    const data = res.data || res || {}
    const language = data.language || null
    if (isKnownLanguage(language)) {
      rememberUserPreferredLanguage(language)
      return language as string
    }
  } catch {}
  return getStoredPreferredLanguage()
}

const applyPreferredLanguageTemplate = async (language?: string | null) => {
  if (!editor.value) return
  contestDraftHydrating = true
  suppressLanguageTemplateReset = true
  try {
    if (isKnownLanguage(language)) {
      selectedLanguage.value = language as string
    }
    await nextTick()
    await setEditorLanguage()
    editor.value.setValue(sampleCode.value, -1)
    refreshAiCodeStats()
  } finally {
    await nextTick()
    contestDraftHydrating = false
    suppressLanguageTemplateReset = false
  }
}

const clearEditorDuringContestRouteChange = () => {
  if (!editor.value) return
  contestDraftHydrating = true
  try {
    editor.value.setValue('', -1)
    refreshAiCodeStats()
  } finally {
    contestDraftHydrating = false
  }
}

const loadLatestContestSubmissionDraft = async (
  pid = problemId.value,
  cid = contestId.value
): Promise<ContestServerDraft | null> => {
  if (!cid || !pid) return null
  const cached = getCachedContestSubmissionDraft(pid, cid)
  if (cached?.code) return cached
  try {
    const pageRes: any = await submissionApi.getMyContestProblemSubmissions(cid, pid, 1, 1)
    const pageData = pageRes.data || pageRes || {}
    const latest = Array.isArray(pageData.records) ? pageData.records[0] : null
    if (!latest?.id) return null

    const detailRes: any = await submissionApi.getDetail(Number(latest.id))
    const detail = detailRes.data || detailRes || {}
    if (!detail?.code) return null
    const draft: ContestServerDraft = {
      code: detail.code,
      language: detail.language || latest.language || selectedLanguage.value,
      updatedAt: Date.now(),
      contestId: Number(cid),
      problemId: Number(pid),
      submissionId: Number(detail.id || latest.id),
      status: detail.status || latest.status || null
    }
    cacheContestSubmissionDraft(draft)
    return draft
  } catch {
    return null
  }
}

// 竞赛切题时优先从服务器最近提交恢复本题代码，避免不同题目的编辑器内容串在一起。
const restoreContestEditorState = async () => {
  if (!isContestMode.value || !editor.value) return
  const restoreContestId = Number(contestId.value)
  const restoreProblemId = Number(problemId.value)
  const stillSameProblem = () => (
    Number(contestId.value) === restoreContestId && Number(problemId.value) === restoreProblemId
  )

  const latestSubmissionDraft = await loadLatestContestSubmissionDraft(restoreProblemId, restoreContestId)
  if (latestSubmissionDraft?.code) {
    if (!stillSameProblem()) return
    await applyEditorCode(latestSubmissionDraft.code, latestSubmissionDraft.language)
    return
  }

  if (!stillSameProblem()) return
  await applyEditorCode(sampleCode.value, selectedLanguage.value)
}

// 加载竞赛配置（允许的语言、是否允许粘贴）
const loadContestInfo = async () => {
  if (!contestId.value) {
    contestInfo.value = null
    contestProblems.value = []
    contestSubmissions.value = []
    contestSubmissionScope.value = 'SELF'
    contestProblemStatusMap.value = {}
    contestRanking.value = []
    pendingContestSubmissionIds.value = []
    stopContestSubmissionPolling()
    languages.value = allLanguageOptions.map(lang => ({ ...lang }))
    return
  }
  restorePendingContestSubmissionIds()
  await loadContestWorkspace()
  startContestSubmissionPolling()
}

const formatStatus = (status: string) => {
  const map: Record<string, string> = {
    ACCEPTED: '通过', WRONG_ANSWER: '答案错误', TIME_LIMIT_EXCEEDED: '超时',
    COMPILATION_ERROR: '编译错误', RUNTIME_ERROR: '运行错误', MEMORY_LIMIT_EXCEEDED: '内存超限',
    PENDING: '等待评测', RUNNING: '评测中'
  }
  return map[status] || status
}

// 编程语言选项
const languages = ref([
  { label: 'C++ (GCC 9.2.0)', value: 'cpp', id: 54, mode: 'c_cpp' },
  { label: 'C (GCC 9.2.0)', value: 'c', id: 50, mode: 'c_cpp' },
  { label: 'Java (OpenJDK 13.0.1)', value: 'java', id: 62, mode: 'java' },
  { label: 'Python (3.8.1)', value: 'python', id: 71, mode: 'python' },
  { label: 'JavaScript (Node.js 12.14.0)', value: 'javascript', id: 63, mode: 'javascript' },
  { label: 'TypeScript (3.7.4)', value: 'typescript', id: 74, mode: 'typescript' },
  { label: 'Assembly (NASM 2.14.02)', value: 'assembly', id: 45, mode: 'assembly_x86' },
  { label: 'Bash (5.0.0)', value: 'bash', id: 46, mode: 'sh' },
  { label: 'C (Clang 7.0.1)', value: 'c_clang', id: 75, mode: 'c_cpp' },
  { label: 'C++ (Clang 7.0.1)', value: 'cpp_clang', id: 76, mode: 'c_cpp' },
  { label: 'C (GCC 7.4.0)', value: 'c_gcc7', id: 48, mode: 'c_cpp' },
  { label: 'C++ (GCC 7.4.0)', value: 'cpp_gcc7', id: 52, mode: 'c_cpp' },
  { label: 'C (GCC 8.3.0)', value: 'c_gcc8', id: 49, mode: 'c_cpp' },
  { label: 'C++ (GCC 8.3.0)', value: 'cpp_gcc8', id: 53, mode: 'c_cpp' },
  { label: 'C# (Mono 6.6.0.161)', value: 'csharp', id: 51, mode: 'csharp' },
  { label: 'Go (1.13.5)', value: 'go', id: 60, mode: 'golang' },
  { label: 'Rust (1.40.0)', value: 'rust', id: 73, mode: 'rust' }
])
const allLanguageOptions = languages.value.map(lang => ({ ...lang }))

// 示例代码（根据选择的语言动态生成）
const sampleCode = computed(() => {
  switch (selectedLanguage.value) {
    case 'java':
      return `public class Main {
    public static void main(String[] args) {
        /* 在此处写入代码 */
    }
}`
    case 'python':
      return `def main():
    # 在此处写入代码
    pass

if __name__ == "__main__":
    main()`
    case 'cpp':
    case 'cpp_clang':
    case 'cpp_gcc7':
    case 'cpp_gcc8':
      return `#include <iostream>
using namespace std;

int main() {
    /* 在此处写入代码 */
    return 0;
}`
    case 'javascript':
      return `function main() {
    /* 在此处写入代码 */
}

main();`
    case 'c':
    case 'c_clang':
    case 'c_gcc7':
    case 'c_gcc8':
      return `#include <stdio.h>

int main() {
    /* 在此处写入代码 */
    return 0;
}`
    case 'typescript':
      return `function main(): void {
    /* 在此处写入代码 */
}

main();`
    case 'assembly':
      return `section .text
global _start

_start:
    ; 在此处写入代码
    
    ; 退出程序
    mov eax, 1      ; 系统调用号 (sys_exit)
    xor ebx, ebx    ; 退出代码 0
    int 0x80        ; 调用内核`
    case 'bash':
      return `#!/bin/bash

# 在此处写入代码
echo "Hello, World!"`
    case 'csharp':
      return `using System;

class Program {
    static void Main() {
        // 在此处写入代码
        Console.WriteLine("Hello, World!");
    }
}`
    case 'go':
      return `package main

${"import"} "fmt"

func main() {
    // 在此处写入代码
    fmt.Println("Hello, World!")
}`
    case 'rust':
      return `fn main() {
    // 在此处写入代码
    println!("Hello, World!");
}`
    default:
      return ''
  }
})

// 状态变量
const activeDescriptionTab = ref('description')
const contestWorkspaceTabs = [
  { name: 'answer', label: '答题', icon: '题' },
  { name: 'ranking', label: '排行', icon: '榜' },
  { name: 'submissions', label: '提交', icon: '交' },
  { name: 'details', label: '详情', icon: '详' }
]
const contestWorkspaceTab = ref<'answer' | 'ranking' | 'submissions' | 'details'>('answer')
const contestHeaderCompact = ref(false)
const contestSidebarCollapsed = ref(false)
const selectedLanguage = ref('java')
const showSolution = ref(false)
const solutionsLoadedForProblem = ref<number | null>(null)
const testDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const submitDialogVisible = ref(false)
const testInput = ref('[2, 7, 11, 15]\n9')
const expectedOutput = ref('[0, 1]')
const showExpectedOutput = ref(true)
const testRunning = ref(false)
const submitting = ref(false)
const actualOutput = ref('')
const actualOutputIsError = ref(false)
const testResultMessage = ref('')

type RunSample = {
  key: string
  label: string
  input: string
  expected: string
  explanation?: string
}

const selectedTestSampleKey = ref('public')

// 判断样例是否有实际内容；输入为空但有固定输出时也算有效样例。
const sampleHasContent = (input?: string, expected?: string) => {
  return String(input ?? '').length > 0 || String(expected ?? '').length > 0
}

// 汇总题目的公开样例：优先使用 examples 数组，兼容旧字段 inputExample/debugInputExample。
const testSamples = computed<RunSample[]>(() => {
  const samples: RunSample[] = []
  const rawExamples = Array.isArray(problem.value?.examples) ? problem.value.examples : []

  rawExamples.forEach((example: any, index: number) => {
    const input = String(example?.input ?? example?.inputExample ?? '')
    const expected = String(example?.output ?? example?.expectedOutput ?? example?.outputExample ?? '')
    const explanation = String(example?.explanation ?? example?.explain ?? '')
    if (sampleHasContent(input, expected)) {
      samples.push({
        key: `example-${index + 1}`,
        label: `公开样例 ${index + 1}`,
        input,
        expected,
        explanation
      })
    }
  })

  if (samples.length === 0 && sampleHasContent(problem.value?.inputExample, problem.value?.outputExample)) {
    samples.push({
      key: 'public',
      label: '公开样例 1',
      input: String(problem.value?.inputExample ?? ''),
      expected: String(problem.value?.outputExample ?? '')
    })
  }

  if (sampleHasContent(problem.value?.debugInputExample, problem.value?.debugOutputExample)) {
    const debugInput = String(problem.value?.debugInputExample ?? '')
    const debugExpected = String(problem.value?.debugOutputExample ?? '')
    const duplicated = samples.some(sample => sample.input === debugInput && sample.expected === debugExpected)
    if (!duplicated) {
      samples.push({
        key: 'legacy-debug',
        label: `公开样例 ${samples.length + 1}`,
        input: debugInput,
        expected: debugExpected
      })
    }
  }

  if (samples.length === 0) {
    samples.push({ key: 'manual', label: '空输入', input: '', expected: '' })
  }
  return samples
})

// 当前“运行测试”选中的样例，用户点击公开样例 1/2/N 时会切换这里。
const selectedTestSample = computed(() => {
  return testSamples.value.find(sample => sample.key === selectedTestSampleKey.value) || testSamples.value[0]
})

// 题面只展示真实公开样例，手动空输入兜底样例不出现在题干里。
const displayExamples = computed(() => testSamples.value.filter(sample => sample.key !== 'manual'))

// 题目切换或样例删除后，保证选中的样例 key 仍然存在。
const ensureSelectedTestSample = () => {
  if (!testSamples.value.some(sample => sample.key === selectedTestSampleKey.value)) {
    selectedTestSampleKey.value = testSamples.value[0]?.key || 'public'
  }
}

// 把当前公开样例同步到运行测试面板，做到“题面看到什么，运行测试就能选什么”。
const applySelectedTestSample = (resetResult = true) => {
  ensureSelectedTestSample()
  const sample = selectedTestSample.value
  testInput.value = sample?.input ?? ''
  expectedOutput.value = sample?.expected ?? ''
  if (resetResult) {
    setTestOutput('')
    testResultMessage.value = ''
  }
}

const selectTestSample = (key: string) => {
  selectedTestSampleKey.value = key
  applySelectedTestSample(true)
}

const normalizeOutputForDiff = (value?: string) => String(value ?? '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')

const escapeOutputHtml = (value?: string) => normalizeOutputForDiff(value)
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;')

const getFirstDiffIndex = (expected: string, actual: string) => {
  const max = Math.max(expected.length, actual.length)
  for (let i = 0; i < max; i++) {
    if (expected[i] !== actual[i]) return i
  }
  return -1
}

const getLineColumn = (text: string, index: number) => {
  let line = 1
  let column = 1
  for (let i = 0; i < Math.min(index, text.length); i++) {
    if (text[i] === '\n') {
      line++
      column = 1
    } else {
      column++
    }
  }
  return { line, column }
}

const visibleCharLabel = (ch?: string) => {
  if (ch === undefined) return '缺少字符'
  if (ch === '\n') return '`\\n` 换行'
  if (ch === '\t') return '`\\t` 制表符'
  if (ch === ' ') return '空格'
  return `\`${ch}\``
}

const highlightOutputAt = (text: string, index: number) => {
  const source = normalizeOutputForDiff(text)
  if (index < 0) return escapeOutputHtml(source)
  if (index >= source.length) {
    return `${escapeOutputHtml(source)}<span class="diff-missing">缺少字符</span>`
  }
  const ch = source[index]
  const classes = [
    'diff-highlight',
    ch === ' ' ? 'space-highlight' : '',
    ch === '\n' ? 'newline-highlight' : ''
  ].filter(Boolean).join(' ')
  const display = ch === '\n' ? '↵\n' : ch
  return [
    escapeOutputHtml(source.slice(0, index)),
    `<span class="${classes}">${escapeOutputHtml(display)}</span>`,
    escapeOutputHtml(source.slice(index + 1))
  ].join('')
}

const buildOutputDiffSummary = (expectedRaw: string, actualRaw: string) => {
  const expected = normalizeOutputForDiff(expectedRaw)
  const actual = normalizeOutputForDiff(actualRaw)
  const diffIndex = getFirstDiffIndex(expected, actual)

  if (diffIndex < 0) {
    return {
      hasDiff: false,
      diffIndex,
      message: '',
      expectedHtml: escapeOutputHtml(expected),
      actualHtml: escapeOutputHtml(actual || '无输出')
    }
  }

  const { line, column } = getLineColumn(expected, Math.min(diffIndex, expected.length))
  const expectedChar = expected[diffIndex]
  const actualChar = actual[diffIndex]
  let reason = '输出内容与期望不一致'

  if (expected.trimEnd() === actual.trimEnd() && expected !== actual) {
    reason = '末尾空格或换行不一致'
  } else if (expected.trim() === actual.trim() && expected !== actual) {
    reason = '首尾空格或换行不一致'
  } else if (expected.replace(/\s+/g, '') === actual.replace(/\s+/g, '') && expected !== actual) {
    reason = '空格或换行位置不一致'
  } else if (expected.toLowerCase() === actual.toLowerCase() && expected !== actual) {
    reason = '大小写不一致，评测会区分大小写'
  } else if (
    expectedChar !== undefined &&
    actualChar !== undefined &&
    expectedChar.toLowerCase() === actualChar.toLowerCase() &&
    expectedChar !== actualChar
  ) {
    reason = '大小写不一致，评测会区分大小写'
  } else if ([expectedChar, actualChar].some(ch => ch === ' ' || ch === '\n' || ch === '\t')) {
    reason = '空白字符不一致，可能多了或少了空格/换行'
  }

  return {
    hasDiff: true,
    diffIndex,
    message: `第 ${line} 行第 ${column} 列不同：期望 ${visibleCharLabel(expectedChar)}，实际 ${visibleCharLabel(actualChar)}。${reason}。`,
    expectedHtml: highlightOutputAt(expected, diffIndex),
    actualHtml: actual
      ? highlightOutputAt(actual, diffIndex)
      : '<span class="diff-missing">无输出</span>'
  }
}

const testOutputDiff = computed(() => {
  const expected = expectedOutput.value || ''
  const actual = actualOutput.value || ''
  if (actualOutputIsError.value || !testResultMessage.value || testResultMessage.value.includes('测试通过')) {
    return {
      hasDiff: false,
      message: '',
      expectedHtml: escapeOutputHtml(expected),
      actualHtml: escapeOutputHtml(actual || '无输出')
    }
  }
  return buildOutputDiffSummary(expected, actual)
})

const currentTestContextForAi = () => {
  if (!testResultMessage.value || testRunning.value) return ''
  const expected = normalizeOutputForDiff(expectedOutput.value || '')
  const actual = normalizeOutputForDiff(actualOutput.value || '')
  if (!expected && !actual && !testResultMessage.value) return ''
  const diff = buildOutputDiffSummary(expected, actual)
  return [
    `测试状态: ${testResultMessage.value}`,
    `期望输出:\n${expected || '（空）'}`,
    `实际输出:\n${actual || '（空）'}`,
    diff.hasDiff ? `差异定位: ${diff.message}` : '',
    '请先判断是否为输出格式、大小写、空格或换行问题；只有格式无法解释时再分析算法。'
  ].filter(Boolean).join('\n')
}
const settingsVisible = ref(false)
const editorTheme = ref('ace/theme/monokai')
const fontSize = ref(20)
const lineHeight = ref('1.5')
const fontFamily = ref("'Consolas', 'Monaco', monospace")
const activeCodeTab = ref('java')
const solutionEditorVisible = ref(false)
const solutionSubmitting = ref(false)
const solutionForm = reactive({
  title: '',
  language: 'java',
  description: '',
  code: '',
  sourceSubmissionId: null as number | null,
  sourceRuntime: '',
  sourceMemory: ''
})
const solutionQuillRef = ref()
const solutionImageInput = ref<HTMLInputElement>()
const solutionEditorOptions = {
  placeholder: '直接写你的解题思路即可，不必照模板；也支持代码块、引用、列表和图片',
  theme: 'snow',
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      ['code-block', 'blockquote'],
      [{ header: [2, 3, false] }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      ['link'],
      ['clean']
    ]
  }
}

const EDITOR_SETTINGS_KEY = 'codepower_editor_settings'
const USER_PREFERRED_LANGUAGE_KEY = 'codepower_preferred_language'

// 读取用户最近使用语言；当前题没有历史提交时，用它决定默认模板。
const getStoredPreferredLanguage = () => {
  try {
    const value = localStorage.getItem(USER_PREFERRED_LANGUAGE_KEY)
    return isKnownLanguage(value) ? value : null
  } catch {
    return null
  }
}

// 成功提交后记住当前语言，让下一道新题默认使用用户习惯语言。
const rememberUserPreferredLanguage = (language?: string | null) => {
  if (!isKnownLanguage(language)) return
  try {
    localStorage.setItem(USER_PREFERRED_LANGUAGE_KEY, language as string)
  } catch {}
}

// 保存编辑器主题、字号、行高和字体等本地设置。
const saveEditorSettings = () => {
  localStorage.setItem(EDITOR_SETTINGS_KEY, JSON.stringify({
    theme: editorTheme.value,
    fontSize: fontSize.value,
    lineHeight: lineHeight.value,
    fontFamily: fontFamily.value
  }))
}

// 加载编辑器设置和默认语言偏好。
const loadEditorSettings = () => {
  try {
    const saved = localStorage.getItem(EDITOR_SETTINGS_KEY)
    if (saved) {
      const s = JSON.parse(saved)
      if (s.theme) editorTheme.value = s.theme
      if (s.fontSize) fontSize.value = s.fontSize
      if (s.lineHeight) lineHeight.value = s.lineHeight
      if (s.fontFamily) fontFamily.value = s.fontFamily
    }
    const preferredLanguage = getStoredPreferredLanguage()
    if (preferredLanguage) {
      selectedLanguage.value = preferredLanguage
    }
  } catch {}
}

// AI 辅助功能：抽屉式对话只围绕当前题、当前代码和最近反馈，不把无关历史塞给模型。
type AiChatMessage = {
  role: 'system' | 'user' | 'assistant'
  content: string
}

const showAiDrawer = ref(false)
const aiQuestion = ref('')
const aiLoading = ref(false)
const aiStreaming = ref(false)
const aiStreamStatus = ref('')
const aiConversationId = ref<number | null>(null)
const aiQuotaInfo = ref<{ remaining: number; quota: number; bonus: number; total: number } | null>(null)
const aiModels = ref<{ id: string; name: string; cost: string }[]>([])
const selectedModel = ref<string>('')
const aiModelTouched = ref(false)
const aiAbortController = ref<AbortController | null>(null)
const aiCodeStats = ref({ lines: 0, chars: 0, hasCode: false })
const selectedModelCost = computed(() => {
  const m = aiModels.value.find(m => m.id === selectedModel.value)
  return m ? parseFloat(m.cost) : 1.0
})
const selectedModelCostText = computed(() => Number.isInteger(selectedModelCost.value)
  ? String(selectedModelCost.value)
  : selectedModelCost.value.toFixed(1).replace(/\.0$/, '')
)
const aiTotalAvailable = computed(() => {
  if (!aiQuotaInfo.value) return 0
  if (typeof aiQuotaInfo.value.total === 'number') {
    return Math.max(0, aiQuotaInfo.value.total)
  }
  return Math.max(0, (aiQuotaInfo.value.remaining || 0) + (aiQuotaInfo.value.bonus || 0))
})
const aiMessagesRef = ref<HTMLElement | null>(null)
const aiIntroMessage = '我会按老师带练的方式读取当前题目、公开样例、语言、编辑器代码和最近提交结果。回答会先把题目翻译成输入/输出/变量，再用样例手算，然后检查你当前代码里最值得改的一处；不会直接给完整可提交代码。'
const aiMessages = ref<AiChatMessage[]>([
  { role: 'system', content: aiIntroMessage }
])
let aiSessionPersistTimer: number | undefined

const aiConversationStorageKey = () => `codepower_ai_conversation_problem_${problemId.value}`
const aiSessionStorageKey = () => `codepower_ai_session_problem_${problemId.value}`

// 保存当前题的 AI 会话草稿，防止抽屉关闭或页面刷新后丢失上下文。
const persistAiSessionState = () => {
  try {
    sessionStorage.setItem(aiSessionStorageKey(), JSON.stringify({
      conversationId: aiConversationId.value,
      question: aiQuestion.value,
      messages: aiMessages.value
    }))
  } catch {}
}

// 延迟保存 AI 会话，避免输入时频繁写 sessionStorage。
const scheduleAiSessionPersist = () => {
  if (typeof window === 'undefined') return
  if (aiSessionPersistTimer !== undefined) {
    window.clearTimeout(aiSessionPersistTimer)
  }
  aiSessionPersistTimer = window.setTimeout(() => {
    aiSessionPersistTimer = undefined
    persistAiSessionState()
  }, 120)
}

// 恢复当前题的 AI 会话状态。
const restoreAiSessionState = () => {
  try {
    const saved = sessionStorage.getItem(aiSessionStorageKey())
    if (!saved) return false
    const parsed = JSON.parse(saved)

    if (typeof parsed?.conversationId === 'number' && parsed.conversationId > 0) {
      aiConversationId.value = parsed.conversationId
    } else if (typeof parsed?.conversationId === 'string' && parsed.conversationId.trim()) {
      const conversationId = Number(parsed.conversationId)
      if (!Number.isNaN(conversationId) && conversationId > 0) {
        aiConversationId.value = conversationId
      }
    }

    if (typeof parsed?.question === 'string') {
      aiQuestion.value = parsed.question
    }

    if (Array.isArray(parsed?.messages) && parsed.messages.length > 0) {
      const messages = parsed.messages
        .filter((item: any) => item && (item.role === 'system' || item.role === 'user' || item.role === 'assistant'))
        .map((item: any) => ({ role: item.role, content: typeof item.content === 'string' ? item.content : '' }))

      aiMessages.value = messages.length > 0 && messages[0].role === 'system'
        ? messages
        : [{ role: 'system', content: aiIntroMessage }, ...messages.filter((item: AiChatMessage) => item.role !== 'system')]
    }

    return true
  } catch {
    return false
  }
}

// 判断本地是否已有 AI 对话内容，避免打开抽屉时把用户刚输入的问题清空。
const hasLocalAiConversation = () => {
  return aiMessages.value.some(message => message.role !== 'system' && message.content.trim().length > 0)
    || aiLoading.value
    || aiStreaming.value
    || aiQuestion.value.trim().length > 0
}

// 根据题目、当前代码和最近提交状态生成快捷提问建议。
const dynamicAiSuggestions = computed(() => {
  const title = problem.value?.title ? `《${problem.value.title}》` : '这道题'
  const latestSubmission = userSubmissions.value?.[0]
  const suggestions: string[] = [
    `帮我分析${title}的考点`,
    '帮我分析时间复杂度和空间复杂度',
    aiCodeStats.value.hasCode
      ? '我当前的代码能不能过？先查输出格式和边界'
      : `把${title}拆成输入、输出、要维护的变量`,
    '列 3 个最容易错的边界，并说明为什么'
  ]
  if (latestSubmission?.status && latestSubmission.status !== 'ACCEPTED') {
    suggestions[2] = `我上次提交是${formatStatus(latestSubmission.status)}，先按输出格式和样例差异定位`
  }
  return suggestions
})

// 测试结果
const testResult = reactive({
  success: false,
  status: '',
  score: 0,
  passedTests: 0,
  totalTests: 0,
  output: '',
  runtime: '',
  memory: '',
  language: '',
  code: '',
  createdAt: '',
  submissionId: null as number | null,
  acceptedInsights: null as any,
  testCases: [] as Array<{status: string, executionTime: number, memoryUsed: number, orderNum: number, input?: string, expectedOutput?: string, stdout?: string, actualOutput?: string, errorMessage?: string}>
})

const isContestResultPending = computed(() => (
  isContestMode.value && isPendingSubmissionStatus(testResult.status)
))

const resultStatusText = computed(() => {
  if (isContestResultPending.value) return formatStatus(testResult.status || 'PENDING')
  if (testResult.success) return 'Accepted'
  return testResult.output || 'Wrong Answer'
})

const resultStatusColor = computed(() => {
  if (isContestResultPending.value) return '#e6a23c'
  return testResult.success ? '#4CAF50' : '#f44336'
})

const resultStatusClass = computed(() => ({
  pending: isContestResultPending.value,
  accepted: testResult.success,
  failed: !testResult.success && !isContestResultPending.value
}))

// 提交结果
const submitResultVisible = ref(false)
const submitResult = reactive({
  success: false,
  score: 0,
  passedTests: 0,
  totalTests: 0,
  runtime: '',
  memory: ''
})
const lastSolutionInviteSubmissionId = ref<number | null>(null)

// 竞赛得分百分比统一保留两位小数，便于页面和弹窗展示。
const formatPercent = (value?: number) => {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return '0.00'
  return num.toFixed(2)
}

// Ace 编辑器实例；第三方类型较松，这里用 any 避免模板项目类型过度复杂。
const editor = ref<any>(null)

// 初始化 Ace 编辑器，并加载主题、语言模式、补全和基础编辑体验。
const initAceEditor = async () => {
  try {
    const aceInstance = await loadAceCore()
    await Promise.all([
      loadAceExt(),
      loadAceMode('java'),
      loadAceTheme(editorTheme.value)
    ])

    editor.value = aceInstance.edit('editor')
    editor.value.setTheme(editorTheme.value)
    editor.value.setFontSize(fontSize.value)

    await setEditorLanguage()

    editor.value.setOptions({
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true,
      enableSnippets: true,
      showLineNumbers: true,
      tabSize: 4,
      showPrintMargin: false,
      showInvisibles: true,
      invisibles: {
        "space": "·",
        "tab": "→   ",
        "eol": ""
      },
      useSoftTabs: true,
      navigateWithinSoftTabs: true,
      highlightActiveLine: true,
      wrap: true,
      displayIndentGuides: true,
      behavioursEnabled: true,
      autoScrollEditorIntoView: true,
      copyWithEmptySelection: true,
      useWorker: true,
      fontFamily: fontFamily.value
    })

    changeLineHeight()

    editor.value.on("changeSelection", function() {
      const cursor = editor.value.getCursorPosition()
      const session = editor.value.getSession()
      const currentLine = session.getLine(cursor.row)
      const currentChar = currentLine && currentLine[cursor.column - 1]

      const brackets: Record<string, string> = {
        '(': ')', ')': '(',
        '{': '}', '}': '{',
        '[': ']', ']': '['
      }

      if (currentChar && currentChar in brackets) {
        const range = session.getBracketRange(cursor)
        if (range) {
        }
      }
    })

    editor.value.setValue(sampleCode.value, -1)
  } catch (error) {
    console.error('加载ACE编辑器失败:', error)
  }
}

// 根据用户选择的语言切换 Ace 语法高亮模式。
const setEditorLanguage = async () => {
  if (!editor.value) return

  const selectedLangObj = languages.value.find(lang => lang.value === selectedLanguage.value)
  const mode = selectedLangObj?.mode || 'java'
  await loadAceMode(mode)
  editor.value.session.setMode(`ace/mode/${mode}`)
}

// 获取当前编辑器代码内容。
// 读取编辑器当前代码；若 Ace 尚未初始化则回退到普通 ref 内容。
const getEditorContent = () => {
  return editor.value ? editor.value.getValue() : sampleCode.value
}

// 监听语言改变：普通训练切模板，竞赛恢复/切题时避免误清空历史代码。
watch(selectedLanguage, async () => {
  if (editor.value) {
    await setEditorLanguage()
    if (suppressLanguageTemplateReset || contestRouteHydrating) return
    if (isContestMode.value) {
      if (!editor.value.getValue().trim()) {
        editor.value.setValue(sampleCode.value, -1)
      }
      return
    }
    editor.value.setValue(sampleCode.value, -1)
  }
})

// 获取难度对应的标签类型
const getDifficultyType = (difficulty: string) => {
  switch (difficulty) {
    case '简单': return 'success'
    case '普通': return 'warning'
    case '困难': return 'danger'
    case '极限': return 'danger'
    default: return 'info'
  }
}

// 获取提交状态对应的标签类型
const getSubmissionStatusType = (status: string) => {
  switch (status) {
    case 'ACCEPTED': case '通过': return 'success'
    case 'WRONG_ANSWER': case '错误': case '答案错误': return 'danger'
    case 'TIME_LIMIT_EXCEEDED': case '超时': return 'warning'
    case 'COMPILATION_ERROR': case '编译错误': return 'danger'
    case 'RUNTIME_ERROR': case '运行错误': return 'danger'
    case 'MEMORY_LIMIT_EXCEEDED': case '内存超限': return 'warning'
    case 'PENDING': case 'RUNNING': case '等待评测': case '评测中': return 'warning'
    default: return 'info'
  }
}

// 解锁题解
const unlockSolution = async () => {
  try {
    if (officialSolutions.value.length === 0) {
      showSolution.value = false
      ElMessage.info('当前题目暂无官方题解')
      return
    }
    showSolution.value = true
    ElMessage.success('题解已解锁')
  } catch (error) {
    console.error('解锁题解失败:', error)
    ElMessage.error('解锁题解失败，请稍后再试')
  }
}

// 查看提交详情
const viewSubmissionDetail = async (submission: any) => {
  try {
    const res: any = await submissionApi.getDetail(submission.id)
    const detail = res.data || res
    // 填充到结果弹窗
    testResult.status = detail.status || ''
    testResult.success = detail.status === 'ACCEPTED'
    testResult.score = detail.score || 0
    testResult.output = formatStatus(detail.status)
    testResult.runtime = detail.executionTime != null ? `${detail.executionTime} ms` : '-'
    testResult.memory = detail.memoryUsed != null ? `${(detail.memoryUsed / 1024).toFixed(1)} MB` : '-'
    testResult.language = detail.language || submission.language || ''
    testResult.code = detail.code || ''
    testResult.createdAt = detail.createdAt || submission.createdAt || ''
    testResult.submissionId = detail.id || submission.id || null
    testResult.acceptedInsights = null
    testResult.testCases = Array.isArray(detail.testResults) ? detail.testResults : []
    testResult.passedTests = testResult.testCases.filter(tc => tc.status === 'ACCEPTED').length
    testResult.totalTests = testResult.testCases.length
    resultDialogVisible.value = true
  } catch {
    ElMessage.error('加载提交详情失败')
  }
}

// 管理员、竞赛创建者或本人从排行榜下钻查看选手提交历史。
const openUserSubmissions = async (user: ContestRankingItem) => {
  if (!contestId.value || !user.userId) return
  if (!canViewContestSubmission(user)) {
    ElMessage.warning('普通参赛者只能查看自己的提交记录')
    return
  }
  contestSelectedUser.value = user
  contestUserSubmissionsVisible.value = true
  contestUserSubmissionsLoading.value = true
  contestUserSubmissions.value = []
  try {
    const res: any = await submissionApi.getContestUserSubmissions(contestId.value, Number(user.userId), 1, 50)
    const data = res.data || res || {}
    contestUserSubmissions.value = data.records || []
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.data?.message || '加载选手提交记录失败')
  } finally {
    contestUserSubmissionsLoading.value = false
  }
}

let contestPasteHandler: ((event: ClipboardEvent) => void) | null = null

const removeContestPasteGuard = () => {
  if (contestPasteHandler && editor.value?.container) {
    editor.value.container.removeEventListener('paste', contestPasteHandler, true)
  }
  contestPasteHandler = null
}

// 竞赛模式限制：按竞赛配置过滤语言，并可禁止粘贴。
const applyContestRestrictions = () => {
  if (!contestId.value || !contestInfo.value) return

  languages.value = allLanguageOptions.map(lang => ({ ...lang }))
  removeContestPasteGuard()

  // 过滤允许的编译器
  if (contestInfo.value.allowedLanguages) {
    const allowed = contestInfo.value.allowedLanguages.split(',').map((s: string) => s.trim())
    if (allowed.length > 0 && allowed[0] !== '') {
      languages.value = languages.value.filter(l => allowed.includes(l.value))
      if (languages.value.length > 0 && !languages.value.some(l => l.value === selectedLanguage.value)) {
        selectedLanguage.value = languages.value[0].value
      }
    }
  }

  // 禁止粘贴
  if (contestInfo.value.allowPaste === 0 && editor.value) {
    contestPasteHandler = (event: ClipboardEvent) => {
      event.preventDefault()
      event.stopPropagation()
      ElMessage.warning('竞赛模式下禁止粘贴代码')
    }
    editor.value.container.addEventListener('paste', contestPasteHandler, true)
  }
}

// 打开测试代码对话框
const openTestCodeModal = () => {
  applySelectedTestSample(true)
  testDialogVisible.value = true
}

const setTestOutput = (output: string, isError = false) => {
  actualOutput.value = output
  actualOutputIsError.value = isError
}

// 使用当前样例或自定义输入调试运行，并把输出差异展示给用户。
const runTestWithFixedError = () => {
  if (testRunning.value) return;
  ensureSelectedTestSample()
  const sample = selectedTestSample.value

  // 检查题目ID是否有效
  if (!problemId.value) {
    ElMessage.error('题目ID无效，无法运行测试');
    console.error('题目ID无效:', problemId.value);

    // 显示错误信息
    testRunning.value = false;
    testResultMessage.value = '✗ 测试失败! 题目ID无效';
    setTestOutput('题目ID无效，无法运行测试', true);
    testDialogVisible.value = true;
    return;
  }

  testRunning.value = true;
  testResultMessage.value = '';
  setTestOutput('');
  testInput.value = sample?.input ?? '';
  expectedOutput.value = sample?.expected ?? '';
  
  // 获取编辑器中的代码。
  const code = getEditorContent();
  
  // 获取当前选择的语言 ID，后端会转换为 Judge0 语言编号。
  const languageId = getLanguageId(selectedLanguage.value);
  
  // 构建样例运行请求，竞赛模式会额外携带 contestId 以便后端记录上下文。
  const params = {
    code,
    languageId,
    language: selectedLanguage.value,
    stdin: sample?.input ?? '',
    expectedOutput: sample?.expected ?? '',
    timeLimit: problem.value.timeLimit,
    memoryLimit: problem.value.memoryLimit,
    ...(contestId.value ? { contestId: contestId.value } : {})
  };
  
  console.log('发送测试请求:', params);
  console.log('题目ID:', problemId.value);
  
  // 调用后端样例运行 API：这里不会写入正式提交记录，只用于调试公开样例。
  problemApi.testCode(problemId.value, params)
    .then(response => {
      testRunning.value = false;
      console.log('测试结果:', response);
      
      // 解析响应数据
      const {
        statusId,
        statusDescription,
        stdout,
        stderr,
        compile_output,
        time,
        memory,
        stdin_decoded,
        expectedOutput_decoded,
        originalCode
      } = response;
      
      // 处理输入和期望输出
      const inputText = stdin_decoded !== undefined ? stdin_decoded : (sample?.input ?? '');
      const expectedText = expectedOutput_decoded !== undefined ? expectedOutput_decoded : (sample?.expected ?? '');
      
      testInput.value = inputText;
      // 设置期望输出
      expectedOutput.value = expectedText;
      
      // 处理实际输出
      let outputText = '';
      if (stdout !== undefined && stdout !== null) {
        outputText = stdout;
      }

      // 检查编译错误
      const compileError = compile_output || '';

      // 检查运行时错误
      const runtimeError = stderr || '';

      // 判断测试是否成功
      if (statusId === 3) {
        // 测试通过
        setTestOutput(outputText);
        testResultMessage.value = '✓ 测试通过! 您的代码运行正确。';
      } else {
        // 编译错误
        if (statusId === 6 || compileError) {
          setTestOutput(compileError || statusDescription || '编译错误', true);
          testResultMessage.value = `✗ 编译错误! ${statusDescription || '请检查您的代码。'}`;
          testDialogVisible.value = true;
          return;
        }

        // 时间限制超出
        if (statusId === 5) {
          setTestOutput(outputText || '执行超时', true);
          testResultMessage.value = '✗ 超时! 您的代码执行时间超出限制。';
          testDialogVisible.value = true;
          return;
        }

        // 运行时错误 / 内存超限 / 系统信号类错误
        if ((statusId >= 7 && statusId <= 12) || runtimeError) {
          setTestOutput(runtimeError || statusDescription || '运行时错误', true);
          testResultMessage.value = `✗ 运行时错误! ${statusDescription || '请检查您的代码。'}`;
          testDialogVisible.value = true;
          return;
        }

        // 其他情况：输出与期望不匹配
        if (outputText === '') {
          setTestOutput('');
        } else {
          setTestOutput(outputText);
        }
        testResultMessage.value = `✗ 测试失败! ${statusDescription || '输出结果与预期不符'}。`;
      }
      
      // 打开测试对话框显示结果
      testDialogVisible.value = true;
    })
    .catch(error => {
      testRunning.value = false;
      console.error('测试代码失败:', error);

      // 错误处理
      let errorMessage = '服务器错误，请稍后再试。';

      // 尝试提取具体错误信息
      if (error.response && error.response.data) {
        errorMessage = error.response.data.error || error.response.data.message || errorMessage;
      }

      testResultMessage.value = `✗ 测试失败! ${errorMessage}`;
      setTestOutput(`请求失败: ${errorMessage}`, true);

      // 打开测试对话框显示错误
      testDialogVisible.value = true;
    });
}

// 打开测试对话框并自动运行测试
const openTestDialog = () => {
  testDialogVisible.value = true;
  // 延迟一下再运行测试，确保对话框已经打开
  setTimeout(() => {
    runTestWithFixedError();
  }, 100);
}

// 保留兼容性
const runTest = openTestDialog;

// 修正 AI 返回 Markdown 的标题和列表格式，保证讲解在抽屉中更容易阅读。
const normalizeAiMarkdown = (text: string) => {
  let source = (text || '')
    .replace(/\r\n/g, '\n')
    .replace(/^\s*\*([^*\n]{2,48})\*\s*$/gm, '**$1**')
    .replace(/^\s*(先看题|这题在做什么|解题思路|考点分析|当前语言怎么做|复杂度结论|复杂度分析|复杂度|为什么不是更高|样例推演|走样例|推演结论|代码检查|看你的代码|可能出错的原因|怎么验证|下一步|新手第一步|易错点|最容易错的边界|最小反例|边界情况处理|边界检查|关键状态变化|初始化|状态转移方程|核心思路)\s*[:：]?\s*$/gm, '### $1')
    .replace(/^\s*(\d+)\.\s*([^。\n]{2,34}(?:（[^）]{1,18}）|\([^)]{1,18}\))?)\s*$/gm, '#### $1. $2')
    .replace(/([^\n])\n(#{3,4}\s)/g, '$1\n\n$2')
    .replace(/(#{3,4}\s[^\n]+)\n(?!\n)/g, '$1\n\n')
    .replace(/([^\n])\n(-\s|\d+\.\s)/g, '$1\n\n$2')
    .trim()
  return source
}

// 将 AI Markdown 转成 HTML；解析失败时保留原文本，避免页面空白。
const renderMarkdown = (text: string) => {
  if (!text) return ''
  try {
    return marked.parse(normalizeAiMarkdown(text), { gfm: true, breaks: false }) as string
  } catch {
    return text
  }
}

// 兼容 HTML 题解和 Markdown 题解两种内容来源。
const renderRichContent = (text: string) => {
  if (!text) return ''
  return /<\/?[a-z][\s\S]*>/i.test(text) ? text : renderMarkdown(text)
}

// 提取富文本纯文本，用于判断题解正文是否为空。
const stripHtmlText = (html: string) => {
  const div = document.createElement('div')
  div.innerHTML = html || ''
  return div.textContent?.trim() || ''
}

// 复制代码或题解内容，浏览器剪贴板不可用时回退到 textarea 方案。
const copyText = async (text: string, successMessage = '已复制') => {
  if (!text) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
  ElMessage.success(successMessage)
}

// 统一兼容不同接口字段中的题解作者名称。
const getSolutionAuthorName = (solution: any) => {
  return solution?.username || solution?.userName || solution?.authorName || '未知用户'
}

// 从题解作者跳转到用户主页。
const goToUserProfile = (userId?: number | string) => {
  const targetUserId = Number(userId)
  if (!Number.isFinite(targetUserId) || targetUserId <= 0) return
  router.push(targetUserId === Number(currentUserId.value) ? '/profile' : `/users/${targetUserId}`)
}

// 题解图片上传前压缩成 base64，避免富文本内容过大。
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
      let width = img.width
      let height = img.height
      if (width > maxWidth) {
        height = Math.round(height * maxWidth / width)
        width = maxWidth
      }
      canvas.width = width
      canvas.height = height
      canvas.getContext('2d')?.drawImage(img, 0, 0, width, height)
      let quality = 0.82
      let result = canvas.toDataURL('image/jpeg', quality)
      while (result.length > maxSize * 1.37 && quality > 0.32) {
        quality -= 0.1
        result = canvas.toDataURL('image/jpeg', quality)
      }
      resolve(result)
    }
    img.onerror = reject
    img.src = url
  })
}

// 触发题解编辑器中的隐藏图片上传控件。
const insertSolutionImage = () => {
  solutionImageInput.value?.click()
}

// 校验并压缩题解图片，然后插入 Quill 当前光标位置。
const handleSolutionImageUpload = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }

  try {
    const dataUrl = await compressImage(file)
    const quill = solutionQuillRef.value?.getQuill()
    if (quill) {
      const range = quill.getSelection(true)
      quill.insertEmbed(range.index, 'image', dataUrl)
      quill.setSelection(range.index + 1)
      solutionForm.description = quill.root.innerHTML
    }
  } catch {
    ElMessage.error('图片处理失败')
  } finally {
    if (solutionImageInput.value) solutionImageInput.value.value = ''
  }
}

// 兼容 axios 拦截器和部分旧接口返回格式，统一取出业务数据。
const extractApiPayload = (payload: any) => payload?.data ?? payload ?? {}

// 把后端记录的 KB 内存换算成 MB 文本。
const formatSubmissionMemory = (memoryUsed?: number | null) => {
  return memoryUsed != null ? `${(Number(memoryUsed) / 1024).toFixed(1)} MB` : ''
}

// 从提交详情中提取可发布题解的 AC 代码来源。
const buildAcceptedSourceFromDetail = (detail: any, fallback?: any) => {
  const data = detail || {}
  const id = data.id || fallback?.id
  if (!id || (data.status || fallback?.status) !== 'ACCEPTED') return null
  return {
    id,
    code: data.code || fallback?.code || '',
    language: data.language || fallback?.language || selectedLanguage.value,
    executionTime: data.executionTime ?? fallback?.executionTime ?? null,
    memoryUsed: data.memoryUsed ?? fallback?.memoryUsed ?? null
  }
}

// 优先使用刚刚通过的提交，否则从历史记录中找最近一次日常 AC。
const findLatestAcceptedSubmission = () => {
  if (testResult.success && testResult.submissionId) {
    return buildAcceptedSourceFromDetail({
      id: testResult.submissionId,
      code: testResult.code,
      language: testResult.language || selectedLanguage.value,
      status: 'ACCEPTED'
    })
  }
  return userSubmissions.value.find(item => item.status === 'ACCEPTED' && !item.contestId) || null
}

// 发布社区题解前重新拉取 AC 提交详情，保证题解绑定的是服务端保存的通过代码。
const resolveAcceptedSolutionSource = async () => {
  if (isContestMode.value) {
    ElMessage.warning('竞赛提交暂不作为社区题解来源，请在题库模式下通过后再分享')
    return null
  }

  const latest = findLatestAcceptedSubmission()
  if (!latest) {
    ElMessage.warning('请先提交并通过本题，再分享题解')
    return null
  }

  try {
    const detailRes: any = await submissionApi.getDetail(latest.id)
    const detail = extractApiPayload(detailRes)
    const accepted = buildAcceptedSourceFromDetail(detail, latest)
    if (!accepted?.code) {
      ElMessage.warning('没有找到可用于题解的 AC 提交代码，请重新提交通过后再试')
      return null
    }
    return accepted
  } catch (error) {
    console.error('加载AC提交失败:', error)
    ElMessage.error('加载AC提交失败，请稍后再试')
    return null
  }
}

// 把 AC 提交代码、语言、耗时和内存同步到题解发布表单。
const applyAcceptedSourceToSolution = (source: any) => {
  solutionForm.sourceSubmissionId = Number(source.id)
  solutionForm.code = source.code || ''
  solutionForm.language = source.language || selectedLanguage.value
  solutionForm.sourceRuntime = source.executionTime != null ? `${source.executionTime} ms` : ''
  solutionForm.sourceMemory = formatSubmissionMemory(source.memoryUsed)
}

// 用户点击“同步 AC 代码”时，从最近 AC 提交回填题解表单。
const syncCurrentCodeToSolution = async () => {
  const source = await resolveAcceptedSolutionSource()
  if (!source) return
  applyAcceptedSourceToSolution(source)
  ElMessage.success('已同步最近一次AC提交代码')
}

// 正式提交代码：普通训练同步拿结果，竞赛模式先返回 PENDING 再后台轮询。
const submitCode = async () => {
  submitting.value = true
  submitDialogVisible.value = false

  const code = getEditorContent()
  const languageId = getLanguageId(selectedLanguage.value)

  if (isContestMode.value) {
    // 竞赛提交走异步队列：先显示 PENDING，后台评测完成后再刷新题目状态和得分。
    resultDialogVisible.value = true
    prepareContestPendingResult(code, selectedLanguage.value, null, 'PENDING')
    try {
      const response: any = await problemApi.submitCodeAsync(problemId.value, {
        code,
        languageId,
        language: selectedLanguage.value,
        contestId: contestId.value
      })
      const data = response.data || response
      const submissionId = Number(data.submissionId)
      const pendingRecord = {
        id: submissionId,
        problemId: problemId.value,
        language: selectedLanguage.value,
        status: data.status || 'PENDING',
        score: 0,
        executionTime: null,
        memoryUsed: null,
        contestId: contestId.value,
        createdAt: new Date().toISOString()
      }

      prepareContestPendingResult(code, selectedLanguage.value, submissionId, data.status || 'PENDING')
      cacheContestDraftFromSubmit(
        code,
        selectedLanguage.value,
        Number(problemId.value),
        Number(contestId.value),
        submissionId,
        data.status || 'PENDING'
      )
      userSubmissions.value = upsertSubmissionRecord(userSubmissions.value, pendingRecord)
      contestSubmissions.value = upsertSubmissionRecord(contestSubmissions.value, pendingRecord)
      const currentProgress = getContestProblemProgress(Number(problemId.value))
      mergeContestProblemProgress(Number(problemId.value), {
        status: currentProgress.status === 'ACCEPTED' ? 'ACCEPTED' : 'PENDING',
        attempts: Math.max(Number(currentProgress.attempts || 0), 1),
        latestStatus: data.status || 'PENDING',
        lastSubmissionAt: pendingRecord.createdAt
      })
      trackPendingContestSubmission(submissionId)
      rememberUserPreferredLanguage(selectedLanguage.value)
      submitting.value = false
      ElMessage.success('已提交，后台评测中')
      void loadContestWorkspace()
    } catch (error: any) {
      testResult.success = false
      testResult.status = ''
      testResult.score = 0
      testResult.passedTests = 0
      testResult.totalTests = 0
      testResult.output = error?.response?.data?.message || '提交失败，请稍后重试'
      testResult.runtime = '-'
      testResult.memory = '-'
      testResult.language = selectedLanguage.value
      testResult.code = code
      testResult.createdAt = new Date().toLocaleString()
      testResult.submissionId = null
      testResult.acceptedInsights = null
      testResult.testCases = []
      resultDialogVisible.value = true
    } finally {
      submitting.value = false
    }
    return
  }

  // 普通训练直接等待后端同步返回全部测试点结果，同时触发画像、任务和题解邀请。
  resultDialogVisible.value = true

  try {
    const response: any = await problemApi.submitCode(problemId.value, {
      code,
      languageId,
      language: selectedLanguage.value,
      ...(contestId.value ? { contestId: contestId.value } : {})
    })

    submitting.value = false
    const data = response.data || response

    testResult.status = data.status || ''
    testResult.success = data.success
    testResult.score = data.score || 0
    testResult.passedTests = data.passedTests || 0
    testResult.totalTests = data.totalTests || 0
    testResult.output = data.status === 'ACCEPTED' ? '全部通过' : formatStatus(data.status)
    testResult.runtime = data.executionTime != null ? `${data.executionTime} ms` : '-'
    testResult.memory = data.memoryUsed != null ? `${(data.memoryUsed / 1024).toFixed(1)} MB` : '-'
    testResult.language = selectedLanguage.value
    testResult.code = code
    testResult.createdAt = new Date().toLocaleString()
    testResult.submissionId = data.submissionId || null
    testResult.acceptedInsights = data.acceptedInsights || null
    testResult.testCases = Array.isArray(data.testResults) ? data.testResults : []
    rememberUserPreferredLanguage(selectedLanguage.value)

    if (testResult.submissionId) {
      userSubmissions.value = [{
        id: testResult.submissionId,
        problemId: problemId.value,
        language: selectedLanguage.value,
        status: data.status,
        score: testResult.score,
        executionTime: data.executionTime,
        memoryUsed: data.memoryUsed,
        contestId: contestId.value || null,
        createdAt: new Date().toISOString()
      }, ...userSubmissions.value.filter(item => item.id !== testResult.submissionId)]
    }

    // 提交后从 API 重新加载历史，确保通过状态、测试点和画像触发结果都是后端最终值。
    await loadSubmissions()
    if (isContestMode.value) {
      await loadContestWorkspace()
    }
    maybePromptSolutionInvite()
  } catch (error: any) {
    submitting.value = false
    testResult.success = false
    testResult.status = ''
    testResult.score = 0
    testResult.passedTests = 0
    testResult.totalTests = 0
    testResult.output = error?.response?.data?.message || '提交失败，请稍后重试'
    testResult.runtime = '-'
    testResult.memory = '-'
    testResult.language = selectedLanguage.value
    testResult.code = code
    testResult.createdAt = new Date().toLocaleString()
    testResult.submissionId = null
    testResult.acceptedInsights = null
    testResult.testCases = []
  }
}

// AI 消息变化后滚动到底部，保持流式输出可见。
const scrollAiToBottom = () => {
  nextTick(() => {
    if (aiMessagesRef.value) {
      aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
    }
  })
}

// 重置 AI 对话状态，但会尝试保留后端会话 ID，方便继续同一题上下文。
const resetAiConversationState = () => {
  aiAbortController.value?.abort()
  aiAbortController.value = null
  aiLoading.value = false
  aiStreaming.value = false
  aiStreamStatus.value = ''
  aiConversationId.value = null
  aiMessages.value = [{ role: 'system', content: aiIntroMessage }]
  try {
    const saved = localStorage.getItem(aiConversationStorageKey())
    if (saved) aiConversationId.value = Number(saved) || null
  } catch {}
  scheduleAiSessionPersist()
}

// 统计当前代码行数和字符数，帮助 AI 判断用户是否已经写了有效代码。
const refreshAiCodeStats = () => {
  const code = getEditorContent() || ''
  const normalizedCode = code.replace(/\s+/g, '')
  const normalizedTemplate = (sampleCode.value || '').replace(/\s+/g, '')
  const hasCode = !!normalizedCode && normalizedCode !== normalizedTemplate
  aiCodeStats.value = {
    lines: code ? code.split(/\r?\n/).length : 0,
    chars: code.length,
    hasCode
  }
}

// 确保当前题有一个 AI 对话，后端会用它绑定题目上下文。
const ensureAiConversation = async () => {
  if (aiConversationId.value) return aiConversationId.value
  const convRes: any = await aiApi.createConversation({
    title: `题目${problemId.value}讨论`,
    problemId: problemId.value,
    type: 'PROBLEM'
  })
  aiConversationId.value = (convRes.data || convRes).id
  localStorage.setItem(aiConversationStorageKey(), String(aiConversationId.value))
  return aiConversationId.value
}

// 从后端拉取已有 AI 消息，刷新后仍能看到同一题的历史对话。
const hydrateAiMessages = async () => {
  if (!aiConversationId.value) return
  try {
    const res: any = await aiApi.getMessages(aiConversationId.value)
    const messages = (res.data || res || [])
      .filter((item: any) => item.role === 'user' || item.role === 'assistant')
      .map((item: any) => ({ role: item.role, content: item.content || '' }))
    aiMessages.value = [{ role: 'system', content: aiIntroMessage }, ...messages]
    scheduleAiSessionPersist()
    scrollAiToBottom()
  } catch {
    localStorage.removeItem(aiConversationStorageKey())
    aiConversationId.value = null
  }
}

// 把网关超时、用户停止、积分不足等错误转换成用户看得懂的提示。
const formatAiFailureMessage = (e: any, aborted: boolean) => {
  if (aborted) return '已停止生成。'
  const raw = e?.response?.data?.message || e?.message || '网络错误，请稍后重试。'
  if (e?.response?.status === 502 || String(raw).includes('502')) {
    return 'AI服务网关暂时没有拿到模型响应（502）。本次不会扣除积分，可以稍后直接重试。'
  }
  if (e?.response?.status === 504 || String(raw).includes('504')) {
    return 'AI模型响应超时（504）。本次不会扣除积分，可以换轻量模型或稍后重试。'
  }
  return raw
}

watch(aiQuestion, () => {
  scheduleAiSessionPersist()
})

// 向 AI 提问：携带当前题、当前代码、语言和最近一次样例测试反馈。
const askAi = async () => {
  if (!aiQuestion.value.trim() || aiLoading.value) return

  const question = aiQuestion.value
  aiMessages.value.push({ role: 'user', content: question })
  aiQuestion.value = ''
  scheduleAiSessionPersist()
  aiLoading.value = true
  aiStreaming.value = false
  aiStreamStatus.value = '老师正在整理题面、样例和你的代码'
  scrollAiToBottom()

  let assistantMessageIndex = -1
  try {
    const conversationId = await ensureAiConversation()
    refreshAiCodeStats()
    const currentCode = getEditorContent()
    const currentLang = selectedLanguage.value
    assistantMessageIndex = aiMessages.value.push({ role: 'assistant', content: '' }) - 1
    scheduleAiSessionPersist()
    scrollAiToBottom()

    aiAbortController.value = new AbortController()
    await aiApi.chatStream({
      conversationId,
      message: question,
      model: selectedModel.value || undefined,
      userCode: currentCode || undefined,
      language: currentLang || undefined,
      testContext: currentTestContextForAi() || undefined
    }, {
      onOpen: () => {
        aiStreamStatus.value = '流式通道已建立，等待第一段讲解'
        scheduleAiSessionPersist()
        scrollAiToBottom()
      },
      onState: (message: string) => {
        aiStreamStatus.value = message || '正在等待第一段讲解'
        scheduleAiSessionPersist()
        scrollAiToBottom()
      },
      onDelta: (content: string) => {
        aiStreaming.value = true
        aiStreamStatus.value = '正在逐段返回讲解'
        const assistantMessage = aiMessages.value[assistantMessageIndex]
        if (assistantMessage) assistantMessage.content += content
        scheduleAiSessionPersist()
        scrollAiToBottom()
      },
      onDone: (data: any) => {
        const assistantMessage = aiMessages.value[assistantMessageIndex]
        if (assistantMessage && !assistantMessage.content && data?.content) {
          assistantMessage.content = data.content
        }
        aiStreamStatus.value = ''
        scheduleAiSessionPersist()
      }
    }, aiAbortController.value.signal)
  } catch (e: any) {
    const aborted = e?.name === 'AbortError'
    const errMsg = formatAiFailureMessage(e, aborted)
    if (e.response?.status === 403 && errMsg.includes('AI')) {
      aiMessages.value.push({ role: 'assistant', content: `${errMsg}\n\n前往个人中心签到或完成每日任务可获取更多积分。` })
    } else if (assistantMessageIndex >= 0 && aiMessages.value[assistantMessageIndex] && !aiMessages.value[assistantMessageIndex].content) {
      aiMessages.value[assistantMessageIndex].content = errMsg
    } else {
      aiMessages.value.push({ role: 'assistant', content: errMsg })
    }
    scheduleAiSessionPersist()
  } finally {
    aiLoading.value = false
    aiStreaming.value = false
    aiStreamStatus.value = ''
    aiAbortController.value = null
    loadAiQuota()
    scheduleAiSessionPersist()
    scrollAiToBottom()
  }
}

// 停止当前流式回答，对应 AbortController 取消 fetch。
const stopAiStream = () => {
  aiAbortController.value?.abort()
}

// 打开 AI 抽屉前刷新代码统计，让 AI 提示能知道用户是否已写代码。
const openAiDrawer = () => {
  refreshAiCodeStats()
  showAiDrawer.value = true
}

onUnmounted(() => {
  stopAiStream()
  if (aiSessionPersistTimer !== undefined) {
    window.clearTimeout(aiSessionPersistTimer)
    aiSessionPersistTimer = undefined
  }
  persistAiSessionState()
})

// 加载用户 AI 额度和可用模型；失败时不阻塞做题主流程。
const loadAiQuota = async () => {
  try {
    const res: any = await import('@/api/level').then(m => m.levelApi.getLevelInfo())
    const data = res.data || res
    const remaining = Number(data.dailyAiRemaining || 0)
    const quota = Number(data.dailyAiQuota || 0)
    const bonus = Number(data.bonusAiPoints || 0)
    const total = typeof data.totalAiRemaining === 'number'
      ? data.totalAiRemaining
      : remaining + bonus
    aiQuotaInfo.value = { remaining, quota, bonus, total }
  } catch { /* 忽略额度加载失败 */ }
  if (aiModels.value.length === 0) {
    try {
      const res: any = await aiApi.getModels()
      const list = res.data || res
      if (Array.isArray(list)) {
        aiModels.value = [...list].sort((a, b) => {
          const aCost = Number.parseFloat(a.cost || '1')
          const bCost = Number.parseFloat(b.cost || '1')
          return aCost - bCost
        })
        if (!aiModelTouched.value && aiModels.value.length > 0) {
          selectedModel.value = pickPreferredAiModel(aiModels.value)
        }
      }
    } catch { /* 忽略模型列表加载失败 */ }
  }
}

// 默认选择轻量、响应更稳的模型，用户仍可在抽屉中手动切换。
const pickPreferredAiModel = (models: Array<{ id: string; name: string }>) => {
  return models.find(m => m.id.includes('Qwen3-8B') || m.name.includes('Qwen3 8B'))?.id
    || models.find(m => m.id.includes('THUDM/GLM-4-9B') || m.name.includes('GLM-4 9B'))?.id
    || models.find(m => m.id.includes('DeepSeek-V3') && !m.id.includes('V3.2'))?.id
    || models.find(m => m.id.includes('Qwen3-32B'))?.id
    || models[0]?.id
}

// 使用 AI 快捷建议：把建议填入输入框并直接发起提问。
const useAiSuggestion = (suggestion: string) => {
  aiQuestion.value = suggestion;
  askAi();
}

watch(showAiDrawer, async (val) => {
  if (val) {
    refreshAiCodeStats()
    const restored = hasLocalAiConversation() || restoreAiSessionState()
    if (!restored) {
      resetAiConversationState()
    }
    await loadAiQuota()
    if (aiConversationId.value && !hasLocalAiConversation()) {
      await hydrateAiMessages()
    }
    scheduleAiSessionPersist()
  }
})

watch(activeDescriptionTab, (tab) => {
  if (tab === 'solution') {
    fetchSolutionData()
  }
})

watch(() => route.params.id, async (val) => {
  if (isContestMode.value) {
    contestRouteHydrating = true
  }

  try {
    problemId.value = Number(val) || 0
    resetAiConversationState()
    if (isContestMode.value) {
      contestWorkspaceTab.value = 'answer'
      activeDescriptionTab.value = 'description'
      const restored = await restoreContestEditorFromCache(problemId.value, contestId.value)
      if (!restored) {
        clearEditorDuringContestRouteChange()
      }
    }
    await reloadProblemWorkspace(false)
    await restoreEditorForWorkspace()
  } finally {
    contestRouteHydrating = false
  }
})

watch(() => route.query.contestId, async () => {
  contestRouteHydrating = true
  try {
    await loadContestInfo()
    if (isContestMode.value) {
      contestWorkspaceTab.value = 'answer'
      activeDescriptionTab.value = 'description'
      startContestClock()
    } else {
      stopContestClock()
    }
    await restoreEditorForWorkspace()
  } finally {
    contestRouteHydrating = false
  }
})

// 打开设置菜单
const openSettingsMenu = () => {
  settingsVisible.value = true
}

// 改变主题
const changeTheme = async () => {
  if (!editor.value) return
  await loadAceTheme(editorTheme.value)
  editor.value.setTheme(editorTheme.value)
  saveEditorSettings()
}

// 改变字体大小
const changeFontSize = () => {
  if (!editor.value) return
  editor.value.setFontSize(fontSize.value)
  saveEditorSettings()
}

// 改变行高
const changeLineHeight = () => {
  if (!editor.value) return
  const container = editor.value.container
  container.style.lineHeight = lineHeight.value
  const textLayer = container.querySelector('.ace_text-layer')
  if (textLayer) {
    textLayer.style.lineHeight = lineHeight.value
  }
  saveEditorSettings()
}

const changeFontFamily = () => {
  if (!editor.value) return
  editor.value.setOption('fontFamily', fontFamily.value)
  saveEditorSettings()
}

// 运行代码（提交代码）
const runCode = () => {
  // 打开确认对话框
  submitDialogVisible.value = true;
}

// 获取语言展示名称
const getLanguageLabel = (lang) => {
  const langObj = languages.value.find(item => item.value === lang);
  return langObj ? langObj.label : lang;
}

// 解析原始题解字符串
const parseSolution = (solutionHtml) => {
  if (!solutionHtml) return { thinking: '', code: {} };
  
  // 创建一个临时的DOM元素来解析HTML
  const tempDiv = document.createElement('div');
  tempDiv.innerHTML = solutionHtml;
  
  // 提取思路部分（方法一和方法二的描述）
  const thinking = [];
  const methods = tempDiv.querySelectorAll('h3');
  methods.forEach(method => {
    thinking.push(`<h3>${method.textContent}</h3>`);
    let currentNode = method.nextSibling;
    while (currentNode && currentNode.nodeName !== 'H3' && currentNode.nodeName !== 'PRE') {
      if (currentNode.outerHTML) thinking.push(currentNode.outerHTML);
      currentNode = currentNode.nextSibling;
    }
  });
  
  // 提取代码部分
  const codes = {};
  const preElements = tempDiv.querySelectorAll('pre');
  preElements.forEach((pre, index) => {
    const code = pre.textContent.trim();
    if (code.includes('class Solution')) {
      if (code.includes('public class Solution')) {
        codes.java = code;
      } else if (code.includes('HashMap')) {
        codes.java = code;
      }
    }
  });
  
  // 如果解析出Java代码，为其他语言生成类似代码
  if (codes.java) {
    codes.python = `
class Solution:
    def twoSum(self, nums, target):
        # 方法一：暴力枚举
        n = len(nums)
        for i in range(n):
            for j in range(i + 1, n):
                if nums[i] + nums[j] == target:
                    return [i, j]
        return []
        
        # 方法二：哈希表
        # hashtable = {}
        # for i, num in enumerate(nums):
        #     if target - num in hashtable:
        #         return [hashtable[target - num], i]
        #     hashtable[num] = i
        # return []`;
    
    codes.cpp = `
class Solution {
public:
    vector<int> twoSum(vector<int>& nums, int target) {
        // 方法一：暴力枚举
        int n = nums.size();
        for (int i = 0; i < n; ++i) {
            for (int j = i + 1; j < n; ++j) {
                if (nums[i] + nums[j] == target) {
                    return {i, j};
                }
            }
        }
        return {};
        
        // 方法二：哈希表
        // unordered_map<int, int> hashtable;
        // for (int i = 0; i < nums.size(); ++i) {
        //     auto it = hashtable.find(target - nums[i]);
        //     if (it != hashtable.end()) {
        //         return {it->second, i};
        //     }
        //     hashtable[nums[i]] = i;
        // }
        // return {};
    }
};`;
    
    codes.javascript = `
/**
 * @param {number[]} nums
 * @param {number} target
 * @return {number[]}
 */
var twoSum = function(nums, target) {
    // 方法一：暴力枚举
    const n = nums.length;
    for (let i = 0; i < n; ++i) {
        for (let j = i + 1; j < n; ++j) {
            if (nums[i] + nums[j] === target) {
                return [i, j];
            }
        }
    }
    return [];
    
    // 方法二：哈希表
    // const map = new Map();
    // for (let i = 0; i < nums.length; ++i) {
    //     if (map.has(target - nums[i])) {
    //         return [map.get(target - nums[i]), i];
    //     }
    //     map.set(nums[i], i);
    // }
    // return [];
};`;
  }
  
  return {
    thinking: thinking.join(''),
    code: codes
  };
};

// 解析题解内容
const parsedSolution = parseSolution(problem.solution);
problem.solutionThinking = parsedSolution.thinking;
problem.solutionCode = parsedSolution.code;

const buildSolutionOutline = () => `
  <p><strong>关键观察：</strong></p>
  <p><strong>核心思路：</strong></p>
  <p><strong>复杂度分析：</strong></p>
  <p><strong>注意事项：</strong></p>
`

// 打开社区题解编辑器
const openSolutionEditor = async () => {
  if (!localStorage.getItem('authToken')) {
    ElMessage.warning('请先登录后再分享解法')
    return
  }

  const source = await resolveAcceptedSolutionSource()
  if (!source) return

  solutionForm.title = `题解：${problem.value?.title || `题目 ${problemId.value}`}（${getLanguageLabel(selectedLanguage.value)}）`
  solutionForm.description = ''
  applyAcceptedSourceToSolution(source)
  solutionEditorVisible.value = true
}

const applySolutionOutline = () => {
  if (!solutionForm.description || !solutionForm.description.trim()) {
    solutionForm.description = buildSolutionOutline()
    return
  }
  const current = stripHtmlText(solutionForm.description)
  if (!current || current.length < 12) {
    solutionForm.description = buildSolutionOutline()
  }
}

const openSolutionEditorFromAcceptedInvite = async () => {
  if (isContestMode.value) return
  await openSolutionEditor()
  if (solutionEditorVisible.value && testResult.acceptedInsights?.solutionInvite?.recommended) {
    solutionForm.title = `高效题解：${problem.value?.title || `题目 ${problemId.value}`}（${getLanguageLabel(selectedLanguage.value)}）`
  }
}

const maybePromptSolutionInvite = () => {
  if (isContestMode.value) return
  const invite = testResult.acceptedInsights?.solutionInvite
  if (!testResult.success || !invite?.recommended || !testResult.submissionId) return
  if (lastSolutionInviteSubmissionId.value === testResult.submissionId) return
  lastSolutionInviteSubmissionId.value = testResult.submissionId

  ElMessageBox.confirm(
    `这次提交在本题历史通过记录里表现不错。要不要整理成题解？符合条件时可获得 ${invite.expReward || 30} 经验 + ${invite.aiPointsReward || 3} AI积分。`,
    '要不要分享这份解法？',
    {
      confirmButtonText: '去发布题解',
      cancelButtonText: '先不了',
      type: 'success'
    }
  ).then(() => {
    openSolutionEditorFromAcceptedInvite()
  }).catch(() => {})
}

// 提交题解到服务器
const submitSolution = async () => {
  if (!solutionForm.title.trim()) {
    ElMessage.warning('请填写题解标题')
    return
  }
  if (!solutionForm.sourceSubmissionId) {
    ElMessage.warning('请先同步一条已通过的提交，再发布题解')
    return
  }
  if (!solutionForm.code.trim()) {
    ElMessage.warning('没有找到已通过提交的代码，请重新提交通过后再试')
    return
  }

  solutionSubmitting.value = true
  try {
    const res: any = await solutionApi.createSolution({
      problemId: problemId.value,
      title: solutionForm.title.trim(),
      description: solutionForm.description.trim(),
      code: solutionForm.code,
      language: solutionForm.language,
      sourceSubmissionId: solutionForm.sourceSubmissionId
    })
    const data = res?.data || res || {}

    ElMessage.success(data.rewardMessage || '题解分享成功')
    solutionEditorVisible.value = false
    await fetchSolutionData()
  } catch (error) {
    console.error('提交题解失败:', error)
    const message = (error as any)?.response?.data?.error || (error as any)?.response?.data?.message || '提交题解失败，请稍后再试'
    ElMessage.error(message)
  } finally {
    solutionSubmitting.value = false
  }
}

// 获取语言ID
const getLanguageId = (language: string): number => {
  // 查找选中的语言对象
  const selectedLangObj = languages.value.find(lang => lang.value === language);
  
  if (selectedLangObj && selectedLangObj.id) {
    return selectedLangObj.id;
  }
  
  // 如果没有找到对应的语言对象或id，使用默认值
  switch (language) {
    case 'java':
      return 62;  // Judge0 语言编号：Java (OpenJDK 13.0.1)
    case 'python':
      return 71;  // Judge0 语言编号：Python (3.8.1)
    case 'cpp':
      return 54;  // C++ (GCC 9.2.0)
    case 'c':
      return 50;  // C (GCC 9.2.0)
    case 'javascript':
      return 63;  // Judge0 语言编号：JavaScript (Node.js 12.14.0)
    case 'typescript':
      return 74;  // Judge0 语言编号：TypeScript (3.7.4)
    case 'csharp':
      return 51;  // C# (Mono 6.6.0.161)
    case 'go':
      return 60;  // Go (1.13.5)
    case 'rust':
      return 73;  // Judge0 语言编号：Rust (1.40.0)
    default:
      return 54;  // 默认使用C++ (GCC 9.2.0)
  }
}
</script>

<style scoped>
.problem-ide-container {
  width: 100%;
  height: calc(100vh - 120px);
  padding: 8px 12px;
  box-sizing: border-box;
  position: relative;
}

.problem-ide-container.contest-mode {
  --contest-sidebar-width: 236px;
  background: #f5f7fb;
  display: grid;
  grid-template-columns: var(--contest-sidebar-width) minmax(0, 1fr);
  grid-template-rows: auto minmax(0, 1fr);
  gap: 8px 12px;
  align-items: stretch;
  justify-items: stretch;
  overflow: hidden;
}

.problem-ide-container.contest-mode.contest-sidebar-collapsed {
  --contest-sidebar-width: 74px;
}

.contest-workspace-header {
  grid-column: 1 / -1;
  grid-row: 1;
  margin-bottom: 0;
  padding: 10px 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.18);
}

.contest-workspace-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, auto) auto;
  gap: 14px;
  align-items: center;
}

.contest-workspace-summary {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.contest-rank-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
  gap: 14px;
  margin-top: 6px;
}

.contest-rank-main-card,
.contest-rank-side-card {
  border-radius: 14px;
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
}

.contest-rank-main-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.contest-rank-card-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 8px;
}

.contest-rank-main-value {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.contest-rank-main-value.champion {
  color: #fde68a;
}

.contest-rank-main-value.silver {
  color: #e2e8f0;
}

.contest-rank-main-value.bronze {
  color: #fdba74;
}

.contest-rank-main-icon {
  font-size: 24px;
}

.contest-rank-main-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.contest-rank-stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
}

.contest-rank-stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.contest-rank-stat-card strong {
  font-size: 24px;
  color: #fff;
}

.contest-rank-gap-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
}

.contest-rank-avatar-lane {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.contest-rank-lane-item {
  min-width: 92px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
  padding: 12px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.contest-rank-lane-item.current {
  background: rgba(37, 99, 235, 0.28);
  border-color: rgba(191, 219, 254, 0.6);
}

.contest-rank-lane-item.podium {
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.16);
}

.contest-rank-lane-item.champion {
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.34), rgba(255, 255, 255, 0.12));
}

.contest-rank-lane-rank {
  color: var(--lane-color);
  font-size: 13px;
  font-weight: 700;
}

.contest-rank-lane-name {
  max-width: 100%;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contest-rank-lane-score {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
}

.contest-workspace-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.contest-workspace-title {
  font-size: 18px;
  font-weight: 700;
}

.contest-workspace-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.92);
}

.contest-top-metrics {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.contest-workspace-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.contest-workspace-header :deep(.contest-header-pill.el-button) {
  border-color: rgba(191, 219, 254, 0.28);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.92);
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.10);
  backdrop-filter: blur(8px);
}

.contest-workspace-header :deep(.contest-header-pill.el-button:hover) {
  border-color: rgba(219, 234, 254, 0.42);
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.98);
  transform: translateY(-1px);
}

.contest-workspace-header :deep(.contest-back-btn.el-button) {
  height: 36px;
  padding: 0 13px;
  border-radius: 11px;
}

.contest-workspace-header :deep(.contest-back-btn.el-button:hover) {
  color: rgba(255, 255, 255, 0.98);
}

.contest-header-compact .contest-workspace-header :deep(.contest-back-btn.el-button) {
  height: 32px;
  padding: 0 11px;
}

.contest-workspace-tab {
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  padding: 8px 15px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  font-weight: 700;
  transition: transform 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.contest-workspace-tab:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.18);
}

.contest-workspace-tab.active {
  background: #fff;
  color: #1d4ed8;
}

.contest-top-metric {
  min-width: 78px;
  padding: 5px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.contest-top-metric span {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.contest-top-metric strong {
  display: block;
  margin-top: 2px;
  color: #fff;
  font-size: 15px;
}

.contest-header-compact .contest-workspace-header {
  padding: 6px 12px;
}

.contest-header-compact .contest-workspace-main {
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.contest-header-compact .contest-top-metrics {
  display: none;
}

.contest-header-compact .contest-workspace-title {
  font-size: 17px;
}

.contest-header-compact .contest-workspace-meta {
  display: none;
}

.contest-mode > .contest-nav-card {
  grid-column: 1;
  grid-row: 2;
  align-self: stretch;
  position: relative;
  z-index: 5;
  display: flex;
  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e5ecf7;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(15, 35, 95, 0.08);
}

.contest-side-tabs {
  width: 58px;
  padding: 10px 7px 12px;
  border-right: 1px solid #eef2f7;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.contest-side-tab {
  width: 44px;
  min-height: 58px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}

.contest-side-tab:hover,
.contest-side-tab.active {
  color: #2563eb;
  border-color: #bfdbfe;
  background: #eff6ff;
}

.contest-side-tab-label {
  line-height: 1.1;
}

.contest-side-tab-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: #f1f5f9;
  font-size: 13px;
}

.contest-side-tab.active .contest-side-tab-icon {
  background: #2563eb;
  color: #fff;
}

.contest-sidebar-toggle {
  margin-top: auto;
  width: 44px;
  min-height: 44px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.contest-sidebar-toggle:hover {
  transform: translateY(-1px);
  border-color: #93c5fd;
  color: #2563eb;
  background: #eff6ff;
}

.contest-quick-nav {
  width: 44px;
  margin: 6px 0 10px;
  padding: 6px;
  border: 1px solid #e5ecf7;
  border-radius: 16px;
  background: rgba(248, 251, 255, 0.94);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.contest-quick-nav-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 11px;
  background: #fff;
  color: #2563eb;
  cursor: pointer;
  font-size: 16px;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.08);
  transition: transform 0.18s ease, background 0.18s ease, color 0.18s ease, opacity 0.18s ease;
}

.contest-quick-nav-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #2563eb;
  color: #fff;
}

.contest-quick-nav-btn:disabled {
  cursor: not-allowed;
  opacity: 0.35;
  box-shadow: none;
}

.contest-side-panel {
  flex: 1;
  min-width: 0;
  padding: 12px 12px 14px;
  overflow: auto;
}

.contest-nav-card.collapsed .contest-side-tabs {
  width: 100%;
  padding: 10px 8px 12px;
  align-items: center;
}

.contest-nav-card.collapsed .contest-side-tab,
.contest-nav-card.collapsed .contest-sidebar-toggle {
  width: 100%;
}

.contest-nav-card.collapsed .contest-quick-nav {
  width: 100%;
  align-items: center;
}

.contest-nav-card.collapsed .contest-side-tab-label {
  display: none;
}

.contest-nav-card.collapsed .contest-side-tab-icon {
  width: 28px;
  height: 28px;
}

.contest-nav-card.collapsed .contest-side-panel {
  display: none;
}

.contest-side-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  color: #172554;
  margin-bottom: 10px;
}

.contest-side-head span {
  color: #64748b;
  font-size: 12px;
}

.contest-number-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.contest-number-cell {
  height: 38px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-weight: 800;
  transition: transform 0.15s ease, border-color 0.15s ease, background 0.15s ease, color 0.15s ease;
}

.contest-number-cell:hover {
  transform: translateY(-1px);
  border-color: #93c5fd;
  color: #2563eb;
}

.contest-number-cell.active {
  border-style: solid;
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
  box-shadow: inset 0 0 0 1px #bfdbfe;
}

.contest-number-cell.accepted {
  border-style: solid;
  border-color: #86efac;
  background: #f0fdf4;
  color: #16a34a;
}

.contest-number-cell.attempted {
  border-style: solid;
  border-color: #fbbf24;
  background: #fffbeb;
  color: #d97706;
}

.contest-number-cell.pending {
  border-style: solid;
  border-color: #60a5fa;
  background: #eff6ff;
  color: #2563eb;
}

.contest-side-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  color: #64748b;
  font-size: 12px;
}

.contest-side-legend span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  background: #cbd5e1;
}

.legend-dot.accepted {
  background: #22c55e;
}

.legend-dot.attempted {
  background: #f59e0b;
}

.legend-dot.pending {
  background: #60a5fa;
}

.legend-dot.active {
  background: #2563eb;
}

.contest-side-current {
  margin-top: 14px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid #e6eefb;
}

.contest-side-current-label {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 5px;
}

.contest-side-current strong {
  display: block;
  color: #172554;
  line-height: 1.35;
  margin-bottom: 6px;
}

.contest-side-current span {
  color: #64748b;
  font-size: 12px;
}

.contest-problem-strip {
  padding: 12px 14px;
  border: 1px solid #e6eefb;
  border-radius: 14px;
  background:
    radial-gradient(circle at 4% 10%, rgba(64, 158, 255, 0.12), transparent 32%),
    #fff;
  box-shadow: 0 10px 26px rgba(15, 35, 95, 0.06);
}

.contest-strip-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.contest-strip-head strong {
  color: #172554;
  margin-right: 10px;
}

.contest-strip-head span,
.contest-strip-tip {
  color: #64748b;
  font-size: 13px;
}

.contest-problem-chips {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
  max-height: 118px;
  overflow: auto;
  padding-right: 4px;
}

.contest-problem-chip {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 9px 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.contest-problem-chip:hover {
  border-color: #93c5fd;
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.1);
}

.contest-problem-chip.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.contest-problem-chip.accepted {
  border-color: #67c23a;
}

.contest-problem-chip.attempted {
  border-color: #e6a23c;
}

.contest-problem-chip.pending {
  border-color: #60a5fa;
  background: #eff6ff;
}

.contest-chip-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #334155;
  font-weight: 800;
}

.contest-chip-title {
  min-width: 0;
  color: #111827;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contest-chip-score {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.contest-tab-panel {
  grid-column: 2;
  grid-row: 2;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: auto;
}

.contest-rank-dashboard-panel {
  margin-bottom: 12px;
  padding: 16px;
  border-radius: 16px;
  background:
    radial-gradient(circle at 8% 18%, rgba(255, 255, 255, 0.18), transparent 28%),
    linear-gradient(135deg, #1d4ed8 0%, #0f172a 100%);
}

.contest-ranking-table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  border: 1px solid #e6eefb;
  box-shadow: 0 10px 28px rgba(15, 35, 95, 0.06);
}

.contest-tab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-weight: 800;
  color: #172554;
}

.contest-tab-subtitle {
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.rank-number {
  font-weight: 800;
}

.rank-number-1 { color: #b88200; }
.rank-number-2 { color: #7f8a98; }
.rank-number-3 { color: #c06a3a; }
.rank-number-normal { color: #606266; }

.ranking-user-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.ranking-user-btn.disabled {
  cursor: default;
}

.ranking-user-meta {
  min-width: 0;
}

.ranking-user-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.ranking-user-name {
  font-weight: 700;
  color: #303133;
}

.ranking-user-sub {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.ac-cell { color: #67c23a; font-weight: 700; font-size: 13px; }
.wrong-count { color: #f56c6c; font-size: 11px; }
.wa-cell { color: #f56c6c; font-weight: 700; }
.untried-cell { color: #c0c4cc; }

.movement-up {
  color: #67c23a;
  font-weight: 800;
}

.movement-down {
  color: #f56c6c;
  font-weight: 800;
}

.movement-flat {
  color: #c0c4cc;
}

.contest-table-muted {
  color: #c0c4cc;
}

.contest-submission-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  min-width: 0;
}

.contest-submission-user span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contest-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
}

.contest-detail-meta-card {
  padding: 14px 16px;
  border: 1px solid #edf2ff;
  border-radius: 14px;
  background: #f8fbff;
}

.contest-detail-meta-card span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.contest-detail-meta-card strong {
  display: block;
  margin-top: 6px;
  color: #172554;
  font-size: 17px;
}

:deep(.rank-gold td) { background-color: #fff8e6 !important; }
:deep(.rank-silver td) { background-color: #f5f5f5 !important; }
:deep(.rank-bronze td) { background-color: #fef3f0 !important; }
:deep(.rank-self td) {
  box-shadow: inset 3px 0 0 #f56c6c;
}

.problem-layout {
  display: flex;
  gap: 12px;
  height: 100%;
  width: 100%;
  box-sizing: border-box;
}

.contest-mode .problem-layout {
  grid-column: 2;
  grid-row: 2;
  min-height: 0;
  min-width: 0;
  height: 100%;
  width: 100%;
}

.contest-mode .problem-card {
  flex: 0.92 1 0;
}

.contest-mode .code-card {
  flex: 1.08 1 0;
}

.contest-nav-card {
  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 0;
}

.contest-nav-inner {
  height: 100%;
}

.contest-nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.contest-problem-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.contest-problem-item {
  width: 100%;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.contest-problem-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.12);
}

.contest-problem-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.contest-problem-item.accepted {
  border-color: #67c23a;
}

.contest-problem-item.attempted {
  border-color: #e6a23c;
}

.contest-problem-item.pending {
  border-color: #60a5fa;
  background: #eff6ff;
}

.contest-problem-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.contest-problem-order {
  font-size: 12px;
  color: #6b7280;
}

.contest-problem-state {
  font-size: 18px;
  font-weight: 700;
}

.contest-problem-title {
  color: #111827;
  font-weight: 600;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.contest-problem-score {
  color: #2563eb;
  font-size: 13px;
}

.contest-nav-divider {
  height: 1px;
  background: #eef2f7;
  margin: 16px 0;
}

.contest-nearby-ranking {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.contest-around-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.contest-around-item.current {
  border-color: #93c5fd;
  background: #eff6ff;
}

.contest-around-rank {
  min-width: 54px;
  font-size: 13px;
  font-weight: 700;
  color: #4b5563;
}

.contest-around-rank.champion {
  color: #d97706;
}

.contest-around-rank.silver {
  color: #64748b;
}

.contest-around-rank.bronze {
  color: #c2410c;
}

.contest-around-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.contest-around-name {
  color: #111827;
  font-weight: 600;
}

.contest-around-summary {
  color: #6b7280;
  font-size: 12px;
}

.contest-nav-section-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.contest-submission-empty {
  color: #9ca3af;
  font-size: 13px;
}

.contest-recent-submissions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.contest-submission-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #f9fafb;
  cursor: pointer;
  text-align: left;
}

.contest-submission-problem {
  color: #111827;
  font-weight: 500;
}

.contest-submission-time {
  color: #9ca3af;
  font-size: 12px;
  margin-top: 4px;
}

.problem-card, .code-card {
  flex: 1 1 0;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  padding: 0 0 0 0;
}

.problem-description,
.code-editor-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.el-card__body) {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

.problem-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 22px;
  padding: 2px 6px 4px;
}

.problem-header-divider {
  margin-top: 12px;
  border-bottom: 1px solid #e0e0e0;
}

.problem-title {
  flex: 1;
  min-width: 260px;
}

.problem-title h2 {
  margin: 0;
  color: #2196F3;
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.problem-author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.problem-author span {
  padding: 2px 8px;
  border-radius: 999px;
  background: #f0f7ff;
  color: #409eff;
  font-weight: 700;
}

.author-link {
  border: none;
  padding: 0;
  background: transparent;
  color: #606266;
  cursor: pointer;
  font-size: 13px;
}

.author-link:hover {
  color: #409eff;
}

.problem-tags {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.problem-stats {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 540px;
}

.problem-action-btn {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: #f8fbff;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 6px 10px;
  border: 1px solid #e4edf8;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  color: #2c78d4;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.06);
}

.stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 8px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 15px;
}

.stat-icon.success {
  background: #ecfdf3;
  color: #27c93f;
}

.stat-icon.text {
  font-size: 12px;
  font-weight: 900;
}

.problem-content {
  line-height: 1.7;
  color: #333;
  font-size: 16px;
}

.problem-section {
  margin-bottom: 28px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.section-content {
  padding: 0 10px;
}

.example-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.example-title {
  font-weight: 700;
  color: #303133;
  font-size: 15px;
  padding-bottom: 4px;
  border-bottom: 1px solid #ebeef5;
}

.example-label {
  font-weight: 600;
  font-size: 14px;
  color: #606266;
}

.example-pre {
  background-color: #f5f7fa;
  padding: 10px 14px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  margin: 0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-x: auto;
}

.example-explain {
  padding: 10px 14px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

/* 增强富文本内容样式 */
.problem-rich-content {
  color: #444;
}

.problem-rich-content :deep(code) {
  background-color: #f0f0f0;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', monospace;
  color: #d63200;
  font-size: 0.95em;
}

.problem-rich-content :deep(pre) {
  overflow-x: auto;
  background-color: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
  border: 1px solid #e0e0e0;
  line-height: 1.5;
  margin: 12px 0;
}

.problem-rich-content :deep(a) {
  color: #1976d2;
  text-decoration: none;
}

.problem-rich-content :deep(a):hover {
  text-decoration: underline;
}

.problem-rich-content :deep(blockquote) {
  border-left: 4px solid #b0cfff;
  margin: 16px 0;
  padding-left: 16px;
  color: #666;
}

.problem-rich-content :deep(ul), .problem-rich-content :deep(ol) {
  padding-left: 24px;
  margin: 12px 0;
}

.problem-rich-content :deep(li) {
  margin-bottom: 6px;
}

.problem-rich-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

.problem-rich-content :deep(th), .problem-rich-content :deep(td) {
  border: 1px solid #e0e0e0;
  padding: 8px;
  text-align: left;
}

.problem-rich-content :deep(th) {
  background-color: #f5f7fa;
}

.problem-rich-content :deep(img) {
  max-width: 100%;
  display: block;
  margin: 12px 0;
  border-radius: 6px;
}

.problem-rich-content :deep(h1), .problem-rich-content :deep(h2), 
.problem-rich-content :deep(h3), .problem-rich-content :deep(h4) {
  margin-top: 24px;
  margin-bottom: 16px;
  color: #333;
}

.problem-rich-content :deep(p) {
  margin: 12px 0;
  line-height: 1.7;
}

.problem-rich-content :deep(strong) {
  font-weight: 600;
  color: #333;
}

.problem-rich-content :deep(em) {
  font-style: italic;
  color: #555;
}

.problem-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow: auto;
}

.solution-locked {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 250px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.language-select {
  display: flex;
  align-items: center;
  gap: 10px;
}

.editor-controls {
  display: flex;
  gap: 10px;
}

.code-editor {
  height: calc(100% - 60px);
  overflow: hidden;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 10px;
}

.ace-editor-container {
  width: 100% !important;
  height: 100% !important;
  font-family: 'Consolas', 'Monaco', monospace;
}

/* 隐藏模拟编辑器的样式 */
.mock-ace-editor,
.editor-toolbar,
.line-numbers,
.code-content {
  display: none;
}

/* AI 辅助抽屉样式 */
.ai-drawer {
  --ai-primary: #409eff;
  --ai-primary-dark: #2c78d4;
  --ai-ink: #193549;
  --ai-teal: #0f766e;
  --ai-soft: #f0f7ff;
  --ai-border: rgba(64, 158, 255, 0.18);
}

.ai-drawer :deep(.el-drawer__header) {
  margin: 0;
  padding: 16px 22px;
  background:
    radial-gradient(circle at 8% 18%, rgba(255, 255, 255, 0.28), transparent 26%),
    radial-gradient(circle at 88% 12%, rgba(86, 182, 194, 0.28), transparent 28%),
    linear-gradient(135deg, #193549 0%, #2c78d4 56%, #409eff 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 12px 30px rgba(44, 120, 212, 0.2);
}

.ai-drawer :deep(.el-drawer__close-btn) {
  color: rgba(255, 255, 255, 0.86);
  transition: transform 0.2s ease, color 0.2s ease, background 0.2s ease;
  border-radius: 10px;
}

.ai-drawer :deep(.el-drawer__close-btn:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  transform: rotate(90deg);
}

.ai-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.ai-drawer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.ai-brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #193549;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: -0.03em;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(240, 247, 255, 0.82));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.88),
    0 14px 30px rgba(0, 0, 0, 0.16);
}

.ai-drawer-title-group {
  min-width: 0;
}

.ai-drawer-kicker {
  color: #fff;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.ai-drawer-subtitle {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  line-height: 1.5;
}

.ai-drawer-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.ai-drawer-meta :deep(.el-tag) {
  height: 26px;
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.28);
  color: #fff;
  backdrop-filter: blur(8px);
}

.ai-assistant-container {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: calc(100vh - 75px);
  padding: 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 14% 8%, rgba(64, 158, 255, 0.15), transparent 27%),
    radial-gradient(circle at 88% 18%, rgba(86, 182, 194, 0.13), transparent 29%),
    linear-gradient(180deg, #f8fbff 0%, #f4f8fe 45%, #ffffff 100%);
}

.ai-assistant-container::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(25, 53, 73, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(25, 53, 73, 0.035) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.52), transparent 58%);
}

.ai-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 178px;
  gap: 18px;
  margin: 18px 18px 10px;
  padding: 18px;
  overflow: hidden;
  border-radius: 24px;
  color: #fff;
  background:
    radial-gradient(circle at 86% 18%, rgba(255, 255, 255, 0.28), transparent 25%),
    radial-gradient(circle at 14% 100%, rgba(86, 182, 194, 0.28), transparent 28%),
    linear-gradient(135deg, #193549 0%, #2c78d4 58%, #409eff 100%);
  box-shadow: 0 22px 48px rgba(44, 120, 212, 0.22);
  animation: ai-panel-rise 0.36s cubic-bezier(0.22, 1, 0.36, 1);
}

.ai-hero::after {
  content: 'Code Power';
  position: absolute;
  right: -18px;
  bottom: -12px;
  color: rgba(255, 255, 255, 0.08);
  font-size: 54px;
  font-weight: 900;
  letter-spacing: -0.06em;
  white-space: nowrap;
}

.ai-hero-copy,
.ai-hero-badges,
.ai-hero-terminal {
  position: relative;
  z-index: 1;
}

.ai-hero-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  color: #d7ebff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.ai-hero-label::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #27c93f;
  box-shadow: 0 0 0 6px rgba(39, 201, 63, 0.16);
}

.ai-hero-title {
  font-size: 19px;
  font-weight: 900;
  line-height: 1.35;
  letter-spacing: -0.01em;
}

.ai-hero-desc {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  line-height: 1.6;
}

.ai-hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.ai-hero-badges :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.24);
  color: #fff;
  backdrop-filter: blur(8px);
}

.ai-hero-terminal {
  align-self: stretch;
  min-height: 126px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 18px;
  background: rgba(25, 53, 73, 0.52);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  font-family: 'Fira Code', 'Consolas', monospace;
}

.ai-terminal-top {
  display: flex;
  gap: 6px;
  margin-bottom: 14px;
}

.ai-terminal-top span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff5f56;
}

.ai-terminal-top span:nth-child(2) { background: #ffbd2e; }
.ai-terminal-top span:nth-child(3) { background: #27c93f; }

.ai-terminal-line {
  margin-top: 7px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  line-height: 1.4;
}

.ai-terminal-line::before {
  content: '>';
  margin-right: 6px;
  color: rgba(255, 255, 255, 0.42);
}

.ai-terminal-line.accent {
  color: #b7f7ff;
}

.ai-terminal-line.muted {
  color: rgba(215, 235, 255, 0.62);
}

.ai-session-card {
  position: relative;
  z-index: 1;
  margin: 0 18px 12px;
  padding: 12px 14px;
  border: 1px solid var(--ai-border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(240, 247, 255, 0.88));
  box-shadow: 0 16px 34px rgba(44, 120, 212, 0.09);
  backdrop-filter: blur(12px);
}

.ai-session-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-session-label {
  flex-shrink: 0;
  color: #193549;
  font-size: 12px;
  font-weight: 800;
}

.ai-model-select {
  flex: 1;
  min-width: 0;
}

.model-option-name {
  float: left;
  max-width: 230px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-option-cost {
  float: right;
  margin-left: 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.ai-cost-hint {
  flex-shrink: 0;
  padding: 5px 9px;
  border-radius: 999px;
  background: #e6f2ff;
  color: var(--ai-primary-dark);
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.ai-context-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.context-chip {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #2c78d4;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.context-chip.strong {
  background: linear-gradient(135deg, #193549 0%, #2c78d4 100%);
  color: #fff;
  box-shadow: 0 10px 22px rgba(44, 120, 212, 0.18);
}

.context-chip.soft {
  background: rgba(15, 118, 110, 0.09);
  color: #0f766e;
}

.ai-messages {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 18px 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.72));
  scroll-behavior: smooth;
}

.ai-messages::-webkit-scrollbar {
  width: 7px;
}

.ai-messages::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.22);
}

.ai-message {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 16px;
  animation: ai-msg-slide-in 0.3s ease-out;
}

@keyframes ai-msg-slide-in {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-msg-fade-enter-active { animation: ai-msg-slide-in 0.3s ease-out; }

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 900;
  flex-shrink: 0;
  box-shadow: 0 10px 20px rgba(25, 53, 73, 0.12);
}

.ai-message.system .msg-avatar {
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  color: #2c78d4;
  border: 1px solid rgba(64, 158, 255, 0.18);
}

.ai-message.user .msg-avatar {
  background: linear-gradient(135deg, #2c78d4 0%, #409eff 100%);
  color: #fff;
}

.ai-message.assistant .msg-avatar {
  background: linear-gradient(135deg, #193549 0%, #0f766e 100%);
  color: #fff;
}

.msg-bubble {
  max-width: min(82%, 720px);
  padding: 12px 15px;
  border-radius: 18px;
  line-height: 1.6;
  font-size: 14px;
  word-break: break-word;
}

.ai-message.system .msg-bubble {
  background: linear-gradient(135deg, rgba(240, 247, 255, 0.94), rgba(255, 255, 255, 0.9));
  color: #476173;
  border: 1px solid rgba(64, 158, 255, 0.14);
  font-size: 13px;
  box-shadow: 0 10px 24px rgba(44, 120, 212, 0.06);
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-message.user .msg-bubble {
  background: linear-gradient(135deg, #2c78d4 0%, #409eff 100%);
  color: #fff;
  border-bottom-right-radius: 6px;
  box-shadow: 0 12px 26px rgba(44, 120, 212, 0.18);
}

.ai-message.assistant .msg-bubble {
  max-width: min(94%, 880px);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(64, 158, 255, 0.13);
  border-bottom-left-radius: 6px;
  box-shadow: 0 15px 34px rgba(25, 53, 73, 0.08);
  backdrop-filter: blur(8px);
}

.ai-message .message-content {
  line-height: 1.7;
}

.ai-stream-placeholder {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #476173;
  font-size: 13px;
  letter-spacing: 0.02em;
}

.ai-stream-placeholder::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #27c93f;
  box-shadow: 0 0 0 6px rgba(39, 201, 63, 0.12);
  animation: ai-live-pulse 1.35s ease-in-out infinite;
}

.stream-cursor {
  color: var(--ai-teal);
  animation: stream-cursor-blink 1s steps(1, end) infinite;
}

/* AI 消息中的 Markdown 正文 */
.markdown-body {
  font-size: 14px;
  color: #303133;
  overflow-x: auto;
}

.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4 {
  margin: 14px 0 8px;
  font-weight: 700;
  color: #193549;
  line-height: 1.35;
}

.markdown-body h1:first-child,
.markdown-body h2:first-child,
.markdown-body h3:first-child,
.markdown-body h4:first-child {
  margin-top: 0;
}

.markdown-body h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
}

.markdown-body h3::before {
  content: '';
  width: 4px;
  height: 15px;
  border-radius: 999px;
  background: var(--ai-teal);
}

.markdown-body h4 { font-size: 14px; }

.markdown-body p {
  margin: 7px 0;
}

.markdown-body ul, .markdown-body ol {
  padding-left: 20px;
  margin: 8px 0;
}

.markdown-body li {
  margin: 4px 0;
  padding-left: 2px;
}

.markdown-body table {
  width: 100%;
  margin: 10px 0 12px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: auto;
  font-size: 13px;
  line-height: 1.55;
  border: 1px solid #dce8f4;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.markdown-body th,
.markdown-body td {
  padding: 8px 10px;
  border-right: 1px solid #e8f0f8;
  border-bottom: 1px solid #e8f0f8;
  vertical-align: top;
  text-align: left;
  min-width: 70px;
}

.markdown-body th:last-child,
.markdown-body td:last-child {
  border-right: none;
}

.markdown-body tr:last-child td {
  border-bottom: none;
}

.markdown-body th {
  background: #f3f8fd;
  color: #193549;
  font-weight: 800;
  white-space: nowrap;
}

.markdown-body tbody tr:nth-child(even) td {
  background: #fbfdff;
}

.markdown-body pre {
  background: #102536;
  color: #e8f4ff;
  border-radius: 10px;
  padding: 11px 12px;
  margin: 10px 0;
  overflow-x: auto;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  border: 1px solid rgba(16, 37, 54, 0.12);
}

.markdown-body code {
  background: #eef7ff;
  padding: 2px 5px;
  border-radius: 6px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
  color: #193549;
  word-break: break-word;
}

.markdown-body pre code {
  background: none;
  padding: 0;
  color: inherit;
  word-break: normal;
}

.markdown-body strong {
  font-weight: 700;
  color: #193549;
}

.markdown-body blockquote {
  border-left: 3px solid var(--ai-teal);
  padding-left: 12px;
  margin: 8px 0;
  color: #606266;
}

/* AI 正在输入提示 */
.ai-typing-indicator {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-end;
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 11px 14px !important;
  background: rgba(255, 255, 255, 0.98) !important;
  border: 1px solid rgba(64, 158, 255, 0.14) !important;
  box-shadow: 0 12px 28px rgba(25, 53, 73, 0.08);
}

.typing-copy {
  margin-right: 4px;
  color: #476173;
  font-size: 13px;
}

.typing-bubble .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ai-teal);
  animation: typing-dot 1.4s infinite;
}

.typing-bubble .dot:nth-child(2) { animation-delay: 0.2s; }
.typing-bubble .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing-dot {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

@keyframes stream-cursor-blink {
  0%, 49% { opacity: 1; }
  50%, 100% { opacity: 0; }
}

/* AI 快捷建议 */
.ai-suggestions {
  position: relative;
  z-index: 1;
  margin: 0 18px 10px;
  padding: 12px 14px 14px;
  max-height: 176px;
  overflow-y: auto;
  border: 1px solid rgba(64, 158, 255, 0.13);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 30px rgba(25, 53, 73, 0.06);
  backdrop-filter: blur(10px);
}

.suggestions-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.suggestions-title {
  font-size: 13px;
  color: #193549;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.suggestions-subtitle {
  margin-top: 2px;
  color: #7a8b9a;
  font-size: 11px;
}

.suggestions-hint {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  background: #f0f7ff;
  color: #2c78d4;
  font-size: 11px;
  font-weight: 800;
}

.suggestions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.suggestion-chip {
  text-align: left;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(64, 158, 255, 0.13);
  border-radius: 15px;
  padding: 10px 12px;
  font-size: 12px;
  color: #476173;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  outline: none;
  box-shadow: 0 8px 18px rgba(25, 53, 73, 0.04);
}

.suggestion-chip:hover {
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f2ff 100%);
  border-color: rgba(64, 158, 255, 0.34);
  color: #2c78d4;
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(44, 120, 212, 0.12);
}

.suggestion-chip:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  transform: none;
}

/* AI 输入栏 */
.ai-input-bar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  margin: 0 18px 16px;
  padding: 12px;
  border: 1px solid rgba(64, 158, 255, 0.14);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 32px rgba(25, 53, 73, 0.08);
  backdrop-filter: blur(10px);
}

.ai-input-bar :deep(.el-textarea__inner) {
  border-radius: 16px;
  border: 1px solid rgba(64, 158, 255, 0.18);
  padding: 12px 14px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
  background: #fbfdff;
}

.ai-input-bar :deep(.el-textarea__inner:focus) {
  border-color: rgba(64, 158, 255, 0.52);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.ai-input-bar :deep(.el-button.is-circle) {
  width: 42px;
  height: 42px;
  box-shadow: 0 10px 20px rgba(44, 120, 212, 0.18);
}

.ai-input-bar :deep(.el-button--primary.is-circle) {
  background: linear-gradient(135deg, #2c78d4 0%, #409eff 100%);
  border: none;
}

.ai-input-bar :deep(.el-button--danger.is-circle) {
  background: linear-gradient(135deg, #ef4444 0%, #f97316 100%);
  border: none;
}

@keyframes ai-panel-rise {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes ai-live-pulse {
  0% { transform: scale(0.96); opacity: 0.68; }
  50% { transform: scale(1.08); opacity: 1; }
  100% { transform: scale(0.96); opacity: 0.68; }
}

@media (max-width: 720px) {
  .ai-drawer :deep(.el-drawer__header) {
    padding: 14px 16px;
  }

  .ai-drawer-header {
    align-items: flex-start;
  }

  .ai-drawer-meta {
    display: none;
  }

  .ai-hero {
    grid-template-columns: 1fr;
    margin: 14px 14px 10px;
    padding: 16px;
  }

  .ai-hero-terminal {
    display: none;
  }

  .ai-session-card,
  .ai-suggestions,
  .ai-input-bar {
    margin-left: 14px;
    margin-right: 14px;
  }

  .ai-session-row {
    flex-wrap: wrap;
  }

  .ai-session-label {
    width: 100%;
  }

  .ai-cost-hint {
    display: none;
  }

  .ai-messages {
    padding-left: 14px;
    padding-right: 14px;
  }

  .msg-bubble {
    max-width: calc(100% - 44px);
  }

  .suggestions-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ai-hero,
  .ai-message,
  .ai-msg-fade-enter-active,
  .ai-stream-placeholder::before,
  .typing-bubble .dot,
  .stream-cursor {
    animation: none !important;
  }
}

/* 测试对话框样式 */
.test-input,
.test-expected,
.test-section {
  margin-bottom: 20px;
}

.test-input h4,
.test-expected h4,
.test-section h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #303133;
}

.test-running {
  min-height: 200px;
  position: relative;
}

.running-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  margin-top: 80px;
}

.test-result-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  font-size: 1.1rem;
  font-weight: 500;
}

.test-result-header.success {
  background-color: #f0f9eb;
  color: #67c23a;
  border-left: 4px solid #67c23a;
}

.test-result-header.error {
  background-color: #fef0f0;
  color: #f56c6c;
  border-left: 4px solid #f56c6c;
}

.result-output,
.result-expected {
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  margin: 0;
}

.execution-info {
  display: flex;
  gap: 25px;
}

.info-item {
  display: flex;
  gap: 8px;
}

.info-label {
  color: #606266;
}

.info-value {
  font-weight: 500;
}

.submit-warning {
  color: #e6a23c;
  font-size: 0.9rem;
}

.score-info {
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  margin: 10px 0;
}

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.score-item:last-child {
  margin-bottom: 0;
}

.score-label {
  color: #606266;
  font-weight: 500;
}

.score-value {
  color: #409EFF;
  font-weight: 600;
}

.submit-result-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  font-size: 1.1rem;
  font-weight: 500;
}

.submit-result-header.success {
  background-color: #f0f9eb;
  color: #67c23a;
}

.submit-result-header.error {
  background-color: #fef0f0;
  color: #f56c6c;
}

.submit-section {
  margin-bottom: 20px;
}

.submit-section:last-child {
  margin-bottom: 0;
}

.submit-section h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #303133;
}

@media (max-width: 1400px) {
  .problem-layout {
    gap: 18px;
  }
}

@media (max-width: 1100px) {
  .problem-ide-container.contest-mode {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto minmax(0, 1fr);
  }

  .contest-workspace-header,
  .contest-mode > .contest-nav-card,
  .contest-mode > .problem-layout,
  .contest-mode > .contest-tab-panel {
    grid-column: 1;
  }

  .contest-mode > .contest-nav-card {
    grid-row: auto;
    height: auto;
    max-height: 220px;
  }

  .contest-mode > .problem-layout,
  .contest-mode > .contest-tab-panel {
    grid-row: auto;
  }
}

@media (max-width: 900px) {
  .problem-layout {
    flex-direction: column;
    gap: 16px;
  }

  .problem-header {
    flex-direction: column;
    gap: 16px;
  }

  .problem-title {
    min-width: 0;
    width: 100%;
  }

  .problem-stats {
    justify-content: flex-start;
    max-width: none;
    width: 100%;
  }

  .problem-card, .code-card {
    width: 100%;
    min-width: 0;
    padding: 0;
  }
}

/* 测试代码弹窗样式 */
.test-code-popup {
  position: fixed;
  z-index: 1000;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  overflow: auto;
}

.test-code-content {
  background-color: white;
  margin: 10% auto;
  padding: 20px;
  width: 60%;
  max-width: 700px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  animation: modalSlideIn 0.3s ease;
}

@keyframes modalSlideIn {
  from {opacity: 0; transform: translateY(-20px);}
  to {opacity: 1; transform: translateY(0);}
}

.test-code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e0e0e0;
}

.test-code-title {
  font-size: 18px;
  font-weight: 500;
  color: #2196F3;
  margin: 0;
}

.test-code-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.test-input-section, .test-output-section {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 15px;
}

.test-section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.test-section-title {
  font-weight: 500;
  color: #2196F3;
  margin-top: 0;
  margin-bottom: 0;
  font-size: 16px;
}

.test-sample-switcher {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.test-sample-label {
  color: #606266;
  font-size: 12px;
}

.test-code {
  background-color: #f8f9fa;
  border-radius: 4px;
  padding: 10px;
  font-family: monospace;
  white-space: pre-wrap;
  overflow-x: auto;
  margin-bottom: 10px;
}

.test-results-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.test-result-box {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 10px;
}

.test-result-title {
  font-weight: 500;
  color: #2196F3;
  margin-top: 0;
  margin-bottom: 8px;
  font-size: 14px;
}

.diff-highlight {
  background-color: rgba(255, 0, 0, 0.3);
  border-radius: 2px;
  position: relative;
  animation: blink-highlight 1s infinite;
}

.diff-highlight::after {
  content: "";
  position: absolute;
}

@keyframes blink-highlight {
  0% { opacity: 0.8; }
  50% { opacity: 0.4; }
  100% { opacity: 0.8; }
}

.diff-missing {
  position: relative;
  background-color: rgba(255, 235, 235, 0.5);
  padding: 0 2px;
  border-radius: 2px;
}

.missing-char {
  color: #f56c6c;
  font-weight: bold;
  text-decoration: underline;
  text-decoration-style: dashed;
}

.missing-space {
  color: #f56c6c;
  font-weight: bold;
  position: relative;
}

.space-highlight {
  position: relative;
  background-color: rgba(255, 0, 0, 0.3);
  animation: blink-highlight 1s infinite;
}

.space-highlight::before {
  content: "␣";
  position: absolute;
  color: rgba(255, 0, 0, 0.8);
  font-size: 14px;
  top: -2px;
  left: 0;
}

/* 结果弹窗样式 */
:global(.result-dialog .el-dialog__body) {
  padding-top: 12px;
}

.result-dialog-body {
  max-height: 72vh;
  overflow-y: auto;
  padding-right: 4px;
}

.result-summary {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.summary-item {
  text-align: center;
  padding: 8px;
}

.summary-value {
  font-size: 20px;
  font-weight: bold;
  color: #2196F3;
  margin: 5px 0;
  word-break: break-word;
}

.summary-label {
  font-size: 14px;
  color: #777;
}

.summary-value.pending {
  color: #e6a23c;
}

.contest-judging-notice {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px 14px;
  border: 1px solid #f3d19e;
  border-radius: 8px;
  background: #fdf6ec;
  color: #8a5a12;
}

.contest-judging-notice strong {
  display: block;
  margin-bottom: 3px;
  color: #b7791f;
  font-size: 15px;
}

.contest-judging-notice p {
  margin: 0;
  color: #8a6d3b;
  font-size: 13px;
  line-height: 1.5;
}

.contest-judging-spinner {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  border: 2px solid rgba(230, 162, 60, 0.24);
  border-top-color: #e6a23c;
  border-radius: 50%;
  animation: contest-judging-spin 0.8s linear infinite;
}

@keyframes contest-judging-spin {
  to { transform: rotate(360deg); }
}

.accepted-insights-card {
  margin-bottom: 12px;
  padding: 18px;
  border: 1px solid #d7ebff;
  border-radius: 18px;
  background:
    radial-gradient(circle at 8% 20%, rgba(64, 158, 255, 0.16), transparent 30%),
    linear-gradient(135deg, #f8fbff 0%, #ffffff 54%, #eef7ff 100%);
  box-shadow: 0 16px 36px rgba(64, 158, 255, 0.12);
}

.accepted-insights-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
}

.accepted-kicker {
  display: inline-flex;
  margin-bottom: 6px;
  padding: 4px 9px;
  border-radius: 999px;
  background: #ecfdf3;
  color: #16a34a;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.accepted-insights-head h3 {
  margin: 0;
  color: #16324f;
  font-size: 19px;
}

.accepted-insights-head p {
  margin: 6px 0 0;
  color: #637587;
  font-size: 13px;
  line-height: 1.6;
}

.accepted-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.accepted-metric-card {
  padding: 12px;
  border: 1px solid rgba(64, 158, 255, 0.16);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 10px 22px rgba(25, 53, 73, 0.05);
}

.accepted-metric-card span,
.accepted-metric-card small {
  display: block;
  color: #7a8b9a;
  font-size: 12px;
}

.accepted-metric-card strong {
  display: block;
  margin: 5px 0 4px;
  color: #2196f3;
  font-size: 22px;
  line-height: 1.1;
}

.accepted-metric-card.global strong {
  color: #16a34a;
}

.solution-invite-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  padding: 13px 14px;
  border: 1px solid #e4edf8;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.78);
}

.solution-invite-card.recommended {
  border-color: #b7e4c7;
  background: linear-gradient(135deg, rgba(236, 253, 243, 0.92), rgba(255, 255, 255, 0.86));
}

.solution-invite-card strong {
  color: #16324f;
}

.solution-invite-card p {
  margin: 4px 0 0;
  color: #637587;
  font-size: 13px;
  line-height: 1.6;
}

.test-case-list {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 10px;
}

.test-case-list .result-header {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 2px;
  color: #666;
  font-size: 14px;
}

.test-case {
  margin-top: 0;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: white;
}

.test-case h4 {
  margin: 0 0 10px 0;
  color: #2196F3;
  display: flex;
  justify-content: space-between;
}

.test-case-score {
  background-color: #4CAF50;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.test-case-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-top: 10px;
  color: #777;
  font-size: 14px;
}

.submitted-code-collapse {
  margin-bottom: 12px;
}

.submitted-code-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.submitted-code-pre {
  max-height: 260px;
  overflow: auto;
  margin: 0;
  padding: 14px 88px 14px 14px;
  border-radius: 6px;
  background: #1f2937;
  color: #e5e7eb;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.code-block-wrapper {
  position: relative;
}

.copy-code-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  border-color: rgba(255, 255, 255, 0.32);
  background: rgba(255, 255, 255, 0.12);
  color: #f8fafc;
  backdrop-filter: blur(4px);
}

.copy-code-btn:hover {
  border-color: rgba(255, 255, 255, 0.55);
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.test-case-error {
  margin: 12px 0 0;
  padding: 10px 12px;
  border-left: 3px solid #d05451;
  border-radius: 6px;
  background: #fff8f8;
  color: #9f1d1d;
  white-space: pre-wrap;
  overflow-x: auto;
}

.test-case-io-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.test-case-io-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
  padding: 10px;
}

.test-case-io-item strong {
  display: block;
  margin-bottom: 6px;
  color: #606266;
  font-size: 13px;
}

.test-case-io-item pre {
  margin: 0;
  white-space: pre-wrap;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
}

.test-case p {
  margin: 5px 0;
}

.judging-placeholder {
  grid-column: 1 / -1;
  border-left-color: #e6a23c;
  background: #fffaf2;
}

.judging-placeholder h4 {
  color: #b7791f;
}

.judging-placeholder p {
  color: #8a6d3b;
}

@media (max-width: 768px) {
  .result-summary {
    grid-template-columns: 1fr;
  }

  .accepted-metrics-grid {
    grid-template-columns: 1fr;
  }

  .solution-invite-card {
    flex-direction: column;
    align-items: stretch;
  }

  .result-dialog-body {
    max-height: 70vh;
  }

  .solution-editor-top {
    grid-template-columns: 1fr;
  }

  .solution-language-select {
    width: 100%;
  }
}

.success {
  border-left: 4px solid #4CAF50;
}

.error {
  border-left: 4px solid #f44336;
}

.compile-error {
  margin-top: 15px;
  padding: 15px;
  border-left: 4px solid #D05451;
  border-radius: 6px;
  background-color: #FFF8F8;
  font-family: monospace;
}

.compile-error-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  color: #D05451;
  font-weight: bold;
}

.compile-error-body {
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.6;
  color: #555;
  background-color: #FAFAFA;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #EEE;
  overflow-x: auto;
}

#testCodeResult {
  font-size: 15px;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 4px;
  display: inline-block;
  margin-left: 15px;
}

#testCodeResult:not(:empty) {
  background-color: #f8f8f8;
  border-left: 4px solid;
}

/* 成功状态 */
#testCodeResult:not(:empty):first-letter {
  font-size: 18px;
  font-weight: bold;
}

/* 根据内容设置颜色 */
#testCodeResult:contains('测试通过') {
  color: #4caf50;
  border-color: #4caf50;
  background-color: #f1f8e9;
}

#testCodeResult:contains('测试失败') {
  color: #f44336;
  border-color: #f44336;
  background-color: #fef0f0;
}

.test-result-message {
  font-size: 15px;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 4px;
  display: inline-block;
  margin-left: 15px;
}

.test-result-message:not(:empty) {
  background-color: #f8f8f8;
  border-left: 4px solid #ddd;
}

.test-result-message:not(:empty)::first-letter {
  font-size: 18px;
  font-weight: bold;
}

.success-message {
  color: #4caf50;
  border-color: #4caf50 !important;
  background-color: #f1f8e9 !important;
}

.error-message {
  color: #f56c6c;
  background-color: #fef0f0;
  padding: 8px;
  border-radius: 4px;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.5;
  overflow-x: auto;
}

.difficulty-blue {
  background-color: #e6f0fa !important;
  color: #1976d2 !important;
  border-color: #b3d8fd !important;
}

/* 题解样式 */
.solution-tabs {
  margin-top: 10px;
}

.solution-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.solution-title {
  font-weight: bold;
  font-size: 16px;
}

.solution-info {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #888;
}

.solution-card {
  margin-bottom: 15px;
}

.community-solution-post {
  border-radius: 10px;
}

.solution-post-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.solution-post-author {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  cursor: pointer;
}

.solution-author-name {
  font-weight: 600;
  color: #303133;
}

.solution-post-author:hover .solution-author-name {
  color: #409eff;
}

.clickable-user-avatar {
  transition: transform 0.2s ease;
}

.solution-post-author:hover .clickable-user-avatar {
  transform: translateY(-1px);
}

.solution-post-title {
  margin: 0 0 14px;
  font-size: 20px;
  line-height: 1.35;
  color: #111827;
}

.solution-body {
  font-size: 14px;
  line-height: 1.6;
}

.solution-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

.solution-empty {
  padding: 24px 0;
}

.community-solutions {
  margin-top: 10px;
  margin-bottom: 20px;
}

.add-solution {
  margin-top: 20px;
  text-align: center;
}

/* 添加限制条件样式 */
.problem-limits-block ul {
  list-style-type: none;
  padding-left: 0;
}

.problem-limits-block li {
  padding: 5px 0;
  font-weight: 500;
}

/* 题解样式 */
.solution-content {
  margin: 10px 0;
}

.solution-thinking {
  margin-bottom: 30px;
}

.solution-thinking h3, .solution-code h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.solution-code-tabs {
  margin-top: 15px;
}

.code-preview {
  background-color: #282c34;
  color: #abb2bf;
  padding: 15px 88px 15px 15px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}

:global(.solution-editor-dialog .el-dialog__body) {
  max-height: 72vh;
  overflow-y: auto;
}

.solution-editor-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 12px;
  margin-bottom: 12px;
}

.solution-quill-wrap {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.solution-quill-wrap :deep(.ql-toolbar) {
  border: none;
  border-bottom: 1px solid #dcdfe6;
}

.solution-quill-wrap :deep(.ql-container) {
  border: none;
  min-height: 180px;
  font-size: 14px;
}

.solution-quill-wrap :deep(.ql-editor) {
  min-height: 180px;
}

.solution-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin: 12px 0 16px;
  flex-wrap: wrap;
}

.solution-outline-hint {
  margin: 0 0 10px;
  color: #637587;
  font-size: 13px;
  line-height: 1.6;
}

.solution-code-source {
  width: 100%;
}

.solution-source-alert {
  margin-bottom: 10px;
}

.solution-editor-form :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.5;
}

.submission-table :deep(.el-table__body-wrapper) {
  overflow-x: hidden;
}

.submission-table :deep(.cell) {
  padding: 0 8px;
  white-space: nowrap;
}

.test-result-content {
  background-color: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 10px;
  font-family: 'Consolas', 'Monaco', monospace;
  white-space: pre-wrap;
  overflow-x: auto;
  min-height: 40px;
  max-height: 200px;
  overflow-y: auto;
}

.diff-content {
  margin: 0;
  line-height: 1.65;
}

.test-result-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #333;
}

.test-result-box {
  margin-bottom: 20px;
}

.test-results-container {
  margin-top: 15px;
}

/* AI 抽屉内容区覆盖样式 */
.ai-drawer :deep(.el-drawer) {
  display: flex;
  flex-direction: column;
}

.ai-drawer :deep(.el-drawer__header) {
  flex-shrink: 0;
}

.ai-drawer :deep(.el-drawer__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  height: auto;
}

/* AI 求助抽屉：压缩顶部装饰，把空间让给对话和输入 */
.ai-drawer :deep(.el-drawer__header) {
  padding: 12px 18px;
  background: #ffffff;
  border-bottom: 1px solid #e5edf7;
  box-shadow: none;
}

.ai-drawer :deep(.el-drawer__close-btn) {
  color: #64748b;
}

.ai-drawer :deep(.el-drawer__close-btn:hover) {
  color: #1f2937;
  background: #f1f5f9;
  transform: none;
}

.ai-drawer-kicker {
  color: #193549;
  font-size: 16px;
}

.ai-drawer-subtitle {
  color: #64748b;
}

.ai-drawer-meta :deep(.el-tag) {
  background: #f8fbff;
  border-color: #dbeafe;
  color: #2c78d4;
  backdrop-filter: none;
}

.ai-brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #f0f7ff;
  color: #0f766e;
  box-shadow: none;
}

.ai-brand-svg,
.ai-avatar-svg {
  width: 23px;
  height: 23px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.ai-assistant-container {
  height: 100%;
  min-height: 0;
  background: #f8fbff;
}

.ai-assistant-container::before {
  display: none;
}

.ai-context-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 48px;
  margin: 10px 12px 8px;
  padding: 9px 12px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e2edf8;
  box-shadow: none;
}

.ai-session-context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.ai-context-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.context-ready-dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  flex-shrink: 0;
  background: #22c55e;
  box-shadow: 0 0 0 5px rgba(34, 197, 94, 0.14);
}

.ai-context-title {
  color: #193549;
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-context-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  color: #64748b;
  font-size: 11px;
}

.ai-context-meta span {
  padding: 4px 8px;
  border-radius: 999px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  color: #2c78d4;
  white-space: nowrap;
}

.ai-session-card {
  margin: 8px 12px 8px;
  padding: 10px 12px;
  border-radius: 14px;
  box-shadow: none;
  background: #ffffff;
}

.ai-context-strip {
  display: none;
}

.ai-messages {
  flex: 1 1 auto;
  min-height: 0;
  padding: 10px 12px 10px;
  background: #f8fbff;
  overflow-x: hidden;
}

.msg-bubble {
  max-width: calc(100% - 48px);
}

.ai-suggestions {
  flex-shrink: 0;
  margin: 0 12px 8px;
  padding: 8px 10px;
  max-height: none;
  overflow: visible;
  border-radius: 12px;
  box-shadow: none;
}

.suggestions-head {
  align-items: center;
  margin-bottom: 6px;
}

.suggestions-subtitle {
  display: none;
}

.suggestion-chip {
  min-height: 32px;
  padding: 7px 9px;
  border-radius: 10px;
  line-height: 1.35;
}

.ai-input-bar {
  flex-shrink: 0;
  margin: 0 12px 10px;
  padding: 9px;
  border-radius: 12px;
  box-shadow: none;
}

@media (max-width: 760px) {
  .ai-drawer-header {
    align-items: flex-start;
  }

  .ai-drawer-meta {
    justify-content: flex-start;
  }

  .ai-context-bar {
    align-items: flex-start;
    flex-direction: column;
    margin: 10px 10px 8px;
  }

  .ai-context-meta {
    flex-wrap: wrap;
  }

  .ai-session-card,
  .ai-suggestions,
  .ai-input-bar {
    margin-left: 10px;
    margin-right: 10px;
  }

  .suggestions-grid {
    grid-template-columns: 1fr;
  }
}

/* 差异高亮样式 */
:deep(.diff-highlight) {
  background-color: #ffcccc;
  color: #ff0000;
  font-weight: bold;
  border-radius: 2px;
  padding: 0 2px;
}

:deep(.diff-highlight.space-highlight) {
  background-color: #ffcccc;
  border: 1px dashed #ff0000;
  color: transparent;
  position: relative;
}

:deep(.diff-highlight.space-highlight::after) {
  content: '␣';
  position: absolute;
  left: 0;
  top: -2px;
  color: #ff0000;
}

:deep(.diff-highlight.newline-highlight) {
  display: inline-block;
  min-width: 1.4em;
}

:deep(.diff-missing) {
  background-color: #e6f7ff;
  border-radius: 2px;
  padding: 0 2px;
  border-left: 2px solid #1890ff;
}

:deep(.missing-char) {
  color: #1890ff;
  font-weight: bold;
  text-decoration: underline;
}

:deep(.missing-space) {
  color: #1890ff;
  border: 1px dashed #1890ff;
}

:deep(.diff-info) {
  margin-top: 12px;
  padding: 10px 12px;
  background-color: #fff7e6;
  border-left: 4px solid #faad14;
  border-radius: 4px;
  color: #5f3b00;
  font-size: 13px;
  line-height: 1.6;
}

:deep(.error-message) {
  color: #ff4d4f;
  white-space: pre-wrap;
  font-family: monospace;
  background-color: #fff2f0;
  padding: 8px;
  border-left: 3px solid #ff4d4f;
  display: block;
  margin: 4px 0;
}
</style>
