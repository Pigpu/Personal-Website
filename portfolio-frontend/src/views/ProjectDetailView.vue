<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { projectUi } from '../locales/projects'
import { siteLanguage } from '../services/homepage'
import { attachmentTarget } from '../utils/projects'
import type { Project } from '../utils/projects'
import '../styles/life.css'

interface ProjectLike { id: number; username: string; createdAt: string }
const route = useRoute()
const { t, locale } = useI18n()
const copy = computed(() => projectUi[siteLanguage(locale.value)])
const project = ref<Project | null>(null)
const loading = ref(true)
const error = ref('')
const hasLiked = ref(false)
const likeStatusLoading = ref(false)
const liking = ref(false)
const likesList = ref<ProjectLike[]>([])
const likesError = ref('')
const likesLoading = ref(false)
const showLikesList = ref(false)
const coverFailed = ref(false)
const isLoggedIn = computed(() => !!localStorage.getItem('token'))
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const messageDialog = ref<HTMLDialogElement | null>(null)
const message = ref({ title: '', content: '' })
let requestNumber = 0
let likesRequest = 0
let activeRequest: AbortController | undefined

function showMessage(title: string, content: string) {
  message.value = { title, content }
  if (!messageDialog.value?.open) messageDialog.value?.showModal()
}
function formatDate(value: string | null | undefined, full = false) {
  const date = value ? new Date(value) : null
  if (!date || !Number.isFinite(date.getTime())) return '—'
  const language = locale.value === 'en' ? 'en-GB' : locale.value === 'ja' ? 'ja-JP' : 'zh-CN'
  return full ? date.toLocaleString(language) : date.toLocaleDateString(language)
}
async function fetchLikesList() {
  if (!isAdmin.value || !project.value) return
  const id = project.value.id
  const request = requestNumber
  const listRequest = ++likesRequest
  likesLoading.value = true
  likesError.value = ''
  try {
    const response = await axios.get<ProjectLike[]>('/api/projects/' + id + '/likes-list')
    if (request === requestNumber && listRequest === likesRequest) likesList.value = response.data
  } catch { if (request === requestNumber && listRequest === likesRequest) likesError.value = copy.value.likesLoadFailed }
  finally { if (request === requestNumber && listRequest === likesRequest) likesLoading.value = false }
}
async function fetchProjectDetail() {
  const request = ++requestNumber
  activeRequest?.abort()
  const controller = new AbortController()
  activeRequest = controller
  loading.value = true
  error.value = ''
  project.value = null
  hasLiked.value = false
  likeStatusLoading.value = false
  likesList.value = []
  likesError.value = ''
  likesLoading.value = false
  showLikesList.value = false
  coverFailed.value = false
  try {
    const id = route.params.id
    if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id)) throw new Error('Invalid project id')
    const response = await axios.get<Project>('/api/projects/' + id, { signal: controller.signal })
    if (request !== requestNumber) return
    project.value = response.data
    if (isLoggedIn.value) {
      likeStatusLoading.value = true
      void axios.get<boolean>('/api/projects/' + id + '/like-status', { signal: controller.signal })
        .then(response => { if (request === requestNumber) hasLiked.value = response.data })
        .catch(() => { /* A later like action still reports authentication errors. */ })
        .finally(() => { if (request === requestNumber) likeStatusLoading.value = false })
    }
    if (isAdmin.value) void fetchLikesList()
  } catch (cause) {
    if (request === requestNumber && !axios.isCancel(cause)) error.value = copy.value.loadFailed
  } finally { if (request === requestNumber) loading.value = false }
}
async function handleLike() {
  if (!project.value || liking.value || likeStatusLoading.value) return
  if (!isLoggedIn.value) { showMessage(t('article.msgNeedLoginTitle'), t('article.msgNeedLoginDesc')); return }
  const request = requestNumber
  const id = project.value.id
  liking.value = true
  try {
    const response = await axios.post<{ likeCount: number; isLiked: boolean }>('/api/projects/' + id + '/like', {})
    if (request === requestNumber && project.value) {
      project.value.likeCount = response.data.likeCount
      hasLiked.value = response.data.isLiked
      if (isAdmin.value) void fetchLikesList()
    }
  } catch {
    if (request === requestNumber) showMessage(copy.value.notice, copy.value.likeFailed)
  } finally { liking.value = false }
}
function handleDownload() {
  if (!project.value?.attachmentUrl) return
  // Preserve both user-provided external links and older locally uploaded attachments.
  const target = attachmentTarget(project.value.attachmentUrl)
  if (target.external) window.open(target.url, '_blank', 'noopener,noreferrer')
  else window.location.href = target.url
}
function fileName(value: string) {
  const name = value.split('?')[0]?.split('/').pop() || copy.value.attachment
  try { return decodeURIComponent(name) } catch { return name }
}
watch(() => route.params.id, fetchProjectDetail, { immediate: true })
onUnmounted(() => { requestNumber++; activeRequest?.abort() })
</script>

