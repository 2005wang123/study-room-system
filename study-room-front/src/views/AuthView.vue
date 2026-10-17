<!-- src/views/AuthView.vue —— 启动首屏：自习室预约系统 · 登录 / 注册 -->
<template>
  <div class="auth-root">
    <!-- 左侧品牌展示面板 -->
    <aside class="brand-panel">
      <div class="orb orb-1" aria-hidden="true"></div>
      <div class="orb orb-2" aria-hidden="true"></div>
      <div class="grain" aria-hidden="true"></div>

      <div class="brand-inner">
        <!-- 品牌标识 -->
        <div class="brand-mark reveal" style="--d: 0.05s">
          <span class="logo">
            <svg viewBox="0 0 32 32" width="22" height="22" fill="none" aria-hidden="true">
              <path
                d="M16 9c-2.5-1.5-6-1.5-8 0v14c2-1.5 5.5-1.5 8 0 2.5-1.5 6-1.5 8 0V9c-2-1.5-5.5-1.5-8 0z"
                stroke="white"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path d="M16 9v14" stroke="white" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <span class="brand-name">自习室预约系统</span>
        </div>

        <!-- 主标题 -->
        <p class="eyebrow reveal" style="--d: 0.12s">STUDY ROOM · 智能预约</p>
        <h1 class="brand-title reveal" style="--d: 0.2s">
          安静一隅，<br />专注所成
        </h1>
        <p class="brand-sub reveal" style="--d: 0.28s">
          实时座位 · 智能签到 · 信用守护<br />让每一次学习都被妥善安放。
        </p>

        <!-- 特性清单 -->
        <ul class="feature-list">
          <li class="reveal" style="--d: 0.36s">
            <span class="feature-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 21s-7-6.4-7-11a7 7 0 0 1 14 0c0 4.6-7 11-7 11z" />
                <circle cx="12" cy="10" r="2.4" />
              </svg>
            </span>
            <div>
              <p class="feature-title">实时座位地图</p>
              <p class="feature-desc">按楼层 / 区域浏览，颜色实时反映可约状态</p>
            </div>
          </li>
          <li class="reveal" style="--d: 0.44s">
            <span class="feature-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="9" />
                <path d="M8.5 12.4l2.4 2.4 4.6-5.2" />
              </svg>
            </span>
            <div>
              <p class="feature-title">智能签到签退</p>
              <p class="feature-desc">预约状态自动流转，超时未签到计入信用</p>
            </div>
          </li>
          <li class="reveal" style="--d: 0.52s">
            <span class="feature-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 3l7 3v5c0 4.4-3 7.8-7 10-4-2.2-7-5.6-7-10V6z" />
                <path d="M9 12l2 2 4-4" />
              </svg>
            </span>
            <div>
              <p class="feature-title">信用积分守护</p>
              <p class="feature-desc">初始 500 分，违约扣分，守护公平秩序</p>
            </div>
          </li>
        </ul>
      </div>

      <p class="brand-foot reveal" style="--d: 0.6s">© 2026 自习室预约系统 · 让专注更简单</p>
    </aside>

    <!-- 右侧认证卡片 -->
    <main class="auth-panel">
      <div class="auth-shell reveal" style="--d: 0.18s">
        <div class="auth-card">
          <!-- 已登录便捷入口 -->
          <div v-if="alreadyLoggedIn" class="already">
            <span class="already-dot" aria-hidden="true"></span>
            <span class="already-text">您已登录</span>
            <button type="button" class="already-link" @click="enterSystem">直接进入系统 →</button>
          </div>

          <!-- 卡片内品牌（移动端显示） -->
          <div class="card-brand">
            <span class="logo">
              <svg viewBox="0 0 32 32" width="18" height="18" fill="none" aria-hidden="true">
                <path
                  d="M16 9c-2.5-1.5-6-1.5-8 0v14c2-1.5 5.5-1.5 8 0 2.5-1.5 6-1.5 8 0V9c-2-1.5-5.5-1.5-8 0z"
                  stroke="white"
                  stroke-width="1.6"
                  stroke-linejoin="round"
                />
                <path d="M16 9v14" stroke="white" stroke-width="1.6" stroke-linecap="round" />
              </svg>
            </span>
            <span class="brand-name">自习室预约系统</span>
          </div>

          <Transition name="step" mode="out-in">
            <!-- 登录 / 注册 步骤 -->
            <div v-if="step === 'auth'" key="auth" class="auth-step">
              <!-- 切换标签 -->
              <div class="tabs" role="tablist">
                <span class="tab-indicator" :class="{ register: mode === 'register' }" aria-hidden="true"></span>
                <button
                  class="tab"
                  :class="{ active: mode === 'login' }"
                  role="tab"
                  :aria-selected="mode === 'login'"
                  @click="switchMode('login')"
                >
                  登录
                </button>
                <button
                  class="tab"
                  :class="{ active: mode === 'register' }"
                  role="tab"
                  :aria-selected="mode === 'register'"
                  @click="switchMode('register')"
                >
                  注册
                </button>
              </div>

              <Transition name="swap" mode="out-in">
                <!-- 登录表单 -->
                <form v-if="mode === 'login'" key="login" class="auth-form" @submit.prevent="handleLogin">
                  <header class="form-head">
                    <h2>欢迎回来</h2>
                    <p>使用学号继续您的学习旅程</p>
                  </header>

                  <div class="field">
                    <label for="login-user">学号</label>
                    <div class="input-wrap">
                      <span class="input-icon">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <circle cx="12" cy="8" r="4" />
                          <path d="M4 20c0-4 4-6 8-6s8 2 8 6" />
                        </svg>
                      </span>
                      <input
                        id="login-user"
                        v-model="loginForm.username"
                        type="text"
                        inputmode="numeric"
                        autocomplete="username"
                        placeholder="请输入学号"
                        :class="{ invalid: fieldError.loginUser }"
                        @input="fieldError.loginUser = ''"
                      />
                    </div>
                    <span v-if="fieldError.loginUser" class="field-error">{{ fieldError.loginUser }}</span>
                  </div>

                  <div class="field">
                    <label for="login-pwd">密码</label>
                    <div class="input-wrap">
                      <span class="input-icon">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <rect x="5" y="10" width="14" height="10" rx="2.5" />
                          <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                        </svg>
                      </span>
                      <input
                        id="login-pwd"
                        v-model="loginForm.password"
                        :type="showLoginPwd ? 'text' : 'password'"
                        autocomplete="current-password"
                        placeholder="请输入密码"
                        :class="{ invalid: fieldError.loginPwd }"
                        @input="fieldError.loginPwd = ''"
                      />
                      <button type="button" class="pwd-toggle" @click="showLoginPwd = !showLoginPwd" :aria-label="showLoginPwd ? '隐藏密码' : '显示密码'">
                        <svg v-if="!showLoginPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
                          <circle cx="12" cy="12" r="3" />
                        </svg>
                        <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M3 3l18 18" />
                          <path d="M10.6 6.2A9.7 9.7 0 0 1 12 6c6.4 0 10 6 10 6a16 16 0 0 1-3.3 3.8M6.5 8.4A16 16 0 0 0 2 12s3.6 7 10 7a9.4 9.4 0 0 0 3.4-.6" />
                        </svg>
                      </button>
                    </div>
                    <span v-if="fieldError.loginPwd" class="field-error">{{ fieldError.loginPwd }}</span>
                  </div>

                  <p v-if="authError" class="banner error">{{ authError }}</p>
                  <p v-if="regSuccess" class="banner success">注册成功，请使用学号登录</p>

                  <button type="submit" class="btn" :disabled="loading">
                    <span>{{ loading ? '登录中…' : '登 录' }}</span>
                    <span class="arrow" aria-hidden="true">
                      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M5 12h14M13 6l6 6-6 6" />
                      </svg>
                    </span>
                  </button>

                  <p class="form-foot">首次登录请使用初始密码（身份证号后 6 位），登录后及时修改。</p>
                </form>

                <!-- 注册表单 -->
                <form v-else key="register" class="auth-form" @submit.prevent="handleRegister">
                  <header class="form-head">
                    <h2>创建新账户</h2>
                    <p>注册即可预约座位、查看预约记录</p>
                  </header>

                  <div class="field">
                    <label for="reg-user">学号</label>
                    <div class="input-wrap">
                      <span class="input-icon">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <circle cx="12" cy="8" r="4" />
                          <path d="M4 20c0-4 4-6 8-6s8 2 8 6" />
                        </svg>
                      </span>
                      <input
                        id="reg-user"
                        v-model="registerForm.username"
                        type="text"
                        inputmode="numeric"
                        autocomplete="username"
                        placeholder="请输入学号"
                        :class="{ invalid: fieldError.regUser }"
                        @input="fieldError.regUser = ''"
                      />
                    </div>
                    <span v-if="fieldError.regUser" class="field-error">{{ fieldError.regUser }}</span>
                  </div>

                  <div class="field">
                    <label for="reg-pwd">密码</label>
                    <div class="input-wrap">
                      <span class="input-icon">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <rect x="5" y="10" width="14" height="10" rx="2.5" />
                          <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                        </svg>
                      </span>
                      <input
                        id="reg-pwd"
                        v-model="registerForm.password"
                        :type="showRegPwd ? 'text' : 'password'"
                        autocomplete="new-password"
                        placeholder="请设置密码（至少 6 位）"
                        :class="{ invalid: fieldError.regPwd }"
                        @input="fieldError.regPwd = ''"
                      />
                      <button type="button" class="pwd-toggle" @click="showRegPwd = !showRegPwd" :aria-label="showRegPwd ? '隐藏密码' : '显示密码'">
                        <svg v-if="!showRegPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
                          <circle cx="12" cy="12" r="3" />
                        </svg>
                        <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M3 3l18 18" />
                          <path d="M10.6 6.2A9.7 9.7 0 0 1 12 6c6.4 0 10 6 10 6a16 16 0 0 1-3.3 3.8M6.5 8.4A16 16 0 0 0 2 12s3.6 7 10 7a9.4 9.4 0 0 0 3.4-.6" />
                        </svg>
                      </button>
                    </div>
                    <div v-if="registerForm.password" class="strength">
                      <span class="strength-track"><span class="strength-fill" :class="regStrength.cls" :style="{ width: regStrength.w + '%' }"></span></span>
                      <span class="strength-text" :class="regStrength.cls">{{ regStrength.text }}</span>
                    </div>
                    <span v-if="fieldError.regPwd" class="field-error">{{ fieldError.regPwd }}</span>
                  </div>

                  <div class="field">
                    <label for="reg-confirm">确认密码</label>
                    <div class="input-wrap">
                      <span class="input-icon">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                          <rect x="5" y="10" width="14" height="10" rx="2.5" />
                          <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                        </svg>
                      </span>
                      <input
                        id="reg-confirm"
                        v-model="registerForm.confirm"
                        :type="showRegPwd ? 'text' : 'password'"
                        autocomplete="new-password"
                        placeholder="请再次输入密码"
                        :class="{ invalid: fieldError.regConfirm }"
                        @input="fieldError.regConfirm = ''"
                      />
                    </div>
                    <span v-if="fieldError.regConfirm" class="field-error">{{ fieldError.regConfirm }}</span>
                  </div>

                  <p v-if="regError" class="banner error">{{ regError }}</p>

                  <button type="submit" class="btn" :disabled="loading">
                    <span>{{ loading ? '注册中…' : '注 册' }}</span>
                    <span class="arrow" aria-hidden="true">
                      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M5 12h14M13 6l6 6-6 6" />
                      </svg>
                    </span>
                  </button>
                </form>
              </Transition>
            </div>

            <!-- 首次登录 / 弱密码：修改密码步骤 -->
            <form v-else key="change" class="auth-step auth-form" @submit.prevent="handleChangePassword">
              <header class="form-head">
                <h2>设置您的新密码</h2>
                <p>检测到您仍使用初始密码，为保障账户安全请先修改</p>
              </header>

              <div class="field">
                <label for="ch-old">当前密码</label>
                <div class="input-wrap">
                  <span class="input-icon">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="5" y="10" width="14" height="10" rx="2.5" />
                      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                    </svg>
                  </span>
                  <input
                    id="ch-old"
                    v-model="changeForm.oldPassword"
                    :type="showChangePwd ? 'text' : 'password'"
                    autocomplete="current-password"
                    placeholder="请输入当前密码"
                  />
                  <button type="button" class="pwd-toggle" @click="showChangePwd = !showChangePwd" :aria-label="showChangePwd ? '隐藏密码' : '显示密码'">
                    <svg v-if="!showChangePwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M3 3l18 18" />
                      <path d="M10.6 6.2A9.7 9.7 0 0 1 12 6c6.4 0 10 6 10 6a16 16 0 0 1-3.3 3.8M6.5 8.4A16 16 0 0 0 2 12s3.6 7 10 7a9.4 9.4 0 0 0 3.4-.6" />
                    </svg>
                  </button>
                </div>
              </div>

              <div class="field">
                <label for="ch-new">新密码</label>
                <div class="input-wrap">
                  <span class="input-icon">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="5" y="10" width="14" height="10" rx="2.5" />
                      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                    </svg>
                  </span>
                  <input
                    id="ch-new"
                    v-model="changeForm.newPassword"
                    :type="showChangePwd ? 'text' : 'password'"
                    autocomplete="new-password"
                    placeholder="请设置新密码（至少 6 位）"
                  />
                </div>
                <div v-if="changeForm.newPassword" class="strength">
                  <span class="strength-track"><span class="strength-fill" :class="changeStrength.cls" :style="{ width: changeStrength.w + '%' }"></span></span>
                  <span class="strength-text" :class="changeStrength.cls">{{ changeStrength.text }}</span>
                </div>
              </div>

              <div class="field">
                <label for="ch-confirm">确认新密码</label>
                <div class="input-wrap">
                  <span class="input-icon">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="5" y="10" width="14" height="10" rx="2.5" />
                      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                    </svg>
                  </span>
                  <input
                    id="ch-confirm"
                    v-model="changeForm.confirmPassword"
                    :type="showChangePwd ? 'text' : 'password'"
                    autocomplete="new-password"
                    placeholder="请再次输入新密码"
                  />
                </div>
              </div>

              <p v-if="changeError" class="banner error">{{ changeError }}</p>
              <p v-if="changeSuccess" class="banner success">密码修改成功，正在返回登录…</p>

              <button type="submit" class="btn" :disabled="loading">
                <span>{{ loading ? '修改中…' : '确认修改' }}</span>
                <span class="arrow" aria-hidden="true">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 12h14M13 6l6 6-6 6" />
                  </svg>
                </span>
              </button>
            </form>
          </Transition>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login, register, changePassword } from '@/api/user'
