<!-- 创建题目页面 — 含 AI 辅助出题 -->
<template>
  <div class="problem-create">
    <!-- AI辅助出题抽屉 -->
    <el-drawer
      v-model="aiDrawerVisible"
      :title="aiDrawerTitle"
      :size="aiDrawerSize"
      :class="['ai-problem-drawer', { 'ai-batch-drawer': isAiBatchMode }]"
      :before-close="handleAiDrawerClose"
    >
      <div class="ai-drawer-content" :class="{ 'batch-workbench': isAiBatchMode }">
        <el-tabs v-model="aiGenerationMode" class="ai-mode-tabs">
          <el-tab-pane label="单题出题" name="single" :disabled="aiGenerating" />
          <el-tab-pane label="批量出题" name="batch" :disabled="aiGenerating" />
        </el-tabs>
        <div class="ai-mode-note">
          {{ aiModeNote }}
        </div>
        <div class="ai-prompt-section">
          <h3>告诉AI你的出题方向</h3>
          <el-input
            v-model="aiPrompt"
            type="textarea"
            :rows="5"
            :placeholder="aiPromptPlaceholder"
          />
          <div class="ai-generation-options">
            <el-form-item label="生成难度">
              <el-select v-model="aiDifficultyMode" style="width: 112px">
                <el-option label="自动判断" value="AUTO" />
                <el-option label="简单" value="简单" />
                <el-option label="普通" value="普通" />
                <el-option label="困难" value="困难" />
                <el-option label="极限" value="极限" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="isAiBatchMode" label="题目数量">
              <el-input-number v-model="aiBatchCount" :min="2" :max="6" />
            </el-form-item>
            <el-form-item label="公开样例数量">
              <el-input-number v-model="aiExampleCount" :min="1" :max="6" />
            </el-form-item>
            <el-form-item label="隐藏评测点">
              <el-input-number v-model="aiTestCaseCount" :min="1" :max="10" />
            </el-form-item>
            <el-form-item label="总分">
              <el-input-number v-model="totalScoreValue" :min="1" :max="100" :step="1" />
            </el-form-item>
          </div>
          <div class="ai-examples">
            <p>示例提示：</p>
            <div class="ai-example-tags">
              <el-tag
                v-for="(example, index) in aiExamplePrompts"
                :key="example"
                @click="useAiExample(index)"
                class="example-tag"
              >
                {{ example }}
              </el-tag>
            </div>
          </div>
          <div class="ai-actions">
            <div class="ai-model-selector">
              <span class="ai-model-label">AI模型：</span>
              <el-select
                v-model="selectedAiModel"
                placeholder="选择AI模型"
                :loading="aiModelsLoading"
                style="width: 280px"
                size="default"
              >
                <el-option
                  v-for="m in aiModels"
                  :key="m.id"
                  :label="`${m.name} (${m.cost}x积分)`"
                  :value="m.id"
                />
              </el-select>
            </div>
            <div class="ai-action-buttons">
            <el-button type="primary" :loading="aiGenerating" @click="generateProblem">
              {{ aiGenerationButtonText }}
            </el-button>
            <el-button @click="aiDrawerVisible = false">取消</el-button>
            </div>
          </div>
        </div>
        
        <el-divider v-if="aiResponse || aiGenerating || aiGenerationErrorText" />

        <el-alert
          v-if="aiGenerationErrorText && !aiGenerating"
          class="ai-generation-error"
          type="error"
          :title="aiGenerationErrorText"
          show-icon
          :closable="false"
        />
        
        <div v-if="aiGenerating" class="ai-generating-animation">
          <div class="ai-loading-container">
            <div class="ai-loading-icon">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div class="ai-loading-text">
              <div class="ai-loading-title">{{ aiGenerationTitle }}</div>
              <div class="ai-loading-subtitle">{{ aiGenerationSubtitle }}</div>
              <div class="ai-loading-status">{{ aiGenerationStatusText }}<template v-if="aiGenerationElapsedText"> · {{ aiGenerationElapsedText }}</template></div>
              <div class="ai-loading-steps">
                <div class="ai-step" :class="{'active': aiGeneratingStep >= 1}">
                  <span>1. 读取生成参数</span>
                  <span v-if="aiGeneratingStep >= 1" class="typing-cursor">
                    <span v-for="(token, index) in aiStepTokens[0]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
                <div class="ai-step" :class="{'active': aiGeneratingStep >= 2}">
                  <span>2. 提交异步任务</span>
                  <span v-if="aiGeneratingStep >= 2" class="typing-cursor">
                    <span v-for="(token, index) in aiStepTokens[1]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
                <div class="ai-step" :class="{'active': aiGeneratingStep >= 3}">
                  <span>3. 调用模型生成</span>
                  <span v-if="aiGeneratingStep >= 3" class="typing-cursor">
                    <span v-for="(token, index) in aiStepTokens[2]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
                <div class="ai-step" :class="{'active': aiGeneratingStep >= 4}">
                  <span>4. 校验字段格式</span>
                  <span v-if="aiGeneratingStep >= 4" class="typing-cursor">
                    <span v-for="(token, index) in aiStepTokens[3]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
                <div class="ai-step" :class="{'active': aiGeneratingStep >= 5}">
                  <span>5. 整理预览结果</span>
                  <span v-if="aiGeneratingStep >= 5" class="typing-cursor">
                    <span v-for="(token, index) in aiStepTokens[4]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="aiResponse && !aiGenerating" class="ai-response-section">
          <h3>{{ isAiBatchMode ? '待批量创建的题目' : 'AI生成的题目' }}</h3>
          <div class="ai-response-content">
            <div v-if="isAiBatchMode && aiCandidateProblems.length > 0" class="ai-batch-panel">
              <div class="ai-batch-header">
                <div>
                  <strong>本次生成 {{ aiCandidateProblems.length }} 道题</strong>
                  <span>确认后会批量创建为私有题，后续可逐题编辑或公开。</span>
                </div>
                <el-button
                  type="success"
                  :loading="aiBatchSaving"
                  :disabled="aiCandidateProblems.length === 0"
                  @click="saveBatchAiProblems"
                >
                  一键创建全部题目
                </el-button>
              </div>
              <div class="ai-batch-list">
                <div
                  v-for="(candidate, index) in aiCandidateProblems"
                  :key="index"
                  class="ai-batch-item"
                  :class="{ active: activeAiCandidateIndex === index }"
                  @click="selectAiCandidate(index)"
                >
                  <div class="ai-batch-index">{{ index + 1 }}</div>
                  <div class="ai-batch-meta">
                    <strong>{{ candidate.title || '未命名题目' }}</strong>
                    <span>{{ candidate.difficulty || '普通' }} · {{ (candidate.tags || []).join('、') || '未标注标签' }}</span>
                    <p>{{ stripHtml(candidate.description || '').slice(0, 72) || '暂无题面摘要' }}</p>
                    <small>{{ (candidate.examples || []).length }} 组公开样例 · {{ (candidate.testCases || []).length }} 个隐藏评测点</small>
                  </div>
                  <div class="ai-batch-actions" @click.stop>
                    <el-button size="small" @click="selectAiCandidate(index)">预览</el-button>
                    <el-button size="small" type="primary" plain @click="regenerateBatchProblem(index)">重生成</el-button>
                    <el-button
                      v-if="aiCandidateProblems.length > 1"
                      size="small"
                      type="danger"
                      plain
                      @click="removeBatchProblem(index)"
                    >
                      移除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <el-card class="ai-result-card" shadow="hover">
              <template #header>
                <div class="ai-result-header">
                  <h4>{{ aiResponse.title }}</h4>
                  <div class="ai-result-tags">
                    <el-tag size="small" type="success">{{ aiResponse.difficulty }}</el-tag>
                    <el-tag size="small" v-for="(tag, index) in aiResponse.tags" :key="index">{{ tag }}</el-tag>
                  </div>
                </div>
              </template>
              
              <div class="ai-result-description">
                <h5>题目描述</h5>
            <div v-html="aiResponse.description"></div>
              </div>
              
              <el-divider />
              
              <el-collapse>
                <el-collapse-item title="输入输出格式" name="1">
                  <div class="io-format">
                    <div class="input-format">
                      <h5>输入格式</h5>
                      <div v-html="aiResponse.inputFormat"></div>
                    </div>
                    <div class="output-format">
                      <h5>输出格式</h5>
                      <div v-html="aiResponse.outputFormat"></div>
                    </div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="示例与提示" name="2">
                  <div class="examples-section">
                    <div v-for="(example, index) in aiResponse.examples" :key="index" class="example">
                      <h5>公开样例 {{ index + 1 }} 输入</h5>
                      <pre>{{ example.input }}</pre>
                      <h5>公开样例 {{ index + 1 }} 输出</h5>
                      <pre>{{ example.output }}</pre>
                      <div v-if="example.explanation" class="example-explanation" v-html="example.explanation"></div>
                    </div>
                    <div class="hint" v-if="aiResponse.hint">
                      <h5>提示</h5>
                      <div v-html="aiResponse.hint"></div>
                    </div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="测试用例" name="3">
                  <div class="test-cases">
                    <div v-for="(testCase, index) in aiResponse.testCases" :key="index" class="test-case">
                      <h5>隐藏评测点 {{ index + 1 }} · {{ testCase.score }}分</h5>
                      <div class="test-case-content">
                        <div class="test-case-input">
                          <strong>输入：</strong>
                          <pre>{{ testCase.input }}</pre>
                        </div>
                        <div class="test-case-output">
                          <strong>输出：</strong>
                          <pre>{{ testCase.output }}</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </el-card>
            
            <div class="ai-response-actions">
              <el-button v-if="!isAiBatchMode" type="primary" @click="applyAiProblem">应用到表单</el-button>
              <el-button
                v-if="isAiBatchMode && aiCandidateProblems.length > 0"
                type="success"
                :loading="aiBatchSaving"
                @click="saveBatchAiProblems"
              >
                一键创建全部题目
              </el-button>
              <el-button @click="regenerateProblem">{{ isAiBatchMode ? '重新生成整套题' : '重新生成' }}</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
    
    <!-- AI辅助生成题解抽屉 -->
    <el-drawer
      v-model="aiSolutionDrawerVisible"
      title="AI辅助生成题解"
      size="50%"
      :before-close="handleAiSolutionDrawerClose"
    >
      <div class="ai-drawer-content">
        <div class="ai-prompt-section">
          <h3>告诉AI你需要什么样的题解</h3>
          <el-input
            v-model="aiSolutionPrompt"
            type="textarea"
            :rows="5"
            placeholder="例如：请为这道题目生成一个详细解题思路和代码实现，包括时间复杂度分析..."
          />
          <div class="ai-examples">
            <p>示例提示：</p>
            <div class="ai-example-tags">
            <el-tag @click="useAiSolutionExample(0)" class="example-tag">生成一个简洁清晰的解题思路，包含时空复杂度分析</el-tag>
            <el-tag @click="useAiSolutionExample(1)" class="example-tag">提供多种解法对比和C++代码实现</el-tag>
            <el-tag @click="useAiSolutionExample(2)" class="example-tag">生成详细的步骤分解和Python代码</el-tag>
              <el-tag @click="useAiSolutionExample(3)" class="example-tag">请解释该问题的最优算法并提供代码示例</el-tag>
              <el-tag @click="useAiSolutionExample(4)" class="example-tag">生成初学者容易理解的解题思路和代码</el-tag>
            </div>
          </div>
          <div class="ai-actions">
            <el-button type="primary" :loading="aiSolutionGenerating" @click="generateSolution">
              {{ aiSolutionGenerating ? '生成中...' : '生成题解' }}
            </el-button>
            <el-button @click="aiSolutionDrawerVisible = false">取消</el-button>
          </div>
        </div>
        
        <el-divider v-if="aiSolutionResponse || aiSolutionGenerating" />
        
        <div v-if="aiSolutionGenerating" class="ai-generating-animation">
          <div class="ai-loading-container">
            <div class="ai-loading-icon">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div class="ai-loading-text">
              <div class="ai-loading-title">AI正在思考题解...</div>
              <div class="ai-loading-steps">
                <div class="ai-step" :class="{'active': aiSolutionGeneratingStep >= 1}">
                  <span>1. 分析问题特征</span>
                  <span v-if="aiSolutionGeneratingStep >= 1" class="typing-cursor">
                    <span v-for="(token, index) in aiSolutionStepTokens[0]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
                <div class="ai-step" :class="{'active': aiSolutionGeneratingStep >= 2}">
                  <span>2. 设计算法思路</span>
                  <span v-if="aiSolutionGeneratingStep >= 2" class="typing-cursor">
                    <span v-for="(token, index) in aiSolutionStepTokens[1]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
                <div class="ai-step" :class="{'active': aiSolutionGeneratingStep >= 3}">
                  <span>3. 优化时间复杂度</span>
                  <span v-if="aiSolutionGeneratingStep >= 3" class="typing-cursor">
                    <span v-for="(token, index) in aiSolutionStepTokens[2]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
                <div class="ai-step" :class="{'active': aiSolutionGeneratingStep >= 4}">
                  <span>4. 编写示例代码</span>
                  <span v-if="aiSolutionGeneratingStep >= 4" class="typing-cursor">
                    <span v-for="(token, index) in aiSolutionStepTokens[3]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
                <div class="ai-step" :class="{'active': aiSolutionGeneratingStep >= 5}">
                  <span>5. 完善解题说明</span>
                  <span v-if="aiSolutionGeneratingStep >= 5" class="typing-cursor">
                    <span v-for="(token, index) in aiSolutionStepTokens[4]" :key="index" 
                          :style="{ animationDelay: `${index * 100}ms` }" 
                          class="typing-token">{{ token }}</span>
                    <span class="cursor">|</span>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="aiSolutionResponse && !aiSolutionGenerating" class="ai-response-section">
          <h3>AI生成的题解</h3>
          <div class="ai-response-content">
            <el-card class="ai-result-card" shadow="hover">
              <template #header>
                <div class="ai-result-header">
            <h4>解题思路</h4>
                </div>
              </template>
              
            <div class="solution-thinking" v-html="aiSolutionResponse.thinking"></div>
              
              <el-divider />
            
            <h4>代码实现</h4>
              <div class="solution-code-container">
                <div class="solution-language-selector">
                  <el-radio-group v-model="activeSolutionLang" size="small">
                    <el-radio-button v-for="(code, lang) in aiSolutionResponse.codes" :key="lang" :label="lang">
                      {{ lang }}
                    </el-radio-button>
                  </el-radio-group>
                </div>
                
                <div v-for="(code, lang) in aiSolutionResponse.codes" :key="lang" 
                     v-show="activeSolutionLang === lang" class="code-preview-container">
                  <pre class="code-preview">{{ code }}</pre>
                  <el-button size="small" @click="copySolutionCode(code)" class="copy-code-btn">
                    <el-icon><Document /></el-icon> 复制代码
                  </el-button>
            </div>
              </div>
            </el-card>
            
            <div class="ai-response-actions">
              <el-button type="primary" @click="applyAiSolution">应用到题解</el-button>
              <el-button @click="regenerateSolution">重新生成</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-card class="form-card" shadow="never">
      <template #header>
        <div class="card-header">
          <h2>创建新题目</h2>
          <div class="header-actions">
            <el-button type="info" @click="openAiDrawer('single')">
              <el-icon><Setting /></el-icon> AI单题出题
            </el-button>
            <el-button type="warning" plain @click="openAiDrawer('batch')">
              <el-icon><Plus /></el-icon> AI批量出题
            </el-button>
            <el-button type="success" @click="saveDraft">保存草稿</el-button>
            <el-button type="primary" @click="submitProblem">提交题目</el-button>
          </div>
        </div>
      </template>

      <el-form :model="problemForm" :rules="rules" ref="problemFormRef" label-position="top" class="problem-form">
        <el-card class="form-section-card" shadow="hover">
          <template #header>
            <div class="form-section-header">
              <h3>基本信息</h3>
            </div>
          </template>

        <el-row :gutter="20">
          <el-col :span="16">
            <!-- 基本信息 -->
            <el-form-item label="题目标题" prop="title">
              <el-input v-model="problemForm.title" placeholder="请输入题目标题" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="problemForm.difficulty" placeholder="请选择难度" style="width: 100%">
                <el-option label="简单" value="简单" />
                <el-option label="普通" value="普通" />
                <el-option label="困难" value="困难" />
                <el-option label="极限" value="极限" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题目标签" prop="categories">
          <el-select
            v-model="problemForm.categories"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或创建标签"
            style="width: 100%"
          >
            <el-option v-for="tag in commonTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        </el-card>

        <el-card class="form-section-card" shadow="hover">
          <template #header>
            <div class="form-section-header">
              <h3>题目描述与格式</h3>
            </div>
          </template>
        <!-- 题目描述 - 富文本编辑器 -->
        <el-form-item label="题目描述" prop="description">
          <div class="editor-container">
            <QuillEditor
              ref="descriptionEditorRef"
              v-model:content="problemForm.description"
              contentType="html"
              theme="snow"
              toolbar="full"
              placeholder="请在这里详细描述题目要求..."
            />
          </div>
        </el-form-item>

        <!-- 输入输出格式 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="输入格式" prop="inputFormat">
              <div class="editor-container">
                <QuillEditor
                  ref="inputFormatEditorRef"
                  v-model:content="problemForm.inputFormat"
                  contentType="html"
                  theme="snow"
                  toolbar="essential"
                  placeholder="请描述输入数据的格式..."
                />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="输出格式" prop="outputFormat">
              <div class="editor-container">
                <QuillEditor
                  ref="outputFormatEditorRef"
                  v-model:content="problemForm.outputFormat"
                  contentType="html"
                  theme="snow"
                  toolbar="essential"
                  placeholder="请描述输出结果的格式..."
                />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        </el-card>

        <el-card class="form-section-card" shadow="hover">
          <template #header>
            <div class="form-section-header">
              <h3>公开样例</h3>
            </div>
          </template>
        <div class="examples-editor">
          <div
            v-for="(example, index) in problemForm.examples"
            :key="example.uid || index"
            class="public-example-item"
          >
            <div class="public-example-header">
              <span>公开样例 {{ index + 1 }}</span>
              <el-button
                v-if="problemForm.examples.length > 1"
                type="danger"
                link
                native-type="button"
                :icon="Delete"
                @click.stop="removePublicExample(index)"
              >
                删除
              </el-button>
            </div>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item :label="`公开样例 ${index + 1} 输入`">
                  <el-input
                    v-model="example.input"
                    type="textarea"
                    :rows="5"
                    placeholder="请输入样例输入数据；无输入题可留空"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="`公开样例 ${index + 1} 输出`"
                  :prop="`examples.${index}.output`"
                  :rules="publicExampleRules.output"
                >
                  <el-input
                    v-model="example.output"
                    type="textarea"
                    :rows="5"
                    placeholder="请输入样例输出结果"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="样例解释（可选）">
              <el-input
                v-model="example.explanation"
                type="textarea"
                :rows="2"
                placeholder="可简单说明该样例的计算过程"
              />
            </el-form-item>
          </div>
          <el-button type="primary" plain :icon="Plus" @click="addPublicExample">
            添加公开样例
          </el-button>
        </div>

        <!-- 提示信息 -->
        <el-form-item label="提示信息（可选）">
          <div class="editor-container">
            <QuillEditor
              ref="hintEditorRef"
              v-model:content="problemForm.hint"
              contentType="html"
              theme="snow"
              toolbar="essential"
              placeholder="可以添加一些解题提示..."
            />
          </div>
        </el-form-item>
        </el-card>

        <el-card class="form-section-card" shadow="hover">
          <template #header>
            <div class="form-section-header">
              <h3>测试用例（计入评分）</h3>
          <div class="total-score-setting">
            <div class="total-score-input">
              <span>总分值：</span>
              <el-input-number 
                v-model="totalScoreValue" 
                :min="1" 
                :max="100" 
                :step="1" 
                size="small"
              />
            </div>
          </div>
        </div>
          </template>
        
        <div v-for="(testCase, index) in problemForm.testCases" :key="index" class="test-case-item">
          <el-row :gutter="20">
            <el-col :span="10">
              <el-form-item :label="`测试用例 ${index + 1} 输入`" :prop="`testCases.${index}.input`" :rules="testCaseRules.input">
                <el-input 
                  v-model="testCase.input" 
                  type="textarea" 
                  :rows="4" 
                  placeholder="请输入测试用例输入数据；无输入题可留空"
                />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item :label="`测试用例 ${index + 1} 输出`" :prop="`testCases.${index}.output`" :rules="testCaseRules.output">
                <el-input 
                  v-model="testCase.output" 
                  type="textarea" 
                  :rows="4" 
                  placeholder="请输入测试用例期望输出"
                />
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <div class="test-case-controls">
                <el-form-item :label="`分值`" :prop="`testCases.${index}.score`" :rules="testCaseRules.score">
                  <el-input-number 
                    v-model="testCase.score" 
                    :min="1" 
                    :max="totalScoreValue" 
                    :step="1" 
                    size="small"
                  />
                </el-form-item>
                <el-button 
                  type="danger" 
                  circle 
                  @click="removeTestCase(index)" 
                  :disabled="problemForm.testCases.length <= 1"
                  icon="Delete"
                  size="small"
                />
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="add-test-case">
          <el-button type="primary" plain @click="addTestCase">
            <el-icon><Plus /></el-icon> 添加测试用例
          </el-button>
        </div>
        </el-card>

        <el-card class="form-section-card" shadow="hover">
          <template #header>
            <div class="form-section-header">
              <h3>参考题解</h3>
          <el-button type="info" @click="openAiSolutionDrawer">
            <el-icon><Setting /></el-icon> AI辅助生成题解
          </el-button>
        </div>
          </template>
        
        <el-form-item label="解题思路">
          <div class="editor-container">
            <QuillEditor
              ref="solutionEditorRef"
              v-model:content="problemForm.solution"
              contentType="html"
              theme="snow"
              toolbar="full"
              placeholder="请输入解题思路和完整代码..."
            />
          </div>
        </el-form-item>

        <el-form-item label="参考代码">
          <div class="code-editors-container">
            <div class="language-selector">
              <span>语言:</span>
              <el-select v-model="activeCodeTab" placeholder="选择语言" size="small" style="width: 120px">
                <el-option
                  v-for="lang in codeLanguages"
                  :key="lang.id"
                  :label="getLanguageOptionLabel(lang)"
                  :value="lang.id">
                </el-option>
              </el-select>
              <el-tooltip
                content="带有 ✓ 标记的语言表示已有参考代码实现"
                placement="top"
                effect="light"
              >
                <el-icon class="help-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div class="code-notice">
              <el-alert
                title="请为每种语言提供参考代码实现，未实现的语言将不会保存到数据库"
                type="info"
                :closable="false"
                show-icon
              />
            </div>
            <div id="codeEditor" class="ace-editor-container"></div>
          </div>
        </el-form-item>
        </el-card>

        <el-card class="form-section-card" shadow="hover">
          <template #header>
            <div class="form-section-header">
              <h3>设置</h3>
            </div>
          </template>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="时间限制 (ms)" prop="timeLimit">
              <el-input-number v-model="problemForm.timeLimit" :min="100" :max="10000" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="内存限制 (MB)" prop="memoryLimit">
              <el-input-number v-model="problemForm.memoryLimit" :min="16" :max="1024" :step="16" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可见性">
              <el-radio-group v-model="problemForm.isPublic">
                <el-radio :value="true">公开</el-radio>
                <el-radio :value="false">私有</el-radio>
              </el-radio-group>
              <div class="privacy-tip">私有题可放入私有题目集并导入竞赛；非作者仅在授权竞赛中可做题。</div>
            </el-form-item>
          </el-col>
        </el-row>

          <div class="form-actions">
            <el-button type="info" @click="cancel">取消</el-button>
            <el-button type="success" @click="saveDraft">保存草稿</el-button>
            <el-button type="primary" @click="submitProblem">提交题目</el-button>
          </div>
        </el-card>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Setting, Plus, Delete, Loading, Document, QuestionFilled } from '@element-plus/icons-vue';
