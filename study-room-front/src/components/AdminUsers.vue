<!-- src/components/AdminUsers.vue 用户管理（仅管理员） -->
<template>
  <div class="admin-page">
    <div class="admin-header">
      <div>
        <h2 class="admin-title">🛠️ 用户管理</h2>
        <p class="admin-subtitle">管理学生/管理员账号，支持新增、启停、删除、重置密码</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="fetchUsers">
        <span>{{ loading ? '刷新中...' : '刷新' }}</span>
      </button>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <input
        v-model="keyword"
        class="search-input"
        type="text"
        placeholder="按用户名/学号搜索..."
        @keyup.enter="search"
      />
      <button class="primary-btn" @click="showCreate = !showCreate">
        {{ showCreate ? '收起' : '➕ 新增用户' }}
      </button>
    </div>

    <!-- 新增用户表单 -->
    <div v-if="showCreate" class="create-form">
      <input v-model="createForm.username" class="form-input" placeholder="用户名/学号（必填）" />
      <input v-model="createForm.idCard" class="form-input" placeholder="完整身份证号（初始密码=后6位），或直接填6位数字初始密码" />
      <select v-model="createForm.role" class="form-input">
        <option :value="0">学生</option>
        <option :value="1">管理员</option>
      </select>
      <select v-model="createForm.status" class="form-input">
        <option :value="1">启用</option>
        <option :value="0">禁用</option>
      </select>
      <button class="primary-btn" :disabled="creating" @click="handleCreate">
        {{ creating ? '创建中...' : '确认创建' }}
      </button>
    </div>

    <!-- 状态区 -->
    <div v-if="loading" class="state-wrapper">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="state-wrapper">
      <div class="state-icon">⚠️</div>
      <p>{{ error }}</p>
      <button class="state-btn" @click="fetchUsers">重试</button>
    </div>
    <div v-else-if="users.length === 0" class="state-wrapper">
      <div class="state-icon">👥</div>
      <p>暂无用户</p>
    </div>

    <!-- 用户列表 -->
    <div v-else class="user-list">
      <div v-for="u in users" :key="u.id" class="user-card">
        <div class="user-main">
          <div class="user-avatar">{{ u.username ? u.username[0].toUpperCase() : '?' }}</div>
          <div class="user-info">
            <div class="user-name">{{ u.username }}</div>
            <div class="user-meta">
              <span class="tag" :class="u.role === 1 ? 'tag-admin' : ''">{{ u.role === 1 ? '管理员' : '学生' }}</span>
              <span class="tag" :class="u.status === 1 ? 'tag-ok' : 'tag-bad'">{{ u.status === 1 ? '正常' : '已禁用' }}</span>
              <span v-if="u.isFirstLogin" class="tag tag-warn">首次登录</span>
              <span class="tag tag-time">{{ formatTime(u.createTime) }}</span>
            </div>
          </div>
        </div>
        <div class="user-actions">
          <button class="mini-btn" :disabled="actingId === u.id" @click="handleResetPwd(u)">重置密码</button>
          <button class="mini-btn danger" :disabled="actingId === u.id" @click="handleDelete(u)">删除</button>
          <button
            class="mini-btn danger"
            :disabled="actingId === u.id"
            @click="handleToggleStatus(u)"
          >{{ u.status === 1 ? '禁用' : '启用' }}</button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination">
      <button class="page-btn" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">上一页</button>
      <span class="page-info">第 {{ pageNum }} / {{ totalPages }} 页（共 {{ total }} 条）</span>
      <button class="page-btn" :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { getAdminUsers, createAdminUser, updateUserStatus, resetUserPassword, deleteAdminUser } from '@/api/user';

const emit = defineEmits(['toast']);

const users = ref([]);
const loading = ref(false);
const error = ref(null);
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const showCreate = ref(false);
const creating = ref(false);
const actingId = ref(null);
const createForm = ref({ username: '', idCard: '', role: 0, status: 1 });

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

const extractPage = (data) => {
  if (data?.records) return data;
  if (data?.list) return { records: data.list, total: data.total };
  if (Array.isArray(data)) return { records: data, total: data.length };
  return { records: [], total: 0 };
};

const fetchUsers = async () => {
  loading.value = true;
  error.value = null;
  try {
    const res = await getAdminUsers({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    });
    let data = res;
    if (data?.code === 200) data = data.data;
    else if (data?.data?.code === 200) data = data.data.data;
    const page = extractPage(data);
    users.value = page.records || [];
    total.value = page.total || users.value.length;
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '加载失败，请重试';
  } finally {
    loading.value = false;
  }
};

const search = () => { pageNum.value = 1; fetchUsers(); };
const goPage = (p) => { pageNum.value = p; fetchUsers(); };

