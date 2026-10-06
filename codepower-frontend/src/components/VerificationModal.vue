<!-- 人机验证弹窗 — 登录/注册时的交互式验证 -->
<template>
  <div v-if="isVisible" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <div class="header-left">
          <span class="header-icon">&#60;/&#62;</span>
          <h3>Code Verification</h3>
        </div>
        <button @click="close" class="close-btn">&times;</button>
      </div>
      <div v-if="!isLoading" class="modal-body">
        <div class="code-container">
          <div class="code-toolbar">
            <span class="code-lang">Python</span>
            <span class="code-hint">What is the output?</span>
          </div>
          <pre class="code-block"><code>{{ challenge.prompt }}</code></pre>
        </div>
        <div class="options-container">
          <button
            v-for="(option, index) in challenge.options"
            :key="index"
            class="option-btn"
            :class="{
              selected: selectedAnswer === option,
              'correct-animation': correctAnswer === option,
              'wrong-animation': wrongAnswer === option
            }"
            @click="selectOption(option)"
          >
            <span class="option-label">{{ String.fromCharCode(65 + index) }}</span>
            <span class="option-text">{{ option }}</span>
          </button>
        </div>
      </div>
      <div v-else class="modal-body loading-container">
        <div class="loading-spinner"></div>
        <p>{{ loadingMessage }}</p>
      </div>
      <div class="modal-footer">
        <button @click="refreshChallenge" class="btn-refresh" :disabled="isLoading">
          <span class="refresh-icon">&#8635;</span> 换一题
        </button>
        <button @click="submit" class="btn-primary" :disabled="isLoading || !selectedAnswer">
          {{ isLoading ? '验证中...' : '确认' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { getVerificationChallenge, submitVerification } from '@/api/auth';

// 编程题验证弹窗：登录前获取一道短代码题，答对后把验证 token 交给登录接口。

const isVisible = ref(false);
const isLoading = ref(false);
const loadingMessage = ref('正在加载验证题目...');
const challenge = reactive({
  challengeId: '',
  prompt: '',
  options: [] as string[]
});
const selectedAnswer = ref<string | null>(null);
const correctAnswer = ref<string | null>(null);
const wrongAnswer = ref<string | null>(null);
const retryCount = ref(0);
const maxRetries = 3;

const emit = defineEmits(['success', 'failure', 'cancel']);

// 打开弹窗并加载一轮新的验证题。
const open = async () => {
  isVisible.value = true;
  isLoading.value = true;
  loadingMessage.value = '正在加载验证题目...';
  selectedAnswer.value = null;
  correctAnswer.value = null;
  wrongAnswer.value = null;
  retryCount.value = 0;
  await loadChallenge();
};

// 从后端获取验证题、选项和 challengeId。
const loadChallenge = async () => {
  try {
    const response = await getVerificationChallenge();
    if (response && response.challengeId) {
      Object.assign(challenge, response);
      isLoading.value = false;
    } else {
      throw new Error('无效的验证码数据');
    }
  } catch (error) {
    console.error('加载编程题验证挑战失败:', error);
    if (retryCount.value < maxRetries) {
      retryCount.value++;
      loadingMessage.value = `加载失败，正在重试 (${retryCount.value}/${maxRetries})...`;
      setTimeout(loadChallenge, 1500);
    } else {
      loadingMessage.value = '验证码加载失败，请点击确认按钮重试';
      ElMessage.error('验证码加载失败，请刷新页面重试');
      isLoading.value = false;
    }
  }
};

// 刷新验证题，同时清空用户选择和错误状态。
const refreshChallenge = async () => {
  isLoading.value = true;
  loadingMessage.value = '正在更换验证题目...';
  selectedAnswer.value = null;
  correctAnswer.value = null;
  wrongAnswer.value = null;
  await loadChallenge();
};

const close = () => {
  isVisible.value = false;
  emit('cancel');
};

const selectOption = (option: string) => {
  if (correctAnswer.value || wrongAnswer.value) return;
  selectedAnswer.value = option;
};

// 提交用户选择，成功时把后端签发的 verificationToken 返回给父组件。
const submit = async () => {
  if (isLoading.value && retryCount.value >= maxRetries) {
    retryCount.value = 0;
    loadingMessage.value = '正在重新加载验证题目...';
    await loadChallenge();
    return;
  }

  if (!selectedAnswer.value) {
    ElMessage.warning('请选择一个答案');
    return;
  }

  isLoading.value = true;
  loadingMessage.value = '正在验证...';

  try {
    const result = await submitVerification(challenge.challengeId, [selectedAnswer.value]);

    if (result && result.success) {
      isLoading.value = false;
      correctAnswer.value = selectedAnswer.value;
      setTimeout(() => {
        emit('success', result.verificationToken);
        close();
      }, 600);
    } else {
      isLoading.value = false;
      wrongAnswer.value = selectedAnswer.value;
      const errorMsg = result?.message || '验证失败';
      ElMessage.error(errorMsg);

      setTimeout(() => {
        wrongAnswer.value = null;
        selectedAnswer.value = null;
        refreshChallenge();
      }, 800);
    }
  } catch (error) {
    console.error('Verification submission failed:', error);
    isLoading.value = false;
    emit('failure', '验证服务出错');
    ElMessage.error('验证服务出错，请稍后重试');
    close();
  }
};

defineExpose({
  open
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
}

.modal-content {
  width: 480px;
  max-width: 95vw;
  background-color: #1e1e2e;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
  overflow: hidden;
  animation: modal-fade 0.3s ease-out;
}

@keyframes modal-fade {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  padding: 14px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #181825;
  border-bottom: 1px solid #313244;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  color: #89b4fa;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  font-size: 1.1rem;
  font-weight: bold;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #cdd6f4;
  font-weight: 500;
}

.close-btn {
  background: none;
  border: none;
  color: #6c7086;
  font-size: 1.8rem;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #f38ba8;
}

.modal-body {
  padding: 0;
  text-align: center;
}

.loading-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #1e1e2e;
  color: #cdd6f4;
  min-height: 300px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 20px auto;
  border: 4px solid rgba(137, 180, 250, 0.3);
  border-radius: 50%;
  border-top-color: #89b4fa;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.code-container {
  background-color: #11111b;
  border-bottom: 1px solid #313244;
}

.code-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #181825;
  border-bottom: 1px solid #313244;
}

.code-lang {
  color: #a6e3a1;
  font-size: 0.8rem;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  padding: 2px 8px;
  background-color: rgba(166, 227, 161, 0.1);
  border-radius: 4px;
}

.code-hint {
  color: #6c7086;
  font-size: 0.8rem;
}

.code-block {
  margin: 0;
  padding: 20px 24px;
  text-align: left;
  font-family: 'Fira Code', 'Cascadia Code', 'Consolas', monospace;
  font-size: 0.95rem;
  line-height: 1.7;
  color: #cdd6f4;
  white-space: pre;
  overflow-x: auto;
}

.code-block code {
  font-family: inherit;
}

.options-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 16px 20px;
  background-color: #1e1e2e;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background-color: #313244;
  border: 2px solid transparent;
  border-radius: 10px;
  color: #cdd6f4;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  font-size: 0.95rem;
}

