<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

// 定义导航链接
const navLinks = [
  { name: '主页', path: '/' },
  { name: '生涯', path: '/career' },
  { name: '生活', path: '/moments' }
]
</script>

<template>
  <nav class="fixed top-6 left-1/2 -translate-x-1/2 z-50 w-[90%] max-w-6xl">
    <div class="bg-slate-900/40 backdrop-blur-xl border border-white/10 px-6 py-3 rounded-full flex items-center shadow-2xl">
      
      <div class="flex-1 flex justify-start">
        <div class="text-xl font-black tracking-tighter text-blue-400 cursor-pointer">
          About me.
        </div>
      </div>

      <div class="flex items-center gap-1">
        <router-link 
          v-for="link in navLinks" 
          :key="link.path"
          :to="link.path"
          class="px-4 py-2 rounded-full text-sm font-medium transition-all duration-300 relative group"
          :class="route.path === link.path ? 'text-white' : 'text-slate-400 hover:text-slate-200'"
        >
          {{ link.name }}
          <div v-if="route.path === link.path" 
               class="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 bg-blue-400 rounded-full shadow-[0_0_8px_#60a5fa]">
          </div>
        </router-link>
      </div>

      <div class="flex-1 flex justify-end gap-3">
        <button class="p-2 text-slate-400 hover:text-white transition-colors">
          <span class="text-s font-mono">EN/JP</span>
        </button>
        <button class="px-4 py-1.5 bg-blue-500 hover:bg-blue-600 text-white text-xs font-bold rounded-full transition-all">
          LOGIN
        </button>
      </div>

    </div>
  </nav>
</template>