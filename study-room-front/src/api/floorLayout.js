// src/api/floorLayout.js
import request from '../utils/request';

// 获取指定楼层的已发布结构图（公开）
export const getPublishedLayout = (floorId) => {
  return request.get(`/study-room/floor-layout/${floorId}`);
};

// 获取草稿（管理员，无草稿时返回已发布版本）
export const getDraftLayout = (floorId) => {
  return request.get(`/study-room/floor-layout/${floorId}/draft`);
};

// 保存草稿
export const saveDraftLayout = (floorId, layoutJson) => {
  return request.post(`/study-room/floor-layout/${floorId}/draft`, { layoutJson });
};

// 发布结构图（同时同步 seat 表）
export const publishLayout = (floorId, layoutJson) => {
  return request.post(`/study-room/floor-layout/${floorId}/publish`, { layoutJson });
};
