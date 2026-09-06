// src/views/SeatMap.vue
<template>
  <div class="app-container">
    <!-- 侧边栏 - DeepSeek 风格 -->
    <aside class="sidebar" :class="{ 'sidebar-open': isSidebarOpen }">
      <div class="sidebar-content">
        <!-- 顶部用户信息 -->
        <div class="sidebar-user" @click="handleLogin">
          <div class="user-avatar">
            <span v-if="!userInfo.isLoggedIn">🤖</span>
            <img v-else :src="userInfo.avatar || '/default-avatar.png'" alt="avatar" />
          </div>
          <div class="user-info">
            <div class="user-name">{{ userInfo.isLoggedIn ? userInfo.name : '游客' }}</div>
            <div class="user-status">{{ userInfo.isLoggedIn ? '已登录' : '点击登录' }}</div>
          </div>
        </div>

        <!-- 导航菜单 - DeepSeek 风格 -->
        <nav class="sidebar-nav">
          <div
            v-for="item in menuItems"
            :key="item.key"
            class="nav-item"
            :class="{ 'nav-item-active': activeMenu === item.key }"
            @click="handleMenuClick(item.key)"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
          </div>
        </nav>

        <!-- 底部操作 -->
        <div class="sidebar-footer">
          <div class="nav-item" @click="toggleTheme">
            <span class="nav-icon">{{ isDarkTheme ? '🌙' : '☀️' }}</span>
            <span class="nav-label">{{ isDarkTheme ? '深色模式' : '浅色模式' }}</span>
          </div>
          <div class="nav-item" @click="handleLogout" v-if="userInfo.isLoggedIn">
            <span class="nav-icon">🚪</span>
            <span class="nav-label">退出登录</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- 移动端抽屉遮罩：点击关闭 -->
    <div v-if="isSidebarOpen" class="sidebar-backdrop" @click="isSidebarOpen = false"></div>

    <!-- 主内容区域 - 跟随侧边栏移动 -->
    <main class="main-content" :class="{ 'main-content-shifted': isSidebarOpen }">
      <!-- 顶部导航栏 -->
      <header class="top-header">
        <button class="menu-toggle" @click="toggleSidebar">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor">
            <path
              v-if="!isSidebarOpen"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 6h16M4 12h16M4 18h16"
            />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <div class="header-center">
          <h1 class="header-title">{{ pageTitle }}</h1>
          <div v-if="activeMenu === 'home'" class="header-stats">
            <span class="stat-item">
              <span class="stat-dot green"></span>
              空闲 {{ freeCount }}
            </span>
            <span class="stat-item">
              <span class="stat-dot partial"></span>
              部分可约 {{ partialCount }}
            </span>
            <span class="stat-item">
              <span class="stat-dot red"></span>
              已约满 {{ fullCount }}
            </span>
            <span class="stat-item">
              <span class="stat-dot gray"></span>
              维修 {{ maintenanceCount }}
            </span>
          </div>
        </div>

        <div class="header-actions">
          <button class="header-btn" @click="fetchSeats" :disabled="loading">
            <svg
              viewBox="0 0 24 24"
              width="20"
              height="20"
              fill="none"
              stroke="currentColor"
              :class="{ spinning: loading }"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
              />
            </svg>
          </button>
        </div>
      </header>

      <!-- 座位网格（仅首页显示） -->
      <template v-if="activeMenu === 'home'">
        <div class="seat-container">
          <!-- 楼层 / 区域 / 日期筛选 -->
          <div class="map-toolbar">
            <label class="toolbar-label">
              <span>楼层</span>
              <select v-model="selectedFloorId" @change="onFloorChange">
                <option :value="null">全部楼层</option>
                <option v-for="f in floors" :key="f.id" :value="f.id">
                  {{ f.floor_name || f.floorName }}
                </option>
              </select>
            </label>
            <label class="toolbar-label">
              <span>区域</span>
              <select v-model="selectedAreaId" @change="onAreaChange">
                <option :value="null">全部区域</option>
                <option v-for="a in areas" :key="a.id" :value="a.id">
                  {{ a.area_name || a.areaName }}
                </option>
              </select>
            </label>
            <label class="toolbar-label">
              <span>日期</span>
              <input type="date" v-model="selectedDate" :min="getTodayStr()" @change="fetchSeats" />
            </label>
          </div>

          <!-- 楼层结构图模式 -->
          <div v-if="publishedLayout && selectedAreaId == null" class="layout-mode-bar">
            <span class="layout-mode-title">{{
              layoutMode ? '🖼️ 楼层结构图（点击座位可预约）' : '💡 该楼层已有结构图'
            }}</span>
            <button class="state-btn small" @click="layoutMode = !layoutMode">
              {{ layoutMode ? '切换到列表视图' : '查看结构图' }}
            </button>
          </div>
          <template v-if="layoutMode && publishedLayout && selectedAreaId == null">
            <SeatMapCanvas :layout="publishedLayout" :seats="seats" @seat-click="handleSeatClick" />
          </template>
          <template v-else>
            <div v-if="loading" class="state-wrapper">
              <div class="loading-spinner"></div>
              <p>加载中...</p>
            </div>

            <div v-else-if="error" class="state-wrapper">
              <div class="state-icon">⚠️</div>
              <p>{{ error }}</p>
              <button class="state-btn" @click="fetchSeats">重试</button>
            </div>

            <div v-else-if="seats.length === 0" class="state-wrapper">
              <div class="state-icon">🪑</div>
              <p>暂无座位数据</p>
            </div>

            <div v-else class="area-list">
              <div v-for="group in seatGroups" :key="group.key" class="area-section">
                <div class="area-header">
                  <span class="area-title">{{ group.areaName }}</span>
                  <span class="area-count">{{ group.seats.length }} 个座位</span>
                </div>
                <div class="seat-grid">
                  <div
                    v-for="seat in group.seats"
                    :key="seat.id"
                    class="seat-card"
                    :class="getStatusClass(seat)"
                    @click="handleSeatClick(seat)"
                  >
                    <span class="seat-number">{{ seat.seat_no || seat.seatNo || seat.id }}</span>
                    <span class="seat-status-text">{{ getStatusText(seat) }}</span>
                    <span v-if="seat.myReservationId" class="seat-user-name">我的预约</span>
                    <span v-else-if="seat.userName" class="seat-user-name">{{ seat.userName }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>

      <!-- 预约记录 -->
      <template v-else-if="activeMenu === 'records'">
        <ReservationRecords v-if="userInfo.isLoggedIn" @toast="onChildToast" @go-home="activeMenu = 'home'" />
        <div v-else class="state-wrapper">
          <div class="state-icon">🔒</div>
          <p>请先登录后查看预约记录</p>
          <button class="state-btn" @click="isLoginVisible = true">去登录</button>
        </div>
      </template>

      <!-- 个人中心 -->
      <template v-else-if="activeMenu === 'profile'">
        <ProfileCenter
          v-if="userInfo.isLoggedIn"
          @toast="onChildToast"
          @logout="handleLogout"
          @switch-account="handleSwitchAccount"
        />
        <div v-else class="state-wrapper">
          <div class="state-icon">🔒</div>
          <p>请先登录后进入个人中心</p>
          <button class="state-btn" @click="isLoginVisible = true">去登录</button>
        </div>
      </template>

      <!-- 帮助中心 -->
      <template v-else-if="activeMenu === 'help'">
        <HelpCenter />
      </template>

      <!-- 公告 -->
      <template v-else-if="activeMenu === 'announcements'">
        <Announcements :user-info="userInfo" @loaded="onAnnouncementLoaded" @toast="onChildToast" />
      </template>

      <!-- 公告管理（仅管理员） -->
      <template v-else-if="activeMenu === 'admin-announcements'">
        <AnnouncementAdmin
          v-if="userInfo.isLoggedIn && Number(userInfo.role) === 1"
          @toast="onChildToast"
          @changed="onAnnouncementChanged"
        />
        <div v-else class="state-wrapper">
          <div class="state-icon">🔒</div>
          <p>仅管理员可访问公告管理</p>
        </div>
      </template>

      <!-- 用户管理（仅管理员） -->
      <template v-else-if="activeMenu === 'admin'">
        <AdminUsers v-if="userInfo.isLoggedIn && Number(userInfo.role) === 1" @toast="onChildToast" />
        <div v-else class="state-wrapper">
          <div class="state-icon">🔒</div>
          <p>仅管理员可访问用户管理</p>
        </div>
      </template>

      <!-- 楼层绘图（仅管理员） -->
      <template v-else-if="activeMenu === 'floor-editor'">
        <FloorLayoutEditor v-if="userInfo.isLoggedIn && Number(userInfo.role) === 1" @toast="onChildToast" />
        <div v-else class="state-wrapper">
          <div class="state-icon">🔒</div>
          <p>仅管理员可访问楼层绘图</p>
        </div>
      </template>
    </main>

    <!-- 登录弹窗 -->
    <LoginModal v-model:visible="isLoginVisible" @login-success="handleLoginSuccess" />
    <SeatBookingModal
      v-model:visible="bookingVisible"
      :seat="bookingSeat"
      :loading="bookingLoading"
      :initial-date="selectedDate"
      @confirm="handleBookingConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getSeats, getFloors, getAreas, bookSeat, cancelBooking } from '@/api/seat'
import { getPublishedLayout } from '@/api/floorLayout'
import { getAnnouncements } from '@/api/announcement'
import LoginModal from '@/components/LoginModal.vue'
import SeatBookingModal from '@/components/SeatBookingModal.vue'
import ReservationRecords from '@/components/ReservationRecords.vue'
import ProfileCenter from '@/components/ProfileCenter.vue'
import HelpCenter from '@/components/HelpCenter.vue'
import Announcements from '@/components/Announcements.vue'
import AdminUsers from '@/components/AdminUsers.vue'
import AnnouncementAdmin from '@/components/AnnouncementAdmin.vue'
import FloorLayoutEditor from '@/components/FloorLayoutEditor.vue'
import SeatMapCanvas from '@/components/SeatMapCanvas.vue'
import type {
  AnnouncementItem,
  ApiErrorShape,
  AreaInfo,
  FloorInfo,
  FloorLayoutData,
  LoginResult,
  SeatQuery,
  SeatReservation
} from '@/types/api'
import type { LayoutShape } from '@/types/layout'

// --- 本地类型 ---
interface SeatRecord {
  id: number | string
  seatId?: number | string
  seat_id?: number | string
  seatNo?: string
  seat_no?: string
  status: number | string
  floorId?: number | string
  floor_id?: number | string
  areaId?: number | string
  area_id?: number | string
  floorName?: string
  floor_name?: string
  areaName?: string
  area_name?: string
  bookedRatio?: number | null
  userName?: string
  myReservationId?: number | string | null
  reservations?: SeatReservation[]
}
interface ClickSeat {
  id?: number | string
  seatId?: number | string
  seatNo?: string
  status?: number | string
  bookedRatio?: number | null
  userName?: string
  myReservationId?: number | string | null
  reservations?: SeatReservation[]
}
interface UserState {
  isLoggedIn: boolean
  name?: string
  avatar?: string
  userId: number | null
  role?: number | string
  needChangePassword?: boolean
  isFirstLogin?: boolean
}

// --- 响应式数据 ---
const isSidebarOpen = ref(true)
const isLoginVisible = ref(false)
const isDarkTheme = ref(true)
const activeMenu = ref('home')
const seats = ref<SeatRecord[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const bookingVisible = ref(false)
const bookingSeat = ref<ClickSeat | null>(null)
const bookingLoading = ref(false)
const publishedLayout = ref<LayoutShape | null>(null)
const layoutMode = ref(false)

// --- 工具函数 ---
const getTodayStr = () => {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const formatTime = (time?: string | null) => {
  if (!time) return '-'
  const t = String(time).replace('T', ' ')
  const match = t.match(/^(\d{4}-\d{2}-\d{2})[ ](\d{2}:\d{2})/)
  if (match) return `${match[1]} ${match[2]}`
  return t
}

// --- 楼层 / 区域 / 日期筛选 ---
const floors = ref<FloorInfo[]>([])
const areas = ref<AreaInfo[]>([])
const selectedFloorId = ref<number | null>(null)
const selectedAreaId = ref<number | null>(null)
const selectedDate = ref(getTodayStr())

const userInfo = ref<UserState>({
  isLoggedIn: false,
  name: '',
  avatar: '',
  userId: null,
  role: 0
})

// --- 菜单配置 ---
// --- 公告未读角标（已读机制） ---
const READ_KEY = 'announcement_read_ids'
const announcementList = ref<AnnouncementItem[]>([])
const announcementReadIds = ref<number[]>(loadReadIds())
const announcementCount = computed(() => {
  if (!announcementList.value.length) return 0
  const readSet = new Set(announcementReadIds.value)
  return announcementList.value.filter((a) => !readSet.has(Number(a.id))).length
})

function loadReadIds(): number[] {
  try {
    const arr = JSON.parse(localStorage.getItem(READ_KEY) || '[]')
    return Array.isArray(arr) ? arr.map(Number).filter(Boolean) : []
  } catch (e) {
    return []
  }
}

const saveReadIds = () => {
  try {
    localStorage.setItem(READ_KEY, JSON.stringify(announcementReadIds.value))
  } catch (e) {}
}

const markAnnouncementsRead = () => {
  const ids = announcementList.value.map((a) => Number(a.id)).filter(Boolean)
  if (!ids.length) return
  const set = new Set([...announcementReadIds.value, ...ids])
  announcementReadIds.value = Array.from(set)
  saveReadIds()
}

const loadAnnouncementBadge = async () => {
  try {
    const res = await getAnnouncements()
    let data: unknown = res
    if ((data as { code?: number })?.code === 200) data = (data as { data: AnnouncementItem[] }).data
    else if ((data as { data?: { code?: number } })?.data?.code === 200)
      data = (data as { data: { data: AnnouncementItem[] } }).data.data
    announcementList.value = Array.isArray(data) ? (data as AnnouncementItem[]) : []
  } catch (e) {
    // 忽略角标加载失败
  }
}
const menuItems = computed(() => {
  const items = [
    { key: 'home', icon: '🏠', label: '座位地图' },
    { key: 'records', icon: '📋', label: '预约记录' },
    { key: 'profile', icon: '👤', label: '个人中心' },
    {
      key: 'announcements',
      icon: '📢',
      label: '公告',
      badge: announcementCount.value > 0 ? String(announcementCount.value) : ''
    },
    { key: 'help', icon: '❓', label: '帮助中心' }
  ]
  // 管理员专属菜单
  if (Number(userInfo.value.role) === 1) {
    items.splice(
      3,
      0,
      { key: 'admin', icon: '🛠️', label: '用户管理' },
      { key: 'admin-announcements', icon: '📝', label: '公告管理' },
      { key: 'floor-editor', icon: '🎨', label: '楼层绘图' }
    )
  }
  return items
})

// --- 页面标题 ---
const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    home: '📚 自习室座位管理',
    records: '📋 预约记录',
    profile: '👤 个人中心',
    help: '❓ 帮助中心',
    announcements: '📢 公告',
    admin: '🛠️ 用户管理',
    'admin-announcements': '📝 公告管理',
    'floor-editor': '🎨 楼层绘图'
  }
  return titles[activeMenu.value] || '📚 自习室座位管理'
})

