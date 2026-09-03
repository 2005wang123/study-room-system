<!-- src/components/Announcements.vue -->
<template>
  <div class="announcement-page">
    <div class="announcement-header">
      <div>
        <h2 class="announcement-title">📢 公告</h2>
        <p class="announcement-subtitle">自习室最新通知与规则说明</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="fetchAnnouncements">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" :class="{ spinning: loading }">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        <span>{{ loading ? '刷新中...' : '刷新' }}</span>
      </button>
    </div>

    <!-- 搜索 -->
    <div v-if="list.length > 0" class="search-box">
      <span class="search-icon">🔍</span>
      <input v-model="keyword" class="search-input" type="text" placeholder="搜索公告标题或内容..." />
    </div>

    <!-- 状态区 -->
    <div v-if="loading" class="state-wrapper">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="state-wrapper">
      <div class="state-icon">⚠️</div>
      <p>{{ error }}</p>
      <button class="state-btn" @click="fetchAnnouncements">重试</button>
    </div>

    <div v-else-if="filteredList.length === 0" class="state-wrapper">
      <div class="state-icon">📢</div>
      <p>{{ list.length === 0 ? '暂无公告' : '没有匹配的公告' }}</p>
    </div>

    <!-- 公告列表 -->
    <div v-else class="announcement-list">
      <div
        v-for="item in filteredList"
        :key="item.id"
        class="announcement-card"
        :class="{ expanded: expandedId === item.id }"
      >
        <div class="announcement-card-header" @click="toggleExpand(item.id)">
          <div class="announcement-info">
            <h3 class="announcement-name">{{ item.title }}</h3>
            <div class="announcement-meta">
              <span class="meta-item">👤 {{ item.publisher || '管理员' }}</span>
              <span class="meta-item">🕐 {{ formatTime(item.publishTime) }}</span>
            </div>
          </div>
          <span class="expand-arrow">{{ expandedId === item.id ? '▾' : '▸' }}</span>
        </div>

        <div v-if="expandedId === item.id" class="announcement-body">
          <div class="announcement-content">{{ item.content }}</div>

          <!-- 评论区 -->
          <div class="comment-section">
            <div class="comment-header">
              <span class="comment-title">💬 评论（{{ commentsMap[item.id]?.length || 0 }}）</span>
            </div>

            <!-- 评论加载状态 -->
            <div v-if="commentLoadingId === item.id" class="comment-state">
              <div class="loading-spinner small"></div>
              <p>评论加载中...</p>
            </div>
            <div v-else-if="commentErrorId === item.id" class="comment-state">
              <p>评论加载失败</p>
              <button class="state-btn small" @click="fetchComments(item.id)">重试</button>
            </div>

            <template v-else>
              <!-- 评论列表 -->
              <div v-if="(commentsMap[item.id] || []).length > 0" class="comment-list">
                <div v-for="c in commentsMap[item.id]" :key="c.id" class="comment-item">
                  <div class="comment-avatar">{{ avatarChar(c) }}</div>
                  <div class="comment-main">
                    <div class="comment-top">
                      <span class="comment-user">{{ c.username }}</span>
                      <span class="comment-time">{{ formatTime(c.createTime) }}</span>
                      <button
                        v-if="canDeleteComment(c)"
                        class="comment-delete"
                        title="删除评论"
                        @click="removeComment(c, item.id)"
                      >✕</button>
                    </div>
                    <div class="comment-content">{{ c.content }}</div>
                  </div>
                  <button
                    class="like-btn"
                    :class="{ liked: !!c.liked }"
                    :disabled="likeActingId === c.id"
                    @click="toggleLike(c, item.id)"
                  >
                    <span>{{ c.liked ? '❤️' : '🤍' }}</span>
                    <span>{{ c.likeCount || 0 }}</span>
                  </button>
                </div>
              </div>
              <div v-else class="comment-state">
                <p>暂无评论，快来抢沙发～</p>
              </div>

              <!-- 发表评论 -->
              <div class="comment-input-row">
                <textarea
                  :value="draftMap[item.id] || ''" @input="draftMap[item.id] = $event.target.value"
                  class="comment-input"
                  rows="2"
                  maxlength="500"
                  :placeholder="userInfo.isLoggedIn ? '写下你的评论...' : '登录后即可发表评论'"
                  @keydown.enter.exact.prevent="submitComment(item.id)"
                ></textarea>
                <button
                  class="comment-submit"
                  :disabled="submittingId === item.id || !(draftMap[item.id] || '').trim()"
                  @click="submitComment(item.id)"
                >{{ submittingId === item.id ? '发送中...' : '发表' }}</button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import {
  getAnnouncements,
  getAnnouncementComments,
  addAnnouncementComment,
  deleteAnnouncementComment,
  likeAnnouncementComment,
  unlikeAnnouncementComment
} from '@/api/announcement';

