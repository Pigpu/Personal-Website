<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import axios from 'axios'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import AboutEditor from '../components/AboutEditor.vue'
import { aboutUi } from '../locales/about'
import { deleteFriendLink, getAboutContent } from '../services/about'
import type { AboutContent, FriendLink } from '../services/about'
import { siteLanguage } from '../services/homepage'
import { friendLinkHost, friendLinkUrl } from '../utils/about'
import '../styles/life.css'

const { t, locale } = useI18n()
const copy = computed(() => aboutUi[siteLanguage(locale.value)])
const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN')
const content = ref<AboutContent>({ translations: {}, links: [] })
const translation = computed(() => content.value.translations[siteLanguage(locale.value)] ?? {
  title: t('about.title'), purpose1: t('about.purpose1'), purpose2: t('about.purpose2'),
})
const loading = ref(true)
const error = ref('')
const editor = ref<InstanceType<typeof AboutEditor> | null>(null)
const deleteDialog = ref<HTMLDialogElement | null>(null)
const linkToDelete = ref<FriendLink | null>(null)
const deleting = ref(false)
const deleteError = ref('')
let requestNumber = 0
const socialLinks = [
  { name: 'X (Twitter)', url: 'https://x.com/Kaede__P', icon: 'x' },
  { name: 'YouTube', url: 'https://youtube.com/@KaedeP0111', icon: 'youtube' },
  { name: 'Bilibili', url: 'https://space.bilibili.com/26553865', icon: 'bilibili' },
]

async function load() {
  const request = ++requestNumber
  loading.value = true
  error.value = ''
  try {
    const result = await getAboutContent()
    if (request === requestNumber) content.value = result
  } catch { if (request === requestNumber) error.value = copy.value.loadFailed }
  finally { if (request === requestNumber) loading.value = false }
}
function profileSaved(value: AboutContent) {
  requestNumber++
  content.value = value
  loading.value = false
  error.value = ''
}
function linkSaved(value: FriendLink) {
  requestNumber++
  content.value.links = [...content.value.links.filter(link => link.id !== value.id), value].sort((a, b) => a.id - b.id)
  loading.value = false
  error.value = ''
}
function askDelete(value: FriendLink) {
  if (!isAdmin.value) return
  linkToDelete.value = value
  deleteError.value = ''
  deleteDialog.value?.showModal()
}
function closeDelete() { if (!deleting.value) deleteDialog.value?.close() }
async function removeLink() {
  if (!isAdmin.value || !linkToDelete.value || deleting.value) return
  const id = linkToDelete.value.id
  deleting.value = true
  deleteError.value = ''
  try {
    await deleteFriendLink(id)
    requestNumber++
    content.value.links = content.value.links.filter(link => link.id !== id)
    loading.value = false
    error.value = ''
    deleteDialog.value?.close()
  } catch (cause) {
    deleteError.value = axios.isAxiosError(cause) && [401, 403].includes(cause.response?.status || 0) ? copy.value.forbidden : copy.value.deleteFailed
  } finally { deleting.value = false }
}
onMounted(load)
onUnmounted(() => { requestNumber++ })
</script>

