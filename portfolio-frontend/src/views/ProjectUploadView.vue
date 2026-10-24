<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { projectUi } from '../locales/projects'
import { siteLanguage } from '../services/homepage'
import { emptyProject, projectCategories, projectChanged, projectDraft, projectPayload, uploadedMediaType } from '../utils/projects'
import type { Project, ProjectDraft, UploadRole } from '../utils/projects'
import '../styles/life.css'

const router = useRouter()
const route = useRoute()
const { locale, t } = useI18n()
const copy = computed(() => projectUi[siteLanguage(locale.value)])
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const isEditMode = computed(() => route.query.id !== undefined)
const form = reactive<ProjectDraft>(emptyProject())
const baseline = ref<ProjectDraft>(emptyProject())
const isModified = computed(() => projectChanged(form, baseline.value))
const loading = ref(true)
const loadError = ref('')
const error = ref('')
const isSubmitting = ref(false)
const saveSucceeded = ref(false)
const busy = computed(() => isSubmitting.value || uploading.value !== null)
const uploading = ref<UploadRole | null>(null)
const progress = reactive<Record<UploadRole, number>>({ cover: 0, media: 0, attachment: 0 })
const uploadErrors = reactive<Record<UploadRole, string>>({ cover: '', media: '', attachment: '' })
const uploaded = reactive<Record<UploadRole, boolean>>({ cover: false, media: false, attachment: false })
const fileInputs: Partial<Record<UploadRole, HTMLInputElement>> = {}
const leaveDialog = ref<HTMLDialogElement | null>(null)
const successDialog = ref<HTMLDialogElement | null>(null)
let leaveResolver: ((value: boolean) => void) | null = null
let returnTimer: ReturnType<typeof setTimeout> | undefined
let loadRequest = 0
const categories = computed(() => [...new Set([...projectCategories, form.category])])
const panels = computed(() => [
  { role: 'cover' as const, field: 'coverUrl' as const, title: copy.value.cover, hint: copy.value.coverHint, accept: 'image/*' },
  { role: 'media' as const, field: 'mediaUrl' as const, title: copy.value.media, hint: copy.value.mediaHint, accept: 'video/*,audio/*' },
  { role: 'attachment' as const, field: 'attachmentUrl' as const, title: copy.value.attachment, hint: copy.value.attachmentHint, accept: '.zip,.rar,.pdf,.7z' },
])
function categoryLabel(value: string) {
  const labels: Record<string, string> = { '视频剪辑': copy.value.categoryVideo, '音乐相关': copy.value.categoryMusic, '技术开发': copy.value.categoryTech, '其他': copy.value.categoryOther }
  return labels[value] || value
}
function report(cause: unknown, fallback: string) {
  return axios.isAxiosError(cause) && [401, 403].includes(cause.response?.status || 0) ? copy.value.authFailed : fallback
}
function setFileInput(role: UploadRole, element: Element | ComponentPublicInstance | null) {
  if (element instanceof HTMLInputElement) fileInputs[role] = element
}
async function loadData() {
  if (!isAdmin.value) { loading.value = false; return }
  const request = ++loadRequest
  loading.value = true
  loadError.value = ''
  error.value = ''
  saveSucceeded.value = false
  Object.assign(form, emptyProject())
  baseline.value = emptyProject()
  for (const role of ['cover', 'media', 'attachment'] as const) { uploadErrors[role] = ''; progress[role] = 0; uploaded[role] = false }
  try {
    if (isEditMode.value) {
      const id = route.query.id
      if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id)) throw new Error('Invalid project id')
      const response = await axios.get<Project>('/api/projects/' + id)
      if (request !== loadRequest) return
      const loaded = projectDraft(response.data)
      Object.assign(form, loaded)
      baseline.value = { ...loaded }
    }
  } catch (cause) { if (request === loadRequest) loadError.value = report(cause, copy.value.loadFailed) }
  finally { if (request === loadRequest) loading.value = false }
}
function keepEditing() {
  const resolve = leaveResolver
  leaveResolver = null
  leaveDialog.value?.close()
  resolve?.(false)
}
function confirmLeave() {
  const resolve = leaveResolver
  leaveResolver = null
  leaveDialog.value?.close()
  resolve?.(true)
}
function requestLeave(): boolean | Promise<boolean> {
  if (busy.value) { error.value = copy.value.wait; return false }
  if (saveSucceeded.value || !isModified.value) return true
  leaveResolver?.(false)
  return new Promise(resolve => { leaveResolver = resolve; if (!leaveDialog.value?.open) leaveDialog.value?.showModal() })
}
onBeforeRouteLeave(requestLeave)
onBeforeRouteUpdate(requestLeave)
watch(() => route.query.id, () => { void loadData() })
function beforeUnload(event: BeforeUnloadEvent) {
  if ((isModified.value && !saveSucceeded.value) || busy.value) { event.preventDefault(); event.returnValue = '' }
}
async function uploadFile(event: Event, role: UploadRole) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !isAdmin.value || busy.value || loading.value || saveSucceeded.value) return
  uploading.value = role
  uploadErrors[role] = ''
  uploaded[role] = false
  progress[role] = 0
  error.value = ''
  const body = new FormData()
  body.append('file', file)
  body.append('type', role)
  try {
    const response = await axios.post<string>('/api/upload/project', body, {
      responseType: 'text',
      onUploadProgress: event => {
        progress[role] = event.total ? Math.min(99, Math.round(event.loaded * 100 / event.total)) : 0
      },
    })
    const url = response.data.trim()
    if (!/^https?:\/\//i.test(url) && !url.startsWith('/uploads/')) throw new Error('Invalid upload response')
    if (role === 'cover') form.coverUrl = url
    if (role === 'media') {
      form.mediaUrl = url
      form.mediaType = uploadedMediaType(file)
    }
    if (role === 'attachment') form.attachmentUrl = url
    progress[role] = 100
    uploaded[role] = true
  } catch (cause) { uploadErrors[role] = report(cause, copy.value.uploadFailed) }
  finally { uploading.value = null; input.value = '' }
}
async function submitProject() {
  if (!isAdmin.value || loading.value || loadError.value || busy.value || saveSucceeded.value) return
  const payload = projectPayload(form)
  if (!payload.title || !payload.coverUrl || !payload.mediaUrl) { error.value = copy.value.required; return }
  isSubmitting.value = true
  error.value = ''
  try {
    if (isEditMode.value) await axios.put('/api/projects/' + route.query.id, payload)
    else await axios.post('/api/projects/save', payload)
    baseline.value = { ...form }
    saveSucceeded.value = true
    successDialog.value?.showModal()
    returnTimer = setTimeout(() => { void router.push('/projects') }, 1200)
  } catch (cause) { error.value = report(cause, copy.value.saveFailed) }
  finally { isSubmitting.value = false }
}
onMounted(() => { void loadData(); window.addEventListener('beforeunload', beforeUnload) })
onUnmounted(() => {
  loadRequest++
  window.removeEventListener('beforeunload', beforeUnload)
  if (returnTimer) clearTimeout(returnTimer)
  leaveResolver?.(false)
})
</script>

