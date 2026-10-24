<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import HomeContentEditor from "../components/HomeContentEditor.vue";
import { homeUi } from "../locales/homepage";
import { getHomeContent, getRandomQuote, siteLanguage, songLink } from "../services/homepage";
import type { EditorSection, HomeContent, Quote } from "../services/homepage";

const { t, locale } = useI18n();
const copy = computed(() => homeUi[siteLanguage(locale.value)]);
const isAdmin = computed(() => localStorage.getItem("user_role") === "ROLE_ADMIN");
const editor = ref<InstanceType<typeof HomeContentEditor> | null>(null);
const content = ref<HomeContent>({ translations: {}, listening: null });
const quote = ref<Quote | null>(null);
const loadFailed = ref(false);
const translated = computed(() => content.value.translations[siteLanguage(locale.value)] ?? {
  desc1: t('home.desc1'), desc2: t('home.desc2'),
  contactPublic: t('home.contactPublic'), contactProf: t('home.contactProf'),
});
const listeningUrl = computed(() => songLink(content.value.listening?.url));
let quoteRequest = 0;
async function loadContent() {
  try { content.value = await getHomeContent(); loadFailed.value = false; }
  catch { loadFailed.value = true; }
}
async function loadQuote() {
  const request = ++quoteRequest;
  quote.value = null;
  try {
    const selected = await getRandomQuote(siteLanguage(locale.value));
    if (request === quoteRequest) quote.value = selected;
  } catch { /* Optional content stays hidden when unavailable. */ }
}
function openEditor(section: EditorSection) { void editor.value?.open(section); }
async function onContentUpdated(section: EditorSection) {
  if (section === 'quotes') await loadQuote();
  else await loadContent();
}
watch(locale, () => { void loadQuote(); }, { immediate: true });
onMounted(loadContent);
</script>

<template>
  <div class="home-scene">
    <svg
      class="home-linework home-linework--upper"
      viewBox="0 0 720 640"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M60 -80C30 215 220 440 760 460" />
      <path d="M120 -80C90 180 260 380 760 400" />
      <path d="M180 -80C150 145 300 320 760 340" />
      <path d="M240 -80C210 110 340 260 760 280" />
    </svg>
    <svg
      class="home-linework home-linework--lower"
      viewBox="0 0 720 640"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <g transform="translate(720 640) rotate(180)">
        <path d="M120 -80C90 180 260 380 760 400" />
        <path d="M180 -80C150 145 300 320 760 340" />
        <path d="M240 -80C210 110 340 260 760 280" />
      </g>
    </svg>
    <svg class="home-note home-note--small" viewBox="0 0 48 64" fill="none" aria-hidden="true" focusable="false">
      <path d="M30 44V10C31 18 41 19 41 28" /><ellipse cx="22" cy="47" rx="8" ry="6" transform="rotate(-18 22 47)" />
    </svg>
    <svg class="home-note home-note--large" viewBox="0 0 64 64" fill="none" aria-hidden="true" focusable="false">
      <path d="M22 47V18L50 10V39M22 25L50 17" /><ellipse cx="15" cy="49" rx="7" ry="5" transform="rotate(-18 15 49)" /><ellipse cx="43" cy="41" rx="7" ry="5" transform="rotate(-18 43 41)" />
    </svg>
    <div class="home-page">
      <div v-if="isAdmin" class="home-admin-tools">
        <button type="button" @click="openEditor('profile')">{{ copy.edit }}</button>
        <button type="button" @click="openEditor('quotes')">{{ copy.manageQuotes }}</button>
        <p v-if="loadFailed" role="status">{{ copy.loadFailed }}</p>
      </div>
      <section
        class="home-hero"
        aria-labelledby="home-title"
      >
        <div class="home-introduction">
          <p class="home-eyebrow">SHIMIZU KAEDE</p>
          <h1 id="home-title">{{ t('home.greeting') }}</h1>
          <p class="home-intro">{{ t('home.intro') }}</p>
          <div class="home-description">
            <p v-if="translated.desc1">{{ translated.desc1 }}</p>
            <p v-if="translated.desc2">{{ translated.desc2 }}</p>
          </div>
          <div class="home-discovery" :class="{ 'home-discovery--with-quote': quote }">
            <blockquote v-if="quote" class="home-quote">
              <span class="home-quote-mark" aria-hidden="true">“</span>
              <p>{{ quote.content }}</p>
              <cite v-if="quote.source">{{ copy.quoteAttribution }} · {{ quote.source }}</cite>
            </blockquote>
            <div class="home-links">
              <router-link to="/career">{{ t('nav.career') }}<span aria-hidden="true">↗</span></router-link>
              <router-link to="/projects">{{ t('nav.projects') }}<span aria-hidden="true">↗</span></router-link>
            </div>
          </div>
        </div>
        <div class="home-side">
          <figure class="home-identity">
          <div class="home-logo-field">
            <img
              src="/logo.png"
              alt="Kaede"
              class="home-logo"
              width="2048"
              height="1205"
              fetchpriority="high"
            />
          </div>
          <figcaption>{{ t('home.logoCredit') }}</figcaption>
          </figure>
          <section v-if="(content.listening && listeningUrl) || isAdmin" class="home-listening" aria-labelledby="listening-heading">
            <div class="home-listening-heading"><h2 id="listening-heading">{{ copy.listening }}</h2><button v-if="isAdmin" type="button" @click="openEditor('listening')">{{ copy.update }}</button></div>
            <a v-if="content.listening && listeningUrl" :href="listeningUrl" target="_blank" rel="noopener noreferrer" class="home-song" :aria-label="`${content.listening.title} — ${content.listening.artist} · ${copy.openSong}`">
              <svg class="home-song-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false"><path d="M12 3v10.55A3.97 3.97 0 0 0 10 13a4 4 0 1 0 4 4V7h4V3h-6Z" /></svg>
              <span class="home-song-copy"><strong>{{ content.listening.title }}</strong><span class="home-song-artist">{{ content.listening.artist }}</span></span><span aria-hidden="true">↗</span>
            </a>
            <p v-else class="home-listening-empty">{{ copy.noListening }}</p>
          </section>
        </div>
      </section>
      <section
        class="home-contact"
        aria-labelledby="contact-title"
      >
        <h2 id="contact-title">{{ t('about.contactTitle') }}<span aria-hidden="true">↗</span></h2>
        <div class="home-contact-copy">
            <p v-if="translated.contactPublic">{{ translated.contactPublic }}</p>
            <p v-if="translated.contactProf" class="home-contact-note">{{ translated.contactProf }}</p>
        </div>
      </section>
    </div>
    <HomeContentEditor v-if="isAdmin" ref="editor" @updated="onContentUpdated" />
  </div>