import { QuillEditor } from '@vueup/vue-quill';
import { createProblem, getAllTags } from '@/api/problem';
import { aiApi } from '@/api/ai';
import { loadAceCore, loadAceExt, loadAceMode, loadAceTheme } from '@/utils/aceLoader';
import '@vueup/vue-quill/dist/vue-quill.snow.css';

// 路由器
const router = useRouter();
const route = useRoute();
const problemFormRef = ref(null);

// 创建页面始终为创建模式，不是编辑模式
const isEditMode = ref(false);
const problemId = ref(null);

// AI辅助出题相关
const aiDrawerVisible = ref(false);
const aiPrompt = ref('');
const aiGenerating = ref(false);
const aiResponse = ref(null);
const aiCandidateProblems = ref<any[]>([]);
const activeAiCandidateIndex = ref(0);
const aiBatchSaving = ref(false);
const aiGenerationMode = ref<'single' | 'batch'>('single');
const aiDifficultyMode = ref('AUTO');
const aiGeneratingStep = ref(0);
const aiBatchCount = ref(3);
const aiExampleCount = ref(2);
const aiTestCaseCount = ref(5);
const aiGenerationStatusText = ref('');
const aiGenerationErrorText = ref('');
const aiGenerationStartedAt = ref(0);
const aiGenerationElapsedSeconds = ref(0);
// AI模型选择相关
const aiModels = ref<Array<{ id: string; name: string; cost: string }>>([]);
const selectedAiModel = ref('');
const aiModelsLoading = ref(false);
const aiStepTokens = ref([
  ['读取标签', '读取难度', '读取样例数', '...'],
  ['创建任务', '返回任务ID', '开始轮询', '...'],
  ['等待模型', '生成题面', '生成评测点', '...'],
  ['校验JSON', '过滤标签', '平衡分值', '...'],
  ['整理题目', '生成预览', '完成', '...']
]);
const isAiBatchMode = computed(() => aiGenerationMode.value === 'batch');
const effectiveAiBatchCount = computed(() => isAiBatchMode.value ? aiBatchCount.value : 1);
const aiDrawerTitle = computed(() => isAiBatchMode.value ? 'AI批量出题工作台' : 'AI单题出题');
const aiDrawerSize = computed(() => isAiBatchMode.value ? '100%' : 'min(1040px, 96vw)');
const aiModeNote = computed(() => isAiBatchMode.value
  ? '一次生成多道完整题目，适合按范围、难度和考点快速创建一套练习题；确认后会批量保存为私有题。'
  : '只生成一道题，结果用于预览并应用到当前创建表单。'
);
const aiPromptPlaceholder = computed(() => isAiBatchMode.value
  ? '例如：围绕前缀和、哈希表和双指针生成一套 4 道普通难度练习题，题目从入门到综合递进，每题 2 组公开样例、5 个隐藏评测点，并提供 C++/Java/Python 参考代码...'
  : '例如：设计一道普通难度的数组统计题，要求有清晰数据范围、2 组公开样例、5 个隐藏评测点，并提供 C++/Java/Python 参考代码...'
);
const aiExamplePrompts = computed(() => isAiBatchMode.value
  ? [
      '生成一套数组与哈希表练习题',
      '生成一套动态规划入门到进阶题',
      '生成一套二分查找专项练习',
      '生成一套字符串处理练习题'
    ]
  : [
      '创建一道关于动态规划的中等难度题目',
      '生成一道简单的数组题，适合初学者',
      '设计一道困难的图论算法题',
      '生成一道需要使用哈希表的题目',
      '创建一道考察二分查找的题目'
    ]
);
const aiGenerationButtonText = computed(() => {
  if (aiGenerating.value) return isAiBatchMode.value ? '批量生成中...' : '生成中...';
  return isAiBatchMode.value ? '生成题目套组' : '生成单题';
});
const aiGenerationTitle = computed(() => isAiBatchMode.value ? '正在批量生成题目' : '正在生成单题');
const aiGenerationSubtitle = computed(() =>
  '状态来自后端异步任务轮询，页面会等真实结果；生成失败不会扣 AI 积分。'
);
const aiGenerationElapsedText = computed(() =>
  aiGenerationElapsedSeconds.value > 0 ? `已等待 ${aiGenerationElapsedSeconds.value}s` : ''
);
// AI辅助生成题解相关
const aiSolutionDrawerVisible = ref(false);
const aiSolutionPrompt = ref('');
const aiSolutionGenerating = ref(false);
const aiSolutionResponse = ref(null);
const aiSolutionGeneratingStep = ref(0);
const activeSolutionLang = ref('Java'); // 当前选中的题解代码语言
const aiSolutionStepTokens = ref([
  ['识别问题类型', '分析数据约束', '确定核心难点', '...'],
  ['构建解题框架', '选择数据结构', '设计算法流程', '...'],
  ['分析时间复杂度', '评估空间需求', '优化算法效率', '...'],
  ['编写代码框架', '实现核心逻辑', '处理边界情况', '...'],
  ['完善算法分析', '添加详细注释', '整合最终题解', '...']
]);

