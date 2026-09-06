<template>
  <div class="floor-editor">
    <!-- 顶部工具栏 -->
    <div class="fe-bar">
      <div class="fe-bar-sec">
        <span class="fe-label">楼层</span>
        <select class="fe-select" :value="floorId ?? ''" @change="handleFloorChange">
          <option value="" disabled>请选择楼层</option>
          <option v-for="fl in floors" :key="fl.id" :value="fl.id">
            {{ fl.floorName || fl.floor_name }}
          </option>
        </select>
        <button type="button" class="fe-btn ghost" title="自定义 / 创建楼层与区域" @click="managerVisible = true">
          🗂 楼层/区域管理
        </button>
      </div>

      <div class="fe-tools">
        <button
          v-for="t in tools"
          :key="t.id"
          type="button"
          class="fe-tool"
          :class="{ active: mode === t.id }"
          :title="t.tip"
          @click="mode = t.id"
        >
          <span class="fe-tool-icon">{{ t.icon }}</span
          >{{ t.label }}
        </button>
        <select v-if="mode === 'seat'" v-model.number="seatToolType" class="fe-select seat-type" title="新座位类型">
          <option :value="1">普通</option>
          <option :value="2">靠窗</option>
          <option :value="3">带插座</option>
        </select>
      </div>

      <div class="fe-bar-sec">
        <button type="button" class="fe-btn" title="撤销 Ctrl+Z" :disabled="!canUndo" @click="undo">↶</button>
        <button type="button" class="fe-btn" title="重做 Ctrl+Y" :disabled="!canRedo" @click="redo">↷</button>
        <button type="button" class="fe-btn danger" :disabled="!selectedId" @click="deleteSelected">删除</button>
        <span class="fe-sep"></span>
        <button type="button" class="fe-btn" title="缩小" @click="zoomOut">－</button>
        <span class="fe-zoom">{{ Math.round(zoom * 100) }}%</span>
        <button type="button" class="fe-btn" title="放大" @click="zoomIn">＋</button>
        <button type="button" class="fe-btn" @click="fitView">适应</button>
        <button type="button" class="fe-btn" @click="resetView">100%</button>
        <span class="fe-sep"></span>
        <button type="button" class="fe-btn" @click="pickBg" :title="doc.canvas?.bgImage ? '更换底图' : '上传底图'">
          {{ doc.canvas?.bgImage ? '换底图' : '底图' }}
        </button>
        <button v-if="doc.canvas?.bgImage" type="button" class="fe-btn" @click="removeBg">去底图</button>
        <input ref="bgFileEl" type="file" accept="image/*" class="fe-hidden" @change="onBgFile" />
        <button type="button" class="fe-btn" @click="clearAll">清空</button>
        <span class="fe-sep"></span>
        <button type="button" class="fe-btn ghost" :disabled="!dirty" @click="reloadLayout">放弃修改</button>
        <button type="button" class="fe-btn" :disabled="saving || !floorId" @click="saveDraft">
          {{ saving ? '保存中…' : '存草稿' }}
        </button>
        <button type="button" class="fe-btn primary" :disabled="publishing || !floorId" @click="publishNow">
          {{ publishing ? '发布中…' : '🚀 发布' }}
        </button>
      </div>
    </div>

    <!-- 画布 + 属性面板 -->
    <div class="fe-main">
      <div ref="viewportEl" class="fe-viewport">
        <svg
          ref="svgEl"
          class="fe-svg"
          :width="sceneW * zoom"
          :height="sceneH * zoom"
          :viewBox="`0 0 ${sceneW} ${sceneH}`"
          @pointerdown="onSvgPointerDown"
        >
          <!-- 画布背景与底图 -->
          <rect x="0" y="0" :width="sceneW" :height="sceneH" class="fe-bg" />
          <image
            v-if="doc.canvas?.bgImage"
            :href="doc.canvas.bgImage"
            x="0"
            y="0"
            :width="sceneW"
            :height="sceneH"
            preserveAspectRatio="xMidYMid meet"
          />

          <!-- 区域 -->
          <g v-for="a in doc.areas" :key="a.id" :data-fid="a.id">
            <rect
              :x="a.x"
              :y="a.y"
              :width="a.w"
              :height="a.h"
              :fill="a.fill || areaFill"
              :stroke="a.stroke || areaStroke"
              stroke-width="2"
              rx="6"
            />
            <text v-if="a.name" :x="a.x + 10" :y="a.y + 22" class="fe-area-name">{{ a.name }}</text>
          </g>

          <!-- 墙体 -->
          <line
            v-for="wl in doc.walls"
            :key="wl.id"
            :data-fid="wl.id"
            :x1="wl.x1"
            :y1="wl.y1"
            :x2="wl.x2"
            :y2="wl.y2"
            :stroke="wl.color || '#8a8a96'"
            :stroke-width="wl.thickness || 8"
            stroke-linecap="round"
          />

          <!-- 自由画笔 -->
          <polyline
            v-for="pt in doc.paths"
            :key="pt.id"
            :data-fid="pt.id"
            :points="polyPoints(pt.points)"
            :stroke="pt.color || '#999999'"
            :stroke-width="pt.thickness || 5"
            fill="none"
            stroke-linecap="round"
            stroke-linejoin="round"
          />

          <!-- 文字 -->
          <text
            v-for="tx in doc.texts"
            :key="tx.id"
            :data-fid="tx.id"
            :x="tx.x"
            :y="tx.y"
            :font-size="tx.fontSize || 24"
            :fill="tx.color || '#e6e6ee'"
            class="fe-text"
          >
            {{ tx.content }}
          </text>

          <!-- 座位 -->
          <g
            v-for="s in doc.seats"
            :key="s.id"
            :data-fid="s.id"
            :transform="`translate(${s.x + (s.w || 44) / 2} ${s.y + (s.h || 44) / 2}) rotate(${s.rotation || 0})`"
          >
            <rect
              :x="-(s.w || 44) / 2"
              :y="-(s.h || 44) / 2"
              :width="s.w || 44"
              :height="s.h || 44"
              rx="6"
              :fill="seatFill(s.seatType, s.status)"
              stroke="rgba(255,255,255,0.35)"
              stroke-width="1"
            />
            <text
              x="0"
              y="0"
              text-anchor="middle"
              dominant-baseline="central"
              class="fe-seat-no"
              :font-size="Math.max(10, Math.min(15, (s.w || 44) / 4.5))"
            >
              {{ s.seatNo }}
            </text>
          </g>

          <!-- 绘制预览 -->
          <g v-if="preview" class="fe-preview">
            <rect
              v-if="preview.kind === 'area'"
              :x="previewRect.x"
              :y="previewRect.y"
              :width="previewRect.w"
              :height="previewRect.h"
              fill="rgba(102,126,234,0.18)"
              stroke="#9db4ff"
              stroke-width="2"
              stroke-dasharray="6 4"
            />
            <line
              v-else-if="preview.kind === 'wall'"
              :x1="preview.x1"
              :y1="preview.y1"
              :x2="preview.x2"
              :y2="preview.y2"
              stroke="#9db4ff"
              stroke-width="3"
              stroke-dasharray="6 4"
            />
            <polyline
              v-else-if="preview.kind === 'path'"
              :points="polyPoints(preview.points)"
              stroke="#9db4ff"
              stroke-width="3"
              fill="none"
              stroke-dasharray="6 4"
            />
          </g>

          <!-- 选中框 -->
          <rect
            v-if="selBox"
            :x="selBox.x - 5"
            :y="selBox.y - 5"
            :width="selBox.w + 10"
            :height="selBox.h + 10"
            class="fe-selbox"
          />
        </svg>

        <div v-if="loadingLayout" class="fe-overlay">加载中…</div>
        <div v-else-if="isEmpty" class="fe-overlay fe-hint">
          左侧工具栏选择「区域 / 座位 / 墙体 / 画笔 / 文字」开始绘制
        </div>
      </div>

      <FloorPropPanel
        v-if="panelNode"
        :kind="panelNode.kind"
        :obj="panelNode.obj"
        :areas="areaOptions"
        @change="onPropChange"
        @delete="deleteSelected"
      />
      <div v-else class="fe-panel-empty">
        <p>📌 使用提示</p>
        <ul>
          <li>选择工具：点选对象后可拖动</li>
          <li>绘制区域：先画大区域，内部可再细分</li>
          <li>座位编号自动按“楼层-序号”生成</li>
          <li>完成后记得「存草稿」或「发布」</li>
        </ul>
        <p v-if="dirty" class="fe-dirty">● 有未保存修改</p>
      </div>

      <FloorAreaManager
        v-if="managerVisible"
        @close="managerVisible = false"
        @toast="onManagerToast"
        @changed="onCatalogChanged"
      />
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getAreas, getFloors } from '@/api/seat'
import { getDraftLayout, publishLayout, saveDraftLayout } from '@/api/floorLayout'
import type { ApiErrorShape, AreaInfo, FloorInfo } from '@/types/api'
import type {
  LayoutAreaShape,
  LayoutCanvasShape,
  LayoutPathShape,
  LayoutSeatShape,
  LayoutTextShape,
  LayoutWallShape
} from '@/types/layout'
import FloorPropPanel from './floor-editor/FloorPropPanel.vue'
import FloorAreaManager from './floor-editor/FloorAreaManager.vue'
import type { EditableNode, EditorKind, EditorTool } from './floor-editor/types'
import {
  MAX_ZOOM,
  MIN_ZOOM,
  SCENE_H_DEFAULT,
  SCENE_W_DEFAULT,
  areaDefaultFill,
  areaDefaultStroke,
  boundsOf,
  clamp,
  clampRectToScene,
  isSeatNoDuplicate,
  nextSeatNo,
  normalizeRect,
  pointInRect,
  pointOnPolyline,
  round1,
  seatFill,
  uid
} from '@/utils/floorEditor'

