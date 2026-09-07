import { describe, expect, it } from 'vitest'
import {
  alignAnchor,
  areaNamePos,
  clampRectToScene,
  distToSegment,
  isSeatNoDuplicate,
  nextSeatNo,
  normalizeRect,
  pickAreaForPoint,
  pointInRect,
  pointOnPolyline,
  rectInside,
  rectsOverlap,
  resizeHandlesFor,
  resizeRectInScene,
  rotatedBounds,
  seatLabelFontSize,
  seatLabelPos
} from '@/utils/floorEditor'
import type { LayoutSeatShape } from '@/types/layout'

describe('normalizeRect', () => {
  it('支持反向拖拽并规范化', () => {
    expect(normalizeRect(100, 50, 40, 90)).toEqual({ x: 40, y: 50, w: 60, h: 40 })
  })
})

describe('clampRectToScene', () => {
  it('把超出边界的矩形拉回场景内', () => {
    expect(clampRectToScene(-10, 200, 40, 40, 1000, 800)).toEqual({ x: 0, y: 200 })
    expect(clampRectToScene(990, 790, 40, 40, 1000, 800)).toEqual({ x: 960, y: 760 })
  })
})

describe('nextSeatNo', () => {
  it('从已有编号向后递增并补零', () => {
    const seats = [{ seatNo: '1-03' }, { seatNo: '2-01' }, { seatNo: '1-07' }]
    expect(nextSeatNo(1, seats)).toBe('1-08')
    expect(nextSeatNo(2, seats)).toBe('2-02')
  })
  it('空画布从 01 开始', () => {
    expect(nextSeatNo(3, [])).toBe('3-01')
  })
  it('不匹配前缀的编号不干扰计数', () => {
    const seats = [{ seatNo: 'A-05' }, { seatNo: '座位1' }]
    expect(nextSeatNo(1, seats)).toBe('1-01')
  })
})

describe('pickAreaForPoint', () => {
  const areas = [
    { id: 'a-big', x: 0, y: 0, w: 1000, h: 800 },
    { id: 'a-small', x: 100, y: 100, w: 200, h: 150 }
  ]
  it('命中多个区域时选择面积最小者', () => {
    expect(pickAreaForPoint(areas, 150, 120)).toBe('a-small')
    expect(pickAreaForPoint(areas, 500, 400)).toBe('a-big')
  })
  it('未命中返回 null', () => {
    expect(pickAreaForPoint(areas, 5000, 5000)).toBeNull()
  })
})

describe('点与图形命中', () => {
  it('pointInRect 支持旋转', () => {
    expect(pointInRect(10, 10, 0, 0, 20, 20)).toBe(true)
    expect(pointInRect(30, 10, 0, 0, 20, 20)).toBe(false)
    // 绕中心旋转 45 度后，原矩形角点应落到外部
    expect(pointInRect(0, 0, 0, 0, 20, 20, 45)).toBe(false)
    // 中心仍在内
    expect(pointInRect(10, 10, 0, 0, 20, 20, 45)).toBe(true)
  })
  it('distToSegment / pointOnPolyline', () => {
    expect(distToSegment(5, 3, 0, 0, 10, 0)).toBe(3)
    expect(
      pointOnPolyline(
        5,
        3,
        [
          [0, 0],
          [10, 0]
        ],
        4
      )
    ).toBe(true)
    expect(
      pointOnPolyline(
        5,
        10,
        [
          [0, 0],
          [10, 0]
        ],
        4
      )
    ).toBe(false)
  })
})

describe('矩形关系与旋转外接框', () => {
  it('rectsOverlap 判断相交', () => {
    expect(rectsOverlap(0, 0, 10, 10, 5, 5, 10, 10)).toBe(true)
    expect(rectsOverlap(0, 0, 10, 10, 20, 20, 5, 5)).toBe(false)
  })
  it('rectInside 判断完全包含', () => {
    expect(rectInside(2, 2, 4, 4, 0, 0, 10, 10)).toBe(true)
    expect(rectInside(-1, 2, 4, 4, 0, 0, 10, 10)).toBe(false)
  })
  it('rotatedBounds 返回外接矩形且覆盖旋转后的角点', () => {
    const b = rotatedBounds(0, 0, 20, 20, 45)
    expect(b.w).toBeCloseTo(28.28, 1)
    expect(b.h).toBeCloseTo(28.28, 1)
  })
})

