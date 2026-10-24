<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { ref, onMounted, onUnmounted, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import axios from "axios"; // 🌟 新增：用于请求天气 API
import { getCountdown, saveCountdown } from '../services/countdown';
import { daysUntil, legacyCountdown } from '../utils/countdown';
import type { Countdown } from '../utils/countdown';

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
  if (languageDropdown.value) languageDropdown.value.open = false;
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
  { key: "nav.articles", path: "/articles" },
  { key: "nav.projects", path: "/projects" },
  { key: "nav.about", path: "/about" },
];

const isLinkActive = (path: string) => {
  if (path === "/articles") {
    return route.path === "/articles" || route.path.startsWith("/article/") || route.path === "/editor";
  }
  return path === "/" ? route.path === "/" : route.path.startsWith(path);
};

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

const countdown = ref<Countdown | null>(null);
const countdownStatus = ref<'loading' | 'ready' | 'error'>('loading');
const daysLeft = ref<number | null>(null);

const calculateCountdown = () => {
  daysLeft.value = countdown.value ? daysUntil(countdown.value.targetDate) : null;
};

let countdownRequest: Promise<boolean> | null = null;
const loadCountdown = (): Promise<boolean> => {
  if (countdownRequest) return countdownRequest;
  countdownStatus.value = 'loading';
  countdownRequest = (async () => {
    try {
      countdown.value = await getCountdown();
      calculateCountdown();
      countdownStatus.value = 'ready';
      return true;
    } catch {
      countdownStatus.value = 'error';
      return false;
    } finally { countdownRequest = null; }
  })();
  return countdownRequest;
};

const showCountdownModal = ref(false);
const editForm = ref({ event: "", date: "" });
const countdownSaving = ref(false);
const countdownOpening = ref(false);
const countdownError = ref('');
const countdownImported = ref(false);

// 打开弹窗：只有管理员点击才有效
const openCountdownModal = async () => {
  if (!isAdmin.value || countdownOpening.value) return;
  countdownOpening.value = true;
  countdownError.value = '';
  countdownImported.value = false;
  const loaded = await loadCountdown();
  let initial = countdown.value;
  if (loaded && !initial) {
    try { initial = legacyCountdown(localStorage.getItem('countdown_event'), localStorage.getItem('countdown_date')); }
    catch { /* Browser storage is optional. */ }
    countdownImported.value = !!initial;
  }
  editForm.value = { event: initial?.event ?? '', date: initial?.targetDate ?? '' };
  if (!loaded) countdownError.value = utilityCopy.value.countdownLoadError;
  countdownOpening.value = false;
  showCountdownModal.value = true;
};

// 保存配置
const saveCountdownConfig = async () => {
  if (!isAdmin.value || countdownSaving.value || countdownStatus.value !== 'ready') return;
  const event = editForm.value.event.trim();
  if (!event || event.length > 120 || daysUntil(editForm.value.date) === null) {
    countdownError.value = utilityCopy.value.countdownInvalid;
    return;
  }
  countdownSaving.value = true;
  countdownError.value = '';
  try {
    countdown.value = await saveCountdown({ event, targetDate: editForm.value.date });
    calculateCountdown();
    // Retire the old per-browser configuration only after the shared save succeeds.
    try { localStorage.removeItem('countdown_event'); localStorage.removeItem('countdown_date'); } catch { /* Optional cleanup. */ }
    showCountdownModal.value = false;
  } catch (error) {
    countdownError.value = axios.isAxiosError(error) && [401, 403].includes(error.response?.status ?? 0)
      ? utilityCopy.value.countdownForbidden : utilityCopy.value.countdownSaveError;
  } finally { countdownSaving.value = false; }
};

const closeCountdownModal = () => { if (!countdownSaving.value) showCountdownModal.value = false; };

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
      `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current_weather=true`, { timeout: 10000 }
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

