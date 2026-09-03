<!-- src/components/ProfileCenter.vue -->
<template>
  <div class="profile-page">
    <div class="profile-header">
      <h2 class="profile-title">👤 个人中心</h2>
      <p class="profile-subtitle">管理您的账户信息与安全设置</p>
    </div>

    <!-- 用户信息卡片 -->
    <div class="user-card">
      <div class="user-card-avatar">
        <span v-if="!profile.avatar">👤</span>
        <img v-else :src="profile.avatar" alt="avatar" />
      </div>
      <div class="user-card-info">
        <div class="user-card-name">{{ profile.username || '用户' }}</div>
        <div class="user-card-tags">
          <span class="tag">{{ roleText }}</span>
          <span class="tag" :class="profile.status === 1 ? 'tag-ok' : 'tag-bad'">{{ statusText }}</span>
          <span v-if="profile.isFirstLogin" class="tag tag-warn">首次登录</span>
        </div>
        <div class="user-card-id">学号 / 账号：{{ profile.username || '-' }}</div>
      </div>
    </div>

    <!-- 预约统计 -->
    <div v-if="loaded" class="stat-grid">
      <div class="stat-card">
        <div class="stat-value">{{ totalCount }}</div>
        <div class="stat-label">总预约</div>
      </div>
      <div class="stat-card active">
        <div class="stat-value">{{ activeCount }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-card done">
        <div class="stat-value">{{ completedCount }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card ended">
        <div class="stat-value">{{ cancelledCount }}</div>
        <div class="stat-label">已取消</div>
      </div>
    </div>

    <!-- 修改密码 -->
    <div class="section-card">
      <div class="section-title">🔑 修改密码</div>
      <p class="section-desc">建议定期修改密码，密码需包含大小写字母、数字和特殊字符，长度不少于8位</p>

      <form class="pwd-form" @submit.prevent="handleChangePassword">
        <div class="form-group">
          <label>当前密码</label>
          <div class="password-wrapper">
            <input v-model="pwdForm.oldPassword" :type="showPwd.old ? 'text' : 'password'" placeholder="请输入当前密码" required />
            <button type="button" class="password-toggle" @click="showPwd.old = !showPwd.old">{{ showPwd.old ? '🙈' : '👁️' }}</button>
          </div>
        </div>

        <div class="form-group">
          <label>新密码</label>
          <div class="password-wrapper">
            <input v-model="pwdForm.newPassword" :type="showPwd.new ? 'text' : 'password'" placeholder="请输入新密码" required @input="checkStrength" />
            <button type="button" class="password-toggle" @click="showPwd.new = !showPwd.new">{{ showPwd.new ? '🙈' : '👁️' }}</button>
          </div>
          <div v-if="pwdForm.newPassword" class="password-strength">
            <div class="strength-bar">
              <div class="strength-fill" :class="strength.class" :style="{ width: strength.percentage + '%' }"></div>
            </div>
            <span class="strength-text">{{ strength.text }}</span>
            <span v-if="strength.isWeak" class="strength-warning">⚠️ 密码过于简单，建议包含大小写字母、数字和特殊字符</span>
          </div>
        </div>

        <div class="form-group">
          <label>确认新密码</label>
          <div class="password-wrapper">
            <input v-model="pwdForm.confirmPassword" :type="showPwd.confirm ? 'text' : 'password'" placeholder="请再次输入新密码" required />
            <button type="button" class="password-toggle" @click="showPwd.confirm = !showPwd.confirm">{{ showPwd.confirm ? '🙈' : '👁️' }}</button>
          </div>
          <span v-if="pwdError" class="field-error">{{ pwdError }}</span>
        </div>

        <button type="submit" class="primary-btn" :disabled="pwdLoading">
          {{ pwdLoading ? '修改中...' : '确认修改密码' }}
        </button>
      </form>
    </div>

    <!-- 账户操作 -->
    <div class="section-card">
      <div class="section-title">⚙️ 账户操作</div>
      <div class="action-row">
        <div class="action-info">
          <div class="action-name">切换账户</div>
          <div class="action-desc">退出当前账号并登录其他账号</div>
        </div>
        <button class="switch-btn" @click="handleSwitchAccount">切换账户</button>
      </div>
      <div class="action-row">
        <div class="action-info">
          <div class="action-name">退出登录</div>
          <div class="action-desc">退出当前账号，下次使用需重新登录</div>
        </div>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { getUserInfo, changePassword } from '@/api/user';
import { getMyReservations } from '@/api/seat';

const emit = defineEmits(['toast', 'logout', 'switch-account']);

const profile = reactive({
  username: '',
  role: 0,
  status: 1,
  isFirstLogin: false,
  avatar: ''
});

const records = ref([]);
const loaded = ref(false);

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});
const showPwd = reactive({ old: false, new: false, confirm: false });
const pwdLoading = ref(false);
const pwdError = ref('');

const roleText = computed(() => profile.role === 1 ? '管理员' : '学生');
const statusText = computed(() => profile.status === 1 ? '账号正常' : '账号已禁用');
const totalCount = computed(() => records.value.length);
const activeCount = computed(() => records.value.filter(r => Number(r.status) === 0 || Number(r.status) === 1).length);
const completedCount = computed(() => records.value.filter(r => Number(r.status) === 2).length);
const cancelledCount = computed(() => records.value.filter(r => Number(r.status) === 4).length);

