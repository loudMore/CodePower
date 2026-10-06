<!-- 文件说明：第三方授权回调页面，处理未绑定账号时的绑定或注册流程。 -->
<template>
  <div class="oauth-page">
    <el-card class="oauth-card" shadow="hover" v-loading="loading">
      <div v-if="errorMessage" class="oauth-state">
        <h2>授权没有完成</h2>
        <p>{{ errorMessage }}</p>
        <el-button type="primary" @click="router.replace('/auth/login')">返回登录</el-button>
      </div>

      <div v-else-if="linkSession" class="oauth-content">
        <div class="oauth-header">
          <img
            v-if="linkSession.providerAvatar"
            :src="linkSession.providerAvatar"
            class="oauth-avatar"
            alt=""
          />
          <div class="oauth-avatar fallback" v-else>{{ linkSession.providerLabel?.slice(0, 1) }}</div>
          <div>
            <h2>{{ linkSession.providerLabel }} 尚未绑定 CodePower 账号</h2>
            <p>{{ displayProviderAccount }}</p>
          </div>
        </div>

        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="请选择绑定已有账号，或按原注册流程创建新账号并自动完成绑定。"
        />

        <el-tabs v-model="activeTab" class="oauth-tabs">
          <el-tab-pane label="已有账号" name="existing">
            <el-form
              ref="existingFormRef"
              :model="existingForm"
              :rules="existingRules"
              label-position="top"
              class="oauth-form"
            >
              <el-form-item label="用户名或邮箱" prop="account">
                <el-input v-model="existingForm.account" size="large" placeholder="输入 CodePower 用户名或邮箱" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="existingForm.password" type="password" show-password size="large" placeholder="输入 CodePower 密码" />
              </el-form-item>
              <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="bindExisting">
                验证并绑定登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="新用户注册" name="new">
            <el-form
              ref="createFormRef"
              :model="createForm"
              :rules="createRules"
              label-position="top"
              class="oauth-form"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="createForm.username" size="large" placeholder="设置用户名" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input
                  v-model="createForm.email"
                  type="email"
                  autocomplete="email"
                  :spellcheck="false"
                  size="large"
                  placeholder="用于登录、找回密码和通知"
                  @blur="normalizeCreateEmail"
                />
              </el-form-item>
              <el-form-item label="地区" prop="region">
                <el-select v-model="createForm.region" filterable clearable placeholder="选填：请选择省级地区" size="large" style="width: 100%">
                  <el-option
                    v-for="region in REGION_OPTIONS"
                    :key="region.value"
                    :label="region.label"
                    :value="region.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="邮箱验证码" prop="verificationCode">
                <div class="code-row">
                  <el-input v-model="createForm.verificationCode" size="large" placeholder="输入验证码" />
                  <el-button size="large" :disabled="sendingCode || countdown > 0" :loading="sendingCode" @click="requestEmailCode">
                    {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="createForm.password" type="password" show-password size="large" placeholder="设置登录密码" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="createForm.confirmPassword" type="password" show-password size="large" placeholder="再次输入密码" />
              </el-form-item>
              <el-form-item prop="agreement">
                <el-checkbox v-model="createForm.agreement">
                  我已阅读并同意
                  <router-link to="/terms" class="link">服务条款</router-link>
                </el-checkbox>
              </el-form-item>
              <el-button type="primary" size="large" class="full-button" :loading="submitting" @click="createAccount">
                注册并绑定登录
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-else class="oauth-state">
        <h2>正在处理授权登录</h2>
        <p>请稍候...</p>
      </div>
    </el-card>

    <VerificationModal
      ref="verificationModalRef"
      @success="onVerificationSuccess"
      @failure="message => ElMessage.error(message)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { oauthApi } from '@/api/oauth'
import { sendEmailCode } from '@/api/auth'
import VerificationModal from '@/components/VerificationModal.vue'
import { REGION_OPTIONS } from '@/utils/regions'

// OAuth 回调页：处理 GitHub/Gitee 授权后的登录、绑定已有账号或新账号注册绑定。
const route = useRoute()
const router = useRouter()

// 页面流程状态：loading 显示授权处理，linkSession 表示外部账号尚未绑定本系统账号。
const loading = ref(true)
const submitting = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const errorMessage = ref('')
const activeTab = ref('existing')
const linkSession = ref<any | null>(null)
const existingFormRef = ref<FormInstance>()
const createFormRef = ref<FormInstance>()
const verificationModalRef = ref()

// 已有账号绑定表单：用本系统用户名/邮箱 + 密码确认账号归属。
const existingForm = reactive({
  account: '',
  password: ''
})

// 新账号注册绑定表单：第三方授权后仍要求邮箱验证码和密码。
const createForm = reactive({
  username: '',
  email: '',
  region: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
  agreement: false
})

// 第三方授权补全注册也走同一套邮箱规范，验证码发送和注册绑定使用同一个小写邮箱键。
const normalizeEmailValue = (email: string) => String(email || '').trim().toLowerCase()

const normalizeCreateEmail = () => {
  createForm.email = normalizeEmailValue(createForm.email)
}

// 展示授权平台返回的昵称、用户名和邮箱，帮助用户确认当前绑定对象。
const displayProviderAccount = computed(() => {
  if (!linkSession.value) return ''
  const name = linkSession.value.providerNickname || linkSession.value.providerUsername || '已授权账号'
  const email = linkSession.value.providerEmail ? ` · ${linkSession.value.providerEmail}` : ''
  return `${name}${email}`
})

const existingRules: FormRules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== createForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const createRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线和中文', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  verificationCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  agreement: [{
    validator: (_rule: any, value: boolean, callback: any) => {
      value ? callback() : callback(new Error('请先同意服务条款'))
    },
    trigger: 'change'
  }]
}