<template>
  <div class="life-scene">
    <FixedSiteBackground />
    <div class="life-page work-editor-page">
      <router-link to="/projects" class="work-back"><span aria-hidden="true">←</span>{{ t('project.backToList') }}</router-link>
      <header class="life-header work-editor-header"><div><p class="life-eyebrow">A LITTLE CREATION</p><h1 class="life-title">{{ isEditMode ? copy.editorEdit : copy.editorNew }}</h1><p class="life-subtitle">{{ copy.editorHint }}</p></div></header>
      <div v-if="!isAdmin" class="life-state"><p>{{ copy.adminOnly }}</p><router-link to="/login" class="life-action">{{ copy.login }}</router-link></div>
      <p v-else-if="loading" class="life-state" role="status">{{ copy.loading }}</p>
      <div v-else-if="loadError" class="life-state" role="alert"><p>{{ loadError }}</p><button type="button" class="life-action" @click="loadData">{{ copy.retry }}</button></div>
      <form v-else class="work-form" :aria-busy="busy" @submit.prevent="submitProject">
        <p v-if="error" class="work-form-error" role="alert">{{ error }}</p>
        <fieldset :disabled="busy || saveSucceeded">
          <div class="work-fields">
            <div><label for="work-title">{{ copy.titleLabel }}</label><input id="work-title" v-model="form.title" :placeholder="copy.titlePlaceholder" required /></div>
            <div><label for="work-category">{{ copy.category }}</label><select id="work-category" v-model="form.category"><option v-for="value in categories" :key="value" :value="value">{{ categoryLabel(value) }}</option></select></div>
          </div>
          <label for="work-description">{{ copy.description }}</label><textarea id="work-description" v-model="form.description" rows="5" :placeholder="copy.descriptionPlaceholder"></textarea>
          <p class="work-upload-note">{{ copy.uploadHint }}</p>
          <div class="work-upload-grid">
            <section v-for="panel in panels" :key="panel.role" class="work-upload-panel">
              <h2>{{ panel.title }}<span v-if="panel.role === 'attachment'">{{ copy.optional }}</span></h2>
              <label :for="'work-url-' + panel.role">{{ copy.url }}</label>
              <input :id="'work-url-' + panel.role" v-model="form[panel.field]" type="text" placeholder="https://… / /uploads/…" :required="panel.role !== 'attachment'" @input="uploaded[panel.role] = false" />
              <div v-if="panel.role === 'cover'" class="work-cover-preview"><img v-if="form.coverUrl" :src="form.coverUrl" :alt="copy.cover" /><svg v-else viewBox="0 0 48 48" aria-hidden="true"><path d="M9 9h30v30H9zM9 32l10-10 8 7 5-5 7 8" /><circle cx="31" cy="17" r="3" /></svg></div>
              <p class="work-upload-hint">{{ panel.hint }}</p>
              <button type="button" class="work-file-button" @click="fileInputs[panel.role]?.click()"><span aria-hidden="true">↑</span>{{ form[panel.field] ? copy.replace : copy.upload }}</button>
              <input :ref="el => setFileInput(panel.role, el)" type="file" hidden :accept="panel.accept" @change="uploadFile($event, panel.role)" />
              <div v-if="uploading === panel.role" class="work-upload-progress" role="status"><progress :value="progress[panel.role]" max="100"></progress><span>{{ copy.uploading }} {{ progress[panel.role] }}%</span></div>
              <p v-else-if="uploaded[panel.role]" class="work-upload-status" role="status">{{ copy.uploaded }}</p>
              <p v-if="uploadErrors[panel.role]" class="work-upload-error" role="alert">{{ uploadErrors[panel.role] }}</p>
              <div v-if="panel.role === 'media'" class="work-media-type" role="group" :aria-label="copy.mediaType"><label><input v-model="form.mediaType" type="radio" value="VIDEO" />{{ copy.video }}</label><label><input v-model="form.mediaType" type="radio" value="AUDIO" />{{ copy.audio }}</label></div>
            </section>
          </div>
        </fieldset>
        <footer class="work-form-actions"><router-link to="/projects" class="work-form-cancel" :aria-disabled="busy" @click="busy && $event.preventDefault()">{{ copy.cancel }}</router-link><button type="submit" :disabled="busy || saveSucceeded">{{ isSubmitting ? copy.saving : uploading ? copy.uploading : isEditMode ? copy.save : copy.publish }}</button></footer>
      </form>
    </div>
    <Teleport to="body">
      <dialog ref="leaveDialog" class="life-dialog" aria-labelledby="leave-work-title" @cancel.prevent="keepEditing" @click.self="keepEditing" @close="keepEditing">
        <div class="life-dialog-content"><header class="life-dialog-header"><h2 id="leave-work-title">{{ copy.leaveTitle }}</h2></header><p class="life-dialog-note">{{ copy.leaveHint }}</p><footer class="life-dialog-actions"><button type="button" @click="confirmLeave">{{ copy.leave }}</button><button type="button" class="life-dialog-primary" @click="keepEditing">{{ copy.keepEditing }}</button></footer></div>
      </dialog>
      <dialog ref="successDialog" class="life-dialog" aria-labelledby="saved-work-title">
        <div class="life-dialog-content"><header class="life-dialog-header"><h2 id="saved-work-title">{{ isEditMode ? copy.updated : copy.published }}</h2></header><p>{{ copy.successHint }}</p><footer class="life-dialog-actions"><router-link to="/projects" class="work-success-link">{{ t('project.backToList') }} →</router-link></footer></div>
      </dialog>
    </Teleport>
  </div>
