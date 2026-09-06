import { describe, expect, it } from 'vitest'
import {
  clampRectToScene,
  distToSegment,
  isSeatNoDuplicate,
  nextSeatNo,
  normalizeRect,
  pickAreaForPoint,
  pointInRect,
  pointOnPolyline
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
