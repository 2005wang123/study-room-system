// src/api/user.js
import request from '../utils/request';

// 登录
export const login = (data) => {
  return request({
    url: '/user/login',
    method: 'post',
    data
  });
};

// 注册
export const register = (data) => {
  return request({
    url: '/user/register',
    method: 'post',
    data
  });
};

// 退出登录
export const logout = () => {
  return request({
    url: '/user/logout',
    method: 'post'
  });
};

// 获取当前用户信息
export const getUserInfo = () => {
  return request({
    url: '/user/info',
    method: 'get'
  });
};

// 修改密码
export const changePassword = (data) => {
  return request({
    url: '/user/change-password',
    method: 'post',
    data
  });
};

// 刷新token
export const refreshToken = () => {
  return request({
    url: '/user/refresh',
    method: 'post'
  });
};

// ===== 管理员：用户管理 =====

// 用户分页列表
export const getAdminUsers = (params) => {
  return request({
    url: '/user/admin/users',
    method: 'get',
    params
  });
};

// 新增用户
export const createAdminUser = (data) => {
  return request({
    url: '/user/admin/users',
    method: 'post',
    data
  });
};

// 启用/禁用用户
export const updateUserStatus = (id, status) => {
  return request({
    url: `/user/admin/users/${id}/status`,
    method: 'put',
    params: { status }
  });
};

// 重置密码
export const resetUserPassword = (id) => {
  return request({
    url: `/user/admin/users/${id}/reset-password`,
    method: 'put'
  });
};

// 删除用户
export const deleteAdminUser = (id) => {
  return request({
    url: `/user/admin/users/${id}`,
    method: 'delete'
  });
};
