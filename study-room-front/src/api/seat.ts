// src/api/seat.ts
import request from '@/utils/request'
import type {
  AreaInfo,
  FloorInfo,
  ReservationVO,
  SeatBookingParams,
  SeatInfo,
  SeatQuery,
  SeatReservation
} from '@/types/api'

// 获取楼层列表
export const getFloors = () => {
  return request.get<FloorInfo[]>('/study-room/floors')
}

// 获取指定楼层的区域列表
export const getAreas = (floorId: number) => {
  return request.get<AreaInfo[]>(`/study-room/floors/${floorId}/areas`)
}

// 获取座位列表（可按楼层/区域/日期过滤）
export const getSeats = (params: SeatQuery = {}) => {
  return request.get<SeatInfo[]>('/study-room/seats', { params })
}

// 获取座位在某一天的进行中预约时段
export const getSeatReservations = (seatId: number, date: string) => {
  return request.get<SeatReservation[]>(`/study-room/seats/${seatId}/reservations`, { params: { date } })
}

// 预约座位
export const bookSeat = (data: SeatBookingParams) => {
  return request.post<null>('/study-room/seats/book', data)
}

// 取消预约（可按预约记录 ID 精确取消）
export const cancelBooking = (seatId: number, reservationId: number) => {
  return request.delete<null>(`/study-room/seats/${seatId}/cancel`, { params: { reservationId } })
}

// 获取当前用户的预约记录（含座位、楼层、区域信息）
export const getMyReservations = () => {
  return request.get<ReservationVO[]>('/study-room/my-reservations')
}

// 签到（待签到 -> 使用中）
export const checkIn = (reservationId: number) => {
  return request.post<null>(`/study-room/check-in?reservationId=${reservationId}`)
}

// 签退（使用中 -> 已完成）
export const checkOut = (reservationId: number) => {
  return request.post<null>(`/study-room/check-out?reservationId=${reservationId}`)
}

// 删除预约记录（仅已完成/违约/已取消等历史记录）
export const deleteReservation = (reservationId: number) => {
  return request.delete<null>(`/study-room/reservations/${reservationId}`)
}