// 读取 query 参数，兼容 Vue Router 中同名参数变成数组的情况。
const firstQuery = (name: string) => {
  const value = route.query[name]
  return Array.isArray(value) ? value[0] : value
}

// 登录后只允许跳转站内路径，避免 OAuth redirect 被外部地址利用。
const safeRedirect = (redirect?: unknown) => {
  if (
    typeof redirect === 'string' &&
    redirect.startsWith('/') &&
    !redirect.startsWith('//') &&
    !redirect.startsWith('/auth')
  ) {
    return redirect
  }
  return '/'
}

// 授权登录或绑定完成后保存本系统 Token，并通知顶层 App 更新登录态。
const finishLogin = async (payload: any) => {
  if (!payload?.token || !payload?.user) {
    throw new Error('登录响应不完整')
  }
  localStorage.setItem('authToken', payload.token)
  localStorage.setItem('userInfo', JSON.stringify(payload.user))
  window.dispatchEvent(new Event('user-login'))
  ElMessage.success('登录成功')
  await router.replace(safeRedirect(payload.redirect))
}

// 已有账号绑定：验证本系统账号密码，通过后后端把外部身份写入绑定表。
const bindExisting = () => {
  existingFormRef.value?.validate(async valid => {
    if (!valid || !linkSession.value) return
    try {
      await ElMessageBox.confirm(
        `确认将当前 ${linkSession.value.providerLabel} 授权账号绑定到该 CodePower 账号吗？`,
        '绑定确认',
        { confirmButtonText: '确认绑定', cancelButtonText: '取消', type: 'warning' }
      )
      submitting.value = true
      const payload = await oauthApi.bindExisting(linkSession.value.sessionId, {
        account: existingForm.account,
        password: existingForm.password
      })
      await finishLogin(payload)
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error(error.response?.data?.message || error.message || '绑定失败')
      }
    } finally {
      submitting.value = false
    }
  })
}

