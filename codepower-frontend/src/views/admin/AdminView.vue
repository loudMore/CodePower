<!-- 管理后台 — 数据概览、用户管理、题目管理、审核 -->
<template>
  <div class="admin-container">
    <h1>管理后台</h1>

    <div class="dashboard-cards" v-loading="dashboardLoading">
      <el-card v-for="item in dashboardItems" :key="item.label" shadow="hover" class="stat-card">
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </el-card>
    </div>

    <!-- 管理后台按业务域拆成多个页签：用户、题目、反馈、角色审核、AI 模型、评测集群和统计。 -->
    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="用户管理" name="users">
        <div class="tab-toolbar">
          <el-input v-model="userKeyword" placeholder="搜索用户名或邮箱..." clearable style="width: 250px" @input="onUserFilterChange" />
          <el-select v-model="roleFilter" placeholder="角色筛选" clearable @change="onUserFilterChange" style="width: 150px; margin-left: 12px">
            <el-option label="普通用户" value="NORMAL_USER" />
            <el-option label="高级用户" value="SENIOR_USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </div>
        <el-table :data="users" v-loading="usersLoading" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="150" />
          <el-table-column prop="email" label="邮箱" min-width="220" />
          <el-table-column prop="role" label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'SENIOR_USER' ? 'warning' : 'info'">
                {{ roleLabel(row.role) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'danger'">
                {{ userStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="等级" width="90" />
          <el-table-column prop="exp" label="经验" width="100" />
          <el-table-column label="操作" width="360">
            <template #default="{ row }">
              <el-select :model-value="row.role" @change="(v: string) => changeRole(row, v)" style="width: 130px" size="small">
                <el-option label="普通用户" value="NORMAL_USER" />
                <el-option label="高级用户" value="SENIOR_USER" />
                <el-option label="管理员" value="ADMIN" />
              </el-select>
              <el-button text :type="row.status === 1 ? 'danger' : 'success'" @click="toggleUserStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleUserAction(row, cmd)">
                <el-button text type="primary">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="resetPassword">重置密码</el-dropdown-item>
                    <el-dropdown-item command="addExp">调整经验</el-dropdown-item>
                    <el-dropdown-item command="addAiPoints">调整AI积分</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination background layout="prev, pager, next" :total="userTotal" :page-size="adminPageSize" :current-page="userPage"
          @current-change="p => { userPage = p; searchUsers() }" style="margin-top: 16px; justify-content: center; display: flex" />
      </el-tab-pane>

      <el-tab-pane label="题目管理" name="problems">
        <div class="tab-toolbar">
          <el-input v-model="problemKeyword" placeholder="搜索题目..." clearable style="width: 250px" @input="onProblemFilterChange" />
          <el-select v-model="problemStatusFilter" placeholder="状态筛选" clearable @change="onProblemFilterChange" style="width: 150px; margin-left: 12px">
            <el-option label="已下线" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </div>
        <el-table :data="problems" v-loading="problemsLoading" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="题目" min-width="260" />
          <el-table-column prop="difficulty" label="难度" width="100">
            <template #default="{ row }">
              <el-tag :type="difficultyType(row.difficulty)">{{ row.difficulty || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="visibility" label="可见性" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ problemStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleProblemStatus(row)">
                {{ row.status === 1 ? '下线' : '发布' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination background layout="prev, pager, next" :total="problemTotal" :page-size="adminPageSize" :current-page="problemPage"
          @current-change="p => { problemPage = p; searchProblems() }" style="margin-top: 16px; justify-content: center; display: flex" />
      </el-tab-pane>

      <el-tab-pane :label="`题目反馈处理${pendingReportCount ? ` (${pendingReportCount})` : ''}`" name="reports">
        <div class="tab-toolbar">
          <el-input v-model="reportKeyword" placeholder="搜索题目或反馈内容..." clearable style="width: 250px" @input="onReportFilterChange" />
          <el-select v-model="reportStatusFilter" placeholder="状态筛选" clearable @change="onReportFilterChange" style="width: 160px; margin-left: 12px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="RESOLVED" />
            <el-option label="已忽略" value="DISMISSED" />
          </el-select>
        </div>
        <el-table :data="problemReports" v-loading="reportsLoading" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="problemTitle" label="题目" min-width="220" show-overflow-tooltip />
          <el-table-column prop="reporterName" label="反馈人" width="140" show-overflow-tooltip />
          <el-table-column prop="reportType" label="类型" width="130">
            <template #default="{ row }">
              <el-tag :type="reportTypeType(row.reportType)">{{ reportTypeLabel(row.reportType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="反馈内容" min-width="260" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="reportStatusType(row.status)">{{ reportStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reply" label="处理说明" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.reply || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button type="success" text @click="handleProblemReport(row, 'RESOLVED')">处理</el-button>
              <el-button type="danger" text @click="handleProblemReport(row, 'DISMISSED')">忽略</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination background layout="prev, pager, next" :total="reportTotal" :page-size="adminPageSize" :current-page="reportPage"
          @current-change="p => { reportPage = p; loadProblemReports() }" style="margin-top: 16px; justify-content: center; display: flex" />
      </el-tab-pane>

      <el-tab-pane :label="`角色审核${pendingUpgradeCount ? ` (${pendingUpgradeCount})` : ''}`" name="upgrades">
        <div class="tab-toolbar">
          <el-select v-model="upgradeStatusFilter" placeholder="状态筛选" clearable @change="loadUpgradeRequests" style="width: 150px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </div>
        <el-table :data="upgradeRequests" v-loading="upgradesLoading" stripe>
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="email" label="邮箱" width="200" />
          <el-table-column prop="currentRole" label="当前角色" width="100">
            <template #default="{ row }">
              <el-tag type="info">{{ roleLabel(row.currentRole) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="requestedRole" label="申请角色" width="100">
            <template #default="{ row }">
              <el-tag type="warning">{{ roleLabel(row.requestedRole) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'">
                {{ row.status === 'PENDING' ? '待审核' : row.status === 'APPROVED' ? '已通过' : '已拒绝' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" v-if="upgradeStatusFilter !== 'APPROVED' && upgradeStatusFilter !== 'REJECTED'">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button type="success" text @click="reviewRequest(row, 'approve')">通过</el-button>
                <el-button type="danger" text @click="reviewRequest(row, 'reject')">拒绝</el-button>
              </template>
              <span v-else class="text-muted">{{ row.reviewComment || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="AI模型" name="aiModels">
        <!-- AI 模型配置：管理员维护模型提供商、远程模型列表和用户可选模型。 -->
        <div class="ai-model-admin" v-loading="aiProvidersLoading">
          <aside class="provider-panel">
            <div class="provider-panel-header">
              <div>
                <h3>模型提供商</h3>
                <span>{{ aiProviders.length }} 个源</span>
              </div>
              <el-button type="primary" plain @click="addAiProvider">新增</el-button>
            </div>
            <button
              v-for="provider in aiProviders"
              :key="provider.id"
              class="provider-item"
              :class="{ active: provider.id === activeAiProviderId }"
              @click="selectAiProvider(provider)"
            >
              <div class="provider-logo">{{ provider.name?.slice(0, 1) || 'A' }}</div>
              <div class="provider-info">
                <strong>{{ provider.name }}</strong>
                <span>{{ provider.baseUrl }}</span>
              </div>
              <el-tag size="small" :type="provider.enabled ? 'success' : 'info'">
                {{ provider.enabled ? '启用' : '停用' }}
              </el-tag>
            </button>
          </aside>

          <section v-if="activeAiProvider" class="provider-detail">
            <div class="provider-detail-header">
              <div>
                <h3>{{ activeAiProvider.name || '新增提供商' }}</h3>
                <p>{{ activeAiProvider.baseUrl || '配置 OpenAI 兼容、SiliconFlow、Anthropic 或自定义中转站' }}</p>
              </div>
              <div class="provider-header-actions">
                <el-button type="danger" plain :disabled="!activeAiProvider.id" @click="deleteAiProvider">删除源</el-button>
                <el-button type="success" @click="saveAiProvider">保存配置</el-button>
              </div>
            </div>

            <el-form label-position="top" class="provider-form">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="提供商ID">
                    <el-input v-model="aiProviderForm.providerKey" :disabled="!!activeAiProvider.id" placeholder="siliconflow" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="显示名称">
                    <el-input v-model="aiProviderForm.name" placeholder="SiliconFlow" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="协议类型">
                    <el-select v-model="aiProviderForm.apiType" style="width: 100%">
                      <el-option label="OpenAI兼容" value="OPENAI_COMPATIBLE" />
                      <el-option label="Anthropic模型列表" value="ANTHROPIC" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="16">
                  <el-form-item label="API Base URL">
                    <el-input v-model="aiProviderForm.baseUrl" placeholder="https://api.siliconflow.cn/v1" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="状态">
                    <el-switch v-model="aiProviderForm.enabled" active-text="启用" inactive-text="停用" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item :label="`API Key${activeAiProvider.hasApiKey ? `（已配置 ${activeAiProvider.apiKeyMasked}，留空不修改）` : ''}`">
                <el-input v-model="aiProviderForm.apiKey" type="password" show-password placeholder="留空表示不修改现有密钥" />
              </el-form-item>
            </el-form>

            <div class="model-toolbar">
              <div class="model-toolbar-title">
                <h3>已配置模型</h3>
                <span>启用后的模型会出现在 AI 求助和 AI 出题下拉框</span>
              </div>
              <el-input v-model="aiModelKeyword" clearable placeholder="搜索模型或ID" style="width: 260px" />
              <el-button @click="fetchRemoteAiModels">获取模型列表</el-button>
              <el-button type="primary" plain @click="openCustomModelDialog()">自定义模型</el-button>
            </div>

            <div class="model-list-card">
              <div v-if="filteredAiModels.length === 0" class="model-empty">暂无模型，点击“获取模型列表”或“自定义模型”添加。</div>
              <div v-for="model in filteredAiModels" :key="model.id" class="model-row">
                <div class="model-main">
                  <strong>{{ model.displayName }}</strong>
                  <span>{{ model.modelId }}</span>
                </div>
                <el-input-number v-model="model.costMultiplier" :min="0" :max="20" :step="0.1" size="small" @change="() => saveAiModel(model)" />
                <el-input v-model="model.capabilityTags" placeholder="能力标签" size="small" style="width: 180px" @change="() => saveAiModel(model)" />
                <el-switch v-model="model.enabledBool" @change="() => toggleAiModel(model)" />
                <el-button text type="primary" @click="openCustomModelDialog(model)">配置</el-button>
                <el-button text type="danger" @click="deleteAiModel(model)">删除</el-button>
              </div>
            </div>
          </section>

          <section v-else class="provider-empty">
            <div class="empty-cursor">⌁</div>
            <p>请选择或新增一个模型提供商</p>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="评测集群" name="judgeCluster">
        <!-- 评测集群监控：每秒刷新 Judge0 节点健康、队列、并发和最近提交。 -->
        <div class="judge-cluster" v-loading="judgeClusterLoading && !judgeCluster.checkedAt">
          <div class="cluster-toolbar">
            <div>
              <h3>Judge0 评测集群</h3>
              <span>CodePower 统一排队，Judge0 节点只负责执行，结果回写主库</span>
            </div>
            <div class="cluster-actions">
              <el-tag size="small" :type="judgeClusterLoading ? 'warning' : 'success'">
                {{ judgeClusterLoading ? '刷新中' : '1秒自动刷新' }}
              </el-tag>
                            <el-button
                type="danger"
                plain
                :loading="judgeClusterDemoLoading"
                :disabled="judgeClusterDemoLoading"
                @click="runJudgeClusterDemo"
              >
                <el-icon><VideoPlay /></el-icon>
                一键压测 {{ judgeClusterDemoCount }} 条
              </el-button>
              <el-button type="primary" plain @click="loadJudgeCluster(true)">刷新</el-button>
            </div>
          </div>

          <div class="cluster-summary">
            <el-card v-for="item in judgeClusterItems" :key="item.label" shadow="never" class="cluster-summary-card">
              <div class="cluster-summary-label">{{ item.label }}</div>
              <div class="cluster-summary-value">{{ item.value }}</div>
            </el-card>
          </div>

          <div class="cluster-panels">
            <section class="cluster-panel">
              <div class="cluster-panel-head">
                <strong>全局提交队列</strong>
                <span>先入队，按队列顺序分配；完成顺序允许由节点耗时决定</span>
              </div>
              <div class="queue-meter">
                <el-progress
                  :percentage="queueUsagePercent"
                  :stroke-width="12"
                  :show-text="false"
                  :status="queueUsagePercent >= 90 ? 'exception' : queueUsagePercent >= 70 ? 'warning' : 'success'"
                />
                <span>{{ queueSize }} / {{ queueCapacity }} 等待</span>
              </div>
              <div class="queue-grid">
                <div>
                  <span>工作线程</span>
                  <strong>{{ executor.workerCount || 0 }}</strong>
                </div>
                <div>
                  <span>正在执行</span>
                  <strong>{{ executor.activeCount || 0 }}</strong>
                </div>
                <div>
                  <span>近1分钟完成</span>
                  <strong>{{ executor.completedLastMinute || 0 }}</strong>
                </div>
                <div>
                  <span>平均等待</span>
                  <strong>{{ formatMs(executor.avgQueueWaitMs) }}</strong>
                </div>
                <div>
                  <span>平均执行</span>
                  <strong>{{ formatMs(executor.avgRunMs) }}</strong>
                </div>
                <div>
                  <span>拒绝任务</span>
                  <strong>{{ executor.rejectedTasks || 0 }}</strong>
                </div>
              </div>
            </section>

            <section class="cluster-panel">
              <div class="cluster-panel-head">
                <strong>节点负载</strong>
                <span>按健康状态、权重和并发空闲度选择节点</span>
              </div>
              <div class="node-cards">
                <article v-for="node in judgeClusterNodes" :key="node.name" class="node-card">
                  <div class="node-card-head">
                    <div>
                      <strong>{{ node.name }}</strong>
                      <span>{{ node.systemInfo?.cpu || '-' }} 核 · {{ node.systemInfo?.memory || '-' }}</span>
                    </div>
                    <el-tag size="small" :type="judgeNodeTagType(node)">
                      {{ judgeNodeStatusLabel(node) }}
                    </el-tag>
                  </div>
                  <div class="node-usage">
                    <el-progress
                      :percentage="nodeUsagePercent(node)"
                      :stroke-width="10"
                      :show-text="false"
                      :status="nodeUsagePercent(node) >= 90 ? 'exception' : nodeUsagePercent(node) >= 70 ? 'warning' : 'success'"
                    />
                    <span>{{ node.inFlight || 0 }} / {{ node.maxConcurrent || 0 }}</span>
                  </div>
                  <div class="node-meta">
                    <span>权重 {{ node.weight || 0 }}</span>
                    <span>Workers {{ nodeWorkersLabel(node) }}</span>
                    <span>平均 {{ formatMs(node.avgLatencyMs) }}</span>
                    <span>最近 {{ formatMs(node.lastLatencyMs) }}</span>
                  </div>
                  <div v-if="node.lastError || node.lastHealthError" class="node-error">
                    {{ node.lastError || node.lastHealthError }}
                  </div>
                </article>
              </div>
            </section>
          </div>

          <div class="cluster-table-grid">
            <section class="cluster-panel">
              <div class="cluster-panel-head">
                <strong>正在执行</strong>
                <span>{{ runningTasks.length }} 个任务</span>
              </div>
              <el-table :data="runningTasks" size="small" empty-text="暂无运行任务">
                                <el-table-column label="提交ID" width="96">
                  <template #default="{ row }">{{ formatTaskSubmissionId(row.submissionId) }}</template>
                </el-table-column>
                <el-table-column label="等待" width="100">
                  <template #default="{ row }">{{ formatMs(row.queueWaitMs) }}</template>
                </el-table-column>
                <el-table-column label="开始时间" min-width="160">
                  <template #default="{ row }">{{ formatTime(row.startedAt) || '-' }}</template>
                </el-table-column>
              </el-table>
            </section>

            <section class="cluster-panel">
              <div class="cluster-panel-head">
                <strong>最近提交</strong>
                <span>主库结果，刷新后即同步</span>
              </div>
              <el-table :data="recentSubmissions" size="small" empty-text="暂无提交">
                <el-table-column prop="id" label="ID" width="78" />
                <el-table-column label="来源" width="78">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.contestId ? 'warning' : 'info'">
                      {{ row.contestId ? '竞赛' : '题库' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="problemId" label="题目" width="78" />
                <el-table-column prop="language" label="语言" width="86" show-overflow-tooltip />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">
                    <el-tag size="small" :type="submissionStatusType(row.status)">
                      {{ submissionStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="节点" min-width="130" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.judgeNode || '-' }}</template>
                </el-table-column>
                <el-table-column label="等待/执行" width="130">
                  <template #default="{ row }">
                    {{ formatMs(row.queueWaitMs) }} / {{ formatMs(row.judgeDurationMs) }}
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </div>

          <div class="cluster-footnote">
            上次检查：{{ formatTime(judgeCluster.checkedAt) || '-' }}
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="系统统计" name="stats">
        <div class="overview-grid" v-loading="overviewLoading">
          <el-card v-for="item in overviewItems" :key="item.label" shadow="never" class="overview-card">
            <div class="overview-label">{{ item.label }}</div>
            <div class="overview-value">{{ item.value }}</div>
          </el-card>
        </div>
        <div ref="chartRef" class="chart-box" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="remoteModelDialogVisible" title="从远程列表选择模型" width="760px">
      <div class="remote-model-summary">
        <span>远程返回 {{ remoteModels.length }} 个模型</span>
        <span v-if="remoteModelAddedCount > 0">已隐藏 {{ remoteModelAddedCount }} 个已添加模型</span>
      </div>
      <el-table :data="availableRemoteModels" max-height="440">
        <el-table-column prop="modelId" label="模型ID" min-width="320" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" text @click="importRemoteModel(row)">添加</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有可添加的新模型，当前远程模型都已配置" :image-size="70" />
        </template>
      </el-table>
    </el-dialog>

    <el-dialog v-model="customModelDialogVisible" title="模型配置" width="560px">
      <el-form label-position="top">
        <el-form-item label="模型ID">
          <el-input v-model="customModelForm.modelId" placeholder="deepseek-ai/DeepSeek-V3" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="customModelForm.displayName" placeholder="DeepSeek V3" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="积分倍率">
              <el-input-number v-model="customModelForm.costMultiplier" :min="0" :max="20" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上下文窗口">
              <el-input-number v-model="customModelForm.contextWindow" :min="0" :step="1024" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="能力标签">
          <el-input v-model="customModelForm.capabilityTags" placeholder="chat,code,reasoning" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="customModelForm.enabledBool" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customModelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomModel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

// 管理后台页面：负责用户、题目、反馈、升级申请、AI模型配置和评测集群监控。

const activeTab = ref('users')
const adminPageSize = 20
const dashboardLoading = ref(false)
const dashboardItems = ref([
  { label: '正常用户', value: 0 },
  { label: '已发布题目', value: 0 },
  { label: '今日提交', value: 0 },
  { label: '待处理反馈', value: 0 }
])

const usersLoading = ref(false)
const users = ref<any[]>([])
const userKeyword = ref('')
const roleFilter = ref('')
const userPage = ref(1)
const userTotal = ref(0)

const problemsLoading = ref(false)
const problems = ref<any[]>([])
const problemKeyword = ref('')
const problemStatusFilter = ref<number | undefined>()
const problemPage = ref(1)
const problemTotal = ref(0)

const reportsLoading = ref(false)
const problemReports = ref<any[]>([])
const reportKeyword = ref('')
const reportStatusFilter = ref('PENDING')
const reportPage = ref(1)
const reportTotal = ref(0)
const pendingReportCount = ref(0)

const upgradesLoading = ref(false)
const upgradeRequests = ref<any[]>([])
const upgradeStatusFilter = ref('')
const pendingUpgradeCount = ref(0)

const overviewLoading = ref(false)
const overviewItems = ref([
  { label: '用户总数', value: 0 },
  { label: '禁用用户', value: 0 },
  { label: '总提交数', value: 0 },
  { label: '通过率', value: '0%' },
  { label: '近7天活跃用户', value: 0 },
  { label: '待处理反馈', value: 0 }
])

const judgeClusterLoading = ref(false)
const judgeCluster = ref<any>({})
const judgeClusterDemoLoading = ref(false)
const judgeClusterDemoCount = 36
let judgeClusterTimer: number | null = null
let judgeClusterInFlight = false
const judgeClusterNodes = computed(() => judgeCluster.value?.nodes || [])
const executor = computed(() => judgeCluster.value?.executor || {})
const runningTasks = computed(() => executor.value?.runningTasks || [])
const recentSubmissions = computed(() => judgeCluster.value?.recentSubmissions || [])
const queueSize = computed(() => Number(executor.value?.queueSize || 0))
const queueCapacity = computed(() => queueSize.value + Number(executor.value?.queueRemainingCapacity || 0))
const queueUsagePercent = computed(() => {
  if (queueCapacity.value <= 0) return 0
  return Math.min(100, Math.round(queueSize.value * 100 / queueCapacity.value))
})
const judgeClusterItems = computed(() => [
  { label: '健康节点', value: `${judgeCluster.value?.healthyNodes || 0}/${judgeCluster.value?.totalNodes || 0}` },
  { label: '节点并发', value: `${judgeCluster.value?.totalInFlight || 0}/${judgeCluster.value?.totalCapacity || 0}` },
  { label: '队列等待', value: queueSize.value },
  { label: '主库待评测', value: judgeCluster.value?.pendingSubmissions ?? 0 },
  { label: '主库运行中', value: judgeCluster.value?.runningSubmissions ?? 0 },
  { label: '今日提交', value: judgeCluster.value?.todaySubmissions ?? 0 },
  { label: '今日通过', value: judgeCluster.value?.todayAcceptedSubmissions ?? 0 }
])

const chartRef = ref<HTMLElement>()
let chart: any = null
let chartLib: any = null
let resizeHandler: (() => void) | null = null

const aiProvidersLoading = ref(false)
const aiProviders = ref<any[]>([])
const activeAiProviderId = ref<number | null>(null)
const aiProviderForm = reactive({
  providerKey: '',
  name: '',
  apiType: 'OPENAI_COMPATIBLE',
  baseUrl: '',
  apiKey: '',
  enabled: true
})
const aiModelKeyword = ref('')
const remoteModelDialogVisible = ref(false)
const remoteModels = ref<any[]>([])
const customModelDialogVisible = ref(false)
const customModelForm = reactive<any>({
  id: null,
  modelId: '',
  displayName: '',
  costMultiplier: 1,
  contextWindow: 32768,
  capabilityTags: 'chat,code',
  enabledBool: true
})

const normalizeModelId = (modelId?: string) => (modelId || '').trim().toLowerCase()
const activeAiProvider = computed(() => aiProviders.value.find(item => item.id === activeAiProviderId.value))
const filteredAiModels = computed(() => {
  const keyword = aiModelKeyword.value.trim().toLowerCase()
  const models = activeAiProvider.value?.models || []
  return models.filter((model: any) => !keyword
    || model.modelId?.toLowerCase().includes(keyword)
    || model.displayName?.toLowerCase().includes(keyword))
})
const addedRemoteModelIds = computed(() => new Set(
  (activeAiProvider.value?.models || []).map((model: any) => normalizeModelId(model.modelId))
))
const availableRemoteModels = computed(() => remoteModels.value.filter((model: any) =>
  !addedRemoteModelIds.value.has(normalizeModelId(model.modelId))
))
const remoteModelAddedCount = computed(() => Math.max(0, remoteModels.value.length - availableRemoteModels.value.length))

onMounted(async () => {
  await Promise.all([loadDashboard(), searchUsers()])
})

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  if (judgeClusterTimer) {
    window.clearInterval(judgeClusterTimer)
  }
  if (chart) {
    chart.dispose()
  }
})

const currentAdminId = () => {
  try {
    const raw = localStorage.getItem('userInfo')
    if (!raw) return undefined
    const user = JSON.parse(raw)
    return typeof user.id === 'number' ? user.id : undefined
  } catch {
    return undefined
  }
}

// 加载后台首页概览数据。
const loadDashboard = async () => {
  dashboardLoading.value = true
  try {
    const res: any = await adminApi.getDashboard()
    const d = res.data || res
    pendingReportCount.value = d.pendingProblemReports ?? d.pendingProblemReviews ?? 0
    pendingUpgradeCount.value = d.pendingUpgradeRequests || 0
    dashboardItems.value = [
      { label: '正常用户', value: d.totalUsers || 0 },
      { label: '已发布题目', value: d.totalProblems || 0 },
      { label: '今日提交', value: d.todaySubmissions || 0 },
      { label: '待处理反馈', value: d.pendingProblemReports ?? d.pendingProblemReviews ?? 0 }
    ]
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载管理看板失败')
  } finally {
    dashboardLoading.value = false
  }
}

// 加载平台统计概览：用户、提交、通过率、活跃用户和待处理事项。
const loadOverview = async () => {
  overviewLoading.value = true
  try {
    const res: any = await adminApi.getStatsOverview()
    const d = res.data || res
    overviewItems.value = [
      { label: '用户总数', value: d.totalUsers || 0 },
      { label: '禁用用户', value: d.disabledUsers || 0 },
      { label: '总提交数', value: d.totalSubmissions || 0 },
      { label: '通过率', value: `${d.acceptanceRate || 0}%` },
      { label: '近7天活跃用户', value: d.weeklyActiveUsers || 0 },
      { label: '待处理反馈', value: d.pendingProblemReports ?? d.pendingProblemReviews ?? 0 }
    ]
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载系统统计失败')
  } finally {
    overviewLoading.value = false
  }
}

// 加载评测集群实时状态：节点健康、并发占用、队列、最近提交。
const loadJudgeCluster = async (manual = false) => {
  if (judgeClusterInFlight) return
  judgeClusterInFlight = true
  if (manual || !judgeCluster.value?.checkedAt) {
    judgeClusterLoading.value = true
  }
  try {
    const res: any = await adminApi.getJudgeCluster()
    judgeCluster.value = res.data || res || {}
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载评测集群状态失败')
  } finally {
    judgeClusterInFlight = false
    judgeClusterLoading.value = false
  }
}

// 切到评测集群页签时每秒刷新，离开页签时停止定时器。
const runJudgeClusterDemo = async () => {
  if (judgeClusterDemoLoading.value) return
  judgeClusterDemoLoading.value = true
  try {
    const res: any = await adminApi.triggerJudgeClusterDemo({ count: judgeClusterDemoCount, language: 'cpp' })
    const d = res.data || res
    ElMessage.success(`已注入 ${d.acceptedCount || judgeClusterDemoCount} 条演示评测任务`)
    await loadJudgeCluster(true)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '演示压测注入失败')
  } finally {
    judgeClusterDemoLoading.value = false
  }
}

const resetJudgeClusterTimer = () => {
  if (judgeClusterTimer) {
    window.clearInterval(judgeClusterTimer)
    judgeClusterTimer = null
  }
  if (activeTab.value === 'judgeCluster') {
    judgeClusterTimer = window.setInterval(() => {
      loadJudgeCluster()
    }, 1000)
  }
}

// 计算单个 Judge0 节点的并发占用百分比。
const nodeUsagePercent = (node: any) => {
  const max = Number(node.maxConcurrent || 0)
  if (max <= 0) return 0
  return Math.min(100, Math.round(Number(node.inFlight || 0) * 100 / max))
}

const nodeWorkersLabel = (node: any) => {
  const workers = Number(node.workers || 0)
  if (Number.isFinite(workers) && workers > 0) return String(workers)
  const max = Number(node.maxConcurrent || 0)
  return Number.isFinite(max) && max > 0 ? `${max}（按并发配额）` : '0'
}

const judgeNodeTagType = (node: any) => {
  if (!node.enabled) return 'info'
  if (node.status === 'CIRCUIT_OPEN') return 'warning'
  return node.healthy ? 'success' : 'danger'
}

const judgeNodeStatusLabel = (node: any) => {
  if (!node.enabled) return '停用'
  if (node.status === 'CIRCUIT_OPEN') return '熔断恢复中'
  return node.healthy ? '在线' : '异常'
}

const formatMs = (value: any) => {
  const ms = Number(value || 0)
  if (!Number.isFinite(ms) || ms <= 0) return '0ms'
  if (ms < 1000) return `${Math.round(ms)}ms`
  return `${(ms / 1000).toFixed(ms < 10000 ? 1 : 0)}s`
}

const formatTaskSubmissionId = (value: any) => {
  const id = Number(value)
  if (!Number.isFinite(id) || id === 0) return '-'
  return id < 0 ? `DEMO-${Math.abs(id)}` : String(id)
}

const submissionStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '等待',
    RUNNING: '评测中',
    ACCEPTED: '通过',
    WRONG_ANSWER: '答案错误',
    TIME_LIMIT_EXCEEDED: '超时',
    MEMORY_LIMIT_EXCEEDED: '内存超限',
    COMPILATION_ERROR: '编译错误',
    RUNTIME_ERROR: '运行错误'
  }
  return map[status] || status || '-'
}

const submissionStatusType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    RUNNING: 'primary',
    ACCEPTED: 'success',
    WRONG_ANSWER: 'danger',
    TIME_LIMIT_EXCEEDED: 'warning',
    MEMORY_LIMIT_EXCEEDED: 'warning',
    COMPILATION_ERROR: 'danger',
    RUNTIME_ERROR: 'danger'
  }
  return map[status] || 'info'
}

// 分页搜索用户，支持关键词和角色筛选。
const searchUsers = async () => {
  usersLoading.value = true
  try {
    const res: any = await adminApi.listUsers({
      page: userPage.value,
      size: adminPageSize,
      keyword: userKeyword.value || undefined,
      role: roleFilter.value || undefined
    })
    const d = res.data || res
    users.value = d.records || []
    userTotal.value = d.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载用户列表失败')
  } finally {
    usersLoading.value = false
  }
}

const onUserFilterChange = () => {
  userPage.value = 1
  searchUsers()
}

const changeRole = async (user: any, newRole: string) => {
  if (newRole === user.role) return
  try {
    await ElMessageBox.confirm(`确定将 ${user.username} 的角色改为 ${roleLabel(newRole)}？`, '确认')
    await adminApi.updateUserRole(user.id, newRole)
    user.role = newRole
    ElMessage.success('角色已更新')
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '角色更新失败')
      await searchUsers()
    }
  }
}

const roleLabel = (role: string) => {
  const map: Record<string, string> = {
    NORMAL_USER: '普通用户',
    SENIOR_USER: '高级用户',
    ADMIN: '管理员'
  }
  return map[role] || role
}

const userStatusLabel = (status: number) => {
  const map: Record<number, string> = {
    0: '禁用',
    1: '正常',
    2: '未验证'
  }
  return map[status] || '未知'
}

const toggleUserStatus = async (user: any) => {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await adminApi.updateUserStatus(user.id, newStatus)
    user.status = newStatus
    ElMessage.success('状态已更新')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '状态更新失败')
  }
}

const handleUserAction = async (user: any, command: string) => {
  try {
    if (command === 'resetPassword') {
      const { value } = await ElMessageBox.prompt(`为 ${user.username} 设置新密码`, '重置密码', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPlaceholder: '输入新密码（至少6位）',
        inputType: 'password',
        inputValidator: (v: string) => !v || v.length < 6 ? '密码长度不能小于6位' : true
      })
      await adminApi.resetUserPassword(user.id, value)
      ElMessage.success('密码已重置')
    } else if (command === 'addExp') {
      const { value } = await ElMessageBox.prompt(`为 ${user.username} 调整经验值（正数增加，负数扣除）`, '调整经验', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPlaceholder: '输入经验值，如 100 或 -50',
        inputValidator: (v: string) => !v || isNaN(Number(v)) || Number(v) === 0 ? '请输入非零整数' : true
      })
      await adminApi.adjustUserExp(user.id, Number(value))
      user.exp = Math.max(0, (user.exp || 0) + Number(value))
      ElMessage.success('经验值已调整')
    } else if (command === 'addAiPoints') {
      const { value } = await ElMessageBox.prompt(`为 ${user.username} 调整AI积分（正数增加，负数扣除）`, '调整AI积分', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPlaceholder: '输入积分数，如 50 或 -20',
        inputValidator: (v: string) => !v || isNaN(Number(v)) || Number(v) === 0 ? '请输入非零整数' : true
      })
      await adminApi.adjustUserAiPoints(user.id, Number(value))
      ElMessage.success('AI积分已调整')
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  }
}

// 分页搜索题目，支持关键词和发布状态筛选。
const searchProblems = async () => {
  problemsLoading.value = true
  try {
    const res: any = await adminApi.listProblems({
      page: problemPage.value,
      size: adminPageSize,
      keyword: problemKeyword.value || undefined,
      status: problemStatusFilter.value
    })
    const d = res.data || res
    problems.value = d.records || []
    problemTotal.value = d.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载题目列表失败')
  } finally {
    problemsLoading.value = false
  }
}

const onProblemFilterChange = () => {
  problemPage.value = 1
  searchProblems()
}

// 加载题目反馈处理队列。
const loadProblemReports = async () => {
  reportsLoading.value = true
  try {
    const res: any = await adminApi.listProblemReports({
      page: reportPage.value,
      size: adminPageSize,
      keyword: reportKeyword.value || undefined,
      status: reportStatusFilter.value || undefined
    })
    const d = res.data || res
    problemReports.value = d.records || []
    reportTotal.value = d.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载题目反馈失败')
  } finally {
    reportsLoading.value = false
  }
}

const onReportFilterChange = () => {
  reportPage.value = 1
  loadProblemReports()
}

// 处理题目反馈：标记已处理或忽略，并可填写处理说明。
const handleProblemReport = async (report: any, status: 'RESOLVED' | 'DISMISSED') => {
  try {
    const actionText = status === 'RESOLVED' ? '处理反馈' : '忽略反馈'
    const promptLabel = status === 'RESOLVED' ? '填写处理说明（可选）' : '填写忽略原因（可选）'
    const { value } = await ElMessageBox.prompt(promptLabel, actionText, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: status === 'RESOLVED' ? '例如：已修正题面并同步测试用例' : '例如：内容已在题库规范中说明'
    })
    await adminApi.handleProblemReport(report.id, {
      status,
      reply: value?.trim() || undefined
    })
    ElMessage.success(status === 'RESOLVED' ? '反馈已处理' : '反馈已忽略')
    await Promise.all([loadDashboard(), loadProblemReports()])
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.response?.data?.message || '处理反馈失败')
    }
  }
}

const toggleProblemStatus = async (problem: any) => {
  const newStatus = problem.status === 1 ? 0 : 1
  try {
    await adminApi.updateProblemStatus(problem.id, newStatus)
    problem.status = newStatus
    ElMessage.success(newStatus === 1 ? '题目已发布' : '题目已下线')
    await loadDashboard()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '题目状态更新失败')
  }
}

const problemStatusLabel = (status: number) => status === 1 ? '已发布' : '已下线'

const reportTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    BUG: '题面错误',
    WRONG_ANSWER: '参考答案有误',
    UNCLEAR: '描述不清',
    OTHER: '其他问题'
  }
  return map[type] || type || '-'
}

const reportTypeType = (type: string) => {
  const map: Record<string, string> = {
    BUG: 'danger',
    WRONG_ANSWER: 'warning',
    UNCLEAR: 'info',
    OTHER: 'primary'
  }
  return map[type] || 'info'
}

const reportStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待处理',
    RESOLVED: '已处理',
    DISMISSED: '已忽略'
  }
  return map[status] || status || '-'
}

const reportStatusType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    RESOLVED: 'success',
    DISMISSED: 'info'
  }
  return map[status] || 'info'
}

