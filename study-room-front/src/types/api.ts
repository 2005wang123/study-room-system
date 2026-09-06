// src/types/api.ts —— 后端接口通用类型与领域模型
// 与 study-room-back 中 common/Result、entity、dto 对应

/** 后端统一响应结构（对应 Java common/Result） */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/** MyBatis-Plus IPage 序列化后的分页结构 */
export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ===== 用户 / 认证 =====

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
}

export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface AdminCreateUserParams {
  username: string
  idCard?: string
  /** 0-学生 1-管理员 */
  role?: number
  /** 0-禁用 1-启用 */
  status?: number
  /** 是否重建已被删除的同名用户 */
  rebuild?: boolean
}

/** 登录 / 刷新令牌返回（对应 LoginResponse） */
export interface LoginResult {
  id: number
  username: string
  token: string
  role: number
  isFirstLogin?: boolean
  needChangePassword?: boolean
}

/** 当前登录用户信息（对应 UserInfoResponse） */
export interface UserInfo {
  id: number
  username: string
  /** 0-学生 1-管理员 */
  role: number
  /** 0-禁用 1-启用 */
  status: number
  isFirstLogin?: boolean
  /** 信用积分 */
  points?: number | null
  /** 积分扣至 0 后的禁约截止时间 */
  bookBanUntil?: string | null
}

/** 管理员视角的用户（对应 AdminUserVO） */
export interface AdminUser extends UserInfo {
  createTime?: string
}

// ===== 自习室：楼层 / 区域 / 座位 / 预约 =====

export interface FloorInfo {
  id: number
  floorName: string
  /** 兼容历史下划线字段 */
  floor_name?: string
  floorNumber?: number
  /** 兼容历史下划线字段 */
  floor_number?: number
  bgImageUrl?: string
  status?: number
  remark?: string
}

export interface AreaInfo {
  id: number
  floorId: number
  areaName: string
  /** 兼容历史下划线字段 */
  area_name?: string
  sortOrder?: number
  status?: number
  remark?: string
}

/** 座位（对应 Seat 实体，含展示附加字段） */
export interface SeatInfo {
  id: number
  floorId?: number
  areaId?: number
  seatNo?: string
  xCoord?: number
  yCoord?: number
  seatType?: number
  /** 0-空闲 1-占用 2-预约 3-维修 等 */
  status: number
  userId?: number | null
  userName?: string | null
  areaName?: string
  floorName?: string
  /** 当日已预约比例 0~1 */
  bookedRatio?: number
  /** 当前登录用户当天在该座位的预约记录 ID */
  myReservationId?: number | null
  reservations?: SeatReservation[]
}

/** 座位某天的预约时段（对应 SeatReservationVO） */
export interface SeatReservation {
  id: number
  userId?: number
  userName?: string
  startTime: string
  endTime: string
  /** 0-待签到 1-使用中 2-已完成 3-违约 4-已取消 */
  status: number
}

export interface SeatQuery {
  floorId?: number | string
  areaId?: number | string
  date?: string
}

export interface SeatBookingParams {
  seatId: number
  startTime: string
  endTime: string
}

/** 预约记录 VO（对应 ReservationVO） */
export interface ReservationVO {
  id: number
  userId: number
  seatId: number
  seatNo: string
  floorId: number
  floorName: string
  areaId: number
  areaName: string
  startTime: string
  endTime: string
  /** 0-待签到 1-使用中 2-已完成 3-违约 4-已取消 */
  status: number
  createTime: string
}

// ===== 公告 / 评论 =====

/** 公告（对应 Announcement 实体） */
export interface AnnouncementItem {
  id: number
  title: string
  content: string
  publisher: string
  /** 0-草稿/下架 1-已发布 */
  status: number
  publishTime?: string | null
  createTime?: string
  updateTime?: string | null
}

export interface AnnouncementPayload {
  title: string
  content: string
  status: number
}

/** 公告评论 VO（对应 AnnouncementCommentVO） */
export interface AnnouncementComment {
  id: number
  announcementId: number
  userId: number
  username: string
  content: string
  likeCount: number
  createTime: string
  /** 当前登录用户是否已点赞 */
  liked?: boolean
}

// ===== 楼层结构图 =====

/** 楼层结构图（对应 FloorLayout 实体，layoutJson 为画布 JSON 字符串） */
export interface FloorLayoutData {
  id?: number
  floorId?: number
  layoutJson: string
  version?: number
  status?: number
  createBy?: number
  updateBy?: number
  createTime?: string
  updateTime?: string
}

/** 统一错误对象形态（用于 catch 分支读取后端 message） */
export interface ApiErrorShape {
  response?: {
    data?: {
      code?: number
      message?: string
    }
  }
  message?: string
}