// 代码编辑器相关
const activeCodeTab = ref('cpp');  // 将默认语言改为C++
const codeEditor = ref(null);
const codeLanguages = [
  { id: 'cpp', name: 'C++', mode: 'c_cpp' },
  { id: 'java', name: 'Java', mode: 'java' },
  { id: 'python', name: 'Python', mode: 'python' },
  { id: 'c', name: 'C', mode: 'c_cpp' },
  { id: 'javascript', name: 'JavaScript', mode: 'javascript' },
  { id: 'go', name: 'Go', mode: 'golang' },
  { id: 'rust', name: 'Rust', mode: 'rust' },
  { id: 'csharp', name: 'C#', mode: 'csharp' }
];

// 代码模板
const codeTemplates = {
  java: `
import java.util.*;

public class Main {
    public static void main(String[] args) {
        // 在此处编写解题代码
        Scanner scanner = new Scanner(System.in);
        
        // 读取输入
        
        // 处理并输出结果
    }
}`,
  python: `
# 在此处编写解题代码

# 读取输入

# 处理并输出结果
`,
  cpp: `
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

int main() {
    // 在此处编写解题代码
    
    // 读取输入
    
    // 处理并输出结果
    return 0;
}`,
  c: `
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main() {
    // 在此处编写解题代码
    
    // 读取输入
    
    // 处理并输出结果
    return 0;
}`,
  javascript: `
// 在此处编写解题代码

// 读取输入

// 处理并输出结果
`,
  go: `
package main

${"import"} "fmt"

func main() {
    // 在此处编写解题代码
    
    // 读取输入
    
    // 处理并输出结果
}`,
  rust: `
use std::io;

fn main() {
    // 在此处编写解题代码
    
    // 读取输入
    
    // 处理并输出结果
}`,
  csharp: `
using System;

class Program {
    static void Main() {
        // 在此处编写解题代码
        
        // 读取输入
        
        // 处理并输出结果
    }
}`
};

// 表单数据
const problemForm = reactive({
  title: '',
  difficulty: '普通', // 默认普通难度
  categories: [],
  description: '',
  inputFormat: '',
  outputFormat: '',
  examples: [
    { input: '', output: '', explanation: '' }
  ],
  inputExample: '',
  outputExample: '',
  debugInputExample: '',
  debugOutputExample: '',
  hint: '',
  testCases: [
    { input: '', output: '', score: 20 } // 默认给第一个测试用例分配全部分值
  ],
  solution: '',
  solutionCode: {
    java: '',
    python: '',
    cpp: '',
    javascript: '',
    c: '',
    go: '',
    rust: '',
    csharp: ''
  },
  timeLimit: 1000, // 默认1000ms
  memoryLimit: 256, // 默认256MB
  isPublic: false
});

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入题目标题', trigger: 'blur' },
    { min: 3, max: 100, message: '长度在 3 到 100 个字符', trigger: 'blur' }
  ],
  difficulty: [
    { required: true, message: '请选择难度', trigger: 'change' }
  ],
  categories: [
    { required: true, message: '请至少选择一个标签', trigger: 'change' },
    { type: 'array', min: 1, message: '请至少选择一个标签', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请填写题目描述', trigger: 'blur' }
  ],
  inputFormat: [],
  outputFormat: [
    { required: true, message: '请填写输出格式', trigger: 'blur' }
  ],
  inputExample: [],
  outputExample: [],
  debugInputExample: [],
  debugOutputExample: [],
  timeLimit: [
    { required: true, message: '请设置时间限制', trigger: 'blur' },
    { type: 'number', min: 100, max: 10000, message: '时间限制必须在100-10000ms之间', trigger: 'blur' }
  ],
  memoryLimit: [
    { required: true, message: '请设置内存限制', trigger: 'blur' },
    { type: 'number', min: 16, max: 1024, message: '内存限制必须在16-1024MB之间', trigger: 'blur' }
  ]
};

const publicExampleRules = {
  output: [{ required: true, message: '请输入公开样例输出', trigger: 'blur' }]
};

