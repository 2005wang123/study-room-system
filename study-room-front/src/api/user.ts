// src/api/user.ts
import request from '@/utils/request'
import type {
  ApiResult,
  AdminCreateUserParams,
  AdminUser,
  ChangePasswordParams,
  LoginParams,
  LoginResult,
  PageData,
  RegisterParams,
  UserInfo
} from '@/types/api'

// 登录
export const login = (data: LoginParams) => {
  return request.post<LoginResult>('/user/login', data)
}

// 注册
export const register = (data: RegisterParams) => {
  return request.post<null>('/user/register', data)
}

// 退出登录
export const logout = () => {
  return request.post<null>('/user/logout')
}

// 获取当前用户信息
export const getUserInfo = () => {
  return request.get<UserInfo>('/user/info')
}

// 修改密码
export const changePassword = (data: ChangePasswordParams) => {
  return request.post<null>('/user/change-password', data)
}

// 刷新 token
export const refreshToken = () => {
  return request.post<LoginResult>('/user/refresh')
}

// ===== 管理员：用户管理 =====

// 用户分页列表
export const getAdminUsers = (params: { pageNum: number; pageSize: number; keyword?: string }) => {
  return request.get<PageData<AdminUser>>('/user/admin/users', { params })
}

// 新增用户（返回初始密码）
export const createAdminUser = (data: AdminCreateUserParams) => {
  return request.post<string | null>('/user/admin/users', data)
}

// 启用/禁用用户
export const updateUserStatus = (id: number, status: number) => {
  return request.put<null>(`/user/admin/users/${id}/status`, undefined, { params: { status } })
}

// 重置密码（返回临时密码）
export const resetUserPassword = (id: number) => {
  return request.put<string | null>(`/user/admin/users/${id}/reset-password`)
}

// 删除用户
export const deleteAdminUser = (id: number) => {
  return request.delete<null>(`/user/admin/users/${id}`)
}

// 调整用户信用积分（delta>0 加分，delta<0 减分）
export const adjustUserPoints = (id: number, delta: number) => {
  return request.put<number | null>(`/user/admin/users/${id}/points`, undefined, { params: { delta } })
}

export type { ApiResult }
