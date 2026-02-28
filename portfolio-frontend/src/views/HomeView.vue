<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-[calc(100vh-300px)] text-center px-4 -mt-10">
    <div class="flex flex-col items-center ...">
      <img
        src="/logo.png"
        alt="My Logo"
        class="w-lg h-lg object-contain ..."
      />
    </div>
    <h1 class="text-5xl font-extrabold mb-6 bg-linear-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
      {{ t('home.greeting') }}
    </h1>
    <p class="text-xl text-slate-300 mb-3 max-w-3xl leading-relaxed">
      {{ t('home.intro') }}
    </p>
    <p class="text-m text-slate-400 max-w-3xl leading-relaxed">
      {{ t('home.desc1') }}
    </p>
    <p class="text-m text-slate-400 max-w-3xl leading-relaxed">
      {{ t('home.desc2') }}
    </p>
    <p class="text-m text-slate-400 max-w-3xl leading-relaxed">
      {{ t('home.contactPublic') }}
    </p>
    <p class="text-m text-slate-400 max-w-6xl leading-relaxed">
      {{ t('home.contactProf') }}
    </p>
    <p class="text-xs text-slate-400 max-w-3xl leading-relaxed mt-4">
      {{ t('home.logoCredit') }}
    </p>
  </div>
</template>