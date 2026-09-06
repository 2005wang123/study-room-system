<!-- src/components/LoginModal.vue -->
<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <button class="modal-close" @click="handleClose">✕</button>

      <div class="modal-header">
        <div class="modal-icon">📚</div>
        <h2>图书馆预约系统</h2>
        <p class="modal-subtitle">使用学号登录</p>
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>学号</label>
          <input
            v-model="loginForm.username"
            type="text"
            placeholder="请输入学号"
            required
            autocomplete="username"
            @input="validateUsername"
          />
          <span v-if="usernameError" class="field-error">{{ usernameError }}</span>
          <span v-else class="input-hint">例: 2024001</span>
        </div>

        <div class="form-group">
          <label>密码</label>
          <div class="password-wrapper">
            <input
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              required
              autocomplete="current-password"
              @input="validatePassword"
            />
            <button type="button" class="password-toggle" @click="showPassword = !showPassword">
              {{ showPassword ? '🙈' : '👁️' }}
            </button>
          </div>
          <span v-if="passwordError" class="field-error">{{ passwordError }}</span>
          <span v-else class="input-hint">初始密码为身份证号后6位</span>
        </div>

        <div v-if="errorMessage" class="error-message">⚠️ {{ errorMessage }}</div>

        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>

      <!-- 修改密码区域（登录后显示） -->
      <div v-if="showChangePassword" class="change-password-section">
        <div class="first-login-tip">✅ 登录成功！首次登录/密码过于简单，请先修改初始密码</div>
        <div class="divider">
          <span>修改密码</span>
        </div>
        <form @submit.prevent="handleChangePassword" class="change-password-form">
          <div class="form-group">
            <label>当前密码</label>
            <input v-model="changePwdForm.oldPassword" type="password" placeholder="请输入当前密码" required />
          </div>

          <div class="form-group">
            <label>新密码</label>
            <input
              v-model="changePwdForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              required
              @input="checkPasswordStrength"
            />
            <div v-if="changePwdForm.newPassword" class="password-strength">
              <div class="strength-bar">
                <div
                  class="strength-fill"
                  :class="passwordStrength.class"
                  :style="{ width: passwordStrength.percentage + '%' }"
                ></div>
              </div>
              <span class="strength-text">{{ passwordStrength.text }}</span>
              <span v-if="passwordStrength.isWeak" class="strength-warning">
                ⚠️ 密码过于简单，建议包含大小写字母、数字和特殊字符
              </span>
            </div>
          </div>

          <div class="form-group">
            <label>确认新密码</label>
            <input v-model="changePwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" required />
          </div>

          <div v-if="changePwdError" class="error-message">⚠️ {{ changePwdError }}</div>
          <div v-if="changePwdSuccess" class="success-message">✅ 密码修改成功！请重新登录</div>

          <button type="submit" class="change-pwd-btn" :disabled="changePwdLoading">
            {{ changePwdLoading ? '修改中...' : '修改密码' }}
          </button>
          <button type="button" class="cancel-change-btn" @click="cancelChangePassword">取消</button>
        </form>
      </div>

      <div class="modal-footer">
        <button v-if="!showChangePassword && isLoggedIn" class="change-pwd-link" @click="showChangePassword = true">
          🔑 修改密码
        </button>
        <span class="footer-hint">首次登录请及时修改初始密码</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { login, changePassword } from '@/api/user'
import { ref, watch, computed } from 'vue'
import type { ApiErrorShape } from '@/types/api'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'login-success'])

// 登录表单
const loginForm = ref({
  username: '',
  password: ''
})

const showPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const isLoggedIn = ref(false)

// 表单验证
const usernameError = ref('')
const passwordError = ref('')

const validateUsername = () => {
  const username = loginForm.value.username.trim()
  if (!username) {
    usernameError.value = '请输入学号'
  } else if (!/^\d{6,12}$/.test(username)) {
    usernameError.value = '学号应为6-12位数字'
  } else {
    usernameError.value = ''
  }
}

