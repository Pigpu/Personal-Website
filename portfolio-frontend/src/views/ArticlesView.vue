<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { articleUi } from '../locales/articles'
import { siteLanguage } from '../services/homepage'
import { plainExcerpt, sortedArticles } from '../utils/articles'
import type { Article, ArticleSort, SortDirection } from '../utils/articles'
import '../styles/life.css'

const { t, locale } = useI18n()
const copy = computed(() => articleUi[siteLanguage(locale.value)])
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const articles = ref<Article[]>([])
const loading = ref(true)
const error = ref('')
const sortKey = ref<ArticleSort>('time')
const direction = ref<SortDirection>('desc')
const sorted = computed(() => sortedArticles(articles.value, sortKey.value, direction.value))
const failedCovers = ref(new Set<number>())
const deleteDialog = ref<HTMLDialogElement | null>(null)
const articleToDelete = ref<Article | null>(null)
const deleting = ref(false)
const deleteError = ref('')

function formatDate(value: string | null | undefined) {
  const date = value ? new Date(value) : null
  return date && Number.isFinite(date.getTime()) ? date.toLocaleDateString(locale.value === 'en' ? 'en-GB' : locale.value === 'ja' ? 'ja-JP' : 'zh-CN') : '—'
}
async function fetchArticles() {
  loading.value = true
  error.value = ''
  try { articles.value = (await axios.get<Article[]>('/api/articles')).data }
  catch { error.value = copy.value.loadFailed }
  finally { loading.value = false }
}
function askDelete(article: Article) {
  if (!isAdmin.value) return
  articleToDelete.value = article
  deleteError.value = ''
  deleteDialog.value?.showModal()
}
function closeDelete() { if (!deleting.value) deleteDialog.value?.close() }
async function deleteArticle() {
  if (!isAdmin.value || !articleToDelete.value || deleting.value) return
  deleting.value = true
  deleteError.value = ''
  const id = articleToDelete.value.id
  try {
    await axios.delete(`/api/articles/${id}`)
    articles.value = articles.value.filter(article => article.id !== id)
    deleteDialog.value?.close()
  } catch (cause) {
    deleteError.value = axios.isAxiosError(cause) && [401, 403].includes(cause.response?.status || 0) ? copy.value.authFailed : copy.value.deleteFailed
  } finally { deleting.value = false }
}
onMounted(fetchArticles)
</script>

