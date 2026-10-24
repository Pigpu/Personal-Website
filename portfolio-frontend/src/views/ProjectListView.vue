<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { projectUi } from '../locales/projects'
import { siteLanguage } from '../services/homepage'
import { sortedProjects } from '../utils/projects'
import type { Project, ProjectSort } from '../utils/projects'
import '../styles/life.css'

const { t, locale } = useI18n()
const copy = computed(() => projectUi[siteLanguage(locale.value)])
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const projects = ref<Project[]>([])
const searchKeyword = ref('')
const currentSort = ref<ProjectSort>('latest')
const visible = computed(() => sortedProjects(projects.value, currentSort.value))
const loading = ref(true)
const error = ref('')
const failedCovers = ref(new Set<number>())
const deleteDialog = ref<HTMLDialogElement | null>(null)
const projectToDelete = ref<Project | null>(null)
const deleting = ref(false)
const deleteError = ref('')
let requestNumber = 0
let searchTimer: ReturnType<typeof setTimeout> | undefined
let activeRequest: AbortController | undefined

async function fetchProjects() {
  const request = ++requestNumber
  activeRequest?.abort()
  const controller = new AbortController()
  activeRequest = controller
  loading.value = true
  error.value = ''
  const keyword = searchKeyword.value.trim()
  try {
    const response = await axios.get<Project[]>(keyword ? '/api/projects/search' : '/api/projects', {
      params: keyword ? { keyword } : { sort: currentSort.value }, signal: controller.signal,
    })
    if (request === requestNumber) projects.value = response.data
  } catch (cause) {
    if (request === requestNumber && !axios.isCancel(cause)) error.value = copy.value.loadFailed
  } finally { if (request === requestNumber) loading.value = false }
}
function searchNow() {
  if (searchTimer) clearTimeout(searchTimer)
  void fetchProjects()
}
watch(searchKeyword, () => {
  // Invalidate earlier responses immediately, including during the debounce interval.
  requestNumber++
  activeRequest?.abort()
  loading.value = true
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(searchNow, 220)
})
watch(currentSort, searchNow)
function askDelete(project: Project) {
  if (!isAdmin.value) return
  projectToDelete.value = project
  deleteError.value = ''
  deleteDialog.value?.showModal()
}
function closeDelete() { if (!deleting.value) deleteDialog.value?.close() }
async function deleteProject() {
  if (!isAdmin.value || !projectToDelete.value || deleting.value) return
  deleting.value = true
  deleteError.value = ''
  try {
    await axios.delete('/api/projects/' + projectToDelete.value.id)
    deleteDialog.value?.close()
    await fetchProjects()
  } catch (cause) {
    deleteError.value = axios.isAxiosError(cause) && [401, 403].includes(cause.response?.status || 0) ? copy.value.authFailed : copy.value.deleteFailed
  } finally { deleting.value = false }
}
onMounted(fetchProjects)
onUnmounted(() => { requestNumber++; activeRequest?.abort(); if (searchTimer) clearTimeout(searchTimer) })
</script>

