// src/utils/floorEditor.ts —— 楼层结构图编辑器的纯逻辑（可单测）
import type { LayoutSeatShape } from '@/types/layout'

/** 默认场景逻辑尺寸 */
export const SCENE_W_DEFAULT = 1600
export const SCENE_H_DEFAULT = 1100
export const MIN_ZOOM = 0.2
export const MAX_ZOOM = 3
/** 默认座位边长 */
export const DEFAULT_SEAT_SIZE = 44

/** 生成画布内唯一 id */
export function uid(prefix: string): string {
  return prefix + '-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6)
}

export function clamp(v: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, v))
}

export function round1(v: number): number {
  return Math.round(v * 10) / 10
}

/** 由两点生成规范的矩形（处理反向拖拽） */
export function normalizeRect(
  x1: number,
  y1: number,
  x2: number,
  y2: number
): { x: number; y: number; w: number; h: number } {
  const x = Math.min(x1, x2)
  const y = Math.min(y1, y2)
  return { x, y, w: Math.abs(x2 - x1), h: Math.abs(y2 - y1) }
}

/** 将对象限制在场景内（rect 左上角 + 宽高） */
export function clampRectToScene(
  x: number,
  y: number,
  w: number,
  h: number,
  sceneW: number,
  sceneH: number
): { x: number; y: number } {
  return {
    x: clamp(round1(x), 0, Math.max(0, sceneW - w)),
    y: clamp(round1(y), 0, Math.max(0, sceneH - h))
  }
}

/** 点是否在（可带旋转角度的）矩形内；rect 的 x/y 为左上角 */
export function pointInRect(
  px: number,
  py: number,
  x: number,
  y: number,
  w: number,
  h: number,
  rotationDeg = 0
): boolean {
  if (!rotationDeg) {
    return px >= x && px <= x + w && py >= y && py <= y + h
  }
  const cx = x + w / 2
  const cy = y + h / 2
  const rad = (rotationDeg * Math.PI) / 180
  const cos = Math.cos(rad)
  const sin = Math.sin(rad)
  // 逆旋转，回到未旋转坐标系判断
  const dx = px - cx
  const dy = py - cy
  const lx = dx * cos + dy * sin
  const ly = -dx * sin + dy * cos
  return Math.abs(lx) <= w / 2 && Math.abs(ly) <= h / 2
}

/** 点到线段的最短距离（用于命中墙体/画笔） */
export function distToSegment(px: number, py: number, x1: number, y1: number, x2: number, y2: number): number {
  const dx = x2 - x1
  const dy = y2 - y1
  const lenSq = dx * dx + dy * dy
  if (lenSq === 0) return Math.hypot(px - x1, py - y1)
  let t = ((px - x1) * dx + (py - y1) * dy) / lenSq
  t = clamp(t, 0, 1)
  return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy))
}

/** 点是否在某条折线上（容差为 tolerance） */
export function pointOnPolyline(px: number, py: number, points: number[][], tolerance: number): boolean {
  if (!points || points.length === 0) return false
  for (let i = 0; i < points.length - 1; i++) {
    if (distToSegment(px, py, points[i][0], points[i][1], points[i + 1][0], points[i + 1][1]) <= tolerance) {
      return true
    }
  }
  return points.length === 1 && Math.hypot(px - points[0][0], py - points[0][1]) <= tolerance
}

export interface RectLike {
  x: number
  y: number
  w: number
  h: number
}

/**
 * 生成新座位编号：{楼层号}-{两位序号}，例如 1-01。
 * 依据画布内已有该楼层前缀的编号向后递增。
 */
export function nextSeatNo(floorNumber: number, seats: Array<{ seatNo?: string | null }>): string {
  const prefix = String(floorNumber)
  const re = new RegExp('^' + escapeRegExp(prefix) + '-(\\d+)$')
  let maxSeq = 0
  for (const s of seats) {
    const m = re.exec(String(s.seatNo ?? ''))
    if (m) {
      const seq = Number(m[1])
      if (Number.isFinite(seq) && seq > maxSeq) maxSeq = seq
    }
  }
  return prefix + '-' + String(maxSeq + 1).padStart(2, '0')
}

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 为落在多个区域内的点选择"最小面积"区域（更精确归属）；
 * 没有任何区域包含时返回 null。
 */