.option-btn:hover {
  background-color: #45475a;
  transform: translateY(-1px);
}

.option-btn.selected {
  background-color: rgba(137, 180, 250, 0.15);
  border-color: #89b4fa;
  color: #89b4fa;
}

.option-btn.correct-animation {
  background-color: rgba(166, 227, 161, 0.15);
  border-color: #a6e3a1;
  color: #a6e3a1;
  animation: correct-pulse 0.6s ease;
}

.option-btn.wrong-animation {
  background-color: rgba(243, 139, 168, 0.15);
  border-color: #f38ba8;
  color: #f38ba8;
  animation: shake 0.5s ease-in-out;
}

@keyframes correct-pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.03); }
  100% { transform: scale(1); }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

.option-label {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 28px;
  height: 28px;
  background-color: #45475a;
  border-radius: 6px;
  font-weight: bold;
  font-size: 0.85rem;
  flex-shrink: 0;
}

.option-btn.selected .option-label {
  background-color: #89b4fa;
  color: #1e1e2e;
}

.option-btn.correct-animation .option-label {
  background-color: #a6e3a1;
  color: #1e1e2e;
}

.option-btn.wrong-animation .option-label {
  background-color: #f38ba8;
  color: #1e1e2e;
}

.option-text {
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  word-break: break-all;
}

.modal-footer {
  padding: 14px 20px;
  display: flex;
  justify-content: space-between;
  background-color: #181825;
  border-top: 1px solid #313244;
}

.btn-refresh {
  background-color: #313244;
  color: #a6adc8;
  border: 1px solid #45475a;
  border-radius: 8px;
  padding: 8px 15px;
  cursor: pointer;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.2s;
}

.btn-refresh:hover:not(:disabled) {
  background-color: #45475a;
  color: #cdd6f4;
}

.btn-refresh:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-icon {
  font-size: 1rem;
}

.btn-primary {
  background-color: #89b4fa;
  color: #1e1e2e;
  border: none;
  border-radius: 8px;
  padding: 10px 25px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background-color: #74c7ec;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(137, 180, 250, 0.3);
}

.btn-primary:disabled {
  background-color: #45475a;
  color: #6c7086;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .options-container {
    grid-template-columns: 1fr;
  }

  .code-block {
    font-size: 0.85rem;
    padding: 16px;
  }
}
</style>
