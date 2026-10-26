<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Navbar from './components/Navbar.vue'
const route = useRoute()
const usesLegacyLayout = computed(() => !['/', '/career', '/articles', '/editor', '/projects', '/about', '/login', '/premium-admin'].includes(route.path) && !route.path.startsWith('/article/') && !route.path.startsWith('/projects/') && !route.path.startsWith('/exclusive-player/'))
const { locale } = useI18n()
const skipLabels: Record<string, string> = { zh: '跳至正文', en: 'Skip to content', ja: '本文へ移動' }
</script>

<template>
  <div class="site-shell" :class="{ 'site-shell--legacy': usesLegacyLayout }">
    <a class="skip-link" href="#main-content">{{ skipLabels[locale] }}</a>
    <Navbar />

    <main id="main-content" :class="{ 'site-main--legacy': usesLegacyLayout }" tabindex="-1">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style>
.site-shell { min-height: 100vh; background: var(--site-blue); color: var(--site-ink); font-family: var(--font-sans); }
/* Keep existing pages legible until their individual visual refresh. */
.site-shell--legacy { background: #172a43; color: #f1f5f9; }
.site-main--legacy { padding: 48px 16px; }
.skip-link { position: fixed; left: 16px; top: -100px; z-index: 200; padding: 12px 20px; background: #eef3f9; color: #243c58; border-radius: 4px; }
.skip-link:focus { top: 12px; }
.page-fade-enter-active, .page-fade-leave-active { transition: opacity .15s ease; }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; }
</style>