import type { ApiErrorShape, LoginResult } from '@/types/api'

const router = useRouter()

// 已登录用户：首屏仍展示登录界面，但提供一键进入系统的入口
const alreadyLoggedIn = ref(false)
onMounted(() => {
  alreadyLoggedIn.value = !!localStorage.getItem('token')
})
const enterSystem = () => router.push('/')

type Mode = 'login' | 'register'
const mode = ref<Mode>('login')
const step = ref<'auth' | 'change'>('auth')

// 表单数据
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirm: '' })
const changeForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const showLoginPwd = ref(false)
const showRegPwd = ref(false)
const showChangePwd = ref(false)

const loading = ref(false)
const authError = ref('')
const regError = ref('')
const regSuccess = ref(false)
const changeError = ref('')
const changeSuccess = ref(false)

const fieldError = reactive({
  loginUser: '',
  loginPwd: '',
  regUser: '',
  regPwd: '',
  regConfirm: ''
})

// 持久化（与 SeatMap.handleLoginSuccess 保持一致：token + userInfo）
const saveAuth = (data: LoginResult) => {
  localStorage.setItem('token', data.token)
  localStorage.setItem(
    'userInfo',
    JSON.stringify({
      isLoggedIn: true,
      name: data.username,
      userId: data.id,
      role: data.role || 0,
      needChangePassword: !!data.needChangePassword,
      isFirstLogin: !!data.isFirstLogin
    })
  )
}
const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