<template>
  <div class="life-scene">
    <FixedSiteBackground />
    <div class="life-page work-detail-page">
      <header class="work-detail-header"><router-link to="/projects" class="work-back"><span aria-hidden="true">←</span>{{ t('project.backToList') }}</router-link><router-link v-if="isAdmin && project" :to="'/projects/upload?id=' + project.id" class="life-action">{{ copy.edit }}</router-link></header>
      <p v-if="loading" class="life-state" role="status">{{ copy.loading }}</p>
      <div v-else-if="error" class="life-state" role="alert"><p>{{ error }}</p><button type="button" class="life-action" @click="fetchProjectDetail">{{ copy.retry }}</button></div>
      <template v-else-if="project">
        <div v-if="project.mediaType === 'VIDEO'" class="work-video"><video :src="project.mediaUrl || undefined" :poster="project.coverUrl || undefined" controls preload="metadata" :aria-label="project.title"></video></div>
        <div v-else class="work-audio">
          <div class="work-audio-cover"><img v-if="project.coverUrl && !coverFailed" :src="project.coverUrl" :alt="project.title" @error="coverFailed = true" /><svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="M9 17V5l11-2v12M9 9l11-2" /><ellipse cx="5.5" cy="18" rx="3.5" ry="3" /><ellipse cx="16.5" cy="16" rx="3.5" ry="3" /></svg></div>
          <div class="work-audio-controls"><p>{{ copy.audio }}</p><h2>{{ project.title }}</h2><audio :src="project.mediaUrl || undefined" controls preload="metadata" :aria-label="project.title"></audio></div>
        </div>
        <div class="work-detail-grid">
          <article class="work-description-panel">
            <p v-if="project.category" class="work-detail-category">{{ project.category }}</p>
            <h1>{{ project.title }}</h1>
            <div class="work-detail-meta"><time :datetime="project.createdAt || undefined">{{ t('project.publishedOn') }} {{ formatDate(project.createdAt) }}</time><span>{{ project.viewCount || 0 }} {{ t('project.views') }}</span><span>{{ project.likeCount || 0 }} {{ t('project.likes') }}</span></div>
            <p class="work-full-description">{{ project.description }}</p>
          </article>
          <aside class="work-detail-aside">
            <div class="work-like-panel"><button type="button" class="work-like-button" :class="{ 'is-liked': hasLiked }" :aria-pressed="hasLiked" :aria-label="hasLiked ? t('project.cancelLike') : t('project.promptLike')" :disabled="liking || likeStatusLoading" @click="handleLike"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 5a5.5 5.5 0 0 0-8 1 5.5 5.5 0 0 0-8 7l8 8 8-8a5.5 5.5 0 0 0 0-8Z" /></svg><span>{{ project.likeCount || 0 }}</span></button><p>{{ hasLiked ? t('project.cancelLike') : t('project.promptLike') }}</p></div>
            <div v-if="project.attachmentUrl" class="work-attachment-panel"><h2>{{ t('project.attachment') }}</h2><p>{{ fileName(project.attachmentUrl) }}</p><button type="button" class="life-action" @click="handleDownload">{{ t('project.download') }}<span aria-hidden="true">↓</span></button></div>
            <div v-if="isAdmin" class="work-likes-panel">
              <button type="button" class="work-likes-toggle" :aria-expanded="showLikesList" aria-controls="work-likes" @click="showLikesList = !showLikesList"><span>{{ t('project.likeDetails') }} · {{ likesList.length }}</span><span aria-hidden="true">{{ showLikesList ? '−' : '＋' }}</span></button>
              <div v-if="showLikesList" id="work-likes">
                <p v-if="likesLoading" role="status">{{ copy.loading }}</p>
                <div v-else-if="likesError"><p role="alert">{{ likesError }}</p><button type="button" class="life-inline-button" @click="fetchLikesList">{{ copy.retry }}</button></div>
                <p v-else-if="!likesList.length">{{ t('project.noLikes') }}</p>
                <ul v-else><li v-for="like in likesList" :key="like.id"><span>{{ like.username }}</span><time :datetime="like.createdAt">{{ formatDate(like.createdAt, true) }}</time></li></ul>
              </div>
            </div>
          </aside>
        </div>
      </template>
    </div>
    <Teleport to="body"><dialog ref="messageDialog" class="life-dialog" aria-labelledby="work-notice-title" @click.self="messageDialog?.close()"><div class="life-dialog-content"><header class="life-dialog-header"><h2 id="work-notice-title">{{ message.title }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" @click="messageDialog?.close()">×</button></header><p>{{ message.content }}</p><footer class="life-dialog-actions"><button type="button" class="life-dialog-primary" @click="messageDialog?.close()">{{ copy.close }}</button></footer></div></dialog></Teleport>
  </div>
</template>

<style scoped>
.work-detail-page { max-width: 1040px; }
.work-detail-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 32px; }
.work-back { display: inline-flex; align-items: center; gap: 12px; color: #e0e9f8; font-size: 12px; text-decoration: none; }
.work-back:hover { color: #fff; text-decoration: underline; text-underline-offset: 4px; }
.work-video { width: 100%; aspect-ratio: 16 / 9; overflow: hidden; background: #142e58; border: 1px solid rgb(255 255 255 / 20%); border-radius: 6px; margin-bottom: 36px; }
.work-video video { width: 100%; height: 100%; object-fit: contain; }
.work-audio { display: flex; gap: 36px; align-items: center; padding: 42px; margin-bottom: 36px; border: 1px solid rgb(255 255 255 / 16%); border-radius: 6px; background: #3261aa; }
.work-audio-cover { display: flex; justify-content: center; align-items: center; width: 180px; aspect-ratio: 1; flex-shrink: 0; background: #244d8e; border: 1px solid rgb(255 255 255 / 20%); border-radius: 4px; overflow: hidden; }
.work-audio-cover img { width: 100%; height: 100%; object-fit: cover; }
.work-audio-cover svg { width: 54px; height: 54px; fill: none; stroke: #e0e9f8; stroke-width: 1; }
.work-audio-controls { flex: 1; min-width: 0; }
.work-audio-controls p { color: #e0e9f8; font-size: 12px; margin: 0 0 12px; }
.work-audio-controls h2 { font-size: 23px; font-weight: 500; margin: 0 0 24px; overflow-wrap: anywhere; }
.work-audio-controls audio { width: 100%; }
.work-detail-grid { display: grid; grid-template-columns: minmax(0, 1fr) 270px; gap: 48px; }
.work-description-panel { min-width: 0; }
.work-detail-category { color: #e0e9f8; font-size: 11px; letter-spacing: .08em; margin: 0 0 13px; }
.work-description-panel h1 { margin: 0; font-size: clamp(27px, 3vw, 36px); font-weight: 600; letter-spacing: -.03em; line-height: 1.5; overflow-wrap: anywhere; }
.work-detail-meta { display: flex; flex-wrap: wrap; gap: 10px 22px; color: #e0e9f8; font-size: 11px; line-height: 1.8; margin-top: 18px; }
.work-full-description { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 15px; line-height: 2.1; margin: 30px 0 0; color: #f1f5ff; }
.work-detail-aside { display: grid; align-content: start; gap: 18px; }
.work-like-panel, .work-attachment-panel, .work-likes-panel { padding: 22px; background: #3261aa; border: 1px solid rgb(255 255 255 / 15%); border-radius: 4px; }
.work-like-button { display: flex; justify-content: center; align-items: center; gap: 13px; padding: 12px 22px; width: 100%; border: 1px solid rgb(255 255 255 / 45%); border-radius: 4px; background: transparent; color: #fff; font-size: 21px; }
.work-like-button:hover { background: rgb(255 255 255 / 10%); }
.work-like-button svg { width: 24px; height: 24px; stroke: currentColor; fill: none; stroke-width: 1.5; }
.work-like-button.is-liked svg { fill: #fff; }
.work-like-button:disabled { opacity: .6; }
.work-like-panel p { margin: 13px 0 0; text-align: center; color: #e0e9f8; font-size: 11px; line-height: 1.7; }
.work-attachment-panel h2 { margin: 0; font-size: 14px; font-weight: 500; }
.work-attachment-panel p { margin: 10px 0 18px; color: #e0e9f8; font-size: 11px; line-height: 1.8; overflow-wrap: anywhere; }
.work-attachment-panel .life-action { width: 100%; }
.work-likes-toggle { display: flex; justify-content: space-between; align-items: center; gap: 12px; width: 100%; background: transparent; border: 0; padding: 0; color: #fff; font-size: 12px; text-align: left; }
#work-likes { margin-top: 18px; border-top: 1px solid rgb(255 255 255 / 20%); padding-top: 10px; max-height: 280px; overflow-y: auto; }
#work-likes p { font-size: 12px; color: #e0e9f8; line-height: 1.8; }
#work-likes ul { list-style: none; padding: 0; margin: 0; }
#work-likes li { display: flex; flex-direction: column; gap: 5px; padding: 12px 0; border-bottom: 1px solid rgb(255 255 255 / 12%); font-size: 12px; overflow-wrap: anywhere; }
#work-likes li:last-child { border: 0; }
#work-likes time { color: #e0e9f8; font-size: 10px; }
@media (max-width: 900px) { .work-detail-grid { grid-template-columns: minmax(0, 1fr) 240px; gap: 30px; } .work-audio { padding: 28px; gap: 26px; } .work-audio-cover { width: 140px; } }
@media (max-width: 640px) { .work-detail-header { margin-bottom: 24px; } .work-detail-grid { grid-template-columns: 1fr; gap: 30px; } .work-audio { flex-direction: column; padding: 28px 20px; gap: 22px; } .work-audio-cover { width: 150px; } .work-audio-controls { width: 100%; } .work-audio-controls h2 { font-size: 20px; } .work-detail-meta { gap: 8px 16px; } .work-full-description { font-size: 14px; } }
</style>
