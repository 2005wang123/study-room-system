<!-- src/components/AnnouncementAdmin.vue 公告管理（仅管理员） -->
<template>
  <div class="admin-page">
    <div class="admin-header">
      <div>
        <h2 class="admin-title">📢 公告管理</h2>
        <p class="admin-subtitle">新增、编辑、下架或删除公告，仅已发布的公告对用户可见</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="fetchList">
        <span>{{ loading ? '刷新中...' : '刷新' }}</span>
      </button>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <input
        v-model="keyword"
        class="search-input"
        type="text"
        placeholder="搜索公告标题或内容..."
        @keyup.enter="search"
      />
      <select v-model="statusFilter" class="search-input status-select" @change="search">
        <option :value="''">全部状态</option>
        <option :value="1">已发布</option>
        <option :value="0">草稿/下架</option>
      </select>
      <button class="primary-btn" @click="openCreate">
        ➕ 新增公告
      </button>
    </div>

    <!-- 新增/编辑表单 -->
    <div v-if="formVisible" class="create-form">
      <input v-model="form.title" class="form-input" placeholder="公告标题（必填）" maxlength="200" />
      <textarea
        v-model="form.content"
        class="form-textarea"
        rows="5"
        placeholder="公告内容（必填，支持多行）"
      ></textarea>
      <div class="form-row">
        <select v-model="form.status" class="form-input status-select">
          <option :value="1">已发布（用户可见）</option>
          <option :value="0">草稿/下架（用户不可见）</option>
        </select>
        <button class="primary-btn" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : (editingId ? '保存修改' : '确认发布') }}
        </button>
        <button class="mini-btn" :disabled="saving" @click="closeForm">取消</button>
      </div>
    </div>

    <!-- 状态区 -->
    <div v-if="loading" class="state-wrapper">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="state-wrapper">
      <div class="state-icon">⚠️</div>
      <p>{{ error }}</p>
      <button class="state-btn" @click="fetchList">重试</button>
    </div>
    <div v-else-if="list.length === 0" class="state-wrapper">
      <div class="state-icon">📢</div>
      <p>暂无公告</p>
    </div>

    <!-- 公告列表 -->
    <div v-else class="announcement-list">
      <div v-for="item in list" :key="item.id" class="announcement-card">
        <div class="announcement-main">
          <div class="announcement-info">
            <div class="announcement-name-line">
              <span class="announcement-name">{{ item.title }}</span>
              <span class="tag" :class="item.status === 1 ? 'tag-ok' : 'tag-bad'">
                {{ item.status === 1 ? '已发布' : '草稿/下架' }}
              </span>
            </div>
            <div class="announcement-meta">
              <span class="meta-item">👤 {{ item.publisher || '管理员' }}</span>
              <span class="meta-item">🕐 {{ formatTime(item.publishTime) }}</span>
              <span class="meta-item">✏️ {{ formatTime(item.updateTime) }}</span>
            </div>
            <div class="announcement-preview">{{ item.content }}</div>
          </div>
        </div>
        <div class="announcement-actions">
          <button class="mini-btn" :disabled="actingId === item.id" @click="openEdit(item)">编辑</button>
          <button class="mini-btn danger" :disabled="actingId === item.id" @click="handleDelete(item)">删除</button>
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
import {
  getAdminAnnouncements,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement
} from '@/api/announcement';

const emit = defineEmits(['toast', 'changed']);

const list = ref([]);
const loading = ref(false);
const error = ref(null);
const keyword = ref('');
const statusFilter = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const formVisible = ref(false);
const editingId = ref(null);
const saving = ref(false);
const actingId = ref(null);
const form = ref({ title: '', content: '', status: 1 });

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

const extractPage = (data) => {
  if (data?.records) return data;
  if (data?.list) return { records: data.list, total: data.total };
  if (Array.isArray(data)) return { records: data, total: data.length };
  return { records: [], total: 0 };
};

const toast = (message, type = 'info') => emit('toast', { message, type });

const fetchList = async () => {
  loading.value = true;
  error.value = null;
  try {
    const res = await getAdminAnnouncements({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value)
    });
    let data = res;
    if (data?.code === 200) data = data.data;
    else if (data?.data?.code === 200) data = data.data.data;
    const page = extractPage(data);
    list.value = page.records || [];
    total.value = Number(page.total || list.value.length);
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '加载失败，请重试';
  } finally {
    loading.value = false;
  }
};

const search = () => {
  pageNum.value = 1;
  fetchList();
};

const goPage = (p) => {
  if (p < 1 || p > totalPages.value) return;
  pageNum.value = p;
  fetchList();
};

const openCreate = () => {
  editingId.value = null;
  form.value = { title: '', content: '', status: 1 };
  formVisible.value = true;
};

const openEdit = (item) => {
  editingId.value = item.id;
  form.value = {
    title: item.title || '',
    content: item.content || '',
    status: Number(item.status) === 1 ? 1 : 0
  };
  formVisible.value = true;
};

const closeForm = () => {
  formVisible.value = false;
  editingId.value = null;
};

const handleSave = async () => {
  if (!form.value.title || !form.value.title.trim()) {
    toast('请输入公告标题', 'warning');
    return;
  }
  if (!form.value.content || !form.value.content.trim()) {
    toast('请输入公告内容', 'warning');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      status: Number(form.value.status)
    };
    if (editingId.value) {
      await updateAnnouncement(editingId.value, payload);
      toast('✅ 公告已更新', 'success');
    } else {
      await createAnnouncement(payload);
      toast('✅ 公告已发布', 'success');
    }
    closeForm();
    await fetchList();
    emit('changed');
  } catch (err) {
    toast(`✗ ${err.response?.data?.message || err.message || '保存失败'}`, 'error');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (item) => {
  if (!confirm(`确认删除公告「${item.title}」？删除后用户将无法查看。`)) return;
  actingId.value = item.id;
  try {
    await deleteAnnouncement(item.id);
    toast('✅ 公告已删除', 'success');
    await fetchList();
    emit('changed');
  } catch (err) {
    toast(`✗ ${err.response?.data?.message || err.message || '删除失败'}`, 'error');
  } finally {
    actingId.value = null;
  }
};

const pad = n => String(n).padStart(2, '0');
const formatTime = (time) => {
  if (!time) return '-';
  const t = String(time).replace('T', ' ');
  const match = t.match(/^(\d{4}-\d{2}-\d{2})[ ](\d{2}:\d{2})/);
  if (match) return `${match[1]} ${match[2]}`;
  return t;
};

onMounted(fetchList);
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
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.refresh-btn:hover:not(:disabled) {
  color: var(--text-primary);
  background: var(--sidebar-hover);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.status-select {
  flex: 0 0 auto;
  min-width: 140px;
  width: auto;
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
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--sidebar-bg);
  margin-bottom: 16px;
}

.form-input {
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--sidebar-hover);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
}

.form-input:focus,
.form-textarea:focus {
  border-color: rgba(102, 126, 234, 0.6);
}

.form-textarea {
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--sidebar-hover);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
}

.form-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
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

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.announcement-card {
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

.announcement-main {
  min-width: 0;
  flex: 1;
}

.announcement-info {
  min-width: 0;
}

.announcement-name-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.announcement-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.announcement-meta {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 12px;
  color: var(--text-secondary);
}

.announcement-preview {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  white-space: pre-line;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.announcement-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: var(--sidebar-hover);
  color: var(--text-secondary);
  white-space: nowrap;
}

.tag-ok { background: rgba(76, 175, 80, 0.16); color: #4caf50; }
.tag-bad { background: rgba(244, 67, 54, 0.16); color: #f44336; }

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
