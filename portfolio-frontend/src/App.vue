<script setup lang="ts">
// @ts-ignore
import Navbar from './components/Navbar.vue' // 引入组件
</script>

<template>
  <div class="relative min-h-screen text-slate-100 font-sans overflow-x-hidden">
    <div class="fixed inset-0 -z-20 bg-[url('/src/assets/bg.jpg')] bg-cover bg-center"></div>
    <div class="fixed inset-0 -z-10 bg-slate-950/60 backdrop-blur-md"></div>

    <Navbar />

    <div class="pt-32 pb-12 px-4">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<style>
/* 简单的页面切换淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>