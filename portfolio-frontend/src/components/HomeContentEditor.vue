<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import { homeUi } from '../locales/homepage'
import { getHomeContent, getListening, getQuotes, saveProfile, saveListening, saveQuote, siteLanguage, siteLanguages } from '../services/homepage'
import type { EditorSection, HomeTranslation, Listening, Quote, QuoteDraft, SiteLanguage } from '../services/homepage'

const emit = defineEmits<{ updated: [section: EditorSection] }>()
const { t, locale } = useI18n()
const copy = computed(() => homeUi[siteLanguage(locale.value)])
const dialog = ref<HTMLDialogElement | null>(null)
const opened = ref(false)
const section = ref<EditorSection>('profile')
const language = ref<SiteLanguage>('zh')
const loading = ref(false)
const ready = ref(false)
const saving = ref(false)
const dirty = ref(false)
const error = ref('')
const success = ref('')
const pendingAction = ref<(() => void) | null>(null)
const blankTranslation = (): HomeTranslation => ({ desc1: '', desc2: '', contactPublic: '', contactProf: '' })
const translations = reactive<Record<SiteLanguage, HomeTranslation>>({ zh: blankTranslation(), en: blankTranslation(), ja: blankTranslation() })
const listening = reactive<Listening>({ artist: '', title: '', url: '', enabled: false })
const quotes = ref<Quote[]>([])
const blankQuote = (): QuoteDraft => ({ content: '', source: '', language: null, enabled: true })
const quote = ref<QuoteDraft>(blankQuote())
const fields = ['desc1', 'desc2', 'contactPublic', 'contactProf'] as const

function fallbackTranslation(lang: SiteLanguage): HomeTranslation {
  return Object.fromEntries(fields.map(field => [field, t(`home.${field}`, {}, { locale: lang })])) as unknown as HomeTranslation
}
function requestAction(action: () => void) {
  if (saving.value || loading.value) return
  if (dirty.value) pendingAction.value = action
  else action()
}
function discardChanges() {
  const action = pendingAction.value
  pendingAction.value = null
  dirty.value = false
  action?.()
}
function close() { dialog.value?.close() }
function requestClose() { requestAction(close) }
function setSection(value: EditorSection) {
  requestAction(() => {
    section.value = value
    // Reload before editing another section so discarded edits never leak into a later save.
    void load()
  })
}
function changed() { dirty.value = true; error.value = ''; success.value = '' }
function report(cause: unknown) {
  error.value = axios.isAxiosError(cause) && [401, 403].includes(cause.response?.status || 0)
    ? copy.value.forbidden : copy.value.saveFailed
}
async function load() {
  loading.value = true
  ready.value = false
  error.value = ''
  success.value = ''
  try {
    const [home, song, list] = await Promise.all([getHomeContent(), getListening(), getQuotes()])
    for (const lang of siteLanguages) Object.assign(translations[lang.value], home.translations[lang.value] ?? fallbackTranslation(lang.value))
    Object.assign(listening, song)
    quotes.value = list
    quote.value = blankQuote()
    dirty.value = false
    ready.value = true
  } catch (cause) {
    report(cause)
    if (error.value !== copy.value.forbidden) error.value = copy.value.loadFailed
  } finally { loading.value = false }
}
async function open(value: EditorSection = 'profile') {
  section.value = value
  language.value = siteLanguage(locale.value)
  dirty.value = false
  pendingAction.value = null
  opened.value = true
  dialog.value?.showModal()
  await load()
}
defineExpose({ open })

async function submit() {
  if (!ready.value || saving.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    if (section.value === 'profile') await saveProfile(translations)
    else if (section.value === 'listening') await saveListening({ ...listening })
    else {
      const saved = await saveQuote(quote.value)
      quotes.value = [saved, ...quotes.value.filter(item => item.id !== saved.id)].sort((a, b) => b.id - a.id)
      quote.value = blankQuote()
    }
    dirty.value = false
    success.value = copy.value.saved
    emit('updated', section.value)
    if (section.value !== 'quotes') close()
  } catch (cause) { report(cause) }
  finally { saving.value = false }
}
function editQuote(value?: Quote) {
  requestAction(() => {
    quote.value = value ? { ...value } : blankQuote()
    error.value = ''
    success.value = ''
    dialog.value?.querySelector<HTMLTextAreaElement>('#quote-content')?.focus()
  })
}
function toggleQuote(value: Quote) {
  requestAction(() => { void (async () => {
    saving.value = true
    error.value = ''
    try {
      const saved = await saveQuote({ ...value, enabled: !value.enabled })
      quotes.value = quotes.value.map(item => item.id === saved.id ? saved : item)
      quote.value = blankQuote()
      success.value = copy.value.saved
      emit('updated', 'quotes')
    } catch (cause) { report(cause) }
    finally { saving.value = false }
  })() })
}
function beforeUnload(event: BeforeUnloadEvent) {
  if (opened.value && dirty.value) { event.preventDefault(); event.returnValue = '' }
}
onBeforeRouteLeave(() => !opened.value || !dirty.value || window.confirm(copy.value.discard))
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onUnmounted(() => { window.removeEventListener('beforeunload', beforeUnload); close() })
</script>

