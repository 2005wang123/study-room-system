// src/api/floorLayout.ts
import request from '@/utils/request'
import type { FloorLayoutData } from '@/types/api'

// 获取指定楼层的已发布结构图（公开）
export const getPublishedLayout = (floorId: number) => {
  return request.get<FloorLayoutData>(`/study-room/floor-layout/${floorId}`)
}

// 获取草稿（管理员，无草稿时返回已发布版本）
export const getDraftLayout = (floorId: number) => {
  return request.get<FloorLayoutData>(`/study-room/floor-layout/${floorId}/draft`)
}

// 保存草稿
export const saveDraftLayout = (floorId: number, layoutJson: string) => {
  return request.post<null>(`/study-room/floor-layout/${floorId}/draft`, { layoutJson })
}

// 发布结构图（同时同步 seat 表）
export const publishLayout = (floorId: number, layoutJson: string) => {
  return request.post<{ layoutJson: string; warnings: string[] }>(`/study-room/floor-layout/${floorId}/publish`, {
    layoutJson
  })
}