// 校验
const validateLogin = () => {
  fieldError.loginUser = !/^\d{6,12}$/.test(loginForm.username.trim()) ? '学号应为 6–12 位数字' : ''
  fieldError.loginPwd = loginForm.password.length < 6 ? '密码至少 6 位' : ''
  return !fieldError.loginUser && !fieldError.loginPwd
}
const validateRegister = () => {
  fieldError.regUser = !/^\d{6,12}$/.test(registerForm.username.trim()) ? '学号应为 6–12 位数字' : ''
  fieldError.regPwd = registerForm.password.length < 6 ? '密码至少 6 位' : ''
  fieldError.regConfirm = registerForm.confirm !== registerForm.password ? '两次输入的密码不一致' : ''
  return !fieldError.regUser && !fieldError.regPwd && !fieldError.regConfirm
}

// 密码强度
const scorePwd = (pwd: string) => {
  let s = 0
  if (pwd.length >= 8) s++
  if (pwd.length >= 12) s++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) s++
  if (/\d/.test(pwd)) s++
  if (/[^a-zA-Z0-9]/.test(pwd)) s++
  return s
}
const regStrength = computed(() => {
  const s = scorePwd(registerForm.password)
  if (!s) return { w: 0, cls: '', text: '' }
  if (s <= 2) return { w: 34, cls: 'weak', text: '弱' }
  if (s <= 3) return { w: 67, cls: 'medium', text: '中' }
  return { w: 100, cls: 'strong', text: '强' }
})
const changeStrength = computed(() => {
  const s = scorePwd(changeForm.newPassword)
  if (!s) return { w: 0, cls: '', text: '' }
  if (s <= 2) return { w: 34, cls: 'weak', text: '弱' }
  if (s <= 3) return { w: 67, cls: 'medium', text: '中' }
  return { w: 100, cls: 'strong', text: '强' }
})