<template>
  <Teleport to="body">
    <dialog ref="dialog" class="home-editor" aria-labelledby="home-editor-title" @cancel.prevent="requestClose" @click.self="requestClose" @close="opened = false; pendingAction = null">
      <div class="editor-shell">
        <header class="editor-header">
          <h2 id="home-editor-title">{{ copy.editorTitle }}</h2>
          <button type="button" class="editor-close" :aria-label="copy.close" :disabled="loading || saving" @click="requestClose">×</button>
        </header>
        <nav class="editor-sections" :aria-label="copy.editorTitle">
          <button v-for="value in (['profile', 'quotes', 'listening'] as const)" :key="value" type="button" :aria-pressed="section === value" :disabled="loading || saving" @click="setSection(value)">{{ copy[value] }}</button>
        </nav>
        <p v-if="loading" class="editor-status" role="status">{{ copy.loading }}</p>
        <p v-if="error" class="editor-error" role="alert">{{ error }}</p>
        <p v-if="success" class="editor-status" role="status">{{ success }}</p>
        <div v-if="pendingAction" class="editor-discard" role="alert">
          <p>{{ copy.discard }}</p>
          <button type="button" @click="pendingAction = null">{{ copy.keepEditing }}</button>
          <button type="button" @click="discardChanges">{{ copy.discardChanges }}</button>
        </div>
        <form v-if="ready" class="editor-form" @submit.prevent="submit" @input="changed" @change="changed">
          <fieldset :disabled="saving">
            <template v-if="section === 'profile'">
              <p class="editor-hint">{{ copy.profileHint }}</p>
              <div class="editor-languages">
                <button v-for="lang in siteLanguages" :key="lang.value" type="button" :aria-pressed="language === lang.value" @click="language = lang.value">{{ lang.label }}</button>
              </div>
              <div v-for="field in fields" :key="field" class="editor-field">
                <label :for="`home-${field}`">{{ copy[field] }}</label>
                <textarea :id="`home-${field}`" v-model="translations[language][field]" :lang="language" :rows="field.startsWith('contact') ? 2 : 3" maxlength="5000" />
              </div>
            </template>
            <template v-else-if="section === 'quotes'">
              <p class="editor-hint">{{ copy.quoteHint }}</p>
              <div class="quote-editor-heading"><h3>{{ quote.id ? copy.editQuote : copy.newQuote }}</h3><button v-if="quote.id" type="button" class="editor-text-button" @click="editQuote()">{{ copy.newQuote }}</button></div>
              <div class="editor-field"><label for="quote-content">{{ copy.quoteContent }}</label><textarea id="quote-content" v-model="quote.content" rows="3" maxlength="500" required /></div>
              <div class="editor-grid">
                <div class="editor-field"><label for="quote-source">{{ copy.source }}</label><input id="quote-source" v-model="quote.source" maxlength="120" /></div>
                <div class="editor-field"><label for="quote-language">{{ copy.language }}</label><select id="quote-language" v-model="quote.language"><option :value="null">{{ copy.allLanguages }}</option><option v-for="lang in siteLanguages" :key="lang.value" :value="lang.value">{{ lang.label }}</option></select></div>
              </div>
              <label class="editor-checkbox"><input v-model="quote.enabled" type="checkbox" />{{ copy.enabled }}</label>
              <div class="quote-list">
                <p v-if="!quotes.length" class="editor-hint">{{ copy.noQuotes }}</p>
                <article v-for="item in quotes" :key="item.id" class="quote-list-item" :class="{ 'quote-list-item--disabled': !item.enabled }">
                  <p>{{ item.content }}</p><small>{{ item.source }}{{ item.source ? ' · ' : '' }}{{ siteLanguages.find(lang => lang.value === item.language)?.label || copy.allLanguages }} · {{ item.enabled ? copy.enabled : copy.disabled }}</small>
                  <div><button type="button" class="editor-text-button" @click="editQuote(item)">{{ copy.editQuote }}</button><button type="button" class="editor-text-button" @click="toggleQuote(item)">{{ item.enabled ? copy.disable : copy.enable }}</button></div>
                </article>
              </div>
            </template>
            <template v-else>
              <p class="editor-hint">{{ copy.listeningHint }}</p>
              <label class="editor-checkbox"><input v-model="listening.enabled" type="checkbox" />{{ copy.showListening }}</label>
              <div class="editor-field"><label for="listening-artist">{{ copy.artist }}</label><input id="listening-artist" v-model="listening.artist" maxlength="120" :required="listening.enabled" /></div>
              <div class="editor-field"><label for="listening-title">{{ copy.songTitle }}</label><input id="listening-title" v-model="listening.title" maxlength="200" :required="listening.enabled" /></div>
              <div class="editor-field"><label for="listening-url">{{ copy.songUrl }}</label><input id="listening-url" v-model="listening.url" type="url" maxlength="2000" placeholder="https://music.163.com/#/song?id=…" :required="listening.enabled" /></div>
            </template>
          </fieldset>
          <footer class="editor-footer"><button type="button" :disabled="saving" @click="requestClose">{{ copy.cancel }}</button><button type="submit" class="editor-save" :disabled="saving || !dirty">{{ saving ? copy.saving : copy.save }}</button></footer>
        </form>
      </div>
    </dialog>
  </Teleport>