const handleCreate = async () => {
  if (!createForm.value.username.trim()) {
    emit('toast', { message: '✗ 用户名不能为空', type: 'error' });
    return;
  }
  creating.value = true;
  try {
    const res = await createAdminUser({
      username: createForm.value.username.trim(),
      idCard: createForm.value.idCard || undefined,
      role: Number(createForm.value.role),
      status: Number(createForm.value.status)
    });
    const initialPwd = res?.data;
    emit('toast', {
      message: initialPwd
        ? `✅ ${res.message || '用户创建成功'}，初始密码：${initialPwd}`
        : '✅ ' + (res.message || '用户创建成功'),
      type: 'success'
    });
    showCreate.value = false;
    createForm.value = { username: '', idCard: '', role: 0, status: 1 };
    await fetchUsers();
  } catch (err) {
    const code = err.response?.data?.code;
    const msg = err.response?.data?.message || err.message || '创建失败';
    // 用户名已被删除：弹窗确认后重建
    if (code === 409 && msg.includes('已被删除')) {
      const ok = window.confirm(`用户名「${createForm.value.username}」已被删除。是否重建该用户？重建后将生成新的初始密码，原历史预约记录保留。`);
      if (!ok) {
        emit('toast', { message: '已取消重建', type: 'info' });
        return;
      }
      try {
        const res2 = await createAdminUser({
          username: createForm.value.username.trim(),
          idCard: createForm.value.idCard || undefined,
          role: Number(createForm.value.role),
          status: Number(createForm.value.status),
          rebuild: true
        });
        const initialPwd = res2?.data;
        emit('toast', {
          message: initialPwd ? `✅ 用户已重建，初始密码：${initialPwd}` : '✅ 用户已重建',
          type: 'success'
        });
        showCreate.value = false;
        createForm.value = { username: '', idCard: '', role: 0, status: 1 };
        await fetchUsers();
      } catch (err2) {
        const msg2 = err2.response?.data?.message || err2.message || '重建失败';
        emit('toast', { message: `✗ ${msg2}`, type: 'error' });
      }
      return;
    }
    emit('toast', { message: `✗ ${msg}`, type: 'error' });
  } finally {
    creating.value = false;
  }
};

const handleDelete = async (u) => {
  if (!window.confirm(`确定删除用户 ${u.username} 吗？删除后该账号将无法登录（历史预约记录保留）。`)) return;
  actingId.value = u.id;
  try {
    const res = await deleteAdminUser(u.id);
    emit('toast', { message: '✅ ' + (res.message || '用户已删除'), type: 'success' });
    await fetchUsers();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '删除失败';
    emit('toast', { message: `✗ ${msg}`, type: 'error' });
  } finally {
    actingId.value = null;
  }
};

const handleToggleStatus = async (u) => {
  const target = u.status === 1 ? 0 : 1;
  if (!window.confirm(`确定${target === 1 ? '启用' : '禁用'}用户 ${u.username} 吗？`)) return;
  actingId.value = u.id;
  try {
    const res = await updateUserStatus(u.id, target);
    emit('toast', { message: '✅ ' + (res.message || '操作成功'), type: 'success' });
    await fetchUsers();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '操作失败';
    emit('toast', { message: `✗ ${msg}`, type: 'error' });
  } finally {
    actingId.value = null;
  }
};

const handleResetPwd = async (u) => {
  if (!window.confirm(`确定重置用户 ${u.username} 的密码吗？重置后将显示临时密码，请转告用户尽快修改。`)) return;
  actingId.value = u.id;
  try {
    const res = await resetUserPassword(u.id);
    const temp = res?.data;
    emit('toast', {
      message: temp ? `✅ 密码已重置，临时密码：${temp}` : '✅ 密码已重置',
      type: 'success'
    });
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '重置失败';
    emit('toast', { message: `✗ ${msg}`, type: 'error' });
  } finally {
    actingId.value = null;
  }
};

const formatTime = (t) => {
  if (!t) return '-';
  return String(t).replace('T', ' ').slice(0, 16);
};

onMounted(fetchUsers);
</script>

<style scoped>
.admin-page {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  box-sizing: border-box;
}

.admin-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.admin-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.admin-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.refresh-btn {
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  color: var(--text-primary);
  background: var(--sidebar-hover);
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--sidebar-hover);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
}

.search-input:focus {
  border-color: rgba(102, 126, 234, 0.6);
}

.primary-btn {
  padding: 9px 18px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.primary-btn:hover:not(:disabled) {
  filter: brightness(1.1);
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.create-form {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--sidebar-bg);
  margin-bottom: 16px;
}

.form-input {
  flex: 1;
  min-width: 160px;
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--sidebar-hover);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
}

.form-input:focus {
  border-color: rgba(102, 126, 234, 0.6);
}

.state-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  color: var(--text-secondary);
}

.state-icon { font-size: 44px; margin-bottom: 14px; }

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border-color);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 14px;
}

@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.state-btn {
  margin-top: 14px;
  padding: 8px 22px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  font-size: 14px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--sidebar-bg);
  flex-wrap: wrap;
}

.user-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: var(--sidebar-hover);
  color: var(--text-secondary);
}

.tag-admin { background: rgba(102, 126, 234, 0.2); color: #667eea; }
.tag-ok { background: rgba(76, 175, 80, 0.16); color: #4caf50; }
.tag-bad { background: rgba(244, 67, 54, 0.16); color: #f44336; }
.tag-warn { background: rgba(255, 152, 0, 0.16); color: #ff9800; }
.tag-time { background: transparent; color: var(--text-secondary); }

.user-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.mini-btn {
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.mini-btn:hover:not(:disabled) {
  background: var(--sidebar-hover);
}

.mini-btn.danger {
  color: #f44336;
  border-color: rgba(244, 67, 54, 0.5);
}

.mini-btn.danger:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.12);
}

.mini-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 20px;
}

.page-btn {
  padding: 7px 16px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
}

.page-btn:hover:not(:disabled) {
  background: var(--sidebar-hover);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .admin-page { padding: 16px; }
}
</style>

