<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { articleUi } from '../locales/articles'
import { siteLanguage } from '../services/homepage'
import { articleChanged, articlePayload, DRAFT_KEY, emptyArticle, parseArticleDraft, plainExcerpt } from '../utils/articles'
import type { Article, ArticleDraft } from '../utils/articles'
import '../styles/life.css'

const router = useRouter()
const route = useRoute()
const { locale } = useI18n()
const copy = computed(() => articleUi[siteLanguage(locale.value)])
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const isEditMode = computed(() => route.query.id !== undefined)
const form = reactive<ArticleDraft>(emptyArticle())
const baseline = ref<ArticleDraft>(emptyArticle())
const isModified = computed(() => articleChanged(form, baseline.value))
const loading = ref(true)
const loadError = ref('')
const error = ref('')
const notice = ref('')
const isSubmitting = ref(false)
const uploadedImages = ref(0)
const saveSucceeded = ref(false)
const draftReady = ref(false)
const draftUnavailable = ref(false)
const pendingDraft = ref<ArticleDraft | null>(null)
const draftDialog = ref<HTMLDialogElement | null>(null)
const leaveDialog = ref<HTMLDialogElement | null>(null)
const successDialog = ref<HTMLDialogElement | null>(null)
let leaveResolver: ((leave: boolean) => void) | null = null
let returnTimer: ReturnType<typeof setTimeout> | undefined
const categoryOptions = computed(() => [...new Set(['技术', '生活', '杂谈', '其他', form.category])])
const busy = computed(() => isSubmitting.value || uploadedImages.value > 0)
const draftStatus = computed(() => !isEditMode.value && draftReady.value && isModified.value ? draftUnavailable.value ? copy.value.draftUnavailable : copy.value.draftSaved : '')

