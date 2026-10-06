<!-- 编辑题目页面 -->
<template>
  <div class="problem-edit">
    <el-card class="form-card" shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
    <h2>编辑题目</h2>
          <div class="header-actions">
            <el-button type="success" @click="updateProblem">保存更改</el-button>
          </div>
        </div>
      </template>

      <el-form ref="problemFormRef" :model="problemForm" :rules="rules" label-width="120px" label-position="top" class="problem-form">
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
        
        <el-form-item label="题目标签" prop="tags">
          <el-select
            v-model="problemForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或创建标签"
            style="width: 100%"
          >
            <el-option
              v-for="tag in commonTags"
              :key="typeof tag === 'object' ? tag.id : tag"
              :label="typeof tag === 'object' ? tag.name : tag"
              :value="typeof tag === 'object' ? tag.name : tag"
            />
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
              <div class="total-score">
                <span>总分值: {{ totalScoreValue }}</span>
                <el-input-number 
                  v-model="totalScoreValue" 
                  :min="1" 
                  :max="100" 
                  :step="1" 
                  size="small"
                />
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
            <el-button type="primary" @click="updateProblem">保存更改</el-button>
          </div>
        </el-card>
      </el-form>
    </el-card>

    <!-- AI辅助生成题解抽屉 -->
    <el-drawer
      v-model="aiSolutionDrawerVisible"
      title="AI辅助生成题解"
      direction="rtl"
      size="60%"
    >
      <div class="ai-drawer-content">
        <el-form>
          <el-form-item label="AI提示">
                    <el-input 
              v-model="aiPrompt"
                      type="textarea" 
              :rows="4"
              placeholder="输入提示，例如：'请为这道最长递增子序列问题生成一个详细的解题思路和Java实现代码'"
            ></el-input>
                  </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              @click="generateSolution" 
              :loading="aiLoading"
              :disabled="!aiPrompt || aiPrompt.length < 10"
            >
              生成题解
            </el-button>
                  </el-form-item>
        </el-form>
        
        <div v-if="aiLoading" class="ai-generating-animation">
          <div class="ai-loading-container">
            <div class="ai-loading-icon">
              <el-icon><Loading /></el-icon>
                </div>
            <h3 class="ai-loading-title">AI正在生成题解...</h3>
            <p class="ai-loading-text">这可能需要几秒钟时间，请耐心等待</p>
            
            <div class="ai-loading-steps">
              <div 
                v-for="(step, index) in aiSolutionStepTokens" 
                :key="index"
                :class="['ai-step', { active: aiSolutionGeneratingStep >= index }]"
              >
                {{ step[0] }} - {{ step[1] }} - {{ step[2] }}
              </div>
              </div>
            </div>
        </div>
        
        <div v-else-if="aiResponse.solution" class="ai-response">
          <el-tabs type="border-card">
            <el-tab-pane label="解题思路">
              <div class="ai-solution-content" v-html="aiResponse.solution"></div>
              <div class="ai-response-actions">
                <el-button type="primary" @click="applyAiSolution">应用到题解</el-button>
        </div>
            </el-tab-pane>
            
            <el-tab-pane label="参考代码" v-if="aiResponse.code">
              <div class="solution-language-selector">
                <el-select v-model="aiCodeLanguage" placeholder="选择语言">
                  <el-option
                    v-for="lang in Object.keys(aiResponse.code)"
                    :key="lang"
                    :label="getLanguageName(lang)"
                    :value="lang">
                  </el-option>
                </el-select>
      </div>
              
              <div class="code-preview-container">
                <pre class="code-preview">{{ getCurrentAiCode }}</pre>
                <el-button 
                  class="copy-code-btn" 
                  type="primary" 
                  size="small" 
                  circle
                  @click="copyAiCode"
                  :title="'复制代码'"
                >
                  <el-icon><Document /></el-icon>
                </el-button>
              </div>
              
              <div class="ai-response-actions">
                <el-button type="primary" @click="applyAiCode">应用到代码编辑器</el-button>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { QuillEditor } from '@vueup/vue-quill';
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import { getProblemById, updateProblem as updateProblemApi, getAllTags } from '@/api/problem';
import { aiApi } from '@/api/ai';
import { Plus, Delete, Loading, Setting, Document, QuestionFilled } from '@element-plus/icons-vue';
import { loadAceCore, loadAceExt, loadAceMode, loadAceTheme } from '@/utils/aceLoader';

