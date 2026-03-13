<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { ref, onMounted, onUnmounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import axios from "axios"; // 🌟 新增：用于请求天气 API

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();
const username = ref<string | null>(null);
const showLogoutModal = ref(false);
const showGoodbyeModal = ref(false);
const isAdmin = computed(
  () => localStorage.getItem("user_role") === "ROLE_ADMIN"
);

// ================= 1. 语言与用户逻辑 (原有) =================
const changeLang = (lang: "zh" | "en" | "ja") => {
  locale.value = lang;
  localStorage.setItem("site_lang", lang);
};

const currentLangLabel = computed(() => {
  if (locale.value === "zh") return "中";
  if (locale.value === "en") return "EN";
  if (locale.value === "ja") return "日";
  return "中";
});

const handleLogout = () => {
  showLogoutModal.value = false;
  showGoodbyeModal.value = true;
  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("user_role");
  username.value = null;
  setTimeout(() => {
    router.push("/").then(() => window.location.reload());
  }, 1500);
};

const navLinks = [
  { key: "nav.home", path: "/" },
  { key: "nav.career", path: "/career" },
  { key: "nav.paper", path: "/paper" },
  { key: "nav.articles", path: "/articles" },
  { key: "nav.projects", path: "/projects" },
  { key: "nav.about", path: "/about" },
];

// ================= 2. 实时时间逻辑 =================
const currentTime = ref("");
const currentDate = ref("");
let timer: number;

const updateTime = () => {
  const now = new Date();
  currentDate.value = now
    .toLocaleDateString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    })
    .replace(/\//g, ".");
  currentTime.value = now.toLocaleTimeString("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });

  calculateCountdown();
};

const countdownEvent = ref(
  localStorage.getItem("countdown_event") || "JLPT N2"
);
const targetDate = ref(localStorage.getItem("countdown_date") || "2026-07-05");
const daysLeft = ref<number>(0);

const calculateCountdown = () => {
  const now = new Date();
  const target = new Date(targetDate.value);
  now.setHours(0, 0, 0, 0);
  target.setHours(0, 0, 0, 0);
  const diffTime = target.getTime() - now.getTime();
  daysLeft.value = Math.max(0, Math.ceil(diffTime / (1000 * 60 * 60 * 24)));
};

const showCountdownModal = ref(false);
const editForm = ref({ event: "", date: "" });

// 打开弹窗：只有管理员点击才有效
const openCountdownModal = () => {
  if (!isAdmin.value) return;
  editForm.value.event = countdownEvent.value;
  editForm.value.date = targetDate.value;
  showCountdownModal.value = true;
};

// 保存配置
const saveCountdownConfig = () => {
  if (!editForm.value.event || !editForm.value.date) return; // 简单防空

  // 更新响应式变量
  countdownEvent.value = editForm.value.event;
  targetDate.value = editForm.value.date;

  // 存入浏览器本地缓存，刷新不丢失
  localStorage.setItem("countdown_event", countdownEvent.value);
  localStorage.setItem("countdown_date", targetDate.value);

  // 重新计算并关闭弹窗
  calculateCountdown();
  showCountdownModal.value = false;
};

// ================= 3. 实时天气逻辑 =================
const weatherInfo = ref({ temp: "", icon: "🌤️", loading: true });

const getWeatherIcon = (code: number) => {
  if (code === 0) return "☀️";
  if (code >= 1 && code <= 3) return "⛅";
  if (code >= 45 && code <= 48) return "🌫️";
  if (code >= 51 && code <= 67) return "🌧️";
  if (code >= 71 && code <= 77) return "❄️";
  if (code >= 95) return "⛈️";
  return "🌤️";
};

const fetchWeather = () => {
  // 🌟 安全检查：判断浏览器是否支持定位且当前环境是否允许
  if (navigator && navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        getWeatherData(position.coords.latitude, position.coords.longitude);
      },
      (error) => {
        console.warn("用户拒绝定位或获取失败，使用默认坐标", error);
        getWeatherData(35.6937, 139.7036); // 默认兜底坐标
      }
    );
  } else {
    console.warn("当前环境不支持地理位置 API (可能非 HTTPS)，使用默认坐标");
    getWeatherData(35.6937, 139.7036); // 默认兜底坐标
  }
};

