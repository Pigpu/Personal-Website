<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { useI18n } from 'vue-i18n'
import { careerUi } from '../locales/career'
import { siteLanguage } from '../services/homepage'
import { careerPayload, careerPeriod, careerTags, careerToForm } from '../utils/career'
import type { Career, CareerForm } from '../utils/career'

const { t, locale } = useI18n()
const copy = computed(() => careerUi[siteLanguage(locale.value)])
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const careerList = ref<Career[]>([])
const loading = ref(true)
const loadError = ref('')
const saveError = ref('')
const deleteError = ref('')
const isSaving = ref(false)
const isDeleting = ref(false)
const isEditing = ref(false)
const editDialog = ref<HTMLDialogElement | null>(null)
const deleteDialog = ref<HTMLDialogElement | null>(null)
const itemToDelete = ref<Career | null>(null)
const newTag = ref('')
const emptyForm = (): CareerForm => ({ id: undefined, startDate: '', endDate: '', isCurrent: false, company: '', position: '', description: '', tagsList: [] })
const form = reactive<CareerForm>(emptyForm())

function errorMessage(error: unknown, fallback: string) {
  return axios.isAxiosError(error) && [401, 403].includes(error.response?.status || 0) ? copy.value.authFailed : fallback
}
async function fetchCareers() {
  loading.value = true
  loadError.value = ''
  try { careerList.value = (await axios.get<Career[]>('/api/career/list')).data }
  catch (error) { loadError.value = errorMessage(error, copy.value.loadFailed) }
  finally { loading.value = false }
}
function resetForm() {
  Object.assign(form, emptyForm())
  newTag.value = ''
  isEditing.value = false
  saveError.value = ''
}
function openAddModal() {
  if (!isAdmin.value) return
  resetForm()
  editDialog.value?.showModal()
}
function editItem(item: Career) {
  if (!isAdmin.value || item.id === undefined) return
  resetForm()
  Object.assign(form, careerToForm(item))
  isEditing.value = true
  editDialog.value?.showModal()
}
function closeModal() { if (!isSaving.value) editDialog.value?.close() }
function addTag() {
  const tag = newTag.value.trim()
  if (tag && !form.tagsList.includes(tag)) form.tagsList.push(tag)
  newTag.value = ''
}
function removeTag(index: number) { form.tagsList.splice(index, 1) }
async function submitForm() {
  if (!isAdmin.value || isSaving.value) return
  if (newTag.value.trim()) addTag()
  isSaving.value = true
  saveError.value = ''
  try {
    await axios.post('/api/career/save', careerPayload(form))
    editDialog.value?.close()
    await fetchCareers()
  } catch (error) { saveError.value = errorMessage(error, copy.value.saveFailed) }
  finally { isSaving.value = false }
}
function openDeleteModal(item: Career) {
  if (!isAdmin.value || item.id === undefined) return
  itemToDelete.value = item
  deleteError.value = ''
  deleteDialog.value?.showModal()
}
function closeDeleteModal() { if (!isDeleting.value) deleteDialog.value?.close() }
async function confirmDelete() {
  if (!isAdmin.value || itemToDelete.value?.id === undefined || isDeleting.value) return
  isDeleting.value = true
  deleteError.value = ''
  try {
    await axios.delete(`/api/career/delete/${itemToDelete.value.id}`)
    deleteDialog.value?.close()
    await fetchCareers()
  } catch (error) { deleteError.value = errorMessage(error, copy.value.deleteFailed) }
  finally { isDeleting.value = false }
}
onMounted(fetchCareers)
</script>

