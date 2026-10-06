<!-- 竞赛详情 — 题目列表、排行榜、做题入口 -->
<template>
  <div class="contest-detail" v-loading="loading">
    <div class="contest-header" v-if="contest">
      <el-button text @click="$router.push('/contests')" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回竞赛列表
      </el-button>
      <div class="title-row">
        <h1>{{ contest.title }}</h1>
        <el-tag v-if="contest.isOfficial === 1" type="warning" size="large">官方</el-tag>
        <el-tag v-if="contest.isPublic === 0" type="danger" size="large">非公开</el-tag>
        <el-tag :type="statusType" size="large">{{ statusText }}</el-tag>
      </div>
      <p class="contest-desc">{{ contest.description }}</p>
      <div class="contest-meta">
        <span>创建者：{{ contest.creatorName || '-' }}</span>
        <span>开始：{{ formatDate(contest.startTime) }}</span>
        <span>结束：{{ formatEndDate(contest) }}</span>
        <span>参赛人数：{{ contest.participantCount || 0 }}</span>
        <span v-if="contest.allowedLanguages">限制语言：{{ contest.allowedLanguages }}</span>
      </div>
      <div v-if="canViewInviteCode" class="invite-manage-card">
        <div class="invite-code-info">
          <span class="invite-label">邀请码</span>
          <strong>{{ contest.inviteCode }}</strong>
          <span class="invite-tip">非公开竞赛仅通过邀请码加入</span>
        </div>
        <el-button type="primary" link @click="copyContestInviteCode">复制邀请码</el-button>
      </div>

      <div class="timer-bar" v-if="contest.status !== 'ENDED'">
        <div class="timer-content" v-if="contest.status === 'UPCOMING'">
          <span class="timer-label">距离开始：</span>
          <span class="timer-value">{{ countdownText }}</span>
        </div>
        <div class="timer-content running" v-else-if="contest.status === 'RUNNING'">
          <span class="timer-label">剩余时间：</span>
          <span class="timer-value">{{ countdownText }}</span>
          <div class="timer-progress">
            <el-progress :percentage="progressPercent" :show-text="false" :stroke-width="6" />
          </div>
        </div>
      </div>

      <div class="register-area" v-if="contest.status === 'UPCOMING' && contest.canRegister">
        <el-button type="primary" size="large" @click="handleRegister">立即报名</el-button>
      </div>
      <div class="register-area" v-else-if="contest.status === 'UPCOMING' && contest.registered">
        <el-tag type="success" size="large">已报名，等待开始</el-tag>
      </div>
      <div class="register-area" v-else-if="contest.status === 'RUNNING' && contest.canAccessWorkspace">
        <el-button type="primary" size="large" @click="activeTab = 'problems'">进入比赛</el-button>
      </div>
      <div class="register-area" v-else-if="contest.status === 'RUNNING' && contest.participantTimeExpired">
        <el-tag type="danger" size="large">个人考试时长已到</el-tag>
      </div>
      <div class="register-area" v-else-if="contest.status === 'RUNNING' && contest.canRegister">
        <el-button type="primary" size="large" @click="handleRegister">立即参赛</el-button>
      </div>

      <div class="management-area" v-if="canManageContest">
        <el-button type="primary" plain @click="handleEditContest">编辑竞赛</el-button>
        <el-button type="warning" plain :icon="WarningFilled" @click="openAuditPanel" :loading="auditLoading">异常排查</el-button>
        <el-button type="success" plain @click="openExportPreview" :loading="exportPreviewLoading || exportingResults">导出成绩</el-button>
        <el-button type="danger" plain @click="handleDeleteContest">删除竞赛</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && !contest" description="竞赛不存在" />

    <el-tabs v-model="activeTab" v-if="contest" class="detail-tabs">
      <el-tab-pane label="题目列表" name="problems">
        <el-alert
          v-if="contest.status === 'UPCOMING'"
          title="竞赛尚未开始，开始后即可查看和做题"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-alert
          v-else-if="contest.status === 'RUNNING' && contest.canRegister"
          title="你还未参赛，点击上方按钮后即可进入比赛并开始做题"
          type="warning"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-alert
          v-else-if="contest.status !== 'UPCOMING' && !canAccessProblems"
          title="当前账号无权进入该竞赛工作区"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-table :data="pagedProblems" stripe>
          <el-table-column label="#" width="60">
            <template #default="{ $index }">{{ (problemPage - 1) * problemPageSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="题目">
            <template #default="{ row }">
              <el-link type="primary" @click="goToProblem(row)" :disabled="!canAccessProblems">
                {{ row.problemTitle || '未命名题目' }}
              </el-link>
              <div class="problem-progress-line">
                <span>{{ getProblemProgressText(row.problemId) }}</span>
                <span v-if="getProblemAttempts(row.problemId) > 0">
                  当前 {{ getProblemEarnedScore(row) }}/{{ getProblemTotalScore(row) }} 分
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="得分/总分" width="120">
            <template #default="{ row }">
              {{ getProblemEarnedScore(row) }} / {{ getProblemTotalScore(row) }}
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="100">
            <template #default="{ row }">
              <el-tag :type="difficultyTagType(row.difficulty)" size="small" :class="difficultyTagClass(row.difficulty)">
                {{ normalizeDifficultyLabel(row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="problems.length === 0" description="暂无题目" />
        <el-pagination
          v-if="problems.length > problemPageSize"
          class="problem-pagination"
          v-model:current-page="problemPage"
          :page-size="problemPageSize"
          :total="problems.length"
          layout="prev, pager, next"
          small
        />
      </el-tab-pane>

      <el-tab-pane label="实时排名" name="ranking">
        <div class="ranking-toolbar">
          <div class="ranking-toolbar-left">
            <span class="toolbar-title">排行榜视图</span>
            <el-radio-group v-model="rankingFilter" size="small" @change="handleRankingFilterChange">
              <el-radio-button label="active" value="active">活跃选手</el-radio-button>
              <el-radio-button label="submitted" value="submitted">有提交</el-radio-button>
              <el-radio-button label="all" value="all">全部报名</el-radio-button>
            </el-radio-group>
          </div>
        <div class="ranking-toolbar-right">
            <el-button v-if="canExportResults" text type="warning" :icon="View" @click="openAuditPanel" :loading="auditLoading">
              查看排查
            </el-button>
            <el-button v-if="canExportResults" text type="primary" @click="openExportPreview" :loading="exportPreviewLoading || exportingResults">
              导出成绩
            </el-button>
            <el-button text type="primary" @click="loadRanking" :loading="rankingLoading">刷新排名</el-button>
            <span class="auto-hint" v-if="contest.status === 'RUNNING'">排名每 30 秒自动刷新</span>
          </div>
        </div>

        <div v-if="ranking.length > 0" class="ranking-stage">
          <div class="podium-panel">
            <div class="panel-header">
              <span>前三名概况</span>
              <span class="panel-tip">展示当前前三名和你的名次</span>
            </div>
            <div class="podium-list">
              <div
                v-for="user in podiumUsers"
                :key="user.id"
                class="podium-card"
                :class="[`podium-rank-${user.rank}`, { 'is-self': user.isCurrentUser, disabled: !canViewContestSubmission(user) }]"
                @click="canViewContestSubmission(user) && openUserSubmissions(user)"
              >
                <div class="podium-medal">{{ medalText(user.rank) }}</div>
                <el-avatar :src="user.avatar" :size="58">{{ user.username?.slice(0, 1) }}</el-avatar>
                <div class="podium-name-row">
                  <span class="podium-name">{{ user.username }}</span>
                  <el-tag v-if="user.isCurrentUser" size="small" type="danger">我</el-tag>
                  <el-tag v-else-if="user.isCreator" size="small" type="warning">主理人</el-tag>
                </div>
                <div class="podium-meta">第 {{ user.rank }} 名 · {{ user.solvedCount || 0 }} 题</div>
                <div class="podium-trend" :class="movementClass(user.rankDelta)">
                  {{ movementText(user.rankDelta) }}
                </div>
              </div>
            </div>
          </div>

          <div class="avatar-lane-panel">
            <div class="panel-header">
              <span>排名位置</span>
              <span class="panel-tip">展示榜单前列和你的当前位置</span>
            </div>
            <div class="avatar-lane">
              <template v-for="entry in laneEntries" :key="entry.key">
                <div v-if="entry.type === 'ellipsis'" class="lane-ellipsis">
                  <span></span><span></span><span></span>
                </div>
                <button
                  v-else
                  type="button"
                  class="lane-user"
                  :class="[
                    `lane-rank-${entry.user.rank <= 3 ? entry.user.rank : 'normal'}`,
                    { 'is-self': entry.user.isCurrentUser, 'is-moved-up': (entry.user.rankDelta || 0) > 0, 'is-moved-down': (entry.user.rankDelta || 0) < 0, disabled: !canViewContestSubmission(entry.user) }
                  ]"
                  @click="canViewContestSubmission(entry.user) && openUserSubmissions(entry.user)"
                >
                  <span class="lane-rank">#{{ entry.user.rank }}</span>
                  <el-avatar :src="entry.user.avatar" :size="36">{{ entry.user.username?.slice(0, 1) }}</el-avatar>
                  <div class="lane-user-meta">
                    <div class="lane-name-row">
                      <span class="lane-name">{{ entry.user.username }}</span>
                      <span v-if="entry.user.isCurrentUser" class="lane-self-badge">YOU</span>
                    </div>
                    <div class="lane-subline">
                      <span>{{ entry.user.solvedCount || 0 }} 题</span>
                      <span :class="movementClass(entry.user.rankDelta)">{{ movementText(entry.user.rankDelta) }}</span>
                    </div>
                  </div>
                  <span class="lane-arrow" v-if="entry.user.isCurrentUser">◀</span>
                </button>
              </template>
            </div>
          </div>
        </div>

        <el-table :data="ranking" stripe :row-class-name="rankRowClass" v-if="ranking.length > 0">
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
          <el-table-column v-for="(p, idx) in problems" :key="p.problemId" :label="'#' + (idx + 1)" width="90" align="center">
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
              <span v-else class="table-muted">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无排名数据" />
      </el-tab-pane>

      <el-tab-pane v-if="canManageContest" name="audit">
        <template #label>
          <span class="audit-tab-label">
            异常排查
            <el-badge v-if="auditSummary.total" :value="auditSummary.total" type="warning" />
          </span>
        </template>

        <div class="audit-panel" v-loading="auditLoading">
          <el-alert
            title="以下内容是面向创建者和管理员的反作弊辅助分析，不代表系统自动判定违规。"
            type="warning"
            show-icon
            :closable="false"
            class="audit-alert"
          />

          <div class="audit-summary-grid">
            <div class="audit-summary-card">
              <span>线索总数</span>
              <strong>{{ auditSummary.total || 0 }}</strong>
            </div>
            <div class="audit-summary-card high">
              <span>高优先级</span>
              <strong>{{ auditSummary.highCount || 0 }}</strong>
            </div>
            <div class="audit-summary-card medium">
              <span>中优先级</span>
              <strong>{{ auditSummary.mediumCount || 0 }}</strong>
            </div>
            <div class="audit-summary-card risk">
              <span>最高风险分</span>
              <strong>{{ auditSummary.maxRiskScore || 0 }}</strong>
            </div>
          </div>

          <div class="audit-toolbar">
            <el-select v-model="auditFilters.severity" placeholder="优先级" clearable>
              <el-option label="高" value="HIGH" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="低" value="LOW" />
              <el-option label="提示" value="INFO" />
            </el-select>
            <el-select v-model="auditFilters.type" placeholder="类型" clearable>
              <el-option v-for="type in auditTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
            </el-select>
            <el-input v-model="auditFilters.keyword" placeholder="搜索用户、题目或说明" clearable />
            <el-button :icon="RefreshRight" @click="loadAudit" :loading="auditLoading">刷新</el-button>
            <el-button :icon="Download" @click="openExportPreview" :loading="exportPreviewLoading || exportingResults">下载 Excel</el-button>
          </div>

          <div class="audit-list">
            <article
              v-for="(row, index) in filteredAuditItems"
              :key="`${row.type || 'audit'}-${row.problemId || 'all'}-${row.userScope || index}-${index}`"
              class="audit-card"
              :class="auditSeverityClass(row.severity)"
            >
              <div class="audit-card-head">
                <div class="audit-card-title">
                  <el-tag :type="auditSeverityTagType(row.severity)" size="small">
                    {{ row.severityText || auditSeverityText(row.severity) }}
                  </el-tag>
                  <strong>{{ row.typeText || row.type || '复核线索' }}</strong>
                  <span class="audit-risk-pill" :class="auditRiskClass(row.riskScore)">风险分 {{ row.riskScore ?? '-' }}</span>
                </div>
                <time>{{ formatDate(row.occurredAt) }}</time>
              </div>

              <div class="audit-card-meta">
                <span>
                  <label>用户/范围</label>
                  {{ row.userScope || '-' }}
                </span>
                <span>
                  <label>题目</label>
                  {{ row.problemTitle || '-' }}
                </span>
                <span v-if="row.affectedUserCount">
                  <label>涉及用户</label>
                  {{ row.affectedUserCount }} 人
                </span>
                <span v-if="row.codeVariantCount && row.codeVariantCount > 1">
                  <label>代码变体</label>
                  {{ row.codeVariantCount }} 组
                </span>
                <span v-if="row.submissionIds?.length">
                  <label>相关提交</label>
                  {{ row.submissionIds.length }} 次
                </span>
              </div>

              <div class="audit-factor-tags audit-card-factors">
                <el-tag v-for="factor in row.riskFactors || []" :key="factor" size="small" effect="plain">{{ factor }}</el-tag>
              </div>

              <div class="audit-card-body">
                <section>
                  <span>反作弊分析</span>
                  <p>{{ row.antiCheatSummary || '-' }}</p>
                </section>
                <section>
                  <span>处理建议</span>
                  <p>{{ row.suggestion || row.reviewAction || '-' }}</p>
                </section>
                <section class="wide">
                  <span>线索说明</span>
                  <p>{{ row.description || '-' }}</p>
                </section>
              </div>

              <div class="audit-card-foot">
                <div v-if="row.submissionIds?.length" class="audit-submission-links">
                  <el-button
                    v-for="id in row.submissionIds.slice(0, 6)"
                    :key="id"
                    text
                    type="primary"
                    @click="viewSubmissionById(id)"
                  >
                    #{{ id }}
                  </el-button>
                  <span v-if="row.submissionIds.length > 6" class="table-muted">+{{ row.submissionIds.length - 6 }}</span>
                </div>
                <span v-else class="table-muted">暂无相关提交</span>
              </div>
            </article>
          </div>

          <el-empty v-if="!auditLoading && filteredAuditItems.length === 0" description="暂无符合条件的复核线索" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="submissionDialogVisible"
      width="980px"
      destroy-on-close
      :title="selectedRankingUser ? `${selectedRankingUser.username} 的竞赛提交记录` : '竞赛提交记录'"
    >
      <div v-if="selectedRankingUser" class="submission-dialog-header">
        <div class="submission-user-card">
          <el-avatar :src="selectedRankingUser.avatar" :size="48">{{ selectedRankingUser.username?.slice(0, 1) }}</el-avatar>
          <div>
            <div class="submission-user-title">
              <span>{{ selectedRankingUser.username }}</span>
              <el-tag v-if="selectedRankingUser.isCurrentUser" size="small" type="danger">我</el-tag>
              <el-tag v-else-if="selectedRankingUser.isCreator" size="small" type="warning">创建者</el-tag>
            </div>
            <div class="submission-user-subtitle">
              当前第 {{ selectedRankingUser.rank }} 名 · 解出 {{ selectedRankingUser.solvedCount || 0 }} 题 · 总分 {{ selectedRankingUser.totalScore || 0 }}
            </div>
          </div>
        </div>
      </div>

      <div class="submission-filter-bar">
        <el-select v-model="submissionFilters.problemId" placeholder="题目" clearable filterable>
          <el-option
            v-for="p in problems"
            :key="p.problemId"
            :label="p.problemTitle || `题目 ${p.problemId}`"
            :value="String(p.problemId)"
          />
        </el-select>
        <el-select v-model="submissionFilters.language" placeholder="语言" clearable>
          <el-option v-for="language in submissionLanguageOptions" :key="language" :label="language" :value="language" />
        </el-select>
        <el-select v-model="submissionFilters.status" placeholder="运行结果" clearable>
          <el-option v-for="status in submissionStatusOptions" :key="status" :label="submissionStatusText(status)" :value="status" />
        </el-select>
        <el-button @click="resetSubmissionFilters">重置</el-button>
        <span class="submission-filter-count">共 {{ filteredSubmissionRecords.length }} 条</span>
      </div>

      <el-table :data="pagedSubmissionRecords" stripe v-loading="submissionLoading" class="submission-record-table">
        <el-table-column label="题目" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ getProblemTitle(row.problemId) }}</template>
        </el-table-column>
        <el-table-column prop="language" label="语言" width="120" />
        <el-table-column prop="status" label="运行结果" width="130">
          <template #default="{ row }">
            <el-tag :type="submissionStatusType(row.status)" size="small">{{ submissionStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="90" />
        <el-table-column prop="executionTime" label="耗时(ms)" width="110" />
        <el-table-column prop="memoryUsed" label="内存(KB)" width="110" />
        <el-table-column prop="createdAt" label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="viewSubmissionDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!submissionLoading && filteredSubmissionRecords.length === 0" description="暂无符合条件的提交记录" />

      <el-pagination
        v-if="filteredSubmissionRecords.length > submissionPageSize"
        class="problem-pagination"
        v-model:current-page="submissionPage"
        :page-size="submissionPageSize"
        :total="filteredSubmissionRecords.length"
        layout="prev, pager, next"
        small
        @current-change="handleSubmissionPageChange"
      />
    </el-dialog>

    <el-dialog
      v-model="exportPreviewVisible"
      width="1080px"
      destroy-on-close
      title="成绩导出预览"
    >
      <div v-loading="exportPreviewLoading" class="export-preview">
        <div class="preview-summary-grid">
          <div class="preview-summary-card">
            <span>报名人数</span>
            <strong>{{ exportPreviewSummary.participantCount }}</strong>
          </div>
          <div class="preview-summary-card">
            <span>入场人数</span>
            <strong>{{ exportPreviewSummary.enteredCount }}</strong>
          </div>
          <div class="preview-summary-card">
            <span>有提交人数</span>
            <strong>{{ exportPreviewSummary.submittedCount }}</strong>
          </div>
          <div class="preview-summary-card">
            <span>最高分</span>
            <strong>{{ exportPreviewSummary.highestScore }}</strong>
          </div>
        </div>

        <div class="preview-section">
          <div class="section-title">题目统计</div>
          <el-table :data="exportProblemStats" stripe size="small">
            <el-table-column prop="index" label="#" width="60" />
            <el-table-column prop="title" label="题目" min-width="190" show-overflow-tooltip />
            <el-table-column prop="score" label="分值" width="80" />
            <el-table-column prop="attemptedCount" label="尝试人数" width="100" />
            <el-table-column prop="acceptedCount" label="通过人数" width="100" />
            <el-table-column prop="attemptCount" label="提交次数" width="100" />
            <el-table-column prop="acceptedRate" label="通过率" width="100" />
          </el-table>
        </div>

        <div class="preview-section">
          <div class="section-title">排名预览</div>
          <el-table :data="exportPreviewRanking.slice(0, 10)" stripe size="small">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="username" label="用户" min-width="160" />
            <el-table-column prop="solvedCount" label="通过题数" width="100" />
            <el-table-column prop="totalPenalty" label="罚时(分钟)" width="110" />
            <el-table-column prop="totalScore" label="总分" width="90" />
            <el-table-column prop="lastSubmissionAt" label="最近提交" min-width="170">
              <template #default="{ row }">{{ formatDate(row.lastSubmissionAt) }}</template>
            </el-table-column>
          </el-table>
          <div class="preview-hint">Excel 会分为总览、成绩明细、题目统计、提交历史、代码留痕、测试点明细和异常排查；这里仅展示前 10 名预览。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="exportPreviewVisible = false">取消</el-button>
        <el-button type="primary" @click="exportContestResults" :loading="exportingResults">下载 Excel</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      width="1080px"
      destroy-on-close
      title="提交详情"
    >
      <div v-loading="detailLoading" class="submission-detail" v-if="submissionDetail">
        <div class="detail-meta-grid">
          <div><span>题目</span><strong>{{ getProblemTitle(submissionDetail.problemId) }}</strong></div>
          <div><span>用户</span><strong>{{ submissionDetail.user?.username || '-' }}</strong></div>
          <div><span>语言</span><strong>{{ submissionDetail.language || '-' }}</strong></div>
          <div><span>状态</span><strong>{{ submissionStatusText(submissionDetail.status) }}</strong></div>
          <div><span>分数</span><strong>{{ submissionDetail.score ?? '-' }}</strong></div>
          <div><span>耗时</span><strong>{{ submissionDetail.executionTime ?? '-' }} ms</strong></div>
          <div><span>内存</span><strong>{{ submissionDetail.memoryUsed ?? '-' }} KB</strong></div>
          <div><span>提交时间</span><strong>{{ formatDate(submissionDetail.createdAt) }}</strong></div>
        </div>

        <div class="detail-section" v-if="submissionDetail.errorMessage">
          <div class="section-title">错误信息</div>
          <pre class="detail-pre error">{{ submissionDetail.errorMessage }}</pre>
        </div>

        <div class="detail-section">
          <div class="section-title">代码</div>
          <pre class="detail-pre code">{{ submissionDetail.code || '' }}</pre>
        </div>

        <div class="detail-section">
          <div class="section-title">测试点结果</div>
          <el-table :data="submissionDetail.testResults || []" stripe>
            <el-table-column prop="testCaseId" label="测试点" width="90" />
            <el-table-column prop="status" label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="submissionStatusType(row.status)" size="small">{{ row.status || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executionTime" label="耗时(ms)" width="110" />
            <el-table-column prop="memoryUsed" label="内存(KB)" width="110" />
            <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, onActivated, onDeactivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Download, RefreshRight, View, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contestApi, type RankingFilter } from '@/api/contest'
import { submissionApi } from '@/api/submission'
import { apiDateTimeMs, formatApiDateTime, parseApiDate } from '@/utils/datetime'

// 竞赛详情页面：负责竞赛题目、实时排名、提交下钻、成绩导出和异常排查展示。

const route = useRoute()
const router = useRouter()
const contestId = Number(route.params.id)

type ContestStatus = 'UPCOMING' | 'RUNNING' | 'ENDED' | 'DRAFT'
type SubmissionStatus = 'ACCEPTED' | 'WRONG_ANSWER' | 'RUNTIME_ERROR' | 'TIME_LIMIT_EXCEEDED' | 'PENDING' | 'RUNNING' | string

type ContestProblem = {
  problemId: number
  problemTitle?: string
  score?: number
  difficulty?: string
}

type RankingProblemCell = {
  accepted?: boolean
  acTime?: number
  wrongAttempts?: number
  score?: number
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

type RankingUser = {
  id: number
  userId?: number
  rank: number
  username?: string
  avatar?: string
  isCurrentUser?: boolean
  isCreator?: boolean
  entered?: boolean
  hasSubmission?: boolean
  solvedCount?: number
  totalPenalty?: number
  totalScore?: number
  rankDelta?: number
  enteredAt?: string
  lastSubmissionAt?: string
  problems?: RankingProblemCell[]
}

type SubmissionRecord = {
  id: number
  problemId: number
  language?: string
  status?: SubmissionStatus
  score?: number
  executionTime?: number
  memoryUsed?: number
  createdAt?: string
}

type SubmissionDetail = SubmissionRecord & {
  code?: string
  errorMessage?: string
  user?: {
    username?: string
  }
  testResults?: Array<{
    testCaseId?: number
    status?: SubmissionStatus
    executionTime?: number
    memoryUsed?: number
    errorMessage?: string
  }>
}

type AuditSeverity = 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO' | string

type ContestAuditSummary = {
  total?: number
  highCount?: number
  mediumCount?: number
  lowCount?: number
  infoCount?: number
  maxRiskScore?: number
  averageRiskScore?: number
  participantCount?: number
  submissionCount?: number
  generatedAt?: string
  note?: string
}

type ContestAuditItem = {
  type?: string
  typeText?: string
  severity?: AuditSeverity
  severityText?: string
  riskScore?: number
  riskFactors?: string[]
  antiCheatSummary?: string
  reviewAction?: string
  userScope?: string
  problemId?: number
  problemTitle?: string
  description?: string
  suggestion?: string
  submissionIds?: number[]
  occurredAt?: string
  relatedCount?: number
  affectedUserCount?: number
  codeVariantCount?: number
  hashPrefixes?: string[]
}

type ContestDetail = {
  id?: number
  creatorId?: number
  title?: string
  description?: string
  isOfficial?: number
  isPublic?: number
  inviteCode?: string
  canManage?: boolean
  creatorName?: string
  startTime?: string
  endTime?: string
  participantCount?: number
  allowedLanguages?: string
  status?: ContestStatus
  type?: string
  durationMinutes?: number
  canRegister?: boolean
  registered?: boolean
  canAccessWorkspace?: boolean
  password?: string
  participantDeadline?: string
  participantRemainingSeconds?: number
  participantTimeExpired?: boolean
}

type LaneEntry =
  | { key: string; type: 'ellipsis' }
  | { key: string; type: 'user'; user: RankingUser }

const loading = ref(true)
const rankingLoading = ref(false)
const exportingResults = ref(false)
const exportPreviewVisible = ref(false)
const exportPreviewLoading = ref(false)
const auditLoading = ref(false)
const submissionLoading = ref(false)
const detailLoading = ref(false)
const contest = ref<ContestDetail | null>(null)
const problems = ref<ContestProblem[]>([])
const problemProgressMap = ref<Record<number, ContestProblemProgress | string | null>>({})
const ranking = ref<RankingUser[]>([])
const activeTab = ref('problems')
const countdownText = ref('')
const progressPercent = ref(0)
const problemPage = ref(1)
const problemPageSize = 10
const rankingFilter = ref<RankingFilter>('active')
const previousRankMap = ref<Record<number, number>>({})
const submissionDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const selectedRankingUser = ref<RankingUser | null>(null)
const submissionRecords = ref<SubmissionRecord[]>([])
const submissionPage = ref(1)
const submissionPageSize = 10
const submissionFilters = ref({
  problemId: '',
  language: '',
  status: ''
})
const submissionDetail = ref<SubmissionDetail | null>(null)
const exportPreviewRanking = ref<RankingUser[]>([])
const auditItems = ref<ContestAuditItem[]>([])
const auditSummary = ref<ContestAuditSummary>({})
const auditFilters = ref({
  severity: '',
  type: '',
  keyword: ''
})

const pagedProblems = computed(() => {
  const start = (problemPage.value - 1) * problemPageSize
  return problems.value.slice(start, start + problemPageSize)
})

const problemTitleMap = computed(() => {
  const map = new Map<number, string>()
  problems.value.forEach(problem => {
    map.set(Number(problem.problemId), problem.problemTitle || `题目 ${problem.problemId}`)
  })
  return map
})

const getProblemTitle = (problemId?: number) => {
  if (!problemId) return '-'
  return problemTitleMap.value.get(Number(problemId)) || `题目 ${problemId}`
}

const submissionLanguageOptions = computed(() => {
  return Array.from(new Set(submissionRecords.value.map(item => item.language).filter(Boolean))) as string[]
})

const submissionStatusOptions = computed(() => {
  return Array.from(new Set(submissionRecords.value.map(item => item.status).filter(Boolean))) as SubmissionStatus[]
})

const filteredSubmissionRecords = computed(() => {
  return submissionRecords.value.filter(item => {
    if (submissionFilters.value.problemId && String(item.problemId) !== submissionFilters.value.problemId) return false
    if (submissionFilters.value.language && item.language !== submissionFilters.value.language) return false
    if (submissionFilters.value.status && item.status !== submissionFilters.value.status) return false
    return true
  })
})

const pagedSubmissionRecords = computed(() => {
  const start = (submissionPage.value - 1) * submissionPageSize
  return filteredSubmissionRecords.value.slice(start, start + submissionPageSize)
})

const exportPreviewSummary = computed(() => {
  const list = exportPreviewRanking.value
  const participantCount = list.length
  const enteredCount = list.filter(item => item.entered).length
  const submittedCount = list.filter(item => item.hasSubmission).length
  const acceptedUserCount = list.filter(item => Number(item.solvedCount || 0) > 0).length
  const highestScore = list.reduce((max, item) => Math.max(max, Number(item.totalScore || 0)), 0)
  const averageScore = participantCount > 0
    ? Math.round(list.reduce((sum, item) => sum + Number(item.totalScore || 0), 0) / participantCount)
    : 0
  const totalAttempts = list.reduce((sum, item) => {
    return sum + (item.problems || []).reduce((inner, cell) => inner + Number(cell.wrongAttempts || 0) + (cell.accepted ? 1 : 0), 0)
  }, 0)
  return { participantCount, enteredCount, submittedCount, acceptedUserCount, highestScore, averageScore, totalAttempts }
})

const exportProblemStats = computed(() => {
  const list = exportPreviewRanking.value
  return problems.value.map((problem, index) => {
    let acceptedCount = 0
    let attemptedCount = 0
    let attemptCount = 0
    list.forEach(user => {
      const cell = user.problems?.[index]
      if (!cell) return
      const attempts = Number(cell.wrongAttempts || 0) + (cell.accepted ? 1 : 0)
      if (attempts > 0) attemptedCount++
      if (cell.accepted) acceptedCount++
      attemptCount += attempts
    })
    return {
      index: index + 1,
      problemId: problem.problemId,
      title: problem.problemTitle || `题目 ${problem.problemId}`,
      score: problem.score || 100,
      attemptedCount,
      acceptedCount,
      attemptCount,
      acceptedRate: attemptedCount > 0 ? `${Math.round((acceptedCount / attemptedCount) * 100)}%` : '-'
    }
  })
})

const auditTypeOptions = computed(() => {
  const map = new Map<string, string>()
  auditItems.value.forEach(item => {
    if (item.type) {
      map.set(item.type, item.typeText || item.type)
    }
  })
  return Array.from(map.entries()).map(([value, label]) => ({ value, label }))
})

const filteredAuditItems = computed(() => {
  const keyword = auditFilters.value.keyword.trim().toLowerCase()
  return auditItems.value.filter(item => {
    if (auditFilters.value.severity && item.severity !== auditFilters.value.severity) return false
    if (auditFilters.value.type && item.type !== auditFilters.value.type) return false
    if (!keyword) return true
    const text = [
      item.typeText,
      item.severityText,
      item.userScope,
      item.problemTitle,
      item.description,
      item.suggestion,
      item.antiCheatSummary,
      item.reviewAction,
      item.riskFactors?.join(','),
      item.hashPrefixes?.join(','),
      item.submissionIds?.join(',')
    ].filter(Boolean).join(' ').toLowerCase()
    return text.includes(keyword)
  })
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

const currentUserId = computed<number | null>(() => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return u.id ?? null
  } catch {
    return null
  }
})

const currentUserRole = computed<string | null>(() => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return u.role ?? null
  } catch {
    return null
  }
})

const currentRankingEntry = computed(() => {
  if (!currentUserId.value) return null
  return ranking.value.find(item => Number(item.userId || item.id) === Number(currentUserId.value)) || null
})

const normalizeProblemProgress = (value: ContestProblemProgress | string | null | undefined): ContestProblemProgress => {
  if (!value) return { status: null, attempts: 0, finalAttempts: 0, bestScorePercent: 0, earnedScore: 0 }
  if (typeof value === 'string') {
    return {
      status: value,
      accepted: value === 'ACCEPTED',
      attempts: value ? 1 : 0,
      finalAttempts: value && value !== 'PENDING' && value !== 'RUNNING' ? 1 : 0,
      bestScorePercent: value === 'ACCEPTED' ? 100 : 0
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

const getRankingProblemCell = (problemId?: number) => {
  if (!problemId) return null
  const index = problems.value.findIndex(item => Number(item.problemId) === Number(problemId))
  if (index < 0) return null
  return currentRankingEntry.value?.problems?.[index] || null
}

const getProblemProgress = (problemId?: number): ContestProblemProgress => {
  if (!problemId) return { status: null, attempts: 0, finalAttempts: 0, bestScorePercent: 0, earnedScore: 0 }
  const progress = normalizeProblemProgress(problemProgressMap.value[Number(problemId)])
  if (progress.status || Number(progress.attempts || 0) > 0) return progress

  const rankingCell = getRankingProblemCell(problemId)
  if (!rankingCell) return progress
  const attempts = Number(rankingCell.wrongAttempts || 0) + (rankingCell.accepted ? 1 : 0)
  return {
    status: rankingCell.accepted ? 'ACCEPTED' : attempts > 0 ? 'ATTEMPTED' : null,
    accepted: !!rankingCell.accepted,
    attempts,
    finalAttempts: attempts,
    earnedScore: Number(rankingCell.score || 0)
  }
}

const getProblemTotalScore = (rowOrProblemId: ContestProblem | number) => {
  const row = typeof rowOrProblemId === 'object'
    ? rowOrProblemId
    : problems.value.find(item => Number(item.problemId) === Number(rowOrProblemId))
  const progress = row?.problemId ? getProblemProgress(row.problemId) : null
  return Number(progress?.totalScore ?? row?.score ?? 100)
}

const getProblemEarnedScore = (rowOrProblemId: ContestProblem | number) => {
  const pid = Number(typeof rowOrProblemId === 'object' ? rowOrProblemId.problemId : rowOrProblemId)
  const progress = getProblemProgress(pid)
  const totalScore = getProblemTotalScore(rowOrProblemId)
  if (progress.status === 'ACCEPTED') return totalScore
  if (Number.isFinite(Number(progress.earnedScore))) {
    return Math.max(0, Math.min(totalScore, Math.round(Number(progress.earnedScore || 0))))
  }
  const percent = Math.max(0, Math.min(100, Number(progress.bestScorePercent || 0)))
  return Math.max(0, Math.min(totalScore, Math.round(percent * totalScore / 100)))
}

const getProblemAttempts = (problemId?: number) => Number(getProblemProgress(problemId).attempts || 0)

const getProblemProgressText = (problemId?: number) => {
  const status = getProblemProgress(problemId).status
  if (status === 'ACCEPTED') return '已通过'
  if (status === 'PENDING' || status === 'RUNNING') return '评测中'
  if (status === 'ATTEMPTED') return '已提交'
  return '未尝试'
}

const canExportResults = computed(() => {
  if (!contest.value || !currentUserId.value) return false
  return Number(contest.value.creatorId) === Number(currentUserId.value) || currentUserRole.value === 'ADMIN'
})

const canManageContest = computed(() => !!contest.value?.canManage || canExportResults.value)
const canViewInviteCode = computed(() => canManageContest.value && contest.value?.isPublic === 0 && !!contest.value?.inviteCode)

const canViewContestSubmission = (user: RankingUser) => {
  if (!user) return false
  if (user.isCurrentUser || Number(user.userId || user.id) === Number(currentUserId.value)) return true
  if (currentUserRole.value === 'ADMIN') return true
  return Number(contest.value?.creatorId) === Number(currentUserId.value)
}

const canAccessProblems = computed(() => {
  if (!contest.value) return false
  return !!contest.value.canAccessWorkspace
})

const copyContestInviteCode = async () => {
  if (!contest.value?.inviteCode) return
  await navigator.clipboard.writeText(contest.value.inviteCode)
  ElMessage.success('邀请码已复制')
}

const podiumUsers = computed(() => ranking.value.filter(item => item.rank <= 3))

const laneEntries = computed<LaneEntry[]>(() => {
  if (ranking.value.length <= 8) {
    return ranking.value.map(user => ({ key: `user-${user.userId || user.id}`, type: 'user', user }))
  }

  const keep = new Set<number>()
  ranking.value.slice(0, 5).forEach(user => keep.add(Number(user.userId || user.id)))
  const selfIndex = ranking.value.findIndex(user => user.isCurrentUser)
  if (selfIndex >= 0) {
    ranking.value.slice(Math.max(0, selfIndex - 1), Math.min(ranking.value.length, selfIndex + 2)).forEach(user => keep.add(Number(user.userId || user.id)))
  }
  const lastUser = ranking.value[ranking.value.length - 1]
  if (lastUser) keep.add(Number(lastUser.userId || lastUser.id))

  const result: LaneEntry[] = []
  let inGap = false
  for (const user of ranking.value) {
    const uid = Number(user.userId || user.id)
    if (keep.has(uid)) {
      inGap = false
      result.push({ key: `user-${uid}`, type: 'user', user })
      continue
    }
    if (!inGap) {
      result.push({ key: `ellipsis-${uid}`, type: 'ellipsis' })
      inGap = true
    }
  }
  return result
})

let countdownTimer: ReturnType<typeof setInterval> | null = null
let rankingTimer: ReturnType<typeof setInterval> | null = null
let rankingRequestInFlight = false

const statusText = computed(() => {
  const map: Record<ContestStatus, string> = { UPCOMING: '即将开始', RUNNING: '进行中', ENDED: '已结束', DRAFT: '草稿' }
  return contest.value?.status ? map[contest.value.status] : undefined
})

const statusType = computed(() => {
  const map: Record<ContestStatus, string> = { UPCOMING: '', RUNNING: 'success', ENDED: 'info', DRAFT: 'warning' }
  return contest.value?.status ? map[contest.value.status] : 'info'
})

const medalText = (rank: number) => {
  if (rank === 1) return '金'
  if (rank === 2) return '银'
  if (rank === 3) return '铜'
  return '榜'
}

const movementText = (rankDelta?: number) => {
  if (!rankDelta) return '—'
  return rankDelta > 0 ? `↑${rankDelta}` : `↓${Math.abs(rankDelta)}`
}

const movementClass = (rankDelta?: number) => {
  if (!rankDelta) return 'movement-flat'
  return rankDelta > 0 ? 'movement-up' : 'movement-down'
}

const submissionStatusType = (status?: SubmissionStatus) => {
  if (status === 'ACCEPTED') return 'success'
  if (status === 'WRONG_ANSWER' || status === 'RUNTIME_ERROR' || status === 'TIME_LIMIT_EXCEEDED' || status === 'COMPILE_ERROR' || status === 'COMPILATION_ERROR') return 'danger'
  if (status === 'PENDING' || status === 'RUNNING') return 'warning'
  return 'info'
}

const submissionStatusText = (status?: SubmissionStatus) => {
  const map: Record<string, string> = {
    ACCEPTED: '通过',
    WRONG_ANSWER: '答案错误',
    COMPILE_ERROR: '编译错误',
    COMPILATION_ERROR: '编译错误',
    RUNTIME_ERROR: '运行错误',
    TIME_LIMIT_EXCEEDED: '超时',
    MEMORY_LIMIT_EXCEEDED: '内存超限',
    PENDING: '等待中',
    RUNNING: '运行中'
  }
  return status ? map[status] || status : '-'
}

const auditSeverityText = (severity?: AuditSeverity) => {
  if (severity === 'HIGH') return '高'
  if (severity === 'MEDIUM') return '中'
  if (severity === 'LOW') return '低'
  if (severity === 'INFO') return '提示'
  return severity || '-'
}

const auditSeverityTagType = (severity?: AuditSeverity) => {
  if (severity === 'HIGH') return 'danger'
  if (severity === 'MEDIUM') return 'warning'
  if (severity === 'LOW') return 'info'
  return 'success'
}

const auditRiskClass = (score?: number) => {
  const value = Number(score || 0)
  if (value >= 80) return 'high'
  if (value >= 60) return 'medium'
  if (value >= 30) return 'low'
  return 'info'
}

const auditSeverityClass = (severity?: AuditSeverity) => {
  if (severity === 'HIGH') return 'severity-high'
  if (severity === 'MEDIUM') return 'severity-medium'
  if (severity === 'LOW') return 'severity-low'
  return 'severity-info'
}

const resetSubmissionFilters = () => {
  submissionFilters.value = { problemId: '', language: '', status: '' }
  submissionPage.value = 1
}

// 批量加载当前用户在本场竞赛每道题上的状态和得分，避免题目列表误显示“未尝试”。
const loadProblemProgress = async () => {
  if (!problems.value.length) {
    problemProgressMap.value = {}
    return
  }
  try {
    const res: any = await submissionApi.getUserContestProblemStatus(
      contestId,
      problems.value.map(problem => problem.problemId)
    )
    const data = res.data || res || {}
    problemProgressMap.value = Object.fromEntries(
      Object.entries(data).map(([key, value]) => [Number(key), normalizeProblemProgress(value as any)])
    )
  } catch (e) {
    console.warn('加载竞赛题目得分状态失败', e)
    problemProgressMap.value = {}
  }
}

// 加载竞赛详情和题目列表，决定用户是否能进入竞赛工作区。
const loadContest = async () => {
  loading.value = true
  try {
    const [res, pRes] = await Promise.all([
      contestApi.getContestDetail(contestId),
      contestApi.getContestProblems(contestId)
    ])
    contest.value = res.data || res
    problems.value = pRes.data || pRes || []
    await loadProblemProgress()
  } catch (e) {
    console.error('加载竞赛失败', e)
  } finally {
    loading.value = false
  }
}

// 加载排行榜，并计算名次变化，用于前端展示排名升降。
const loadRanking = async (options: { silent?: boolean } = {}) => {
  if (rankingRequestInFlight) return
  rankingRequestInFlight = true
  if (!options.silent) {
    rankingLoading.value = true
  }
  try {
    const res = await contestApi.getRanking(contestId, rankingFilter.value)
    const rawList = (res.data || res || []) as RankingUser[]
    const nextMap: Record<number, number> = {}
    ranking.value = rawList.map(item => {
      const uid = Number(item.userId || item.id)
      const previousRank = previousRankMap.value[uid]
      const rankDelta = previousRank ? previousRank - item.rank : 0
      if (uid) nextMap[uid] = item.rank
      return {
        ...item,
        rankDelta
      }
    })
    previousRankMap.value = nextMap
  } catch (e) {
    console.error('加载排名失败', e)
  } finally {
    rankingRequestInFlight = false
    if (!options.silent) {
      rankingLoading.value = false
    }
  }
}

// 加载竞赛异常排查线索，只有创建者和管理员可见。
const loadAudit = async () => {
  if (!canManageContest.value) return
  auditLoading.value = true
  try {
    const res = await contestApi.getAudit(contestId)
    const data = (res as any)?.data || res || {}
    auditSummary.value = data.summary || {}
    auditItems.value = Array.isArray(data.items) ? data.items : []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.data?.message || '加载异常排查失败')
  } finally {
    auditLoading.value = false
  }
}

// 打开异常排查页签，提醒教师在线复核，不一定要导出 Excel。
const openAuditPanel = async () => {
  if (!canManageContest.value) {
    ElMessage.warning('只有竞赛创建者或管理员可以查看异常排查')
    return
  }
  activeTab.value = 'audit'
  await loadAudit()
}

// 打开导出前预览，先看排名、参赛人数和题目统计。
const openExportPreview = async () => {
  if (!canExportResults.value) {
    ElMessage.warning('只有竞赛创建者或管理员可以导出成绩')
    return
  }
  exportPreviewVisible.value = true
  exportPreviewLoading.value = true
  try {
    const res = await contestApi.getRanking(contestId, 'all')
    exportPreviewRanking.value = (res.data || res || []) as RankingUser[]
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载成绩预览失败')
  } finally {
    exportPreviewLoading.value = false
  }
}

// 下载后端生成的多工作表竞赛成绩 Excel。
const exportContestResults = async () => {
  if (!canExportResults.value) {
    ElMessage.warning('只有竞赛创建者或管理员可以导出成绩')
    return
  }
  exportingResults.value = true
  try {
    const res = await contestApi.exportResults(contestId)
    const payload = res instanceof Blob ? res : (res as any)?.data || res
    const blob = payload instanceof Blob
      ? payload
      : new Blob([payload], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `contest-${contestId}-results.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('成绩已导出')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '导出失败')
  } finally {
    exportingResults.value = false
  }
}

const handleEditContest = () => {
  if (!contest.value?.id) return
  router.push(`/contests/${contest.value.id}/edit`)
}

const handleDeleteContest = async () => {
  if (!contest.value?.id) return
  try {
    await ElMessageBox.confirm(
      `确定删除竞赛「${contest.value.title || contest.value.id}」吗？删除后列表中将不再展示该竞赛。`,
      '删除竞赛',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await contestApi.deleteContest(contest.value.id)
    ElMessage.success('竞赛已删除')
    router.push('/contests')
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || e?.data?.message || '删除失败')
    }
  }
}

const handleRankingFilterChange = async () => {
  previousRankMap.value = {}
  await loadRanking()
}

// 报名或加入竞赛，私密竞赛会先要求输入竞赛密码。
const handleRegister = async () => {
  try {
    if (contest.value?.password && contest.value.canRegister) {
      const { value } = await ElMessageBox.prompt('请输入竞赛密码', '私密竞赛', { inputType: 'password' })
      await contestApi.register(contestId, value)
    } else if (contest.value?.canRegister) {
      await contestApi.register(contestId)
    }
    await loadContest()
    await loadRanking()
    ElMessage.success(contest.value?.status === 'RUNNING' ? '已加入比赛，祝你发挥顺利！' : '报名成功！')
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || e?.data?.message || '报名失败')
    }
  }
}

// 从竞赛题目跳转到做题 IDE，并通过 query 携带 contestId 进入竞赛提交模式。
const goToProblem = (row: ContestProblem) => {
  if (!canAccessProblems.value) {
    ElMessage.warning(contest.value?.canRegister ? '请先点击上方按钮加入比赛' : '当前账号无权进入该竞赛工作区')
    return
  }
  router.push({ path: `/problems/${row.problemId}`, query: { contestId: String(contestId) } })
}

// 加载某位选手在本场竞赛的提交记录，供排行榜下钻查看。
const loadContestUserSubmissions = async () => {
  if (!selectedRankingUser.value) return
  submissionLoading.value = true
  try {
    submissionPage.value = 1
    const targetUserId = selectedRankingUser.value.userId || selectedRankingUser.value.id
    const res = await submissionApi.getContestUserSubmissions(contestId, targetUserId, 1, 500)
    const data = res.data || res || {}
    submissionRecords.value = data.records || []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.data?.message || '加载选手提交记录失败')
  } finally {
    submissionLoading.value = false
  }
}

// 打开选手提交记录弹窗；普通参赛者只能查看自己的提交。
const openUserSubmissions = async (user: RankingUser) => {
  if (!canViewContestSubmission(user)) {
    ElMessage.warning('普通参赛者只能查看自己的提交记录')
    return
  }
  selectedRankingUser.value = user
  submissionDialogVisible.value = true
  submissionRecords.value = []
  resetSubmissionFilters()
  await loadContestUserSubmissions()
}

const handleSubmissionPageChange = (page: number) => {
  submissionPage.value = page
}

// 查看单次提交详情，包括代码和测试点反馈。
const viewSubmissionDetail = async (submission: SubmissionRecord) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  submissionDetail.value = null
  try {
    const res = await submissionApi.getDetail(submission.id)
    submissionDetail.value = res.data || res
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.data?.message || '加载提交详情失败')
  } finally {
    detailLoading.value = false
  }
}

const viewSubmissionById = async (id: number) => {
  await viewSubmissionDetail({ id, problemId: 0 })
}

// 更新竞赛倒计时和进度条。
const updateCountdown = () => {
  if (!contest.value) return

  const now = Date.now()
  const target = getContestCountdownTarget(contest.value)
  if (!target || !Number.isFinite(target)) {
    countdownText.value = '长期开放'
    progressPercent.value = 0
    return
  }

  const diff = Math.max(0, Math.floor((target - now) / 1000))

  const days = Math.floor(diff / 86400)
  const hours = Math.floor((diff % 86400) / 3600)
  const minutes = Math.floor((diff % 3600) / 60)
  const seconds = diff % 60
  const parts: string[] = []
  if (days > 0) parts.push(`${days}天`)
  if (days > 0 || hours > 0) parts.push(`${hours}小时`)
  if (days > 0 || hours > 0 || minutes > 0) parts.push(`${minutes}分钟`)
  parts.push(`${seconds}秒`)
  countdownText.value = parts.join(' ')

  if (contest.value.status === 'RUNNING') {
    const end = target
    const start = contest.value.participantDeadline && contest.value.durationMinutes
      ? end - contest.value.durationMinutes * 60 * 1000
      : apiDateTimeMs(contest.value.startTime)
    const total = end - start
    const elapsed = now - start
    progressPercent.value = total > 0
      ? Math.min(100, Math.max(0, (elapsed / total) * 100))
      : 0
  }

  if (diff <= 0) {
    if (contest.value.status === 'UPCOMING') {
      contest.value.status = 'RUNNING'
      ElMessage.success('竞赛已开始！')
      loadContest()
      loadRanking()
      startContestTimers()
    } else if (contest.value.status === 'RUNNING') {
      if (contest.value.participantDeadline) {
        contest.value.participantTimeExpired = true
        countdownText.value = '个人考试时长已到'
        loadContest()
      } else {
        contest.value.status = 'ENDED'
        stopContestTimers()
        ElMessage.info('竞赛已结束')
      }
    }
  }
}

const isOpenEndedContest = (c?: ContestDetail | null) => {
  if (!c?.endTime) return !c?.durationMinutes || c.durationMinutes <= 0
  const end = parseApiDate(c.endTime)
  return !end || !Number.isFinite(end.getTime()) || end.getFullYear() >= 2099
}

const getContestCountdownTarget = (c: ContestDetail) => {
  if (c.status === 'UPCOMING') return apiDateTimeMs(c.startTime)
  if (c.status !== 'RUNNING') return null
  if (c.participantDeadline) return apiDateTimeMs(c.participantDeadline)
  if (isOpenEndedContest(c)) return null
  return apiDateTimeMs(c.endTime)
}

const rankRowClass = ({ row }: { row: RankingUser }) => {
  const classes = []
  if (row.rank === 1) classes.push('rank-gold')
  if (row.rank === 2) classes.push('rank-silver')
  if (row.rank === 3) classes.push('rank-bronze')
  if (row.isCurrentUser) classes.push('rank-self')
  return classes.join(' ')
}

const formatDate = (date?: string) => {
  return formatApiDateTime(date)
}

const formatEndDate = (c?: ContestDetail | null) => {
  return isOpenEndedContest(c) ? '长期开放' : formatDate(c?.endTime)
}

const startContestTimers = () => {
  stopContestTimers()
  countdownTimer = setInterval(updateCountdown, 1000)
  updateCountdown()
  if (contest.value?.status === 'RUNNING') {
    rankingTimer = setInterval(() => loadRanking({ silent: true }), 30000)
  }
}

const stopContestTimers = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  if (rankingTimer) {
    clearInterval(rankingTimer)
    rankingTimer = null
  }
}

watch(submissionFilters, () => {
  submissionPage.value = 1
}, { deep: true })

watch(activeTab, tab => {
  if (tab === 'audit' && canManageContest.value && auditItems.value.length === 0) {
    loadAudit()
  }
})

onMounted(async () => {
  await Promise.all([loadContest(), loadRanking()])
  startContestTimers()
})

onActivated(() => {
  if (contest.value) {
    startContestTimers()
  }
})

onDeactivated(() => {
  stopContestTimers()
})

onUnmounted(() => {
  stopContestTimers()
})
</script>

<style scoped>
.contest-detail {
  max-width: 1240px;
  margin: 0 auto;
  padding: 30px 20px;
}

.back-btn {
  margin-bottom: 12px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.title-row h1 {
  font-size: 2rem;
  color: #333;
  margin: 0;
}

.contest-desc {
  color: #666;
  margin-bottom: 16px;
  line-height: 1.6;
}

.contest-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
}

.invite-manage-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  margin: 0 0 18px;
  border: 1px solid #f3d6d6;
  border-radius: 8px;
  background: #fff7f7;
}

.invite-code-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.invite-label,
.invite-tip {
  color: #909399;
  font-size: 13px;
}

.invite-code-info strong {
  color: #f56c6c;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
  font-size: 18px;
}

.timer-bar {
  background: linear-gradient(135deg, #f0f6ff 0%, #f7fbff 100%);
  border-radius: 14px;
  padding: 16px 24px;
  margin-bottom: 20px;
  border: 1px solid #e3eefc;
}

.timer-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.timer-content.running {
  flex-direction: column;
  align-items: flex-start;
}

.timer-label {
  font-size: 15px;
  color: #606266;
}

.timer-value {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
  font-family: monospace;
}

.timer-content.running .timer-value {
  color: #67c23a;
}

.timer-progress {
  width: 100%;
  margin-top: 8px;
}

.register-area {
  margin-bottom: 20px;
}

.management-area {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.detail-tabs {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(15, 35, 95, 0.05);
}

.ranking-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.ranking-toolbar-left,
.ranking-toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-title {
  font-weight: 600;
  color: #303133;
}

.ranking-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(260px, 0.7fr);
  gap: 18px;
  margin-bottom: 18px;
}

.podium-panel,
.avatar-lane-panel {
  border-radius: 16px;
  padding: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #edf2ff;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  color: #303133;
  font-weight: 600;
}

.panel-tip {
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}

.podium-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.podium-card {
  border: none;
  border-radius: 16px;
  padding: 16px 14px;
  text-align: center;
  cursor: pointer;
  background: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 8px 24px rgba(31, 52, 120, 0.08);
}

.podium-card:hover {
  transform: translateY(-2px);
}

.podium-card.disabled {
  cursor: default;
}

.podium-card.disabled:hover {
  transform: none;
}

.podium-rank-1 {
  background: linear-gradient(180deg, #fff6d8 0%, #fffdf4 100%);
}

.podium-rank-2 {
  background: linear-gradient(180deg, #f3f5f8 0%, #fcfdff 100%);
}

.podium-rank-3 {
  background: linear-gradient(180deg, #ffe7de 0%, #fffaf8 100%);
}

.podium-card.is-self {
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.35), 0 10px 24px rgba(245, 108, 108, 0.12);
}

.podium-medal {
  width: 34px;
  height: 34px;
  line-height: 34px;
  border-radius: 50%;
  margin: 0 auto 10px;
  font-weight: 700;
  color: #7a5600;
  background: rgba(255, 255, 255, 0.75);
}

.podium-name-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.podium-name {
  font-weight: 700;
  color: #303133;
}

.podium-meta,
.podium-trend {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
}

.avatar-lane {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow: auto;
  padding-right: 4px;
}

.lane-user {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  border: none;
  border-radius: 14px;
  padding: 10px 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 6px 18px rgba(31, 52, 120, 0.06);
}

.lane-user:hover,
.ranking-user-btn:hover {
  transform: translateX(2px);
}

.lane-user.disabled,
.ranking-user-btn.disabled {
  cursor: default;
}

.lane-user.disabled:hover,
.ranking-user-btn.disabled:hover {
  transform: none;
}

.lane-rank {
  width: 32px;
  font-weight: 700;
  color: #606266;
  flex-shrink: 0;
}

.lane-user-meta {
  min-width: 0;
  flex: 1;
}

.lane-name-row,
.ranking-user-name-row,
.submission-user-title {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.lane-name,
.ranking-user-name {
  font-weight: 600;
  color: #303133;
}

.lane-name {
  max-width: 110px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lane-subline,
.ranking-user-sub,
.submission-user-subtitle {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.problem-progress-line {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.lane-self-badge {
  font-size: 11px;
  color: #f56c6c;
  font-weight: 700;
}

.lane-arrow {
  color: #f56c6c;
  font-size: 16px;
  font-weight: 700;
}

.lane-user.is-self {
  background: linear-gradient(90deg, #fff2f2 0%, #ffffff 100%);
  box-shadow: 0 0 0 1px rgba(245, 108, 108, 0.28), 0 8px 22px rgba(245, 108, 108, 0.12);
}

.lane-rank-1 {
  background: linear-gradient(90deg, #fff7dc 0%, #ffffff 100%);
}

.lane-rank-2 {
  background: linear-gradient(90deg, #f3f5f8 0%, #ffffff 100%);
}

.lane-rank-3 {
  background: linear-gradient(90deg, #ffede5 0%, #ffffff 100%);
}

.lane-ellipsis {
  display: flex;
  justify-content: center;
  gap: 5px;
  padding: 6px 0;
}

.lane-ellipsis span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c0c4cc;
}

.rank-number {
  font-weight: 700;
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

.ranking-user-meta {
  min-width: 0;
}

.ac-cell { color: #67c23a; font-weight: 600; font-size: 13px; }
.wrong-count { color: #f56c6c; font-size: 11px; }
.wa-cell { color: #f56c6c; font-weight: 600; }
.untried-cell { color: #c0c4cc; }

.auto-hint {
  font-size: 12px;
  color: #c0c4cc;
}

.table-muted {
  color: #c0c4cc;
}

.problem-pagination {
  margin-top: 16px;
  justify-content: center;
}

.difficulty-blue {
  background-color: #e6f0fa !important;
  color: #1976d2 !important;
  border-color: #b3d8fd !important;
}

.submission-dialog-header {
  margin-bottom: 16px;
}

.submission-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid #edf2ff;
}

.submission-filter-bar {
  display: grid;
  grid-template-columns: minmax(180px, 1.2fr) minmax(120px, 0.7fr) minmax(130px, 0.7fr) auto auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.submission-filter-count {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}

.submission-record-table {
  width: 100%;
}

.submission-detail {
  min-height: 120px;
}

.audit-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.audit-panel {
  min-height: 260px;
}

.audit-alert {
  margin-bottom: 16px;
}

.audit-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.audit-summary-card {
  border: 1px solid #e8edf5;
  border-radius: 8px;
  padding: 14px 16px;
  background: #fbfcfe;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.audit-summary-card.high {
  border-color: #f7d7d7;
  background: #fff7f7;
}

.audit-summary-card.medium {
  border-color: #f4dfb7;
  background: #fffaf0;
}

.audit-summary-card.risk {
  border-color: #dce6f7;
  background: #f6f9ff;
}

.audit-summary-card span {
  color: #909399;
  font-size: 13px;
}

.audit-summary-card strong {
  color: #303133;
  font-size: 24px;
}

.audit-toolbar {
  display: grid;
  grid-template-columns: minmax(120px, 0.5fr) minmax(150px, 0.7fr) minmax(220px, 1.2fr) auto auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.audit-submission-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.audit-factor-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.audit-list {
  display: grid;
  gap: 12px;
}

.audit-card {
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.04);
}

.audit-card.severity-high {
  border-left: 4px solid #f56c6c;
}

.audit-card.severity-medium {
  border-left: 4px solid #e6a23c;
}

.audit-card.severity-low {
  border-left: 4px solid #409eff;
}

.audit-card.severity-info {
  border-left: 4px solid #67c23a;
}

.audit-card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.audit-card-head time {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
  line-height: 26px;
}

.audit-card-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.audit-card-title strong {
  color: #303133;
  font-size: 15px;
}

.audit-risk-pill {
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 12px;
  font-weight: 700;
  background: #f5f7fa;
}

.audit-risk-pill.high {
  color: #f56c6c;
  background: #fff0f0;
}

.audit-risk-pill.medium {
  color: #e6a23c;
  background: #fff7e8;
}

.audit-risk-pill.low {
  color: #409eff;
  background: #eef6ff;
}

.audit-risk-pill.info {
  color: #909399;
  background: #f5f7fa;
}

.audit-card-meta {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.audit-card-meta span {
  min-width: 0;
  border: 1px solid #edf0f5;
  border-radius: 6px;
  background: #fafbfc;
  padding: 8px 10px;
  color: #303133;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.audit-card-meta label {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.audit-card-factors {
  margin-bottom: 10px;
}

.audit-card-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.audit-card-body section {
  min-width: 0;
  border: 1px solid #edf0f5;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fcfdff;
}

.audit-card-body section.wide {
  grid-column: 1 / -1;
}

.audit-card-body span {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 6px;
}

.audit-card-body p {
  margin: 0;
  color: #4f5b6b;
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.audit-card-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.export-preview {
  min-height: 220px;
}

.preview-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.preview-summary-card {
  border: 1px solid #e6eef8;
  border-radius: 8px;
  padding: 14px 16px;
  background: #f8fbff;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.preview-summary-card span {
  color: #909399;
  font-size: 13px;
}

.preview-summary-card strong {
  color: #303133;
  font-size: 24px;
}

.preview-section {
  margin-top: 18px;
}

.preview-hint {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.detail-meta-grid > div {
  background: #f8fbff;
  border: 1px solid #edf2ff;
  border-radius: 12px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-meta-grid span {
  font-size: 12px;
  color: #909399;
}

.detail-meta-grid strong {
  color: #303133;
  font-size: 14px;
}

.detail-section {
  margin-top: 18px;
}

.section-title {
  font-weight: 700;
  color: #303133;
  margin-bottom: 10px;
}

.detail-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 12px;
  padding: 14px;
  font-size: 13px;
  line-height: 1.6;
  max-height: 320px;
  overflow: auto;
}

.detail-pre.code {
  background: #0f172a;
  color: #e2e8f0;
}

.detail-pre.error {
  background: #fff4f4;
  color: #c45656;
}

.movement-up {
  color: #67c23a;
  font-weight: 700;
}

.movement-down {
  color: #f56c6c;
  font-weight: 700;
}

.movement-flat {
  color: #c0c4cc;
}

:deep(.rank-gold td) { background-color: #fff8e6 !important; }
:deep(.rank-silver td) { background-color: #f5f5f5 !important; }
:deep(.rank-bronze td) { background-color: #fef3f0 !important; }
:deep(.rank-self td) {
  box-shadow: inset 3px 0 0 #f56c6c;
}

@media (max-width: 960px) {
  .ranking-stage {
    grid-template-columns: 1fr;
  }

  .podium-list {
    grid-template-columns: 1fr;
  }

  .submission-filter-bar,
  .audit-summary-grid,
  .audit-card-meta,
  .audit-card-body,
  .audit-toolbar,
  .preview-summary-grid {
    grid-template-columns: 1fr;
  }

  .audit-card-head {
    flex-direction: column;
    gap: 8px;
  }

  .audit-card-head time {
    white-space: normal;
  }

  .audit-card-body section.wide {
    grid-column: auto;
  }
}
</style>
