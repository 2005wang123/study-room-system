import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'

// 1. 引入刚才创建的路由
import router from './router'

const app = createApp(App)

app.use(createPinia())
// 2. 使用路由
app.use(router)

app.mount('#app')