<template>
  <div class="career-scene">
    <div class="career-background" aria-hidden="true">
      <svg class="career-linework career-linework--upper" viewBox="0 0 720 640" fill="none" focusable="false">
        <path d="M60 -80C30 215 220 440 760 460" />
        <path d="M120 -80C90 180 260 380 760 400" />
        <path d="M180 -80C150 145 300 320 760 340" />
        <path d="M240 -80C210 110 340 260 760 280" />
      </svg>
      <svg class="career-linework career-linework--lower" viewBox="0 0 720 640" fill="none" focusable="false">
        <g transform="translate(720 640) rotate(180)">
          <path d="M120 -80C90 180 260 380 760 400" />
          <path d="M180 -80C150 145 300 320 760 340" />
          <path d="M240 -80C210 110 340 260 760 280" />
        </g>
      </svg>
      <svg class="career-note career-note--small" viewBox="0 0 48 64" fill="none" focusable="false">
        <path d="M30 44V10C31 18 41 19 41 28" /><ellipse cx="22" cy="47" rx="8" ry="6" transform="rotate(-18 22 47)" />
      </svg>
      <svg class="career-note career-note--large" viewBox="0 0 64 64" fill="none" focusable="false">
        <path d="M22 47V18L50 10V39M22 25L50 17" /><ellipse cx="15" cy="49" rx="7" ry="5" transform="rotate(-18 15 49)" /><ellipse cx="43" cy="41" rx="7" ry="5" transform="rotate(-18 43 41)" />
      </svg>
    </div>
    <div class="career-page">
      <header class="career-header">
        <div>
          <p class="career-eyebrow">EXPERIENCE</p>
          <h1>{{ t('car.title') }}</h1>
          <p class="career-subtitle">{{ t('car.subtitle') }}</p>
        </div>
        <button v-if="isAdmin" type="button" class="career-add" @click="openAddModal"><span aria-hidden="true">＋</span>{{ copy.add }}</button>
      </header>

      <p v-if="loading" class="career-status" role="status">{{ copy.loading }}</p>
      <div v-else-if="loadError" class="career-status career-load-error" role="alert"><p>{{ loadError }}</p><button type="button" @click="fetchCareers">{{ copy.retry }}</button></div>
      <p v-else-if="!careerList.length" class="career-status">{{ copy.empty }}</p>
      <ol v-else class="career-timeline">
        <li v-for="(item, index) in careerList" :key="item.id ?? index" class="timeline-item" :class="{ 'timeline-item--latest': index === 0 }">
          <article :aria-labelledby="`career-title-${index}`">
            <p class="timeline-period">{{ careerPeriod(item, copy.present) }}</p>
            <div class="timeline-heading">
              <div class="timeline-heading-copy">
                <h2 :id="`career-title-${index}`">
                  <svg class="timeline-icon timeline-company-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 21V4h16v17M9 21v-5h6v5M8 8h1m6 0h1M8 12h1m6 0h1M2 21h20" /></svg>
                  <span>{{ item.company }}</span>
                </h2>
                <p v-if="item.position" class="timeline-position">
                  <svg class="timeline-icon timeline-role-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><rect x="3" y="5" width="18" height="15" rx="2" /><path d="M9 3v4m6-4v4M14 11h4m-4 4h4M6 17c0-2 4-2 4 0" /><circle cx="8" cy="11.5" r="1.5" /></svg>
                  <span>{{ item.position }}</span>
                </p>
              </div>
              <div v-if="isAdmin" class="timeline-actions">
                <button type="button" :disabled="item.id === undefined" @click="editItem(item)">{{ copy.edit }}</button>
                <button type="button" :disabled="item.id === undefined" @click="openDeleteModal(item)">{{ copy.remove }}</button>
              </div>
            </div>
            <p v-if="item.description" class="timeline-description">{{ item.description }}</p>
            <ul v-if="careerTags(item.tags).length" class="timeline-tags">
              <li v-for="tag in careerTags(item.tags)" :key="tag"><span aria-hidden="true">#</span>{{ tag }}</li>
            </ul>
          </article>
        </li>
      </ol>
    </div>

    <Teleport to="body">
      <dialog ref="editDialog" class="career-dialog" aria-labelledby="career-editor-title" @click.self="closeModal" @cancel="isSaving && $event.preventDefault()" @close="resetForm">
        <form class="career-dialog-content" @submit.prevent="submitForm">
          <header class="career-dialog-header"><h2 id="career-editor-title">{{ isEditing ? copy.editTitle : copy.addTitle }}</h2><button type="button" :aria-label="copy.close" :disabled="isSaving" @click="closeModal">×</button></header>
          <p v-if="saveError" class="career-dialog-error" role="alert">{{ saveError }}</p>
          <fieldset :disabled="isSaving">
            <div class="career-form-grid">
              <div class="career-field"><label for="career-start">{{ copy.start }}</label><input id="career-start" v-model="form.startDate" type="date" required /></div>
              <div class="career-field"><label for="career-end">{{ copy.end }}</label><input id="career-end" v-model="form.endDate" type="date" :min="form.startDate" :disabled="form.isCurrent" :required="!form.isCurrent" /></div>
            </div>
            <label class="career-checkbox"><input v-model="form.isCurrent" type="checkbox" />{{ copy.current }}</label>
            <div class="career-form-grid">
              <div class="career-field"><label for="career-company">{{ copy.company }}</label><input id="career-company" v-model="form.company" :placeholder="copy.companyPlaceholder" required /></div>
              <div class="career-field"><label for="career-position">{{ copy.position }}</label><input id="career-position" v-model="form.position" :placeholder="copy.positionPlaceholder" /></div>
            </div>
            <div class="career-field"><label for="career-description">{{ copy.description }}</label><textarea id="career-description" v-model="form.description" rows="5" :placeholder="copy.descriptionPlaceholder" /></div>
            <div class="career-field">
              <label for="career-tag">{{ copy.tags }}</label>
              <ul v-if="form.tagsList.length" class="career-edit-tags"><li v-for="(tag, index) in form.tagsList" :key="tag">{{ tag }}<button type="button" :aria-label="`${copy.removeTag}: ${tag}`" @click="removeTag(index)">×</button></li></ul>
              <div class="career-tag-input"><input id="career-tag" v-model="newTag" :placeholder="copy.tagPlaceholder" @keydown.enter.prevent="addTag" /><button type="button" :aria-label="copy.addTag" @click="addTag">＋</button></div>
            </div>
          </fieldset>
          <footer class="career-dialog-actions"><button type="button" :disabled="isSaving" @click="closeModal">{{ copy.cancel }}</button><button type="submit" class="career-primary" :disabled="isSaving">{{ isSaving ? copy.saving : isEditing ? copy.save : copy.publish }}</button></footer>
        </form>
      </dialog>
      <dialog ref="deleteDialog" class="career-dialog career-dialog--delete" aria-labelledby="career-delete-title" aria-describedby="career-delete-hint" @click.self="closeDeleteModal" @cancel="isDeleting && $event.preventDefault()" @close="itemToDelete = null">
        <div class="career-dialog-content">
          <header class="career-dialog-header"><h2 id="career-delete-title">{{ copy.deleteTitle }}</h2><button type="button" :aria-label="copy.close" :disabled="isDeleting" @click="closeDeleteModal">×</button></header>
          <p class="career-delete-company">{{ itemToDelete?.company }}</p><p id="career-delete-hint" class="career-delete-hint">{{ copy.deleteHint }}</p>
          <p v-if="deleteError" class="career-dialog-error" role="alert">{{ deleteError }}</p>
          <footer class="career-dialog-actions"><button type="button" :disabled="isDeleting" @click="closeDeleteModal">{{ copy.cancel }}</button><button type="button" class="career-danger" :disabled="isDeleting" @click="confirmDelete">{{ isDeleting ? copy.deleting : copy.confirmDelete }}</button></footer>
        </div>
      </dialog>
    </Teleport>
  </div>
