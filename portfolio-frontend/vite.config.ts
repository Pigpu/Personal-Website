import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite' // 新增
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(), // 新增
  ],
  resolve: {
  alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
  }
})