type ToastType = 'success' | 'error' | 'info' | 'warning'

const emit = defineEmits<{
  (e: 'toast', payload: { message: string; type: ToastType }): void
}>()
const toast = (message: string, type: ToastType = 'info') => emit('toast', { message, type })
const onManagerToast = (payload: { message: string; type: ToastType }) => toast(payload.message, payload.type)

// ============ 文档模型 ============
interface EditorDoc {
  canvas?: LayoutCanvasShape
  areas: LayoutAreaShape[]
  walls: LayoutWallShape[]
  paths: LayoutPathShape[]
  texts: LayoutTextShape[]
  seats: LayoutSeatShape[]
}

function emptyDoc(): EditorDoc {
  return {
    canvas: { width: SCENE_W_DEFAULT, height: SCENE_H_DEFAULT, bgImage: '' },
    areas: [],
    walls: [],
    paths: [],
    texts: [],
    seats: []
  }
}

function normalizeDoc(d: Partial<EditorDoc>): EditorDoc {
  const base = emptyDoc()
  const canvas = d.canvas || {}
  return {
    canvas: {
      ...base.canvas,
      ...canvas,
      width: Number(canvas.width) || SCENE_W_DEFAULT,
      height: Number(canvas.height) || SCENE_H_DEFAULT,
      bgImage: typeof canvas.bgImage === 'string' ? canvas.bgImage : ''
    },
    areas: Array.isArray(d.areas) ? (d.areas as LayoutAreaShape[]) : [],
    walls: Array.isArray(d.walls) ? (d.walls as LayoutWallShape[]) : [],
    paths: Array.isArray(d.paths) ? (d.paths as LayoutPathShape[]) : [],
    texts: Array.isArray(d.texts) ? (d.texts as LayoutTextShape[]) : [],
    seats: Array.isArray(d.seats) ? (d.seats as LayoutSeatShape[]) : []
  }
}