<template>
  <div class="life-scene">
    <FixedSiteBackground />
    <div class="life-page works-page">
      <header class="life-header">
        <div><p class="life-eyebrow">MADE WITH INTEREST</p><h1 class="life-title">{{ t('art.title') }}</h1><p class="life-subtitle">{{ t('art.subtitle') }}</p></div>
        <router-link v-if="isAdmin" to="/projects/upload" class="life-action"><span aria-hidden="true">＋</span>{{ t('art.new') }}</router-link>
      </header>
      <div class="works-toolbar">
        <form class="works-search" role="search" @submit.prevent="searchNow">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4.5 4.5" /></svg>
          <input v-model="searchKeyword" type="search" :placeholder="t('art.search')" :aria-label="copy.search" />
          <button v-if="searchKeyword" type="button" :aria-label="copy.clear" @click="searchKeyword = ''">×</button>
        </form>
        <div class="works-sort" role="group" :aria-label="copy.sort">
          <button type="button" :aria-pressed="currentSort === 'latest'" @click="currentSort = 'latest'">{{ t('art.mostR') }}</button>
          <button type="button" :aria-pressed="currentSort === 'likes'" @click="currentSort = 'likes'">{{ t('art.mostS') }}</button>
        </div>
      </div>
      <p v-if="loading" class="life-state" role="status">{{ copy.loading }}</p>
      <div v-else-if="error" class="life-state" role="alert"><p>{{ error }}</p><button type="button" class="life-action" @click="searchNow">{{ copy.retry }}</button></div>
      <p v-else-if="!visible.length" class="life-state">{{ searchKeyword.trim() ? copy.noResults : copy.empty }}</p>
      <ul v-else class="works-grid">
        <li v-for="project in visible" :key="project.id" class="work-card">
          <router-link :to="'/projects/' + project.id" class="work-card-link">
            <div class="work-cover">
              <img v-if="project.coverUrl && !failedCovers.has(project.id)" :src="project.coverUrl" :alt="project.title" loading="lazy" @error="failedCovers.add(project.id)" />
              <div v-else class="work-cover-fallback"><svg viewBox="0 0 48 48" aria-hidden="true"><path d="M9 9h30v30H9zM9 32l10-10 8 7 5-5 7 8" /><circle cx="31" cy="17" r="3" /></svg><span>{{ copy.noCover }}</span></div>
              <span class="work-type"><svg v-if="project.mediaType === 'AUDIO'" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 17V5l11-2v12M9 9l11-2" /><ellipse cx="5.5" cy="18" rx="3.5" ry="3" /><ellipse cx="16.5" cy="16" rx="3.5" ry="3" /></svg><svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 10 6-10 6Z" /></svg>{{ project.mediaType === 'AUDIO' ? copy.audio : copy.video }}</span>
            </div>
            <div class="work-body">
              <p v-if="project.category" class="work-category">{{ project.category }}</p>
              <h2>{{ project.title }}</h2>
              <p class="work-description">{{ project.description }}</p>
              <div class="work-footer">
                <div class="work-stats">
                  <span :aria-label="copy.views + ': ' + (project.viewCount || 0)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></svg>{{ project.viewCount || 0 }}</span>
                  <span :aria-label="copy.likes + ': ' + (project.likeCount || 0)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 5a5.5 5.5 0 0 0-8 1 5.5 5.5 0 0 0-8 7l8 8 8-8a5.5 5.5 0 0 0 0-8Z" /></svg>{{ project.likeCount || 0 }}</span>
                </div>
                <span class="work-detail">{{ t('art.detail') }}</span>
              </div>
            </div>
          </router-link>
          <div v-if="isAdmin" class="work-admin"><router-link :to="'/projects/upload?id=' + project.id" class="life-inline-button">{{ copy.edit }}</router-link><button type="button" class="life-inline-button" @click="askDelete(project)">{{ copy.remove }}</button></div>
        </li>
      </ul>
    </div>
    <Teleport to="body">
      <dialog ref="deleteDialog" class="life-dialog" aria-labelledby="delete-work-title" @click.self="closeDelete" @cancel="deleting && $event.preventDefault()" @close="projectToDelete = null">
        <div class="life-dialog-content"><header class="life-dialog-header"><h2 id="delete-work-title">{{ copy.deleteTitle }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" :disabled="deleting" @click="closeDelete">×</button></header><p>{{ projectToDelete?.title }}</p><p class="life-dialog-note">{{ copy.deleteHint }}</p><p v-if="deleteError" class="life-dialog-error" role="alert">{{ deleteError }}</p><footer class="life-dialog-actions"><button type="button" :disabled="deleting" @click="closeDelete">{{ copy.cancel }}</button><button type="button" class="life-dialog-danger" :disabled="deleting" @click="deleteProject">{{ deleting ? copy.deleting : copy.confirmDelete }}</button></footer></div>
      </dialog>
    </Teleport>
  </div>
</template>

<style scoped>
.works-toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 14px 30px; margin: 34px 0 26px; padding: 9px 18px; background: #214b8d; border: 1px solid rgb(255 255 255 / 8%); border-radius: 4px; }
.works-search { display: flex; align-items: center; gap: 11px; flex: 0 1 370px; min-width: 0; }
.works-search > svg { width: 18px; height: 18px; flex-shrink: 0; fill: none; stroke: #e0e9f8; stroke-width: 1.5; }
.works-search input { min-width: 0; width: 100%; padding: 11px 0; border: 0; background: transparent; color: #fff; font-size: 13px; border-radius: 2px; }
.works-search input::placeholder { color: #d2dff3; }
.works-search input::-webkit-search-cancel-button { display: none; }
.works-search button { border: 0; background: transparent; color: #fff; padding: 4px 7px; font-size: 21px; line-height: 1; }
.works-sort { display: flex; align-self: stretch; gap: 6px; }
.works-sort button { position: relative; padding: 10px 14px; border: 0; border-radius: 3px; background: transparent; color: #d8e5f8; font-size: 13px; }
.works-sort button:hover, .works-sort button[aria-pressed="true"] { color: #fff; }
.works-sort button::after { content: ''; position: absolute; bottom: 1px; left: 50%; width: 22px; height: 2px; background: #fff; transform: translateX(-50%) scaleX(0); transition: transform .2s; }
.works-sort button[aria-pressed="true"]::after { transform: translateX(-50%) scaleX(1); }
.works-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); align-items: stretch; gap: 24px; margin: 0; padding: 0; list-style: none; }
.work-card { min-width: 0; display: flex; flex-direction: column; background: #3261aa; border: 1px solid rgb(255 255 255 / 12%); border-radius: 6px; overflow: hidden; transition: background-color .2s, border-color .2s; }
.work-card:hover { background: #3666b0; border-color: rgb(255 255 255 / 28%); }
.work-card-link { display: flex; flex: 1; flex-direction: column; color: #fff; text-decoration: none; }
.work-card-link:focus-visible { outline-offset: -3px; border-radius: 6px; }
.work-cover { position: relative; aspect-ratio: 16 / 9; background: #244d8e; overflow: hidden; }
.work-cover > img { display: block; width: 100%; height: 100%; object-fit: cover; transition: transform .25s; }
.work-card-link:hover .work-cover > img { transform: scale(1.035); }
.work-cover-fallback { display: flex; height: 100%; justify-content: center; align-items: center; flex-direction: column; gap: 9px; color: #d8e5f8; font-size: 11px; }
.work-cover-fallback svg { width: 35px; height: 35px; stroke: currentColor; stroke-width: 1.25; fill: none; opacity: .75; }
.work-type { position: absolute; top: 12px; left: 12px; display: inline-flex; align-items: center; gap: 6px; padding: 5px 8px; background: rgb(22 53 102 / 90%); border: 1px solid rgb(255 255 255 / 20%); border-radius: 3px; font-size: 10px; }
.work-type svg { width: 13px; height: 13px; stroke: currentColor; stroke-width: 1.5; fill: none; }
.work-body { display: flex; flex: 1; flex-direction: column; padding: 19px 20px 16px; min-width: 0; }
.work-category { color: #e0e9f8; font-size: 10px; letter-spacing: .06em; margin: 0 0 8px; }
.work-body h2 { margin: 0; font-size: 19px; line-height: 1.5; font-weight: 500; letter-spacing: -.02em; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; overflow-wrap: anywhere; }
.work-card-link:hover h2 { text-decoration: underline; text-underline-offset: 4px; }
.work-description { flex: 1; min-height: 48px; margin: 9px 0 20px; color: #e0e9f8; font-size: 13px; line-height: 1.85; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; overflow-wrap: anywhere; }
.work-footer { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding-top: 14px; border-top: 1px solid rgb(255 255 255 / 19%); }
.work-stats { display: flex; gap: 16px; color: #e0e9f8; font-size: 11px; }
.work-stats span { display: inline-flex; align-items: center; gap: 5px; }
.work-stats svg { width: 14px; height: 14px; fill: none; stroke: currentColor; stroke-width: 1.5; stroke-linejoin: round; }
.work-detail { font-size: 11px; white-space: nowrap; }
.work-admin { display: flex; justify-content: flex-end; gap: 2px; padding: 0 10px 7px; }
.work-admin .life-inline-button { font-size: 11px; color: #e0e9f8; }
@media (max-width: 1000px) { .works-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) {
  .works-toolbar { margin-top: 26px; padding: 8px 12px; gap: 5px; }
  .works-search { flex-basis: 100%; }
  .works-search input { font-size: 12px; }
  .works-sort { width: 100%; border-top: 1px solid rgb(255 255 255 / 12%); justify-content: flex-end; padding-top: 3px; }
  .works-sort button { padding: 10px 12px; font-size: 12px; }
  .works-grid { grid-template-columns: 1fr; gap: 20px; }
  .work-body { padding: 18px 18px 16px; }
}
</style>