const difficultyType = (d: string) => {
  const map: Record<string, string> = { EASY: 'success', MEDIUM: 'warning', HARD: '', EXTREME: 'danger', 简单: 'success', 普通: 'warning', 困难: '', 极限: 'danger' }
  return map[d] || 'info'
}

// 加载高级用户升级申请。
const loadUpgradeRequests = async () => {
  upgradesLoading.value = true
  try {
    const res: any = await adminApi.listUpgradeRequests(upgradeStatusFilter.value || undefined)
    const data = (res.data || res) || []
    upgradeRequests.value = data
    pendingUpgradeCount.value = data.filter((item: any) => item.status === 'PENDING').length
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载升级申请失败')
  } finally {
    upgradesLoading.value = false
  }
}

// 审核高级用户申请。
const reviewRequest = async (req: any, action: string) => {
  try {
    let comment = ''
    if (action === 'reject') {
      const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入拒绝理由...'
      })
      comment = value
    } else {
      await ElMessageBox.confirm(`确定通过 ${req.username} 的高级用户申请？`, '确认')
    }
    await adminApi.reviewUpgradeRequest(req.id, action, comment, currentAdminId())
    ElMessage.success(action === 'approve' ? '已通过申请' : '已拒绝申请')
    await Promise.all([loadUpgradeRequests(), loadDashboard()])
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '处理申请失败')
    }
  }
}

