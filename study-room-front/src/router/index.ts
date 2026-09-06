import { createRouter, createWebHistory } from 'vue-router'
// 引入我们刚才写的座位页面
import SeatMap from '../views/SeatMap.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: SeatMap // 默认打开座位图
    }
  ]
})

export default router