const doc = ref<EditorDoc>(emptyDoc())
const sceneW = computed(() => Number(doc.value.canvas?.width) || SCENE_W_DEFAULT)
const sceneH = computed(() => Number(doc.value.canvas?.height) || SCENE_H_DEFAULT)
const areaFill = areaDefaultFill()
const areaStroke = areaDefaultStroke()
const isEmpty = computed(
  () =>
    doc.value.areas.length === 0 &&
    doc.value.walls.length === 0 &&
    doc.value.paths.length === 0 &&
    doc.value.texts.length === 0 &&
    doc.value.seats.length === 0
)

// ============ 楼层 / 区域（数据库区域用于座位归属） ============
const floors = ref<FloorInfo[]>([])
const areaOptions = ref<AreaInfo[]>([])
const floorId = ref<number | null>(null)
const floorNumber = ref(1)

const floorName = computed(() => {
  const f = floors.value.find((x) => x.id === floorId.value)
  return f ? f.floorName || f.floor_name || `楼层 ${floorId.value}` : ''
})

function unwrapArr<T>(res: unknown): T[] {
  if (Array.isArray(res)) return res as T[]
  const r = res as { code?: number; data?: unknown } | null
  if (r && r.code === 200) {
    const d = r.data
    if (Array.isArray(d)) return d as T[]
    const page = d as { records?: T[]; list?: T[] } | null
    if (Array.isArray(page?.records)) return page!.records!
    if (Array.isArray(page?.list)) return page!.list!
  }
  return []
}

const loadFloors = async () => {
  try {
    const res = await getFloors()
    floors.value = unwrapArr<FloorInfo>(res)
    if (floors.value.length && floorId.value == null) {
      floorId.value = floors.value[0].id
    }
    if (floorId.value != null) await switchFloor()
  } catch (err) {
    toast('楼层加载失败：' + errMsg(err, '请检查后端服务'), 'error')
  }
}

const loadAreaOptions = async () => {
  if (floorId.value == null) {
    areaOptions.value = []
    return
  }
  try {
    const res = await getAreas(floorId.value)
    areaOptions.value = unwrapArr<AreaInfo>(res)
  } catch (err) {
    areaOptions.value = []
  }
}

const switchFloor = async () => {
  const f = floors.value.find((x) => x.id === floorId.value)
  floorNumber.value = f ? Number(f.floor_number ?? f.floorNumber ?? 1) || 1 : 1
  await loadAreaOptions()
  await loadLayout()
}

/** 楼层/区域管理弹窗关闭后刷新目录；若当前楼层被删除则自动切换到第一个可用楼层 */
const refreshCatalog = async () => {
  try {
    const res = await getFloors()
    floors.value = unwrapArr<FloorInfo>(res)
  } catch (err) {
    // 忽略：仅刷新目录
  }
  if (floorId.value != null && !floors.value.some((x) => x.id === floorId.value)) {
    if (floors.value.length) {
      floorId.value = floors.value[0].id
      await switchFloor()
    } else {
      floorId.value = null
      doc.value = emptyDoc()
      lastSavedJson.value = JSON.stringify(doc.value)
      areaOptions.value = []
      selectedId.value = null
    }
  } else {
    await loadAreaOptions()
  }
}

const onCatalogChanged = () => {
  refreshCatalog()
}

const handleFloorChange = async (e: Event) => {
  const v = Number((e.target as HTMLSelectElement).value)
  if (!Number.isFinite(v) || v === floorId.value) return
  if (dirty.value && !window.confirm('当前楼层有未保存的修改，切换将丢弃，确定继续？')) {
    ;(e.target as HTMLSelectElement).value = String(floorId.value ?? '')
    return
  }
  floorId.value = v
  await switchFloor()
}

// ============ 加载 / 保存 / 发布 ============
const loadingLayout = ref(false)
const saving = ref(false)
const publishing = ref(false)
const lastSavedJson = ref('')

const dirty = computed(() => lastSavedJson.value !== JSON.stringify(doc.value))

const loadLayout = async () => {
  loadingLayout.value = true
  selectedId.value = null
  preview.value = null
  try {
    if (floorId.value == null) {
      doc.value = emptyDoc()
      lastSavedJson.value = JSON.stringify(doc.value)
      return
    }
    const res = await getDraftLayout(floorId.value)
    const layout = res && res.code === 200 ? res.data : null
    if (layout && layout.layoutJson) {
      doc.value = normalizeDoc(JSON.parse(layout.layoutJson) as Partial<EditorDoc>)
    } else {
      doc.value = emptyDoc()
    }
    lastSavedJson.value = JSON.stringify(doc.value)
    resetHistory()
    await nextTick()
    fitView()
  } catch (err) {
    doc.value = emptyDoc()
    lastSavedJson.value = JSON.stringify(doc.value)
    toast('结构图加载失败：' + errMsg(err, '请重试'), 'error')
  } finally {
    loadingLayout.value = false
  }
}

const reloadLayout = async () => {
  if (dirty.value && !window.confirm('放弃当前未保存的修改并重新加载？')) return
  await loadLayout()
}

const saveDraft = async () => {
  if (floorId.value == null) {
    toast('请先选择楼层', 'warning')
    return
  }
  saving.value = true
  try {
    await saveDraftLayout(floorId.value, JSON.stringify(doc.value))
    lastSavedJson.value = JSON.stringify(doc.value)
    toast('✅ 草稿已保存', 'success')
  } catch (err) {
    toast('保存草稿失败：' + errMsg(err, '请重试'), 'error')
  } finally {
    saving.value = false
  }
}