const resetFields = () => {
  fieldError.loginUser = fieldError.loginPwd = ''
  fieldError.regUser = fieldError.regPwd = fieldError.regConfirm = ''
  authError.value = ''
  regError.value = ''
  regSuccess.value = false
}

const switchMode = (m: Mode) => {
  mode.value = m
  resetFields()
}

const handleLogin = async () => {
  authError.value = ''
  regSuccess.value = false
  if (!validateLogin()) return
  loading.value = true
  try {
    const res = await login({ username: loginForm.username.trim(), password: loginForm.password })
    if (res.code === 200 && res.data) {
      saveAuth(res.data)
      // 首次登录 / 密码过于简单：强制修改密码
      if (res.data.needChangePassword || res.data.isFirstLogin) {
        changeForm.oldPassword = loginForm.password
        changeForm.newPassword = ''
        changeForm.confirmPassword = ''
        changeSuccess.value = false
        changeError.value = ''
        step.value = 'change'
      } else {
        router.push('/')
      }
    } else {
      authError.value = res.message || '登录失败，请检查学号与密码'
    }
  } catch (err) {
    const e = err as ApiErrorShape
    authError.value = e.response?.data?.message || e.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  regError.value = ''
  regSuccess.value = false
  if (!validateRegister()) return
  loading.value = true
  try {
    const res = await register({ username: registerForm.username.trim(), password: registerForm.password })
    if (res.code === 200) {
      mode.value = 'login'
      loginForm.username = registerForm.username.trim()
      loginForm.password = ''
      registerForm.password = ''
      registerForm.confirm = ''
      regSuccess.value = true
    } else {
      regError.value = res.message || '注册失败，请稍后重试'
    }
  } catch (err) {
    const e = err as ApiErrorShape
    regError.value = e.response?.data?.message || e.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

const handleChangePassword = async () => {
  changeError.value = ''
  changeSuccess.value = false
  if (!changeForm.oldPassword) {
    changeError.value = '请输入当前密码'
    return
  }
  if (changeForm.newPassword.length < 6) {
    changeError.value = '新密码至少 6 位'
    return
  }
  if (changeForm.newPassword !== changeForm.confirmPassword) {
    changeError.value = '两次输入的新密码不一致'
    return
  }
  loading.value = true
  try {
    const res = await changePassword({
      oldPassword: changeForm.oldPassword,
      newPassword: changeForm.newPassword,
      confirmPassword: changeForm.confirmPassword
    })
    if (res.code === 200) {
      changeSuccess.value = true
      // 修改成功后后端通常会使旧令牌失效，引导用户重新登录
      setTimeout(() => {
        clearAuth()
        step.value = 'auth'
        mode.value = 'login'
        loginForm.username = ''
        loginForm.password = ''
        changeForm.oldPassword = ''
        changeForm.newPassword = ''
        changeForm.confirmPassword = ''
        authError.value = '密码已更新，请使用新密码重新登录'
      }, 1600)
    } else {
      changeError.value = res.message || '修改失败，请重试'
    }
  } catch (err) {
    const e = err as ApiErrorShape
    changeError.value = e.response?.data?.message || e.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-root {
  position: fixed;
  inset: 0;
  display: flex;
  min-height: 100dvh;
  width: 100vw;
  text-align: left;
  color: #eceaf3;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei',
    'Noto Sans SC', sans-serif;
  z-index: 1;
}

/* ============ 左侧品牌面板 ============ */
.brand-panel {
  position: relative;
  flex: 1 1 56%;
  max-width: 720px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 6vw 5vw;
  overflow: hidden;
  background: linear-gradient(155deg, rgba(22, 22, 34, 0.92) 0%, rgba(11, 11, 19, 0.97) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.07);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  opacity: 0.55;
}
.orb-1 {
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, rgba(124, 101, 246, 0.55), transparent 70%);
  top: -160px;
  left: -120px;
}
.orb-2 {
  width: 460px;
  height: 460px;
  background: radial-gradient(circle, rgba(86, 178, 233, 0.4), transparent 70%);
  bottom: -140px;
  right: -100px;
}
.grain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.05;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 480px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 48px;
}
.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 13px;
  background: linear-gradient(135deg, #6d5efc 0%, #9b5de5 100%);
  box-shadow: 0 10px 30px -8px rgba(123, 94, 252, 0.7), inset 0 1px 1px rgba(255, 255, 255, 0.3);
}
.brand-name {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #f4f2fb;
}

.eyebrow {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(168, 156, 245, 0.9);
  margin-bottom: 18px;
}
.brand-title {
  font-size: clamp(2.6rem, 4.4vw, 3.9rem);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -0.02em;
  margin: 0 0 22px;
  color: #fbfaff;
}
.brand-sub {
  font-size: 15px;
  line-height: 1.7;
  color: rgba(236, 234, 243, 0.62);
  margin: 0 0 44px;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 22px;
}
.feature-list li {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.feature-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #c7bdf7;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #f2f0f8;
  margin: 0 0 4px;
}
.feature-desc {
  font-size: 13px;
  line-height: 1.5;
  color: rgba(236, 234, 243, 0.52);
  margin: 0;
}

.brand-foot {
  position: relative;
  z-index: 1;
  margin-top: 48px;
  font-size: 12px;
  color: rgba(236, 234, 243, 0.4);
}

/* ============ 右侧认证面板 ============ */
.auth-panel {
  flex: 1 1 44%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

/* 双层包边（Double-Bezel） */
.auth-shell {
  width: 100%;
  max-width: 440px;
  padding: 6px;
  border-radius: 30px;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.04));
  box-shadow: 0 40px 90px -24px rgba(0, 0, 0, 0.7);
}
.auth-card {
  position: relative;
  border-radius: 24px;
  background: rgba(17, 17, 26, 0.72);
  backdrop-filter: blur(26px);
  -webkit-backdrop-filter: blur(26px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.12);
  padding: 40px;
}

.card-brand {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 26px;
}

/* 已登录便捷入口 */
.already {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 22px;
  padding: 10px 14px;
  border-radius: 13px;
  background: rgba(74, 214, 160, 0.1);
  border: 1px solid rgba(74, 214, 160, 0.22);
}
.already-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4ad6a0;
  box-shadow: 0 0 0 3px rgba(74, 214, 160, 0.18);
  flex-shrink: 0;
}
.already-text {
  font-size: 13px;
  color: rgba(143, 230, 194, 0.92);
}
.already-link {
  margin-left: auto;
  border: none;
  background: transparent;
  color: #8fe6c2;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 8px;
  transition: background 0.3s, color 0.3s;
}
.already-link:hover {
  background: rgba(74, 214, 160, 0.16);
  color: #b6f2d8;
}

.auth-step {
  display: flex;
  flex-direction: column;
}

/* 切换标签 */
.tabs {
  position: relative;
  display: flex;
  padding: 4px;
  margin-bottom: 28px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
}
.tab {
  position: relative;
  z-index: 1;
  flex: 1;
  border: none;
  background: transparent;
  color: rgba(236, 234, 243, 0.6);
  font-size: 14px;
  font-weight: 600;
  padding: 10px 0;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.5s cubic-bezier(0.32, 0.72, 0, 1);
}
.tab.active {
  color: #fff;
}
.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.16);
  transition: transform 0.55s cubic-bezier(0.32, 0.72, 0, 1);
}
.tab-indicator.register {
  transform: translateX(100%);
}

