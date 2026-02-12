import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import './style.css' // 确保这一行存在，它会引入我们写了 @import "tailwindcss" 的文件

createApp(App).mount('#app')