const publishNow = async () => {
  if (floorId.value == null) {
    toast('请先选择楼层', 'warning')
    return
  }
  const dup = findDuplicateSeatNo()
  if (dup) {
    toast(`座位编号重复：${dup}，请先修改`, 'error')
    return
  }
  const count = doc.value.seats.length
  if (
    !window.confirm(`确定发布「${floorName.value}」的结构图吗？` + `\n将同步 ${count} 个座位到座位表，学生端立即生效。`)
  )
    return
  publishing.value = true
  try {
    const res = await publishLayout(floorId.value, JSON.stringify(doc.value))
    const result = res && res.code === 200 ? res.data : null
    if (!result) throw new Error('发布未返回结果')
    if (result.layoutJson) {
      doc.value = normalizeDoc(JSON.parse(result.layoutJson) as Partial<EditorDoc>)
    }
    lastSavedJson.value = JSON.stringify(doc.value)
    resetHistory()
    const warns = result.warnings || []
    if (warns.length) {
      toast(`⚠️ 已发布，但有 ${warns.length} 条提示：${warns[0]}`, 'warning')
    } else {
      toast('✅ 发布成功，座位已同步', 'success')
    }
    selectedId.value = null
  } catch (err) {
    toast('发布失败：' + errMsg(err, '请重试'), 'error')
  } finally {
    publishing.value = false
  }
}

const findDuplicateSeatNo = (): string | null => {
  const seen = new Set<string>()
  for (const s of doc.value.seats) {
    const no = String(s.seatNo || '')
      .trim()
      .toLowerCase()
    if (!no) continue
    if (seen.has(no)) return String(s.seatNo)
    seen.add(no)
  }
  return null
}

const clearAll = () => {
  if (!window.confirm('确定清空当前楼层的全部图形？清空后需重新绘制。')) return
  commitHistory()
  doc.value = emptyDoc()
  selectedId.value = null
}

// ============ 撤销 / 重做 ============
const undoStack = ref<string[]>([])
const redoStack = ref<string[]>([])
const canUndo = computed(() => undoStack.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)

function resetHistory(): void {
  undoStack.value = []
  redoStack.value = []
}

function commitHistory(): void {
  undoStack.value.push(JSON.stringify(doc.value))
  if (undoStack.value.length > 80) undoStack.value.shift()
  redoStack.value = []
}

let commitTimer: number | undefined
function commitSoon(): void {
  if (commitTimer) window.clearTimeout(commitTimer)
  commitTimer = window.setTimeout(() => {
    commitTimer = undefined
    commitHistory()
  }, 500)
}

function undo(): void {
  const prev = undoStack.value.pop()
  if (prev == null) return
  redoStack.value.push(JSON.stringify(doc.value))
  doc.value = JSON.parse(prev) as EditorDoc
  selectedId.value = null
}

function redo(): void {
  const next = redoStack.value.pop()
  if (next == null) return
  undoStack.value.push(JSON.stringify(doc.value))
  doc.value = JSON.parse(next) as EditorDoc
  selectedId.value = null
}

// ============ 视图（缩放 / 适应 / 平移） ============
const zoom = ref(1)
const viewportEl = ref<HTMLDivElement | null>(null)
const svgEl = ref<SVGSVGElement | null>(null)
const isSpaceDown = ref(false)
const managerVisible = ref(false)

function setZoom(z: number): void {
  zoom.value = clamp(z, MIN_ZOOM, MAX_ZOOM)
}
const zoomIn = () => setZoom(zoom.value * 1.2)
const zoomOut = () => setZoom(zoom.value / 1.2)
const resetView = () => setZoom(1)
function fitView(): void {
  const vp = viewportEl.value
  if (!vp) return
  const pad = 48
  const z = Math.min((vp.clientWidth - pad) / sceneW.value, (vp.clientHeight - pad) / sceneH.value, 1.2)
  setZoom(Math.max(MIN_ZOOM, z))
}

function scenePoint(e: PointerEvent): { x: number; y: number } {
  const svg = svgEl.value
  if (!svg) return { x: 0, y: 0 }
  const r = svg.getBoundingClientRect()
  if (r.width <= 0 || r.height <= 0) return { x: 0, y: 0 }
  return {
    x: ((e.clientX - r.left) * sceneW.value) / r.width,
    y: ((e.clientY - r.top) * sceneH.value) / r.height
  }
}

// ============ 工具与绘制 ============
type DragState =
  | { mode: 'pan'; startClientX: number; startClientY: number; scrollLeft: number; scrollTop: number }
  | {
      mode: 'move'
      kind: EditorKind
      id: string
      origin: MoveOrigin
      startSceneX: number
      startSceneY: number
      moved: boolean
    }
  | { mode: 'draw'; tool: 'area' | 'wall' | 'path'; startSceneX: number; startSceneY: number }
  | null

interface MoveOrigin {
  x?: number
  y?: number
  w?: number
  h?: number
  x1?: number
  y1?: number
  x2?: number
  y2?: number
  points?: number[][]
}

type PreviewShape =
  | { kind: 'area'; x1: number; y1: number; x2: number; y2: number }
  | { kind: 'wall'; x1: number; y1: number; x2: number; y2: number }
  | { kind: 'path'; points: number[][] }

const mode = ref<EditorTool>('select')
const seatToolType = ref(1)
const drag = ref<DragState>(null)
const preview = ref<PreviewShape | null>(null)

const tools: Array<{ id: EditorTool; icon: string; label: string; tip: string }> = [
  { id: 'select', icon: '🖱️', label: '选择', tip: '点选 / 拖动对象' },
  { id: 'area', icon: '▭', label: '区域', tip: '拖拽绘制区域' },
  { id: 'seat', icon: '🪑', label: '座位', tip: '点击放置座位' },
  { id: 'wall', icon: '〰', label: '墙体', tip: '拖拽绘制墙体' },
  { id: 'path', icon: '✏️', label: '画笔', tip: '按住拖动画自由路径' },
  { id: 'text', icon: 'T', label: '文字', tip: '点击添加文字' },
  { id: 'pan', icon: '✋', label: '抓手', tip: '拖动平移画布' }
]