describe('区域/座位名称排版', () => {
  it('areaNamePos 支持左/中/右对齐', () => {
    const area = { x: 100, y: 200, w: 400, h: 200 }
    expect(areaNamePos({ ...area, nameAlign: 'left' }).anchor).toBe('start')
    expect(areaNamePos({ ...area, nameAlign: 'left' }).x).toBe(110)
    expect(areaNamePos({ ...area, nameAlign: 'center' }).anchor).toBe('middle')
    expect(areaNamePos({ ...area, nameAlign: 'center' }).x).toBe(300)
    expect(areaNamePos({ ...area, nameAlign: 'right' }).anchor).toBe('end')
    expect(areaNamePos({ ...area, nameAlign: 'right' }).x).toBe(490)
  })
  it('areaNamePos 使用自定义字号（缺省 18）', () => {
    expect(areaNamePos({ x: 0, y: 0, w: 100, h: 60 }).fontSize).toBe(18)
    expect(areaNamePos({ x: 0, y: 0, w: 100, h: 60, nameSize: 30 }).fontSize).toBe(30)
  })
  it('seatLabelFontSize 优先自定义值，缺省按大小自适应', () => {
    expect(seatLabelFontSize({ w: 44, h: 44, labelSize: 20 })).toBe(20)
    const auto = seatLabelFontSize({ w: 44, h: 44 })
    expect(auto).toBeGreaterThanOrEqual(9)
    expect(auto).toBeLessThanOrEqual(16)
  })
  it('seatLabelPos 支持座位内左/中/右', () => {
    expect(seatLabelPos({ w: 44, h: 44 }).anchor).toBe('middle')
    expect(seatLabelPos({ w: 44, h: 44 }).x).toBe(0)
    expect(seatLabelPos({ w: 44, h: 44, labelAlign: 'left' }).anchor).toBe('start')
    expect(seatLabelPos({ w: 44, h: 44, labelAlign: 'left' }).x).toBeLessThan(0)
    expect(seatLabelPos({ w: 44, h: 44, labelAlign: 'right' }).anchor).toBe('end')
    expect(seatLabelPos({ w: 44, h: 44, labelAlign: 'right' }).x).toBeGreaterThan(0)
  })
  it('alignAnchor 映射 SVG 锚点', () => {
    expect(alignAnchor()).toBe('start')
    expect(alignAnchor('center')).toBe('middle')
    expect(alignAnchor('right')).toBe('end')
  })
})

describe('缩放纯逻辑', () => {
  it('resizeHandlesFor 返回 8 个手柄位置', () => {
    const hs = resizeHandlesFor(100, 200, 200, 100)
    expect(hs).toHaveLength(8)
    const byDir = Object.fromEntries(hs.map((h) => [h.dir, h]))
    expect(byDir.nw).toEqual({ dir: 'nw', x: 100, y: 200 })
    expect(byDir.se).toEqual({ dir: 'se', x: 300, y: 300 })
    expect(byDir.e).toEqual({ dir: 'e', x: 300, y: 250 })
    expect(byDir.w).toEqual({ dir: 'w', x: 100, y: 250 })
  })
  it('resizeRectInScene 支持向右/下增大', () => {
    const r = resizeRectInScene(100, 100, 200, 100, 'se', 40, 30, 10, 10, 1600, 1100)
    expect(r).toEqual({ x: 100, y: 100, w: 240, h: 130 })
  })
  it('resizeRectInScene 向左/上拖拽能扩展并保持边界', () => {
    // 左上手柄向右下拖 → 缩小，最小 10
    const r1 = resizeRectInScene(100, 100, 200, 100, 'nw', 160, 80, 10, 10, 1600, 1100)
    expect(r1.w).toBe(40)
    expect(r1.h).toBe(20)
    // 左上手柄向左上拖 → 扩展到场景边界 (0,0)
    const r2 = resizeRectInScene(40, 30, 200, 100, 'nw', -100, -80, 10, 10, 1600, 1100)
    expect(r2.x).toBe(0)
    expect(r2.y).toBe(0)
    expect(r2.w).toBe(240)
    expect(r2.h).toBe(130)
  })
  it('resizeRectInScene 到达右/下场景边界后停止增长', () => {
    const r = resizeRectInScene(1500, 1000, 100, 100, 'se', 500, 500, 10, 10, 1600, 1100)
    expect(r.x + r.w).toBe(1600)
    expect(r.y + r.h).toBe(1100)
  })
  it('resizeRectInScene 保持最小尺寸', () => {
    const r = resizeRectInScene(0, 0, 100, 100, 'nw', 98, 98, 10, 10, 1600, 1100)
    expect(r.w).toBe(10)
    expect(r.h).toBe(10)
  })
})

describe('isSeatNoDuplicate', () => {
  const seats: LayoutSeatShape[] = [
    { id: 's-1', seatNo: '1-01', x: 0, y: 0 },
    { id: 's-2', seatNo: '1-02', x: 50, y: 0 }
  ]
  it('检测画布内重号（忽略自身）', () => {
    expect(isSeatNoDuplicate('1-01', undefined, seats)).toBe(true)
    expect(isSeatNoDuplicate('1-01', 's-1', seats)).toBe(false)
    expect(isSeatNoDuplicate('1-09', undefined, seats)).toBe(false)
  })
})