/* 表单 */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.form-head {
  margin-bottom: 4px;
}
.form-head h2 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: #fbfaff;
  margin: 0 0 8px;
  font-family: inherit;
}
.form-head p {
  font-size: 13.5px;
  line-height: 1.5;
  color: rgba(236, 234, 243, 0.55);
  margin: 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(236, 234, 243, 0.78);
}
.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.input-icon {
  position: absolute;
  left: 15px;
  display: inline-flex;
  color: rgba(236, 234, 243, 0.42);
  pointer-events: none;
}
.input-wrap input {
  width: 100%;
  padding: 14px 46px 14px 46px;
  font-size: 15px;
  font-family: inherit;
  color: #fbfaff;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  outline: none;
  transition: border-color 0.4s cubic-bezier(0.32, 0.72, 0, 1), box-shadow 0.4s cubic-bezier(0.32, 0.72, 0, 1),
    background 0.4s cubic-bezier(0.32, 0.72, 0, 1);
}
.input-wrap input::placeholder {
  color: rgba(236, 234, 243, 0.38);
}
.input-wrap input:focus {
  border-color: rgba(139, 124, 234, 0.75);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 0 4px rgba(123, 94, 252, 0.16);
}
.input-wrap input.invalid {
  border-color: rgba(244, 112, 122, 0.7);
}
.pwd-toggle {
  position: absolute;
  right: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: rgba(236, 234, 243, 0.5);
  cursor: pointer;
  border-radius: 9px;
  transition: color 0.3s, background 0.3s;
}
.pwd-toggle:hover {
  color: rgba(236, 234, 243, 0.85);
  background: rgba(255, 255, 255, 0.06);
}

