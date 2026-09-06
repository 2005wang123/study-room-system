// src/api/floor.ts —— 楼层 / 区域管理（仅管理员）
import request from '@/utils/request'

export interface FloorPayload {
  floorName: string
  floorNumber?: number
  status?: number
  remark?: string
  bgImageUrl?: string
}

export interface AreaPayload {
  floorId: number
  areaName: string
  sortOrder?: number
  status?: number
  remark?: string
}

// ===== 楼层 =====
export const createFloor = (data: FloorPayload) => request.post<null>('/study-room/admin/floors', data)
export const updateFloor = (id: number, data: FloorPayload) => request.put<null>(`/study-room/admin/floors/${id}`, data)
export const deleteFloor = (id: number) => request.delete<null>(`/study-room/admin/floors/${id}`)

// ===== 区域 =====
export const createArea = (data: AreaPayload) => request.post<null>('/study-room/admin/areas', data)
export const updateArea = (id: number, data: AreaPayload) => request.put<null>(`/study-room/admin/areas/${id}`, data)
export const deleteArea = (id: number) => request.delete<null>(`/study-room/admin/areas/${id}`)