// --- 计算属性 ---
const seatRatio = (s: ClickSeat) => {
  const r = Number(s.bookedRatio)
  return Number.isFinite(r) ? r : 0
}
const freeCount = computed(() => seats.value.filter((s) => seatRatio(s) <= 0 && Number(s.status) !== 3).length)
const partialCount = computed(
  () =>
    seats.value.filter((s) => {
      const r = seatRatio(s)
      return r > 0 && r < 1
    }).length
)
const fullCount = computed(() => seats.value.filter((s) => seatRatio(s) >= 1).length)
const maintenanceCount = computed(() => seats.value.filter((s) => Number(s.status) === 3).length)

// 按区域分组座位（查看全部楼层时分组标题带上楼层名）
const seatGroups = computed(() => {
  const map = new Map<string, { key: string; areaId: number | string; areaName: string; seats: SeatRecord[] }>()
  for (const seat of seats.value) {
    const floorId = seat.floor_id ?? seat.floorId ?? 'other'
    const areaId = seat.area_id ?? seat.areaId ?? 'other'
    const floorName = seat.floor_name || seat.floorName || ''
    const areaName = seat.area_name || seat.areaName || '其他区域'
    const key = `${floorId}-${areaId}`
    if (!map.has(key)) {
      const label = selectedFloorId.value == null && floorName ? `${floorName} · ${areaName}` : areaName
      map.set(key, { key, areaId, areaName: label, seats: [] })
    }
    const group = map.get(key)
    if (group) group.seats.push(seat)
  }
  return Array.from(map.values())
})

