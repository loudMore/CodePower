<!-- 注册页面 -->
<template>
  <div class="auth-card">
    <div class="auth-header">
      <h1 class="title">创建您的 Code Power 账户</h1>
      <p class="subtitle">加入我们，开启无限可能</p>
    </div>

    <el-form 
      :model="registerForm" 
      :rules="registerRules" 
      ref="registerFormRef" 
      label-position="top"
      @submit.prevent="handleRegister"
      class="auth-form"
    >
      <el-form-item prop="username">
        <el-input 
          v-model="registerForm.username" 
          placeholder="设置一个独特的用户名（仅支持字母、数字、下划线和中文）" 
          size="large" 
          :prefix-icon="User"
          :loading="isCheckingUsername"
        />
      </el-form-item>
      <el-form-item prop="email">
        <el-input 
          v-model="registerForm.email" 
          type="email"
          autocomplete="email"
          :spellcheck="false"
          placeholder="您的电子邮箱" 
          size="large" 
          :prefix-icon="Message"
          :loading="isCheckingEmail" 
          @blur="normalizeEmailInput"
        />
      </el-form-item>
      <el-form-item prop="region">
        <el-select
          v-model="registerForm.region"
          filterable
          clearable
          placeholder="选填：请选择省级地区"
          size="large"
          style="width: 100%;"
        >
          <el-option
            v-for="region in REGION_OPTIONS"
            :key="region.value"
            :label="region.label"
            :value="region.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item prop="verificationCode">
          <div class="code-container">
            <el-input v-model="registerForm.verificationCode" placeholder="邮箱验证码" size="large" />
            <el-button @click="handleSendCode" :disabled="isSendingCode || countdown > 0" size="large">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="registerForm.password" type="password" placeholder="设置密码" show-password size="large" :prefix-icon="Lock" />
        <div v-if="registerForm.password" class="password-strength">
          <div class="strength-bar">
            <div class="strength-fill" :class="passwordStrength.level" :style="{ width: passwordStrength.percent + '%' }"></div>
          </div>
          <span class="strength-text" :class="passwordStrength.level">{{ passwordStrength.text }}</span>
        </div>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" show-password size="large" :prefix-icon="Lock" />
      </el-form-item>
      <el-form-item prop="agreement">
        <el-checkbox v-model="registerForm.agreement">
          我已阅读并同意
          <router-link to="/terms" class="link">服务条款</router-link>
        </el-checkbox>
      </el-form-item>
      <el-button 
        type="primary" 
        native-type="submit" 
        class="submit-button" 
        :loading="isLoading || isCheckingUsername || isCheckingEmail" 
        size="large"
      >
        立即注册
      </el-button>
    </el-form>
    
    <div class="switch-link">
      已经有账户了?
      <router-link to="/auth/login" class="link">直接登录</router-link>
    </div>

    <VerificationModal ref="verificationModalRef" @success="onVerificationSuccess" @failure="onVerificationFailure" @cancel="onVerificationCancel" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { sendEmailCode, registerUser, checkUsernameExists, checkEmailExists } from '@/api/auth';
import VerificationModal from '@/components/VerificationModal.vue';
import { User, Lock, Message } from '@element-plus/icons-vue';
import { REGION_OPTIONS } from '@/utils/regions';

const router = useRouter();
const registerFormRef = ref();
const verificationModalRef = ref();
const isLoading = ref(false);
const isSendingCode = ref(false);
const countdown = ref(0);
const isCheckingUsername = ref(false);
const isCheckingEmail = ref(false);

const registerForm = reactive({
  username: '',
  email: '',
  region: '',
  password: '',
  confirmPassword: '',
  verificationCode: '',
  agreement: false,
});

// 邮箱统一去掉前后空格并转成小写，避免大小写或复制空格导致前后端验证码缓存键不一致。
const normalizeEmailValue = (email: string) => String(email || '').trim().toLowerCase();

const normalizeEmailInput = () => {
  registerForm.email = normalizeEmailValue(registerForm.email);
};

const passwordStrength = computed(() => {
  const pwd = registerForm.password;
  if (!pwd) return { level: '', text: '', percent: 0 };
  let score = 0;
  if (pwd.length >= 6) score++;
  if (pwd.length >= 10) score++;
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++;
  if (/\d/.test(pwd)) score++;
  if (/[^a-zA-Z0-9]/.test(pwd)) score++;
  if (score <= 1) return { level: 'weak', text: '弱', percent: 25 };
  if (score <= 3) return { level: 'medium', text: '中', percent: 60 };
  return { level: 'strong', text: '强', percent: 100 };
});

