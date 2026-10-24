<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import { aboutUi } from '../locales/about'
import { getAboutContent, saveAboutProfile, saveFriendLink } from '../services/about'
import type { AboutContent, AboutTranslation, FriendLink } from '../services/about'
import { siteLanguage, siteLanguages } from '../services/homepage'
import type { SiteLanguage } from '../services/homepage'
import { friendLinkUrl } from '../utils/about'
import '../styles/life.css'

type EditorKind = 'profile' | 'link'
const emit = defineEmits<{ profileSaved: [content: AboutContent]; linkSaved: [link: FriendLink] }>()
const { t, locale } = useI18n()
const copy = computed(() => aboutUi[siteLanguage(locale.value)])
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const dialog = ref<HTMLDialogElement | null>(null)
const opened = ref(false)
const kind = ref<EditorKind>('profile')
const activeLinkId = ref<number>()
const language = ref<SiteLanguage>('zh')
const loading = ref(false)
const ready = ref(false)
const saving = ref(false)
const error = ref('')
const pendingDiscard = ref(false)
const blankTranslation = (): AboutTranslation => ({ title: '', purpose1: '', purpose2: '' })
const translations = reactive<Record<SiteLanguage, AboutTranslation>>({ zh: blankTranslation(), en: blankTranslation(), ja: blankTranslation() })
const baselineTranslations = ref('')
const link = reactive({ name: '', url: '', description: '' })
const baselineLink = ref('')
const dirty = computed(() => ready.value && (kind.value === 'profile'
  ? JSON.stringify(translations) !== baselineTranslations.value : JSON.stringify(link) !== baselineLink.value))
let leaveResolver: ((leave: boolean) => void) | null = null
let loadRequest = 0

watch(pendingDiscard, async visible => {
  if (!visible) return
  await nextTick()
  if (pendingDiscard.value) dialog.value?.querySelector<HTMLButtonElement>('.about-discard .life-dialog-primary')?.focus()
})