// 测试用例验证规则
const testCaseRules = {
  input: [],
  output: [{ required: true, message: '请输入测试用例期望输出', trigger: 'blur' }],
  score: [
    { required: true, message: '请设置分值', trigger: 'blur' },
    { type: 'number', min: 1, message: '分值必须大于等于1', trigger: 'blur' }
  ]
};

// 常用标签
const commonTags = ref([]);

// 组件挂载时加载数据并初始化
onMounted(async () => {
  try {
    console.log('ProblemCreate组件已挂载');
    // 加载标签
    await loadTags();
    
    // 初始化编辑器
    await nextTick();
    console.log('开始调用编辑器初始化函数...');
    await initCodeEditor();
    
    // 添加窗口调整大小监听
    window.addEventListener('resize', () => {
      if (codeEditor.value) {
        codeEditor.value.resize();
      }
    });
  } catch (error) {
    console.error('组件初始化失败:', error);
  }
});

// 重置表单数据
const resetProblemForm = () => {
  // 重置为默认值
  Object.assign(problemForm, {
    title: '',
    difficulty: '普通',
    categories: [],
    description: '',
    inputFormat: '',
    outputFormat: '',
    examples: [{ input: '', output: '', explanation: '' }],
    inputExample: '',
    outputExample: '',
    debugInputExample: '',
    debugOutputExample: '',
    hint: '',
    solution: '',
    timeLimit: 1000,
    memoryLimit: 256,
    isPublic: false,
    testCases: [{ input: '', output: '', score: 20 }]
  });
};

let publicExampleUidSeed = 0;

// 创建公开样例对象，uid 只给前端 v-for 使用，避免删除第 3 个样例时误删其它样例。
const createPublicExample = (example: any = {}) => ({
  uid: example.uid || `public-example-${Date.now()}-${++publicExampleUidSeed}`,
  input: String(example?.input ?? example?.inputExample ?? ''),
  output: String(example?.output ?? example?.expectedOutput ?? example?.outputExample ?? ''),
  explanation: String(example?.explanation ?? example?.explain ?? '')
});

// 规范公开样例数组：AI 返回、旧字段 inputExample/outputExample 都统一成 examples。
const normalizePublicExamples = (examples: any[] = []) => {
  const normalized = examples
    .map((example: any) => createPublicExample(example))
    .filter((example: any) => example.input || example.output);
  return normalized.length > 0 ? normalized : [createPublicExample()];
};

// 同步旧字段：后端仍保留 inputExample/outputExample/debugInputExample/debugOutputExample 兼容旧页面。
const syncLegacyExampleFields = () => {
  const examples = normalizePublicExamples(problemForm.examples);
  problemForm.examples = examples;
  const first = examples[0] || { input: '', output: '' };
  const second = examples[1] || first;
  problemForm.inputExample = first.input;
  problemForm.outputExample = first.output;
  problemForm.debugInputExample = second.input;
  problemForm.debugOutputExample = second.output;
  return examples;
};

// 新增一个公开样例，题面和做题 IDE 会按样例 1、2、3 动态展示。
const addPublicExample = () => {
  problemForm.examples.push(createPublicExample());
};

// 删除指定公开样例，至少保留一个，避免题面没有可调试样例。
const removePublicExample = (index: number) => {
  if (problemForm.examples.length <= 1) return;
  problemForm.examples = problemForm.examples.filter((_, currentIndex) => currentIndex !== index);
};

// 加载所有标签
const loadTags = async () => {
  try {
    console.log('开始加载标签...');
    const response = await getAllTags();
    console.log('标签加载结果:', response);
    if (response && response.data && Array.isArray(response.data)) {
      // API返回格式是 {data: [...]} 的情况
      commonTags.value = response.data.map(tag => tag.name);
    } else if (response && Array.isArray(response)) {
      // API直接返回数组的情况
      commonTags.value = response.map(tag => tag.name);
    } else {
      console.error('标签数据格式不正确:', response);
      // 设置一些默认标签作为备选
      commonTags.value = [
        '数组', '字符串', '哈希表', '动态规划', '链表', '二叉树', '栈', '队列', 
        '排序', '贪心', '回溯', '深度优先搜索', '广度优先搜索', '二分查找'
      ];
    }
    console.log('处理后的标签列表:', commonTags.value);
  } catch (error) {
    console.error('加载标签失败:', error);
    ElMessage.warning('加载标签列表失败，将使用默认标签');
    
    // 设置一些默认标签作为备选
    commonTags.value = [
      '数组', '字符串', '哈希表', '动态规划', '链表', '二叉树', '栈', '队列', 
      '排序', '贪心', '回溯', '深度优先搜索', '广度优先搜索', '二分查找'
    ];
  }
};

// 初始化代码编辑器
const initCodeEditor = async () => {
  try {
    const ace = await loadAceCore();
    await loadAceExt();
    await loadAceMode(getLanguageMode(activeCodeTab.value));
    await loadAceTheme('ace/theme/xcode');

    console.log('开始初始化编辑器...');

    await new Promise(resolve => setTimeout(resolve, 100));

    const editorElement = document.getElementById('codeEditor');

    if (!editorElement) {
      console.error('找不到编辑器DOM元素');
      return;
    }

    console.log('找到编辑器DOM元素');

    editorElement.style.width = '100%';
    editorElement.style.height = '450px';

    codeEditor.value = ace.edit(editorElement);
    codeEditor.value.setTheme('ace/theme/xcode');
    codeEditor.value.session.setMode(`ace/mode/${getLanguageMode(activeCodeTab.value)}`);
    codeEditor.value.setOptions({
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true,
      enableSnippets: true,
      showPrintMargin: false,
      fontSize: '14px',
      fontFamily: 'Monaco, Menlo, Consolas, "Courier New", monospace',
      minLines: 20,
      maxLines: 30,
      wrap: true,
      showLineNumbers: true,
      highlightActiveLine: true,
      tabSize: 4
    });

    const initialCode = problemForm.solutionCode[activeCodeTab.value] || codeTemplates[activeCodeTab.value] || '';
    codeEditor.value.setValue(initialCode);
    codeEditor.value.clearSelection();
    codeEditor.value.resize(true);

    console.log('代码编辑器初始化成功');

  } catch (error) {
    console.error('初始化代码编辑器失败:', error);
    ElMessage.error('代码编辑器加载失败，请刷新页面重试');
  }
};

// 获取语言对应的ACE编辑器模式
const getLanguageMode = (langId) => {
  const lang = codeLanguages.find(l => l.id === langId);
  return lang ? lang.mode : 'java';
};

// 更新当前可见编辑器大小
const updateVisibleEditor = () => {
  nextTick(() => {
    let currentEditor = null;
    
    if (activeCodeTab.value === 'java' && codeEditor) {
      currentEditor = codeEditor;
    } else if (activeCodeTab.value === 'python' && codeEditor) {
      currentEditor = codeEditor;
    } else if (activeCodeTab.value === 'cpp' && codeEditor) {
      currentEditor = codeEditor;
    } else if (activeCodeTab.value === 'c' && codeEditor) {
      currentEditor = codeEditor;
    } else if (activeCodeTab.value === 'javascript' && codeEditor) {
      currentEditor = codeEditor;
    }
    
    if (currentEditor) {
      try {
        // 强制重绘编辑器
        currentEditor.resize(true);
        currentEditor.renderer.updateFull();
        console.log(`编辑器 ${activeCodeTab.value} 已更新大小`);
      } catch (error) {
        console.error(`更新编辑器大小失败: ${error}`);
      }
    }
  });
};

// 监听代码语言切换
watch(activeCodeTab, async (newLang, oldLang) => {
  console.log(`切换到${newLang}编辑器`);

  if (codeEditor.value) {
    // 保存当前代码到对应语言
    if (oldLang) {
      problemForm.solutionCode[oldLang] = codeEditor.value.getValue();
      console.log(`已保存${oldLang}代码`);
    }

    const mode = getLanguageMode(newLang);
    await loadAceMode(mode);

    // 切换到新语言的模式
    codeEditor.value.session.setMode(`ace/mode/${mode}`);
    console.log(`已切换到${newLang}模式`);

    // 获取新语言的代码
    let code = '';
    if (problemForm.solutionCode[newLang] && problemForm.solutionCode[newLang].trim() !== '') {
      // 如果已经有保存的代码，使用保存的代码
      code = problemForm.solutionCode[newLang];
      console.log(`使用已保存的${newLang}代码`);
    } else {
      // 否则使用模板代码
      code = codeTemplates[newLang] || '';
      console.log(`使用${newLang}模板代码`);
    }

    codeEditor.value.setValue(code);
    codeEditor.value.clearSelection();

    // 确保编辑器大小正确
    codeEditor.value.resize(true);
  }
});

// 保存草稿
const saveDraft = () => {
  if (!checkScoreBalance()) {
    return;
  }
  
  // 收集当前语言的代码
  if (codeEditor.value) {
    if (!problemForm.solutionCode) {
      problemForm.solutionCode = {};
    }
    problemForm.solutionCode[activeCodeTab.value] = codeEditor.value.getValue();
  }
  
  // 显示保存中消息
  const loadingMessage = ElMessage({
    message: '正在保存草稿...',
    type: 'info',
    duration: 0
  });
  
  // 模拟保存延迟
  setTimeout(() => {
    try {
      // 保存草稿到本地存储
      const drafts = JSON.parse(localStorage.getItem('problemDrafts') || '[]');
      
      // 生成草稿ID
      const draftId = Date.now().toString();
      
      // 构建草稿对象
      const draft = {
        id: draftId,
        title: problemForm.title || '无标题草稿',
        content: JSON.stringify(problemForm),
        updatedAt: new Date().toISOString()
      };
      
      // 添加到草稿列表
      drafts.push(draft);
      localStorage.setItem('problemDrafts', JSON.stringify(drafts));
      
      // 关闭loading消息
      loadingMessage.close();
      ElMessage.success('草稿已保存');
    } catch (error) {
      loadingMessage.close();
      ElMessage.error('保存草稿失败');
      console.error('保存草稿失败:', error);
    }
  }, 800);
};