const props = defineProps({
  userInfo: {
    type: Object,
    default: () => ({ isLoggedIn: false, userId: null, role: 0 })
  }
});

const emit = defineEmits(['loaded', 'toast']);

const list = ref([]);
const loading = ref(false);
const error = ref(null);
const expandedId = ref(null);
const keyword = ref('');

// 评论数据（按公告ID缓存）
const commentsMap = reactive({});
const draftMap = reactive({});
const commentLoadingId = ref(null);
const commentErrorId = ref(null);
const submittingId = ref(null);
const likeActingId = ref(null);

const filteredList = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return list.value;
  return list.value.filter(item =>
    (item.title || '').toLowerCase().includes(kw) ||
    (item.content || '').toLowerCase().includes(kw)
  );
});

const pad = n => String(n).padStart(2, '0');

const formatTime = (time) => {
  if (!time) return '-';
  const t = String(time).replace('T', ' ');
  const match = t.match(/^(\d{4}-\d{2}-\d{2})[ ](\d{2}:\d{2})/);
  if (match) return `${match[1]} ${match[2]}`;
  return t;
};

const extractList = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.rows)) return data.rows;
  if (Array.isArray(data?.list)) return data.list;
  if (Array.isArray(data?.records)) return data.records;
  return [];
};

const toast = (message, type = 'info') => emit('toast', { message, type });

const fetchAnnouncements = async () => {
  loading.value = true;
  error.value = null;
  try {
    const res = await getAnnouncements();
    let data = res;
    if (data?.code === 200) data = data.data;
    else if (data?.data?.code === 200) data = data.data.data;
    list.value = extractList(data);
    emit('loaded', list.value);
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '加载失败，请重试';
  } finally {
    loading.value = false;
  }
};

const toggleExpand = (id) => {
  expandedId.value = expandedId.value === id ? null : id;
  if (expandedId.value === id && !commentsMap[id]) {
    fetchComments(id);
  }
};

// --- 评论相关 ---

const fetchComments = async (announcementId) => {
  commentLoadingId.value = announcementId;
  commentErrorId.value = null;
  try {
    const res = await getAnnouncementComments(announcementId);
    let data = res;
    if (data?.code === 200) data = data.data;
    else if (data?.data?.code === 200) data = data.data.data;
    commentsMap[announcementId] = Array.isArray(data) ? data : [];
  } catch (err) {
    commentErrorId.value = announcementId;
  } finally {
    commentLoadingId.value = null;
  }
};

const submitComment = async (announcementId) => {
  if (!props.userInfo.isLoggedIn) {
    toast('请先登录后再发表评论', 'warning');
    return;
  }
  const content = (draftMap[announcementId] || '').trim();
  if (!content) return;
  submittingId.value = announcementId;
  try {
    const res = await addAnnouncementComment(announcementId, content);
    let data = res;
    if (data?.code === 200) data = data.data;
    else if (data?.data?.code === 200) data = data.data.data;
    if (data && data.id) {
      if (!commentsMap[announcementId]) commentsMap[announcementId] = [];
      commentsMap[announcementId].unshift(data);
    } else {
      // 后端未返回完整对象时刷新列表
      await fetchComments(announcementId);
    }
    draftMap[announcementId] = '';
    toast('✅ 评论成功', 'success');
  } catch (err) {
    toast(`✗ ${err.response?.data?.message || err.message || '评论失败'}`, 'error');
  } finally {
    submittingId.value = null;
  }
};

const removeComment = async (comment, announcementId) => {
  if (!confirm('确认删除这条评论？')) return;
  likeActingId.value = comment.id;
  try {
    await deleteAnnouncementComment(comment.id);
    commentsMap[announcementId] = (commentsMap[announcementId] || []).filter(c => c.id !== comment.id);
    toast('✅ 评论已删除', 'success');
  } catch (err) {
    toast(`✗ ${err.response?.data?.message || err.message || '删除失败'}`, 'error');
  } finally {
    likeActingId.value = null;
  }
};

