import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 引入刚才写的路由配置
import './style.css' // 确保 Tailwind 样式还在

const app = createApp(App)

app.use(router) // 核心：使用路由实例
app.mount('#app')