const validateUsername = (rule: any, value: any, callback: any) => {
  // 只允许字母、数字、下划线和中文
  const pattern = /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/;
  if (!pattern.test(value)) {
    callback(new Error('用户名只能包含字母、数字、下划线和中文'));
    return;
  }
  
  // 远程验证用户名是否存在
  if (value) {
    isCheckingUsername.value = true;
    checkUsernameExists(value)
      .then(response => {
        const exists = Boolean((response as any)?.exists ?? (response as any)?.data?.exists);
        if (exists) {
          callback(new Error('该用户名已被使用'));
        } else {
          callback();
        }
      })
      .catch(() => {
        // 如果请求失败，允许通过验证，后端会再次验证
        callback();
      })
      .finally(() => {
        isCheckingUsername.value = false;
      });
  } else {
    callback();
  }
};

const validateEmail = (rule: any, value: any, callback: any) => {
  const normalizedEmail = normalizeEmailValue(value);
  if (normalizedEmail !== value) {
    registerForm.email = normalizedEmail;
  }

  // 先验证邮箱格式
  const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!emailPattern.test(normalizedEmail)) {
    callback(new Error('请输入有效的邮箱地址'));
    return;
  }
  
  // 远程验证邮箱是否存在
  if (normalizedEmail) {
    isCheckingEmail.value = true;
    checkEmailExists(normalizedEmail)
      .then(response => {
        const exists = Boolean((response as any)?.exists ?? (response as any)?.data?.exists);
        if (exists) {
          callback(new Error('该邮箱已被注册'));
        } else {
          callback();
        }
      })
      .catch(() => {
        // 如果请求失败，允许通过验证，后端会再次验证
        callback();
      })
      .finally(() => {
        isCheckingEmail.value = false;
      });
  } else {
    callback();
  }
};

const validatePass = (rule: any, value: any, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致!'));
  } else {
    callback();
  }
};

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, {min: 6, message: '密码长度至少为6位', trigger: 'blur'}],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validatePass, trigger: 'blur' }],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  agreement: [{
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('请先同意服务条款'));
      } else {
        callback();
      }
    },
    trigger: 'change'
  }]
};

const handleSendCode = async () => {
  normalizeEmailInput();
  registerFormRef.value.validateField('email', async (isValid: boolean) => {
    if (isValid) {
      verificationModalRef.value.open();
    } else {
      ElMessage.warning('请输入有效的邮箱地址');
    }
  });
};

const onVerificationSuccess = async (_token: string) => {
  isSendingCode.value = true;
  try {
    normalizeEmailInput();
    await sendEmailCode(registerForm.email);
    ElMessage.success('验证码已发送，请注意查收');
    countdown.value = 60;
    const timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(timer);
      }
    }, 1000);
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '发送失败，请重试');
  } finally {
    isSendingCode.value = false;
  }
};

const onVerificationFailure = (message: string) => {
  ElMessage.error(message);
};

const onVerificationCancel = () => {
  // 用户关闭验证窗口时无需额外处理
};

const handleRegister = () => {
  normalizeEmailInput();
  registerFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      isLoading.value = true;
      try {
        await registerUser({
          username: registerForm.username,
          email: normalizeEmailValue(registerForm.email),
          region: registerForm.region || undefined,
          password: registerForm.password,
          verificationCode: registerForm.verificationCode
        });
        ElMessage.success('注册成功！正在跳转到登录页...');
        router.push('/auth/login');
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '注册失败');
      } finally {
        isLoading.value = false;
      }
    }
  });
};
</script>

<style scoped>
.auth-card {
  width: 100%;
  max-width: 480px;
  padding: 40px;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 100;
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.subtitle {
  margin-top: 10px;
  color: #909399;
}

.auth-form {
  display: flex;
  flex-direction: column;
}

.submit-button {
  width: 100%;
  margin-top: 10px;
  letter-spacing: 2px;
}

.link {
  color: #409EFF;
  text-decoration: none;
  font-size: 14px;
}
.link:hover {
  text-decoration: underline;
}

.switch-link {
  margin-top: 30px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.code-container {
  display: flex;
  width: 100%;
  gap: 10px;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  width: 100%;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background-color: #e4e7ed;
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background-color 0.3s;
}

.strength-fill.weak { background-color: #f56c6c; }
.strength-fill.medium { background-color: #e6a23c; }
.strength-fill.strong { background-color: #67c23a; }

.strength-text {
  font-size: 12px;
  white-space: nowrap;
}

.strength-text.weak { color: #f56c6c; }
.strength-text.medium { color: #e6a23c; }
.strength-text.strong { color: #67c23a; }
</style> 