// 提交题目
const submitProblem = async () => {
  if (!checkScoreBalance()) {
    return;
  }
  
  // 收集当前编辑器代码
  if (codeEditor.value) {
    if (!problemForm.solutionCode) {
      problemForm.solutionCode = {};
    }
    problemForm.solutionCode[activeCodeTab.value] = codeEditor.value.getValue();
  }
  
  // 清理未修改的代码模板
  const cleanedSolutionCode = {};
  let hasValidSolution = false;
  
  Object.keys(problemForm.solutionCode).forEach(lang => {
    const code = problemForm.solutionCode[lang];
    // 如果代码不为空且不等于模板，则保留
    if (code && code.trim() !== '' && code.trim() !== codeTemplates[lang].trim()) {
      cleanedSolutionCode[lang] = code;
      hasValidSolution = true;
    }
  });
  
  // 检查题解内容
  const hasSolutionText = problemForm.solution && problemForm.solution.trim() !== '';
  
  // 如果既没有文字题解也没有代码题解，询问用户是否需要AI生成
  if (!hasSolutionText && !hasValidSolution) {
    try {
      await ElMessageBox.confirm(
        '您尚未提供题解说明或有效的参考代码。是否需要使用AI自动生成题解？',
        '提示',
        {
          confirmButtonText: '生成题解',
          cancelButtonText: '继续提交',
          type: 'warning'
        }
      );
      
      // 用户选择生成题解
      aiSolutionPrompt.value = `请为题目"${problemForm.title}"生成一个详细的解题思路和C++实现代码`;
      openAiSolutionDrawer();
      await generateSolution();
      return; // 停止提交，等用户确认AI生成的题解
      
    } catch (e) {
      // 用户选择继续提交，不生成题解
      console.log('用户选择继续提交，不生成题解');
    }
  }
  
  if (!problemFormRef.value) return;
  
  problemFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const publicExamples = syncLegacyExampleFields();
        // 准备提交数据
        const problemData = {
          title: problemForm.title,
          difficulty: problemForm.difficulty,
          tags: problemForm.categories.join(','),
          description: problemForm.description,
          inputFormat: problemForm.inputFormat,
          outputFormat: problemForm.outputFormat,
          inputExample: problemForm.inputExample,
          outputExample: problemForm.outputExample,
          examples: JSON.stringify(publicExamples),
          debugInputExample: problemForm.debugInputExample,
          debugOutputExample: problemForm.debugOutputExample,
          hint: problemForm.hint,
          solution: problemForm.solution,
          solutionCode: JSON.stringify(cleanedSolutionCode), // 使用清理后的代码
          timeLimit: problemForm.timeLimit,
          memoryLimit: problemForm.memoryLimit * 1024, // 转为KB
          visibility: problemForm.isPublic ? 'PUBLIC' : 'PRIVATE',
          testCases: JSON.stringify(problemForm.testCases)
        };
        
        // 发送API请求
        const response = await createProblem(problemData);
        
        if (response && (response.code === 200 || response.success === true)) {
          ElMessage.success('题目创建成功！');
          // 跳转回题目列表
          router.push('/problems');
        } else {
          throw new Error(response?.message || '未知错误');
        }
      } catch (error) {
        ElMessage.error({
          message: `创建失败: ${error.message || '未知错误'}`,
          duration: 5000
        });
        console.error('创建题目失败:', error);
      }
    } else {
      ElMessage.error('请完善必填项');
      return false;
    }
  });
};

// 取消
const cancel = () => {
  ElMessageBox.confirm('确定要取消创建？未保存的内容将丢失', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    router.push('/problems');
  }).catch(() => {});
};

// AI 辅助出题相关功能：单题和批量出题共用同一套异步任务、JSON 解析和表单回填流程。
// 优先选择代码/JSON 能力更稳定的模型，减少批量生成时字段缺失或格式跑偏。
const selectPreferredAiModel = (models: Array<{ id: string; name: string; cost: string }>) => {
  const codingJsonModel = models.find((m: any) =>
    m.id?.includes('Qwen3-Coder') || m.name?.includes('Qwen3-Coder')
  );
  if (codingJsonModel) return codingJsonModel.id;

  const deepSeekModel = models.find((m: any) =>
    m.id?.includes('DeepSeek-V3.2') || m.name?.includes('DeepSeek-V3.2')
  );
  if (deepSeekModel) return deepSeekModel.id;

  const minimaxModel = models.find((m: any) =>
    m.id?.includes('MiniMax-M2.5') || m.name?.includes('MiniMax-M2.5')
  );
  if (minimaxModel) return minimaxModel.id;

  const flashModel = models.find((m: any) =>
    m.id?.includes('DeepSeek-V4-Flash') || m.name?.includes('DeepSeek-V4-Flash')
  );
  if (flashModel) return flashModel.id;

  const fastModel = models.find((m: any) =>
    m.id?.includes('Qwen3-8B') || m.name?.includes('Qwen3 8B')
  ) || models.find((m: any) =>
    m.id?.includes('THUDM/GLM-4-9B') || m.name?.includes('GLM-4 9B')
  );
  if (fastModel) return fastModel.id;

  const sorted = [...models].sort((a: any, b: any) => {
    const costA = Number.parseFloat(a.cost || '999');
    const costB = Number.parseFloat(b.cost || '999');
    return costA - costB;
  });
  return sorted[0]?.id || '';
};

// 打开 AI 抽屉时加载可用模型列表，避免页面初始加载被模型接口拖慢。
const loadAiModels = async () => {
  if (aiModels.value.length > 0) return; // 已加载过则跳过
  aiModelsLoading.value = true;
  try {
    const res: any = await aiApi.getModels();
    const data = res.code === 200 ? res.data : res;
    if (Array.isArray(data)) {
      aiModels.value = data;
      selectedAiModel.value = selectPreferredAiModel(data);
    }
  } catch (e) {
    console.error('加载AI模型列表失败:', e);
  } finally {
    aiModelsLoading.value = false;
  }
};

// 打开 AI 出题抽屉；mode=single 是单题，mode=batch 是批量候选题工作台。
const openAiDrawer = (mode: 'single' | 'batch' = 'single') => {
  if (!aiGenerating.value) {
    aiGenerationMode.value = mode;
  }
  aiDrawerVisible.value = true;
  loadAiModels(); // 打开抽屉时加载模型列表
};

// 生成过程中关闭抽屉要二次确认，防止用户误关导致任务状态混乱。
const handleAiDrawerClose = (done) => {
  if (aiGenerating.value) {
    ElMessageBox.confirm('生成过程正在进行中，确定要取消吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      aiGenerating.value = false;
      done();
    }).catch(() => {});
  } else {
    done();
  }
};

// 快捷填入出题提示词：批量模式和单题模式各有不同示例。
const useAiExample = (index) => {
  const batchExamples = [
    '生成一套数组与哈希表练习题，共 4 道，难度从简单到普通，覆盖计数、去重、两数关系和频次统计，每题 2 组公开样例、5 个隐藏评测点。',
    '生成一套动态规划入门到进阶题，共 5 道，依次覆盖一维 DP、路径 DP、背包思想和状态压缩前置训练，每题题面独立、数据范围清晰。',
    '生成一套二分查找专项练习，共 4 道，覆盖有序数组查找、答案二分、边界处理和最小可行值，每题要有不同业务背景。',
    '生成一套字符串处理练习题，共 4 道，覆盖字符统计、子串判断、回文和格式化解析，适合初学者连续训练。'
  ];
  const singleExamples = [
    '创建一道关于动态规划的中等难度题目，要求设计一个计算最长递增子序列长度的算法',
    '生成一道简单的数组题，考察数组元素的查找和统计，适合初学者',
    '设计一道困难的图论算法题，考察最短路径算法的应用',
    '生成一道需要使用哈希表的题目',
    '创建一道考察二分查找的题目'
  ];
  const examples = isAiBatchMode.value ? batchExamples : singleExamples;
  aiPrompt.value = examples[index] || '';
};

const sleep = (ms: number) => new Promise(resolve => window.setTimeout(resolve, ms));

// 当用户选择“自动难度”时，从提示词和当前表单标题中推断目标难度。
const resolveAiDifficulty = () => {
  if (aiDifficultyMode.value !== 'AUTO') return aiDifficultyMode.value;
  const text = `${aiPrompt.value} ${problemForm.title}`.toLowerCase();
  if (/极限|顶级|高阶|挑战|竞赛压轴|hardest|extreme/.test(text)) return '极限';
  if (/困难|难题|进阶|复杂|hard/.test(text)) return '困难';
  if (/普通|中等|中级|medium/.test(text)) return '普通';
  if (/简单|入门|初学|小白|打卡|if|条件判断|easy/.test(text)) return '简单';
  return problemForm.difficulty || '普通';
};

// 给模型提供平台标签范围，约束返回 JSON.tags 必须来自已有标签。
const buildAiTagContext = () => {
  const tags = commonTags.value.join('、');
  return tags
    ? `当前题库可用标签：${tags}。tags 必须从这个列表中按题意多选 1-4 个，不能随意发明；只有题意确实包含图结构、遍历或路径搜索时，才可以使用 BFS/DFS/图。`
    : 'tags 必须使用平台已有标签，按题意多选 1-4 个；如果不确定，优先选择基础标签。';
};

// 给模型提供难度边界说明，减少“简单题却标图论/困难”的情况。
const buildAiDifficultyContext = () => `
难度判断标准：
- 简单：单个知识点，输入输出和条件判断/循环/简单数组/简单数学即可完成，适合初学者打卡。
- 普通：需要组合 1-2 个常见技巧，例如排序、哈希、前缀和、双指针、基础 DP、树的简单遍历。
- 困难：需要较强算法设计或边界处理，例如图论最短路、复杂 DP、回溯剪枝、数据结构综合。
- 极限：接近竞赛压轴，状态设计复杂或需要高级优化。
标签必须服从题意，不能因为题目很短就误标成 BFS/DFS/图。`;

const hasTag = (name: string) => commonTags.value.includes(name);

// 清洗 AI 返回标签：只保留平台已有标签，并根据题意做基础兜底。
const normalizeAiTags = (rawTags: string[], text: string) => {
  const normalizedText = (text || '').toLowerCase();
  let tags = rawTags
    .map(tag => String(tag || '').trim())
    .filter(tag => tag && commonTags.value.includes(tag));

  const looksLikeBasicMath = /奇偶|素数|阶乘|最大公约数|最小公倍数|温度|年份|日期|数学|算术|取模|if|条件判断|打卡/.test(normalizedText);
  const looksLikeGraph = /图|最短路|路径|连通|拓扑|bfs|dfs|广度|深度|队列搜索|迷宫/.test(normalizedText);

  if (looksLikeBasicMath && !looksLikeGraph) {
    tags = tags.filter(tag => !['BFS', 'DFS', '图', '拓扑排序', '并查集'].includes(tag));
    if (hasTag('数学') && !tags.includes('数学')) tags.unshift('数学');
    if (hasTag('模拟') && !tags.includes('模拟')) tags.push('模拟');
  }

  if (tags.length === 0) {
    if (looksLikeBasicMath && hasTag('数学')) tags.push('数学');
    else if (/数组|序列|下标/.test(normalizedText) && hasTag('数组')) tags.push('数组');
    else if (/字符串|字符|回文/.test(normalizedText) && hasTag('字符串')) tags.push('字符串');
    else if (hasTag('模拟')) tags.push('模拟');
  }

  return Array.from(new Set(tags)).slice(0, 4);
};