</template>

<style scoped>
.home-editor { --site-focus: #2857a5; width: min(760px, calc(100% - 32px)); max-height: 90svh; margin: auto; padding: 0; border: 1px solid #d4deec; border-radius: 10px; background: #fff; color: #214777; font-family: var(--font-sans); }
.home-editor::backdrop { background: rgb(15 30 57 / 58%); }
.editor-shell { padding: 28px 32px; }
.editor-header, .quote-editor-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.editor-header h2 { margin: 0; font-size: 22px; font-weight: 600; }
.editor-close { font-size: 28px; line-height: 1; padding: 6px 10px; }
button { border: 0; background: transparent; color: inherit; border-radius: 4px; padding: 8px 12px; font-size: 13px; }
button:hover { background: #edf3fc; }
button:disabled { opacity: .5; cursor: default; }
.editor-sections { display: flex; gap: 10px; border-bottom: 1px solid #dce5f1; margin: 18px 0; padding-bottom: 10px; flex-wrap: wrap; }
.editor-sections button[aria-pressed="true"], .editor-languages button[aria-pressed="true"] { background: #e8f0fc; color: #2857a5; font-weight: 600; }
.editor-hint { color: #4a6382; font-size: 12px; line-height: 1.9; margin: 0 0 20px; }
.editor-languages { display: flex; gap: 8px; margin-bottom: 20px; }
fieldset { border: 0; padding: 0; margin: 0; min-width: 0; }
.editor-field { margin-bottom: 18px; }
.editor-field label { display: block; font-size: 13px; margin-bottom: 8px; }
.editor-field input, .editor-field textarea, .editor-field select { width: 100%; padding: 10px 12px; background: #f6f8fc; border: 1px solid #c6d4e8; border-radius: 4px; color: #214777; font-size: 14px; line-height: 1.7; }
.editor-field textarea { resize: vertical; min-height: 80px; }
.editor-checkbox { display: flex; align-items: center; gap: 9px; font-size: 13px; margin: 0 0 22px; }
.editor-checkbox input { accent-color: #2857a5; }
.editor-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.editor-footer { display: flex; justify-content: flex-end; gap: 12px; padding-top: 22px; margin-top: 10px; border-top: 1px solid #dce5f1; }
.editor-footer .editor-save { background: #2857a5; color: #fff; padding: 10px 22px; }
.editor-status, .editor-error { padding: 10px 12px; border-radius: 4px; font-size: 13px; line-height: 1.7; }
.editor-status { background: #edf3fc; }
.editor-error { background: #fff0ef; color: #9b332e; }
.editor-discard { padding: 12px; background: #fff6df; color: #735514; font-size: 13px; margin-bottom: 16px; }
.editor-discard p { margin: 0 0 8px; }
.quote-editor-heading { margin-bottom: 14px; }
.quote-editor-heading h3 { font-size: 15px; font-weight: 500; }
.quote-list { max-height: 260px; overflow-y: auto; border-top: 1px solid #dce5f1; }
.quote-list-item { padding: 16px 0; border-bottom: 1px solid #e6edf6; }
.quote-list-item p { white-space: pre-wrap; overflow-wrap: anywhere; margin: 0 0 6px; font-size: 14px; line-height: 1.8; }
.quote-list-item small { font-size: 11px; color: #4a6382; }
.quote-list-item--disabled p { color: #607089; }
.editor-text-button { padding: 5px 8px; margin: 6px 6px 0 -8px; font-size: 12px; text-decoration: underline; text-underline-offset: 3px; }
@media (max-width: 640px) { .editor-shell { padding: 22px 18px; } .editor-grid { grid-template-columns: 1fr; gap: 0; } .editor-sections { gap: 2px; } .editor-sections button { padding: 8px; font-size: 12px; } }
</style>