// 新用户注册并绑定：必须通过邮箱验证码，注册成功后直接登录。
const createAccount = () => {
  normalizeCreateEmail()
  createFormRef.value?.validate(async valid => {
    if (!valid || !linkSession.value) return
    submitting.value = true
    try {
      const payload = await oauthApi.createAccount(linkSession.value.sessionId, {
        username: createForm.username,
        email: normalizeEmailValue(createForm.email),
        region: createForm.region || undefined,
        verificationCode: createForm.verificationCode,
        password: createForm.password,
        confirmPassword: createForm.confirmPassword
      })
      await finishLogin(payload)
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || error.message || '注册绑定失败')
    } finally {
      submitting.value = false
    }
  })
}

// 发送邮箱验证码前先弹出编程题验证，和普通注册流程保持一致。
const requestEmailCode = () => {
  normalizeCreateEmail()
  createFormRef.value?.validateField('email', (valid) => {
    if (valid) {
      verificationModalRef.value?.open()
    } else {
      ElMessage.warning('请输入有效邮箱')
    }
  })
}

// 编程题验证成功后真正发送邮箱验证码，并启动倒计时。
const onVerificationSuccess = async () => {
  sendingCode.value = true
  try {
    normalizeCreateEmail()
    await sendEmailCode(createForm.email)
    ElMessage.success('验证码已发送，请注意查收')
    countdown.value = 60
    const timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) window.clearInterval(timer)
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

// 根据 OAuth 回调参数进入不同流程：已绑定直接登录，未绑定进入账号绑定页。
const load = async () => {
  const bindSuccess = firstQuery('oauth_bind')
  const bindError = firstQuery('oauth_bind_error')
  if (bindSuccess) {
    ElMessage.success('第三方账号绑定成功')
    await router.replace('/profile/settings')
    return
  }
  if (bindError) {
    ElMessage.error(String(bindError))
    await router.replace('/profile/settings')
    return
  }

  const oauthError = firstQuery('oauth_error')
  if (oauthError) {
    errorMessage.value = String(oauthError)
    loading.value = false
    return
  }

  const loginId = firstQuery('loginId')
  if (loginId) {
    try {
      const payload = await oauthApi.consumeLoginSession(String(loginId))
      await finishLogin(payload)
    } catch (error: any) {
      errorMessage.value = error.response?.data?.message || error.message || '登录会话已过期'
    } finally {
      loading.value = false
    }
    return
  }

  const sessionId = firstQuery('sessionId')
  if (sessionId) {
    try {
      const session = await oauthApi.getLinkSession(String(sessionId))
      linkSession.value = session
      createForm.email = normalizeEmailValue(session.providerEmail || '')
      createForm.username = (session.providerUsername || session.providerNickname || '')
        .replace(/[^\w\u4e00-\u9fa5]/g, '')
        .slice(0, 20)
    } catch (error: any) {
      errorMessage.value = error.response?.data?.message || error.message || '授权会话已过期'
    } finally {
      loading.value = false
    }
    return
  }

  errorMessage.value = '授权回调缺少必要参数'
  loading.value = false
}

onMounted(load)
</script>

<style scoped>
.oauth-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f4f6f8;
}

.oauth-card {
  width: min(620px, 100%);
  border-radius: 8px;
}

.oauth-state {
  text-align: center;
  padding: 32px 12px;
}

.oauth-state h2 {
  margin: 0 0 12px;
  color: #303133;
}

.oauth-state p {
  color: #606266;
}

.oauth-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.oauth-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #303133;
}

.oauth-header p {
  margin: 0;
  color: #606266;
}

.oauth-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  background: #eef2f7;
}

.oauth-avatar.fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  font-weight: 700;
  font-size: 22px;
}

.oauth-tabs {
  margin-top: 18px;
}

.oauth-form {
  padding-top: 8px;
}

.full-button {
  width: 100%;
}

.code-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.link {
  color: #409eff;
  text-decoration: none;
}

@media (max-width: 640px) {
  .oauth-page {
    padding: 12px;
  }

  .oauth-header {
    align-items: flex-start;
  }

  .code-row {
    flex-direction: column;
  }
}
</style>
