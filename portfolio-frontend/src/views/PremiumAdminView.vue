<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { premiumUi } from '../locales/premium'
import { getPremiumList, publishPremiumAudio, uploadPremiumAudio } from '../services/premium'
import type { PremiumAudio } from '../services/premium'
import { premiumAudioUrl } from '../utils/premium'
import '../styles/life.css'
import '../styles/premium.css'

const { locale } = useI18n()
const copy = computed(() => premiumUi[locale.value === 'en' ? 'en' : locale.value === 'ja' ? 'ja' : 'zh'])
const isAdmin = computed(() => !!localStorage.getItem('token') && localStorage.getItem('user_role') === 'ROLE_ADMIN')
const audioList = ref<PremiumAudio[]>([])
const loading = ref(true)
const loadError = ref('')
const error = ref('')
const uploading = ref(false)
const publishing = ref(false)
const uploaded = ref(false)
const busy = computed(() => uploading.value || publishing.value)
const form = ref({ title: '', audioUrl: '' })
const dirty = computed(() => !!form.value.title || !!form.value.audioUrl)
const editorOpen = ref(false)
const editor = ref<HTMLDialogElement | null>(null)
const discardDialog = ref<HTMLDialogElement | null>(null)
const noticeDialog = ref<HTMLDialogElement | null>(null)
const notice = ref({ title: '', content: '', link: '' })
let discardResolver: ((value: boolean) => void) | null = null
let disposed = false

function report(cause: unknown, fallback: string) {
  const status = axios.isAxiosError(cause) ? cause.response?.status : undefined
  if (status === 401 || status === 403) return copy.value.authFailed
  if (status === 404) return copy.value.backendUnavailable
  return fallback
}
async function fetchList() {
  if (!isAdmin.value) { loading.value = false; return }
  loading.value = true
  loadError.value = ''
  try {
    const values = await getPremiumList()
    if (!disposed) audioList.value = [...values].sort((a, b) => b.id - a.id)
  } catch (cause) {
    if (!disposed) loadError.value = report(cause, copy.value.listFailed)
  } finally { if (!disposed) loading.value = false }
}
function openEditor() {
  form.value = { title: '', audioUrl: '' }
  error.value = ''
  uploaded.value = false
  editorOpen.value = true
  editor.value?.showModal()
}
function settleDiscard(value: boolean) {
  const resolve = discardResolver
  discardResolver = null
  discardDialog.value?.close()
  resolve?.(value)
}
function mayLeave(): boolean | Promise<boolean> {
  if (!editorOpen.value) return true
  if (busy.value) { error.value = copy.value.wait; return false }
  if (!dirty.value) return true
  if (discardResolver) return false
  return new Promise(resolve => {
    discardResolver = resolve
    discardDialog.value?.showModal()
  })
}
async function closeEditor() {
  if (await mayLeave()) editor.value?.close()
}
onBeforeRouteLeave(mayLeave)
function beforeUnload(event: BeforeUnloadEvent) {
  if (editorOpen.value && (dirty.value || busy.value)) { event.preventDefault(); event.returnValue = '' }
}
function showNotice(title: string, content: string, link = '') {
  notice.value = { title, content, link }
  noticeDialog.value?.showModal()
}
async function handleUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || busy.value) return
  uploading.value = true
  uploaded.value = false
  error.value = ''
  try {
    const value = await uploadPremiumAudio(file)
    if (disposed) return
    const url = premiumAudioUrl(value)
    if (!url) throw new Error('Invalid uploaded audio URL')
    form.value.audioUrl = url
    uploaded.value = true
  } catch (cause) {
    if (!disposed) error.value = report(cause, copy.value.uploadFailed)
  } finally {
    if (!disposed) uploading.value = false
    input.value = ''
  }
}
async function submit() {
  if (busy.value) return
  error.value = ''
  const url = premiumAudioUrl(form.value.audioUrl)
  if (!form.value.title.trim() || !url) { error.value = copy.value.invalid; return }
  publishing.value = true
  try {
    await publishPremiumAudio({ title: form.value.title, audioUrl: url })
    if (disposed) return
    editor.value?.close()
    editorOpen.value = false
    form.value = { title: '', audioUrl: '' }
    showNotice(copy.value.published, copy.value.publishedNote)
    await fetchList()
  } catch (cause) {
    if (!disposed) error.value = report(cause, copy.value.publishFailed)
  } finally { if (!disposed) publishing.value = false }
}
function playerLink(uuid: string) { return '/exclusive-player/' + encodeURIComponent(uuid) }
async function copyLink(uuid: string) {
  const url = new URL(playerLink(uuid), window.location.origin).href
  try {
    await navigator.clipboard.writeText(url)
    if (!disposed) showNotice(copy.value.copied, copy.value.copiedNote)
  } catch {
    if (!disposed) showNotice(copy.value.copy, copy.value.copyFailed, url)
  }
}
function displayDate(value: string | null) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isFinite(date.getTime()) ? new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }).format(date) : ''
}
onMounted(() => { void fetchList(); window.addEventListener('beforeunload', beforeUnload) })
onBeforeUnmount(() => {
  disposed = true
  window.removeEventListener('beforeunload', beforeUnload)
  settleDiscard(false)
  editor.value?.close()
  noticeDialog.value?.close()
})
</script>