// 轮询 AI 出题异步任务：后端负责调用大模型，前端只显示阶段和最终候选题。
const waitForAiProblemGeneration = async (jobId: string) => {
  const startedAt = Date.now();
  let delay = 1500;
  const phaseStepMap: Record<string, number> = {
    QUEUED: 1,
    PREPARING: 2,
    CALLING_MODEL: 3,
    NORMALIZING: 4,
    COMPLETED: 5
  };

  while (Date.now() - startedAt < 300000) {
    if (!aiGenerating.value) {
      throw new Error('AI生成已取消');
    }
    await sleep(delay);
    const res: any = await aiApi.getProblemGenerationJob(jobId);
    const job = res.code === 200 ? res.data : res;
    if (!job || !job.status) {
      throw new Error('AI出题任务状态异常');
    }
    const phase = String(job.phase || '');
    aiGeneratingStep.value = phaseStepMap[phase] || (job.status === 'RUNNING' ? 3 : 1);
    aiGenerationStatusText.value = job.message || (job.status === 'RUNNING'
      ? '模型仍在生成题面、样例和评测点'
      : '后端已接收任务，等待模型开始返回');
    if (job.status === 'SUCCEEDED') {
      aiGeneratingStep.value = 5;
      aiGenerationStatusText.value = job.message || '模型已返回结果，正在整理预览';
      return job.result || [];
    }
    if (job.status === 'FAILED') {
      throw new Error(job.error || 'AI生成失败，请稍后重试');
    }
    delay = Math.min(3000, delay + 300);
  }

  throw new Error('AI生成超时，请稍后查看或重试');
};

// 发起 AI 出题：把提示词、标签、难度、公开样例数、隐藏评测点数和目标题数传给后端。
const runAiProblemGeneration = async (options: {
  promptText?: string;
  count?: number;
  replaceIndex?: number;
  successMessage?: string;
} = {}) => {
  const basePrompt = (options.promptText ?? aiPrompt.value).trim();
  if (!basePrompt) {
    ElMessage.warning('请输入提示');
    return;
  }

  const isReplacingOne = typeof options.replaceIndex === 'number';
  if (!isReplacingOne) {
    aiResponse.value = null;
    aiCandidateProblems.value = [];
    activeAiCandidateIndex.value = 0;
  }
  aiGenerationErrorText.value = '';
  aiGenerating.value = true;
  aiGeneratingStep.value = 1;
  aiGenerationStartedAt.value = Date.now();
  aiGenerationElapsedSeconds.value = 0;
  aiGenerationStatusText.value = '正在创建后端异步任务';

  const stepInterval = window.setInterval(() => {
    aiGenerationElapsedSeconds.value = Math.max(0, Math.floor((Date.now() - aiGenerationStartedAt.value) / 1000));
  }, 1000);

  const difficultyMap: Record<string, string> = { '简单': '简单', '普通': '普通', '困难': '困难', '极限': '极限' };
  const reverseDifficultyMap: Record<string, string> = {
    'EASY': '简单', 'MEDIUM': '普通', 'HARD': '困难', 'EXTREME': '极限',
    '简单': '简单', '普通': '普通', '困难': '困难', '极限': '极限'
  };

  try {
    const targetDifficulty = resolveAiDifficulty();
    const requestTags = commonTags.value.length > 0 ? commonTags.value : normalizeAiTags([], aiPrompt.value);
  // 前端只负责创建异步任务和轮询结果，真正的模型调用、JSON 校验和字段归一化都在后端完成。
  const targetCount = Math.max(1, Math.min(6, options.count ?? effectiveAiBatchCount.value));
    const requestPayload = {
      tags: requestTags.length > 0 ? requestTags.join(',') : '模拟',
      difficulty: difficultyMap[targetDifficulty] || targetDifficulty,
      language: 'C++',
      model: selectedAiModel.value || undefined,
      prompt: basePrompt,
      exampleCount: aiExampleCount.value,
      testCaseCount: Math.min(aiTestCaseCount.value, totalScoreValue.value),
      totalScore: totalScoreValue.value,
      count: targetCount
    };

    const jobRes: any = await aiApi.startProblemGeneration(requestPayload);
    const job = jobRes.code === 200 ? jobRes.data : jobRes;
    if (!job?.jobId) {
      throw new Error('AI出题任务创建失败');
    }
    aiGeneratingStep.value = 2;
    aiGenerationStatusText.value = '任务已创建，正在等待模型返回';

    const data = await waitForAiProblemGeneration(job.jobId);

    // 验证返回数据有效性，AI 必须返回对象或数组，不能把普通文本当题目用。
    if (!data || (typeof data !== 'object' && !Array.isArray(data))) {
      ElMessage.error('AI返回的数据格式异常，请调整提示词后重试');
      return;
    }

    // 后端单题返回对象、批量返回数组；这里统一转为候选题列表供预览和人工确认。
    const rawProblems = Array.isArray(data) ? data : [data];
    const candidates = rawProblems
      .slice(0, targetCount)
      .map((item: any) => normalizeAiProblemCandidate(item, reverseDifficultyMap, targetDifficulty))
      .filter((item: any) => item.title || item.description);

    if (candidates.length === 0) {
      ElMessage.error('AI返回的题目内容为空，请尝试更具体的提示词');
      return;
    }

    aiGeneratingStep.value = 5;
    if (isReplacingOne) {
      const replaceIndex = options.replaceIndex as number;
      aiCandidateProblems.value.splice(replaceIndex, 1, candidates[0]);
      selectAiCandidate(Math.min(replaceIndex, aiCandidateProblems.value.length - 1));
      ElMessage.success(options.successMessage || `第 ${replaceIndex + 1} 题已重新生成`);
    } else {
      aiCandidateProblems.value = candidates;
      selectAiCandidate(0);
      ElMessage.success(isAiBatchMode.value ? `已生成 ${candidates.length} 道题目草稿` : '题目生成成功');
    }
  } catch (e: any) {
    console.error('AI题目生成失败:', e);
    const msg = e.response?.data?.message || e.message || '';
    if (msg.includes('积分') || msg.includes('余额')) {
      aiGenerationErrorText.value = 'AI积分不足，无法生成题目';
      ElMessage.error('AI积分不足，无法生成题目');
    } else if (msg.includes('超时') || msg.includes('timeout')) {
      aiGenerationErrorText.value = 'AI生成超时，请稍后重试';
      ElMessage.error('AI生成超时，请稍后重试');
    } else {
      aiGenerationErrorText.value = 'AI生成失败: ' + (msg || '请稍后重试');
      ElMessage.error(aiGenerationErrorText.value);
    }
  } finally {
    window.clearInterval(stepInterval);
    aiGenerating.value = false;
  }
};

const generateProblem = async () => {
  await runAiProblemGeneration();
};

// 归一化 AI 返回的单道题：兼容 examples/testCases/solutionCode 等不同模型字段写法。
const normalizeAiProblemCandidate = (data: any, reverseDifficultyMap: Record<string, string>, targetDifficulty?: string) => {
  const tags = typeof data.tags === 'string'
    ? data.tags.split(/[,，、]/).map((t: string) => t.trim()).filter(Boolean)
    : (Array.isArray(data.tags) ? data.tags : []);
  const textForInference = `${aiPrompt.value} ${data.title || ''} ${data.description || ''}`;
  const validTags = normalizeAiTags(tags, textForInference);

  // 隐藏评测点只进入测试用例表单，不会当作题面公开样例展示。
  const testCases = Array.isArray(data.testCases)
    ? data.testCases.map((tc: any) => ({
        input: tc.input || '',
        output: tc.expectedOutput || tc.output || '',
        score: tc.score || 5
      }))
    : [];
  // 公开样例支持多组，后续会同步到题面展示和做题 IDE 的样例切换按钮。
  const examples = Array.isArray(data.examples)
    ? data.examples.map((example: any) => ({
        input: example.input || '',
        output: example.output || example.expectedOutput || '',
        explanation: example.explanation || ''
      })).filter((example: any) => example.input || example.output)
    : [];
  if (examples.length === 0 && (data.inputExample || data.outputExample)) {
    examples.push({
      input: data.inputExample || '',
      output: data.outputExample || '',
      explanation: ''
    });
  }

  const candidate = {
    title: data.title || '未命名题目',
    difficulty: reverseDifficultyMap[data.difficulty] || data.difficulty || targetDifficulty || '普通',
    tags: validTags.length > 0 ? validTags : (commonTags.value.length > 0 ? [commonTags.value[0]] : []),
    description: cleanAiGeneratedHtml(data.description || ''),
    inputFormat: cleanAiGeneratedHtml(data.inputFormat || ''),
    outputFormat: cleanAiGeneratedHtml(data.outputFormat || ''),
    examples,
    inputExample: data.inputExample || (examples.length > 0 ? examples[0].input : ''),
    outputExample: data.outputExample || (examples.length > 0 ? examples[0].output : ''),
    debugInputExample: data.debugInputExample || (examples.length > 1 ? examples[1].input : (data.inputExample || '')),
    debugOutputExample: data.debugOutputExample || (examples.length > 1 ? examples[1].output : (data.outputExample || '')),
    hint: cleanAiGeneratedHtml(data.hint || ''),
    solution: cleanAiGeneratedHtml(data.solution || ''),
    solutionCode: data.solutionCode || {},
    timeLimit: data.timeLimit || 1000,
    memoryLimit: data.memoryLimit || 262144,
    testCases
  };
  return candidate;
};

// 切换批量候选题预览，右侧表单预览区域跟着变化。
const selectAiCandidate = (index: number) => {
  if (!aiCandidateProblems.value[index]) return;
  activeAiCandidateIndex.value = index;
  aiResponse.value = aiCandidateProblems.value[index];
};

const regenerateProblem = () => {
  generateProblem();
};

// 将 HTML 内容转成纯文本，用于检测 AI 是否把提示词泄漏进题面。
const stripHtml = (html: string) => {
  if (!html) return '';
  return html
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
};

// 检测 prompt 泄漏关键词：如果模型把“请返回 JSON”等约束写进题面，就清理掉。
const promptLeakageMarkers = [
  '出题方向：',
  '用户出题方向',
  '总体出题方向',
  '平台标签约束',
  '当前题库可用标签',
  '难度判断标准',
  '本次目标难度',
  '本次是批量出题',
  '本次是单题模式',
  '批量出题模式',
  'tags 必须',
  '字段必须包含',
  '请返回 JSON',
  '只返回 JSON',
  'JSON.tags',
  '公开样例数量',
  '隐藏评测点数量',
  '隐藏评测总分'
];

const hasPromptLeakage = (text: string) => {
  const normalized = String(text || '').toLowerCase();
  return promptLeakageMarkers.some(marker => normalized.includes(marker.toLowerCase()));
};

// 清理 AI 返回的题面、输入输出格式和提示，过滤掉提示词泄漏段落。
const cleanAiGeneratedHtml = (html: string) => {
  if (!html) return '';
  let cleaned = String(html).replace(/<p[^>]*>[\s\S]*?<\/p>/gi, paragraph => (
    hasPromptLeakage(stripHtml(paragraph)) ? '' : paragraph
  ));
  const lines = cleaned.split(/\r?\n/).filter(line => !hasPromptLeakage(stripHtml(line)));
  cleaned = lines.join('\n').trim();
  return hasPromptLeakage(stripHtml(cleaned)) ? '' : cleaned;
};

