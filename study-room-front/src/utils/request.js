// src/utils/request.js
import axios from 'axios';

// 开发环境走 Vite 代理（/api -> http://localhost:8080）；
// 生产环境可通过 VITE_API_BASE_URL 指定后端地址
const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000,
    withCredentials: true
});

// 请求拦截器 - 添加token
request.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// 响应拦截器 - 处理401
request.interceptors.response.use(
    response => response.data,
    error => {
        if (error.response?.status === 401) {
            // 触发登录弹窗
            window.dispatchEvent(new CustomEvent('unauthorized'));
        }
        return Promise.reject(error);
    }
);

export default request;