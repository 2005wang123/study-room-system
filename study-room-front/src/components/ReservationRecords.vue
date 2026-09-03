<!-- src/components/ReservationRecords.vue -->
<template>
  <div class="records-page">
    <div class="records-header">
      <h2 class="records-title">📋 我的预约记录</h2>
      <p class="records-subtitle">查看您的自习室预约情况，可对进行中的预约进行取消操作</p>
    </div>

    <!-- 统计卡片 -->
    <div v-if="loaded && !error" class="stat-grid">
      <div class="stat-card">
        <div class="stat-value">{{ records.length }}</div>
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
        <div class="stat-value">{{ endedCount }}</div>
        <div class="stat-label">已结束</div>
      </div>
    </div>

    <!-- 筛选 Tab -->
    <div v-if="records.length > 0" class="filter-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="filter-tab"
        :class="{ active: currentTab === tab.value }"
        @click="currentTab = tab.value"
      >{{ tab.label }}</button>
    </div>

    <!-- 状态区 -->
    <div v-if="loading" class="state-wrapper">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="state-wrapper">
      <div class="state-icon">⚠️</div>
      <p>{{ error }}</p>
      <button class="state-btn" @click="fetchRecords">重试</button>
    </div>

    <div v-else-if="filteredRecords.length === 0" class="state-wrapper">
      <div class="state-icon">🗓️</div>
      <p>{{ records.length === 0 ? '暂无预约记录，去座位地图预约一个座位吧' : '当前分类下暂无记录' }}</p>
      <button v-if="records.length === 0" class="state-btn" @click="$emit('go-home')">去预约座位</button>
    </div>

    <!-- 预约列表 -->
    <div v-else class="record-list">
      <div
        v-for="record in filteredRecords"
        :key="record.id"
        class="record-card"
      >
        <div class="record-main">
          <div class="record-seat">
            <span class="seat-badge">🪑 {{ record.seatNo || '未知座位' }}</span>
            <span v-if="record.floorName" class="floor-tag">{{ record.floorName }}</span>
            <span v-if="record.areaName" class="floor-tag">{{ record.areaName }}</span>
          </div>
          <span class="status-badge" :class="'status-' + record.status">
            {{ getStatusText(record.status) }}
          </span>
        </div>

        <div class="record-time">
          <div class="time-row">
            <span class="time-icon">🕐</span>
            <span class="time-text">{{ formatTime(record.startTime) }}</span>
            <span class="time-arrow">→</span>
            <span class="time-text">{{ formatTime(record.endTime) }}</span>
          </div>
          <span class="duration-text">时长 {{ getDuration(record) }}</span>
        </div>

        <div v-if="isActive(record.status)" class="record-actions">
          <button
            v-if="Number(record.status) === 0"
            class="checkin-btn"
            :disabled="actioningId === record.id"
            @click="handleCheckIn(record)"
          >
            {{ actioningId === record.id ? '处理中...' : '✅ 签到' }}
          </button>
          <button
            v-if="Number(record.status) === 1"
            class="checkout-btn"
            :disabled="actioningId === record.id"
            @click="handleCheckOut(record)"
          >
            {{ actioningId === record.id ? '处理中...' : '🏁 签退' }}
          </button>
          <button
            v-if="Number(record.status) === 0"
            class="cancel-btn"
            :disabled="actioningId === record.id"
            @click="handleCancel(record)"
          >
            {{ actioningId === record.id ? '处理中...' : '取消预约' }}
          </button>
        </div>

        <div v-if="canDelete(record.status)" class="record-actions">
          <button
            class="delete-btn"
            :disabled="actioningId === record.id"
            @click="handleDelete(record)"
          >
            {{ actioningId === record.id ? '处理中...' : '🗑️ 删除记录' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { getMyReservations, cancelBooking, checkIn, checkOut, deleteReservation } from '@/api/seat';

const emit = defineEmits(['toast', 'go-home']);

const records = ref([]);
const loading = ref(false);
const loaded = ref(false);
const error = ref(null);
const actioningId = ref(null);
const currentTab = ref('all');

const STATUS = {
  0: { text: '待签到', cls: 'pending' },
  1: { text: '使用中', cls: 'using' },
  2: { text: '已完成', cls: 'done' },
  3: { text: '已违约', cls: 'violated' },
  4: { text: '已取消', cls: 'cancelled' }
};

const tabs = [
  { value: 'all', label: '全部' },
  { value: 'active', label: '进行中' },
  { value: 'done', label: '已完成' },
  { value: 'ended', label: '已结束' }
];

const activeCount = computed(() => records.value.filter(r => isActive(r.status)).length);
const completedCount = computed(() => records.value.filter(r => Number(r.status) === 2).length);
const endedCount = computed(() => records.value.filter(r => Number(r.status) === 3 || Number(r.status) === 4).length);

const filteredRecords = computed(() => {
  const list = records.value;
  switch (currentTab.value) {
    case 'active': return list.filter(r => isActive(r.status));
    case 'done': return list.filter(r => Number(r.status) === 2);
    case 'ended': return list.filter(r => Number(r.status) === 3 || Number(r.status) === 4);
    default: return list;
  }
});

const isActive = (status) => Number(status) === 0 || Number(status) === 1;

const getStatusText = (status) => STATUS[Number(status)]?.text || '未知';

const pad = n => String(n).padStart(2, '0');

const formatTime = (time) => {
  if (!time) return '-';
  // 兼容 "2026-08-25T10:00:00" 与 "2026-08-25 10:00:00"
  const t = String(time).replace('T', ' ');
  const match = t.match(/^(\d{4}-\d{2}-\d{2})[ ](\d{2}:\d{2})/);
  if (match) return `${match[1]} ${match[2]}`;
  return t;
};

const getDuration = (record) => {
  const start = new Date(record.startTime);
  const end = new Date(record.endTime);
  if (isNaN(start) || isNaN(end)) return '-';
  const mins = Math.round((end - start) / 60000);
  if (mins <= 0) return '-';
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  if (h > 0 && m > 0) return `${h}小时${m}分钟`;
  if (h > 0) return `${h}小时`;
  return `${m}分钟`;
};

const extractList = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.rows)) return data.rows;
  if (Array.isArray(data?.list)) return data.list;
  if (Array.isArray(data?.records)) return data.records;
  return [];
};