const languageDropdown = ref<HTMLDetailsElement | null>(null);
const logoutDialog = ref<HTMLDialogElement | null>(null);
const goodbyeDialog = ref<HTMLDialogElement | null>(null);
const countdownDialog = ref<HTMLDialogElement | null>(null);
const utilityCopy = computed(() => {
  const labels = {
    zh: { language: '切换语言', weather: '当地天气', loading: '天气加载中', unavailable: '天气暂不可用', countdown: '倒数日设置', event: '事件名称', date: '目标日期', save: '保存配置', cancel: '取消', navigation: '主导航', countdownEmpty: '设置倒计时', countdownLoading: '倒计时加载中', countdownUnavailable: '倒计时暂不可用', countdownLoadError: '无法读取共享配置，请关闭窗口后重试。', countdownSaveError: '保存失败，请重试。你的修改仍保留在窗口中。', countdownForbidden: '请重新使用管理员账号登录后再保存。', countdownInvalid: '请填写事件名称（最多120字）和有效的目标日期。', countdownHint: '保存后对所有访客生效，刷新页面即可看到。每次保存覆盖当前设置。', countdownImported: '已填入此浏览器的旧设置，点击保存后才会同步给所有访客。', saving: '保存中…' },
    en: { language: 'Change language', weather: 'Local weather', loading: 'Loading weather', unavailable: 'Weather unavailable', countdown: 'Countdown settings', event: 'Event name', date: 'Target date', save: 'Save', cancel: 'Cancel', navigation: 'Main navigation', countdownEmpty: 'Set countdown', countdownLoading: 'Loading countdown', countdownUnavailable: 'Countdown unavailable', countdownLoadError: 'Cannot load shared settings. Close this window and try again.', countdownSaveError: 'Save failed. Your changes are still here; please try again.', countdownForbidden: 'Please sign in again with an administrator account.', countdownInvalid: 'Enter an event name (up to 120 characters) and a valid date.', countdownHint: 'Visitors will see your changes after refreshing. Each save replaces the current settings.', countdownImported: 'Your old browser settings are filled in. Save to share them with all visitors.', saving: 'Saving…' },
    ja: { language: '言語を変更', weather: '現地の天気', loading: '天気を読み込み中', unavailable: '天気情報を取得できません', countdown: 'カウントダウン設定', event: 'イベント名', date: '目標日', save: '保存', cancel: 'キャンセル', navigation: 'メインナビゲーション', countdownEmpty: 'カウントダウンを設定', countdownLoading: '読み込み中', countdownUnavailable: 'カウントダウンを取得できません', countdownLoadError: '共有設定を取得できません。ウィンドウを閉じて再試行してください。', countdownSaveError: '保存できませんでした。入力内容は保持されています。再試行してください。', countdownForbidden: '管理者アカウントで再ログインしてください。', countdownInvalid: 'イベント名（120文字以内）と有効な日付を入力してください。', countdownHint: '保存後、すべての訪問者に共有され、ページの再読み込みで反映されます。保存のたびに現在の設定を上書きします。', countdownImported: 'このブラウザの旧設定を入力しました。保存するとすべての訪問者に共有されます。', saving: '保存中…' },
  };
  return labels[locale.value as keyof typeof labels] || labels.zh;
});

watch(showLogoutModal, open => { if (open) logoutDialog.value?.showModal(); else logoutDialog.value?.close(); });
watch(showGoodbyeModal, open => { if (open) goodbyeDialog.value?.showModal(); else goodbyeDialog.value?.close(); });
watch(showCountdownModal, open => { if (open) countdownDialog.value?.showModal(); else countdownDialog.value?.close(); });
const closeLanguageMenu = (event: Event) => {
  if (languageDropdown.value && !languageDropdown.value.contains(event.target as Node)) languageDropdown.value.open = false;
};
const closeLanguageOnEscape = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && languageDropdown.value?.open) {
    languageDropdown.value.open = false;
    languageDropdown.value.querySelector('summary')?.focus();
  }
};
watch(() => route.fullPath, () => { if (languageDropdown.value) languageDropdown.value.open = false; });

onMounted(() => {
  void loadCountdown();
  document.addEventListener('pointerdown', closeLanguageMenu);
  document.addEventListener('keydown', closeLanguageOnEscape);
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
  document.removeEventListener('pointerdown', closeLanguageMenu);
  document.removeEventListener('keydown', closeLanguageOnEscape);
});
</script>