// 批量候选题中只重生成某一题，便于老师微调整套练习中的单个题目。
const regenerateBatchProblem = async (index: number) => {
  const current = aiCandidateProblems.value[index];
  if (!current) return;

  let extraPrompt = '';
  try {
    const result: any = await ElMessageBox.prompt(
      '可以补充这道题要怎么改，例如“换成字符串考点”“降低难度”“保留题材但增加边界情况”。也可以留空直接重生成。',
      `重生成第 ${index + 1} 题`,
      {
        confirmButtonText: '重生成',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '补充要求，可留空'
      }
    );
    extraPrompt = String(result?.value || '').trim();
  } catch {
    return;
  }

  const promptText = [
    aiPrompt.value.trim(),
    '',
    `当前要替换第 ${index + 1} 题：${current.title || '未命名题目'}。`,
    `原题标签：${(current.tags || []).join('、') || '未标注'}，原题难度：${current.difficulty || '普通'}。`,
    extraPrompt ? `重生成要求：${extraPrompt}` : '请在同一练习套方向下重新设计这道题，避免与其它题目重复。'
  ].join('\n');

  await runAiProblemGeneration({
    promptText,
    count: 1,
    replaceIndex: index,
    successMessage: `第 ${index + 1} 题已重新生成`
  });
};

// 从批量候选列表中移除一道题，不影响其它候选题。
const removeBatchProblem = (index: number) => {
  aiCandidateProblems.value.splice(index, 1);
  if (aiCandidateProblems.value.length === 0) {
    aiResponse.value = null;
    activeAiCandidateIndex.value = 0;
    return;
  }
  selectAiCandidate(Math.min(index, aiCandidateProblems.value.length - 1));
};

// 把当前 AI 候选题应用到创建表单；仍需要人工检查后再提交入库。
const applyAiProblem = () => {
  if (!aiResponse.value) return;
  
  const problem = aiResponse.value;
  
  // 填充表单数据
  problemForm.title = problem.title;
  problemForm.difficulty = problem.difficulty;
  
  // 检查标签是否存在于数据库中的标签列表，防止 AI 生成平台没有的标签。
  const validTags = [];
  if (problem.tags && Array.isArray(problem.tags)) {
    problem.tags.forEach(tag => {
      // 只添加数据库中存在的标签
      if (commonTags.value.includes(tag)) {
        validTags.push(tag);
      }
    });
  }
  problemForm.categories = validTags;
  
  problemForm.description = problem.description;
  problemForm.inputFormat = problem.inputFormat;
  problemForm.outputFormat = problem.outputFormat;
  problemForm.examples = normalizePublicExamples(problem.examples || [{
    input: problem.inputExample,
    output: problem.outputExample,
    explanation: ''
  }]);
  syncLegacyExampleFields();
  problemForm.hint = problem.hint;
  problemForm.solution = problem.solution || problemForm.solution;
  problemForm.timeLimit = problem.timeLimit || problemForm.timeLimit;
  problemForm.memoryLimit = problem.memoryLimit ? Math.round(problem.memoryLimit / 1024) : problemForm.memoryLimit;

  if (problem.solutionCode) {
    ['cpp', 'java', 'python'].forEach(lang => {
      if (problem.solutionCode[lang]) {
        problemForm.solutionCode[lang] = problem.solutionCode[lang];
      }
    });
    if (codeEditor.value && problemForm.solutionCode[activeCodeTab.value]) {
      codeEditor.value.setValue(problemForm.solutionCode[activeCodeTab.value]);
      codeEditor.value.clearSelection();
    }
  }
  
  // 清空测试用例并添加 AI 返回的隐藏评测点。
  problemForm.testCases = [];
  
  if (!problem.testCases || problem.testCases.length === 0) {
    problemForm.testCases = [{ input: '', output: '', score: totalScoreValue.value }];
    ElMessage.warning('AI未返回隐藏评测点，请手动补充测试用例');
    aiDrawerVisible.value = false;
    return;
  }

  // 计算每个测试用例的分值
  const testCaseCount = problem.testCases.length;
  const scorePerCase = Math.floor(totalScoreValue.value / testCaseCount);
  let remainingScore = totalScoreValue.value - (scorePerCase * testCaseCount);
  
  problem.testCases.forEach((testCase, index) => {
    // 为最后一个测试用例分配剩余分数
    const adjustedScore = index === testCaseCount - 1 
      ? scorePerCase + remainingScore 
      : scorePerCase;
      
    problemForm.testCases.push({
      input: testCase.input || '',
      output: testCase.output,
      score: adjustedScore
    });
  });
  
  // 关闭抽屉
  aiDrawerVisible.value = false;
  
  ElMessage.success('已应用AI生成的题目');
};

// 批量保存时把候选题转换成后端创建题目的 payload，默认保存为私有题。
const buildAiProblemPayload = (problem: any) => {
  const examples = normalizePublicExamples(problem.examples || [{
    input: problem.inputExample,
    output: problem.outputExample,
    explanation: ''
  }]);
  const first = examples[0] || { input: '', output: '' };
  const second = examples[1] || first;
  return {
    title: problem.title,
    difficulty: problem.difficulty,
    tags: (problem.tags || []).join(','),
    description: problem.description,
    inputFormat: problem.inputFormat,
    outputFormat: problem.outputFormat,
    inputExample: first.input,
    outputExample: first.output,
    examples: JSON.stringify(examples),
    debugInputExample: second.input,
    debugOutputExample: second.output,
    hint: problem.hint,
    solution: problem.solution,
    solutionCode: JSON.stringify(problem.solutionCode || {}),
    timeLimit: problem.timeLimit || 1000,
    memoryLimit: problem.memoryLimit || 262144,
    visibility: 'PRIVATE',
    testCases: JSON.stringify((problem.testCases || []).map((testCase: any) => ({
      input: testCase.input || '',
      output: testCase.output || testCase.expectedOutput || '',
      score: testCase.score || 1
    })))
  };
};