.field-error {
  font-size: 12px;
  color: #f4707a;
}
.banner {
  font-size: 13px;
  padding: 11px 14px;
  border-radius: 12px;
  margin: 0;
  line-height: 1.45;
}
.banner.error {
  color: #fca2ab;
  background: rgba(244, 112, 122, 0.12);
  border: 1px solid rgba(244, 112, 122, 0.25);
}
.banner.success {
  color: #8fe6c2;
  background: rgba(74, 214, 160, 0.12);
  border: 1px solid rgba(74, 214, 160, 0.25);
}

/* 密码强度 */
.strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 2px;
}
.strength-track {
  flex: 1;
  height: 4px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}
.strength-fill {
  display: block;
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s cubic-bezier(0.32, 0.72, 0, 1);
}
.strength-fill.weak {
  background: #f4707a;
}
.strength-fill.medium {
  background: #f5b14c;
}
.strength-fill.strong {
  background: #4ad6a0;
}
.strength-text {
  font-size: 12px;
  min-width: 18px;
}
.strength-text.weak {
  color: #f4707a;
}
.strength-text.medium {
  color: #f5b14c;
}
.strength-text.strong {
  color: #4ad6a0;
}

/* 主按钮（Button-in-Button） */
.btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  margin-top: 6px;
  padding: 15px 22px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #6d5efc 0%, #9b5de5 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 0.04em;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 12px 32px -10px rgba(123, 94, 252, 0.65);
  transition: box-shadow 0.5s cubic-bezier(0.32, 0.72, 0, 1), transform 0.5s cubic-bezier(0.32, 0.72, 0, 1);
}
.btn:hover:not(:disabled) {
  box-shadow: 0 18px 44px -10px rgba(123, 94, 252, 0.78);
}
.btn:active:not(:disabled) {
  transform: scale(0.985);
}
.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.btn .arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  transition: transform 0.5s cubic-bezier(0.32, 0.72, 0, 1), background 0.5s cubic-bezier(0.32, 0.72, 0, 1);
}
.btn:hover:not(:disabled) .arrow {
  transform: translate(3px, -2px) scale(1.06);
  background: rgba(255, 255, 255, 0.26);
}

