// src/api/announcement.ts
import request from '@/utils/request'
import type { AnnouncementComment, AnnouncementItem, AnnouncementPayload, PageData } from '@/types/api'

// ===== 公告（公开） =====

// 获取已发布的公告列表
export const getAnnouncements = () => {
  return request.get<AnnouncementItem[]>('/announcements')
}

// 获取公告详情
export const getAnnouncementDetail = (id: number) => {
  return request.get<AnnouncementItem>(`/announcements/${id}`)
}

// ===== 公告评论 =====

// 获取公告评论列表（公开，登录后返回点赞状态）
export const getAnnouncementComments = (announcementId: number) => {
  return request.get<AnnouncementComment[]>(`/announcements/${announcementId}/comments`)
}

// 发表评论（需登录）
export const addAnnouncementComment = (announcementId: number, content: string) => {
  return request.post<AnnouncementComment>(`/announcements/${announcementId}/comments`, { content })
}

// 删除评论（本人或管理员）
export const deleteAnnouncementComment = (commentId: number) => {
  return request.delete<null>(`/announcements/comments/${commentId}`)
}

// 点赞评论
export const likeAnnouncementComment = (commentId: number) => {
  return request.post<null>(`/announcements/comments/${commentId}/like`)
}

// 取消点赞评论
export const unlikeAnnouncementComment = (commentId: number) => {
  return request.delete<null>(`/announcements/comments/${commentId}/like`)
}

// ===== 公告管理（仅管理员） =====

// 管理员分页查询全部公告（含草稿/下架）
export const getAdminAnnouncements = (params: {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number
}) => {
  return request.get<PageData<AnnouncementItem>>('/announcements/admin/list', { params })
}

// 新增公告
export const createAnnouncement = (data: AnnouncementPayload) => {
  return request.post<null>('/announcements', data)
}

// 更新公告
export const updateAnnouncement = (id: number, data: AnnouncementPayload) => {
  return request.put<null>(`/announcements/${id}`, data)
}

// 删除公告（逻辑删除）
export const deleteAnnouncement = (id: number) => {
  return request.delete<null>(`/announcements/${id}`)
}