function categoryLabel(value: string) {
  const labels: Record<string, string> = { '技术': copy.value.categoryTech, '生活': copy.value.categoryLife, '杂谈': copy.value.categoryThoughts, '其他': copy.value.categoryOther }
  return labels[value] || value
}
function report(cause: unknown, fallback: string) {
  return axios.isAxiosError(cause) && [401, 403].includes(cause.response?.status || 0) ? copy.value.authFailed : fallback
}
function persistDraft() {
  if (!isAdmin.value || isEditMode.value || loading.value || !draftReady.value || saveSucceeded.value) return
  try {
    if (isModified.value) localStorage.setItem(DRAFT_KEY, JSON.stringify({ ...form, time: Date.now() }))
    else localStorage.removeItem(DRAFT_KEY)
    draftUnavailable.value = false
  } catch { draftUnavailable.value = true }
}
watch(() => ({ ...form }), persistDraft)
async function loadData() {
  if (!isAdmin.value) { loading.value = false; return }
  loading.value = true
  draftReady.value = false
  saveSucceeded.value = false
  loadError.value = ''
  error.value = ''
  notice.value = ''
  Object.assign(form, emptyArticle())
  baseline.value = emptyArticle()
  try {
    if (isEditMode.value) {
      const id = route.query.id
      if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id)) throw new Error('Invalid article id')
      const article = (await axios.get<Article>(`/api/articles/${id}`)).data
      const loaded = { title: article.title, content: article.content || '', category: article.category || '技术' }
      Object.assign(form, loaded)
      baseline.value = { ...loaded }
      draftReady.value = true
    } else {
      try { pendingDraft.value = parseArticleDraft(localStorage.getItem(DRAFT_KEY)) }
      catch { draftUnavailable.value = true }
      if (pendingDraft.value) draftDialog.value?.showModal()
      else draftReady.value = true
    }
  } catch (cause) { loadError.value = report(cause, copy.value.loadFailed) }
  finally { loading.value = false }
}
function restoreDraft() {
  if (pendingDraft.value) Object.assign(form, pendingDraft.value)
  pendingDraft.value = null
  draftReady.value = true
  draftDialog.value?.close()
  notice.value = copy.value.draftRestored
  persistDraft()
}
function startFresh() {
  try { localStorage.removeItem(DRAFT_KEY) } catch { draftUnavailable.value = true }
  pendingDraft.value = null
  draftReady.value = true
  draftDialog.value?.close()
}
function cancelDraft() { draftDialog.value?.close(); void router.push('/articles') }
function keepEditing() {
  const resolve = leaveResolver
  leaveResolver = null
  leaveDialog.value?.close()
  resolve?.(false)
}
function confirmLeave() {
  persistDraft()
  const resolve = leaveResolver
  leaveResolver = null
  leaveDialog.value?.close()
  resolve?.(true)
}
function requestLeave(): boolean | Promise<boolean> {
  if (busy.value) { error.value = copy.value.waitForSave; return false }
  if (saveSucceeded.value || !isModified.value) return true
  persistDraft()
  leaveResolver?.(false)
  return new Promise(resolve => {
    leaveResolver = resolve
    if (!leaveDialog.value?.open) leaveDialog.value?.showModal()
  })
}
onBeforeRouteLeave(requestLeave)
onBeforeRouteUpdate(requestLeave)
watch(() => route.query.id, () => { void loadData() })
function handleBeforeUnload(event: BeforeUnloadEvent) {
  persistDraft()
  if ((isModified.value && !saveSucceeded.value) || busy.value) { event.preventDefault(); event.returnValue = '' }
}
async function onUploadImg(files: File[], callback: (urls: string[]) => void) {
  if (!isAdmin.value) return
  uploadedImages.value += files.length
  error.value = ''
  try {
    const urls = await Promise.all(files.map(async file => {
      const body = new FormData()
      body.append('image', file)
      const result = await axios.post<string>('/api/upload', body, { responseType: 'text' })
      const url = result.data.trim()
      if (!/^https?:\/\//i.test(url) && !url.startsWith('/uploads/')) throw new Error('Invalid upload response')
      return url
    }))
    callback(urls)
  } catch (cause) { error.value = report(cause, copy.value.uploadFailed); callback([]) }
  finally { uploadedImages.value -= files.length }
}
async function saveArticle() {
  if (!isAdmin.value || loading.value || !draftReady.value || loadError.value || busy.value || saveSucceeded.value) return
  if (!form.title.trim() || !form.content.trim()) { error.value = copy.value.required; return }
  isSubmitting.value = true
  error.value = ''
  const saved = { ...form }
  try {
    const payload = articlePayload(saved)
    if (isEditMode.value) await axios.put(`/api/articles/${route.query.id}`, payload)
    else await axios.post('/api/articles', payload)
    baseline.value = saved
    saveSucceeded.value = true
    if (!isEditMode.value) { try { localStorage.removeItem(DRAFT_KEY) } catch { /* Publishing succeeded even when local storage is unavailable. */ } }
    successDialog.value?.showModal()
    returnTimer = setTimeout(() => { void router.push('/articles') }, 1000)
  } catch (cause) { error.value = report(cause, copy.value.saveFailed) }
  finally { isSubmitting.value = false }
}
onMounted(() => { void loadData(); window.addEventListener('beforeunload', handleBeforeUnload) })
onUnmounted(() => {
  persistDraft()
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (returnTimer) clearTimeout(returnTimer)
  leaveResolver?.(false)
  leaveResolver = null
})
</script>

<template>
  <div class="life-scene">
    <FixedSiteBackground />
    <div class="life-page editor-page">
      <router-link to="/articles" class="editor-back"><span aria-hidden="true">←</span>{{ copy.back }}</router-link>
      <header class="life-header editor-heading"><div><p class="life-eyebrow">WRITE A LITTLE</p><h1 class="life-title">{{ isEditMode ? copy.editorEdit : copy.editorNew }}</h1></div></header>
      <div v-if="!isAdmin" class="life-state"><p>{{ copy.adminOnly }}</p><router-link to="/login" class="life-action">{{ copy.login }}</router-link></div>
      <p v-else-if="loading" class="life-state" role="status">{{ copy.loading }}</p>
      <div v-else-if="loadError" class="life-state" role="alert"><p>{{ loadError }}</p><button type="button" class="life-action" @click="loadData">{{ copy.retry }}</button></div>
      <template v-else>
        <p v-if="error" class="editor-error" role="alert">{{ error }}</p>
        <p v-if="notice" class="editor-notice" role="status">{{ notice }}</p>
        <form class="editor-fields" @submit.prevent="saveArticle">
          <div class="editor-category"><label for="article-category">{{ copy.category }}</label><select id="article-category" v-model="form.category" :disabled="busy || !draftReady || saveSucceeded"><option v-for="value in categoryOptions" :key="value" :value="value">{{ categoryLabel(value) }}</option></select></div>
          <div class="editor-title"><label for="article-title">{{ copy.titleLabel }}</label><input id="article-title" v-model="form.title" :placeholder="copy.titlePlaceholder" :disabled="busy || !draftReady || saveSucceeded" required /></div>
          <button type="submit" class="life-action editor-publish" :disabled="busy || !draftReady || saveSucceeded">{{ isSubmitting ? copy.saving : uploadedImages ? copy.uploading : isEditMode ? copy.save : copy.publish }}</button>
        </form>
        <div class="editor-paper" :inert="isSubmitting || !draftReady || saveSucceeded">
          <MdEditor v-model="form.content" theme="light" :language="locale === 'zh' ? 'zh-CN' : 'en-US'" :placeholder="copy.bodyPlaceholder" class="life-markdown-editor" @onUploadImg="onUploadImg" @onSave="saveArticle" />
        </div>
        <p v-if="draftStatus" class="editor-draft-status" role="status">{{ draftStatus }}</p>
      </template>
    </div>
    <Teleport to="body">
      <dialog ref="leaveDialog" class="life-dialog" aria-labelledby="editor-leave-title" @cancel.prevent="keepEditing" @click.self="keepEditing" @close="leaveResolver && keepEditing()">
        <div class="life-dialog-content"><header class="life-dialog-header"><h2 id="editor-leave-title">{{ copy.leaveTitle }}</h2></header><p>{{ copy.leaveHint }}</p><p class="life-dialog-note">{{ isEditMode ? copy.leaveEditHint : draftUnavailable ? copy.draftUnavailable : copy.leaveNewHint }}</p><footer class="life-dialog-actions"><button type="button" @click="confirmLeave">{{ copy.leave }}</button><button type="button" class="life-dialog-primary" autofocus @click="keepEditing">{{ copy.keepEditing }}</button></footer></div>
      </dialog>
      <dialog ref="draftDialog" class="life-dialog" aria-labelledby="editor-draft-title" @cancel.prevent="cancelDraft">
        <div class="life-dialog-content"><header class="life-dialog-header"><h2 id="editor-draft-title">{{ copy.draftTitle }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" @click="cancelDraft">×</button></header><p class="life-dialog-note">{{ copy.draftHint }}</p><div class="draft-preview"><strong>{{ pendingDraft?.title }}</strong><p>{{ plainExcerpt(pendingDraft?.content, 120) }}</p></div><footer class="life-dialog-actions"><button type="button" @click="startFresh">{{ copy.startFresh }}</button><button type="button" class="life-dialog-primary" @click="restoreDraft">{{ copy.restore }}</button></footer></div>
      </dialog>
      <dialog ref="successDialog" class="life-dialog" aria-labelledby="editor-success-title"><div class="life-dialog-content"><h2 id="editor-success-title">{{ isEditMode ? copy.updated : copy.published }}</h2><p class="life-dialog-note" role="status">{{ copy.returning }}</p></div></dialog>
    </Teleport>
  </div>
</template>

<style scoped>
.editor-page { width: min(1280px, calc(100% - 96px)); }
.editor-back { display: inline-flex; align-items: center; gap: 12px; font-size: 12px; color: var(--site-muted); text-decoration: none; margin-bottom: 28px; }
.editor-back:hover { color: #fff; }
.editor-heading { margin-bottom: 32px; }
.editor-heading .life-title { font-size: clamp(28px, 3vw, 40px); }
.editor-fields { display: grid; grid-template-columns: 160px minmax(0, 1fr) auto; align-items: end; gap: 20px; margin: 0 0 22px; }
.editor-fields label { display: block; font-size: 12px; color: var(--site-muted); margin-bottom: 9px; }
.editor-fields input, .editor-fields select { --site-focus: #2857a5; display: block; width: 100%; min-width: 0; background: #fff; color: #214777; border: 1px solid #d4deec; border-radius: 4px; padding: 11px 14px; min-height: 47px; font-size: 15px; }
.editor-fields input::placeholder { color: #607089; font-size: 14px; }
.editor-fields .editor-publish { background: #fff; color: #2857a5; border-color: #fff; min-height: 47px; }
.editor-paper { --site-focus: #2857a5; border: 1px solid rgb(255 255 255 / 55%); border-radius: 5px; overflow: hidden; background: #fff; color: #214777; }
.life-markdown-editor { height: max(560px, 68vh); }
.editor-paper :deep(.md-editor) { --md-color: #214777; --md-hover-color: #2857a5; --md-bk-color: #fff; --md-border-color: #dce5f1; font-family: var(--font-sans); }
.editor-paper :deep(.md-editor-preview) { color: #214777; }
.editor-error { padding: 12px 16px; color: #9b332e; background: #fff0ef; border-radius: 4px; margin: 0 0 20px; font-size: 13px; line-height: 1.8; }
.editor-notice { margin: 0 0 20px; font-size: 12px; color: var(--site-muted); }
.editor-draft-status { margin: 14px 0 0; font-size: 11px; line-height: 1.8; color: var(--site-muted); }
.draft-preview { padding: 12px 14px; border-left: 2px solid #b4c8e5; background: #f6f8fc; margin: 20px 0; }
.draft-preview strong { font-size: 14px; font-weight: 500; }
.draft-preview p { margin: 8px 0 0; color: #4a6382; font-size: 12px; }
@media (max-width: 900px) { .editor-page { width: calc(100% - 64px); } }
@media (max-width: 640px) {
  .editor-page { width: calc(100% - 40px); }
  .editor-fields { grid-template-columns: minmax(0, 1fr) auto; gap: 16px; }
  .editor-title { grid-column: 1 / -1; grid-row: 1; }
  .editor-category { grid-row: 2; }
  .editor-publish { grid-row: 2; }
  .editor-fields input, .editor-fields select { font-size: 14px; }
  .life-markdown-editor { height: max(520px, 68vh); }
}
</style>