</template>

<style scoped>
.work-editor-page { max-width: 1040px; }
.work-back { display: inline-flex; align-items: center; gap: 12px; color: #e0e9f8; font-size: 12px; text-decoration: none; }
.work-back:hover { color: #fff; text-decoration: underline; text-underline-offset: 4px; }
.work-editor-header { margin: 34px 0; }
.work-form { --site-focus: #2857a5; color: #214777; background: #f8faff; border: 1px solid #d6e1f2; border-radius: 6px; padding: 32px; }
.work-form fieldset { border: 0; min-width: 0; padding: 0; margin: 0; }
.work-form fieldset:disabled { opacity: .75; }
.work-form label { display: block; font-size: 12px; margin-bottom: 9px; font-weight: 500; }
.work-form input:not([type="radio"]):not([type="file"]), .work-form select, .work-form textarea { width: 100%; min-width: 0; padding: 11px 12px; border: 1px solid #a7bbd8; border-radius: 4px; background: #fff; color: #214777; font-size: 14px; line-height: 1.65; }
.work-form input::placeholder, .work-form textarea::placeholder { color: #667e9e; }
.work-form textarea { resize: vertical; min-height: 120px; }
.work-fields { display: grid; grid-template-columns: minmax(0, 1fr) 210px; gap: 24px; margin-bottom: 26px; }
.work-upload-note { color: #4a6382; font-size: 12px; line-height: 1.8; margin: 30px 0 14px; }
.work-upload-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; align-items: start; }
.work-upload-panel { min-width: 0; padding: 18px; background: #eaf0fa; border: 1px solid #cedbed; border-radius: 4px; }
.work-upload-panel h2 { margin: 0 0 19px; font-size: 15px; font-weight: 600; display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.work-upload-panel h2 span { font-size: 10px; font-weight: 400; color: #4a6382; }
.work-upload-panel label { font-size: 11px; color: #4a6382; }
.work-upload-panel input:not([type="radio"]):not([type="file"]) { font-size: 12px; padding: 9px 10px; }
.work-cover-preview { display: flex; align-items: center; justify-content: center; aspect-ratio: 16 / 9; background: #dce6f5; margin: 14px 0; border: 1px solid #c5d4e9; border-radius: 3px; overflow: hidden; }
.work-cover-preview img { width: 100%; height: 100%; object-fit: cover; }
.work-cover-preview svg { width: 34px; height: 34px; fill: none; stroke: #647fa3; stroke-width: 1.2; }
.work-upload-hint { min-height: 38px; margin: 14px 0; font-size: 11px; line-height: 1.8; color: #4a6382; }
.work-file-button { display: inline-flex; gap: 9px; justify-content: center; align-items: center; width: 100%; padding: 10px 9px; border: 1px solid #88a4ca; border-radius: 4px; background: #fff; color: #2857a5; font-size: 12px; }
.work-file-button:hover { background: #f4f7fd; }
.work-upload-progress { margin-top: 14px; display: grid; gap: 7px; font-size: 11px; color: #2857a5; }
.work-upload-progress progress { width: 100%; height: 5px; accent-color: #2857a5; }
.work-upload-status { color: #2857a5; font-size: 11px; margin: 12px 0 0; }
.work-upload-error { color: #9b332e; font-size: 12px; line-height: 1.7; margin: 12px 0 0; }
.work-media-type { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 18px; }
.work-media-type label { display: inline-flex; align-items: center; gap: 6px; margin: 0; font-size: 12px; color: #214777; }
.work-media-type input { accent-color: #2857a5; }
.work-form-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; align-items: center; gap: 12px; border-top: 1px solid #cfdbed; padding-top: 24px; margin-top: 28px; }
.work-form-actions button { border: 1px solid #2857a5; background: #2857a5; color: #fff; padding: 11px 23px; font-size: 13px; border-radius: 4px; }
.work-form-actions button:hover { background: #214b91; }
.work-form-actions button:disabled { opacity: .6; cursor: default; }
.work-form-cancel { color: #214777; padding: 11px 18px; font-size: 13px; border: 1px solid #a7bbd8; border-radius: 4px; text-decoration: none; }
.work-form-cancel:hover { background: #eaf0fa; }
.work-form-error { color: #9b332e; background: #fff0ef; border: 1px solid #edc5c2; border-radius: 4px; padding: 11px 14px; font-size: 13px; line-height: 1.8; margin: 0 0 24px; }
.work-success-link { color: #2857a5; font-size: 13px; }
@media (max-width: 900px) { .work-form { padding: 24px; } .work-upload-grid { grid-template-columns: 1fr; } .work-cover-preview { max-width: 340px; } .work-upload-hint { min-height: auto; } }
@media (max-width: 640px) { .work-fields { grid-template-columns: 1fr; gap: 20px; } .work-form { padding: 22px 18px; } .work-editor-header { margin: 28px 0; } .work-upload-panel { padding: 16px; } .work-form-actions { gap: 10px; } }
</style>