const previewRect = computed(() => {
  const p = preview.value
  if (!p || p.kind !== 'area') return { x: 0, y: 0, w: 0, h: 0 }
  return normalizeRect(p.x1, p.y1, p.x2, p.y2)
})

function onSvgPointerDown(e: PointerEvent): void {
  if (e.button !== 0) return
  const pt = scenePoint(e)
  // 平移
  if (mode.value === 'pan' || isSpaceDown.value) {
    const vp = viewportEl.value
    drag.value = {
      mode: 'pan',
      startClientX: e.clientX,
      startClientY: e.clientY,
      scrollLeft: vp ? vp.scrollLeft : 0,
      scrollTop: vp ? vp.scrollTop : 0
    }
    return
  }
  // 选择
  if (mode.value === 'select') {
    const hit = hitTest(pt.x, pt.y)
    if (hit) {
      selectedId.value = hit.id
      drag.value = {
        mode: 'move',
        kind: hit.kind,
        id: hit.id,
        origin: snapshotMoveOrigin(hit.kind, hit.obj),
        startSceneX: pt.x,
        startSceneY: pt.y,
        moved: false
      }
    } else {
      selectedId.value = null
    }
    return
  }
  // 座位 / 文字：单击放置
  if (mode.value === 'seat') {
    placeSeat(pt)
    return
  }
  if (mode.value === 'text') {
    placeText(pt)
    return
  }
  // 区域 / 墙体 / 画笔：拖拽绘制
  const tool = mode.value as 'area' | 'wall' | 'path'
  drag.value = { mode: 'draw', tool, startSceneX: pt.x, startSceneY: pt.y }
  preview.value =
    tool === 'path'
      ? { kind: 'path', points: [[round1(pt.x), round1(pt.y)]] }
      : { kind: tool, x1: pt.x, y1: pt.y, x2: pt.x, y2: pt.y }
}

function onWinPointerMove(e: PointerEvent): void {
  const d = drag.value
  if (!d) return
  if (d.mode === 'pan') {
    const vp = viewportEl.value
    if (vp) {
      vp.scrollLeft = d.scrollLeft - (e.clientX - d.startClientX)
      vp.scrollTop = d.scrollTop - (e.clientY - d.startClientY)
    }
    return
  }
  const pt = scenePoint(e)
  if (d.mode === 'move') {
    const dx = pt.x - d.startSceneX
    const dy = pt.y - d.startSceneY
    if (!d.moved && Math.hypot(dx, dy) > 3) d.moved = true
    if (d.moved) applyMove(d.kind, d.id, d.origin, dx, dy)
    return
  }
  if (d.mode === 'draw' && preview.value) {
    const px = clamp(round1(pt.x), 0, sceneW.value)
    const py = clamp(round1(pt.y), 0, sceneH.value)
    if (preview.value.kind === 'path') {
      const pts = preview.value.points
      const last = pts[pts.length - 1]
      if (last && Math.hypot(px - last[0], py - last[1]) >= 5) pts.push([px, py])
    } else {
      preview.value.x2 = px
      preview.value.y2 = py
    }
  }
}

function onWinPointerUp(): void {
  const d = drag.value
  drag.value = null
  if (!d) return
  if (d.mode === 'move') {
    if (d.moved) commitHistory()
    return
  }
  if (d.mode === 'draw') finalizeDraw()
}

function onWinPointerCancel(): void {
  drag.value = null
  preview.value = null
}

function snapshotMoveOrigin(kind: EditorKind, obj: EditableNode): MoveOrigin {
  switch (kind) {
    case 'area': {
      const o = obj as LayoutAreaShape
      return { x: o.x, y: o.y, w: o.w, h: o.h }
    }
    case 'seat': {
      const o = obj as LayoutSeatShape
      return { x: o.x, y: o.y, w: o.w, h: o.h }
    }
    case 'wall': {
      const o = obj as LayoutWallShape
      return { x1: o.x1, y1: o.y1, x2: o.x2, y2: o.y2 }
    }
    case 'path': {
      const o = obj as LayoutPathShape
      return { points: (o.points || []).map((p) => [p[0], p[1]]) }
    }
    case 'text': {
      const o = obj as LayoutTextShape
      return { x: o.x, y: o.y }
    }
  }
}

function applyMove(kind: EditorKind, id: string, origin: MoveOrigin, dx: number, dy: number): void {
  const node = findNode(id)
  if (!node) return
  if (kind === 'area') {
    const o = node.obj as LayoutAreaShape
    const pos = clampRectToScene(
      (origin.x || 0) + dx,
      (origin.y || 0) + dy,
      o.w || 1,
      o.h || 1,
      sceneW.value,
      sceneH.value
    )
    o.x = pos.x
    o.y = pos.y
  } else if (kind === 'seat') {
    const o = node.obj as LayoutSeatShape
    const w = o.w || 44
    const h = o.h || 44
    const pos = clampRectToScene((origin.x || 0) + dx, (origin.y || 0) + dy, w, h, sceneW.value, sceneH.value)
    o.x = pos.x
    o.y = pos.y
  } else if (kind === 'text') {
    const o = node.obj as LayoutTextShape
    o.x = clamp(round1((origin.x || 0) + dx), 0, sceneW.value)
    o.y = clamp(round1((origin.y || 0) + dy), 0, sceneH.value)
  } else if (kind === 'wall') {
    const o = node.obj as LayoutWallShape
    o.x1 = clamp(round1((origin.x1 || 0) + dx), 0, sceneW.value)
    o.y1 = clamp(round1((origin.y1 || 0) + dy), 0, sceneH.value)
    o.x2 = clamp(round1((origin.x2 || 0) + dx), 0, sceneW.value)
    o.y2 = clamp(round1((origin.y2 || 0) + dy), 0, sceneH.value)
  } else if (kind === 'path') {
    const o = node.obj as LayoutPathShape
    o.points = (origin.points || []).map((p) => [
      clamp(round1(p[0] + dx), 0, sceneW.value),
      clamp(round1(p[1] + dy), 0, sceneH.value)
    ])
  }
}

