// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // <--- 1. 引入 path 模块 (必须)

export default defineConfig({
  plugins: [vue()],
  
  // <--- 2. 添加 resolve 配置
  resolve: {
    alias: {
      // 将 '@' 映射到 'src' 目录的绝对路径
      '@': path.resolve(__dirname, './src') 
    }
  },

  server: {
    port: 5137,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', //后端服务器地址
        changeOrigin: true, //允许跨域
        //rewrite: (path) => path.replace(/^\/api/, '') // ✅ 核心：把 /api 替换为空字符串
      }
    }
  }
})