// --- 辅助函数 ---
const getStatusClass = (seat: SeatRecord) => {
  const s = Number(seat.status)
  if (s === 3) return 'status-maintenance'
  const r = seatRatio(seat)
  if (r <= 0) return 'status-free'
  if (r >= 1) return 'status-occupied'
  return 'status-partial'
}

const getStatusText = (seat: SeatRecord) => {
  const s = Number(seat.status)
  if (s === 3) return '维修中'
  const r = seatRatio(seat)
  if (r <= 0) return '空闲'
  if (r >= 1) return '已约满'
  return '部分可约'
}

// --- 数据获取 ---
const extractList = <T,>(data: unknown): T[] => {
  if (Array.isArray(data)) return data as T[]
  const d = data as { rows?: T[]; list?: T[]; records?: T[] } | null
  if (Array.isArray(d?.rows)) return d!.rows!
  if (Array.isArray(d?.list)) return d!.list!
  if (Array.isArray(d?.records)) return d!.records!
  return []
}

// 解析座位/楼层/区域等列表数据
const parseSeatData = <T,>(response: unknown): T[] => {
  try {
    // 1. 直接就是数组
    if (Array.isArray(response)) return response as T[]
    // 2. request.js 已解包：{ code, message, data }
    const r = response as { code?: number; data?: unknown } | null
    if (r?.code === 200) return extractList<T>(r.data)
    // 3. 兼容未解包的 axios 原始响应
    const r2 = response as { data?: { code?: number; data?: unknown } } | null
    if (r2?.data?.code === 200) return extractList<T>(r2.data.data)
  } catch (err) {
    console.error('解析数据出错:', err)
  }
  return []
}

