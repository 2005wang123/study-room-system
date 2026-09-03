// src/api/announcement.js
import request from '../utils/request';

// ===== 公告（公开） =====

// 获取已发布的公告列表
export const getAnnouncements = () => {
  return request.get('/announcements');
};

// 获取公告详情
export const getAnnouncementDetail = (id) => {
  return request.get(`/announcements/${id}`);
};

// ===== 公告评论 =====

// 获取公告评论列表（公开，登录后返回点赞状态）
export const getAnnouncementComments = (announcementId) => {
  return request.get(`/announcements/${announcementId}/comments`);
};

// 发表评论（需登录）
export const addAnnouncementComment = (announcementId, content) => {
  return request.post(`/announcements/${announcementId}/comments`, { content });
};

// 删除评论（本人或管理员）
export const deleteAnnouncementComment = (commentId) => {
  return request.delete(`/announcements/comments/${commentId}`);
};

// 点赞评论
export const likeAnnouncementComment = (commentId) => {
  return request.post(`/announcements/comments/${commentId}/like`);
};

// 取消点赞评论
export const unlikeAnnouncementComment = (commentId) => {
  return request.delete(`/announcements/comments/${commentId}/like`);
};

// ===== 公告管理（仅管理员） =====

// 管理员分页查询全部公告（含草稿/下架）
export const getAdminAnnouncements = (params) => {
  return request.get('/announcements/admin/list', { params });
};

// 新增公告
export const createAnnouncement = (data) => {
  return request.post('/announcements', data);
};

// 更新公告
export const updateAnnouncement = (id, data) => {
  return request.put(`/announcements/${id}`, data);
};

// 删除公告（逻辑删除）
export const deleteAnnouncement = (id) => {
  return request.delete(`/announcements/${id}`);
};