function finalizeDraw(): void {
  const p = preview.value
  preview.value = null
  if (!p) return
  if (p.kind === 'area') {
    const r = normalizeRect(p.x1, p.y1, p.x2, p.y2)
    if (r.w < 10 || r.h < 10) return
    const area: LayoutAreaShape = {
      id: uid('a'),
      x: round1(r.x),
      y: round1(r.y),
      w: round1(r.w),
      h: round1(r.h),
      name: '',
      fill: areaDefaultFill(),
      stroke: areaDefaultStroke()
    }
    doc.value.areas.push(area)
    selectedId.value = area.id
    commitHistory()
  } else if (p.kind === 'wall') {
    if (Math.hypot(p.x2 - p.x1, p.y2 - p.y1) < 8) return
    const wall: LayoutWallShape = {
      id: uid('w'),
      x1: round1(p.x1),
      y1: round1(p.y1),
      x2: round1(p.x2),
      y2: round1(p.y2),
      color: '#8a8a96',
      thickness: 8
    }
    doc.value.walls.push(wall)
    selectedId.value = wall.id
    commitHistory()
  } else if (p.kind === 'path') {
    const pts = (p.points || []).map((pt) => [round1(pt[0]), round1(pt[1])])
    if (pts.length < 2) return
    const path: LayoutPathShape = {
      id: uid('p'),
      points: pts,
      color: '#999999',
      thickness: 5
    }
    doc.value.paths.push(path)
    selectedId.value = path.id
    commitHistory()
  }
}

function placeSeat(pt: { x: number; y: number }): void {
  if (floorId.value == null) {
    toast('请先选择楼层', 'warning')
    return
  }
  const size = 44
  const pos = clampRectToScene(pt.x - size / 2, pt.y - size / 2, size, size, sceneW.value, sceneH.value)
  const seat: LayoutSeatShape = {
    id: uid('s'),
    seatNo: nextSeatNo(floorNumber.value, doc.value.seats),
    x: pos.x,
    y: pos.y,
    w: size,
    h: size,
    rotation: 0,
    seatType: seatToolType.value,
    status: 0
  }
  doc.value.seats.push(seat)
  selectedId.value = seat.id
  commitHistory()
}

function placeText(pt: { x: number; y: number }): void {
  const text: LayoutTextShape = {
    id: uid('t'),
    x: clamp(round1(pt.x), 0, sceneW.value),
    y: clamp(round1(pt.y), 0, sceneH.value),
    content: '双击区域或座位可设置名称/编号（见右侧面板）',
    fontSize: 22,
    color: '#e6e6ee'
  }
  doc.value.texts.push(text)
  selectedId.value = text.id
  commitHistory()
}

// ============ 命中测试与选择 ============
function findNode(id: string): { kind: EditorKind; obj: EditableNode } | null {
  for (const s of doc.value.seats) if (s.id === id) return { kind: 'seat', obj: s }
  for (const t of doc.value.texts) if (t.id === id) return { kind: 'text', obj: t }
  for (const p of doc.value.paths) if (p.id === id) return { kind: 'path', obj: p }
  for (const w of doc.value.walls) if (w.id === id) return { kind: 'wall', obj: w }
  for (const a of doc.value.areas) if (a.id === id) return { kind: 'area', obj: a }
  return null
}

function hitTest(px: number, py: number): { kind: EditorKind; id: string; obj: EditableNode } | null {
  // 座位（最上层先命中）
  for (let i = doc.value.seats.length - 1; i >= 0; i--) {
    const s = doc.value.seats[i]
    if (pointInRect(px, py, s.x, s.y, s.w || 44, s.h || 44, s.rotation || 0)) {
      return { kind: 'seat', id: s.id, obj: s }
    }
  }
  for (let i = doc.value.texts.length - 1; i >= 0; i--) {
    const t = doc.value.texts[i]
    const b = boundsOf('text', t)
    if (b && px >= b.x && px <= b.x + b.w && py >= b.y && py <= b.y + b.h) {
      return { kind: 'text', id: t.id, obj: t }
    }
  }
  for (let i = doc.value.paths.length - 1; i >= 0; i--) {
    const p = doc.value.paths[i]
    if (pointOnPolyline(px, py, p.points || [], Math.max(6, p.thickness || 5) / 2)) {
      return { kind: 'path', id: p.id, obj: p }
    }
  }
  for (let i = doc.value.walls.length - 1; i >= 0; i--) {
    const w = doc.value.walls[i]
    if (distToSegmentLite(px, py, w.x1, w.y1, w.x2, w.y2) <= Math.max(6, (w.thickness || 8) / 2)) {
      return { kind: 'wall', id: w.id, obj: w }
    }
  }
  for (let i = doc.value.areas.length - 1; i >= 0; i--) {
    const a = doc.value.areas[i]
    if (px >= a.x && px <= a.x + a.w && py >= a.y && py <= a.y + a.h) {
      return { kind: 'area', id: a.id, obj: a }
    }
  }
  return null
}

const selectedId = ref<string | null>(null)
const selectedNode = computed(() => (selectedId.value ? findNode(selectedId.value) : null))
const panelNode = computed(() => {
  const n = selectedNode.value
  return n ? { kind: n.kind, obj: n.obj } : null
})
const selBox = computed(() => {
  const n = selectedNode.value
  if (!n) return null
  return boundsOf(n.kind, n.obj)
})

