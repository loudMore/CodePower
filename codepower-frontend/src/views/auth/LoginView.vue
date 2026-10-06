<!-- 登录页面 -->
<template>
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="title">登录 Code Power</h1>
        <p class="subtitle">欢迎回来！继续您的算法学习之旅。</p>
      </div>

      <el-form 
        :model="loginForm" 
        :rules="loginRules" 
        ref="loginFormRef" 
        label-position="top"
      @submit.prevent="handleLogin"
        class="auth-form"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="用户名或邮箱"
            size="large"
            :prefix-icon="User"
            @blur="normalizeLoginAccount"
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="密码" 
            show-password
            size="large"
            :prefix-icon="Lock"
          ></el-input>
        </el-form-item>
        
        <div class="form-options">
          <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          <router-link to="/auth/forgot-password" class="link">忘记密码?</router-link>
        </div>
        
        <el-button 
          type="primary" 
          class="submit-button" 
        native-type="submit"
        :loading="isLoading"
          size="large"
        >
          立即登录
        </el-button>
      </el-form>

      <div class="social-login">
        <el-divider>或通过以下方式登录</el-divider>
        <div class="social-icons">
          <el-tooltip :content="providerTooltip('github')" placement="top">
            <el-button
              circle
              size="large"
              class="social-icon github"
              :class="{ unavailable: !isProviderAvailable('github') }"
              :aria-disabled="!isProviderAvailable('github')"
              @click="startOAuthLogin('github')"
            >
              <i class="fa-brands fa-github"></i>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="providerTooltip('gitee')" placement="top">
            <el-button
              circle
              size="large"
              class="social-icon gitee"
              :class="{ unavailable: !isProviderAvailable('gitee') }"
              :aria-disabled="!isProviderAvailable('gitee')"
              @click="startOAuthLogin('gitee')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0c-6.628 0-12 5.373-12 12s5.372 12 12 12 12-5.373 12-12-5.372-12-12-12zm.284 21.6c-4.99 0-9.04-4.05-9.04-9.04S7.294 3.52 12.284 3.52c4.99 0 9.04 4.05 9.04 9.04s-4.05 9.04-9.04 9.04zm-.885-9.352l-2.48 2.48-1.41-1.41 3.89-3.89 1.41 1.41-2.48 2.48.001.001zm3.89-.001l2.48-2.48 1.41 1.41-3.89 3.89-1.41-1.41 2.48-2.48.001-.001zM12.284 6.57c-2.02 0-3.66 1.63-3.66 3.66h2.17c0-.82.67-1.49 1.49-1.49s1.49.67 1.49 1.49c0 .82-.67 1.49-1.49 1.49h-1.63v2.17h1.63c2.02 0 3.66-1.63 3.66-3.66s-1.64-3.66-3.66-3.66z"/>
              </svg>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="providerTooltip('wechat')" placement="top">
            <el-button
              circle
              size="large"
              class="social-icon wechat"
              :class="{ unavailable: !isProviderAvailable('wechat') }"
              :aria-disabled="!isProviderAvailable('wechat')"
              @click="startOAuthLogin('wechat')"
            >
              <i class="fa-brands fa-weixin"></i>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="providerTooltip('qq')" placement="top">
            <el-button
              circle
              size="large"
              class="social-icon qq"
              :class="{ unavailable: !isProviderAvailable('qq') }"
              :aria-disabled="!isProviderAvailable('qq')"
              @click="startOAuthLogin('qq')"
            >
              <i class="fa-brands fa-qq"></i>
            </el-button>
          </el-tooltip>
        </div>
      </div>
      
      <div class="switch-link">
        还没有账号? 
        <router-link to="/auth/register" class="link">立即注册</router-link>
      </div>

    <VerificationModal ref="verificationModalRef" @success="onVerificationSuccess" @failure="onVerificationFailure" @cancel="onVerificationCancel" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { loginUser } from '@/api/auth';
import { oauthApi, type OAuthProviderStatus } from '@/api/oauth';
import VerificationModal from '@/components/VerificationModal.vue';
import { User, Lock } from '@element-plus/icons-vue';

// 登录页面：用户名/邮箱密码登录前先通过编程题验证，同时展示 GitHub/Gitee 等授权入口状态。

const router = useRouter();
const route = useRoute();
const loginFormRef = ref();
const verificationModalRef = ref();
const isLoading = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
  remember: false,
});