const fetchSeats = async () => {
  loading.value = true
  error.value = null
  try {
    const params: SeatQuery = { date: selectedDate.value }
    if (selectedFloorId.value != null) params.floorId = selectedFloorId.value
    if (selectedAreaId.value != null) params.areaId = selectedAreaId.value
    const res = await getSeats(params)
    const listData = parseSeatData<SeatRecord>(res)
    seats.value = listData
  } catch (err) {
    const e1 = err as ApiErrorShape
    error.value = e1.message || '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

const fetchFloors = async () => {
  try {
    const res = await getFloors()
    const listData = parseSeatData<FloorInfo>(res)
    floors.value = listData
    if (listData.length > 0 && selectedFloorId.value == null) {
      selectedFloorId.value = listData[0].id
    }
    await fetchAreas()
    await fetchPublishedLayout(selectedFloorId.value)
    await fetchSeats()
  } catch (err) {
    const e2 = err as ApiErrorShape
    error.value = e2.message || '楼层加载失败'
    await fetchSeats()
  }
}

const fetchAreas = async () => {
  selectedAreaId.value = null
  if (selectedFloorId.value == null) {
    areas.value = []
    return
  }
  try {
    const res = await getAreas(selectedFloorId.value)
    const listData = parseSeatData<AreaInfo>(res)
    areas.value = listData
  } catch (err) {
    areas.value = []
  }
}

const fetchPublishedLayout = async (floorId: number | null) => {
  if (floorId == null) {
    publishedLayout.value = null
    layoutMode.value = false
    return
  }
  try {
    const res = await getPublishedLayout(floorId)
    const layout: FloorLayoutData | null = res?.code === 200 ? res.data : null
    publishedLayout.value = layout?.layoutJson ? (JSON.parse(layout.layoutJson) as LayoutShape) : null
    layoutMode.value = !!(publishedLayout.value && selectedAreaId.value == null)
  } catch (e) {
    publishedLayout.value = null
    layoutMode.value = false
  }
}

const onFloorChange = async () => {
  layoutMode.value = false
  await fetchAreas()
  await fetchPublishedLayout(selectedFloorId.value)
  await fetchSeats()
}

const onAreaChange = () => {
  layoutMode.value = false
  fetchSeats()
}

// --- 座位操作 ---
const handleSeatClick = async (seat: ClickSeat) => {
  const status = Number(seat.status)

  if (status === 3) {
    showToast('此座位维修中', 'warning')
    return
  }

  if (!userInfo.value.isLoggedIn) {
    if (window.confirm('请先登录，是否现在登录？')) {
      isLoginVisible.value = true
    }
    return
  }

  try {
    // 当前用户在该座位的进行中预约（用于取消）
    const myRes = (seat.reservations || []).find(
      (r) => Number(r.userId) === Number(userInfo.value.userId) && (Number(r.status) === 0 || Number(r.status) === 1)
    )

    if (myRes) {
      if (Number(myRes.status) === 1) {
        showToast('此座位正在使用中，请到「预约记录」签退', 'info')
        return
      }
      const range = `${formatTime(myRes.startTime)} ~ ${formatTime(myRes.endTime)}`
      if (!window.confirm(`取消 ${seat.seatNo || seat.id} 号座位 ${range} 的预约？`)) return
      await cancelBooking(Number(seat.id), myRes.id)
      await fetchSeats()
      showToast('✅ 取消成功', 'success')
      return
    }

    const r = seatRatio(seat)
    if (r >= 1) {
      showToast('该座位当天已约满，请选择其他时间或座位', 'warning')
      return
    }

    bookingSeat.value = seat
    bookingVisible.value = true
  } catch (err) {
    const e = err as ApiErrorShape
    showToast(`✗ 操作失败: ${e.message || '请重试'}`, 'error')
    await fetchSeats()
  }
}

// --- 轻量级 Toast 提示（替代 alert） ---
// --- 预约时间弹窗确认 ---
const handleBookingConfirm = async (payload: { seatId?: number | string; startTime: string; endTime: string }) => {
  bookingLoading.value = true
  try {
    await bookSeat({
      seatId: Number(payload.seatId),
      startTime: payload.startTime,
      endTime: payload.endTime
    })
    bookingVisible.value = false
    await fetchSeats()
    showToast('✓ 预约成功', 'success')
  } catch (err) {
    const e = err as ApiErrorShape
    const msg = e.response?.data?.message || e.message || '请重试'
    showToast(`✗ 预约失败: ${msg}`, 'error')
  } finally {
    bookingLoading.value = false
  }
}

let toastTimer: number | null = null
const showToast = (message: string, type: 'success' | 'error' | 'info' | 'warning' = 'info') => {
  // 移除已有的 toast
  const existing = document.querySelector('.custom-toast')
  if (existing) existing.remove()
  if (toastTimer) clearTimeout(toastTimer)

  const toast = document.createElement('div')
  toast.className = `custom-toast toast-${type}`
  toast.textContent = message
  document.body.appendChild(toast)

  // 动画进入
  requestAnimationFrame(() => {
    toast.classList.add('toast-show')
  })

  toastTimer = setTimeout(() => {
    toast.classList.remove('toast-show')
    setTimeout(() => toast.remove(), 300)
  }, 2500)
}

// --- 侧边栏控制 ---
const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

const handleMenuClick = (key: string) => {
  // 小屏幕点击后自动关闭
  if (window.innerWidth <= 768) {
    isSidebarOpen.value = false
  }

  activeMenu.value = key

  // 进入公告页：标记已读，角标消失
  if (key === 'announcements') {
    markAnnouncementsRead()
  }

  // 预约记录 / 个人中心需要登录
  if (!userInfo.value.isLoggedIn && (key === 'records' || key === 'profile')) {
    isLoginVisible.value = true
    return
  }

  // 回到首页时刷新楼层 / 区域 / 结构图与座位数据
  if (key === 'home') {
    fetchFloors()
  }
}

// 子页面 toast 事件处理
const onChildToast = ({
  message,
  type = 'info'
}: {
  message: string
  type?: 'success' | 'error' | 'info' | 'warning'
}) => {
  showToast(message, type)
}

// 公告页加载完成后同步列表，并在公告页内自动标记已读
const onAnnouncementLoaded = (data: AnnouncementItem[]) => {
  announcementList.value = Array.isArray(data) ? data : []
  if (activeMenu.value === 'announcements') {
    markAnnouncementsRead()
  }
}

// 公告管理页增删改后刷新角标
const onAnnouncementChanged = () => {
  loadAnnouncementBadge()
}
// --- 主题切换 ---
const applyTheme = () => {
  const root = document.documentElement
  if (isDarkTheme.value) {
    root.style.setProperty('--color-scheme', 'dark')
    root.style.setProperty('--bg-primary', '#0d0d12')
    root.style.setProperty('--text-primary', '#e8e8ec')
    root.style.setProperty('--text-secondary', '#9a9aa5')
    root.style.setProperty('--border-color', 'rgba(255, 255, 255, 0.12)')
    root.style.setProperty('--sidebar-bg', '#16161c')
    root.style.setProperty('--sidebar-hover', 'rgba(255, 255, 255, 0.08)')
    root.style.setProperty('--sidebar-active', 'rgba(102, 126, 234, 0.25)')
    root.style.setProperty('--bg-glass', 'rgba(13, 13, 18, 0.55)')
    root.style.setProperty('--bg-glass-header', 'rgba(13, 13, 18, 0.55)')
    root.style.setProperty('--sidebar-glass', 'rgba(22, 22, 28, 0.75)')
    root.style.setProperty('--particles-opacity', '1')
  } else {
    root.style.setProperty('--color-scheme', 'light')
    root.style.setProperty('--bg-primary', '#faf6ef')
    root.style.setProperty('--text-primary', '#1f2430')
    root.style.setProperty('--text-secondary', '#6b7280')
    root.style.setProperty('--border-color', 'rgba(31, 36, 48, 0.12)')
    root.style.setProperty('--sidebar-bg', '#ffffff')
    root.style.setProperty('--sidebar-hover', 'rgba(31, 36, 48, 0.06)')
    root.style.setProperty('--sidebar-active', 'rgba(102, 126, 234, 0.16)')
    root.style.setProperty('--bg-glass', 'rgba(250, 246, 239, 0.65)')
    root.style.setProperty('--bg-glass-header', 'rgba(250, 246, 239, 0.72)')
    root.style.setProperty('--sidebar-glass', 'rgba(255, 255, 255, 0.75)')
    root.style.setProperty('--particles-opacity', '0.4')
  }
}

const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value
  applyTheme()
}

// --- 登录相关 ---
const handleLogin = () => {
  isLoginVisible.value = true
}

const handleLoginSuccess = (userData: LoginResult & { name?: string; avatar?: string }) => {
  userInfo.value = {
    isLoggedIn: true,
    name: userData.username || userData.name || '用户',
    avatar: userData.avatar || '',
    userId: userData.id,
    role: userData.role || 0,
    needChangePassword: userData.needChangePassword || false,
    isFirstLogin: userData.isFirstLogin || false
  }

  // 保存用户信息到本地
  localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  // 保存token
  if (userData.token) {
    localStorage.setItem('token', userData.token)
  }

  isLoginVisible.value = false

  // 重新获取座位数据
  fetchSeats()
}

const handleLogout = () => {
  if (window.confirm('确认退出登录？')) {
    // 清除本地存储
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')

    userInfo.value = {
      isLoggedIn: false,
      name: '',
      avatar: '',
      userId: null
    }

    // 重新获取座位数据
    fetchSeats()
    showToast('已退出登录', 'success')
  }
}

const handleSwitchAccount = () => {
  if (!window.confirm('确认切换账户？当前账号将退出登录')) return
  // 清除本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')

  userInfo.value = {
    isLoggedIn: false,
    name: '',
    avatar: '',
    userId: null,
    role: 0
  }

  // 重新获取座位数据，并弹出登录框方便登录其他账号
  fetchSeats()
  isLoginVisible.value = true
  showToast('已退出当前账号，请登录其他账号', 'info')
}

// 监听未授权事件
const handleUnauthorized = () => {
  // token过期，弹出登录框
  isLoginVisible.value = true
  // 清除用户信息
  userInfo.value = {
    isLoggedIn: false,
    name: '',
    avatar: '',
    userId: null,
    role: 0
  }
  showToast('登录已过期，请重新登录', 'warning')
}

onMounted(() => {
  fetchFloors()
  applyTheme()

  // 加载公告列表用于侧边栏未读角标
  loadAnnouncementBadge()

  // 移动端默认收起侧边栏（抽屉式），桌面端默认展开
  if (window.innerWidth <= 768) {
    isSidebarOpen.value = false
  }

  // 监听全局未授权事件
  window.addEventListener('unauthorized', handleUnauthorized)

  // 检查是否有保存的用户信息
  const savedUserInfo = localStorage.getItem('userInfo')
  if (savedUserInfo) {
    try {
      const userData = JSON.parse(savedUserInfo)
      userInfo.value = {
        isLoggedIn: true,
        ...userData
      }
    } catch (e) {
      console.error('解析用户信息失败')
    }
  }
})

// 组件卸载时清理事件监听
onUnmounted(() => {
  window.removeEventListener('unauthorized', handleUnauthorized)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style>
/* --- CSS 变量 --- */
:root {
  --sidebar-width: 260px;
  --color-scheme: dark;
  --sidebar-bg: #16161c;
  --sidebar-hover: rgba(255, 255, 255, 0.08);
  --sidebar-active: rgba(102, 126, 234, 0.25);
  --bg-primary: #0d0d12;
  --bg-glass: rgba(13, 13, 18, 0.55);
  --bg-glass-header: rgba(13, 13, 18, 0.55);
  --sidebar-glass: rgba(22, 22, 28, 0.75);
  --particles-opacity: 1;
  --text-primary: #e8e8ec;
  --text-secondary: #9a9aa5;
  --border-color: rgba(255, 255, 255, 0.12);
}

/* --- 全局重置 --- */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
  overflow: hidden;
  color-scheme: var(--color-scheme, dark);
}

body {
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  transition:
    background 0.3s ease,
    color 0.3s ease;
}

/* --- 主容器 --- */
.app-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: transparent;
  transition:
    background-color 0.3s ease,
    color 0.3s ease;
}

/* --- DeepSeek 风格侧边栏 --- */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--sidebar-glass, var(--sidebar-bg));
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-right: 1px solid var(--border-color);
  transform: translateX(-100%);
  transition:
    transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    background-color 0.3s ease,
    border-color 0.3s ease;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-open {
  transform: translateX(0);
}