function deleteSelected(): void {
  const n = selectedNode.value
  if (!n) return
  const id = (n.obj as { id: string }).id
  if (n.kind === 'area') doc.value.areas = doc.value.areas.filter((x) => x.id !== id)
  else if (n.kind === 'wall') doc.value.walls = doc.value.walls.filter((x) => x.id !== id)
  else if (n.kind === 'path') doc.value.paths = doc.value.paths.filter((x) => x.id !== id)
  else if (n.kind === 'text') doc.value.texts = doc.value.texts.filter((x) => x.id !== id)
  else if (n.kind === 'seat') doc.value.seats = doc.value.seats.filter((x) => x.id !== id)
  selectedId.value = null
  commitHistory()
}

// 属性面板变更
function onPropChange(key: string, value: string | number | null): void {
  const n = selectedNode.value
  if (!n) return
  const str = (v: string | number | null): string => String(v ?? '')
  const num = (v: string | number | null, fb = 0): number => {
    const x = Number(v)
    return Number.isFinite(x) ? x : fb
  }
  if (n.kind === 'area') {
    const a = n.obj as LayoutAreaShape
    if (key === 'name') a.name = str(value)
    else if (key === 'fill') a.fill = str(value) || undefined
    else if (key === 'stroke') a.stroke = str(value) || undefined
    else if (key === 'x') a.x = clamp(num(value), 0, sceneW.value)
    else if (key === 'y') a.y = clamp(num(value), 0, sceneH.value)
    else if (key === 'w') a.w = Math.max(10, num(value, 10))
    else if (key === 'h') a.h = Math.max(10, num(value, 10))
  } else if (n.kind === 'seat') {
    const s = n.obj as LayoutSeatShape
    if (key === 'seatNo') {
      const no = str(value).trim()
      if (isSeatNoDuplicate(no, s.id, doc.value.seats)) {
        toast('该座位编号已存在', 'warning')
        return
      }
      s.seatNo = no
    } else if (key === 'seatType') s.seatType = num(value, 1)
    else if (key === 'areaId') s.areaId = value == null || value === '' ? null : num(value)
    else if (key === 'status') s.status = num(value, 0)
    else if (key === 'rotation') s.rotation = clamp(num(value), -180, 180)
    else if (key === 'size') {
      const v = Math.max(20, Math.min(120, num(value, 44)))
      s.w = v
      s.h = v
    } else if (key === 'x')
      s.x = clampRectToScene(num(value), s.y || 0, s.w || 44, s.h || 44, sceneW.value, sceneH.value).x
    else if (key === 'y')
      s.y = clampRectToScene(s.x || 0, num(value), s.w || 44, s.h || 44, sceneW.value, sceneH.value).y
  } else if (n.kind === 'wall') {
    const w = n.obj as LayoutWallShape
    if (key === 'color') w.color = str(value) || undefined
    else if (key === 'thickness') w.thickness = Math.max(1, num(value, 8))
  } else if (n.kind === 'path') {
    const p = n.obj as LayoutPathShape
    if (key === 'color') p.color = str(value) || undefined
    else if (key === 'thickness') p.thickness = Math.max(1, num(value, 5))
  } else if (n.kind === 'text') {
    const t = n.obj as LayoutTextShape
    if (key === 'content') t.content = str(value)
    else if (key === 'fontSize') t.fontSize = Math.max(8, num(value, 24))
    else if (key === 'color') t.color = str(value) || undefined
    else if (key === 'x') t.x = clamp(num(value), 0, sceneW.value)
    else if (key === 'y') t.y = clamp(num(value), 0, sceneH.value)
  }
  commitSoon()
}

// ============ 底图 ============
const bgFileEl = ref<HTMLInputElement | null>(null)
const pickBg = () => bgFileEl.value?.click()

async function onBgFile(e: Event): Promise<void> {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    toast('请选择图片文件', 'warning')
    return
  }
  try {
    const dataUrl = await fileToDataUrl(file)
    const resized = await compressImage(dataUrl, 2000)
    doc.value.canvas = { ...(doc.value.canvas || {}), width: sceneW.value, height: sceneH.value, bgImage: resized }
    toast('✅ 底图已设置', 'success')
    commitHistory()
  } catch (err) {
    toast('底图处理失败：' + errMsg(err, '图片可能过大'), 'error')
  }
}

const removeBg = () => {
  doc.value.canvas = { ...(doc.value.canvas || {}), bgImage: '' }
  commitHistory()
}

function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('图片加载失败'))
    img.src = src
  })
}

async function compressImage(src: string, maxDim: number): Promise<string> {
  const img = await loadImage(src)
  const scale = Math.min(1, maxDim / Math.max(img.width, img.height))
  const w = Math.max(1, Math.round(img.width * scale))
  const h = Math.max(1, Math.round(img.height * scale))
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  if (!ctx) return src
  ctx.drawImage(img, 0, 0, w, h)
  return canvas.toDataURL('image/jpeg', 0.85)
}

// ============ 工具函数 ============
const polyPoints = (points?: number[][]): string => (points || []).map((p) => p.join(',')).join(' ')

function distToSegmentLite(px: number, py: number, x1: number, y1: number, x2: number, y2: number): number {
  const dx = x2 - x1
  const dy = y2 - y1
  const lenSq = dx * dx + dy * dy
  if (lenSq === 0) return Math.hypot(px - x1, py - y1)
  const t = clamp(((px - x1) * dx + (py - y1) * dy) / lenSq, 0, 1)
  return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy))
}

function errMsg(err: unknown, fallback: string): string {
  const e = err as ApiErrorShape
  return e?.response?.data?.message || e?.message || fallback
}

