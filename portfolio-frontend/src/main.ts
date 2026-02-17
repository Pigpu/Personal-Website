import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 引入刚才写的路由配置
import './style.css' // 确保 Tailwind 样式还在
import axios from 'axios';

const app = createApp(App)

axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    console.log("拦截器已注入 Token:", token.substring(0, 10) + "..."); 
  }
  return config;
}, error => {
  if (error.response && error.response.status === 403) {
      alert("🔒 权限不足：仅管理员有权执行此操作。");
    }
    // 顺便处理一下 401（未登录或登录过期）
    if (error.response && error.response.status === 401) {
      alert("⚠️ 请先登录后再进行操作。");
    }
  return Promise.reject(error);
});

app.use(router) // 核心：使用路由实例
app.mount('#app')