<template>
  <section class="life-scene premium-scene">
    <FixedSiteBackground />
    <div class="life-page premium-page">
      <header class="life-header">
        <div>
          <p class="life-eyebrow">PRIVATE AUDIO</p>
          <h1 class="life-title">{{ copy.adminTitle }}</h1>
          <p class="life-subtitle">{{ copy.adminIntro }}</p>
        </div>
        <button v-if="isAdmin" type="button" class="life-action" @click="openEditor"><span aria-hidden="true">＋</span>{{ copy.publish }}</button>
      </header>
      <div v-if="!isAdmin" class="life-state"><p>{{ copy.adminOnly }}</p><RouterLink class="life-action" to="/login">{{ copy.login }}</RouterLink></div>
      <p v-else-if="loading" class="life-state" role="status">{{ copy.loading }}</p>
      <div v-else-if="loadError" class="life-state" role="alert"><p>{{ loadError }}</p><button class="life-action" type="button" @click="fetchList">{{ copy.retry }}</button></div>
      <p v-else-if="!audioList.length" class="life-state">{{ copy.empty }}</p>
      <ul v-else class="premium-list">
        <li v-for="item in audioList" :key="item.id" class="premium-row">
          <div class="premium-row-note" aria-hidden="true">♪</div>
          <div class="premium-row-content">
            <time v-if="displayDate(item.createAt)" :datetime="item.createAt || undefined">{{ displayDate(item.createAt) }}</time>
            <h2>{{ item.title }}</h2><p class="premium-uuid">{{ item.uuid }}</p>
          </div>
          <div class="premium-row-actions">
            <RouterLink :to="playerLink(item.uuid)" target="_blank" rel="noopener noreferrer" class="life-inline-button">{{ copy.listen }} ↗</RouterLink>
            <button type="button" class="life-action" @click="copyLink(item.uuid)">{{ copy.copy }}</button>
          </div>
        </li>
      </ul>
    </div>
    <Teleport to="body">
      <dialog ref="editor" class="life-dialog premium-editor" aria-labelledby="premium-editor-title" @cancel.prevent="closeEditor" @close="editorOpen = false">
        <form class="life-dialog-content premium-form" @submit.prevent="submit" :aria-busy="busy">
          <div class="life-dialog-header"><h2 id="premium-editor-title">{{ copy.publishTitle }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" :disabled="busy" @click="closeEditor">×</button></div>
          <fieldset :disabled="busy">
            <label for="premium-title">{{ copy.title }}</label>
            <input id="premium-title" v-model="form.title" required maxlength="255" :placeholder="copy.titlePlaceholder" />
            <label for="premium-url">{{ copy.url }}</label>
            <input id="premium-url" v-model="form.audioUrl" required :placeholder="copy.urlPlaceholder" aria-describedby="premium-url-hint" @input="uploaded = false" />
            <p id="premium-url-hint" class="life-dialog-note premium-hint">{{ copy.urlHint }}</p>
            <label class="premium-upload" for="premium-file"><span aria-hidden="true">↥</span> {{ uploading ? copy.uploading : copy.upload }}</label>
            <input id="premium-file" class="premium-file" type="file" accept="audio/*" @change="handleUpload" />
            <p v-if="uploaded" class="premium-uploaded" role="status">{{ copy.uploaded }}</p>
          </fieldset>
          <p v-if="error" class="life-dialog-error" role="alert">{{ error }}</p>
          <div class="life-dialog-actions"><button type="button" :disabled="busy" @click="closeEditor">{{ copy.cancel }}</button><button type="submit" class="life-dialog-primary" :disabled="busy">{{ publishing ? copy.publishing : uploading ? copy.uploading : copy.confirmPublish }}</button></div>
        </form>
      </dialog>
      <dialog ref="discardDialog" class="life-dialog" aria-labelledby="premium-discard-title" @cancel.prevent="settleDiscard(false)" @close="settleDiscard(false)">
        <div class="life-dialog-content"><h2 id="premium-discard-title">{{ copy.discard }}</h2><p class="life-dialog-note">{{ copy.discardNote }}</p><div class="life-dialog-actions"><button type="button" autofocus @click="settleDiscard(false)">{{ copy.keepEditing }}</button><button type="button" class="life-dialog-primary" @click="settleDiscard(true)">{{ copy.discardAction }}</button></div></div>
      </dialog>
      <dialog ref="noticeDialog" class="life-dialog" aria-labelledby="premium-notice-title">
        <div class="life-dialog-content"><div class="life-dialog-header"><h2 id="premium-notice-title">{{ notice.title }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" @click="noticeDialog?.close()">×</button></div><p class="life-dialog-note" role="status">{{ notice.content }}</p><input v-if="notice.link" class="premium-link-copy" readonly :value="notice.link" :aria-label="copy.copy" @focus="($event.target as HTMLInputElement).select()" /><div class="life-dialog-actions"><button type="button" class="life-dialog-primary" @click="noticeDialog?.close()">{{ copy.confirm }}</button></div></div>
      </dialog>
    </Teleport>
  </section>
</template>
