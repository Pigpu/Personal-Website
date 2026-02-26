import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite' // 新增
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(), // 新增
  ],
  server: {
    proxy: {
      // 只要是以 /api 开头的请求，统统拦截并转发给本地后端的 8080 端口
      '/api': {
        target: 'http://localhost:8080', 
        changeOrigin: true
      }
    }
  },
  resolve: {
  alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
  }
})