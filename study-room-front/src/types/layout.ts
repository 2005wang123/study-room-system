// src/types/layout.ts —— 楼层结构图画布 JSON 的结构类型
// 画布 JSON 由 FloorLayoutEditor 生成，SeatMapCanvas 负责渲染

/** 水平对齐方式：left-左 center-居中 right-右 */
export type LayoutAlign = 'left' | 'center' | 'right'

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
  /** 区域名称字号（默认 18） */
  nameSize?: number
  /** 区域名称水平对齐（区域内，默认 left） */
  nameAlign?: LayoutAlign
  /** 区域名称颜色 */
  nameColor?: string
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

/** 座位外观形状：rect-圆角方形 round-圆形 diamond-菱形 */
export type LayoutSeatShapeKind = 'rect' | 'round' | 'diamond'

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
  /** 座位外观形状（默认 rect） */
  shape?: LayoutSeatShapeKind
  /** 座位编号字号（缺省时按座位大小自动计算） */
  labelSize?: number
  /** 座位编号水平对齐（座位内，默认 center） */
  labelAlign?: LayoutAlign
  /** 座位编号颜色 */
  labelColor?: string
}

/** 装饰/家具等通用形状的种类 */
export type LayoutGraphicKind = 'rect' | 'roundRect' | 'ellipse' | 'circle' | 'triangle' | 'diamond'

/** 通用图形（矩形/圆角矩形/椭圆/圆/三角/菱形），可用于桌椅、柜子等装饰 */
export interface LayoutGraphicShape {
  id: string
  kind: LayoutGraphicKind
  /** 外接矩形左上角（旋转时以中心旋转） */
  x: number
  y: number
  w: number
  h: number
  rotation?: number
  /** 圆角矩形圆角半径 */
  rx?: number
  fill?: string
  stroke?: string
  strokeWidth?: number
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
  /** 通用图形：桌椅/装饰等 */
  shapes?: LayoutGraphicShape[]
}