</template>

<style scoped>
.home-scene {
  position: relative;
  isolation: isolate;
  min-height: calc(100svh - 118px);
  overflow: hidden;
}
.home-note { position: absolute; z-index: -1; pointer-events: none; stroke: #fff; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
.home-note--small { width: 30px; height: 40px; left: 6%; top: 34%; opacity: .3; transform: rotate(-14deg); }
.home-note--large { width: 52px; height: 52px; right: 7%; bottom: 14%; opacity: .23; transform: rotate(12deg); }
.home-admin-tools { display: flex; align-items: center; gap: 18px; flex-wrap: wrap; margin-bottom: 20px; }
.home-admin-tools button, .home-listening-heading button { background: transparent; color: #fff; border: 0; border-bottom: 1px solid rgb(255 255 255 / 45%); padding: 5px 0; font-size: 12px; }
.home-admin-tools p { margin: 0; font-size: 12px; }
.home-discovery { display: flow-root; margin-top: 32px; }
.home-discovery--with-quote { margin-top: 66px; }
.home-quote { position: relative; margin: 0 0 38px; padding-left: 34px; max-width: 490px; border-left: 1px solid rgb(255 255 255 / 40%); }
.home-quote-mark { position: absolute; left: 12px; top: -6px; color: var(--site-muted); font-family: Georgia, serif; font-size: 28px; line-height: 1.4; opacity: .75; }
.home-quote p { margin: 0; font-size: 14px; line-height: 1.95; white-space: pre-wrap; overflow-wrap: anywhere; }
.home-quote cite { display: block; margin-top: 10px; font-size: 12px; font-style: normal; color: var(--site-muted); }
.home-listening { max-width: 390px; margin: 28px 0 0 auto; padding-top: 18px; border-top: 1px solid rgb(255 255 255 / 24%); }
.home-listening-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.home-listening h2 { margin: 0; font-size: 11px; letter-spacing: .08em; font-weight: 500; color: var(--site-muted); }
.home-listening-heading button { font-size: 11px; }
.home-song { display: grid; grid-template-columns: 22px minmax(0, 1fr) 16px; align-items: center; gap: 14px; margin-top: 12px; color: #fff; text-decoration: none; }
.home-song-icon { width: 22px; height: 22px; color: #fff; opacity: 1; align-self: start; margin-top: 3px; }
.home-song-copy { min-width: 0; }
.home-song strong { font-size: 15px; font-weight: 500; line-height: 1.8; overflow-wrap: anywhere; }
.home-song-artist { display: block; margin-top: 3px; font-size: 12px; line-height: 1.8; color: var(--site-muted); }
.home-song:hover strong { text-decoration: underline; text-underline-offset: 4px; }
.home-listening-empty { margin: 12px 0 0; font-size: 12px; color: var(--site-muted); }
.home-linework {
  position: absolute;
  z-index: -1;
  pointer-events: none;
  color: #fff;
  stroke: currentColor;
  stroke-width: 1.25;
  opacity: 0.28;
}
.home-linework--upper {
  width: 820px;
  height: auto;
  top: -140px;
  right: -140px;
}
.home-linework--lower {
  width: 820px;
  height: auto;
  bottom: -140px;
  left: -180px;
  opacity: 0.24;
}
.home-page {
  position: relative;
  width: min(1120px, calc(100% - 96px));
  margin: 0 auto;
  padding: 68px 0 48px;
}
.home-hero {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 70px;
  align-items: center;
  min-height: 410px;
  padding: 18px 0 60px;
}
.home-eyebrow {
  margin: 0 0 24px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: var(--site-muted);
}
h1 {
  margin: 0;
  font-size: clamp(32px, 3.8vw, 54px);
  font-weight: 600;
  letter-spacing: -0.055em;
  line-height: 1.3;
  text-wrap: balance;
}
.home-intro {
  margin: 17px 0 30px;
  color: var(--site-muted);
  font-size: 18px;
  letter-spacing: 0.015em;
  line-height: 1.8;
}
.home-description {
  max-width: 490px;
  font-size: 15px;
  line-height: 2;
  color: var(--site-ink);
}
.home-description p {
  margin: 0 0 8px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.home-links {
  display: flex;
  gap: 30px;
}
.home-links a {
  display: inline-flex;
  align-items: center;
  gap: 32px;
  padding: 8px 0;
  border-bottom: 1px solid var(--site-muted);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  color: var(--site-ink);
  transition: color 0.15s;
}
.home-links a:hover {
  color: #fff;
  border-bottom-color: #fff;
}
.home-identity {
  margin: 0;
}
.home-logo-field {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 310px;
  padding: 28px;
}
.home-logo {
  width: 100%;
  height: auto;
  object-fit: contain;
}
.home-identity figcaption {
  margin-top: 15px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--site-muted);
  text-align: right;
}
.home-contact {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 36px;
  border-top: 1px solid var(--site-line);
  padding: 30px 0 16px;
}
.home-contact h2 {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin: 0;
  max-width: 130px;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.8;
}
.home-contact-copy p {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}
.home-contact-copy .home-contact-note {
  margin-top: 8px;
  font-size: 12px;
  color: var(--site-muted);
}
@media (min-width: 1500px) {
  .home-page {
    padding-top: 100px;
  }
  .home-hero {
    min-height: 470px;
  }
}
@media (max-width: 900px) {
  .home-page {
    width: calc(100% - 64px);
    padding-top: 40px;
  }
  .home-hero {
    gap: 32px;
  }
  .home-logo-field {
    min-height: 260px;
    padding: 22px;
  }
  .home-contact {
    grid-template-columns: 150px 1fr;
    gap: 24px;
  }
}
@media (max-width: 640px) {
  .home-note--small { left: 8px; top: 48%; width: 22px; opacity: .18; }
  .home-note--large { right: 10px; bottom: 8%; width: 34px; opacity: .16; }
  .home-discovery { margin-top: 22px; }
  .home-discovery--with-quote { margin-top: 60px; }
  .home-quote { margin-bottom: 32px; }
  .home-listening { max-width: none; margin-top: 24px; }
  .home-scene {
    min-height: calc(100svh - 138px);
  }
  .home-linework--upper {
    width: 560px;
    right: -240px;
    top: -120px;
    opacity: 0.22;
  }
  .home-linework--lower {
    width: 560px;
    left: -260px;
    bottom: -160px;
    opacity: 0.2;
  }
  .home-page {
    width: calc(100% - 40px);
    padding-top: 34px;
  }
  .home-hero {
    grid-template-columns: 1fr;
    gap: 34px;
    padding: 0 0 32px;
  }
  .home-eyebrow {
    margin-bottom: 18px;
  }
  h1 {
    font-size: 36px;
  }
  .home-intro {
    margin: 12px 0 22px;
    font-size: 16px;
  }
  .home-description {
    font-size: 14px;
  }
  .home-logo-field {
    min-height: 220px;
    padding: 26px 34px;
  }
  .home-contact {
    grid-template-columns: 1fr;
    gap: 16px;
    padding-top: 24px;
  }
}
</style>
