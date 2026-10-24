<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { articleUi } from '../locales/articles'
import { siteLanguage } from '../services/homepage'
import type { Article } from '../utils/articles'
import '../styles/life.css'

interface ArticleComment { id: number; username: string; role?: string; content: string; createdAt: string; parentId?: number | null }
interface CommentNode extends ArticleComment { children: CommentNode[] }
interface LikeRecord { id: number; username: string; createdAt: string }
const { t, locale } = useI18n()
const route = useRoute()
const copy = computed(() => articleUi[siteLanguage(locale.value)])
const article = ref<Article | null>(null)
const isLoading = ref(true)
const loadError = ref('')
const comments = ref<ArticleComment[]>([])
const commentError = ref('')
const newComment = ref('')
const currentParentId = ref<number | null>(null)
const replyToUsername = ref('')
const isLoggedIn = computed(() => !!localStorage.getItem('token'))
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const currentUsername = localStorage.getItem('username')
const hasLiked = ref(false)
const liking = ref(false)
const submittingComment = ref(false)
const deletingComment = ref(false)
const likesList = ref<LikeRecord[]>([])
const showLikesList = ref(false)
const likesError = ref('')
const deleteDialog = ref<HTMLDialogElement | null>(null)
const messageDialog = ref<HTMLDialogElement | null>(null)
const commentIdToDelete = ref<number | null>(null)
const deleteError = ref('')
const message = ref({ title: '', content: '' })
let requestVersion = 0