// 加载 AI 模型提供商和已导入模型。
const loadAiProviders = async () => {
  aiProvidersLoading.value = true
  try {
    const res: any = await adminApi.listAiProviders()
    const data = (res.data || res || []).map((provider: any) => ({
      ...provider,
      enabled: Number(provider.enabled) === 1,
      models: (provider.models || []).map((model: any) => ({
        ...model,
        enabledBool: Number(model.enabled) === 1
      }))
    }))
    aiProviders.value = data
    if (!activeAiProviderId.value && data.length > 0) {
      selectAiProvider(data[0])
    } else if (activeAiProviderId.value) {
      const current = data.find((item: any) => item.id === activeAiProviderId.value)
      if (current) selectAiProvider(current)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载AI模型配置失败')
  } finally {
    aiProvidersLoading.value = false
  }
}

const selectAiProvider = (provider: any) => {
  activeAiProviderId.value = provider.id
  Object.assign(aiProviderForm, {
    providerKey: provider.providerKey || '',
    name: provider.name || '',
    apiType: provider.apiType || 'OPENAI_COMPATIBLE',
    baseUrl: provider.baseUrl || '',
    apiKey: '',
    enabled: !!provider.enabled
  })
}

const addAiProvider = () => {
  const draft = {
    id: null,
    providerKey: '',
    name: '',
    apiType: 'OPENAI_COMPATIBLE',
    baseUrl: 'https://api.openai.com/v1',
    apiKey: '',
    enabled: true,
    models: []
  }
  aiProviders.value.unshift(draft)
  selectAiProvider(draft)
}

// 保存 AI 模型提供商配置。
const saveAiProvider = async () => {
  if (!aiProviderForm.providerKey.trim() || !aiProviderForm.baseUrl.trim()) {
    ElMessage.warning('请填写提供商ID和Base URL')
    return
  }
  const payload = {
    providerKey: aiProviderForm.providerKey.trim(),
    name: aiProviderForm.name.trim() || aiProviderForm.providerKey.trim(),
    apiType: aiProviderForm.apiType,
    baseUrl: aiProviderForm.baseUrl.trim(),
    apiKey: aiProviderForm.apiKey.trim(),
    enabled: aiProviderForm.enabled ? 1 : 0
  }
  try {
    if (activeAiProvider.value?.id) {
      await adminApi.updateAiProvider(activeAiProvider.value.id, payload)
    } else {
      const res: any = await adminApi.createAiProvider(payload)
      activeAiProviderId.value = (res.data || res).id
    }
    aiProviderForm.apiKey = ''
    ElMessage.success('AI提供商配置已保存')
    await loadAiProviders()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存AI提供商失败')
  }
}

// 删除当前 AI 模型提供商。
const deleteAiProvider = async () => {
  if (!activeAiProvider.value?.id) return
  try {
    await ElMessageBox.confirm(`确定删除提供商 ${activeAiProvider.value.name}？其模型配置也会删除。`, '删除提供商')
    await adminApi.deleteAiProvider(activeAiProvider.value.id)
    activeAiProviderId.value = null
    ElMessage.success('已删除提供商')
    await loadAiProviders()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

const fetchRemoteAiModels = async () => {
  if (!activeAiProvider.value?.id) {
    ElMessage.warning('请先保存提供商配置')
    return
  }
  try {
    const res: any = await adminApi.fetchAiProviderModels(activeAiProvider.value.id)
    remoteModels.value = res.data || res || []
    remoteModelDialogVisible.value = true
    if (remoteModels.value.length === 0) {
      ElMessage.warning('远程接口没有返回模型')
    } else if (availableRemoteModels.value.length === 0) {
      ElMessage.info('远程模型均已添加，列表已自动过滤')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取模型列表失败')
  }
}

const importRemoteModel = async (model: any) => {
  if (!activeAiProvider.value?.id) return
  try {
    await adminApi.createAiModel(activeAiProvider.value.id, {
      modelId: model.modelId,
      displayName: model.displayName || model.modelId,
      costMultiplier: 1,
      contextWindow: 32768,
      capabilityTags: 'chat,code',
      enabled: 1
    })
    ElMessage.success('模型已添加')
    await loadAiProviders()
    remoteModels.value = remoteModels.value.filter((item: any) =>
      normalizeModelId(item.modelId) !== normalizeModelId(model.modelId)
    )
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '添加模型失败')
  }
}

const openCustomModelDialog = (model?: any) => {
  Object.assign(customModelForm, {
    id: model?.id || null,
    modelId: model?.modelId || '',
    displayName: model?.displayName || '',
    costMultiplier: model?.costMultiplier ?? 1,
    contextWindow: model?.contextWindow ?? 32768,
    capabilityTags: model?.capabilityTags || 'chat,code',
    enabledBool: model ? !!model.enabledBool : true
  })
  customModelDialogVisible.value = true
}

// 新增或编辑单个 AI 模型配置。
const saveCustomModel = async () => {
  if (!activeAiProvider.value?.id || !customModelForm.modelId.trim()) {
    ElMessage.warning('请填写模型ID')
    return
  }
  const payload = {
    modelId: customModelForm.modelId.trim(),
    displayName: customModelForm.displayName.trim() || customModelForm.modelId.trim(),
    costMultiplier: customModelForm.costMultiplier,
    contextWindow: customModelForm.contextWindow,
    capabilityTags: customModelForm.capabilityTags,
    enabled: customModelForm.enabledBool ? 1 : 0
  }
  try {
    if (customModelForm.id) {
      await adminApi.updateAiModel(customModelForm.id, payload)
    } else {
      await adminApi.createAiModel(activeAiProvider.value.id, payload)
    }
    customModelDialogVisible.value = false
    ElMessage.success('模型配置已保存')
    await loadAiProviders()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存模型失败')
  }
}

const saveAiModel = async (model: any) => {
  try {
    await adminApi.updateAiModel(model.id, {
      modelId: model.modelId,
      displayName: model.displayName,
      costMultiplier: model.costMultiplier,
      contextWindow: model.contextWindow,
      capabilityTags: model.capabilityTags,
      enabled: model.enabledBool ? 1 : 0
    })
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存模型失败')
  }
}

// 启用或停用某个 AI 模型。
const toggleAiModel = async (model: any) => {
  await saveAiModel(model)
}

const deleteAiModel = async (model: any) => {
  try {
    await ElMessageBox.confirm(`确定删除模型 ${model.modelId}？`, '删除模型')
    await adminApi.deleteAiModel(model.id)
    ElMessage.success('已删除模型')
    await loadAiProviders()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || '删除模型失败')
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

watch(activeTab, async (tab) => {
  if (tab === 'problems' && problems.value.length === 0) await searchProblems()
  if (tab === 'reports' && problemReports.value.length === 0) await loadProblemReports()
  if (tab === 'upgrades' && upgradeRequests.value.length === 0) await loadUpgradeRequests()
  if (tab === 'aiModels' && aiProviders.value.length === 0) await loadAiProviders()
  if (tab === 'judgeCluster') await loadJudgeCluster()
  resetJudgeClusterTimer()
  if (tab === 'stats') {
    await Promise.all([loadOverview(), loadChart()])
  }
})

const loadChart = async () => {
  try {
    chartLib = chartLib || await import('echarts')
    const res: any = await adminApi.getSubmissionStats(14)
    const d = res.data || res
    await nextTick()
    if (!chartRef.value) return
    if (!chart) {
      chart = chartLib.init(chartRef.value)
    }
    chart.setOption({
      title: { text: '近两周提交趋势', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: d.dates || [] },
      yAxis: { type: 'value', name: '提交数' },
      series: [{
        data: d.counts || [],
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(64,158,255,0.2)' },
        lineStyle: { color: '#409EFF', width: 3 }
      }],
      grid: { left: 50, right: 30, top: 60, bottom: 30 }
    })
    if (!resizeHandler) {
      resizeHandler = () => chart?.resize()
      window.addEventListener('resize', resizeHandler)
    }
  } catch (e) {
    console.error('加载统计图表失败', e)
  }
}
</script>

<style scoped>
.admin-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 30px 20px;
}

.admin-container h1 {
  font-size: 2rem;
  color: #333;
  margin-bottom: 24px;
}

.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #409eff;
}

.stat-label {
  margin-top: 8px;
  color: #666;
}

.admin-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  border-radius: 10px;
}

.overview-label {
  color: #909399;
  font-size: 14px;
}

.overview-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.chart-box {
  width: 100%;
  height: 400px;
}

.text-muted {
  color: #909399;
}

.judge-cluster {
  min-height: 420px;
}

.cluster-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.cluster-toolbar h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.cluster-toolbar span,
.cluster-footnote,
.cluster-panel-head span {
  color: #909399;
  font-size: 13px;
}

.cluster-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cluster-summary {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.cluster-summary-card {
  border-radius: 8px;
}

.cluster-summary-label {
  color: #909399;
  font-size: 13px;
}

.cluster-summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.cluster-panels,
.cluster-table-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 14px;
  margin-bottom: 14px;
}

.cluster-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  min-width: 0;
}