/* 移动端抽屉遮罩 */
.sidebar-backdrop {
  display: none;
}

.sidebar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  overflow-y: auto;
}

/* 侧边栏用户信息 */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 20px;
}

.sidebar-user:hover {
  background: var(--sidebar-hover);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

.user-status {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  color: var(--text-secondary);
}

.nav-item:hover {
  background: var(--sidebar-hover);
  color: var(--text-primary);
}

.nav-item-active {
  background: var(--sidebar-active);
  color: var(--text-primary);
}

.nav-item-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 4px 4px 0;
}

.nav-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.nav-label {
  font-size: 14px;
  flex: 1;
}

.nav-badge {
  background: #f44336;
  color: white;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
  font-weight: 600;
}

/* 侧边栏底部 */
.sidebar-footer {
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
  margin-top: 8px;
}

/* --- 主内容区 --- */
.main-content {
  flex: 1;
  width: 100vw;
  height: 100vh;
  padding: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--bg-glass, rgba(13, 13, 18, 0.55));
  position: relative;
  z-index: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.main-content-shifted {
  margin-left: var(--sidebar-width);
  width: calc(100vw - var(--sidebar-width));
}

/* --- 顶部导航栏 --- */
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-glass-header, rgba(13, 13, 18, 0.55));
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition:
    background-color 0.3s ease,
    border-color 0.3s ease;
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}