const commentTree = computed(() => {
  const map = new Map<number, CommentNode>()
  const tree: CommentNode[] = []
  comments.value.forEach(comment => map.set(comment.id, { ...comment, children: [] }))
  for (const comment of comments.value) {
    const node = map.get(comment.id)!
    const parent = comment.parentId && map.get(comment.parentId)
    if (parent && parent.id !== node.id) parent.children.push(node)
    else tree.push(node)
  }
  return tree.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
})
function canDelete(comment: ArticleComment) { return isLoggedIn.value && (isAdmin.value || (!!currentUsername && comment.username === currentUsername)) }
function formatDateTime(value: string | null | undefined, short = false, full = false) {
  const date = value ? new Date(value) : null
  if (!date || !Number.isFinite(date.getTime())) return '—'
  return date.toLocaleString(locale.value === 'en' ? 'en-GB' : locale.value === 'ja' ? 'ja-JP' : 'zh-CN', full ? { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' } : short ? { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' } : { year: 'numeric', month: '2-digit', day: '2-digit' })
}
function showMessage(title: string, content: string) { message.value = { title, content }; if (!messageDialog.value?.open) messageDialog.value?.showModal() }
async function fetchComments() {
  const id = String(route.params.id)
  commentError.value = ''
  try {
    const response = await axios.get<ArticleComment[]>(`/api/comments/article/${id}`)
    if (String(route.params.id) === id) comments.value = response.data
  } catch { if (String(route.params.id) === id) commentError.value = copy.value.loadFailed }
}
async function fetchLikesList() {
  if (!isAdmin.value) return
  const id = String(route.params.id)
  likesError.value = ''
  try {
    const response = await axios.get<LikeRecord[]>(`/api/articles/${id}/likes-list`)
    if (String(route.params.id) === id) likesList.value = response.data
  } catch { if (String(route.params.id) === id) likesError.value = copy.value.loadFailed }
}
async function loadArticle() {
  const version = ++requestVersion
  const id = String(route.params.id)
  isLoading.value = true
  article.value = null
  loadError.value = ''
  comments.value = []
  likesList.value = []
  hasLiked.value = false
  showLikesList.value = false
  cancelReply()
  newComment.value = ''
  void fetchComments()
  try {
    const response = await axios.get<Article>(`/api/articles/${id}`)
    if (version !== requestVersion) return
    article.value = response.data
    if (isLoggedIn.value) {
      try {
        const status = await axios.get<boolean>(`/api/articles/${id}/like-status`)
        if (version === requestVersion) hasLiked.value = status.data
      } catch { /* The article remains readable if the personal like status is unavailable. */ }
    }
    if (isAdmin.value && version === requestVersion) void fetchLikesList()
  } catch { if (version === requestVersion) loadError.value = copy.value.loadFailed }
  finally { if (version === requestVersion) isLoading.value = false }
}
async function handleLike() {
  if (!isLoggedIn.value) { showMessage(t('article.msgNeedLoginTitle'), t('article.msgNeedLoginDesc')); return }
  if (!article.value || liking.value) return
  const id = article.value.id
  liking.value = true
  try {
    const response = await axios.post<{ likeCount: number; isLiked: boolean }>(`/api/articles/${id}/like`, {})
    if (article.value?.id !== id) return
    article.value.likeCount = response.data.likeCount
    hasLiked.value = response.data.isLiked
    if (isAdmin.value) void fetchLikesList()
  } catch { showMessage(t('article.msgOpFailTitle'), t('article.msgOpFailDesc')) }
  finally { liking.value = false }
}
async function setReply(comment: ArticleComment) {
  if (!isLoggedIn.value) { showMessage(t('article.msgNeedLoginTitle'), t('article.loginPrompt')); return }
  currentParentId.value = comment.id
  replyToUsername.value = comment.username
  await nextTick()
  const textarea = document.getElementById('comment-area')
  textarea?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  textarea?.focus({ preventScroll: true })
}
function cancelReply() { currentParentId.value = null; replyToUsername.value = '' }
async function submitComment() {
  if (!newComment.value.trim() || submittingComment.value || !isLoggedIn.value) return
  const articleId = Number(route.params.id)
  submittingComment.value = true
  try {
    await axios.post('/api/comments/save', { content: newComment.value, articleId, parentId: currentParentId.value, userId: null })
    if (Number(route.params.id) !== articleId) return
    newComment.value = ''
    cancelReply()
    await fetchComments()
  } catch { showMessage(copy.value.notice, t('article.msgPublishFail')) }
  finally { submittingComment.value = false }
}
function openDeleteModal(comment: ArticleComment) {
  if (!canDelete(comment)) return
  commentIdToDelete.value = comment.id
  deleteError.value = ''
  deleteDialog.value?.showModal()
}
function closeDelete() { if (!deletingComment.value) deleteDialog.value?.close() }
async function confirmDelete() {
  if (commentIdToDelete.value === null || deletingComment.value) return
  deletingComment.value = true
  try {
    await axios.delete(`/api/comments/${commentIdToDelete.value}`)
    deleteDialog.value?.close()
    await fetchComments()
  } catch { deleteError.value = copy.value.deleteFailed }
  finally { deletingComment.value = false }
}
watch(() => route.params.id, () => { void loadArticle() }, { immediate: true })
onUnmounted(() => { requestVersion++ })
</script>

