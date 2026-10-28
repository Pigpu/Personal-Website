<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import { premiumUi } from '../locales/premium'
import { getPremiumListeners, searchPremiumUsers, grantPremiumListener, revokePremiumListener } from '../services/premium'
import type { PremiumListener, PremiumUser } from '../services/premium'

const { locale } = useI18n()
const copy = computed(() => premiumUi[locale.value === 'en' ? 'en' : locale.value === 'ja' ? 'ja' : 'zh'])
const dialog = ref<HTMLDialogElement | null>(null)
const visible = ref(false)
const listeners = ref<PremiumListener[]>([])
const users = ref<PremiumUser[]>([])
const query = ref('')
const loading = ref(false)
const searching = ref(false)
const busy = ref(false)
const loadError = ref('')
const searchError = ref('')
const error = ref('')
const status = ref('')
let searchTimer: ReturnType<typeof setTimeout> | undefined
let searchRequest = 0
let listRequest = 0
let disposed = false

function report(cause: unknown, fallback: string) {
  const code = axios.isAxiosError(cause) ? cause.response?.status : undefined
  return code === 401 || code === 403 ? copy.value.authFailed : fallback
}
async function loadListeners() {
  const request = ++listRequest
  loading.value = true
  loadError.value = ''
  try {
    const values = await getPremiumListeners()
    if (!disposed && request === listRequest) listeners.value = values
  } catch (cause) {
    if (!disposed && request === listRequest) loadError.value = report(cause, copy.value.listenersFailed)
  } finally { if (!disposed && request === listRequest) loading.value = false }
}
function open() {
  query.value = ''
  users.value = []
  error.value = ''
  status.value = ''
  searchError.value = ''
  visible.value = true
  dialog.value?.showModal()
  void loadListeners()
}
function close() { if (!busy.value) dialog.value?.close() }
function onClose() {
  visible.value = false
  searchRequest++
  listRequest++
  clearTimeout(searchTimer)
}
async function search(value: string, request: number) {
  try {
    const values = await searchPremiumUsers(value)
    if (!disposed && request === searchRequest) users.value = values
  } catch (cause) {
    if (!disposed && request === searchRequest) searchError.value = report(cause, copy.value.searchFailed)
  } finally { if (!disposed && request === searchRequest) searching.value = false }
}
watch(query, value => {
  clearTimeout(searchTimer)
  const request = ++searchRequest
  users.value = []
  searchError.value = ''
  status.value = ''
  searching.value = !!value.trim() && visible.value
  if (searching.value) searchTimer = setTimeout(() => { void search(value, request) }, 220)
})
async function grant(user?: PremiumUser) {
  if (busy.value || loading.value || loadError.value || (!user && !query.value.trim())) return
  busy.value = true
  error.value = ''
  status.value = ''
  // Discard in-flight search results so they cannot overwrite the saved permission state.
  searchRequest++
  clearTimeout(searchTimer)
  searching.value = false
  try {
    const saved = await grantPremiumListener(user ? { userId: user.userId } : { username: query.value.trim() })
    if (disposed) return
    listeners.value = [saved, ...listeners.value.filter(value => value.userId !== saved.userId)]
    users.value = users.value.map(value => value.userId === saved.userId ? { ...value, authorized: true } : value)
    if (!users.value.some(value => value.userId === saved.userId)) {
      users.value.unshift({ userId: saved.userId, username: saved.username, authorized: true, admin: false })
    }
    status.value = copy.value.listenerAdded
  } catch (cause) {
    if (disposed) return
    const code = axios.isAxiosError(cause) ? cause.response?.status : undefined
    error.value = code === 404 ? copy.value.userMissing : report(cause, copy.value.grantFailed)
  } finally { if (!disposed) busy.value = false }
}
async function revoke(userId: number) {
  if (busy.value || loading.value) return
  busy.value = true
  error.value = ''
  status.value = ''
  searchRequest++
  clearTimeout(searchTimer)
  searching.value = false
  try {
    await revokePremiumListener(userId)
    if (disposed) return
    listeners.value = listeners.value.filter(value => value.userId !== userId)
    users.value = users.value.map(value => value.userId === userId ? { ...value, authorized: false } : value)
    status.value = copy.value.listenerRemoved
  } catch (cause) {
    if (!disposed) error.value = report(cause, copy.value.revokeFailed)
  } finally { if (!disposed) busy.value = false }
}
onBeforeUnmount(() => {
  disposed = true
  onClose()
  dialog.value?.close()
})
defineExpose({ open })
</script>