// ============ 键盘 ============
function isTypingTarget(t: EventTarget | null): boolean {
  const el = t as HTMLElement | null
  if (!el) return false
  return el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT' || el.isContentEditable
}

function onKeyDown(e: KeyboardEvent): void {
  const typing = isTypingTarget(e.target)
  if (e.code === 'Space' && !typing) {
    isSpaceDown.value = true
    e.preventDefault()
  }
  if (typing) return
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    e.preventDefault()
    if (e.shiftKey) redo()
    else undo()
    return
  }
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') {
    e.preventDefault()
    redo()
    return
  }
  if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    deleteSelected()
    return
  }
  if (e.key === 'Escape') {
    selectedId.value = null
    if (mode.value !== 'select') mode.value = 'select'
  }
}

function onKeyUp(e: KeyboardEvent): void {
  if (e.code === 'Space') {
    isSpaceDown.value = false
    if (drag.value && drag.value.mode === 'pan') drag.value = null
  }
}

// ============ 生命周期 ============
onMounted(async () => {
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('keyup', onKeyUp)
  window.addEventListener('pointermove', onWinPointerMove)
  window.addEventListener('pointerup', onWinPointerUp)
  window.addEventListener('pointercancel', onWinPointerCancel)
  await loadFloors()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('keyup', onKeyUp)
  window.removeEventListener('pointermove', onWinPointerMove)
  window.removeEventListener('pointerup', onWinPointerUp)
  window.removeEventListener('pointercancel', onWinPointerCancel)
})
</script>
<style scoped>
.floor-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 540px;
  background: var(--bg-primary, #0d0d12);
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.12));
  border-radius: 14px;
  overflow: hidden;
}

/* ---- 顶栏 ---- */
.fe-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 8px 10px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  background: var(--bg-glass-header, rgba(13, 13, 18, 0.6));
}
.fe-bar-sec {
  display: flex;
  align-items: center;
  gap: 4px;
}
.fe-label {
  font-size: 12px;
  color: var(--text-secondary, #9a9aa5);
  margin-right: 4px;
}
.fe-select {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.16));
  border-radius: 8px;
  color: var(--text-primary, #e8e8ec);
  font-size: 13px;
  padding: 5px 8px;
  max-width: 160px;
}
.fe-select.seat-type {
  max-width: 90px;
}
.fe-tools {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
}
.fe-tool {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary, #9a9aa5);
  border-radius: 8px;
  font-size: 13px;
  padding: 5px 9px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}
.fe-tool:hover {
  background: rgba(255, 255, 255, 0.07);
  color: var(--text-primary, #e8e8ec);
}
.fe-tool.active {
  background: rgba(102, 126, 234, 0.28);
  border-color: rgba(102, 126, 234, 0.7);
  color: #fff;
}
.fe-tool-icon {
  font-size: 14px;
  line-height: 1;
}
.fe-btn {
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.16));
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-primary, #e8e8ec);
  border-radius: 8px;
  font-size: 13px;
  padding: 5px 10px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}
.fe-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.12);
}
.fe-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.fe-btn.danger {
  color: #f2554a;
}
.fe-btn.danger:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.16);
}
.fe-btn.primary {
  background: linear-gradient(135deg, #5b6cff, #7a4dff);
  border-color: transparent;
  color: #fff;
  font-weight: 600;
}
.fe-btn.primary:hover:not(:disabled) {
  filter: brightness(1.12);
}
.fe-btn.ghost {
  background: transparent;
}
.fe-zoom {
  font-size: 12px;
  color: var(--text-secondary, #9a9aa5);
  min-width: 44px;
  text-align: center;
}
.fe-sep {
  width: 1px;
  height: 18px;
  background: var(--border-color, rgba(255, 255, 255, 0.16));
  margin: 0 2px;
}
.fe-hidden {
  display: none;
}

/* ---- 主体 ---- */
.fe-main {
  display: flex;
  flex: 1;
  min-height: 0;
}
.fe-viewport {
  position: relative;
  flex: 1;
  min-width: 0;
  overflow: auto;
  background:
    radial-gradient(circle at 1px 1px, rgba(255, 255, 255, 0.045) 1px, transparent 0) 0 0 / 24px 24px,
    var(--bg-primary, #0d0d12);
}
.fe-svg {
  display: block;
  cursor: crosshair;
}
.fe-bg {
  fill: #15151c;
}
.fe-svg text {
  user-select: none;
}
.fe-area-name {
  font-size: 18px;
  font-weight: 600;
  fill: rgba(255, 255, 255, 0.55);
  pointer-events: none;
}
.fe-text {
  user-select: none;
}
.fe-seat-no {
  fill: #fff;
  font-weight: 600;
  pointer-events: none;
}
.fe-preview {
  pointer-events: none;
}
.fe-selbox {
  fill: none;
  stroke: #7ec8ff;
  stroke-width: 2;
  stroke-dasharray: 6 4;
  pointer-events: none;
}
.fe-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  color: var(--text-secondary, #9a9aa5);
  font-size: 13px;
}
.fe-hint {
  font-size: 14px;
  padding-top: 90px;
  align-items: flex-start;
}

/* 空面板提示 */
.fe-panel-empty {
  width: 252px;
  flex: 0 0 auto;
  border-left: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  background: var(--bg-glass, rgba(13, 13, 18, 0.55));
  padding: 16px;
  font-size: 13px;
  color: var(--text-secondary, #9a9aa5);
  line-height: 1.8;
}
.fe-panel-empty p {
  margin: 0 0 8px;
  font-weight: 600;
  color: var(--text-primary, #e8e8ec);
}
.fe-panel-empty ul {
  margin: 0 0 10px;
  padding-left: 18px;
}
.fe-dirty {
  color: #ffb84d !important;
  font-size: 12px;
}
</style>
