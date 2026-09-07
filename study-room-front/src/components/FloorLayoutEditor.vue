<template>
  <div class="floor-editor">
    <!-- 顶栏 -->
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

      <div class="fe-bar-sec">
        <button type="button" class="fe-btn" title="撤销 Ctrl+Z" :disabled="!canUndo" @click="undo">↶</button>
        <button type="button" class="fe-btn" title="重做 Ctrl+Y" :disabled="!canRedo" @click="redo">↷</button>
        <span class="fe-sep"></span>
        <button
          type="button"
          class="fe-btn"
          title="复制选中对象 Ctrl+C"
          :disabled="selectedCount === 0"
          @click="copySelected"
        >
          📋 复制
        </button>
        <button
          type="button"
          class="fe-btn"
          title="剪切选中对象 Ctrl+X"
          :disabled="selectedCount === 0"
          @click="cutSelected"
        >
          ✂ 剪切
        </button>
        <button
          type="button"
          class="fe-btn"
          title="粘贴 Ctrl+V（可多次粘贴）"
          :disabled="!canPaste"
          @click="pasteClipboard"
        >
          📥 粘贴
        </button>
        <span class="fe-sep"></span>
        <button
          type="button"
          class="fe-btn danger"
          title="删除选中 Delete"
          :disabled="selectedCount === 0"
          @click="deleteSelected"
        >
          删除
        </button>
        <button type="button" class="fe-btn" title="全选 Ctrl+A" :disabled="totalCount === 0" @click="selectAll">
          全选
        </button>
      </div>

      <div class="fe-bar-grow"></div>

      <div class="fe-bar-sec">
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
      </div>

      <div class="fe-bar-sec">
        <button type="button" class="fe-btn ghost" :disabled="!dirty" @click="reloadLayout">放弃修改</button>
        <button type="button" class="fe-btn" :disabled="saving || !floorId" @click="saveDraft">
          {{ saving ? '保存中…' : '存草稿' }}
        </button>
        <button type="button" class="fe-btn primary" :disabled="publishing || !floorId" @click="publishNow">
          {{ publishing ? '发布中…' : '🚀 发布' }}
        </button>
      </div>
    </div>

    <!-- 主体：左侧工具 + 画布 + 右侧属性面板 -->
    <div class="fe-main">
      <!-- 左侧工具条 -->
      <div class="fe-toolbar">
        <div class="fe-toolbar-title">工具</div>
        <button
          v-for="t in tools"
          :key="t.id"
          type="button"
          class="fe-tool"
          :class="{ active: mode === t.id }"
          :title="t.tip"
          @click="mode = t.id"
        >
          <span class="fe-tool-icon">{{ t.icon }}</span>
          <span class="fe-tool-label">{{ t.label }}</span>
        </button>

        <!-- 座位工具的次级选项 -->
        <template v-if="mode === 'seat'">
          <div class="fe-toolbar-sub">
            <label>类型</label>
            <select v-model.number="seatToolType" class="fe-select" title="新座位类型">
              <option :value="1">普通</option>
              <option :value="2">靠窗</option>
              <option :value="3">带插座</option>
            </select>
          </div>
          <div class="fe-toolbar-sub">
            <label>形状</label>
            <select v-model="seatShapeTool" class="fe-select" title="新座位形状">
              <option value="rect">方形</option>
              <option value="round">圆形</option>
              <option value="diamond">菱形</option>
            </select>
          </div>
        </template>

        <!-- 图形工具的次级选项 -->
        <template v-if="mode === 'shape'">
          <div class="fe-toolbar-sub">
            <label>图形</label>
            <select v-model="shapeKindTool" class="fe-select" title="要绘制的图形">
              <option v-for="g in graphicKinds" :key="g.id" :value="g.id">{{ g.label }}</option>
            </select>
          </div>
        </template>

        <div class="fe-toolbar-tip">右键单击：切换抓手/选择 · 空格拖动：平移</div>
      </div>

      <!-- 画布 -->
      <div ref="viewportEl" class="fe-viewport" @contextmenu.prevent>
        <svg
          ref="svgEl"
          class="fe-svg"
          :width="sceneW * zoom"
          :height="sceneH * zoom"
          :viewBox="`0 0 ${sceneW} ${sceneH}`"
          :style="{ cursor: svgCursor }"
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
            <text
              v-if="a.name"
              :x="areaLabel(a).x"
              :y="areaLabel(a).y"
              :text-anchor="areaLabel(a).anchor"
              :font-size="areaLabel(a).fontSize"
              :fill="a.nameColor || 'rgba(255,255,255,0.55)'"
              class="fe-area-name"
            >
              {{ a.name }}
            </text>
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

          <!-- 通用图形（桌椅 / 装饰） -->
          <g v-for="g in doc.shapes" :key="g.id" :data-fid="g.id" :transform="graphicTransform(g)">
            <ShapeGlyph :shape="g" />
          </g>

          <!-- 座位 -->
          <g
            v-for="s in doc.seats"
            :key="s.id"
            :data-fid="s.id"
            :transform="`translate(${s.x + (s.w || 44) / 2} ${s.y + (s.h || 44) / 2}) rotate(${s.rotation || 0})`"
          >
            <!-- 方形（圆角） -->
            <rect
              v-if="seatShapeOf(s) === 'rect'"
              :x="-(s.w || 44) / 2"
              :y="-(s.h || 44) / 2"
              :width="s.w || 44"
              :height="s.h || 44"
              rx="6"
              :fill="seatFill(s.seatType, s.status)"
              stroke="rgba(255,255,255,0.35)"
              stroke-width="1"
            />
            <!-- 圆形 -->
            <ellipse
              v-else-if="seatShapeOf(s) === 'round'"
              cx="0"
              cy="0"
              :rx="Math.max(0.5, (s.w || 44) / 2)"
              :ry="Math.max(0.5, (s.h || 44) / 2)"
              :fill="seatFill(s.seatType, s.status)"
              stroke="rgba(255,255,255,0.35)"
              stroke-width="1"
            />
            <!-- 菱形 -->
            <polygon
              v-else
              :points="seatDiamondLocal(s)"
              :fill="seatFill(s.seatType, s.status)"
              stroke="rgba(255,255,255,0.35)"
              stroke-width="1"
              stroke-linejoin="round"
            />
            <text
              :x="seatLabel(s).x"
              :y="0"
              :text-anchor="seatLabel(s).anchor"
              dominant-baseline="central"
              class="fe-seat-no"
              :font-size="seatLabelFont(s)"
              :fill="s.labelColor || '#fff'"
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
            <g v-else-if="preview.kind === 'shape'" :transform="graphicTransform(previewGraphic)">
              <ShapeGlyph :shape="previewGraphic" outline />
            </g>
          </g>

          <!-- 框选预览 -->
          <rect
            v-if="marquee"
            :x="marqueeRect.x"
            :y="marqueeRect.y"
            :width="marqueeRect.w"
            :height="marqueeRect.h"
            class="fe-marquee"
          />

          <!-- 多选 / 单选高亮框 -->
          <rect
            v-for="b in selBoxes"
            :key="b.id"
            :x="b.x - 5"
            :y="b.y - 5"
            :width="b.w + 10"
            :height="b.h + 10"
            class="fe-selbox"
          />

          <!-- 区域/图形缩放手柄（再次调整大小） -->
          <g v-if="resizeHandles.length" class="fe-handles">
            <circle
              v-for="h in resizeHandles"
              :key="h.dir"
              :cx="h.x"
              :cy="h.y"
              :r="5 / zoom"
              :stroke-width="1.5 / zoom"
              :style="{ cursor: h.cursor }"
            />
          </g>
        </svg>

        <div v-if="loadingLayout" class="fe-overlay">加载中…</div>
        <div v-else-if="isEmpty" class="fe-overlay fe-hint">
          从左侧工具栏选择「区域 / 座位 / 形状 / 墙体 / 画笔 / 文字」开始绘制
        </div>
      </div>

      <!-- 右侧：单选属性 / 多选操作 / 空状态提示 -->
      <FloorPropPanel
        v-if="panelNode"
        :kind="panelNode.kind"
        :obj="panelNode.obj"
        :areas="areaOptions"
        @change="onPropChange"
        @delete="deleteSelected"
      />
      <div v-else-if="selectedCount > 1" class="fe-panel fe-panel-multi">
        <div class="fe-panel-head">
          <span class="fe-panel-title">多选 · {{ selectedCount }} 个对象</span>
        </div>
        <div class="fe-panel-body">
          <p class="fe-multi-tip">拖动任意一个选中对象，可整体移动它们</p>
          <div class="fe-multi-actions">
            <button type="button" class="fe-btn" @click="copySelected">📋 复制</button>
            <button type="button" class="fe-btn danger" @click="deleteSelected">🗑 删除</button>
            <button type="button" class="fe-btn" @click="clearSelection">取消选择</button>
          </div>
        </div>
      </div>
      <div v-else class="fe-panel-empty">
        <p>📌 使用提示</p>
        <ul>
          <li>选择工具：点选对象可拖动；按住左键拖出虚线框可框选多个</li>
          <li>Shift+点击：加选/减选；框选后拖动可整体移动</li>
          <li>右键单击切换抓手（再点一次返回选择）；按住空格拖动可平移画布</li>
          <li>绘制区域：先画大区域，内部可再细分</li>
          <li>座位编号自动按“楼层-序号”生成，可改字号与对齐</li>
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
  LayoutGraphicKind,
  LayoutGraphicShape,
  LayoutPathShape,
  LayoutSeatShape,
  LayoutSeatShapeKind,
  LayoutTextShape,
  LayoutWallShape
} from '@/types/layout'
import FloorPropPanel from './floor-editor/FloorPropPanel.vue'
import FloorAreaManager from './floor-editor/FloorAreaManager.vue'
import ShapeGlyph from './floor-editor/ShapeGlyph.vue'
import type { EditableNode, EditorKind, EditorTool, SelectedNode } from './floor-editor/types'
import {
  MAX_ZOOM,
  MIN_ZOOM,
  SCENE_H_DEFAULT,
  SCENE_W_DEFAULT,
  areaDefaultFill,
  areaDefaultStroke,
  areaNamePos,
  boundsOf,
  clamp,
  clampRectToScene,
  graphicDefaultFill,
  graphicDefaultStroke,
  isSeatNoDuplicate,
  nextSeatNo,
  normalizeRect,
  pointInRect,
  pointOnPolyline,
  rectInside,
  round1,
  seatFill,
  seatLabelFontSize,
  seatLabelPos,
  uid,
  resizeHandlesFor,
  resizeRectInScene
} from '@/utils/floorEditor'
import type { ResizeDir } from '@/utils/floorEditor'

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
  shapes: LayoutGraphicShape[]
}