<template>
  <Teleport to="body">
    <dialog ref="dialog" class="life-dialog premium-audience" aria-labelledby="premium-audience-title" @cancel="busy && $event.preventDefault()" @close="onClose">
      <div class="life-dialog-content premium-form">
        <div class="life-dialog-header"><h2 id="premium-audience-title">{{ copy.manageListeners }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" :disabled="busy" @click="close">×</button></div>
        <p class="life-dialog-note premium-audience-hint">{{ copy.listenerHint }}</p>
        <form @submit.prevent="grant()">
          <label for="premium-user-search">{{ copy.searchUsers }}</label>
          <div class="premium-audience-search"><input id="premium-user-search" v-model="query" maxlength="255" autocomplete="off" :placeholder="copy.searchPlaceholder" :disabled="busy" aria-describedby="premium-search-hint" /><button type="submit" class="life-dialog-primary" :disabled="busy || loading || !!loadError || !query.trim()">{{ copy.exactAdd }}</button></div>
          <p id="premium-search-hint" class="life-dialog-note premium-hint">{{ copy.searchHint }}</p>
        </form>
        <section v-if="query.trim()" class="premium-audience-section" aria-labelledby="premium-search-results">
          <h3 id="premium-search-results">{{ copy.searchResults }}</h3>
          <p v-if="searching" class="life-dialog-note" role="status">{{ copy.searching }}</p>
          <p v-else-if="searchError" class="life-dialog-error" role="alert">{{ searchError }}</p>
          <p v-else-if="!users.length" class="life-dialog-note">{{ copy.noUsers }}</p>
          <ul v-else class="premium-audience-list">
            <li v-for="user in users" :key="user.userId"><span>{{ user.username }}</span><span v-if="user.admin" class="premium-audience-badge">{{ copy.adminAccess }}</span><span v-else-if="user.authorized" class="premium-audience-badge">{{ copy.alreadyGranted }}</span><button v-else type="button" :disabled="busy || loading || !!loadError" @click="grant(user)">{{ copy.grant }}</button></li>
          </ul>
        </section>
        <section class="premium-audience-section" aria-labelledby="premium-current-listeners">
          <h3 id="premium-current-listeners">{{ copy.currentListeners }}</h3>
          <p v-if="loading" class="life-dialog-note" role="status">{{ copy.loading }}</p>
          <div v-else-if="loadError"><p class="life-dialog-error" role="alert">{{ loadError }}</p><button type="button" @click="loadListeners">{{ copy.retry }}</button></div>
          <p v-else-if="!listeners.length" class="life-dialog-note">{{ copy.noListeners }}</p>
          <ul v-else class="premium-audience-list"><li v-for="listener in listeners" :key="listener.userId"><span>{{ listener.username }}</span><button type="button" :disabled="busy" :aria-label="copy.revoke + ' ' + listener.username" @click="revoke(listener.userId)">{{ copy.revoke }}</button></li></ul>
        </section>
        <p v-if="error" class="life-dialog-error" role="alert">{{ error }}</p><p v-if="status" class="premium-audience-status" role="status">{{ status }}</p>
        <div class="life-dialog-actions"><button type="button" class="life-dialog-primary" :disabled="busy" @click="close">{{ copy.close }}</button></div>
      </div>
    </dialog>
  </Teleport>
</template>