// 密码强度
const checkStrength = () => {
  const pwd = pwdForm.newPassword;
  if (!pwd) return 0;
  let score = 0;
  if (pwd.length >= 8) score += 1;
  if (pwd.length >= 12) score += 1;
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score += 1;
  if (/\d/.test(pwd)) score += 1;
  if (/[^a-zA-Z0-9]/.test(pwd)) score += 1;
  return score;
};

const strength = computed(() => {
  const score = checkStrength();
  if (!score) return { class: '', text: '', percentage: 0, isWeak: false };
  if (score <= 2) return { class: 'weak', text: '弱', percentage: 30, isWeak: true };
  if (score <= 3) return { class: 'medium', text: '中', percentage: 60, isWeak: true };
  return { class: 'strong', text: '强', percentage: 100, isWeak: false };
});

const extractList = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.rows)) return data.rows;
  if (Array.isArray(data?.list)) return data.list;
  if (Array.isArray(data?.records)) return data.records;
  return [];
};

const fetchProfile = async () => {
  try {
    const res = await getUserInfo();
    let data = res;
    if (data?.code === 200) data = data.data;
    else if (data?.data?.code === 200) data = data.data.data;
    if (data) {
      profile.username = data.username || profile.username;
      profile.role = data.role ?? profile.role;
      profile.status = data.status ?? profile.status;
      profile.isFirstLogin = !!data.isFirstLogin;
    }
  } catch (err) {
    // 忽略：父组件会处理未授权
    console.error('获取用户信息失败', err);
  }
};

const fetchRecords = async () => {
  try {
    const res = await getMyReservations();
    let data = res;
    if (data?.code === 200) data = data.data;
    else if (data?.data?.code === 200) data = data.data.data;
    records.value = extractList(data);
    loaded.value = true;
  } catch (err) {
    console.error('获取预约统计失败', err);
    loaded.value = true;
  }
};

const handleChangePassword = async () => {
  pwdError.value = '';
  if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
    pwdError.value = '请填写完整的密码信息';
    return;
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    pwdError.value = '两次输入的新密码不一致';
    return;
  }
  if (pwdForm.newPassword.length < 8) {
    pwdError.value = '新密码长度不能少于8位';
    return;
  }
  if (pwdForm.newPassword === pwdForm.oldPassword) {
    pwdError.value = '新密码不能与当前密码相同';
    return;
  }

  pwdLoading.value = true;
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    });
    emit('toast', { message: '✅ 密码修改成功', type: 'success' });
    pwdForm.oldPassword = '';
    pwdForm.newPassword = '';
    pwdForm.confirmPassword = '';
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '修改密码失败';
    pwdError.value = msg;
  } finally {
    pwdLoading.value = false;
  }
};

const handleLogout = () => {
  emit('logout');
};

const handleSwitchAccount = () => {
  emit('switch-account');
};

onMounted(() => {
  fetchProfile();
  fetchRecords();
});
</script>

<style scoped>
.profile-page {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
  box-sizing: border-box;
}

.profile-header {
  margin-bottom: 20px;
}

.profile-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.profile-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

/* 用户信息卡片 */
.user-card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 22px;
  margin-bottom: 20px;
}

.user-card-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
  overflow: hidden;
}

.user-card-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-card-info {
  min-width: 0;
}

.user-card-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.user-card-tags {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: var(--sidebar-hover);
  color: var(--text-secondary);
}

.tag-ok { background: rgba(76, 175, 80, 0.15); color: #4caf50; }
.tag-bad { background: rgba(244, 67, 54, 0.15); color: #f44336; }
.tag-warn { background: rgba(255, 152, 0, 0.15); color: #ff9800; }

.user-card-id {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 8px;
}

/* 统计卡片 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 16px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.stat-card.active .stat-value { color: #667eea; }
.stat-card.done .stat-value { color: #4caf50; }
.stat-card.ended .stat-value { color: #ff9800; }

/* 区块卡片 */
.section-card {
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 22px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.section-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 6px;
  margin-bottom: 18px;
}

/* 表单 */
.pwd-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  color: var(--text-secondary);
}

.form-group input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--sidebar-hover);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.18);
}

.form-group input::placeholder {
  color: var(--text-secondary);
  opacity: 0.6;
}

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
  color: var(--text-secondary);
}

.password-strength {
  margin-top: 4px;
}

.strength-bar {
  width: 100%;
  height: 4px;
  background: var(--border-color);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 2px;
}

.strength-fill.weak { background: #f44336; }
.strength-fill.medium { background: #ff9800; }
.strength-fill.strong { background: #4caf50; }

.strength-text {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
  display: inline-block;
}

.strength-warning {
  display: block;
  font-size: 12px;
  color: #ff9800;
  margin-top: 4px;
}

.field-error {
  font-size: 12px;
  color: #f44336;
}

.primary-btn {
  padding: 11px 0;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.primary-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.primary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 账户操作 */
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.action-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.action-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.logout-btn {
  padding: 9px 22px;
  border-radius: 10px;
  border: 1px solid rgba(244, 67, 54, 0.5);
  background: transparent;
  color: #f44336;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: rgba(244, 67, 54, 0.12);
}

.switch-btn {
  padding: 9px 22px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.switch-btn:hover {
  opacity: 0.9;
}

.action-row + .action-row {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

@media (max-width: 768px) {
  .profile-page { padding: 16px; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .user-card { flex-direction: column; text-align: center; }
}
</style>
