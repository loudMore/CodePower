<!-- 账户设置 — 修改密码、换绑邮箱 -->
<template>
  <div class="settings-container">
    <el-card shadow="hover" class="settings-card">
      <template #header>
        <div class="card-header">
          <h2>账户设置</h2>
        </div>
      </template>

      <el-form label-width="120px" class="settings-form">
        <el-form-item label="账号邮箱">
          <div class="settings-value">{{ userInfo.email }}</div>
          <el-button type="primary" link @click="showChangeEmailDialog = true">修改邮箱</el-button>
        </el-form-item>

        <el-form-item label="账号密码">
          <div class="settings-value">********</div>
          <el-button type="primary" link @click="showChangePasswordDialog = true">修改密码</el-button>
        </el-form-item>

        <el-divider />

        <el-form-item label="授权登录">
          <div class="oauth-bind-list">
            <div v-for="item in oauthBindings" :key="item.provider" class="oauth-bind-row">
              <div class="oauth-bind-main">
                <span class="oauth-dot" :class="`oauth-${item.provider}`"></span>
                <div>
                  <div class="oauth-bind-title">{{ item.label }}</div>
                  <div class="oauth-bind-sub">
                    <template v-if="item.bound">
                      已绑定 {{ item.providerNickname || item.providerUsername || '授权账号' }}
                    </template>
                    <template v-else>
                      {{ item.available ? '未绑定' : item.message }}
                    </template>
                  </div>
                </div>
              </div>
              <div class="oauth-bind-actions">
                <el-button
                  v-if="item.bound"
                  type="danger"
                  link
                  :loading="oauthActionLoading === item.provider"
                  @click="unlinkOAuth(item.provider)"
                >
                  解绑
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  link
                  :class="{ unavailable: !item.available }"
                  :loading="oauthActionLoading === item.provider"
                  @click="bindOAuth(item.provider)"
                >
                  绑定
                </el-button>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-divider />

        <el-form-item label="通知设置">
          <el-switch v-model="notificationSettings.email" active-text="邮件通知" />
        </el-form-item>

        <el-form-item label="订阅周报">
          <el-switch v-model="notificationSettings.newsletter" active-text="每周接收学习提醒" />
        </el-form-item>

        <el-divider />

        <el-form-item label="界面设置">
          <el-form-item label="深色模式" label-width="100px">
            <el-switch v-model="interfaceSettings.darkMode" />
          </el-form-item>

          <el-form-item label="代码编辑器" label-width="100px">
            <el-select v-model="interfaceSettings.editorTheme" placeholder="选择主题">
              <el-option label="默认主题" value="default" />
              <el-option label="暗黑主题" value="dark" />
              <el-option label="高对比度" value="highcontrast" />
            </el-select>
          </el-form-item>

          <el-form-item label="代码字体大小" label-width="100px">
            <el-slider v-model="interfaceSettings.fontSize" :min="12" :max="20" show-stops />
          </el-form-item>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveSettings">保存设置</el-button>
          <el-button @click="resetSettings">恢复默认</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>

  <!-- 修改邮箱对话框 -->
  <el-dialog v-model="showChangeEmailDialog" title="修改邮箱" width="500px" @close="resetEmailForm">
    <el-form :model="emailForm" label-width="100px" :rules="emailRules" ref="emailFormRef">
      <el-form-item label="当前密码" prop="password">
        <el-input v-model="emailForm.password" type="password" placeholder="请输入当前密码验证身份" show-password />
      </el-form-item>
      <el-form-item label="新邮箱地址" prop="newEmail">
        <el-input
          v-model="emailForm.newEmail"
          type="email"
          autocomplete="email"
          :spellcheck="false"
          placeholder="请输入新邮箱地址"
          @blur="normalizeEmailInput"
        />
      </el-form-item>
      <el-form-item label="邮箱验证码" prop="verificationCode">
        <div class="code-input-row">
          <el-input v-model="emailForm.verificationCode" placeholder="请输入验证码" />
          <el-button @click="sendEmailCode" :disabled="emailCodeCountdown > 0 || isSendingEmailCode" :loading="isSendingEmailCode">
            {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="showChangeEmailDialog = false">取消</el-button>
        <el-button type="primary" @click="changeEmail" :loading="isChangingEmail">确认修改</el-button>
      </span>
    </template>
  </el-dialog>

  <!-- 修改密码对话框 -->
  <el-dialog v-model="showChangePasswordDialog" title="修改密码" width="500px" @close="resetPasswordForm">
    <el-form :model="passwordForm" label-width="100px" :rules="passwordRules" ref="passwordFormRef">
      <el-form-item label="当前密码" prop="currentPassword">
        <el-input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="showChangePasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="changePassword" :loading="isChangingPassword">确认修改</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import { oauthApi, type OAuthProviderStatus } from '@/api/oauth'

const router = useRouter()

const userInfo = reactive({
  id: 0,
  username: '',
  email: '',
})

const notificationSettings = reactive({
  email: true,
  newsletter: true
})

const interfaceSettings = reactive({
  darkMode: false,
  editorTheme: 'default',
  fontSize: 14
})

const oauthBindings = ref<OAuthProviderStatus[]>([])
const oauthActionLoading = ref('')

// ========== 修改邮箱 ==========
const showChangeEmailDialog = ref(false)
const emailFormRef = ref<FormInstance>()
const isSendingEmailCode = ref(false)
const isChangingEmail = ref(false)
const emailCodeCountdown = ref(0)
const emailForm = reactive({
  password: '',
  newEmail: '',
  verificationCode: ''
})

const normalizeEmailValue = (email: string) => String(email || '').trim().toLowerCase()

const normalizeEmailInput = () => {
  emailForm.newEmail = normalizeEmailValue(emailForm.newEmail)
}

const emailRules = reactive<FormRules>({
  password: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newEmail: [
    { required: true, message: '请输入新邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' }
  ]
})

const sendEmailCode = async () => {
  normalizeEmailInput()
  if (!emailForm.password) {
    ElMessage.warning('请先输入当前密码')
    return
  }
  if (!emailForm.newEmail) {
    ElMessage.warning('请先输入新邮箱地址')
    return
  }
  isSendingEmailCode.value = true
  try {
    await userApi.sendChangeEmailCode(emailForm.newEmail, emailForm.password)
    ElMessage.success('验证码已发送到新邮箱')
    emailCodeCountdown.value = 60
    const timer = setInterval(() => {
      emailCodeCountdown.value--
      if (emailCodeCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || error.message || '发送验证码失败')
  } finally {
    isSendingEmailCode.value = false
  }
}

const changeEmail = () => {
  normalizeEmailInput()
  emailFormRef.value?.validate(async (valid) => {
    if (valid) {
      isChangingEmail.value = true
      try {
        await userApi.changeEmail(emailForm.password, emailForm.newEmail, emailForm.verificationCode)
        ElMessage.success('邮箱修改成功')
        showChangeEmailDialog.value = false
        userInfo.email = normalizeEmailValue(emailForm.newEmail)
        const stored = localStorage.getItem('userInfo')
        if (stored) {
          try {
            const info = JSON.parse(stored)
            info.email = normalizeEmailValue(emailForm.newEmail)
            localStorage.setItem('userInfo', JSON.stringify(info))
          } catch {}
        }
        resetEmailForm()
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || error.message || '邮箱修改失败')
      } finally {
        isChangingEmail.value = false
      }
    }
  })
}

const resetEmailForm = () => {
  emailForm.password = ''
  emailForm.newEmail = ''
  emailForm.verificationCode = ''
  emailCodeCountdown.value = 0
}

// ========== 修改密码 ==========
const showChangePasswordDialog = ref(false)
const passwordFormRef = ref<FormInstance>()
const isChangingPassword = ref(false)
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = reactive<FormRules>({
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const changePassword = () => {
  passwordFormRef.value?.validate(async (valid) => {
    if (valid) {
      isChangingPassword.value = true
      try {
        await userApi.changePassword(passwordForm.currentPassword, passwordForm.newPassword)
        ElMessage.success('密码修改成功')
        showChangePasswordDialog.value = false
        resetPasswordForm()
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || error.message || '密码修改失败')
      } finally {
        isChangingPassword.value = false
      }
    }
  })
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

// ========== 第三方授权登录绑定 ==========
const loadOAuthBindings = async () => {
  try {
    const res = await oauthApi.getMyBindings()
    oauthBindings.value = Array.isArray(res) ? res : []
  } catch {
    oauthBindings.value = []
  }
}

const bindOAuth = async (provider: string) => {
  const item = oauthBindings.value.find(binding => binding.provider === provider)
  if (!item?.available) {
    ElMessage.warning(item ? `${item.label}：${item.message || '暂不可用'}` : '授权状态暂未加载')
    return
  }
  oauthActionLoading.value = provider
  try {
    const res: any = await oauthApi.getAuthorizeUrl(provider, 'bind', '/profile/settings')
    if (res?.url) {
      window.location.href = res.url
    } else {
      ElMessage.error('授权地址生成失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || error.message || '暂时无法发起绑定')
  } finally {
    oauthActionLoading.value = ''
  }
}

const unlinkOAuth = async (provider: string) => {
  try {
    await ElMessageBox.confirm(
      '解绑后将不能再使用该平台登录当前账号，但仍可使用用户名/邮箱和密码登录。',
      '解绑授权账号',
      { confirmButtonText: '确认解绑', cancelButtonText: '取消', type: 'warning' }
    )
    oauthActionLoading.value = provider
    await oauthApi.unlink(provider)
    ElMessage.success('解绑成功')
    await loadOAuthBindings()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || '解绑失败')
    }
  } finally {
    oauthActionLoading.value = ''
  }
}

// ========== 通用设置 ==========
const saveSettings = () => {
  localStorage.setItem('interfaceSettings', JSON.stringify(interfaceSettings))
  localStorage.setItem('notificationSettings', JSON.stringify(notificationSettings))
  ElMessage.success('设置已保存')
}

const resetSettings = async () => {
  try {
    await ElMessageBox.confirm('确定要恢复默认设置吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    Object.assign(interfaceSettings, { darkMode: false, editorTheme: 'default', fontSize: 14 })
    Object.assign(notificationSettings, { email: true, newsletter: true })
    ElMessage.success('设置已重置为默认值')
  } catch {}
}

const loadUserInfo = () => {
  const stored = localStorage.getItem('userInfo')
  if (stored) {
    try {
      const data = JSON.parse(stored)
      userInfo.id = data.id || 0
      userInfo.username = data.username || ''
      userInfo.email = data.email || ''
    } catch {}
  }
}

onMounted(() => {
  const authToken = localStorage.getItem('authToken')
  if (!authToken) {
    ElMessage.warning('请先登录')
    router.push('/auth/login?redirect=/profile/settings')
    return
  }
  loadUserInfo()
  loadOAuthBindings()

  const savedInterface = localStorage.getItem('interfaceSettings')
  if (savedInterface) {
    try { Object.assign(interfaceSettings, JSON.parse(savedInterface)) } catch {}
  }
  const savedNotification = localStorage.getItem('notificationSettings')
  if (savedNotification) {
    try { Object.assign(notificationSettings, JSON.parse(savedNotification)) } catch {}
  }
})
</script>

<style scoped>
.settings-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.settings-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.settings-form {
  max-width: 600px;
}

.settings-value {
  display: inline-block;
  margin-right: 10px;
  font-weight: 500;
}

.code-input-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.oauth-bind-list {
  width: min(520px, 100%);
  display: grid;
  gap: 10px;
}

.oauth-bind-row {
  min-height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.oauth-bind-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.oauth-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex: 0 0 auto;
  background: #909399;
}

.oauth-github { background: #303133; }
.oauth-gitee { background: #c71d23; }
.oauth-wechat { background: #07c160; }
.oauth-qq { background: #1e6fff; }

.oauth-bind-title {
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.oauth-bind-sub {
  margin-top: 3px;
  color: #909399;
  font-size: 13px;
  line-height: 1.3;
}

.oauth-bind-actions {
  flex: 0 0 auto;
}

.oauth-bind-actions .unavailable {
  color: #909399;
}

@media (max-width: 768px) {
  .settings-form {
    max-width: 100%;
  }

  .el-form-item {
    margin-bottom: 22px;
  }

  .oauth-bind-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>