<template>
  <header class="site-header">
    <div class="nav-main">
      <router-link to="/" class="nav-brand">About me<span>.</span></router-link>
      <nav class="nav-links" :aria-label="utilityCopy.navigation">
        <router-link v-for="link in navLinks" :key="link.path" :to="link.path"
          :aria-current="isLinkActive(link.path) ? 'page' : undefined"
          :class="{ 'is-active': isLinkActive(link.path) }">
          {{ t(link.key) }}
        </router-link>
      </nav>
      <div class="nav-actions">
        <details ref="languageDropdown" class="language-menu">
          <summary :aria-label="utilityCopy.language">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" aria-hidden="true"><circle cx="12" cy="12" r="9"/><ellipse cx="12" cy="12" rx="4" ry="9"/><path d="M3 12h18"/></svg>
            <span>{{ currentLangLabel }}</span><span class="language-chevron" aria-hidden="true">⌄</span>
          </summary>
          <div class="language-options">
            <button @click="changeLang('zh')" :aria-pressed="locale === 'zh'">中文<span v-if="locale === 'zh'" aria-hidden="true">✓</span></button>
            <button @click="changeLang('en')" :aria-pressed="locale === 'en'">English<span v-if="locale === 'en'" aria-hidden="true">✓</span></button>
            <button @click="changeLang('ja')" :aria-pressed="locale === 'ja'">日本語<span v-if="locale === 'ja'" aria-hidden="true">✓</span></button>
          </div>
        </details>
        <router-link v-if="!username" to="/login" class="nav-login">{{ t('nav.login') }}<span aria-hidden="true">↗</span></router-link>
        <button v-else class="nav-account" @click="showLogoutModal = true"><span class="account-dot" aria-hidden="true"></span>{{ username }}</button>
      </div>
    </div>
    <div class="nav-utilities">
      <div class="utility-left">
        <span class="utility-date">{{ currentDate }}</span><time class="utility-time">{{ currentTime }}</time>
        <span v-if="isAdmin || countdown || countdownStatus === 'error'" class="utility-divider" aria-hidden="true"></span>
        <button v-if="isAdmin" class="utility-countdown" @click="openCountdownModal" :disabled="countdownOpening" :aria-label="utilityCopy.countdown">
          <template v-if="countdownStatus === 'ready' && countdown"><span>{{ countdown.event }}</span><strong v-if="daysLeft !== null">D−{{ daysLeft }}</strong></template>
          <span v-else>{{ countdownStatus === 'loading' ? utilityCopy.countdownLoading : countdownStatus === 'error' ? utilityCopy.countdownUnavailable : utilityCopy.countdownEmpty }}</span>
        </button>
        <span v-else-if="countdownStatus === 'ready' && countdown" class="utility-countdown"><span>{{ countdown.event }}</span><strong v-if="daysLeft !== null">D−{{ daysLeft }}</strong></span>
        <span v-else-if="countdownStatus === 'error'" class="utility-countdown">{{ utilityCopy.countdownUnavailable }}</span>
      </div>
      <div class="utility-weather" :aria-label="utilityCopy.weather">
        <span v-if="weatherInfo.loading">{{ utilityCopy.loading }}</span>
        <template v-else-if="weatherInfo.temp"><span aria-hidden="true">{{ weatherInfo.icon }}</span><span>{{ weatherInfo.temp }}</span></template>
        <span v-else>{{ utilityCopy.unavailable }}</span>
      </div>
    </div>
  </header>

  <Teleport to="body">
    <dialog ref="logoutDialog" class="nav-dialog" aria-labelledby="logout-title" @close="showLogoutModal = false" @click.self="showLogoutModal = false">
      <div class="nav-dialog-content">
        <h2 id="logout-title">{{ t('nav.quit') }}</h2><p>{{ t('nav.quitDesc') }}</p>
        <div class="dialog-actions"><button class="dialog-secondary" @click="showLogoutModal = false">{{ t('nav.back') }}</button><button class="dialog-primary" @click="handleLogout">{{ t('nav.confirm') }}</button></div>
      </div>
    </dialog>
    <dialog ref="goodbyeDialog" class="nav-dialog" aria-labelledby="goodbye-title" @close="showGoodbyeModal = false">
      <div class="nav-dialog-content"><h2 id="goodbye-title">{{ t('nav.quitMes') }}</h2><p>{{ t('nav.quitMes2') }}</p></div>
    </dialog>
    <dialog ref="countdownDialog" class="nav-dialog" aria-labelledby="countdown-title" @close="showCountdownModal = false" @click.self="closeCountdownModal" @cancel.prevent="closeCountdownModal">
      <form class="nav-dialog-content" @submit.prevent="saveCountdownConfig" :aria-busy="countdownSaving">
        <h2 id="countdown-title">{{ utilityCopy.countdown }}</h2>
        <p>{{ countdownImported ? utilityCopy.countdownImported : utilityCopy.countdownHint }}</p>
        <label for="countdown-event">{{ utilityCopy.event }}</label><input id="countdown-event" v-model="editForm.event" maxlength="120" :disabled="countdownSaving || countdownStatus !== 'ready'" required />
        <label for="countdown-date">{{ utilityCopy.date }}</label><input id="countdown-date" type="date" min="0001-01-01" max="9999-12-31" v-model="editForm.date" :disabled="countdownSaving || countdownStatus !== 'ready'" required />
        <p v-if="countdownError" class="countdown-error" role="alert">{{ countdownError }}</p>
        <div class="dialog-actions"><button type="button" class="dialog-secondary" :disabled="countdownSaving" @click="closeCountdownModal">{{ utilityCopy.cancel }}</button><button type="submit" class="dialog-primary" :disabled="countdownSaving || countdownStatus !== 'ready'">{{ countdownSaving ? utilityCopy.saving : utilityCopy.save }}</button></div>
      </form>
    </dialog>
  </Teleport>