const validatePassword = () => {
  const password = loginForm.value.password
  if (!password) {
    passwordError.value = '请输入密码'
  } else if (password.length < 6) {
    passwordError.value = '密码长度不能少于6位'
  } else {
    passwordError.value = ''
  }
}

// 修改密码
const showChangePassword = ref(false)
const changePwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const changePwdLoading = ref(false)
const changePwdError = ref('')
const changePwdSuccess = ref(false)

// 密码强度检测
const checkPasswordStrength = () => {
  const pwd = changePwdForm.value.newPassword
  if (!pwd) return 0

  let score = 0
  if (pwd.length >= 8) score += 1
  if (pwd.length >= 12) score += 1
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score += 1
  if (/\d/.test(pwd)) score += 1
  if (/[^a-zA-Z0-9]/.test(pwd)) score += 1

  return score
}

const passwordStrength = computed(() => {
  const score = checkPasswordStrength()
  if (!score) return { class: '', text: '', percentage: 0, isWeak: false }

  if (score <= 2) {
    return { class: 'weak', text: '弱', percentage: 30, isWeak: true }
  } else if (score <= 3) {
    return { class: 'medium', text: '中', percentage: 60, isWeak: true }
  } else {
    return { class: 'strong', text: '强', percentage: 100, isWeak: false }
  }
})

// 登录处理
const handleLogin = async () => {
  errorMessage.value = ''

  // 前端验证
  validateUsername()
  validatePassword()
  if (usernameError.value || passwordError.value) {
    return
  }

  loading.value = true

  try {
    const res = await login({
      username: loginForm.value.username.trim(),
      password: loginForm.value.password
    })

    if (res.code === 200) {
      isLoggedIn.value = true
      emit('login-success', res.data)
      // 检查是否需要修改密码（首次登录或密码过于简单）
      if (res.data.needChangePassword || res.data.isFirstLogin) {
        showChangePassword.value = true
      } else {
        // 不需要修改密码，直接关闭弹窗
        emit('update:visible', false)
      }
    } else {
      errorMessage.value = res.message || '登录失败，请检查学号和密码'
    }
  } catch (err) {
    errorMessage.value = (err as ApiErrorShape).message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

// 修改密码处理
const handleChangePassword = async () => {
  changePwdError.value = ''
  changePwdSuccess.value = false

  // 前端验证
  if (!changePwdForm.value.oldPassword) {
    changePwdError.value = '请输入当前密码'
    return
  }

  if (changePwdForm.value.newPassword !== changePwdForm.value.confirmPassword) {
    changePwdError.value = '两次输入的密码不一致'
    return
  }

  if (changePwdForm.value.newPassword.length < 6) {
    changePwdError.value = '密码长度不能少于6位'
    return
  }

  // 检查密码强度
  if (passwordStrength.value.isWeak) {
    if (!confirm('密码过于简单，是否继续使用？建议使用包含大小写字母、数字和特殊字符的密码。')) {
      return
    }
  }

  changePwdLoading.value = true

  try {
    const res = await changePassword({
      oldPassword: changePwdForm.value.oldPassword,
      newPassword: changePwdForm.value.newPassword,
      confirmPassword: changePwdForm.value.confirmPassword
    })

    if (res.code === 200) {
      changePwdSuccess.value = true
      // 修改密码成功后，提示用户重新登录
      setTimeout(() => {
        // 清除登录状态和 token
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        isLoggedIn.value = false
        showChangePassword.value = false
        changePwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
        changePwdSuccess.value = false
        // 关闭弹窗
        emit('update:visible', false)
        // 触发未授权事件，让父组件清除用户信息
        window.dispatchEvent(new CustomEvent('unauthorized'))
      }, 2000)
    } else {
      changePwdError.value = res.message || '修改密码失败'
    }
  } catch (err) {
    changePwdError.value = (err as ApiErrorShape).message || '网络错误，请重试'
  } finally {
    changePwdLoading.value = false
  }
}

// 取消修改密码
const cancelChangePassword = () => {
  showChangePassword.value = false
  changePwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  changePwdError.value = ''
  changePwdSuccess.value = false
}

// 关闭弹窗
const handleClose = () => {
  emit('update:visible', false)
}

// 重置表单
watch(
  () => props.visible,
  (newVal) => {
    if (!newVal) {
      loginForm.value = { username: '', password: '' }
      errorMessage.value = ''
      usernameError.value = ''
      passwordError.value = ''
      changePwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      changePwdError.value = ''
      changePwdSuccess.value = false
      showChangePassword.value = false
    }
  }
)
</script>

<style scoped>
/* --- 弹窗遮罩 --- */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* --- 弹窗内容 --- */
.modal-content {
  background: var(--bg-primary, #1a1a2e);
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  border-radius: 16px;
  padding: 32px;
  width: 90%;
  max-width: 420px;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  animation: slideUp 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  background: none;
  border: none;
  color: var(--text-secondary, rgba(255, 255, 255, 0.6));
  font-size: 18px;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: all 0.2s;
  line-height: 1;
}

.modal-close:hover {
  background: var(--sidebar-hover, rgba(255, 255, 255, 0.06));
  color: var(--text-primary, #ffffff);
}

/* --- 弹窗头部 --- */
.modal-header {
  text-align: center;
  margin-bottom: 24px;
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.modal-header h2 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-primary, #ffffff);
}

.modal-subtitle {
  font-size: 14px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.6));
}

/* --- 表单 --- */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary, #ffffff);
}

.form-group input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.15));
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-primary, #ffffff);
  font-size: 14px;
  transition: all 0.2s;
  outline: none;
}