<template>
  <div class="life-scene">
    <FixedSiteBackground />
    <div class="life-page about-page">
      <header class="life-header">
        <div><p class="life-eyebrow">A LITTLE ABOUT ME</p><h1 class="life-title">{{ translation.title }}</h1></div>
        <button v-if="isAdmin" type="button" class="life-action" @click="editor?.open('profile')">{{ copy.editProfile }}</button>
      </header>
      <section class="about-introduction" :aria-label="translation.title">
        <p v-if="translation.purpose1">{{ translation.purpose1 }}</p>
        <p v-if="translation.purpose2">{{ translation.purpose2 }}</p>
      </section>
      <section class="about-section" aria-labelledby="friend-links-heading">
        <header class="about-section-header"><div><h2 id="friend-links-heading">{{ copy.friends }}</h2><p class="about-section-hint">{{ copy.friendsHint }}</p></div><button v-if="isAdmin" type="button" class="life-action" @click="editor?.open('link')"><span aria-hidden="true">＋</span>{{ copy.addLink }}</button></header>
        <p v-if="loading" class="about-state" role="status">{{ copy.loading }}</p>
        <div v-else-if="error" class="about-state" role="alert"><p>{{ error }}</p><button type="button" class="life-action" @click="load">{{ copy.retry }}</button></div>
        <p v-else-if="!content.links.length" class="about-state">{{ copy.empty }}</p>
        <ul v-else class="friend-grid">
          <li v-for="link in content.links" :key="link.id" class="friend-card">
            <a :href="friendLinkUrl(link.url) || undefined" :aria-disabled="!friendLinkUrl(link.url)" target="_blank" rel="noopener noreferrer" class="friend-link" :title="copy.newWindow">
              <div class="friend-heading"><h3>{{ link.name }}</h3><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 17 17 7M6 7h11v11" /></svg></div>
              <p v-if="link.description" class="friend-description">{{ link.description }}</p>
              <span class="friend-domain">{{ friendLinkHost(link.url) }}</span>
            </a>
            <div v-if="isAdmin" class="friend-admin"><button type="button" class="life-inline-button" @click="editor?.open('link', link.id)">{{ copy.edit }}</button><button type="button" class="life-inline-button" @click="askDelete(link)">{{ copy.remove }}</button></div>
          </li>
        </ul>
      </section>
      <div class="about-bottom">
        <section aria-labelledby="about-contact-heading">
          <h2 id="about-contact-heading">{{ t('about.contactTitle') }}</h2>
          <p class="about-contact-description">{{ t('about.contactDesc') }}</p>
          <a href="mailto:kaedep0828@gmail.com" class="about-email">kaedep0828@gmail.com <span aria-hidden="true">↗</span></a>
        </section>
        <section aria-labelledby="about-social-heading">
          <h2 id="about-social-heading">{{ t('about.other') }}</h2>
          <div class="about-social-links">
            <a v-for="social in socialLinks" :key="social.name" :href="social.url" target="_blank" rel="noopener noreferrer" :title="copy.newWindow">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path v-if="social.icon === 'x'" d="m5 4 14 16M19 4 5 20" /><template v-else-if="social.icon === 'youtube'"><rect x="2.5" y="5" width="19" height="14" rx="4" /><path d="m10 9 6 3-6 3Z" /></template><template v-else><rect x="3" y="7" width="18" height="13" rx="3" /><path d="m7 3 4 4m6-4-4 4M8 12v3m8-3v3" /></template></svg>
              {{ social.name }}<span class="social-arrow" aria-hidden="true">↗</span>
            </a>
          </div>
        </section>
      </div>
    </div>
    <AboutEditor v-if="isAdmin" ref="editor" @profile-saved="profileSaved" @link-saved="linkSaved" />
    <Teleport to="body">
      <dialog ref="deleteDialog" class="life-dialog" aria-labelledby="delete-friend-title" @click.self="closeDelete" @cancel="deleting && $event.preventDefault()" @close="linkToDelete = null">
        <div class="life-dialog-content"><header class="life-dialog-header"><h2 id="delete-friend-title">{{ copy.deleteTitle }}</h2><button type="button" class="life-dialog-close" :aria-label="copy.close" :disabled="deleting" @click="closeDelete">×</button></header><p>{{ linkToDelete?.name }}</p><p class="life-dialog-note">{{ copy.deleteHint }}</p><p v-if="deleteError" class="life-dialog-error" role="alert">{{ deleteError }}</p><footer class="life-dialog-actions"><button type="button" :disabled="deleting" @click="closeDelete">{{ copy.cancel }}</button><button type="button" class="life-dialog-danger" :disabled="deleting" @click="removeLink">{{ deleting ? copy.deleting : copy.confirmDelete }}</button></footer></div>
      </dialog>
    </Teleport>
  </div>
</template>