</template>

<style scoped>
.site-header, .nav-dialog {
  --site-ink: #214777;
  --site-muted: #4a6382;
  --site-line: rgb(33 71 119 / 18%);
  --site-focus: #2857a5;
}
.site-header { position: sticky; top: 0; z-index: 50; background: #fff; color: var(--site-ink); border-bottom: 1px solid var(--site-line); }
.nav-main, .nav-utilities { width: calc(100% - clamp(64px, 7vw, 160px)); margin: 0 auto; }
.nav-main { min-height: 78px; display: grid; grid-template-columns: 180px 1fr 180px; gap: 20px; align-items: center; }
.nav-brand { font-size: 22px; font-weight: 600; letter-spacing: -.07em; text-decoration: none; color: var(--site-ink); width: fit-content; }
.nav-brand span { color: var(--site-accent); }
.nav-links { display: flex; justify-content: space-evenly; gap: 8px; align-self: stretch; }
.nav-links a { position: relative; display: inline-flex; flex: 1; justify-content: center; align-items: center; padding: 0 12px; white-space: nowrap; color: #4a6382; font-size: 17px; font-weight: 500; letter-spacing: .02em; text-decoration: none; transition: color .2s ease, background-color .2s ease; }
.nav-links a:focus-visible, .nav-links .is-active { color: var(--site-accent); }
.nav-links .is-active { font-weight: 600; }
.nav-links a::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 3px; background: var(--site-accent); transform: scaleX(0); transform-origin: center; transition: transform .25s cubic-bezier(.2, .7, .3, 1); }
.nav-links .is-active::after, .nav-links a:focus-visible::after { transform: scaleX(1); }
.nav-links a:focus-visible { background: rgb(40 87 165 / 8%); outline-offset: -4px; }
@media (hover: hover) {
  .nav-links a:hover { color: var(--site-accent); background: rgb(40 87 165 / 8%); }
  .nav-links a:hover::after { transform: scaleX(1); }
}
.nav-actions { display: flex; align-items: center; justify-content: flex-end; gap: 20px; font-size: 12px; }
.language-menu { position: relative; }
.language-menu summary { display: flex; align-items: center; gap: 7px; list-style: none; padding: 10px 0; }
.language-menu summary::-webkit-details-marker { display: none; }
.language-menu svg { width: 17px; height: 17px; }
.language-chevron { color: var(--site-muted); margin-left: 1px; }
.language-options { position: absolute; right: 0; top: calc(100% + 5px); width: 144px; background: #fff; padding: 6px; border: 1px solid var(--site-line); border-radius: 6px; box-shadow: 0 8px 20px rgb(36 60 88 / 10%); }
.language-options button { display: flex; justify-content: space-between; width: 100%; padding: 10px 12px; text-align: left; border: 0; background: transparent; border-radius: 3px; color: var(--site-ink); }
.language-options button:hover, .language-options button[aria-pressed="true"] { background: #edf3fc; }
.nav-login { display: flex; align-items: center; justify-content: center; gap: 17px; background: var(--site-accent); color: #fff; text-decoration: none; padding: 9px 16px; border-radius: 4px; font-size: 12px; white-space: nowrap; }
.nav-login:hover { background: #294d7c; }
.nav-account { display: inline-flex; align-items: center; gap: 7px; max-width: 110px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--site-ink); background: transparent; border: 1px solid var(--site-line); padding: 8px 12px; border-radius: 4px; }
.account-dot { width: 5px; height: 5px; border-radius: 50%; background: var(--site-accent); flex-shrink: 0; }
.nav-utilities { display: flex; justify-content: space-between; align-items: center; gap: 18px; min-height: 39px; border-top: 1px solid rgb(36 60 88 / 12%); color: var(--site-muted); font-size: 11px; letter-spacing: .03em; }
.utility-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.utility-time { font-variant-numeric: tabular-nums; }
.utility-date { font-variant-numeric: tabular-nums; }
.utility-divider { width: 1px; height: 11px; background: var(--site-line); }
.utility-countdown { display: flex; gap: 9px; align-items: center; min-width: 0; background: transparent; border: 0; color: inherit; padding: 0; font-size: inherit; }
.countdown-error { color: #a12d40 !important; }
.nav-dialog :disabled { cursor: default; opacity: .55; }
.utility-countdown > span { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.utility-countdown strong { font-weight: 500; font-variant-numeric: tabular-nums; white-space: nowrap; }
button.utility-countdown:hover { color: #182e4a; text-decoration: underline; }
.utility-weather { display: flex; align-items: center; gap: 6px; white-space: nowrap; }
.nav-dialog { margin: auto; padding: 0; width: min(400px, calc(100% - 40px)); border: 1px solid var(--site-line); border-radius: 8px; color: var(--site-ink); background: #fff; font-family: var(--font-sans); }
.nav-dialog::backdrop { background: rgb(24 43 67 / 45%); }
.nav-dialog-content { padding: 30px; }
.nav-dialog h2 { font-size: 21px; font-weight: 600; margin: 0 0 16px; }
.nav-dialog p { font-size: 14px; line-height: 1.8; color: var(--site-muted); }
.nav-dialog label { display: block; font-size: 12px; margin: 18px 0 8px; }
.nav-dialog input { display: block; width: 100%; background: #f4f7fc; border: 1px solid #8ba7ca; border-radius: 4px; padding: 10px 12px; color: var(--site-ink); }
.dialog-actions { display: flex; gap: 12px; margin-top: 26px; justify-content: flex-end; }
.dialog-actions button { border-radius: 4px; padding: 10px 16px; font-size: 13px; }
.dialog-primary { background: var(--site-accent); color: #fff; border: 1px solid var(--site-accent); }
.dialog-secondary { background: transparent; color: var(--site-ink); border: 1px solid #7896ba; }
@media (max-width: 1000px) { .nav-main { grid-template-columns: 135px 1fr 155px; gap: 12px; } .nav-links { gap: 4px; } .nav-links a { font-size: 16px; padding: 0 8px; } .nav-actions { gap: 15px; } }
@media (max-width: 900px) { .nav-main, .nav-utilities { width: calc(100% - 64px); } }
@media (max-width: 760px) {
  .nav-main, .nav-utilities { width: calc(100% - 40px); }
  .nav-main { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 0; padding-top: 13px; }
  .nav-brand { font-size: 21px; }
  .nav-actions { min-height: 36px; }
  .nav-links { order: 3; flex: 0 0 100%; justify-content: space-between; gap: 4px; overflow-x: auto; margin-top: 8px; }
  .nav-links a { font-size: 14px; min-height: 44px; padding: 0 6px; }
  .nav-utilities { min-height: 36px; font-size: 10px; gap: 8px; }
  .utility-left { gap: 8px; }
  .utility-countdown > span { max-width: 85px; }
  .utility-weather { letter-spacing: 0; }
}
@media (max-width: 370px) { .utility-date { display: none; } }
</style>