</template>

<style scoped>
.career-scene { position: relative; isolation: isolate; min-height: calc(100svh - 118px); overflow: hidden; }
.career-background { position: fixed; inset: 118px 0 0; z-index: -1; overflow: hidden; pointer-events: none; }
.career-linework { position: absolute; width: 820px; height: auto; stroke: #fff; stroke-width: 1.25; opacity: .28; }
.career-linework--upper { top: -140px; right: -140px; }
.career-linework--lower { bottom: -140px; left: -180px; opacity: .24; }
.career-note { position: absolute; stroke: #fff; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
.career-note--small { width: 30px; height: 40px; left: 6%; top: 34%; opacity: .3; transform: rotate(-14deg); }
.career-note--large { width: 52px; height: 52px; right: 7%; bottom: 14%; opacity: .23; transform: rotate(12deg); }
.career-page { position: relative; width: min(1120px, calc(100% - 96px)); margin: 0 auto; padding: 68px 0 80px; }
.career-header { display: flex; align-items: center; justify-content: space-between; gap: 28px; padding-bottom: 36px; border-bottom: 1px solid var(--site-line); }
.career-eyebrow { margin: 0 0 18px; color: var(--site-muted); font-size: 11px; font-weight: 500; letter-spacing: .22em; }
h1 { font-size: clamp(32px, 3.2vw, 46px); font-weight: 600; letter-spacing: -.04em; line-height: 1.3; margin: 0; }
.career-subtitle { margin: 15px 0 0; font-size: 14px; color: var(--site-muted); line-height: 1.9; }
.career-add { display: inline-flex; align-items: center; gap: 12px; flex-shrink: 0; color: #fff; background: transparent; border: 1px solid rgb(255 255 255 / 55%); padding: 11px 18px; border-radius: 4px; font-size: 13px; transition: background-color .2s; }
.career-add:hover { background: rgb(255 255 255 / 10%); }
.career-add > span { font-size: 18px; line-height: 1; }
.career-timeline { list-style: none; margin: 46px 0 0; padding: 0; }
.timeline-item { position: relative; padding: 0 0 54px 48px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-item::before { content: ''; position: absolute; left: 0; top: 3px; width: 13px; height: 13px; border: 2px solid #fff; border-radius: 50%; background: var(--site-blue); z-index: 1; transition: background-color .2s; }
.timeline-item:not(:last-child)::after { content: ''; position: absolute; left: 6px; top: 10px; bottom: -10px; width: 1px; background: rgb(255 255 255 / 36%); }
.timeline-item--latest::before { background: #fff; box-shadow: 0 0 0 5px rgb(255 255 255 / 10%); }
.timeline-period { margin: 0 0 14px; color: var(--site-muted); font-size: 12px; font-variant-numeric: tabular-nums; letter-spacing: .03em; line-height: 1.7; }
.timeline-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; }
.timeline-heading-copy { min-width: 0; }
.timeline-heading h2 { display: grid; grid-template-columns: 24px minmax(0, 1fr); gap: 12px; align-items: start; margin: 0; max-width: 850px; font-size: 23px; font-weight: 500; line-height: 1.55; letter-spacing: -.02em; overflow-wrap: anywhere; }
.timeline-icon { flex-shrink: 0; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.timeline-company-icon { width: 24px; height: 24px; margin-top: 6px; color: #fff; }
.timeline-role-icon { width: 20px; height: 20px; margin: 3px 2px 0; }
.timeline-position { display: grid; grid-template-columns: 24px minmax(0, 1fr); gap: 12px; align-items: start; margin: 9px 0 0; font-size: 15px; line-height: 1.8; color: var(--site-muted); overflow-wrap: anywhere; }
.timeline-description { margin: 19px 0 0; max-width: 900px; font-size: 15px; line-height: 1.95; white-space: pre-wrap; overflow-wrap: anywhere; }
.timeline-tags { display: flex; flex-wrap: wrap; list-style: none; padding: 0; margin: 19px 0 0; gap: 8px; }
.timeline-tags li { display: inline-flex; gap: 5px; padding: 3px 9px; border: 1px solid rgb(255 255 255 / 22%); border-radius: 4px; background: rgb(255 255 255 / 6%); font-size: 12px; color: var(--site-muted); line-height: 1.8; overflow-wrap: anywhere; }
.timeline-tags li > span { opacity: .7; }
.timeline-actions { display: flex; gap: 5px; flex-shrink: 0; }
.timeline-actions button, .career-load-error button { border: 0; background: transparent; color: #fff; padding: 8px 10px; border-radius: 3px; font-size: 12px; transition: background-color .2s; }
.timeline-actions button:hover, .career-load-error button:hover { background: rgb(255 255 255 / 10%); }
.career-status { margin: 46px 0; font-size: 14px; line-height: 1.9; color: var(--site-muted); }
.career-load-error p { margin: 0 0 8px; }
.career-load-error button { padding-left: 0; text-decoration: underline; text-underline-offset: 4px; }
.career-dialog { --site-focus: #2857a5; width: min(640px, calc(100% - 32px)); max-height: 90svh; padding: 0; margin: auto; border: 1px solid #d4deec; border-radius: 8px; background: #fff; color: #214777; font-family: var(--font-sans); }
.career-dialog::backdrop { background: rgb(15 30 57 / 58%); }
.career-dialog-content { padding: 28px 32px; }
.career-dialog-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px; }
.career-dialog-header h2 { font-size: 22px; font-weight: 600; margin: 0; }
.career-dialog-header > button { padding: 4px 8px; line-height: 1; font-size: 28px; }
.career-dialog button { border: 0; background: transparent; color: inherit; border-radius: 4px; }
.career-dialog button:hover { background: #edf3fc; }
.career-dialog button:disabled { opacity: .5; cursor: default; }
.career-dialog fieldset { border: 0; padding: 0; margin: 0; min-width: 0; }
.career-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.career-field { margin-bottom: 20px; min-width: 0; }
.career-field > label { display: block; margin-bottom: 8px; font-size: 13px; }
.career-field input, .career-field textarea { display: block; min-width: 0; width: 100%; border: 1px solid #c6d4e8; background: #f6f8fc; color: #214777; border-radius: 4px; padding: 10px 12px; font-size: 14px; line-height: 1.7; }
.career-field input:disabled { opacity: .5; }
.career-field textarea { resize: vertical; min-height: 100px; }
.career-checkbox { display: flex; align-items: center; gap: 9px; margin: 0 0 24px; font-size: 13px; line-height: 1.7; }
.career-checkbox input { accent-color: #2857a5; }
.career-edit-tags { display: flex; flex-wrap: wrap; gap: 8px; list-style: none; margin: 0 0 12px; padding: 0; }
.career-edit-tags li { display: inline-flex; align-items: center; gap: 8px; padding: 5px 8px 5px 10px; background: #edf3fc; border-radius: 4px; font-size: 12px; }
.career-edit-tags button { padding: 2px 5px; font-size: 16px; line-height: 1; }
.career-tag-input { position: relative; }
.career-tag-input input { padding-right: 48px; }
.career-tag-input > button { position: absolute; right: 5px; top: 5px; bottom: 5px; width: 36px; font-size: 22px; }
.career-dialog-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; padding-top: 22px; border-top: 1px solid #dce5f1; }
.career-dialog-actions button { padding: 10px 18px; font-size: 13px; }
.career-dialog-actions .career-primary { background: #2857a5; color: #fff; }
.career-dialog-actions .career-primary:hover { background: #214b91; }
.career-dialog-actions .career-danger { background: #ab3838; color: #fff; }
.career-dialog-actions .career-danger:hover { background: #922c2c; }
.career-dialog-error { padding: 12px; margin: 0 0 20px; border-radius: 4px; background: #fff0ef; color: #9b332e; font-size: 13px; line-height: 1.8; }
.career-dialog--delete { width: min(440px, calc(100% - 32px)); }
.career-delete-company { font-size: 15px; line-height: 1.8; overflow-wrap: anywhere; }
.career-delete-hint { color: #4a6382; font-size: 13px; line-height: 1.9; }
@media (max-width: 900px) { .career-page { width: calc(100% - 64px); padding-top: 40px; } }
@media (max-width: 760px) { .career-background { top: 138px; } }
@media (max-width: 640px) {
  .career-page { width: calc(100% - 40px); padding: 34px 0 56px; }
  .career-scene { min-height: calc(100svh - 138px); }
  .career-linework { width: 560px; }
  .career-linework--upper { right: -240px; top: -120px; opacity: .22; }
  .career-linework--lower { left: -260px; bottom: -160px; opacity: .2; }
  .career-note--small { left: 8px; top: 48%; width: 22px; opacity: .18; }
  .career-note--large { right: 10px; bottom: 8%; width: 34px; opacity: .16; }
  .career-header { align-items: flex-start; gap: 18px; flex-wrap: wrap; padding-bottom: 26px; }
  h1 { font-size: 34px; }
  .career-subtitle { font-size: 13px; }
  .career-add { font-size: 12px; padding: 9px 13px; }
  .career-timeline { margin-top: 34px; }
  .timeline-item { padding-left: 30px; padding-bottom: 40px; }
  .timeline-heading { flex-direction: column; gap: 10px; }
  .timeline-heading h2 { font-size: 20px; }
  .timeline-company-icon { margin-top: 3px; }
  .timeline-role-icon { margin-top: 2px; }
  .timeline-period { font-size: 11px; margin-bottom: 12px; }
  .timeline-position, .timeline-description { font-size: 14px; }
  .timeline-description { margin-top: 16px; }
  .timeline-actions { gap: 12px; }
  .timeline-actions button { padding: 6px 0; }
  .timeline-tags { gap: 8px; }
  .career-dialog-content { padding: 22px 18px; }
  .career-form-grid { grid-template-columns: 1fr; gap: 0; }
  .career-dialog-header h2 { font-size: 20px; }
}
</style>
