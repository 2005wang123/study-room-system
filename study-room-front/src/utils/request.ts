// src/utils/request.ts
import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import type { ApiResult } from '@/types/api'

const instance = axios.create({
  // 开发环境走 Vite 代理（/api -> http://localhost:8080）；
  // 生产环境可通过 VITE_API_BASE_URL 指定后端地址
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  withCredentials: true
})

// 请求拦截器 - 添加 token
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 - 统一解包后端 Result 结构并处理 401
instance.interceptors.response.use(
  // 后端统一返回 { code, message, data }，这里直接返回 data（即整个 Result 对象）
  (response) => response.data as never,
  (error) => {
    if (error.response?.status === 401) {
      // 触发登录弹窗
      window.dispatchEvent(new CustomEvent('unauthorized'))
    }
    return Promise.reject(error)
  }
)

interface ApiClient {
  request<T = unknown>(config: AxiosRequestConfig): Promise<ApiResult<T>>
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResult<T>>
}

const apiClient: ApiClient = {
  request<T = unknown>(config: AxiosRequestConfig): Promise<ApiResult<T>> {
    return instance.request(config) as unknown as Promise<ApiResult<T>>
  },
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResult<T>> {
    return instance.get(url, config) as unknown as Promise<ApiResult<T>>
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>> {
    return instance.post(url, data, config) as unknown as Promise<ApiResult<T>>
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>> {
    return instance.put(url, data, config) as unknown as Promise<ApiResult<T>>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResult<T>> {
    return instance.delete(url, config) as unknown as Promise<ApiResult<T>>
  }
}

export default apiClient