// 批量创建 AI 候选题：逐题调用创建接口，成功的先落库，失败的汇总提示。
const saveBatchAiProblems = async () => {
  const selected = aiCandidateProblems.value.filter(Boolean);
  if (selected.length === 0) {
    ElMessage.warning('请先生成要创建的题目');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定批量创建这 ${selected.length} 道 AI 生成题目吗？题目会先保存为私有题，后续可以逐题编辑、测试和公开。`,
      '批量创建确认',
      {
        confirmButtonText: '批量创建',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
  } catch {
    return;
  }

  aiBatchSaving.value = true;
  let success = 0;
  const failed: string[] = [];
  try {
    for (const problem of selected) {
      try {
        const response: any = await createProblem(buildAiProblemPayload(problem));
        if (response && (response.code === 200 || response.success === true || response.problemId)) {
          success += 1;
        } else {
          failed.push(problem.title || '未命名题目');
        }
      } catch (error) {
        console.error('保存AI题目失败:', problem.title, error);
        failed.push(problem.title || '未命名题目');
      }
    }

    if (success > 0 && failed.length === 0) {
      ElMessage.success(`已批量创建 ${success} 道私有题`);
      aiDrawerVisible.value = false;
      router.push('/problems');
    } else if (success > 0) {
      ElMessage.warning(`已创建 ${success} 道，${failed.length} 道创建失败`);
    } else {
      ElMessage.error('题目均创建失败，请稍后重试');
    }
  } finally {
    aiBatchSaving.value = false;
  }
};

// 总分值计算
const totalScore = computed(() => {
  return problemForm.testCases.reduce((sum, testCase) => sum + (testCase.score || 0), 0);
});

// 总分值设置
const totalScoreValue = ref(20); // 默认总分值为20
const isScoreBalanceError = computed(() => {
  return totalScore.value !== totalScoreValue.value;
});

// 监听总分值变化，确保不超过100
watch(totalScoreValue, (newVal) => {
  if (newVal > 100) {
    totalScoreValue.value = 100;
    ElMessage.warning('总分值不能超过100分');
  }
  if (aiTestCaseCount.value > newVal) {
    aiTestCaseCount.value = Math.max(1, newVal);
  }
});

watch(aiGenerationMode, (mode) => {
  if (mode === 'batch' && aiBatchCount.value < 2) {
    aiBatchCount.value = 2;
  }
});

// 检查总分值是否平衡
const checkScoreBalance = () => {
  if (totalScore.value !== totalScoreValue.value) {
    ElMessage.error(`测试用例总分值(${totalScore.value})必须等于设定总分值(${totalScoreValue.value})`);
    return false;
  }
  return true;
};

// 添加测试用例
const addTestCase = () => {
  const defaultScore = Math.max(1, Math.min(10, totalScoreValue.value));
  problemForm.testCases.push({ input: '', output: '', score: defaultScore });
};

// 删除测试用例
const removeTestCase = (index) => {
  if (problemForm.testCases.length > 1) {
    problemForm.testCases.splice(index, 1);
  }
};

// AI辅助生成题解相关
const openAiSolutionDrawer = () => {
  aiSolutionDrawerVisible.value = true;
};

const handleAiSolutionDrawerClose = (done) => {
  if (aiSolutionGenerating.value) {
    ElMessageBox.confirm('生成过程正在进行中，确定要取消吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      aiSolutionGenerating.value = false;
      done();
    }).catch(() => {});
  } else {
    done();
  }
};

const useAiSolutionExample = (index) => {
  const examples = [
    '生成一个简洁清晰的解题思路，包含时空复杂度分析',
    '提供多种解法对比和C++代码实现',
    '生成详细的步骤分解和Python代码',
    '请解释该问题的最优算法并提供代码示例',
    '生成初学者容易理解的解题思路和代码'
  ];
  aiSolutionPrompt.value = examples[index];
};

const generateSolution = async () => {
  if (!aiSolutionPrompt.value.trim()) {
    ElMessage.warning('请输入题解提示');
    return;
  }

  aiSolutionGenerating.value = true;
  aiSolutionGeneratingStep.value = 1;

  const stepInterval = window.setInterval(() => {
    aiSolutionGeneratingStep.value = aiSolutionGeneratingStep.value >= 5 ? 5 : aiSolutionGeneratingStep.value + 1;
  }, 1500);

  try {
    const res: any = await aiApi.generateSolutionDraft({
      title: problemForm.title,
      difficulty: problemForm.difficulty,
      description: problemForm.description,
      inputFormat: problemForm.inputFormat,
      outputFormat: problemForm.outputFormat,
      prompt: aiSolutionPrompt.value
    });
    const response = res.code === 200 ? res.data : res;

    aiSolutionGeneratingStep.value = 5;
    aiSolutionResponse.value = {
      thinking: response.solution || '',
      codes: {
        'C++': response.code?.cpp || '',
        'Java': response.code?.java || '',
        'Python': response.code?.python || ''
      }
    };

    activeSolutionLang.value = 'C++';
    ElMessage.success('题解生成成功');
  } catch (error) {
    console.error('生成题解失败:', error);
    ElMessage.error('生成题解失败，请稍后重试');
  } finally {
    window.clearInterval(stepInterval);
    aiSolutionGenerating.value = false;
  }
};

const regenerateSolution = () => {
  generateSolution();
};

const applyAiSolution = () => {
  if (!aiSolutionResponse.value) return;
  
  // 应用解题思路
  problemForm.solution = aiSolutionResponse.value.thinking;
  
  // 应用代码 - 优先处理C++代码
  if (aiSolutionResponse.value.codes['C++']) {
    problemForm.solutionCode.cpp = aiSolutionResponse.value.codes['C++'];
    
    // 如果当前是C++编辑器，直接更新显示
    if (activeCodeTab.value === 'cpp' && codeEditor.value) {
      codeEditor.value.setValue(aiSolutionResponse.value.codes['C++']);
      codeEditor.value.clearSelection();
    }
  }
  
  if (aiSolutionResponse.value.codes['Java']) {
    problemForm.solutionCode.java = aiSolutionResponse.value.codes['Java'];
    
    // 如果当前是Java编辑器，直接更新显示
    if (activeCodeTab.value === 'java' && codeEditor.value) {
      codeEditor.value.setValue(aiSolutionResponse.value.codes['Java']);
      codeEditor.value.clearSelection();
    }
  }
  
  if (aiSolutionResponse.value.codes['Python']) {
    problemForm.solutionCode.python = aiSolutionResponse.value.codes['Python'];
    
    // 如果当前是Python编辑器，直接更新显示
    if (activeCodeTab.value === 'python' && codeEditor.value) {
      codeEditor.value.setValue(aiSolutionResponse.value.codes['Python']);
      codeEditor.value.clearSelection();
    }
  }
  
  // 关闭抽屉
  aiSolutionDrawerVisible.value = false;
  
  ElMessage.success('已应用AI生成的题解');
};

// 复制题解代码
const copySolutionCode = (code) => {
  navigator.clipboard.writeText(code)
    .then(() => {
      ElMessage.success('代码已复制到剪贴板');
    })
    .catch(err => {
      ElMessage.error('复制失败，请手动复制');
      console.error('复制代码失败:', err);
    });
};

// 获取语言选项标签，显示哪些语言已有代码
const getLanguageOptionLabel = (lang) => {
  const hasCode = problemForm.solutionCode && 
                  problemForm.solutionCode[lang.id] && 
                  problemForm.solutionCode[lang.id].trim() !== '' &&
                  problemForm.solutionCode[lang.id].trim() !== codeTemplates[lang.id].trim();
  
  return hasCode ? `${lang.name} ✓` : lang.name;
};
</script>

<style scoped>
.problem-create {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.form-card {
  margin-bottom: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
}

.card-header h2 {
  margin: 0;
  font-size: 1.8rem;
  color: #303133;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.problem-form {
  margin-top: 20px;
}

.editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  min-height: 200px;
  overflow: hidden;
}

.code-editors-container {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 15px;
  background-color: #fafafa;
  width: 100%;
}

.language-selector {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.language-selector span {
  font-weight: bold;
}

.ace-editor-container {
  width: 100%;
  height: 450px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  position: relative;
  background-color: #fafafa;
}

.test-cases-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.total-score-setting {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 15px;
  background-color: #f0f9eb;
  border-radius: 4px;
  border: 1px solid #e1f3d8;
}

.total-score-input {
  display: flex;
  align-items: center;
  gap: 10px;
}

.test-case-item {
  margin-bottom: 20px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background-color: #f9f9f9;
  transition: all 0.3s;
}

.test-case-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.examples-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.public-example-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fbfcff;
}

.public-example-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #303133;
  font-weight: 600;
}

.test-case-controls {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  height: 100%;
  padding-top: 30px;
}

.add-test-case {
  margin: 20px 0;
  display: flex;
  justify-content: center;
}

.solution-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 10px 15px;
  background-color: #f4f4f5;
  border-radius: 8px;
}

.ai-problem-drawer :deep(.el-drawer__body) {
  padding: 0;
  overflow: hidden;
}

.ai-batch-drawer :deep(.el-drawer) {
  max-width: 100vw;
}

.ai-drawer-content {
  height: 100%;
  min-height: 0;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  background: #ffffff;
}

.ai-drawer-content.batch-workbench {
  padding: 18px 28px;
}

.ai-mode-tabs {
  flex-shrink: 0;
}

.ai-mode-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.ai-mode-note {
  margin-top: -4px;
  padding: 8px 10px;
  border: 1px solid #e4ebf5;
  border-radius: 8px;
  background: #f8fbff;
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}

.ai-prompt-section {
  flex-shrink: 0;
  margin-bottom: 0;
}

.ai-prompt-section h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #1f2937;
}

.ai-examples {
  margin-top: 10px;
}

.ai-examples p {
  margin: 0 0 8px;
  color: #4b5563;
}

.ai-generation-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin: 12px 0 4px;
  padding: 10px;
  border: 1px solid #e4ebf5;
  border-radius: 8px;
  background: #f8fbff;
}

.ai-generation-options :deep(.el-form-item) {
  margin-bottom: 0;
}

.ai-example-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.example-tag {
  margin: 0;
  cursor: pointer;
  transition: all 0.3s;
}

.example-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.ai-actions {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ai-model-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-model-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.ai-action-buttons {
  display: flex;
  gap: 10px;
}

.ai-response-section {
  flex: 1;
  min-height: 0;
  margin-top: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.ai-response-content {
  background-color: #f9fbff;
  padding: 12px;
  border-radius: 8px;
}

.batch-workbench .ai-response-content {
  display: grid;
  grid-template-columns: minmax(320px, 0.42fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.ai-batch-panel {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #d9e7ff;
  border-radius: 8px;
  background: #f7fbff;
}

.ai-batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.ai-batch-header div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.ai-batch-header strong {
  color: #1f2937;
}

.ai-batch-header span {
  color: #64748b;
  font-size: 13px;
}

.ai-batch-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 10px;
}

.ai-batch-item {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.ai-batch-item.active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.14);
}

.ai-batch-index {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1d4ed8;
  background: #eff6ff;
  font-weight: 700;
}

.ai-batch-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ai-batch-meta strong,
.ai-batch-meta span,
.ai-batch-meta p,
.ai-batch-meta small {
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-batch-meta strong,
.ai-batch-meta span {
  white-space: nowrap;
}

.ai-batch-meta span,
.ai-batch-meta small {
  color: #64748b;
  font-size: 12px;
}

.ai-batch-meta p {
  margin: 2px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.ai-batch-actions {
  grid-column: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.ai-candidate-panel {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #d9e7ff;
  border-radius: 8px;
  background: #f7fbff;
}

.ai-candidate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: #303133;
  font-weight: 600;
}

.ai-candidate-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.ai-candidate-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.ai-candidate-item.active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.14);
}

.ai-candidate-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.ai-candidate-meta strong,
.ai-candidate-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-candidate-meta span {
  color: #606266;
  font-size: 12px;
}

.ai-result-card {
  margin-bottom: 20px;
  min-width: 0;
}

.ai-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ai-result-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-result-description {
  margin-bottom: 15px;
}

.io-format {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.examples-section {
  margin-top: 15px;
}

.example {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.example pre {
  background-color: #f8f8f8;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 10px 0;
}

.example-explanation {
  color: #606266;
  font-size: 13px;
  margin-top: 8px;
}

.hint {
  background-color: #f0f9eb;
  padding: 15px;
  border-radius: 4px;
}

.test-cases {
  margin-top: 15px;
}

.test-case {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.test-case-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.test-case-input pre, .test-case-output pre {
  background-color: #f8f8f8;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 10px 0;
}

.ai-response-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.batch-workbench .ai-response-actions {
  grid-column: 1 / -1;
}

@media (max-width: 1080px) {
  .ai-drawer-content.batch-workbench {
    padding: 16px;
  }

  .batch-workbench .ai-response-content {
    display: block;
  }
}

@media (max-width: 720px) {
  .ai-actions,
  .ai-model-selector,
  .ai-action-buttons {
    width: 100%;
  }

  .ai-action-buttons {
    justify-content: flex-end;
  }

  .io-format,
  .test-case-content {
    grid-template-columns: 1fr;
  }
}

.code-preview {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  white-space: pre-wrap;
  font-family: monospace;
  max-height: 350px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.5;
  border: 1px solid #dcdfe6;
}

.solution-thinking {
  margin-bottom: 20px;
}

.tabs-container {
  margin-top: 20px;
}

.full-width {
  width: 100%;
}

.ai-generating-animation {
  flex: 1;
  min-height: 220px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  overflow-y: auto;
  padding: 12px 0;
}

.ai-generation-error {
  margin: 12px 0;
}

.ai-loading-container {
  width: min(640px, 100%);
  text-align: left;
  padding: 18px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #f8fbff;
}

.ai-loading-icon {
  display: inline-flex;
  font-size: 24px;
  color: #409eff;
  margin-bottom: 8px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    opacity: 0.6;
    transform: scale(0.9);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
  100% {
    opacity: 0.6;
    transform: scale(0.9);
  }
}

.ai-loading-text {
  font-size: 14px;
  color: #606266;
}

.ai-loading-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 6px;
  color: #303133;
}

.ai-loading-subtitle {
  margin-bottom: 12px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.ai-loading-status {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 5px 8px;
  border-radius: 6px;
  background: #edf5ff;
  color: #1d4ed8;
  font-size: 13px;
}

.ai-loading-steps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.ai-step {
  min-height: 42px;
  padding: 9px 12px 9px 26px;
  border-radius: 8px;
  background-color: #ffffff;
  border: 1px solid #e5edf7;
  width: 100%;
  text-align: left;
  transition: all 0.3s;
  position: relative;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.ai-step:before {
  content: "";
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #dcdfe6;
}

.ai-step.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.ai-step.active:before {
  background-color: #409eff;
}

.solution-code-container {
  margin-top: 20px;
}

.solution-language-selector {
  margin-bottom: 15px;
}

.code-preview-container {
  position: relative;
}

.copy-code-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  opacity: 0.7;
  transition: opacity 0.3s;
}

.copy-code-btn:hover {
  opacity: 1;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .io-format,
  .test-case-content,
  .ai-generation-options,
  .ai-loading-steps {
    grid-template-columns: 1fr;
  }

  .ai-actions,
  .ai-model-selector,
  .ai-action-buttons {
    align-items: stretch;
    width: 100%;
  }

  .ai-actions,
  .ai-model-selector {
    flex-direction: column;
  }
  
  .card-header {
    flex-direction: column;
    gap: 15px;
  }
  
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
}

.form-section-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.form-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-section-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #303133;
  font-weight: 600;
}

.subsection-title {
  font-size: 1rem;
  color: #606266;
  margin: 20px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  gap: 10px;
}

.privacy-tip {
  width: 100%;
  margin-top: 6px;
  color: #7a8b9a;
  font-size: 12px;
  line-height: 1.5;
}

.typing-cursor {
  margin-left: 10px;
  font-style: italic;
  color: #409eff;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.typing-token {
  opacity: 0;
  animation: fadeIn 0.3s forwards;
  margin-right: 2px;
  font-weight: 600;
}

.cursor {
  animation: blink 1s infinite;
  font-weight: bold;
  margin-left: 2px;
}

@keyframes fadeIn {
  0% { opacity: 0; }
  100% { opacity: 1; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.code-editor-wrapper {
  width: 100%;
  height: 450px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  position: relative;
  background-color: #ffffff;
}

.code-editor {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  font-size: 14px;
}

.code-notice {
  margin-bottom: 15px;
}

.help-icon {
  color: #909399;
  cursor: pointer;
  font-size: 16px;
  margin-left: 5px;
}

.help-icon:hover {
  color: #409EFF;
}
</style>