<style scoped>
.about-introduction { max-width: 850px; margin-top: 34px; }
.about-introduction p { margin: 0 0 23px; font-size: 15px; line-height: 2.1; color: #f1f5ff; white-space: pre-wrap; overflow-wrap: anywhere; }
.about-introduction p:last-child { margin-bottom: 0; }
.about-section { margin-top: 52px; border-top: 1px solid rgb(255 255 255 / 28%); padding-top: 36px; }
.about-section-header { display: flex; justify-content: space-between; align-items: center; gap: 22px; margin-bottom: 25px; }
.about-page h2 { font-size: 22px; line-height: 1.6; font-weight: 500; letter-spacing: -.02em; margin: 0; }
.about-section-hint { color: #e0e9f8; font-size: 13px; line-height: 1.9; margin: 9px 0 0; }
.about-state { font-size: 13px; color: #e0e9f8; line-height: 1.9; margin: 24px 0 0; }
.about-state p { margin: 0 0 15px; }
.friend-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; padding: 0; margin: 0; list-style: none; }
.friend-card { min-width: 0; display: flex; flex-direction: column; border: 1px solid rgb(255 255 255 / 15%); border-radius: 4px; background: #3261aa; transition: background-color .2s, border-color .2s; }
.friend-card:hover { background: #3666b0; border-color: rgb(255 255 255 / 28%); }
.friend-link { color: #fff; display: flex; flex: 1; flex-direction: column; padding: 21px 22px; text-decoration: none; border-radius: 4px; }
.friend-link:focus-visible { outline-offset: -3px; }
.friend-heading { display: flex; align-items: start; justify-content: space-between; gap: 15px; }
.friend-heading h3 { font-size: 18px; line-height: 1.6; font-weight: 500; margin: 0; overflow-wrap: anywhere; }
.friend-link:hover h3 { text-decoration: underline; text-underline-offset: 4px; }
.friend-heading svg { width: 20px; height: 20px; flex-shrink: 0; stroke: currentColor; fill: none; stroke-width: 1.4; margin-top: 3px; }
.friend-description { font-size: 12px; line-height: 1.85; color: #e0e9f8; margin: 10px 0 0; white-space: pre-wrap; overflow-wrap: anywhere; }
.friend-domain { font-size: 10px; letter-spacing: .02em; color: #e0e9f8; margin-top: auto; padding-top: 21px; overflow-wrap: anywhere; }
.friend-admin { display: flex; justify-content: flex-end; gap: 2px; padding: 0 10px 8px; }
.friend-admin button { font-size: 11px; color: #e0e9f8; }
.about-bottom { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 55px; margin-top: 48px; padding-top: 36px; border-top: 1px solid rgb(255 255 255 / 28%); }
.about-bottom h2 { font-size: 19px; }
.about-contact-description { color: #e0e9f8; font-size: 13px; line-height: 1.95; margin: 16px 0 13px; }
.about-email { display: inline-flex; gap: 15px; color: #fff; text-decoration: none; font-size: 14px; line-height: 1.9; border-bottom: 1px solid rgb(255 255 255 / 45%); padding-bottom: 5px; }
.about-email:hover { border-color: #fff; }
.about-social-links { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 21px; }
.about-social-links a { display: inline-flex; align-items: center; gap: 9px; color: #fff; font-size: 12px; text-decoration: none; border: 1px solid rgb(255 255 255 / 35%); border-radius: 4px; padding: 10px 12px; transition: background-color .2s; }
.about-social-links a:hover { background: rgb(255 255 255 / 10%); }
.about-social-links svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 1.4; stroke-linecap: round; stroke-linejoin: round; }
.social-arrow { font-size: 13px; padding-left: 4px; }
@media (max-width: 1000px) { .friend-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .about-bottom { gap: 35px; } }
@media (max-width: 640px) { .about-introduction { margin-top: 28px; } .about-introduction p { font-size: 14px; } .about-section { margin-top: 38px; padding-top: 29px; } .about-section-header { flex-wrap: wrap; align-items: flex-start; gap: 18px; } .friend-grid { grid-template-columns: 1fr; gap: 16px; } .about-bottom { grid-template-columns: 1fr; gap: 32px; margin-top: 36px; padding-top: 30px; } .about-page h2 { font-size: 21px; } .about-bottom h2 { font-size: 19px; } }
</style>