function fallback(lang: SiteLanguage): AboutTranslation {
  return { title: t('about.title', {}, { locale: lang }), purpose1: t('about.purpose1', {}, { locale: lang }), purpose2: t('about.purpose2', {}, { locale: lang }) }
}
function report(cause: unknown, fallbackMessage: string) {
  return axios.isAxiosError(cause) && [401, 403].includes(cause.response?.status || 0) ? copy.value.forbidden : fallbackMessage
}
async function load() {
  const request = ++loadRequest
  loading.value = true
  ready.value = false
  error.value = ''
  try {
    const content = await getAboutContent()
    if (request !== loadRequest) return
    for (const lang of siteLanguages) Object.assign(translations[lang.value], content.translations[lang.value] ?? fallback(lang.value))
    const existing = activeLinkId.value === undefined ? undefined : content.links.find(item => item.id === activeLinkId.value)
    if (kind.value === 'link' && activeLinkId.value !== undefined && !existing) throw new Error('Link no longer exists')
    Object.assign(link, { name: existing?.name ?? '', url: existing?.url ?? '', description: existing?.description ?? '' })
    baselineTranslations.value = JSON.stringify(translations)
    baselineLink.value = JSON.stringify(link)
    ready.value = true
  } catch (cause) { if (request === loadRequest) error.value = report(cause, copy.value.loadFailed) }
  finally { if (request === loadRequest) loading.value = false }
}
async function open(value: EditorKind, id?: number) {
  if (!isAdmin.value || opened.value) return
  kind.value = value
  activeLinkId.value = id
  language.value = siteLanguage(locale.value)
  pendingDiscard.value = false
  opened.value = true
  dialog.value?.showModal()
  await load()
}
defineExpose({ open })
function requestClose() {
  if (saving.value || loading.value) return
  if (dirty.value) pendingDiscard.value = true
  else dialog.value?.close()
}
function keepEditing() {
  pendingDiscard.value = false
  const resolve = leaveResolver
  leaveResolver = null
  resolve?.(false)
}
function discardChanges() {
  pendingDiscard.value = false
  const resolve = leaveResolver
  leaveResolver = null
  dialog.value?.close()
  resolve?.(true)
}
function closed() {
  opened.value = false
  pendingDiscard.value = false
  const resolve = leaveResolver
  leaveResolver = null
  resolve?.(false)
}
async function submit() {
  if (!isAdmin.value || !ready.value || saving.value || pendingDiscard.value || !dirty.value) return
  if (kind.value === 'profile' && siteLanguages.some(lang => !translations[lang.value].title.trim())) {
    error.value = copy.value.invalidProfile
    return
  }
  const normalizedUrl = friendLinkUrl(link.url.trim())
  if (kind.value === 'link' && (!link.name.trim() || !normalizedUrl)) {
    error.value = copy.value.invalidLink
    return
  }
  saving.value = true
  error.value = ''
  try {
    if (kind.value === 'profile') {
      emit('profileSaved', await saveAboutProfile(translations))
      baselineTranslations.value = JSON.stringify(translations)
    } else {
      emit('linkSaved', await saveFriendLink({ ...link, url: normalizedUrl!, id: activeLinkId.value }))
      baselineLink.value = JSON.stringify(link)
    }
    dialog.value?.close()
  } catch (cause) { error.value = report(cause, copy.value.saveFailed) }
  finally { saving.value = false }
}
onBeforeRouteLeave(() => {
  if (!opened.value) return true
  if (saving.value || loading.value) return false
  if (!dirty.value) return true
  leaveResolver?.(false)
  return new Promise<boolean>(resolve => { leaveResolver = resolve; pendingDiscard.value = true })
})
function beforeUnload(event: BeforeUnloadEvent) {
  if (opened.value && (dirty.value || saving.value)) { event.preventDefault(); event.returnValue = '' }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onUnmounted(() => { loadRequest++; window.removeEventListener('beforeunload', beforeUnload); dialog.value?.close(); leaveResolver?.(false) })
</script>

<template>
  <Teleport to="body">
    <dialog ref="dialog" class="life-dialog about-editor" aria-labelledby="about-editor-title" @cancel.prevent="requestClose" @click.self="requestClose" @close="closed">
      <div class="life-dialog-content">
        <header class="life-dialog-header"><h2 id="about-editor-title">{{ kind === 'profile' ? copy.editProfile : activeLinkId === undefined ? copy.addLink : copy.editLink }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" :disabled="loading || saving" @click="requestClose">×</button></header>
        <p v-if="loading" class="life-dialog-note" role="status">{{ copy.loading }}</p>
        <p v-if="error" class="life-dialog-error" role="alert">{{ error }}</p>
        <button v-if="!ready && !loading" type="button" @click="load">{{ copy.retry }}</button>
        <div v-if="pendingDiscard" class="about-discard" role="alert"><h3>{{ copy.discardTitle }}</h3><p>{{ copy.discardHint }}</p><div class="life-dialog-actions"><button type="button" @click="discardChanges">{{ copy.discard }}</button><button type="button" class="life-dialog-primary" @click="keepEditing">{{ copy.keepEditing }}</button></div></div>
        <form v-if="ready" @submit.prevent="submit" :aria-busy="saving">
          <fieldset :disabled="saving || pendingDiscard">
            <template v-if="kind === 'profile'">
              <p class="about-editor-hint">{{ copy.profileHint }}</p>
              <div class="about-languages"><button v-for="lang in siteLanguages" :key="lang.value" type="button" :aria-pressed="language === lang.value" @click="language = lang.value">{{ lang.label }}</button></div>
              <label for="about-title">{{ copy.title }}</label><input id="about-title" v-model="translations[language].title" :lang="language" maxlength="120" required />
              <label for="about-purpose1">{{ copy.purpose1 }}</label><textarea id="about-purpose1" v-model="translations[language].purpose1" :lang="language" rows="5" maxlength="10000"></textarea>
              <label for="about-purpose2">{{ copy.purpose2 }}</label><textarea id="about-purpose2" v-model="translations[language].purpose2" :lang="language" rows="5" maxlength="10000"></textarea>
            </template>
            <template v-else>
              <p class="about-editor-hint">{{ copy.linkHint }}</p>
              <label for="friend-name">{{ copy.name }}</label><input id="friend-name" v-model="link.name" maxlength="120" required />
              <label for="friend-url">{{ copy.url }}</label><input id="friend-url" v-model="link.url" type="url" maxlength="2000" placeholder="https://…" required />
              <label for="friend-description">{{ copy.description }}</label><textarea id="friend-description" v-model="link.description" maxlength="500" rows="3"></textarea>
            </template>
          </fieldset>
          <footer class="life-dialog-actions"><button type="button" :disabled="saving || pendingDiscard" @click="requestClose">{{ copy.cancel }}</button><button type="submit" class="life-dialog-primary" :disabled="saving || !dirty || pendingDiscard">{{ saving ? copy.saving : copy.save }}</button></footer>
        </form>
      </div>
    </dialog>
  </Teleport>
</template>

<style scoped>
.about-editor { width: min(740px, calc(100% - 32px)); }
.about-editor fieldset { border: 0; margin: 0; padding: 0; min-width: 0; }
.about-editor fieldset:disabled { opacity: .6; }
.about-editor label { display: block; font-size: 12px; font-weight: 500; margin: 20px 0 8px; }
.about-editor input, .about-editor textarea { display: block; width: 100%; padding: 10px 12px; border: 1px solid #a7bbd8; border-radius: 4px; color: #214777; background: #f8faff; font-size: 14px; line-height: 1.8; }
.about-editor textarea { resize: vertical; min-height: 90px; }
.about-editor .about-editor-hint { color: #4a6382; font-size: 12px; margin: 0 0 20px; line-height: 1.9; }
.about-languages { display: flex; gap: 8px; flex-wrap: wrap; border-bottom: 1px solid #dce5f1; padding-bottom: 10px; margin-bottom: 22px; }
.about-languages button[aria-pressed="true"] { background: #e8f0fc; color: #2857a5; font-weight: 600; }
.about-discard { padding: 16px 18px; margin: 20px 0; border: 1px solid #b9cce5; border-radius: 4px; background: #edf3fc; }
.about-discard h3 { font-size: 15px; font-weight: 600; margin: 0; }
.about-discard p { font-size: 12px; color: #4a6382; }
.about-discard .life-dialog-actions { margin-top: 14px; }
</style>