.menu-toggle {
  background: none;
  border: none;
  color: var(--text-primary);
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-toggle:hover {
  background: var(--sidebar-hover);
}

.header-center {
  flex: 1;
  text-align: center;
  padding: 0 16px;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.header-stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.stat-dot.green {
  background: #4caf50;
}
.stat-dot.red {
  background: #f44336;
}
.stat-dot.gray {
  background: #9e9e9e;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.header-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-btn:hover {
  background: var(--sidebar-hover);
  color: var(--text-primary);
}

.header-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* --- 座位容器 --- */
.seat-container {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* --- 状态页面 --- */
.state-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--text-secondary);
}

.state-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

.state-btn {
  margin-top: 16px;
  padding: 8px 24px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  font-size: 14px;
  transition: opacity 0.2s;
}

.state-btn:hover {
  .state-btn.small {
    margin-top: 0;
    padding: 5px 14px;
    font-size: 13px;
  }

  .layout-mode-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 16px;
    margin-bottom: 16px;
    background: var(--sidebar-hover, rgba(255, 255, 255, 0.08));
    border: 1px solid var(--border-color, rgba(255, 255, 255, 0.12));
    border-radius: 10px;
    flex-wrap: wrap;
  }
  .layout-mode-title {
    font-size: 14px;
    color: var(--text-secondary, #9a9aa5);
  }
  opacity: 0.9;
}

/* --- 座位网格 --- */
.seat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 160px));
  gap: 20px;
  justify-content: center;
  max-width: 1200px;
  margin: 0 auto;
}

