<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { premiumUi } from '../locales/premium'
import { getPremiumAudio } from '../services/premium'
import type { PremiumAudio } from '../services/premium'
import { premiumAudioUrl, premiumUuid } from '../utils/premium'
import '../styles/life.css'
import '../styles/premium.css'

const route = useRoute()
const { locale } = useI18n()
const copy = computed(() => premiumUi[locale.value === 'en' ? 'en' : locale.value === 'ja' ? 'ja' : 'zh'])
const audio = ref<PremiumAudio | null>(null)
const player = ref<HTMLAudioElement | null>(null)
const loading = ref(true)
const failure = ref<'notFound' | 'unavailable' | 'network' | null>(null)
const mediaFailed = ref(false)
const playing = ref(false)
const source = computed(() => premiumAudioUrl(audio.value?.audioUrl))
const errorText = computed(() => failure.value === 'notFound' ? copy.value.notFoundNote
  : failure.value === 'unavailable' ? copy.value.backendUnavailable : copy.value.playFailed)
let request = 0

async function loadAudio() {
  const current = ++request
  player.value?.pause()
  audio.value = null
  failure.value = null
  loading.value = true
  mediaFailed.value = false
  playing.value = false
  const uuid = premiumUuid(route.params.uuid)
  if (!uuid) { failure.value = 'notFound'; loading.value = false; return }
  try {
    const value = await getPremiumAudio(uuid)
    if (current !== request) return
    audio.value = value
    if (!premiumAudioUrl(value.audioUrl)) mediaFailed.value = true
  } catch (cause) {
    if (current !== request) return
    const status = axios.isAxiosError(cause) ? cause.response?.status : undefined
    failure.value = status === 404 ? 'notFound' : status === 401 || status === 403 ? 'unavailable' : 'network'
  } finally { if (current === request) loading.value = false }
}
function retryMedia() {
  mediaFailed.value = !source.value
  if (source.value) player.value?.load()
}
watch(() => route.params.uuid, loadAudio, { immediate: true })
onBeforeUnmount(() => { request++; player.value?.pause() })
</script>

<template>
  <section class="life-scene premium-scene premium-player-scene">
    <FixedSiteBackground />
    <div class="premium-player-layout">
      <div v-if="loading" class="premium-player-state" role="status"><p class="life-eyebrow">PRIVATE AUDIO</p><p>{{ copy.loading }}</p></div>
      <div v-else-if="failure" class="premium-player-state" role="alert">
        <p class="life-eyebrow">PRIVATE AUDIO</p>
        <h1>{{ failure === 'notFound' ? copy.notFound : copy.playerTitle }}</h1><p>{{ errorText }}</p>
        <div class="premium-state-actions"><button type="button" class="life-action" @click="loadAudio">{{ copy.retry }}</button><RouterLink to="/" class="life-inline-button">{{ copy.home }} ↗</RouterLink></div>
      </div>
      <article v-else-if="audio" class="premium-player">
        <div class="premium-record" :class="{ 'premium-record--playing': playing }" aria-hidden="true">
          <div class="premium-record-ring"></div><div class="premium-record-center">♪</div>
        </div>
        <p class="premium-player-eyebrow">PRIVATE AUDIO</p>
        <h1>{{ audio.title }}</h1><p class="premium-player-intro">{{ copy.playerIntro }}</p>
        <audio v-if="source" ref="player" :key="audio.uuid" :src="source" controls preload="metadata" :aria-label="copy.playerLabel"
          @error="mediaFailed = true; playing = false" @playing="playing = true" @pause="playing = false" @ended="playing = false"></audio>
        <div v-if="mediaFailed" class="premium-media-error" role="alert"><p>{{ copy.mediaFailed }}</p><button v-if="source" type="button" @click="retryMedia">{{ copy.retryMedia }}</button></div>
        <RouterLink class="premium-home" to="/">{{ copy.home }} ↗</RouterLink>
      </article>
    </div>
  </section>
</template>