const fetchRecords = async () => {
  loading.value = true;
  error.value = null;
  try {
    const res = await getMyReservations();
    let data = res;
    if (data?.code === 200) data = data.data;
    else if (data?.data?.code === 200) data = data.data.data;
    records.value = extractList(data);
    loaded.value = true;
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '加载失败，请重试';
  } finally {
    loading.value = false;
  }
};

const handleCancel = async (record) => {
  if (!window.confirm(`确定取消 ${record.seatNo || record.seatId} 号座位的预约吗？`)) return;
  actioningId.value = record.id;
  try {
    await cancelBooking(record.seatId, record.id);
    emit('toast', { message: '✅ 预约已取消', type: 'success' });
    await fetchRecords();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '取消失败，请重试';
    emit('toast', { message: `✗ ${msg}`, type: 'error' });
    await fetchRecords();
  } finally {
    actioningId.value = null;
  }
};

const canDelete = (status) => [2, 3, 4].includes(Number(status));

const handleDelete = async (record) => {
  if (!window.confirm(`确定删除 ${record.seatNo || record.seatId} 号座位的这条预约记录吗？删除后不可恢复。`)) return;
  actioningId.value = record.id;
  try {
    await deleteReservation(record.id);
    emit('toast', { message: '✅ 预约记录已删除', type: 'success' });
    await fetchRecords();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '删除失败，请重试';
    emit('toast', { message: `✗ ${msg}`, type: 'error' });
  } finally {
    actioningId.value = null;
  }
};

const handleCheckIn = async (record) => {
  if (!window.confirm(`确认对 ${record.seatNo || record.seatId} 号座位签到吗？`)) return;
  actioningId.value = record.id;
  try {
    await checkIn(record.id);
    emit('toast', { message: '✅ 签到成功，请按时到座使用', type: 'success' });
    await fetchRecords();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '签到失败，请重试';
    emit('toast', { message: `✗ ${msg}`, type: 'error' });
  } finally {
    actioningId.value = null;
  }
};

const handleCheckOut = async (record) => {
  if (!window.confirm(`确认结束 ${record.seatNo || record.seatId} 号座位使用并签退吗？`)) return;
  actioningId.value = record.id;
  try {
    await checkOut(record.id);
    emit('toast', { message: '✅ 签退成功，座位已释放', type: 'success' });
    await fetchRecords();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '签退失败，请重试';
    emit('toast', { message: `✗ ${msg}`, type: 'error' });
  } finally {
    actioningId.value = null;
  }
};

onMounted(fetchRecords);
</script>

<style scoped>
.records-page {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  box-sizing: border-box;
}

.records-header {
  margin-bottom: 20px;
}

.records-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.records-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

/* 统计卡片 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
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
  font-size: 26px;
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

/* 筛选 Tab */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 7px 18px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tab:hover {
  color: var(--text-primary);
  background: var(--sidebar-hover);
}

.filter-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
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

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
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

/* 记录列表 */
.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-card {
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 16px 18px;
  transition: border-color 0.2s;
}

.record-card:hover {
  border-color: rgba(102, 126, 234, 0.5);
}

.record-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.record-seat {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.seat-badge {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.floor-tag {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--sidebar-hover);
  padding: 3px 10px;
  border-radius: 12px;
}

.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
  white-space: nowrap;
}

.status-0 { background: rgba(102, 126, 234, 0.2); color: #667eea; }
.status-1 { background: rgba(76, 175, 80, 0.18); color: #4caf50; }
.status-2 { background: rgba(76, 175, 80, 0.12); color: #81c784; }
.status-3 { background: rgba(244, 67, 54, 0.15); color: #f44336; }
.status-4 { background: rgba(158, 158, 158, 0.18); color: #9e9e9e; }

.record-time {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.time-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-icon {
  font-size: 14px;
}

.time-text {
  font-size: 14px;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.time-arrow {
  color: var(--text-secondary);
}

.duration-text {
  font-size: 12px;
  color: var(--text-secondary);
}

.record-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
  display: flex;
  justify-content: flex-end;
}

.cancel-btn {
  padding: 7px 18px;
  border-radius: 8px;
  border: 1px solid rgba(244, 67, 54, 0.5);
  background: transparent;
  color: #f44336;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.12);
}

.cancel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-btn {
  padding: 7px 18px;
  border-radius: 8px;
  border: 1px solid rgba(244, 67, 54, 0.5);
  background: transparent;
  color: #f44336;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  margin-right: 10px;
}

.delete-btn:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.12);
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.checkin-btn {
  padding: 7px 18px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #4caf50 0%, #2e7d32 100%);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  margin-right: 10px;
}

.checkin-btn:hover:not(:disabled) {
  filter: brightness(1.1);
}

.checkout-btn {
  padding: 7px 18px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  margin-right: 10px;
}

.checkout-btn:hover:not(:disabled) {
  filter: brightness(1.1);
}

.checkin-btn:disabled,
.checkout-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .records-page { padding: 16px; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
