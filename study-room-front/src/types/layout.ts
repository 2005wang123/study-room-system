// src/types/layout.ts —— 楼层结构图画布 JSON 的结构类型
// 画布 JSON 由 FloorLayoutEditor 生成，SeatMapCanvas 负责渲染

export interface LayoutAreaShape {
  id: string
  name?: string
  x: number
  y: number
  w: number
  h: number
  /** 区域填充色 */
  fill?: string
  /** 区域描边色 */
  stroke?: string
}

export interface LayoutWallShape {
  id: string
  x1: number
  y1: number
  x2: number
  y2: number
  color?: string
  thickness?: number
}

export interface LayoutPathShape {
  id: string
  points?: number[][]
  color?: string
  thickness?: number
}

export interface LayoutTextShape {
  id: string
  x: number
  y: number
  content: string
  fontSize?: number
  color?: string
}

export interface LayoutSeatShape {
  /** 画布内唯一 id */
  id: string
  /** 座位编号，如 A-01 */
  seatNo: string
  /** 关联数据库座位 id（由发布流程写入） */
  seatId?: number | string
  x: number
  y: number
  w?: number
  h?: number
  rotation?: number
  /** 所属区域（数据库区域 id，Area 表） */
  areaId?: number | null
  /** 1-普通 2-靠窗 3-带插座 */
  seatType?: number
  /** 0-正常 3-维修 */
  status?: number
}

export interface LayoutCanvasShape {
  width?: number
  height?: number
  /** 底图（dataURL 或 URL） */
  bgImage?: string
}

export interface LayoutShape {
  canvas?: LayoutCanvasShape
  areas?: LayoutAreaShape[]
  walls?: LayoutWallShape[]
  paths?: LayoutPathShape[]
  texts?: LayoutTextShape[]
  seats?: LayoutSeatShape[]
}
