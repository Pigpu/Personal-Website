<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { ref, onMounted, computed } from "vue";
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const route = useRoute();
const router = useRouter();
const username = ref<string | null>(null);
const showLogoutModal = ref(false);
const showGoodbyeModal = ref(false);

// 切换语言的函数
const changeLang = (lang: 'zh' | 'en' | 'ja') => {
  locale.value = lang // 切换界面语言
  localStorage.setItem('site_lang', lang) // 记住用户的选择
}

// 用于在按钮上显示当前语言
const currentLangLabel = computed(() => {
  if (locale.value === 'zh') return '中'
  if (locale.value === 'en') return 'EN'
  if (locale.value === 'ja') return '日'
  return '中'
})

// 初始化时获取登录状态
onMounted(() => {
  username.value = localStorage.getItem("username");
});

// 退出登录逻辑
const handleLogout = () => {
  showLogoutModal.value = false;
  showGoodbyeModal.value = true;

  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("user_role");

  username.value = null;

  setTimeout(() => {
    router.push("/").then(() => {
      window.location.reload();
    });
  }, 1500);
};

// ✅ 修改点 1：将写死的中文换成 i18n 的字典 key
const navLinks = [
  { key: "nav.home", path: "/" },
  { key: "nav.career", path: "/career" },
  { key: "nav.articles", path: "/articles" },
  { key: "nav.projects", path: "/projects" },
  { key: "nav.about", path: "/about" },
];
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
          {{ t(link.key) }}
          
          <div
            v-if="route.path === link.path"
            class="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 bg-blue-400 rounded-full shadow-[0_0_8px_#60a5fa]"
          >
          </div>
        </router-link>
      </div>

      <div class="flex-1 flex justify-end gap-4 items-center">
        
        <div class="relative group z-50 flex items-center h-full">
          <div class="px-2 py-2 rounded-full text-xs relative group text-slate-400">
          Change language
          </div>
          <button class="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 border border-white/10 text-slate-300 hover:text-white hover:bg-white/10 transition-all">
            <span class="text-xs font-black">{{ currentLangLabel }}</span>
          </button>
          
          <div class="absolute right-0 top-full pt-2 w-32 opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 pointer-events-none group-hover:pointer-events-auto">
            <div class="bg-slate-900/95 backdrop-blur-2xl border border-white/10 rounded-2xl overflow-hidden shadow-2xl py-2 flex flex-col">
              <button @click="changeLang('zh')" :class="['px-5 py-3 text-sm text-left hover:bg-white/10 transition-colors', locale === 'zh' ? 'text-blue-400 font-bold' : 'text-slate-300']">
                🇨🇳 中文
              </button>
              <button @click="changeLang('en')" :class="['px-5 py-3 text-sm text-left hover:bg-white/10 transition-colors', locale === 'en' ? 'text-blue-400 font-bold' : 'text-slate-300']">
                🇺🇸 English
              </button>
              <button @click="changeLang('ja')" :class="['px-5 py-3 text-sm text-left hover:bg-white/10 transition-colors', locale === 'ja' ? 'text-blue-400 font-bold' : 'text-slate-300']">
                🇯🇵 日本語
              </button>
            </div>
          </div>
        </div>

        <router-link
          v-if="!username"
          to="/login"
          class="px-5 py-1.5 bg-blue-500 hover:bg-blue-600 text-white text-xs font-bold rounded-full transition-all shadow-[0_0_15px_rgba(59,130,246,0.3)]"
        >
          {{ t('nav.login') }}
        </router-link>

        <button
          v-else
          @click="showLogoutModal = true"
          class="flex items-center gap-2 px-4 py-1.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-full transition-all group"
        >
          <span class="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-pulse"></span>
          <span class="text-slate-200 group-hover:text-white text-xs font-bold tracking-wider">
            {{ username }}
          </span>
        </button>
      </div>

      <Teleport to="body">
        <Transition name="fade">
          <div
            v-if="showLogoutModal"
            class="fixed inset-0 z-[100] flex items-center justify-center p-6"
          >
            <div
              class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"
              @click="showLogoutModal = false"
            ></div>

            <div class="relative w-full max-w-xs bg-slate-900/90 backdrop-blur-2xl border border-white/10 p-8 rounded-[2.5rem] shadow-2xl">
              <div class="text-center mb-6">
                <div class="w-12 h-12 bg-red-500/20 text-red-400 rounded-2xl flex items-center justify-center mx-auto mb-4 text-xl">
                  ⚠️
                </div>
                <h3 class="text-lg font-bold text-white">{{ t('nav.quit') }}</h3>
                <p class="text-slate-400 text-xs mt-2">{{ t('nav.quitDesc') }}</p>
              </div>

              <div class="space-y-3">
                <button
                  @click="handleLogout"
                  class="w-full py-3 bg-red-500 hover:bg-red-600 text-white text-sm font-bold rounded-2xl transition-all shadow-lg shadow-red-500/20"
                >
                  {{ t('nav.confirm') }}
                </button>
                <button
                  @click="showLogoutModal = false"
                  class="w-full py-3 bg-white/5 hover:bg-white/10 text-slate-300 text-sm font-bold rounded-2xl transition-all border border-white/5"
                >
                  {{ t('nav.back') }}
                </button>
              </div>
            </div>
          </div>
        </Transition>
        <Transition name="fade">
          <div
            v-if="showGoodbyeModal"
            class="fixed inset-0 z-[100] flex items-center justify-center p-6"
          >
            <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"></div>

            <div class="relative bg-slate-900/90 backdrop-blur-2xl border border-white/10 px-10 py-8 rounded-4xl shadow-2xl flex flex-col items-center scale-in-center">
              <div class="text-4xl mb-4 animate-bounce">👋</div>
              <h3 class="text-xl font-black text-white">{{ t('nav.quitMes') }}</h3>
              <p class="text-slate-400 text-sm mt-2">{{ t('nav.quitMes2') }}</p>
            </div>
          </div>
        </Transition>
      </Teleport>

    </div>
  </nav>
</template>

<style scoped>
.scale-in-center {
  animation: scale-in-center 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}
@keyframes scale-in-center {
  0% { transform: scale(0.9); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>