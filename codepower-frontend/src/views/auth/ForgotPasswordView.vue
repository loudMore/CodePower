<!-- 忘记密码 — 邮箱验证码重置密码 -->
<template>
  <div class="auth-card">
    <div v-if="step === 1">
      <div class="auth-header">
        <h1 class="title">找回您的账户</h1>
        <p class="subtitle">请输入您的注册邮箱，我们将引导您重设密码。</p>
      </div>

      <el-form 
        :model="form" 
        :rules="rules" 
        ref="formRef" 
        label-position="top"
        @submit.prevent="handleRequestReset"
        class="auth-form"
      >
        <el-form-item prop="email">
          <el-input 
            v-model="form.email" 
            type="email"
            autocomplete="email"
            :spellcheck="false"
            placeholder="请输入注册时使用的邮箱"
            size="large"
            :prefix-icon="Message"
            @blur="normalizeEmailInput"
          ></el-input>
        </el-form-item>
        
        <el-button 
          type="primary" 
          class="submit-button" 
          native-type="submit"
          :loading="isLoading"
          size="large"
        >
          发送验证码
        </el-button>
      </el-form>
    </div>

    <div v-if="step === 2">
      <div class="auth-header">
        <h1 class="title">重设密码</h1>
        <p class="subtitle">我们已向 {{ form.email }} 发送了验证码，请输入以完成重设。</p>
      </div>

      <el-form 
        :model="form" 
        :rules="rules" 
        ref="formRef" 
        label-position="top"
        @submit.prevent="handleResetPassword"
        class="auth-form"
      >
        <el-form-item prop="code">
          <el-input 
            v-model="form.code" 
            placeholder="邮件验证码"
            size="large"
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            type="password"
            placeholder="您的新密码"
            show-password
            size="large"
            :prefix-icon="Lock"
          ></el-input>
        </el-form-item>
        
        <el-button 
          type="primary" 
          class="submit-button" 
          native-type="submit"
          :loading="isLoading"
          size="large"
        >
          确认重置密码
        </el-button>
      </el-form>
    </div>
    
    <div class="switch-link">
      <template v-if="step === 1">
        想起来了? 
        <router-link to="/auth/login" class="link">返回登录</router-link>
      </template>
      <template v-if="step === 2">
        记起旧密码了?
        <a href="#" @click.prevent="step = 1" class="link">返回上一步</a>
      </template>
    </div>

    <VerificationModal ref="verificationModalRef" @success="onVerificationSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import VerificationModal from '@/components/VerificationModal.vue';
import { requestPasswordReset, resetPassword } from '@/api/auth'; 
import { Message, Lock } from '@element-plus/icons-vue';

const router = useRouter();
const formRef = ref();
const verificationModalRef = ref();
const isLoading = ref(false);
const step = ref(1);

const form = reactive({
  email: '',
  code: '',
  password: ''
});

// 邮箱统一去掉前后空格并转小写，避免找回密码验证码缓存键大小写不一致。
const normalizeEmailValue = (email: string) => String(email || '').trim().toLowerCase();

const normalizeEmailInput = () => {
  form.email = normalizeEmailValue(form.email);
};

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] }
  ],
  code: [
    { required: true, message: '请输入您收到的验证码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' }
  ]
};

const handleRequestReset = () => {
  normalizeEmailInput();
  formRef.value.validateField('email', (valid: boolean) => {
    if (valid) {
      verificationModalRef.value.open();
    }
  });
};

const onVerificationSuccess = async (token: string) => {
  isLoading.value = true;
  try {
    normalizeEmailInput();
    await requestPasswordReset(form.email, token); 
    ElMessage.success('验证码已发送，请检查您的邮箱。');
    step.value = 2; // 验证通过后进入设置新密码步骤
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败，请重试');
  } finally {
    isLoading.value = false;
  }
};

const handleResetPassword = () => {
  normalizeEmailInput();
  formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      isLoading.value = true;
      try {
        await resetPassword({
          email: form.email,
          code: form.code,
          newPassword: form.password
        });
        ElMessage.success('密码重置成功！正在跳转到登录页...');
        router.push('/auth/login');
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '重置失败，请检查验证码是否正确');
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
  max-width: 450px;
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
</style> 