.seat-card {
  aspect-ratio: 1;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  user-select: none;
  min-height: 100px;
}

.seat-card:hover:not(.status-occupied):not(.status-maintenance) {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}

.seat-card:active:not(.status-occupied):not(.status-maintenance) {
  transform: scale(0.96);
}

.status-free {
  background: linear-gradient(145deg, #4caf50 0%, #388e3c 100%);
  box-shadow: 0 4px 16px rgba(76, 175, 80, 0.25);
}

.status-occupied {
  background: linear-gradient(145deg, #f44336 0%, #c62828 100%);
  box-shadow: 0 4px 16px rgba(244, 67, 54, 0.25);
  cursor: not-allowed;
  opacity: 0.7;
}

.status-maintenance {
  background: linear-gradient(145deg, #757575 0%, #424242 100%);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  cursor: not-allowed;
  opacity: 0.5;
}

.seat-number {
  font-size: clamp(1.2rem, 1.8vw, 1.8rem);
  font-weight: 700;
  letter-spacing: 0.5px;
}

.seat-status-text {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 2px;
}

.seat-user-name {
  font-size: 10px;
  opacity: 0.8;
  background: rgba(0, 0, 0, 0.2);
  padding: 2px 10px;
  border-radius: 12px;
  margin-top: 4px;
}

/* --- 自定义 Toast 提示 --- */
.custom-toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%) translateY(-20px);
  padding: 12px 24px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  z-index: 9999;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  pointer-events: none;
  max-width: 80vw;
  text-align: center;
}

.custom-toast.toast-show {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.toast-success {
  background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%);
}

.toast-error {
  background: linear-gradient(135deg, #f44336 0%, #c62828 100%);
}

.toast-warning {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
}

.toast-info {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* --- 响应式 --- */
@media (max-width: 768px) {
  :root {
    --sidebar-width: 280px;
  }

  .sidebar-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.45);
    z-index: 999;
  }

  .main-content-shifted {
    margin-left: 0;
    width: 100vw;
  }

  .sidebar {
    width: var(--sidebar-width);
  }

  .sidebar-open {
    transform: translateX(0);
  }

  .top-header {
    padding: 12px 16px;
  }

  .header-title {
    font-size: 16px;
  }

  .header-stats {
    font-size: 12px;
    gap: 12px;
  }

  .seat-container {
    padding: 16px;
  }

  .seat-grid {
    grid-template-columns: repeat(auto-fill, minmax(90px, 110px));
    gap: 14px;
  }
}

@media (max-width: 480px) {
  .header-stats {
    flex-wrap: wrap;
    gap: 8px;
  }

  .seat-grid {
    grid-template-columns: repeat(auto-fill, minmax(75px, 90px));
    gap: 10px;
  }

  .seat-number {
    font-size: 1rem;
  }
}
/* --- 地图工具栏 --- */
.map-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-end;
  justify-content: center;
  margin: 0 auto 24px;
  max-width: 900px;
  padding: 14px 18px;
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 14px;
}