function emptyDoc(): EditorDoc {
  return {
    canvas: { width: SCENE_W_DEFAULT, height: SCENE_H_DEFAULT, bgImage: '' },
    areas: [],
    walls: [],
    paths: [],
    texts: [],
    seats: [],
    shapes: []
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
    seats: Array.isArray(d.seats) ? (d.seats as LayoutSeatShape[]) : [],
    shapes: Array.isArray(d.shapes) ? (d.shapes as LayoutGraphicShape[]) : []
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
    doc.value.seats.length === 0 &&
    doc.value.shapes.length === 0
)
const totalCount = computed(
  () =>
    doc.value.areas.length +
    doc.value.walls.length +
    doc.value.paths.length +
    doc.value.texts.length +
    doc.value.seats.length +
    doc.value.shapes.length
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
      clearSelection()
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
  clearSelection()
  preview.value = null
  marquee.value = null
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
    clearSelection()
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
  markHistoryGroup()
  doc.value = emptyDoc()
  clearSelection()
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

/** 把某个 JSON 快照作为“变更前状态”压入撤销栈 */
function pushUndoSnapshot(json: string): void {
  undoStack.value.push(json)
  if (undoStack.value.length > 80) undoStack.value.shift()
  redoStack.value = []
}

/** 在当前状态尚未被修改时调用，作为接下来一次/一组编辑的撤销点 */
function commitHistory(): void {
  pushUndoSnapshot(JSON.stringify(doc.value))
}

/**
 * 连续编辑（如属性面板连续输入）只建立一次撤销点：
 * 距离上一次建立超过 800ms 或从未建立时，先记录当前状态。
 */
let historyTimer: number | undefined
function markHistoryGroup(): void {
  if (historyTimer) return
  commitHistory()
  historyTimer = window.setTimeout(() => {
    historyTimer = undefined
  }, 800)
}

function undo(): void {
  const prev = undoStack.value.pop()
  if (prev == null) return
  redoStack.value.push(JSON.stringify(doc.value))
  doc.value = JSON.parse(prev) as EditorDoc
  clearSelection()
}

function redo(): void {
  const next = redoStack.value.pop()
  if (next == null) return
  undoStack.value.push(JSON.stringify(doc.value))
  doc.value = JSON.parse(next) as EditorDoc
  clearSelection()
}

// ============ 视图（缩放 / 适应 / 平移） ============
const zoom = ref(1)
const viewportEl = ref<HTMLDivElement | null>(null)
const svgEl = ref<SVGSVGElement | null>(null)
const isSpaceDown = ref(false)
const panActive = ref(false)
const managerVisible = ref(false)

const svgCursor = computed(() => {
  if (panActive.value) return 'grabbing'
  if (mode.value === 'pan') return 'grab'
  if (mode.value === 'select') return 'default'
  return 'crosshair'
})

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
      ids: string[]
      /** 拖动开始前的文档快照，用于在结束时建立撤销点 */
      historyJson: string
      origins: MoveOriginRef[]
      startSceneX: number
      startSceneY: number
      moved: boolean
    }
  | { mode: 'marquee'; additive: boolean; startSceneX: number; startSceneY: number }
  | {
      mode: 'select-pending'
      kind: EditorKind
      id: string
      startSceneX: number
      startSceneY: number
      moved: boolean
    }
  | {
      mode: 'resize'
      kind: 'area' | 'shape'
      id: string
      dir: ResizeDir
      origin: { x: number; y: number; w: number; h: number }
      minW: number
      minH: number
      startSceneX: number
      startSceneY: number
      /** 拖动开始前的文档快照，用于在结束时建立撤销点 */
      historyJson: string
      moved: boolean
    }
  | {
      mode: 'draw'
      tool: 'area' | 'wall' | 'path' | 'shape'
      startSceneX: number
      startSceneY: number
    }
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

interface MoveOriginRef {
  id: string
  origin: MoveOrigin
}

type PreviewShape =
  | { kind: 'area'; x1: number; y1: number; x2: number; y2: number }
  | { kind: 'wall'; x1: number; y1: number; x2: number; y2: number }
  | { kind: 'path'; points: number[][] }
  | { kind: 'shape'; x1: number; y1: number; x2: number; y2: number }

const mode = ref<EditorTool>('select')
const seatToolType = ref(1)
const seatShapeTool = ref<LayoutSeatShapeKind>('rect')
const shapeKindTool = ref<LayoutGraphicKind>('rect')
const drag = ref<DragState>(null)
const preview = ref<PreviewShape | null>(null)
const marquee = ref<{ x1: number; y1: number; x2: number; y2: number } | null>(null)

const tools: Array<{ id: EditorTool; icon: string; label: string; tip: string }> = [
  { id: 'select', icon: '🖱️', label: '选择', tip: '点选 / 框选对象（可多选后整体拖动）' },
  { id: 'pan', icon: '✋', label: '抓手', tip: '拖动平移画布' },
  { id: 'area', icon: '▭', label: '区域', tip: '拖拽绘制区域' },
  { id: 'seat', icon: '🪑', label: '座位', tip: '点击放置座位（可切换类型与形状）' },
  { id: 'shape', icon: '⬠', label: '形状', tip: '拖拽绘制矩形/圆形/三角等图形（桌椅、装饰）' },
  { id: 'wall', icon: '〰', label: '墙体', tip: '拖拽绘制墙体' },
  { id: 'path', icon: '✏️', label: '画笔', tip: '按住拖动画自由路径' },
  { id: 'text', icon: 'T', label: '文字', tip: '点击添加文字' }
]

const graphicKinds: Array<{ id: LayoutGraphicKind; label: string }> = [
  { id: 'rect', label: '矩形' },
  { id: 'roundRect', label: '圆角矩形' },
  { id: 'ellipse', label: '椭圆' },
  { id: 'circle', label: '圆形' },
  { id: 'triangle', label: '三角形' },
  { id: 'diamond', label: '菱形' }
]

const previewRect = computed(() => {
  const p = preview.value
  if (!p || p.kind !== 'area') return { x: 0, y: 0, w: 0, h: 0 }
  return normalizeRect(p.x1, p.y1, p.x2, p.y2)
})

const marqueeRect = computed(() => {
  const m = marquee.value
  if (!m) return { x: 0, y: 0, w: 0, h: 0 }
  return normalizeRect(m.x1, m.y1, m.x2, m.y2)
})

/** 图形绘制预览 */
const previewGraphic = computed((): LayoutGraphicShape => {
  const p = preview.value
  const r = p && p.kind === 'shape' ? normalizeRect(p.x1, p.y1, p.x2, p.y2) : { x: 0, y: 0, w: 0, h: 0 }
  const minSide = Math.min(r.w, r.h)
  return {
    id: 'preview',
    kind: shapeKindTool.value,
    x: r.x,
    y: r.y,
    w: r.w,
    h: r.h,
    rx: shapeKindTool.value === 'roundRect' ? Math.min(16, Math.max(0, minSide / 2)) : undefined,
    rotation: 0,
    fill: graphicDefaultFill(),
    stroke: '#9db4ff',
    strokeWidth: 2
  }
})

function onSvgPointerDown(e: PointerEvent): void {
  const pt = scenePoint(e)
  // 鼠标右键：单击切换“抓手 / 选择”工具
  if (e.button === 2) {
    mode.value = mode.value === 'pan' ? 'select' : 'pan'
    return
  }
  if (e.button !== 0) return

  // 抓手工具 / 按住空格：拖动平移画布
  if (mode.value === 'pan' || isSpaceDown.value) {
    startPan(e)
    return
  }

  // 选择
  if (mode.value === 'select') {
    const handle = hitResizeHandle({ x: pt.x, y: pt.y })
    if (handle) {
      startResize(handle, pt.x, pt.y)
      return
    }
    const hit = hitTest(pt.x, pt.y)
    if (hit) {
      if (e.shiftKey) {
        // Shift 点击：加选 / 减选
        toggleSelect(hit.id)
        if (selectedSet.value.has(hit.id)) beginMoveSelection(pt.x, pt.y)
        return
      }
      if (selectedSet.value.has(hit.id)) {
        // 已选中对象：直接拖动整体移动
        beginMoveSelection(pt.x, pt.y)
        return
      }
      // 未选中对象：先待定（单击=选中；拖拽：非区域=移动该对象，区域=框选）
      drag.value = {
        mode: 'select-pending',
        kind: hit.kind,
        id: hit.id,
        startSceneX: pt.x,
        startSceneY: pt.y,
        moved: false
      }
      return
    }
    // 空白处：开始框选（Shift 可追加），点击空白则取消选择
    drag.value = { mode: 'marquee', additive: e.shiftKey, startSceneX: pt.x, startSceneY: pt.y }
    marquee.value = { x1: pt.x, y1: pt.y, x2: pt.x, y2: pt.y }
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
  // 区域 / 形状 / 墙体 / 画笔：拖拽绘制
  const tool = mode.value as 'area' | 'wall' | 'path' | 'shape'
  drag.value = { mode: 'draw', tool, startSceneX: pt.x, startSceneY: pt.y }
  preview.value =
    tool === 'path'
      ? { kind: 'path', points: [[round1(pt.x), round1(pt.y)]] }
      : { kind: tool, x1: pt.x, y1: pt.y, x2: pt.x, y2: pt.y }
}

function startPan(e: PointerEvent): void {
  const vp = viewportEl.value
  panActive.value = true
  drag.value = {
    mode: 'pan',
    startClientX: e.clientX,
    startClientY: e.clientY,
    scrollLeft: vp ? vp.scrollLeft : 0,
    scrollTop: vp ? vp.scrollTop : 0
  }
}

/** 拖动开始时为每个选中对象记录初始几何 */
function beginMoveSelection(sx: number, sy: number): void {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  const origins: MoveOriginRef[] = ids
    .map((id) => {
      const n = findNode(id)
      return n ? { id, origin: snapshotMoveOrigin(n.kind, n.obj) } : null
    })
    .filter((x): x is { id: string; origin: MoveOrigin } => x !== null)
  drag.value = {
    mode: 'move',
    ids,
    historyJson: JSON.stringify(doc.value),
    origins,
    startSceneX: sx,
    startSceneY: sy,
    moved: false
  }
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
    if (d.moved) applyMoveByIds(d.ids, d.origins, dx, dy)
    return
  }
  if (d.mode === 'resize') {
    const dx = pt.x - d.startSceneX
    const dy = pt.y - d.startSceneY
    if (!d.moved && (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5)) d.moved = true
    if (d.moved) applyResizeDrag(d, dx, dy)
    return
  }
  if (d.mode === 'select-pending') {
    const dx = pt.x - d.startSceneX
    const dy = pt.y - d.startSceneY
    if (d.moved || Math.hypot(dx, dy) <= 3) return
    d.moved = true
    if (d.kind === 'area') {
      // 在未选中区域上拖拽 → 切换为框选，便于选取区域内的座位/桌椅
      drag.value = { mode: 'marquee', additive: false, startSceneX: d.startSceneX, startSceneY: d.startSceneY }
      marquee.value = { x1: d.startSceneX, y1: d.startSceneY, x2: pt.x, y2: pt.y }
    } else {
      // 在未选中对象上拖拽 → 选中并移动该对象
      selectedIds.value = [d.id]
      beginMoveSelection(pt.x, pt.y)
    }
    return
  }
  if (d.mode === 'marquee') {
    const m = marquee.value
    if (m) {
      m.x2 = pt.x
      m.y2 = pt.y
    }
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
  if (d.mode === 'pan') {
    panActive.value = false
    return
  }
  if (d.mode === 'move') {
    if (d.moved) pushUndoSnapshot(d.historyJson)
    return
  }
  if (d.mode === 'resize') {
    if (d.moved) pushUndoSnapshot(d.historyJson)
    return
  }
  if (d.mode === 'select-pending') {
    // 单击未选中对象 → 选中；拖动时已在上层转换为框选或移动
    if (!d.moved) selectedIds.value = [d.id]
    return
  }
  if (d.mode === 'marquee') {
    finishMarquee(d.additive)
    return
  }
  if (d.mode === 'draw') finalizeDraw()
}

function onWinPointerCancel(): void {
  drag.value = null
  preview.value = null
  marquee.value = null
  panActive.value = false
}

function snapshotMoveOrigin(kind: EditorKind, obj: EditableNode): MoveOrigin {
  switch (kind) {
    case 'area':
    case 'shape': {
      const o = obj as { x: number; y: number; w?: number; h?: number }
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

function applyMoveByIds(ids: string[], origins: MoveOriginRef[], dx: number, dy: number): void {
  for (const id of ids) {
    const n = findNode(id)
    if (!n) continue
    const ref = origins.find((o) => o.id === id)
    if (!ref) continue
    applyMove(n.kind, id, ref.origin, dx, dy)
  }
}

function applyMove(kind: EditorKind, id: string, origin: MoveOrigin, dx: number, dy: number): void {
  const node = findNode(id)
  if (!node) return
  if (kind === 'area' || kind === 'shape') {
    const o = node.obj as LayoutAreaShape | LayoutGraphicShape
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

/** 框选结算：选择完全落在框内的对象 */
function finishMarquee(additive: boolean): void {
  const m = marquee.value
  marquee.value = null
  if (!m) return
  const r = normalizeRect(m.x1, m.y1, m.x2, m.y2)
  if (r.w < 3 && r.h < 3) {
    // 视为点击空白处
    if (!additive) clearSelection()
    return
  }
  const inside: string[] = []
  for (const n of allNodes()) {
    const b = boundsOf(n.kind, n.obj)
    if (b && rectInside(b.x, b.y, b.w, b.h, r.x, r.y, r.w, r.h)) {
      inside.push((n.obj as { id: string }).id)
    }
  }
  if (additive) {
    const set = new Set([...selectedIds.value, ...inside])
    selectedIds.value = Array.from(set)
  } else {
    selectedIds.value = inside
  }
}

function finalizeDraw(): void {
  const p = preview.value
  preview.value = null
  if (!p) return
  if (p.kind === 'area') {
    const r = normalizeRect(p.x1, p.y1, p.x2, p.y2)
    if (r.w < 10 || r.h < 10) return
    markHistoryGroup()
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
    selectedIds.value = [area.id]
  } else if (p.kind === 'shape') {
    const r = normalizeRect(p.x1, p.y1, p.x2, p.y2)
    if (r.w < 4 || r.h < 4) return
    markHistoryGroup()
    const kind = shapeKindTool.value
    const graphic: LayoutGraphicShape = {
      id: uid('g'),
      kind,
      x: round1(r.x),
      y: round1(r.y),
      w: round1(r.w),
      h: round1(r.h),
      rx: kind === 'roundRect' ? Math.min(16, Math.max(0, Math.min(r.w, r.h) / 2)) : undefined,
      rotation: 0,
      fill: graphicDefaultFill(),
      stroke: graphicDefaultStroke(),
      strokeWidth: 1.5
    }
    doc.value.shapes.push(graphic)
    selectedIds.value = [graphic.id]
  } else if (p.kind === 'wall') {
    if (Math.hypot(p.x2 - p.x1, p.y2 - p.y1) < 8) return
    markHistoryGroup()
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
    selectedIds.value = [wall.id]
  } else if (p.kind === 'path') {
    const pts = (p.points || []).map((pt) => [round1(pt[0]), round1(pt[1])])
    if (pts.length < 2) return
    markHistoryGroup()
    const path: LayoutPathShape = {
      id: uid('p'),
      points: pts,
      color: '#999999',
      thickness: 5
    }
    doc.value.paths.push(path)
    selectedIds.value = [path.id]
  }
}

function placeSeat(pt: { x: number; y: number }): void {
  if (floorId.value == null) {
    toast('请先选择楼层', 'warning')
    return
  }
  const size = 44
  const pos = clampRectToScene(pt.x - size / 2, pt.y - size / 2, size, size, sceneW.value, sceneH.value)
  markHistoryGroup()
  const seat: LayoutSeatShape = {
    id: uid('s'),
    seatNo: nextSeatNo(floorNumber.value, doc.value.seats),
    x: pos.x,
    y: pos.y,
    w: size,
    h: size,
    rotation: 0,
    shape: seatShapeTool.value,
    seatType: seatToolType.value,
    status: 0
  }
  doc.value.seats.push(seat)
  selectedIds.value = [seat.id]
}

function placeText(pt: { x: number; y: number }): void {
  markHistoryGroup()
  const text: LayoutTextShape = {
    id: uid('t'),
    x: clamp(round1(pt.x), 0, sceneW.value),
    y: clamp(round1(pt.y), 0, sceneH.value),
    content: '双击区域或座位可设置名称/编号（见右侧面板）',
    fontSize: 22,
    color: '#e6e6ee'
  }
  doc.value.texts.push(text)
  selectedIds.value = [text.id]
}

// ============ 命中测试与选择 ============
function findNode(id: string): { kind: EditorKind; obj: EditableNode } | null {
  for (const s of doc.value.seats) if (s.id === id) return { kind: 'seat', obj: s }
  for (const t of doc.value.texts) if (t.id === id) return { kind: 'text', obj: t }
  for (const g of doc.value.shapes) if (g.id === id) return { kind: 'shape', obj: g }
  for (const p of doc.value.paths) if (p.id === id) return { kind: 'path', obj: p }
  for (const w of doc.value.walls) if (w.id === id) return { kind: 'wall', obj: w }
  for (const a of doc.value.areas) if (a.id === id) return { kind: 'area', obj: a }
  return null
}

/** 按绘制顺序返回全部可编辑节点 */
function allNodes(): Array<{ kind: EditorKind; obj: EditableNode }> {
  const arr: Array<{ kind: EditorKind; obj: EditableNode }> = []
  for (const a of doc.value.areas) arr.push({ kind: 'area', obj: a })
  for (const w of doc.value.walls) arr.push({ kind: 'wall', obj: w })
  for (const p of doc.value.paths) arr.push({ kind: 'path', obj: p })
  for (const t of doc.value.texts) arr.push({ kind: 'text', obj: t })
  for (const g of doc.value.shapes) arr.push({ kind: 'shape', obj: g })
  for (const s of doc.value.seats) arr.push({ kind: 'seat', obj: s })
  return arr
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
  for (let i = doc.value.shapes.length - 1; i >= 0; i--) {
    const g = doc.value.shapes[i]
    const b = boundsOf('shape', g)
    if (b && pointInRect(px, py, b.x, b.y, b.w, b.h)) {
      return { kind: 'shape', id: g.id, obj: g }
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

const selectedIds = ref<string[]>([])
const selectedSet = computed(() => new Set(selectedIds.value))
const selectedCount = computed(() => selectedIds.value.length)
const selectedNodes = computed(() => {
  const arr: SelectedNode[] = []
  for (const id of selectedIds.value) {
    const n = findNode(id)
    if (n) arr.push(n)
  }
  return arr
})
const selectedNode = computed(() => (selectedNodes.value.length === 1 ? selectedNodes.value[0] : null))
const panelNode = computed(() => {
  const n = selectedNode.value
  return n ? { kind: n.kind, obj: n.obj } : null
})
const selBoxes = computed(() => {
  const arr: Array<{ id: string; x: number; y: number; w: number; h: number }> = []
  for (const id of selectedIds.value) {
    const n = findNode(id)
    if (!n) continue
    const b = boundsOf(n.kind, n.obj)
    if (b) arr.push({ id, x: b.x, y: b.y, w: b.w, h: b.h })
  }
  return arr
})

const RESIZE_CURSOR: Record<ResizeDir, string> = {
  n: 'ns-resize',
  s: 'ns-resize',
  e: 'ew-resize',
  w: 'ew-resize',
  ne: 'nesw-resize',
  sw: 'nesw-resize',
  nw: 'nwse-resize',
  se: 'nwse-resize'
}

interface ResizeHandleHit {
  kind: 'area' | 'shape'
  id: string
  dir: ResizeDir
}

/** 单选区域/图形时显示 8 个缩放手柄（可再次调整大小） */
const resizeHandles = computed(() => {
  if (mode.value !== 'select') return []
  const n = selectedNode.value
  if (!n || (n.kind !== 'area' && n.kind !== 'shape')) return []
  const o = n.obj as { x: number; y: number; w?: number; h?: number; rotation?: number }
  if (o.rotation) return []
  const w = o.w || 44
  const h = o.h || 44
  return resizeHandlesFor(o.x, o.y, w, h).map((hd) => ({
    dir: hd.dir,
    x: hd.x,
    y: hd.y,
    cursor: RESIZE_CURSOR[hd.dir]
  }))
})

/** 命中缩放手柄（选择工具 + 单选区域/图形时） */
function hitResizeHandle(pt: { x: number; y: number }): ResizeHandleHit | null {
  const n = selectedNode.value
  if (!n || (n.kind !== 'area' && n.kind !== 'shape')) return null
  const o = n.obj as { id: string; x: number; y: number; w?: number; h?: number; rotation?: number }
  if (o.rotation) return null
  const w = o.w || 44
  const h = o.h || 44
  const hitR = Math.max(9, 10 / zoom.value)
  for (const hd of resizeHandlesFor(o.x, o.y, w, h)) {
    if (Math.hypot(pt.x - hd.x, pt.y - hd.y) <= hitR) {
      return { kind: n.kind as 'area' | 'shape', id: o.id, dir: hd.dir }
    }
  }
  return null
}

function startResize(hit: ResizeHandleHit, sx: number, sy: number): void {
  const n = findNode(hit.id)
  if (!n) return
  const o = n.obj as LayoutAreaShape | LayoutGraphicShape
  const isArea = hit.kind === 'area'
  drag.value = {
    mode: 'resize',
    kind: hit.kind,
    id: hit.id,
    dir: hit.dir,
    origin: { x: o.x, y: o.y, w: o.w || 10, h: o.h || 10 },
    minW: isArea ? 10 : 4,
    minH: isArea ? 10 : 4,
    startSceneX: sx,
    startSceneY: sy,
    historyJson: JSON.stringify(doc.value),
    moved: false
  }
}

function applyResizeDrag(d: Extract<DragState, { mode: 'resize' }>, dx: number, dy: number): void {
  const node = findNode(d.id)
  if (!node) return
  const o = node.obj as LayoutAreaShape | LayoutGraphicShape
  const r = resizeRectInScene(
    d.origin.x,
    d.origin.y,
    d.origin.w,
    d.origin.h,
    d.dir,
    dx,
    dy,
    d.minW,
    d.minH,
    sceneW.value,
    sceneH.value
  )
  o.x = r.x
  o.y = r.y
  o.w = r.w
  o.h = r.h
}

function clearSelection(): void {
  selectedIds.value = []
}
function selectAll(): void {
  selectedIds.value = allNodes().map((n) => (n.obj as { id: string }).id)
}
function toggleSelect(id: string): void {
  if (selectedSet.value.has(id)) {
    selectedIds.value = selectedIds.value.filter((x) => x !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

// ============ 复制 / 剪切 / 粘贴 ============
const clipboard = ref<SelectedNode[]>([])
const canPaste = computed(() => clipboard.value.length > 0)
let pasteSeq = 0

function copySelected(): void {
  const nodes = selectedNodes.value
  if (!nodes.length) return
  clipboard.value = nodes.map((n) => ({ kind: n.kind, obj: cloneJson(n.obj) }))
  pasteSeq = 0
  toast(`已复制 ${nodes.length} 个对象，Ctrl+V 粘贴`, 'success')
}

function cutSelected(): void {
  const nodes = selectedNodes.value
  if (!nodes.length) return
  clipboard.value = nodes.map((n) => ({ kind: n.kind, obj: cloneJson(n.obj) }))
  pasteSeq = 0
  deleteSelected()
  toast(`已剪切 ${nodes.length} 个对象，Ctrl+V 粘贴`, 'success')
}

/** 深拷贝并分配新 id，返回可加入文档的对象列表 */
function pasteClipboard(): void {
  if (!clipboard.value.length) return
  markHistoryGroup()
  pasteSeq += 1
  const step = 24 * pasteSeq
  const newIds: string[] = []
  for (const item of clipboard.value) {
    const clone = cloneJson(item.obj) as EditableNode
    const id = (clone as { id: string }).id
    const newId = uid(id.split('-')[0])
    ;(clone as { id: string }).id = newId
    if (item.kind === 'seat') {
      const s = clone as LayoutSeatShape
      s.seatId = undefined
      s.seatNo = nextSeatNo(floorNumber.value, doc.value.seats)
    }
    // 轻微错位，避免与原件完全重叠
    const c = clone as { x?: number; y?: number }
    if (typeof c.x === 'number') c.x = clamp(round1(c.x + step), 0, sceneW.value)
    if (typeof c.y === 'number') c.y = clamp(round1(c.y + step), 0, sceneH.value)
    pushClone(item.kind, clone)
    newIds.push(newId)
  }
  selectedIds.value = newIds
  toast(`已粘贴 ${newIds.length} 个对象`, 'success')
}

function pushClone(kind: EditorKind, clone: EditableNode): void {
  if (kind === 'area') doc.value.areas.push(clone as LayoutAreaShape)
  else if (kind === 'seat') doc.value.seats.push(clone as LayoutSeatShape)
  else if (kind === 'shape') doc.value.shapes.push(clone as LayoutGraphicShape)
  else if (kind === 'wall') doc.value.walls.push(clone as LayoutWallShape)
  else if (kind === 'path') doc.value.paths.push(clone as LayoutPathShape)
  else if (kind === 'text') doc.value.texts.push(clone as LayoutTextShape)
}

function deleteSelected(): void {
  if (!selectedIds.value.length) return
  markHistoryGroup()
  const remove = new Set(selectedIds.value)
  doc.value.areas = doc.value.areas.filter((x) => !remove.has(x.id))
  doc.value.walls = doc.value.walls.filter((x) => !remove.has(x.id))
  doc.value.paths = doc.value.paths.filter((x) => !remove.has(x.id))
  doc.value.texts = doc.value.texts.filter((x) => !remove.has(x.id))
  doc.value.seats = doc.value.seats.filter((x) => !remove.has(x.id))
  doc.value.shapes = doc.value.shapes.filter((x) => !remove.has(x.id))
  clearSelection()
}

function cloneJson<T>(v: T): T {
  return JSON.parse(JSON.stringify(v)) as T
}

// ============ 渲染辅助 ============
const seatShapeOf = (s: LayoutSeatShape): LayoutSeatShapeKind => s.shape || 'rect'
const seatLabel = (s: LayoutSeatShape) => seatLabelPos(s)
const seatLabelFont = (s: LayoutSeatShape) => seatLabelFontSize(s)
const areaLabel = (a: LayoutAreaShape) => areaNamePos(a)

const seatDiamondLocal = (s: LayoutSeatShape): string => {
  const w = s.w || 44
  const h = s.h || 44
  return `0,${-h / 2} ${w / 2},0 0,${h / 2} ${-w / 2},0`
}

const graphicTransform = (g: LayoutGraphicShape): string => {
  if (!g.rotation) return ''
  const cx = g.x + g.w / 2
  const cy = g.y + g.h / 2
  return `translate(${cx} ${cy}) rotate(${g.rotation}) translate(${-cx} ${-cy})`
}

// ============ 属性面板变更 ============
function onPropChange(key: string, value: string | number | null): void {
  const n = selectedNode.value
  if (!n) return
  // 座位编号重号先校验，避免产生无意义的撤销点
  if (n.kind === 'seat' && key === 'seatNo') {
    const s = n.obj as LayoutSeatShape
    const no = String(value ?? '').trim()
    if (isSeatNoDuplicate(no, s.id, doc.value.seats)) {
      toast('该座位编号已存在', 'warning')
      return
    }
  }
  markHistoryGroup()
  const str = (v: string | number | null): string => String(v ?? '')
  const num = (v: string | number | null, fb = 0): number => {
    const x = Number(v)
    return Number.isFinite(x) ? x : fb
  }
  if (n.kind === 'area') {
    const a = n.obj as LayoutAreaShape
    if (key === 'name') a.name = str(value)
    else if (key === 'nameSize') a.nameSize = Math.max(8, Math.min(200, num(value, 18)))
    else if (key === 'nameAlign') {
      const v = str(value)
      a.nameAlign = v === 'center' || v === 'right' ? (v as 'center' | 'right') : 'left'
    } else if (key === 'x') a.x = clamp(num(value), 0, sceneW.value)
    else if (key === 'y') a.y = clamp(num(value), 0, sceneH.value)
    else if (key === 'w') a.w = Math.max(10, num(value, 10))
    else if (key === 'h') a.h = Math.max(10, num(value, 10))
  } else if (n.kind === 'seat') {
    const s = n.obj as LayoutSeatShape
    if (key === 'seatNo') s.seatNo = str(value).trim()
    else if (key === 'shape') {
      const v = str(value)
      s.shape = v === 'round' || v === 'diamond' ? v : 'rect'
    } else if (key === 'seatType') s.seatType = num(value, 1)
    else if (key === 'areaId') s.areaId = value == null || value === '' ? null : num(value)
    else if (key === 'status') s.status = num(value, 0)
    else if (key === 'rotation') s.rotation = clamp(num(value), -180, 180)
    else if (key === 'size') {
      const v = Math.max(20, Math.min(120, num(value, 44)))
      s.w = v
      s.h = v
    } else if (key === 'labelSize')
      s.labelSize = value == null || value === '' ? undefined : Math.max(6, Math.min(80, num(value)))
    else if (key === 'labelAlign') {
      const v = str(value)
      s.labelAlign = v === 'center' || v === 'right' ? v : 'left'
    } else if (key === 'labelColor') s.labelColor = str(value) || undefined
    else if (key === 'x')
      s.x = clampRectToScene(num(value), s.y || 0, s.w || 44, s.h || 44, sceneW.value, sceneH.value).x
    else if (key === 'y')
      s.y = clampRectToScene(s.x || 0, num(value), s.w || 44, s.h || 44, sceneW.value, sceneH.value).y
  } else if (n.kind === 'shape') {
    const g = n.obj as LayoutGraphicShape
    if (key === 'fill') g.fill = str(value) || undefined
    else if (key === 'stroke') g.stroke = str(value) || undefined
    else if (key === 'strokeWidth') g.strokeWidth = Math.max(0.5, Math.min(30, num(value, 1.5)))
    else if (key === 'rotation') g.rotation = clamp(num(value), -180, 180)
    else if (key === 'x') g.x = clamp(num(value), 0, sceneW.value)
    else if (key === 'y') g.y = clamp(num(value), 0, sceneH.value)
    else if (key === 'w') g.w = Math.max(4, num(value, 4))
    else if (key === 'h') g.h = Math.max(4, num(value, 4))
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
    markHistoryGroup()
    doc.value.canvas = { ...(doc.value.canvas || {}), width: sceneW.value, height: sceneH.value, bgImage: resized }
    toast('✅ 底图已设置', 'success')
  } catch (err) {
    toast('底图处理失败：' + errMsg(err, '图片可能过大'), 'error')
  }
}

const removeBg = () => {
  markHistoryGroup()
  doc.value.canvas = { ...(doc.value.canvas || {}), bgImage: '' }
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
  const mod = e.ctrlKey || e.metaKey
  const key = e.key.toLowerCase()
  if (mod && key === 'z') {
    e.preventDefault()
    if (e.shiftKey) redo()
    else undo()
    return
  }
  if (mod && key === 'y') {
    e.preventDefault()
    redo()
    return
  }
  if (mod && key === 'c') {
    e.preventDefault()
    copySelected()
    return
  }
  if (mod && key === 'x') {
    e.preventDefault()
    cutSelected()
    return
  }
  if (mod && key === 'v') {
    e.preventDefault()
    pasteClipboard()
    return
  }
  if (mod && key === 'a') {
    e.preventDefault()
    selectAll()
    return
  }
  if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    deleteSelected()
    return
  }
  if (e.key === 'Escape') {
    clearSelection()
    if (mode.value !== 'select') mode.value = 'select'
  }
}

function onKeyUp(e: KeyboardEvent): void {
  if (e.code === 'Space') {
    isSpaceDown.value = false
    if (drag.value && drag.value.mode === 'pan') {
      drag.value = null
      panActive.value = false
    }
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
  if (historyTimer) window.clearTimeout(historyTimer)
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
.fe-bar-grow {
  flex: 1;
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

/* ---- 左侧工具条 ---- */
.fe-toolbar {
  width: 88px;
  flex: 0 0 auto;
  border-right: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  background: var(--bg-glass, rgba(13, 13, 18, 0.55));
  padding: 10px 6px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
  overflow-y: auto;
}
.fe-toolbar-title {
  font-size: 11px;
  color: var(--text-secondary, #9a9aa5);
  text-align: center;
  padding: 2px 0 6px;
  letter-spacing: 2px;
}
.fe-tool {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary, #9a9aa5);
  border-radius: 10px;
  font-size: 11px;
  padding: 7px 2px 6px;
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
  font-size: 18px;
  line-height: 1;
}
.fe-tool-label {
  font-size: 11px;
  line-height: 1.1;
}
.fe-toolbar-sub {
  margin-top: 6px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-color, rgba(255, 255, 255, 0.12));
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.fe-toolbar-sub label {
  font-size: 11px;
  color: var(--text-secondary, #9a9aa5);
  text-align: center;
}
.fe-toolbar-sub .fe-select {
  width: 100%;
  max-width: none;
  font-size: 12px;
  padding: 4px 6px;
}
.fe-toolbar-tip {
  margin-top: auto;
  padding-top: 10px;
  font-size: 10px;
  line-height: 1.5;
  color: var(--text-secondary, #9a9aa5);
  text-align: center;
}

/* ---- 画布 ---- */
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
}
.fe-bg {
  fill: #15151c;
}
.fe-svg text {
  user-select: none;
}
.fe-area-name {
  font-weight: 600;
  pointer-events: none;
}
.fe-text {
  user-select: none;
}
.fe-seat-no {
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
.fe-handles circle {
  fill: #0a0c14;
  stroke: #7ec8ff;
  stroke-width: 1.5;
}

.fe-marquee {
  fill: rgba(102, 170, 255, 0.12);
  stroke: #7ec8ff;
  stroke-width: 1.5;
  stroke-dasharray: 5 4;
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

/* ---- 右侧面板（通用） ---- */
.fe-panel {
  width: 256px;
  flex: 0 0 auto;
  border-left: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  background: var(--bg-glass, rgba(13, 13, 18, 0.55));
  display: flex;
  flex-direction: column;
  max-height: 100%;
  overflow: hidden;
}
.fe-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.08));
}
.fe-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #e8e8ec);
}
.fe-panel-body {
  padding: 10px 12px 12px;
  overflow: auto;
  flex: 1;
}

/* 多选面板 */
.fe-panel-multi .fe-panel-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.fe-multi-tip {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary, #9a9aa5);
  line-height: 1.7;
}
.fe-multi-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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