const toggleLike = async (comment, announcementId) => {
  if (!props.userInfo.isLoggedIn) {
    toast('请先登录后再点赞', 'warning');
    return;
  }
  if (likeActingId.value === comment.id) return;
  likeActingId.value = comment.id;
  const target = (commentsMap[announcementId] || []).find(c => c.id === comment.id);
  try {
    if (comment.liked) {
      await unlikeAnnouncementComment(comment.id);
      if (target) {
        target.liked = false;
        target.likeCount = Math.max(0, (target.likeCount || 0) - 1);
      }
    } else {
      await likeAnnouncementComment(comment.id);
      if (target) {
        target.liked = true;
        target.likeCount = (target.likeCount || 0) + 1;
      }
    }
  } catch (err) {
    toast(`✗ ${err.response?.data?.message || err.message || '操作失败'}`, 'error');
  } finally {
    likeActingId.value = null;
  }
};

const canDeleteComment = (comment) => {
  if (Number(props.userInfo.role) === 1) return true;
  return props.userInfo.isLoggedIn && Number(comment.userId) === Number(props.userInfo.userId);
};

const avatarChar = (comment) => {
  const name = comment.username || '用';
  return name ? name[0].toUpperCase() : '用';
};

onMounted(fetchAnnouncements);
</script>

<style scoped>
.announcement-page {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  box-sizing: border-box;
}

.announcement-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.announcement-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.announcement-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
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

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 搜索 */
.search-box {
  position: relative;
  margin-bottom: 16px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 11px 14px 11px 40px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--sidebar-bg);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.18);
}

.search-input::placeholder {
  color: var(--text-secondary);
  opacity: 0.6;
}

/* 状态区 */
.state-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  color: var(--text-secondary);
}

.state-icon {
  font-size: 44px;
  margin-bottom: 14px;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border-color);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 14px;
}

.loading-spinner.small {
  width: 20px;
  height: 20px;
  border-width: 2px;
  margin-bottom: 8px;
}

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

.state-btn.small {
  margin-top: 8px;
  padding: 6px 14px;
  font-size: 13px;
}

/* 公告列表 */
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.announcement-card {
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  overflow: hidden;
  transition: border-color 0.2s;
}

.announcement-card.expanded {
  border-color: rgba(102, 126, 234, 0.5);
}

.announcement-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  cursor: pointer;
}

.announcement-card-header:hover .announcement-name {
  color: #667eea;
}

.announcement-info {
  min-width: 0;
}

.announcement-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  transition: color 0.2s;
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

.expand-arrow {
  color: var(--text-secondary);
  font-size: 14px;
  flex-shrink: 0;
}

.announcement-body {
  padding: 0 18px 18px;
}

.announcement-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.8;
  white-space: pre-line;
  background: var(--sidebar-hover);
  border-radius: 10px;
  padding: 14px 16px;
}

/* 评论区 */
.comment-section {
  margin-top: 18px;
}

.comment-header {
  margin-bottom: 12px;
}

.comment-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.comment-state p {
  margin: 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: var(--sidebar-hover);
  border-radius: 10px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.comment-user {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.comment-delete {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all 0.2s;
}

.comment-delete:hover {
  color: #f44336;
  background: rgba(244, 67, 54, 0.12);
}

.comment-content {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.6;
  word-break: break-word;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}

.like-btn:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.1);
  border-color: rgba(244, 67, 54, 0.4);
}

.like-btn.liked {
  color: #f44336;
  border-color: rgba(244, 67, 54, 0.5);
  background: rgba(244, 67, 54, 0.08);
}

.like-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.comment-input-row {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  align-items: flex-end;
}

.comment-input {
  flex: 1;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: var(--sidebar-hover);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
  box-sizing: border-box;
}

.comment-input:focus {
  border-color: rgba(102, 126, 234, 0.6);
}

.comment-submit {
  padding: 9px 18px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.comment-submit:hover:not(:disabled) {
  filter: brightness(1.1);
}

.comment-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .announcement-page { padding: 16px; }
}
</style>
