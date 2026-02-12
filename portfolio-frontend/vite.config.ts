import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite' // 新增

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(), // 新增
  ],
})