.form-foot {
  font-size: 12px;
  line-height: 1.5;
  color: rgba(236, 234, 243, 0.42);
  margin: 4px 0 0;
  text-align: center;
}

/* ============ 动效 ============ */
.reveal {
  opacity: 0;
  transform: translateY(22px);
  filter: blur(8px);
  animation: reveal 0.9s cubic-bezier(0.32, 0.72, 0, 1) forwards;
  animation-delay: var(--d, 0s);
}
@keyframes reveal {
  to {
    opacity: 1;
    transform: none;
    filter: blur(0);
  }
}

.step-enter-active,
.step-leave-active,
.swap-enter-active,
.swap-leave-active {
  transition: opacity 0.45s cubic-bezier(0.32, 0.72, 0, 1), transform 0.45s cubic-bezier(0.32, 0.72, 0, 1);
}
.step-enter-from,
.swap-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.step-leave-to,
.swap-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* ============ 响应式 ============ */
@media (max-width: 960px) {
  .auth-root {
    flex-direction: column;
    overflow-y: auto;
  }
  .brand-panel {
    flex: none;
    max-width: none;
    width: 100%;
    padding: 56px 28px 40px;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.07);
    justify-content: flex-start;
  }
  .brand-mark {
    margin-bottom: 32px;
  }
  .feature-list {
    display: none;
  }
  .brand-foot {
    display: none;
  }
  .auth-panel {
    flex: none;
    width: 100%;
    padding: 32px 20px 48px;
  }
  .card-brand {
    display: flex;
  }
}
@media (max-width: 480px) {
  .auth-card {
    padding: 28px 22px;
  }
  .brand-title {
    font-size: 2.3rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .reveal {
    animation: none;
    opacity: 1;
    transform: none;
    filter: none;
  }
}
</style>
