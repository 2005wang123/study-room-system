import { createRouter, createWebHistory, START_LOCATION } from 'vue-router'
// 引入我们刚才写的座位页面
import SeatMap from '../views/SeatMap.vue'
import AuthView from '../views/AuthView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      // 登录 / 注册：启动首屏（未登录时由路由守卫重定向至此）
      path: '/login',
      name: 'login',
      component: AuthView
    },
    {
      path: '/',
      name: 'home',
      component: SeatMap, // 默认打开座位图
      meta: { requiresAuth: true }
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
})

// 路由守卫
// 1) 项目启动首屏：无论是否已登录，都强制展示登录/注册界面
// 2) 未登录访问受保护页面 -> /login
// 使用 START_LOCATION 判定「真正首次导航」，不依赖可变标志，避免 HMR / 页面状态导致守卫失效。
router.beforeEach((to, from) => {
  const meta = to.meta as { requiresAuth?: boolean }
  const token = localStorage.getItem('token')

  // 首屏强制登录/注册：仅当 from 为起始位置（每次整页加载都是全新判定），且当前不是 /login
  if (from === START_LOCATION && to.path !== '/login') {
    return '/login'
  }

  // 未登录访问受保护页面 -> 登录页
  if (meta.requiresAuth && !token) {
    return '/login'
  }

  return true
})

export default router