.toolbar-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.toolbar-label select,
.toolbar-label input {
  background: var(--sidebar-hover);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 14px;
  outline: none;
  min-width: 140px;
}

.toolbar-label select:focus,
.toolbar-label input:focus {
  border-color: #667eea;
}

.toolbar-label option {
  background-color: var(--bg-primary, #0f172a);
  color: var(--text-primary, #e2e8f0);
}

/* --- 区域分组 --- */
.area-list {
  display: flex;
  flex-direction: column;
  gap: 28px;
  max-width: 1200px;
  margin: 0 auto;
}

.area-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.area-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.area-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}

.area-count {
  font-size: 12px;
  color: var(--text-secondary);
}

/* --- 半红半绿（部分可约） --- */
.status-partial {
  background: linear-gradient(135deg, #f44336 0%, #f44336 50%, #4caf50 50%, #4caf50 100%);
  box-shadow: 0 4px 16px rgba(244, 67, 54, 0.2);
}

.stat-dot.partial {
  background: linear-gradient(135deg, #f44336 50%, #4caf50 50%);
}

@media (max-width: 768px) {
  .map-toolbar {
    padding: 12px;
  }
  .toolbar-label {
    width: 100%;
  }
  .toolbar-label select,
  .toolbar-label input {
    width: 100%;
    min-width: 0;
  }
}
</style>