const getWeatherData = async (lat: number, lon: number) => {
  try {
    const res = await axios.get(
      `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current_weather=true`
    );
    const current = res.data.current_weather;
    weatherInfo.value = {
      temp: `${Math.round(current.temperature)}°C`,
      icon: getWeatherIcon(current.weathercode),
      loading: false,
    };
  } catch (error) {
    console.error("获取天气请求失败", error);
    weatherInfo.value.loading = false;
  }
};

onMounted(() => {
  // 🌟 使用 try...catch 保护初始化钩子，防止任何单一功能报错拖垮整个网页
  try {
    username.value = localStorage.getItem("username");

    // 启动时钟与天气
    updateTime();
    timer = window.setInterval(updateTime, 1000);
    fetchWeather();
  } catch (err) {
    console.error("Navbar 初始化遇到错误，但已被拦截:", err);
  }
});

onUnmounted(() => {
  clearInterval(timer);
});
</script>

<template>
  <div class="fixed top-6 left-0 w-full z-50 flex justify-center pointer-events-none px-6">

    <div class="absolute left-10 top-1/2 -translate-y-1/2 hidden xl:flex items-center gap-5 pointer-events-auto opacity-70 hover:opacity-100 transition-opacity duration-300">

      <div class="flex flex-col items-start">
        <span class="text-slate-400 text-[11px] font-black tracking-[0.2em] mb-0.5">{{ currentDate }}</span>
        <span class="text-white text-2xl font-black font-mono drop-shadow-lg tracking-tight">{{ currentTime }}</span>
      </div>

      <div class="h-8 w-px bg-white/20 rounded-full"></div>

      <div
        @click="openCountdownModal"
        class="flex flex-col items-start justify-center bg-blue-500/10 border border-blue-500/20 px-3 py-1.5 rounded-xl shadow-[0_0_15px_rgba(59,130,246,0.15)] transition-all"
        :class="isAdmin ? 'cursor-pointer hover:bg-blue-500/30 hover:scale-105 active:scale-95' : 'cursor-default hover:bg-blue-500/20'"
      >
        <span class="text-blue-400 text-[9px] font-black uppercase tracking-widest mb-0.5">{{ countdownEvent }}</span>
        <div class="flex items-baseline gap-1 leading-none">
          <span class="text-slate-300 text-xs font-bold">D-</span>
          <span class="text-white text-xl font-black font-mono">{{ daysLeft }}</span>
        </div>
      </div>

    </div>

    <nav class="w-full max-w-6xl bg-slate-900/50 backdrop-blur-xl border border-white/10 px-6 py-3 rounded-full flex items-center shadow-2xl pointer-events-auto">
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
          ></div>
        </router-link>
      </div>

      <div class="flex-1 flex justify-end gap-4 items-center">
        <div class="relative group z-50 flex items-center h-full">
          <div class="px-2 py-2 rounded-full text-xs relative group text-slate-400 hidden md:block">
            Change language
          </div>
          <button class="flex items-center justify-center w-8 h-8 rounded-full bg-white/5 border border-white/10 text-slate-300 hover:text-white hover:bg-white/10 transition-all">
            <span class="text-xs font-black">{{ currentLangLabel }}</span>
          </button>

          <div class="absolute right-0 top-full pt-2 w-32 opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 pointer-events-none group-hover:pointer-events-auto">
            <div class="bg-slate-900/95 backdrop-blur-2xl border border-white/10 rounded-2xl overflow-hidden shadow-2xl py-2 flex flex-col">
              <button
                @click="changeLang('zh')"
                :class="['px-5 py-3 text-sm text-left hover:bg-white/10 transition-colors', locale === 'zh' ? 'text-blue-400 font-bold' : 'text-slate-300']"
              >🇨🇳 中文</button>
              <button
                @click="changeLang('en')"
                :class="['px-5 py-3 text-sm text-left hover:bg-white/10 transition-colors', locale === 'en' ? 'text-blue-400 font-bold' : 'text-slate-300']"
              >🇺🇸 English</button>
              <button
                @click="changeLang('ja')"
                :class="['px-5 py-3 text-sm text-left hover:bg-white/10 transition-colors', locale === 'ja' ? 'text-blue-400 font-bold' : 'text-slate-300']"
              >🇯🇵 日本語</button>
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
    </nav>

    <div class="absolute right-10 top-1/2 -translate-y-1/2 hidden xl:flex items-center gap-3 pointer-events-auto opacity-70 hover:opacity-100 transition-opacity duration-300 bg-slate-900/40 backdrop-blur-md border border-white/10 px-4 py-2 rounded-2xl shadow-xl">
      <template v-if="!weatherInfo.loading">
        <span class="text-2xl drop-shadow-md">{{ weatherInfo.icon }}</span>
        <div class="flex flex-col items-end">
          <span class="text-slate-400 text-[10px] font-black uppercase tracking-widest leading-none mb-1">Local</span>
          <span class="text-white text-sm font-bold leading-none">{{ weatherInfo.temp }}</span>
        </div>
      </template>
      <template v-else>
        <span class="text-slate-400 text-xs animate-pulse">Fetching...</span>
      </template>
    </div>

    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showLogoutModal"
          class="fixed inset-0 z-[100] flex items-center justify-center p-6"
        >
          <div
            class="absolute inset-0 bg-slate-950/80 backdrop-blur-md"
            @click="showLogoutModal = false"
          ></div>
          <div class="relative w-full max-w-xs bg-slate-900 border border-white/10 p-8 rounded-[2.5rem] shadow-2xl">
            <div class="text-center mb-6">
              <div class="w-12 h-12 bg-red-500/20 text-red-400 rounded-2xl flex items-center justify-center mx-auto mb-4 text-xl">⚠️</div>
              <h3 class="text-lg font-bold text-white">{{ t('nav.quit') }}</h3>
              <p class="text-slate-400 text-xs mt-2">{{ t('nav.quitDesc') }}</p>
            </div>
            <div class="space-y-3">
              <button
                @click="handleLogout"
                class="w-full py-3 bg-red-500 hover:bg-red-600 text-white text-sm font-bold rounded-2xl transition-all shadow-lg shadow-red-500/20"
              >{{ t('nav.confirm') }}</button>
              <button
                @click="showLogoutModal = false"
                class="w-full py-3 bg-white/5 hover:bg-white/10 text-slate-300 text-sm font-bold rounded-2xl transition-all border border-white/5"
              >{{ t('nav.back') }}</button>
            </div>
          </div>
        </div>
      </Transition>
      <Transition name="fade">
        <div
          v-if="showGoodbyeModal"
          class="fixed inset-0 z-[100] flex items-center justify-center p-6"
        >
          <div class="absolute inset-0 bg-slate-950/80 backdrop-blur-md"></div>
          <div class="relative bg-slate-900 border border-white/10 px-10 py-8 rounded-[2.5rem] shadow-2xl flex flex-col items-center scale-in-center">
            <div class="text-4xl mb-4 animate-bounce">👋</div>
            <h3 class="text-xl font-black text-white">{{ t('nav.quitMes') }}</h3>
            <p class="text-slate-400 text-sm mt-2">{{ t('nav.quitMes2') }}</p>
          </div>
        </div>
      </Transition>
      <Transition name="fade">
        <div
          v-if="showCountdownModal"
          class="fixed inset-0 z-[100] flex items-center justify-center p-6"
        >
          <div
            class="absolute inset-0 bg-slate-950/80 backdrop-blur-md"
            @click="showCountdownModal = false"
          ></div>

          <div class="relative w-full max-w-sm bg-slate-900 border border-white/10 p-8 rounded-[2rem] shadow-2xl scale-in-center">
            <h3 class="text-xl font-black text-white mb-6 flex items-center gap-2">
              <span class="text-blue-400">⏱️</span> 倒数日设置
            </h3>

            <div class="space-y-4 mb-8">
              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-400 ml-1">事件名称 (建议简短)</label>
                <input
                  v-model="editForm.event"
                  type="text"
                  placeholder="例如: JLPT N2"
                  class="w-full bg-slate-800 border border-white/5 rounded-xl p-3 text-white text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all"
                />
              </div>

              <div class="space-y-1.5">
                <label class="text-xs font-bold text-slate-400 ml-1">目标日期</label>
                <input
                  v-model="editForm.date"
                  type="date"
                  class="w-full bg-slate-800 border border-white/5 rounded-xl p-3 text-white text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all scheme-dark"
                />
              </div>
            </div>

            <div class="flex gap-3">
              <button
                @click="saveCountdownConfig"
                class="flex-1 bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 rounded-xl transition-all shadow-lg shadow-blue-500/20 active:scale-95"
              >
                保存配置
              </button>
              <button
                @click="showCountdownModal = false"
                class="px-5 bg-white/5 hover:bg-white/10 text-slate-300 font-bold py-3 rounded-xl transition-all border border-white/5 active:scale-95"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<style scoped>
.scale-in-center {
  animation: scale-in-center 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}
@keyframes scale-in-center {
  0% {
    transform: scale(0.9);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>