// 登录账号如果是邮箱就按邮箱规则统一小写；用户名保持原样，避免影响用户名大小写习惯。
const normalizeLoginAccount = () => {
  const value = String(loginForm.username || '').trim();
  loginForm.username = value.includes('@') ? value.toLowerCase() : value;
};

const providers = ref<OAuthProviderStatus[]>([]);

const loginRules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

// 解析登录后的安全跳转地址，避免跳到外部或认证页自身。
const resolveSafeRedirect = () => {
  const redirect = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect;
  if (
    typeof redirect === 'string' &&
    redirect.startsWith('/') &&
    !redirect.startsWith('//') &&
    !redirect.startsWith('/auth')
  ) {
    return redirect;
  }
  return '/';
};

const providerMap = () => Object.fromEntries(providers.value.map(item => [item.provider, item]));

const isProviderAvailable = (provider: string) => {
  const item = providerMap()[provider];
  return !!item?.available;
};

const providerTooltip = (provider: string) => {
  const item = providerMap()[provider];
  if (!item) return '正在读取授权状态...';
  return item.available ? `通过 ${item.label} 登录` : `${item.label}：${item.message || '暂不可用'}`;
};

// 读取第三方授权平台状态，不可用的平台在页面上保留但给出提示。
const loadOAuthProviders = async () => {
  try {
    const res = await oauthApi.getProviders();
    providers.value = Array.isArray(res) ? res : [];
  } catch {
    providers.value = [];
  }
};

// 发起第三方授权登录，后端返回授权地址后跳转到平台。
const startOAuthLogin = async (provider: string) => {
  if (!isProviderAvailable(provider)) {
    ElMessage.warning(providerTooltip(provider));
    return;
  }
  try {
    const res: any = await oauthApi.getAuthorizeUrl(provider, 'login', resolveSafeRedirect());
    if (res?.url) {
      window.location.href = res.url;
    } else {
      ElMessage.error('授权地址生成失败');
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '授权登录暂不可用');
  }
};

// 点击登录后先打开编程题验证弹窗，验证成功后再真正调用登录接口。
const handleLogin = () => {
  normalizeLoginAccount();
  loginFormRef.value.validate((valid: boolean) => {
    if (valid) {
      isLoading.value = true;
      verificationModalRef.value.open();
    }
  });
};

// 验证成功后携带 verificationToken 登录，成功后保存 token 和用户信息。
const onVerificationSuccess = async (token: string) => {
  try {
    const response = await loginUser({
      username: loginForm.username,
      password: loginForm.password,
      verificationToken: token,
      rememberMe: loginForm.remember,
    });
    
    // 检查响应是否成功
    if (response && response.token) {
      localStorage.setItem('authToken', response.token);
      localStorage.setItem('userInfo', JSON.stringify(response.user));
      ElMessage.success('登录成功！');
      
      // 触发自定义事件，通知App组件更新用户状态
      window.dispatchEvent(new Event('user-login'));
      
      await nextTick();
      await router.replace(resolveSafeRedirect());
    } else {
      console.error('登录响应格式不正确:', response);
      ElMessage.error('登录失败：服务器响应格式不正确');
      isLoading.value = false;
    }
  } catch (error: any) {
    console.error('登录失败:', error);
    ElMessage.error(error.response?.data?.message || '登录失败，请稍后重试');
    isLoading.value = false;
  }
};

const onVerificationFailure = (message: string) => {
  ElMessage.error(message);
  isLoading.value = false;
};

// 处理验证窗口关闭的情况
const onVerificationCancel = () => {
  console.log('验证窗口被关闭');
  isLoading.value = false;
};

onMounted(() => {
  loadOAuthProviders();
});
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css');

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

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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

.social-login {
  margin-top: 30px;
  text-align: center;
}

.social-icons {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.social-icon {
  font-size: 20px;
  border: none;
  transition: all 0.3s ease;
}

.social-icon.github {
  color: #333;
}
.social-icon.gitee {
   color: #C71D23;
}

.social-icon.wechat {
  color: #07c160;
}

.social-icon.qq {
  color: #1e6fff;
}

.social-icon:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.social-icon.unavailable {
  opacity: 0.52;
  filter: grayscale(0.35);
}

.switch-link {
  margin-top: 30px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}
</style> 
