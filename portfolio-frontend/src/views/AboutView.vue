<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const socialLinks = [
  {
    name: 'X (Twitter)',
    url: 'https://x.com/Kaede__P',
    icon: '🐦', 
    color: 'hover:bg-blue-500/10 hover:text-blue-400 hover:border-blue-500/50'
  },
  {
    name: 'YouTube',
    url: 'https://youtube.com/@KaedeP0111',
    icon: '▶️',
    color: 'hover:bg-red-500/10 hover:text-red-400 hover:border-red-500/50'
  },
  {
    name: 'Bilibili',
    url: 'https://space.bilibili.com/26553865',
    icon: '📺',
    color: 'hover:bg-pink-500/10 hover:text-pink-400 hover:border-pink-500/50'
  }
];

// 技术栈标签
const techStacks = [
  { category: 'Frontend', items: ['Vue 3', 'TypeScript', 'Tailwind CSS', 'Vite'] },
  { category: 'Backend', items: ['Java','Spring Boot', 'Spring Security', 'PostgreSQL'] },
  { category: 'Tools', items: ['Git', 'Gemini', 'Docker', 'Maven', 'Nginx',] }
];

</script>

<template>
  <div class="min-h-[80vh] flex flex-col items-center justify-center px-6 py-12 animate-fade-in">
    <div class="w-full max-w-3xl bg-slate-900/60 backdrop-blur-xl border border-white/10 rounded-[2rem] p-8 md:p-12 shadow-2xl">
      
      <section class="mb-12">
        <h2 class="text-3xl font-black text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-emerald-400 mb-6">
          {{ t('about.title') }}
        </h2>
        <p class="text-slate-300 leading-relaxed space-y-4">
          {{ t('about.purpose1') }}<br/><br/>
          {{ t('about.purpose2') }}
        </p>
      </section>

      <hr class="border-white/5 mb-10" />

      <section class="mb-12">
        <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
          <span>🛠️</span> {{ t('about.techStack') }}
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div v-for="stack in techStacks" :key="stack.category" class="bg-white/5 rounded-2xl p-5 border border-white/5">
            <h4 class="text-sm font-black text-slate-500 uppercase tracking-wider mb-4">{{ stack.category }}</h4>
            <div class="flex flex-wrap gap-2">
              <span 
                v-for="item in stack.items" 
                :key="item"
                class="px-3 py-1 bg-white/10 text-slate-300 text-xs font-medium rounded-full"
              >
                {{ item }}
              </span>
            </div>
          </div>
        </div>
      </section>

      <hr class="border-white/5 mb-10" />

      <section>
        <h3 class="text-xl font-bold text-white mb-4 flex items-center gap-2">
          <span>📬</span> {{ t('about.contactTitle') }}
        </h3>
        <p class="text-slate-400 text-sm mb-6">
          {{ t('about.contactDesc') }} 
          <a href="mailto:kaedep0828@gmail.com" class="text-blue-400 hover:text-blue-300 transition-colors font-medium">
            kaedep0828@gmail.com
          </a>
        </p>
      </section>

      <hr class="border-white/5 mb-10" />

      <section>
        <h3 class="text-xl font-bold text-white mb-5 flex items-center gap-2">
          <span>❤</span> {{ t('about.other') }}
        </h3>
        <div class="flex flex-wrap gap-4">
          <a 
            v-for="social in socialLinks" 
            :key="social.name"
            :href="social.url"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-2 px-5 py-3 rounded-xl border border-white/10 bg-white/5 text-slate-300 font-bold transition-all duration-300 active:scale-95"
            :class="social.color"
          >
            <span class="text-xl">{{ social.icon }}</span>
            {{ social.name }}
          </a>
        </div>
      </section>

    </div>
  </div>
</template>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.6s ease-out forwards;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

<!-- <template>
  <div class="flex flex-col items-center justify-center min-h-[calc(100vh-300px)] text-center px-4 -mt-10">
    <h1 class="text-5xl font-extrabold mb-6 bg-linear-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
      关于此网站
    </h1>
    <p class="text-xl text-slate-300 mb-1 max-w-2xl leading-relaxed">
      技术栈：Java; SpringBoot; Vue; CSS; Vite; Tailwind; PostgreSQL
    </p>
    <p class="text-xl text-slate-400 mb-1 max-w-3xl leading-relaxed">
      本网站旨在展示个人经历，兴趣爱好，代码能力，内容和功能尚不完整，努力开发中。
    </p>
    <p class="text-xl text-slate-400 max-w-3xl leading-relaxed">
      若您在浏览过程中遇到任何问题，欢迎携报错信息通过以下方式联系我。
    </p>
    <p class="text-base text-slate-300 max-w-2xl leading-relaxed">
      Email：见主页
    </p>
    <p class="text-base text-slate-300 max-w-2xl leading-relaxed">
      QQ：1965813544
    </p>
  </div>
</template> -->