const router = useRouter();
const route = useRoute();
const problemFormRef = ref(null);
const loading = ref(true);
const problemId = Number(route.params.id);
const codeEditor = ref(null);
const activeCodeTab = ref('cpp');
const commonTags = ref([]);

// 代码编辑器相关
const codeLanguages = [
  { id: 'java', name: 'Java', mode: 'java' },
  { id: 'python', name: 'Python', mode: 'python' },
  { id: 'cpp', name: 'C++', mode: 'c_cpp' },
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
}
    `,
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
}
    `,
  c: `
#include <stdio.h>
#include <stdlib.h>

int main() {
    // 在此处编写解题代码
    
    // 读取输入
    
    // 处理并输出结果
    return 0;
}
    `,
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
}
    `,
  rust: `
use std::io;

fn main() {
    // 在此处编写解题代码
    
    // 读取输入
    
    // 处理并输出结果
}
    `,
  csharp: `
using System;

class Program {
    static void Main() {
        // 在此处编写解题代码
        
        // 读取输入
        
        // 处理并输出结果
    }
}
    `
};

// 保存原始代码模板，用于检测是否修改
const originalTemplates = {};
Object.keys(codeTemplates).forEach(key => {
  originalTemplates[key] = codeTemplates[key].trim();
});

// 问题表单数据
const problemForm = ref({
  title: '',
  difficulty: '普通',
  tags: [] as string[],
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
  solution: '',
  testCases: [{
    input: '',
    output: '',
    score: 10
  }],
  timeLimit: 1000,
  memoryLimit: 262144,
  isPublic: false,
  solutionCode: {
    java: '',
    python: '',
    cpp: '',
    c: '',
    javascript: '',
    go: '',
    rust: '',
    csharp: ''
  }
});

// AI辅助相关
const aiPrompt = ref('');
const aiLoading = ref(false);
const aiResponse = ref({
  solution: '',
  code: {}
});
const aiCodeLanguage = ref('java');

// AI辅助生成题解相关变量
const aiSolutionDrawerVisible = ref(false);
const aiSolutionGeneratingStep = ref(0);
const aiSolutionStepTokens = ref([
  ['识别问题类型', '分析数据约束', '确定核心难点', '...'],
  ['构建解题框架', '选择数据结构', '设计算法流程', '...'],
  ['分析时间复杂度', '评估空间需求', '优化算法效率', '...'],
  ['编写代码框架', '实现核心逻辑', '处理边界情况', '...'],
  ['完善算法分析', '添加详细注释', '整合最终题解', '...']
]);

// 打开AI辅助生成题解抽屉
const openAiSolutionDrawer = () => {
  aiSolutionDrawerVisible.value = true;
  if (!aiPrompt.value) {
    aiPrompt.value = `请为题目"${problemForm.title}"生成一个详细的解题思路和C++实现代码`;
  }
};

// 获取当前选中的AI生成代码
const getCurrentAiCode = computed(() => {
  if (!aiResponse.value.code || !aiCodeLanguage.value) return '';
  return aiResponse.value.code[aiCodeLanguage.value] || '';
});

// 获取语言名称
const getLanguageName = (langId) => {
  const lang = codeLanguages.find(l => l.id === langId);
  return lang ? lang.name : langId;
};

// 复制AI生成的代码
const copyAiCode = () => {
  const code = getCurrentAiCode.value;
  if (!code) return;
  
  navigator.clipboard.writeText(code)
    .then(() => {
      ElMessage.success('代码已复制到剪贴板');
    })
    .catch(() => {
      ElMessage.error('复制失败，请手动复制');
    });
};

// 生成题解
const generateSolution = async () => {
  if (!aiPrompt.value || aiPrompt.value.length < 10) {
    ElMessage.warning('请输入有效的提示内容');
    return;
  }

  aiLoading.value = true;
  aiSolutionGeneratingStep.value = 1;

  const stepInterval = window.setInterval(() => {
    aiSolutionGeneratingStep.value = aiSolutionGeneratingStep.value >= aiSolutionStepTokens.value.length - 1
      ? aiSolutionStepTokens.value.length - 1
      : aiSolutionGeneratingStep.value + 1;
  }, 1200);

  try {
    const prompt = `题目: ${problemForm.title}\n\n描述: ${stripHtml(problemForm.description)}\n\n输入格式: ${stripHtml(problemForm.inputFormat)}\n\n输出格式: ${stripHtml(problemForm.outputFormat)}\n\n示例输入: ${problemForm.inputExample}\n\n示例输出: ${problemForm.outputExample}\n\n用户请求: ${aiPrompt.value}`;

    const res: any = await aiApi.generateSolution(problemId, prompt);
    const response = res.code === 200 ? res.data : res;

    aiSolutionGeneratingStep.value = aiSolutionStepTokens.value.length - 1;
    aiResponse.value = response;

    if (response.code && Object.keys(response.code).length > 0) {
      aiCodeLanguage.value = Object.keys(response.code)[0];
    }

    ElMessage.success('题解生成成功');
  } catch (error) {
    console.error('生成题解失败:', error);
    ElMessage.error('生成题解失败，请稍后重试');
  } finally {
    window.clearInterval(stepInterval);
    aiLoading.value = false;
    aiSolutionGeneratingStep.value = 0;
  }
};

// 应用AI生成的题解
const applyAiSolution = () => {
  if (!aiResponse.value.solution) return;
  
  problemForm.solution = aiResponse.value.solution;
  ElMessage.success('已应用AI生成的题解');
};

// 应用AI生成的代码
const applyAiCode = () => {
  const code = getCurrentAiCode.value;
  if (!code || !aiCodeLanguage.value) return;
  
  // 保存当前编辑器语言的代码
  if (activeCodeTab.value !== aiCodeLanguage.value) {
    problemForm.value.solutionCode[activeCodeTab.value] = codeEditor.value.getValue();
  }
  
  // 切换到AI代码的语言
  activeCodeTab.value = aiCodeLanguage.value;
  
  // 更新编辑器内容
  nextTick(() => {
    if (codeEditor.value) {
      codeEditor.value.setValue(code);
      codeEditor.value.clearSelection();
    }
    
    // 更新表单数据
    problemForm.value.solutionCode[aiCodeLanguage.value] = code;
    
    ElMessage.success('已应用AI生成的代码');
  });
};

// 移除 HTML 标签，用于判断 AI 题解内容是否为空。
const stripHtml = (html) => {
  if (!html) return '';
  return html.replace(/<[^>]*>?/gm, '');
};

let publicExampleUidSeed = 0;

// 创建公开样例对象，uid 只给前端列表使用，避免删除样例时因为索引复用导致误删。
const createPublicExample = (example: any = {}) => ({
  uid: example.uid || `public-example-${Date.now()}-${++publicExampleUidSeed}`,
  input: String(example?.input ?? example?.inputExample ?? ''),
  output: String(example?.output ?? example?.expectedOutput ?? example?.outputExample ?? ''),
  explanation: String(example?.explanation ?? example?.explain ?? '')
});

// 规范公开样例数组：兼容旧字段、AI 返回字段和后端 examples 字段。
const normalizePublicExamples = (examples: any[] = []) => {
  const normalized = examples
    .map((example: any) => createPublicExample(example))
    .filter((example: any) => example.input || example.output);
  return normalized.length > 0 ? normalized : [createPublicExample()];
};

// 同步旧版样例字段，确保保存后老接口和做题 IDE 都能读取到样例。
const syncLegacyExampleFields = () => {
  const examples = normalizePublicExamples(problemForm.value.examples);
  problemForm.value.examples = examples;
  const first = examples[0] || { input: '', output: '' };
  const second = examples[1] || first;
  problemForm.value.inputExample = first.input;
  problemForm.value.outputExample = first.output;
  problemForm.value.debugInputExample = second.input;
  problemForm.value.debugOutputExample = second.output;
  return examples;
};

// 新增一个公开样例，题面和样例运行区域会动态展示。
const addPublicExample = () => {
  problemForm.value.examples.push(createPublicExample());
};

// 删除指定公开样例，至少保留一个公开样例。
const removePublicExample = (index: number) => {
  if (problemForm.value.examples.length <= 1) return;
  problemForm.value.examples = problemForm.value.examples.filter((_, currentIndex) => currentIndex !== index);
};

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入题目标题', trigger: 'blur' },
    { min: 3, max: 100, message: '标题长度应在3-100个字符之间', trigger: 'blur' }
  ],
  difficulty: [
    { required: true, message: '请选择难度级别', trigger: 'change' }
  ],
  tags: [
    { required: true, message: '请至少选择一个标签', trigger: 'change' },
    { type: 'array', min: 1, message: '请至少选择一个标签', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入题目描述', trigger: 'blur' }
  ],
  inputFormat: [],
  outputFormat: [
    { required: true, message: '请输入输出格式说明', trigger: 'blur' }
  ],
  inputExample: [],
  outputExample: [],
  timeLimit: [
    { required: true, message: '请设置时间限制', trigger: 'blur' },
    { type: 'number', min: 100, max: 10000, message: '时间限制应在100-10000ms之间', trigger: 'blur' }
  ],
  memoryLimit: [
    { required: true, message: '请设置内存限制', trigger: 'blur' },
    { type: 'number', min: 16, max: 1024, message: '内存限制应在16-1024MB之间', trigger: 'blur' }
  ]
};

const publicExampleRules = {
  output: [{ required: true, message: '请输入公开样例输出', trigger: 'blur' }]
};

// 测试用例验证规则
const testCaseRules = {
  input: [],
  output: [
    { required: true, message: '请输入测试用例期望输出', trigger: 'blur' }
  ],
  score: [
    { required: true, message: '请设置分值', trigger: 'blur' },
    { type: 'number', min: 1, message: '分值必须大于0', trigger: 'blur' }
  ]
};

// 计算总分值
const totalScore = computed(() => {
  return problemForm.value.testCases.reduce((sum, testCase) => sum + (testCase.score || 0), 0);
});

// 总分值设置
const totalScoreValue = ref(20); // 默认总分值为20
const isScoreBalanceError = computed(() => {
  return totalScore.value !== totalScoreValue.value;
});

// 检查分数是否平衡
const checkScoreBalance = () => {
  const totalScore = problemForm.value.testCases.reduce((sum, tc) => sum + (tc.score || 0), 0);
  
  if (totalScore !== totalScoreValue.value) {
    ElMessage.warning(`测试用例总分值(${totalScore})与设定总分值(${totalScoreValue.value})不一致，请调整`);
    return false;
  }
  
  return true;
};

// 监听总分值变化，确保不超过最大值
watch(totalScoreValue, (newValue) => {
  if (newValue > 100) {
    totalScoreValue.value = 100;
    ElMessage.warning('题目总分值不能超过100分');
  }
});

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

    const initialCode = problemForm.value.solutionCode[activeCodeTab.value] || codeTemplates[activeCodeTab.value] || '';
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

// 监听代码语言切换
watch(activeCodeTab, async (newLang, oldLang) => {
  console.log(`切换到${newLang}编辑器`);

  if (codeEditor.value) {
    // 保存当前代码到对应语言
    if (oldLang) {
      problemForm.value.solutionCode[oldLang] = codeEditor.value.getValue();
      console.log(`已保存${oldLang}代码`);
    }

    const mode = getLanguageMode(newLang);
    await loadAceMode(mode);

    // 切换到新语言的模式
    codeEditor.value.session.setMode(`ace/mode/${mode}`);
    console.log(`已切换到${newLang}模式`);

    // 获取新语言的代码
    let code = '';
    if (problemForm.value.solutionCode[newLang] && problemForm.value.solutionCode[newLang].trim() !== '') {
      // 如果已经有保存的代码，使用保存的代码
      code = problemForm.value.solutionCode[newLang];
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

// 加载题目数据：把后端详情转换成编辑表单、公开样例、隐藏测试点和参考代码。
const loadProblemData = async () => {
  if (!problemId) {
    ElMessage.error('未找到题目ID，请重试');
    router.push('/problems');
    return;
  }

  loading.value = true;
  try {
    // 从 API 获取题目详情，编辑页需要包含测试点和题解等完整信息。
    const response = await getProblemById(problemId);
    
    if (response) {
      const problem = response;
      console.log('Loaded problem data:', problem);
      
      // 填充表单基础字段。
      problemForm.value.title = problem.title || '';
      problemForm.value.difficulty = problem.difficulty || '普通';
      
      // 处理标签：兼容字符串、对象数组和字符串数组三种返回格式。
      if (problem.tags) {
        if (typeof problem.tags === 'string') {
          // 如果是字符串，按逗号分割
          problemForm.value.tags = problem.tags.split(',').map(tag => tag.trim()).filter(tag => tag);
        } else if (Array.isArray(problem.tags)) {
          // 如果是数组，直接使用或者提取name字段
          problemForm.value.tags = problem.tags.map(tag => typeof tag === 'object' ? tag.name : tag);
        } else {
          problemForm.value.tags = [];
        }
      } else {
        problemForm.value.tags = [];
      }
      
      problemForm.value.description = problem.description || '';
      problemForm.value.inputFormat = problem.inputFormat || '';
      problemForm.value.outputFormat = problem.outputFormat || '';
      problemForm.value.examples = normalizePublicExamples(problem.examples || [
        { input: problem.inputExample || '', output: problem.outputExample || '', explanation: '' },
        { input: problem.debugInputExample || '', output: problem.debugOutputExample || '', explanation: '' }
      ]);
      syncLegacyExampleFields();
      problemForm.value.hint = problem.hint || '';
      problemForm.value.solution = problem.solution || '';
      
      // 设置可见性
      problemForm.value.isPublic = problem.visibility === 'PUBLIC';
      
      // 转换内存限制从KB到MB
      problemForm.value.memoryLimit = problem.memoryLimit ? Math.floor(problem.memoryLimit / 1024) : 256;
      problemForm.value.timeLimit = problem.timeLimit || 1000;
      
      // 处理隐藏测试用例：后端字段 expectedOutput 在前端表单中显示为 output。
      if (problem.testCases && problem.testCases.length > 0) {
        problemForm.value.testCases = problem.testCases.map(tc => ({
          id: tc.id || null,
          input: tc.input || '',
          output: tc.expectedOutput || '', // 使用后端返回的expectedOutput字段
          score: tc.score || 10
        }));
      } else {
        // 如果没有测试用例，添加一个默认的测试用例
        problemForm.value.testCases = [{ input: '', output: '', score: 10 }];
      }
      
      // 加载代码解决方案
      if (problem.solutionCode) {
        // 如果是字符串，尝试解析为JSON
        if (typeof problem.solutionCode === 'string') {
          try {
            problemForm.value.solutionCode = JSON.parse(problem.solutionCode);
          } catch (e) {
            console.error('解析题解代码失败:', e);
          }
        } else {
          // 如果已经是对象，直接赋值
        problemForm.value.solutionCode = problem.solutionCode;
        }
        
        // 更新代码编辑器的值
        nextTick(() => {
          if (codeEditor.value && problemForm.value.solutionCode[activeCodeTab.value]) {
            codeEditor.value.setValue(problemForm.value.solutionCode[activeCodeTab.value]);
            codeEditor.value.clearSelection();
          }
        });
      }
      
      // 更新总分值
      totalScoreValue.value = problemForm.value.testCases.reduce((sum, tc) => sum + (tc.score || 0), 0);
      
      ElMessage.success('题目数据加载成功');
    } else {
      ElMessage.error('题目数据为空');
    }
  } catch (error) {
    console.error('加载题目详情失败:', error);
    ElMessage.error('加载题目详情失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 加载所有标签
const loadTags = async () => {
  try {
    const response = await getAllTags();
    if (response) {
      // 统一标签格式：后端可能返回对象数组，也可能直接返回字符串数组。
      commonTags.value = Array.isArray(response) 
        ? response.map(tag => typeof tag === 'object' ? tag.name : tag) 
        : [];
      console.log('Loaded tags:', commonTags.value);
    }
  } catch (error) {
    console.error('加载标签失败:', error);
    ElMessage.warning('加载标签列表失败，将使用默认标签');
    commonTags.value = ['数组', '字符串', '哈希表', '动态规划', '链表'];
  }
};

// 验证分数总和
const validateScore = () => {
  if (totalScore.value > totalScoreValue.value) {
    ElMessage.warning(`测试用例总分值(${totalScore.value})超过了设定总分值(${totalScoreValue.value})`);
  }
};

// 添加测试用例
const addTestCase = () => {
  const defaultScore = Math.max(1, Math.min(10, totalScoreValue.value));
  problemForm.value.testCases.push({ input: '', output: '', score: defaultScore });
};

// 删除测试用例
const removeTestCase = (index) => {
  if (problemForm.value.testCases.length > 1) {
    problemForm.value.testCases.splice(index, 1);
  }
};

// 提交更新题目：保存题面、公开样例、隐藏测试点、题解和可见性。
const updateProblem = async () => {
  if (!checkScoreBalance()) {
    return;
  }
  
  // 收集当前语言的代码
  if (codeEditor.value) {
    if (!problemForm.value.solutionCode) {
      problemForm.value.solutionCode = {};
    }
    problemForm.value.solutionCode[activeCodeTab.value] = codeEditor.value.getValue();
  }
  
  // 清理未修改的代码模板
  const cleanedSolutionCode = {};
  let hasValidSolution = false;
  
  Object.keys(problemForm.value.solutionCode).forEach(lang => {
    const code = problemForm.value.solutionCode[lang];
    // 如果代码不为空且不等于模板，则保留
    if (code && code.trim() !== '' && code.trim() !== codeTemplates[lang].trim()) {
      cleanedSolutionCode[lang] = code;
      hasValidSolution = true;
    }
  });
  
  // 检查题解内容
  const hasSolutionText = problemForm.value.solution && problemForm.value.solution.trim() !== '';
  
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
      aiPrompt.value = `请为题目"${problemForm.value.title}"生成一个详细的解题思路和C++实现代码`;
      openAiSolutionDrawer();
      await generateSolution();
      return; // 停止提交，等用户确认AI生成的题解
      
    } catch (e) {
      // 用户选择继续提交，不生成题解
      console.log('用户选择继续提交，不生成题解');
    }
  }
  
  problemFormRef.value.validate(async (valid) => {
    if (valid) {
      // 显示提交中消息
      const loadingMessage = ElMessage({
        message: '正在更新题目...',
        type: 'info',
        duration: 0
      });
      
      try {
        const publicExamples = syncLegacyExampleFields();
        // 准备提交数据：examples 是多个公开样例，旧字段保留首个和第二个样例做兼容。
        const problemData = {
          title: problemForm.value.title,
          difficulty: problemForm.value.difficulty,
          tags: problemForm.value.tags.join(','),
          description: problemForm.value.description,
          inputFormat: problemForm.value.inputFormat,
          outputFormat: problemForm.value.outputFormat,
          inputExample: problemForm.value.inputExample,
          outputExample: problemForm.value.outputExample,
          examples: JSON.stringify(publicExamples),
          debugInputExample: problemForm.value.debugInputExample,
          debugOutputExample: problemForm.value.debugOutputExample,
          hint: problemForm.value.hint,
          solution: problemForm.value.solution,
          solutionCode: JSON.stringify(cleanedSolutionCode), // 使用清理后的代码
          timeLimit: problemForm.value.timeLimit,
          memoryLimit: problemForm.value.memoryLimit * 1024, // 转为KB
          visibility: problemForm.value.isPublic ? 'PUBLIC' : 'PRIVATE',
          testCases: JSON.stringify(problemForm.value.testCases.map(tc => ({
            input: tc.input,
            expectedOutput: tc.output, // 使用后端期望的字段名expectedOutput，但值仍然来自tc.output
            score: tc.score
          })))
        };
        
        console.log('Submitting problem update:', problemData);
        
        // 调用API更新问题
        const response = await updateProblemApi(problemId, problemData);
        
        // 关闭loading消息
        loadingMessage.close();
        
        if (response && (response.code === 200 || response.success === true)) {
        // 显示成功消息
        ElMessage.success({
          message: response.message || '题目更新成功！',
          duration: 3000
        });
        
        // 跳转到题库页面
        router.push('/problems');
        } else {
          throw new Error(response?.message || '未知错误');
        }
      } catch (error: any) {
        // 关闭loading消息
        loadingMessage.close();
        
        // 显示错误消息
        const message = error?.response?.data?.message || error?.response?.data?.error || error?.message || '未知错误';
        ElMessage.error({
          message: `更新失败: ${message}`,
          duration: 5000
        });
      }
    } else {
      ElMessage.error('请完善必填项');
      return false;
    }
  });
};

// 取消
const cancel = () => {
  ElMessageBox.confirm('确定要取消编辑？未保存的修改将丢失', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    router.push('/problems');
  }).catch(() => {});
};

// 获取语言选项标签，显示哪些语言已有代码
const getLanguageOptionLabel = (lang) => {
  const hasCode = problemForm.value.solutionCode && 
                  problemForm.value.solutionCode[lang.id] && 
                  problemForm.value.solutionCode[lang.id].trim() !== '' &&
                  problemForm.value.solutionCode[lang.id].trim() !== codeTemplates[lang.id].trim();
  
  return hasCode ? `${lang.name} ✓` : lang.name;
};

// 初始化
onMounted(async () => {
  // 并行加载标签和题目数据
  await Promise.all([loadTags(), loadProblemData()]);
  
  // 初始化代码编辑器
  initCodeEditor();
});
</script>

<style scoped>
.problem-edit {
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
  font-size: 18px;
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
  gap: 10px;
  margin-top: 20px;
}

.privacy-tip {
  width: 100%;
  margin-top: 6px;
  color: #7a8b9a;
  font-size: 12px;
  line-height: 1.5;
}

.error-text {
  color: #F56C6C;
  font-weight: bold;
}

/* AI辅助相关样式 */
.ai-drawer-content {
  padding: 20px;
}

.ai-generating-animation {
  padding: 30px;
  text-align: center;
}

.ai-loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ai-loading-icon {
  font-size: 40px;
  color: #409eff;
  margin-bottom: 20px;
  animation: rotate 2s linear infinite;
}

.ai-loading-title {
  font-size: 20px;
  margin-bottom: 10px;
}

.ai-loading-text {
  color: #606266;
  margin-bottom: 30px;
}

.ai-loading-steps {
  width: 100%;
  max-width: 500px;
}

.ai-step {
  padding: 10px;
  margin-bottom: 10px;
  border-radius: 4px;
  background-color: #f5f7fa;
  color: #909399;
  transition: all 0.3s;
}

.ai-step.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.ai-solution-content {
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #f9f9f9;
  margin-bottom: 20px;
  max-height: 400px;
  overflow-y: auto;
}

.ai-response-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 15px;
}

.code-preview-container {
  position: relative;
  margin: 15px 0;
}

.code-preview {
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #f9f9f9;
  max-height: 300px;
  overflow-y: auto;
  font-family: monospace;
  white-space: pre-wrap;
}

.copy-code-btn {
  position: absolute;
  top: 10px;
  right: 10px;
}

.solution-language-selector {
  margin-bottom: 15px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
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

.code-notice {
  margin-bottom: 15px;
}
</style> 