<template>
  <div class="life-scene">
    <FixedSiteBackground />
    <div class="life-page reading-page">
      <router-link to="/articles" class="reading-back">{{ t('article.backToList') }}</router-link>
      <p v-if="isLoading" class="life-state" role="status">{{ t('article.loading') }}</p>
      <div v-else-if="loadError" class="life-state" role="alert"><p>{{ loadError }}</p><button type="button" class="life-action" @click="loadArticle">{{ copy.retry }}</button></div>
      <template v-else-if="article">
        <header class="reading-header">
          <p class="life-eyebrow">{{ article.category }}</p><h1>{{ article.title }}</h1>
          <div class="reading-meta"><time :datetime="article.createdAt || undefined">{{ formatDateTime(article.createdAt) }}</time><span>{{ article.viewCount || 0 }} {{ t('article.views') }}</span><button type="button" :disabled="liking" @click="handleLike">{{ article.likeCount || 0 }} {{ t('article.likes') }}</button><router-link v-if="isAdmin" :to="`/editor?id=${article.id}`">{{ copy.edit }}</router-link></div>
        </header>
        <div class="reading-paper"><MdPreview :modelValue="article.content || ''" theme="light" :language="locale === 'zh' ? 'zh-CN' : 'en-US'" /></div>
        <div class="reading-interactions">
          <button type="button" class="reading-like" :class="{ 'reading-like--active': hasLiked }" :aria-pressed="hasLiked" :disabled="liking" @click="handleLike"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" /></svg>{{ article.likeCount || 0 }} · {{ hasLiked ? t('article.liked') : t('article.likes') }}</button>
          <p>{{ hasLiked ? t('article.cancelLike') : t('article.promptLike') }}</p>
        </div>
        <section v-if="isAdmin" class="reading-likes">
          <button type="button" :aria-expanded="showLikesList" @click="showLikesList = !showLikesList">{{ t('article.adminLikes') }} · {{ likesList.length }}<span aria-hidden="true">{{ showLikesList ? '−' : '＋' }}</span></button>
          <div v-if="showLikesList"><p v-if="likesError" role="alert">{{ likesError }}</p><p v-else-if="!likesList.length">{{ t('article.noLikes') }}</p><ul v-else><li v-for="like in likesList" :key="like.id"><span>{{ like.username }}</span><time>{{ formatDateTime(like.createdAt, false, true) }}</time></li></ul></div>
        </section>
        <section class="reading-comments" aria-labelledby="reading-comments-title">
          <header class="reading-comments-header"><h2 id="reading-comments-title">{{ t('article.commentTitle') }}</h2><span>{{ comments.length }} {{ t('article.commentCount') }}</span></header>
          <form v-if="isLoggedIn" class="comment-composer" @submit.prevent="submitComment">
            <div v-if="currentParentId" class="comment-reply-to"><span>{{ t('article.replyingTo') }} @{{ replyToUsername }}</span><button type="button" @click="cancelReply">{{ t('article.cancel') }}</button></div>
            <textarea id="comment-area" v-model="newComment" :aria-label="t('article.commentTitle')" :placeholder="currentParentId ? t('article.placeholderReply') : t('article.placeholderComment')" :disabled="submittingComment" rows="4" required />
            <div><button type="submit" :disabled="submittingComment || !newComment.trim()">{{ submittingComment ? copy.saving : currentParentId ? t('article.submitReply') : t('article.submitComment') }}</button></div>
          </form>
          <div v-else class="comment-login"><p>{{ t('article.loginPrompt') }}</p><router-link to="/login">{{ t('article.loginNow') }}</router-link></div>
          <div v-if="commentError" class="life-state" role="alert"><p>{{ commentError }}</p><button type="button" class="life-action" @click="fetchComments">{{ copy.retry }}</button></div>
          <div v-else class="comment-list">
            <article v-for="comment in commentTree" :key="comment.id" class="comment-thread">
              <div class="comment-author-row"><div class="comment-avatar" aria-hidden="true">{{ (comment.username || '?').charAt(0).toUpperCase() }}</div><div class="comment-author"><span>{{ comment.username || t('article.anonymous') }}</span><span v-if="comment.role === 'ROLE_ADMIN'" class="comment-staff">{{ t('article.staff') }}</span><time>{{ formatDateTime(comment.createdAt, true) }}</time></div><div class="comment-actions"><button v-if="canDelete(comment)" type="button" @click="openDeleteModal(comment)">{{ t('article.deleteBtn') }}</button><button type="button" @click="setReply(comment)">{{ t('article.replyBtn') }}</button></div></div>
              <p class="comment-content">{{ comment.content }}</p>
              <div v-if="comment.children.length" class="comment-replies"><article v-for="child in comment.children" :key="child.id" class="comment-child"><div class="comment-author-row"><div class="comment-author"><span>{{ child.username || t('article.anonymous') }}</span><span>{{ t('article.repliedTo') }} @{{ comment.username || t('article.anonymous') }}</span></div><div class="comment-actions"><button v-if="canDelete(child)" type="button" @click="openDeleteModal(child)">{{ t('article.deleteBtn') }}</button><button type="button" @click="setReply(child)">{{ t('article.replyBtn') }}</button></div></div><p class="comment-content">{{ child.content }}</p></article></div>
            </article>
            <p v-if="!comments.length" class="life-state">{{ t('article.noComments') }}</p>
          </div>
        </section>
      </template>
    </div>
    <Teleport to="body">
      <dialog ref="deleteDialog" class="life-dialog" aria-labelledby="comment-delete-title" @click.self="closeDelete" @cancel="deletingComment && $event.preventDefault()" @close="commentIdToDelete = null"><div class="life-dialog-content"><h2 id="comment-delete-title">{{ t('article.delConfirmTitle') }}</h2><p class="life-dialog-note">{{ copy.deleteHint }}</p><p v-if="deleteError" class="life-dialog-error" role="alert">{{ deleteError }}</p><footer class="life-dialog-actions"><button type="button" :disabled="deletingComment" @click="closeDelete">{{ t('article.delCancelBtn') }}</button><button type="button" class="life-dialog-danger" :disabled="deletingComment" @click="confirmDelete">{{ deletingComment ? copy.deleting : t('article.delConfirmBtn') }}</button></footer></div></dialog>
      <dialog ref="messageDialog" class="life-dialog" aria-labelledby="article-message-title"><div class="life-dialog-content"><h2 id="article-message-title">{{ message.title }}</h2><p class="life-dialog-note">{{ message.content }}</p><footer class="life-dialog-actions"><button type="button" class="life-dialog-primary" @click="messageDialog?.close()">{{ copy.close }}</button></footer></div></dialog>
    </Teleport>
  </div>
