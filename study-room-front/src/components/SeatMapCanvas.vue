<template>
  <div ref="wrapEl" class="seat-map-canvas">
    <svg :viewBox="`0 0 ${cw} ${ch}`" class="map-svg" xmlns="http://www.w3.org/2000/svg">
      <!-- 底图 -->
      <image
        v-if="layout?.canvas?.bgImage"
        :href="layout.canvas.bgImage"
        x="0"
        y="0"
        :width="cw"
        :height="ch"
        preserveAspectRatio="xMidYMid meet"
      />

      <!-- 区域 -->
      <g v-for="a in areas" :key="a.id" class="map-area">
        <rect
          :x="a.x"
          :y="a.y"
          :width="a.w"
          :height="a.h"
          :fill="a.fill || 'rgba(102,126,234,0.22)'"
          :stroke="a.stroke || '#667eea'"
          stroke-width="2"
          rx="8"
        />
        <text
          v-if="a.name"
          :x="areaLabel(a).x"
          :y="areaLabel(a).y"
          :text-anchor="areaLabel(a).anchor"
          :font-size="areaLabel(a).fontSize"
          :fill="a.nameColor || 'rgba(255,255,255,0.6)'"
          class="area-name"
        >
          {{ a.name }}
        </text>
      </g>

      <!-- 墙体 -->
      <line
        v-for="w in walls"
        :key="w.id"
        :x1="w.x1"
        :y1="w.y1"
        :x2="w.x2"
        :y2="w.y2"
        :stroke="w.color || '#8a8a96'"
        :stroke-width="w.thickness || 8"
        stroke-linecap="round"
        class="map-wall"
      />

      <!-- 自由画笔路径 -->
      <polyline
        v-for="p in paths"
        :key="p.id"
        :points="pointsToStr(p.points)"
        :stroke="p.color || '#999999'"
        :stroke-width="p.thickness || 5"
        fill="none"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="map-path"
      />

      <!-- 文字标注 -->
      <text
        v-for="t in texts"
        :key="t.id"
        :x="t.x"
        :y="t.y"
        :font-size="t.fontSize || 24"
        :fill="t.color || '#e8e8ec'"
        class="map-text"
      >
        {{ t.content }}
      </text>

      <!-- 通用图形（桌椅/装饰） -->
      <g v-for="g in graphics" :key="g.id" class="map-graphic" :transform="graphicTransform(g)">
        <ShapeGlyph :shape="g" />
      </g>

      <!-- 座位 -->
      <g
        v-for="s in seats"
        :key="s.id"
        class="map-seat"
        :transform="`translate(${s.x + (s.w || 44) / 2} ${s.y + (s.h || 44) / 2}) rotate(${s.rotation || 0})`"
        @click="onSeatClick(s)"
        @mouseenter="onSeatEnter(s, $event)"
        @mouseleave="tip.visible = false"
      >
        <!-- 方形（圆角） -->
        <rect
          v-if="seatShapeOf(s) === 'rect'"
          :x="-(s.w || 44) / 2"
          :y="-(s.h || 44) / 2"
          :width="s.w || 44"
          :height="s.h || 44"
          :fill="seatFill(s)"
          stroke="#d8d8e0"
          stroke-width="1.5"
          rx="6"
          class="seat-rect"
        />
        <!-- 圆形 / 椭圆 -->
        <ellipse
          v-else-if="seatShapeOf(s) === 'round'"
          cx="0"
          cy="0"
          :rx="Math.max(0.5, (s.w || 44) / 2)"
          :ry="Math.max(0.5, (s.h || 44) / 2)"
          :fill="seatFill(s)"
          stroke="#d8d8e0"
          stroke-width="1.5"
          class="seat-rect"
        />
        <!-- 菱形 -->
        <polygon
          v-else
          :points="diamondLocal(s)"
          :fill="seatFill(s)"
          stroke="#d8d8e0"
          stroke-width="1.5"
          stroke-linejoin="round"
          class="seat-rect"
        />
        <text
          :x="seatLabel(s).x"
          :y="0"
          :text-anchor="seatLabel(s).anchor"
          dominant-baseline="central"
          class="seat-no"
          :font-size="seatLabelFont(s)"
          :fill="s.labelColor || '#ffffff'"
        >
          {{ s.seatNo }}
        </text>
        <circle v-if="seatData(s)?.myReservationId" cx="0" cy="0" r="3" fill="#ffffff" opacity="0.9" />
      </g>
    </svg>

    <div v-if="!layout" class="map-empty">该楼层暂无结构图，请管理员在「楼层绘图」中绘制并发布</div>

    <!-- 座位悬浮提示 -->
    <div
      v-if="tip.visible"
      class="map-tip"
      :class="{ below: tip.below }"
      :style="{ left: tip.left + 'px', top: tip.top + 'px' }"
    >
      <div class="map-tip-title">{{ tip.text }}</div>
      <div class="map-tip-sub">{{ tip.hint }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import ShapeGlyph from '@/components/floor-editor/ShapeGlyph.vue'
import type { LayoutGraphicShape, LayoutSeatShape, LayoutShape } from '@/types/layout'
import { areaNamePos, seatLabelFontSize, seatLabelPos } from '@/utils/floorEditor'

/** getSeats 返回的座位实时状态（字段较宽松，兼容历史数据） */
interface SeatStatusLike {
  id?: number | string
  seatId?: number | string
  seat_id?: number | string
  seat_no?: number | string
  seatNo?: string
  status?: number | string
  bookedRatio?: number | null
  userName?: string
  myReservationId?: number | string | null
}

const props = withDefaults(
  defineProps<{
    layout: LayoutShape | null
    // 座位实时状态（来自现有 getSeats 接口）
    seats: SeatStatusLike[]
  }>(),
  { layout: null, seats: () => [] }
)
const emit = defineEmits<{ (e: 'seat-click', data: SeatStatusLike): void }>()

const cw = computed(() => Number(props.layout?.canvas?.width) || 1600)
const ch = computed(() => Number(props.layout?.canvas?.height) || 1100)
const areas = computed(() => props.layout?.areas || [])
const walls = computed(() => props.layout?.walls || [])
const paths = computed(() => props.layout?.paths || [])
const texts = computed(() => props.layout?.texts || [])
const graphics = computed(() => props.layout?.shapes || [])
const seats = computed(() => props.layout?.seats || [])

const seatShapeOf = (s: LayoutSeatShape): string => s.shape || 'rect'
const seatLabel = (s: LayoutSeatShape) => seatLabelPos(s)
const seatLabelFont = (s: LayoutSeatShape) => seatLabelFontSize(s)
const areaLabel = (a: {
  x: number
  y: number
  w: number
  h: number
  nameSize?: number
  nameAlign?: 'left' | 'center' | 'right'
}) => areaNamePos(a)

const diamondLocal = (s: LayoutSeatShape): string => {
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

const seatDataMap = computed(() => {
  const map = new Map<string, SeatStatusLike>()
  props.seats.forEach((s) => {
    const id = s.id ?? s.seatId ?? s.seat_id
    if (id != null) map.set(String(id), s)
  })
  return map
})

const seatData = (s: LayoutSeatShape): SeatStatusLike | undefined => seatDataMap.value.get(String(s.seatId))

const seatRatio = (s: SeatStatusLike | undefined): number => {
  const r = Number(s?.bookedRatio)
  return Number.isFinite(r) ? r : 0
}

const seatFill = (s: LayoutSeatShape): string => {
  const data = seatData(s)
  if (data && Number(data.status) === 3) return '#7a7a85'
  const r = seatRatio(data)
  if (r <= 0) return '#4caf50'
  if (r >= 1) return '#f44336'
  return '#f5a623'
}

const seatTitle = (s: LayoutSeatShape): string => {
  const data = seatData(s)
  if (!data) return `${s.seatNo}（未同步数据）`
  if (Number(data.status) === 3) return `${s.seatNo} · 维修中`
  const r = seatRatio(data)
  const state = r <= 0 ? '空闲' : r >= 1 ? '已约满' : '部分可约'
  const user = data.userName ? ` · ${data.userName}` : ''
  const mine = data.myReservationId ? ' · 我的预约' : ''
  return `${s.seatNo} · ${state}${user}${mine}`
}
const wrapEl = ref<HTMLDivElement | null>(null)
const tip = ref<{
  visible: boolean
  text: string
  hint: string
  left: number
  top: number
  below: boolean
}>({ visible: false, text: '', hint: '', left: 0, top: 0, below: false })

const seatHint = (s: LayoutSeatShape): string => {
  const data = seatData(s)
  if (!data) return '该座位尚未与座位表同步，请联系管理员发布'
  if (Number(data.status) === 3) return '维修中，暂不可预约'
  if (data.myReservationId) return '你已预约该座位，点击可查看/操作'
  const r = seatRatio(data)
  if (r >= 1) return '今日已约满'
  return '点击可预约该座位'
}

function onSeatEnter(s: LayoutSeatShape, e: MouseEvent): void {
  const wrap = wrapEl.value
  const g = e.currentTarget as SVGGraphicsElement | null
  if (!wrap || !g) return
  const wr = wrap.getBoundingClientRect()
  const sr = g.getBoundingClientRect()
  const centerX = sr.left - wr.left + sr.width / 2
  const below = sr.top - wr.top < 74
  tip.value = {
    visible: true,
    text: seatTitle(s),
    hint: seatHint(s),
    left: Math.min(Math.max(centerX, 84), Math.max(84, wrap.clientWidth - 84)),
    top: below ? sr.bottom - wr.top + 8 : sr.top - wr.top - 8,
    below
  }
}

const onSeatClick = (s: LayoutSeatShape) => {
  const data = seatData(s) ?? { id: s.seatId, seat_no: s.seatNo, seatNo: s.seatNo, status: s.status ?? 0 }
  emit('seat-click', data)
}

const pointsToStr = (points?: number[][]): string => {
  return (points || []).map((p) => p.join(',')).join(' ')
}
</script>

<style scoped>
.seat-map-canvas {
  position: relative;
  width: 100%;
  overflow-x: auto;
  background: var(--bg-primary, #0d0d12);
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.12));
  border-radius: 12px;
  padding: 12px;
  box-sizing: border-box;
}
.map-svg {
  width: 100%;
  height: auto;
  display: block;
  border-radius: 8px;
  background: #12121a;
  min-width: 600px;
}
.map-area .area-name {
  font-weight: 600;
  pointer-events: none;
  user-select: none;
}
.map-seat {
  cursor: pointer;
}
.map-seat .seat-rect {
  transition:
    filter 0.15s,
    stroke 0.15s;
  stroke: var(--border-color, rgba(255, 255, 255, 0.25));
}
.map-seat:hover .seat-rect {
  filter: brightness(1.25);
  stroke: #ffffff;
  stroke-width: 2.5;
}
.seat-no {
  font-weight: 600;
  pointer-events: none;
  user-select: none;
}
.map-empty {
  color: var(--text-secondary, #9a9aa5);
  font-size: 14px;
  text-align: center;
  padding: 48px 0;
}
.map-tip {
  position: absolute;
  z-index: 30;
  max-width: 260px;
  padding: 7px 10px;
  border-radius: 8px;
  background: rgba(24, 24, 32, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.45);
  transform: translate(-50%, -100%);
  pointer-events: none;
  text-align: left;
}
.map-tip.below {
  transform: translate(-50%, 0);
}
.map-tip-title {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  line-height: 1.4;
}
.map-tip-sub {
  margin-top: 3px;
  font-size: 11px;
  color: #a9a9b8;
  line-height: 1.4;
}
</style>