<template>
  <div class="life-scene">
    <FixedSiteBackground />
    <div class="life-page">
      <header class="life-header">
        <div><p class="life-eyebrow">LIFE & NOTES</p><h1 class="life-title">{{ t('life.title') }}</h1><p class="life-subtitle">{{ t('life.subtitle') }}</p></div>
        <router-link v-if="isAdmin" to="/editor" class="life-action"><span aria-hidden="true">＋</span>{{ copy.create }}</router-link>
      </header>
      <div class="article-toolbar">
        <div class="article-sort" role="group" :aria-label="copy.sort">
          <button type="button" :aria-pressed="sortKey === 'time'" @click="sortKey = 'time'">{{ copy.time }}</button>
          <button type="button" :aria-pressed="sortKey === 'likes'" @click="sortKey = 'likes'">{{ copy.likes }}</button>
        </div>
        <select v-model="direction" class="article-direction" :aria-label="copy.sort">
          <option value="desc">{{ copy.descending }} · {{ sortKey === 'time' ? copy.newest : copy.mostLiked }}</option>
          <option value="asc">{{ copy.ascending }} · {{ sortKey === 'time' ? copy.oldest : copy.leastLiked }}</option>
        </select>
      </div>
      <p v-if="loading" class="life-state" role="status">{{ copy.loading }}</p>
      <div v-else-if="error" class="life-state" role="alert"><p>{{ error }}</p><button type="button" class="life-action" @click="fetchArticles">{{ copy.retry }}</button></div>
      <p v-else-if="!sorted.length" class="life-state">{{ copy.empty }}</p>
      <ol v-else class="article-list">
        <li v-for="article in sorted" :key="article.id" class="article-row" :class="{ 'article-row--admin': isAdmin }">
          <router-link :to="`/article/${article.id}`" class="article-row-link">
            <div class="article-cover">
              <img v-if="article.coverUrl && !failedCovers.has(article.id)" :src="article.coverUrl" :alt="article.title" loading="lazy" @error="failedCovers.add(article.id)" />
              <div v-else class="article-cover-fallback"><svg viewBox="0 0 48 48" fill="none" aria-hidden="true"><path d="M11 7h26v34H11zM17 16h14M17 23h14M17 30h8" /></svg><span>{{ copy.noCover }}</span></div>
            </div>
            <div class="article-body">
              <p class="article-category">{{ article.category }}</p>
              <h2>{{ article.title }}</h2>
              <p class="article-excerpt">{{ plainExcerpt(article.content || article.summary) }}</p>
              <div class="article-meta">
                <span :aria-label="`${copy.views}: ${article.viewCount || 0}`"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></svg>{{ article.viewCount || 0 }}</span>
                <span :aria-label="`${copy.likeCount}: ${article.likeCount || 0}`"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" /></svg>{{ article.likeCount || 0 }}</span>
                <time :datetime="article.createdAt || undefined" :aria-label="copy.date">{{ formatDate(article.createdAt) }}</time>
              </div>
            </div>
          </router-link>
          <div v-if="isAdmin" class="article-admin-actions"><router-link :to="`/editor?id=${article.id}`" class="life-inline-button">{{ copy.edit }}</router-link><button type="button" class="life-inline-button" @click="askDelete(article)">{{ copy.remove }}</button></div>
        </li>
      </ol>
    </div>
    <Teleport to="body">
      <dialog ref="deleteDialog" class="life-dialog" aria-labelledby="delete-article-title" @click.self="closeDelete" @cancel="deleting && $event.preventDefault()" @close="articleToDelete = null">
        <div class="life-dialog-content"><header class="life-dialog-header"><h2 id="delete-article-title">{{ copy.deleteTitle }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" :disabled="deleting" @click="closeDelete">×</button></header><p>{{ articleToDelete?.title }}</p><p class="life-dialog-note">{{ copy.deleteHint }}</p><p v-if="deleteError" class="life-dialog-error" role="alert">{{ deleteError }}</p><footer class="life-dialog-actions"><button type="button" :disabled="deleting" @click="closeDelete">{{ copy.cancel }}</button><button type="button" class="life-dialog-danger" :disabled="deleting" @click="deleteArticle">{{ deleting ? copy.deleting : copy.confirmDelete }}</button></footer></div>
      </dialog>
    </Teleport>
  </div>
</template>

<style scoped>
.article-toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-top: 34px; padding: 7px 18px; background: #214b8d; border: 1px solid rgb(255 255 255 / 8%); border-radius: 4px; }
.article-sort { display: flex; gap: 6px; }
.article-sort button { position: relative; border: 0; border-radius: 3px; background: transparent; color: #d8e5f8; padding: 11px 14px; font-size: 13px; transition: color .2s; }
.article-sort button:hover { color: #fff; }
.article-sort button[aria-pressed="true"] { color: #fff; font-weight: 500; }
.article-sort button::after { content: ''; position: absolute; left: 50%; bottom: 3px; width: 22px; height: 2px; background: #fff; transform: translateX(-50%) scaleX(0); transition: transform .2s; }
.article-sort button[aria-pressed="true"]::after { transform: translateX(-50%) scaleX(1); }
.article-direction { background: transparent; border: 1px solid rgb(255 255 255 / 25%); border-radius: 3px; color: #e0e9f8; padding: 6px 10px; font-size: 12px; }
.article-direction option { color: #214777; background: #fff; }
.article-list { display: grid; gap: 20px; list-style: none; margin: 24px 0 0; padding: 0; }
.article-row { --site-focus: #fff; --article-ink: #fff; --article-muted: #e0e9f8; position: relative; background: #3261aa; color: var(--article-ink); border: 1px solid rgb(255 255 255 / 10%); border-radius: 4px; transition: background-color .2s, border-color .2s; }
.article-row:hover { background: #3666b0; border-color: rgb(255 255 255 / 22%); }
.article-row-link { display: grid; grid-template-columns: 240px minmax(0, 1fr); gap: 28px; align-items: center; padding: 24px; text-decoration: none; color: var(--article-ink); }
.article-row-link:focus-visible { outline-offset: -4px; border-radius: 4px; }
.article-cover { width: 100%; aspect-ratio: 16 / 10; overflow: hidden; border: 1px solid rgb(255 255 255 / 14%); border-radius: 3px; background: rgb(255 255 255 / 5%); }
.article-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform .25s ease; }
.article-row-link:hover .article-cover img { transform: scale(1.035); }
.article-row-link:hover h2 { color: #fff; text-decoration: underline; text-underline-offset: 5px; }
.article-cover-fallback { display: flex; height: 100%; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: var(--article-muted); font-size: 11px; }
.article-cover-fallback svg { width: 38px; height: 38px; stroke: currentColor; stroke-width: 1.1; opacity: .65; }
.article-body { min-width: 0; }
.article-row--admin .article-body { padding-right: 100px; }
.article-category { margin: 0 0 10px; color: var(--article-muted); font-size: 11px; letter-spacing: .08em; }
.article-body h2 { margin: 0; font-size: 23px; line-height: 1.55; letter-spacing: -.02em; font-weight: 500; overflow-wrap: anywhere; }
.article-excerpt { margin: 12px 0 0; color: var(--article-muted); font-size: 14px; line-height: 1.9; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; overflow-wrap: anywhere; }
.article-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 22px; color: var(--article-muted); margin-top: 18px; font-size: 11px; line-height: 1.7; }
.article-meta > span { display: inline-flex; align-items: center; gap: 6px; }
.article-meta svg { width: 14px; height: 14px; fill: none; stroke: currentColor; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
.article-admin-actions { position: absolute; right: 15px; bottom: 24px; display: flex; gap: 3px; }
.article-admin-actions .life-inline-button { color: #e0e9f8; }
.article-admin-actions .life-inline-button:hover { background: rgb(255 255 255 / 10%); color: #fff; }
@media (max-width: 900px) { .article-row-link { grid-template-columns: 190px minmax(0, 1fr); gap: 22px; padding: 22px; } .article-row--admin .article-body { padding-right: 0; } .article-row--admin .article-meta { padding-right: 95px; } }
@media (max-width: 640px) {
  .article-toolbar { margin-top: 26px; padding: 8px 10px; gap: 12px; }
  .article-sort button { padding: 9px 10px; font-size: 12px; }
  .article-direction { max-width: 100%; font-size: 11px; padding: 6px 8px; }
  .article-list { gap: 16px; margin-top: 20px; }
  .article-row-link { grid-template-columns: 104px minmax(0, 1fr); align-items: start; gap: 18px; padding: 18px 16px; }
  .article-cover { aspect-ratio: 1; }
  .article-body h2 { font-size: 18px; }
  .article-category { margin-bottom: 6px; font-size: 10px; }
  .article-excerpt { font-size: 12px; line-height: 1.8; -webkit-line-clamp: 2; margin-top: 8px; }
  .article-meta { gap: 6px 13px; margin-top: 12px; font-size: 10px; }
  .article-row--admin .article-row-link { padding-bottom: 8px; }
  .article-row--admin .article-meta { padding-right: 0; }
  .article-admin-actions { position: static; justify-content: flex-end; padding: 0 8px 12px; }
}
@media (max-width: 380px) { .article-row-link { grid-template-columns: 88px minmax(0, 1fr); gap: 14px; } }
</style>