export function pickAreaForPoint(areas: Array<RectLike & { id: string }>, px: number, py: number): string | null {
  let best: string | null = null
  let bestArea = Infinity
  for (const a of areas) {
    if (a.w <= 0 || a.h <= 0) continue
    if (px >= a.x && px <= a.x + a.w && py >= a.y && py <= a.y + a.h) {
      const s = a.w * a.h
      if (s < bestArea) {
        bestArea = s
        best = a.id
      }
    }
  }
  return best
}

/** JSON 深拷贝（画布对象均为可序列化结构） */
export function cloneJson<T>(v: T): T {
  return JSON.parse(JSON.stringify(v)) as T
}

/** 计算选中框（近似文本宽度），用于覆盖层高亮 */
export function boundsOf(kind: string, obj: unknown): { x: number; y: number; w: number; h: number } | null {
  if (!obj) return null
  switch (kind) {
    case 'area':
    case 'seat': {
      const o = obj as { x: number; y: number; w?: number; h?: number; rotation?: number }
      const w = o.w || DEFAULT_SEAT_SIZE
      const h = o.h || DEFAULT_SEAT_SIZE
      // 旋转时用外接矩形近似
      const rad = ((o.rotation || 0) * Math.PI) / 180
      const cw = Math.abs(w * Math.cos(rad)) + Math.abs(h * Math.sin(rad))
      const ch = Math.abs(w * Math.sin(rad)) + Math.abs(h * Math.cos(rad))
      const cx = o.x + w / 2
      const cy = o.y + h / 2
      return { x: cx - cw / 2, y: cy - ch / 2, w: cw, h: ch }
    }
    case 'wall': {
      const o = obj as { x1: number; y1: number; x2: number; y2: number }
      return { x: Math.min(o.x1, o.x2), y: Math.min(o.y1, o.y2), w: Math.abs(o.x2 - o.x1), h: Math.abs(o.y2 - o.y1) }
    }
    case 'path': {
      const o = obj as { points?: number[][] }
      const pts = o.points || []
      if (!pts.length) return null
      let minX = Infinity
      let minY = Infinity
      let maxX = -Infinity
      let maxY = -Infinity
      for (const p of pts) {
        minX = Math.min(minX, p[0])
        minY = Math.min(minY, p[1])
        maxX = Math.max(maxX, p[0])
        maxY = Math.max(maxY, p[1])
      }
      return { x: minX, y: minY, w: maxX - minX, h: maxY - minY }
    }
    case 'text': {
      const o = obj as { x: number; y: number; content?: string; fontSize?: number }
      const fs = o.fontSize || 24
      const len = String(o.content || '').length
      return { x: o.x, y: o.y - fs, w: Math.max(20, len * fs * 0.62), h: fs * 1.3 }
    }
    default:
      return null
  }
}

/** 座位颜色：按类型区分；维修中灰色 */
export function seatFill(type: number | undefined, status: number | string | undefined): string {
  if (Number(status) === 3) return '#7a7a85'
  switch (Number(type) || 1) {
    case 2:
      return '#0ea5a4' // 靠窗
    case 3:
      return '#d97706' // 带插座
    default:
      return '#4f7cff' // 普通
  }
}

export function areaDefaultFill(): string {
  return 'rgba(102, 126, 234, 0.22)'
}
export function areaDefaultStroke(): string {
  return '#667eea'
}

export function isSeatNoDuplicate(seatNo: string, seatId: string | undefined, seats: LayoutSeatShape[]): boolean {
  const no = String(seatNo || '').trim()
  if (!no) return false
  return seats.some(
    (s) =>
      s.id !== seatId &&
      String(s.seatNo || '')
        .trim()
        .toLowerCase() === no.toLowerCase()
  )
}