</template>

<style scoped>
.reading-page { max-width: 920px; }
.reading-back { display: inline-block; color: var(--site-muted); font-size: 12px; text-decoration: none; margin-bottom: 34px; }
.reading-back:hover { color: #fff; }
.reading-header { margin-bottom: 30px; }
.reading-header .life-eyebrow { margin-bottom: 14px; }
.reading-header h1 { margin: 0; font-size: clamp(28px, 3.5vw, 42px); font-weight: 600; letter-spacing: -.035em; line-height: 1.45; overflow-wrap: anywhere; }
.reading-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 10px 24px; color: var(--site-muted); margin-top: 20px; font-size: 12px; }
.reading-meta button, .reading-meta a { border: 0; background: transparent; padding: 0; color: inherit; font-size: inherit; text-decoration: none; }
.reading-meta button:hover, .reading-meta a:hover { text-decoration: underline; text-underline-offset: 4px; }
.reading-paper { --site-focus: #2857a5; background: #fff; color: #214777; border-radius: 5px; padding: 32px 40px; overflow-wrap: anywhere; }
.reading-paper :deep(.md-editor) { --md-color: #214777; --md-bk-color: #fff; font-family: var(--font-sans); }
.reading-paper :deep(.md-editor-preview) { color: #214777; }
.reading-paper :deep(.md-editor-preview-wrapper) { padding: 0; }
.reading-paper :deep(img) { max-width: 100%; height: auto; border-radius: 3px; }
.reading-interactions { margin: 30px 0; text-align: center; }
.reading-like { display: inline-flex; align-items: center; justify-content: center; gap: 10px; background: transparent; border: 1px solid rgb(255 255 255 / 55%); border-radius: 4px; color: #fff; padding: 12px 20px; font-size: 13px; }
.reading-like svg { width: 17px; height: 17px; fill: none; stroke: currentColor; stroke-width: 1.5; }
.reading-like--active { background: #fff; color: #2857a5; }
.reading-like--active svg { fill: currentColor; }
.reading-like:disabled { opacity: .6; cursor: default; }
.reading-interactions p { margin: 10px 0 0; font-size: 11px; color: var(--site-muted); }
.reading-likes { max-width: 500px; margin: 32px auto 0; padding-bottom: 10px; border-bottom: 1px solid var(--site-line); }
.reading-likes > button { display: flex; justify-content: space-between; width: 100%; padding: 12px 0; border: 0; color: #fff; background: transparent; font-size: 13px; }
.reading-likes p { font-size: 12px; color: var(--site-muted); }
.reading-likes ul { max-height: 220px; overflow-y: auto; list-style: none; margin: 0; padding: 0; }
.reading-likes li { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px; border-top: 1px solid rgb(255 255 255 / 15%); padding: 12px 0; font-size: 12px; }
.reading-likes time { color: var(--site-muted); }
.reading-comments { margin-top: 58px; }
.reading-comments-header { display: flex; gap: 16px; flex-wrap: wrap; align-items: baseline; margin-bottom: 24px; }
.reading-comments-header h2 { margin: 0; font-size: 23px; font-weight: 500; }
.reading-comments-header > span { font-size: 12px; color: var(--site-muted); }
.comment-composer { --site-focus: #2857a5; padding: 20px; border-radius: 5px; background: #fff; color: #214777; margin-bottom: 28px; }
.comment-composer textarea { display: block; width: 100%; border: 0; background: transparent; color: #214777; resize: vertical; font-size: 14px; line-height: 1.9; padding: 8px; }
.comment-composer textarea::placeholder { color: #63748c; }
.comment-composer > div:last-child { display: flex; justify-content: flex-end; margin-top: 12px; }
.comment-composer button { border: 0; background: #2857a5; color: #fff; border-radius: 4px; padding: 9px 15px; font-size: 12px; }
.comment-composer button:disabled { opacity: .55; cursor: default; }
.comment-reply-to { display: flex; gap: 12px; justify-content: space-between; align-items: center; font-size: 12px; margin-bottom: 12px; padding: 4px 8px; background: #edf3fc; border-radius: 3px; }
.comment-reply-to button { background: transparent; color: #2857a5; }
.comment-login { padding: 24px; border: 1px solid var(--site-line); border-radius: 4px; margin-bottom: 28px; font-size: 13px; }
.comment-login p { margin: 0 0 12px; color: var(--site-muted); }
.comment-login a { color: #fff; text-decoration: underline; text-underline-offset: 4px; }
.comment-thread { padding: 25px 0; border-top: 1px solid var(--site-line); }
.comment-author-row { display: flex; align-items: center; gap: 12px; }
.comment-avatar { display: flex; flex: 0 0 34px; width: 34px; height: 34px; align-items: center; justify-content: center; border-radius: 50%; border: 1px solid rgb(255 255 255 / 45%); font-size: 13px; }
.comment-author { display: flex; flex: 1; align-items: center; gap: 8px; flex-wrap: wrap; min-width: 0; font-size: 13px; }
.comment-author time, .comment-child .comment-author > span:nth-child(2) { color: var(--site-muted); font-size: 11px; }
.comment-author time { flex-basis: 100%; }
.comment-staff { padding: 1px 5px; background: #fff; color: #2857a5; border-radius: 2px; font-size: 9px; }
.comment-actions { display: flex; gap: 6px; flex-shrink: 0; }
.comment-actions button { padding: 6px; border: 0; border-radius: 3px; color: #fff; background: transparent; font-size: 11px; }
.comment-actions button:hover { background: rgb(255 255 255 / 10%); }
.comment-content { margin: 15px 0 0 46px; font-size: 14px; line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
.comment-replies { margin: 22px 0 0 46px; padding-left: 20px; border-left: 1px solid rgb(255 255 255 / 30%); }
.comment-child + .comment-child { margin-top: 24px; }
.comment-child .comment-content { margin-left: 0; font-size: 13px; }
@media (max-width: 640px) {
  .reading-paper { padding: 22px 18px; }
  .reading-meta { gap: 10px 16px; font-size: 11px; }
  .reading-comments { margin-top: 40px; }
  .comment-composer { padding: 14px; }
  .comment-author-row { flex-wrap: wrap; gap: 8px; }
  .comment-actions { margin-left: auto; }
  .comment-content { margin-left: 0; }
  .comment-replies { margin-left: 16px; padding-left: 16px; }
}
</style>