.form-group input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.form-group input::placeholder {
  color: var(--text-secondary, rgba(255, 255, 255, 0.4));
}

.input-hint {
  font-size: 12px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.4));
}

.field-error {
  font-size: 12px;
  color: #f44336;
}

/* --- 密码显示切换 --- */
.password-wrapper {
  position: relative;
}

.password-wrapper input {
  padding-right: 44px;
}

.password-toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.6));
  transition: opacity 0.2s;
}

.password-toggle:hover {
  opacity: 0.8;
}

/* --- 错误消息 --- */
.error-message {
  background: rgba(244, 67, 54, 0.1);
  color: #f44336;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
}

/* --- 登录按钮 --- */
.login-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 4px;
}

.login-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.login-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* --- 密码强度 --- */
.password-strength {
  margin-top: 6px;
}

.strength-bar {
  width: 100%;
  height: 4px;
  background: var(--border-color, rgba(255, 255, 255, 0.1));
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 2px;
}

.strength-fill.weak {
  background: #f44336;
}

.strength-fill.medium {
  background: #ff9800;
}

.strength-fill.strong {
  background: #4caf50;
}

.strength-text {
  font-size: 12px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.6));
  margin-top: 2px;
  display: inline-block;
}

.strength-warning {
  display: block;
  font-size: 12px;
  color: #ff9800;
  margin-top: 4px;
}

/* --- 登录成功提示 --- */
.first-login-tip {
  background: rgba(76, 175, 80, 0.15);
  color: #4caf50;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 14px;
}

/* --- 修改密码区域 --- */
.change-password-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.divider {
  text-align: center;
  margin-bottom: 16px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.6));
  font-size: 14px;
}

.change-password-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.change-pwd-btn {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
  margin-top: 8px;
}

.change-pwd-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.change-pwd-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cancel-change-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.2));
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary, rgba(255, 255, 255, 0.6));
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
}

.cancel-change-btn:hover {
  background: var(--sidebar-hover, rgba(255, 255, 255, 0.06));
}

.success-message {
  background: rgba(76, 175, 80, 0.15);
  color: #4caf50;
  padding: 10px 14px;
  border-radius: 8px;
  margin: 10px 0;
  font-size: 14px;
}

/* --- 底部 --- */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.change-pwd-link {
  background: none;
  border: none;
  color: #667eea;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
}

.change-pwd-link:hover {
  text-decoration: underline;
}

.footer-hint {
  font-size: 12px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.4));
}

/* --- 响应式 --- */
@media (max-width: 480px) {
  .modal-content {
    padding: 24px 20px;
  }

  .modal-header h2 {
    font-size: 18px;
  }
}
</style>