.cluster-panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.cluster-panel-head strong {
  font-size: 15px;
  color: #303133;
}

.queue-meter {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.queue-meter span {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.queue-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.queue-grid div {
  border: 1px solid #eef1f5;
  border-radius: 6px;
  padding: 10px;
  background: #fafbfc;
}

.queue-grid span {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 5px;
}

.queue-grid strong {
  color: #303133;
  font-size: 18px;
}

.node-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.node-card {
  border: 1px solid #eef1f5;
  border-radius: 8px;
  padding: 12px;
  background: #fafbfc;
  min-width: 0;
}

.node-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.node-card-head div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.node-card-head strong {
  color: #303133;
  font-size: 14px;
}

.node-card-head span,
.node-meta,
.node-error {
  color: #606266;
  font-size: 12px;
}

.node-usage {
  display: grid;
  grid-template-columns: 1fr 58px;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.node-usage span {
  color: #606266;
  font-size: 13px;
}

.node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.node-error {
  margin-top: 8px;
  color: #f56c6c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cluster-footnote {
  margin-top: 12px;
  text-align: right;
}

.ai-model-admin {
  min-height: 620px;
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 22px;
}

.provider-panel {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  padding: 14px;
  background: #fbfcfe;
}

.provider-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px 14px;
  border-bottom: 1px solid #edf1f7;
  margin-bottom: 10px;
}

.provider-panel-header h3,
.provider-detail-header h3,
.model-toolbar-title h3 {
  margin: 0;
  font-size: 18px;
}

.provider-panel-header span,
.provider-detail-header p,
.model-toolbar-title span {
  color: #909399;
  font-size: 13px;
}

.provider-item {
  width: 100%;
  border: 0;
  background: transparent;
  display: grid;
  grid-template-columns: 44px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 13px 12px;
  border-radius: 16px;
  cursor: pointer;
  text-align: left;
  transition: all 0.18s ease;
}

.provider-item:hover,
.provider-item.active {
  background: #eef3f7;
}

.provider-logo {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #111827, #0f766e);
  font-weight: 800;
}

.provider-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.provider-info span {
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.provider-detail,
.provider-empty {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  padding: 22px;
  background: #fff;
}

.provider-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.provider-header-actions {
  display: flex;
  gap: 10px;
}

.provider-form {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  margin-bottom: 20px;
}

.model-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.remote-model-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin: -4px 0 12px;
  color: #606266;
  font-size: 13px;
}

.model-toolbar-title {
  margin-right: auto;
}

.model-list-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
}

.model-row {
  display: grid;
  grid-template-columns: 1fr 130px 180px 80px 70px 70px;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #edf1f7;
}

.model-row:last-child {
  border-bottom: 0;
}

.model-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.model-main span {
  color: #909399;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.model-empty,
.provider-empty {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  flex-direction: column;
}

.empty-cursor {
  font-size: 54px;
  color: #cbd5e1;
}

@media (max-width: 960px) {
  .dashboard-cards,
  .overview-grid,
  .cluster-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-model-admin {
    grid-template-columns: 1fr;
  }

  .cluster-panels,
  .cluster-table-grid,
  .node-cards {
    grid-template-columns: 1fr;
  }

  .model-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .dashboard-cards,
  .overview-grid,
  .cluster-summary {
    grid-template-columns: 1fr;
  }

  .tab-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .cluster-toolbar,
  .cluster-panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .queue-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>



