<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import axios from 'axios'
import { useI18n } from 'vue-i18n'
import FixedSiteBackground from '../components/FixedSiteBackground.vue'
import { authUi } from '../locales/auth'
import '../styles/life.css'

const { t, locale } = useI18n()
const copy = computed(() => authUi[locale.value === 'en' ? 'en' : locale.value === 'ja' ? 'ja' : 'zh'])
const isLogin = ref(true)
const submitting = ref(false)
const completing = ref(false)
const busy = computed(() => submitting.value || completing.value)
const form = ref({ username: '', password: '', captcha: '', uuid: '' })
const captchaImg = ref('')
const captchaLoading = ref(false)
const captchaFailed = ref(false)
const notice = ref<'login' | 'register' | 'error' | null>(null)
const message = ref({ title: '', content: '' })
const noticeDialog = ref<HTMLDialogElement | null>(null)
let transitionTimer: ReturnType<typeof setTimeout> | undefined
let disposed = false

const noticeTitle = computed(() => notice.value === 'login' ? t('login.successLoginTitle')
  : notice.value === 'register' ? t('login.successRegTitle') : message.value.title)
const noticeDescription = computed(() => notice.value === 'login' ? t('login.successLoginDesc')
  : notice.value === 'register' ? t('login.successRegDesc') : message.value.content)

watch(notice, (value) => {
  if (value && !noticeDialog.value?.open) noticeDialog.value?.showModal()
  else if (!value) noticeDialog.value?.close()
}, { flush: 'post' })

const showMessage = (title: string, content: string) => {
  message.value = { title, content }
  notice.value = 'error'
}

const fetchCaptcha = async () => {
  if (captchaLoading.value) return
  captchaLoading.value = true
  captchaFailed.value = false
  captchaImg.value = ''
  form.value.uuid = ''
  try {
    const response = await axios.get('/api/auth/captcha')
    if (disposed) return
    form.value.uuid = response.data.uuid
    captchaImg.value = response.data.img
  } catch {
    if (!disposed) captchaFailed.value = true
  } finally {
    if (!disposed) captchaLoading.value = false
  }
}

watch(isLogin, (login) => {
  if (!login) {
    form.value.captcha = ''
    fetchCaptcha()
  }
})

const handleSubmit = async () => {
  if (busy.value) return
  const login = isLogin.value
  if (!login && !form.value.captcha) {
    showMessage(t('login.captcha'), t('login.msgInputCaptcha'))
    return
  }
  submitting.value = true
  const payload = login ? { username: form.value.username, password: form.value.password } : { ...form.value }
  try {
    const response = await axios.post(login ? '/api/auth/login' : '/api/auth/register', payload)
    if (disposed) return
    completing.value = true
    if (login) {
      const { token, username, role } = response.data
      localStorage.setItem('token', token)
      localStorage.setItem('username', username)
      localStorage.setItem('user_role', role)
      notice.value = 'login'
      transitionTimer = setTimeout(() => { window.location.href = '/' }, 1500)
    } else {
      notice.value = 'register'
      transitionTimer = setTimeout(() => {
        notice.value = null
        isLogin.value = true
        form.value.username = ''
        form.value.password = ''
        form.value.captcha = ''
        completing.value = false
      }, 1500)
    }
  } catch (error) {
    if (disposed) return
    const reason = axios.isAxiosError(error) ? error.response?.data : undefined
    const fallback = login ? t('login.errDefault') : copy.value.registerFailed
    const title = reason === '用户名已存在' ? t('login.errUsernameExist')
      : reason === '验证码错误' ? t('login.errCaptcha') : fallback
    const content = reason === '用户名已存在' ? t('login.errUsernameExistDesc')
      : reason === '验证码错误' ? t('login.errRetry') : fallback
    showMessage(title, content)
    if (!login) {
      fetchCaptcha()
      form.value.captcha = ''
    }
  } finally {
    if (!disposed) submitting.value = false
  }
}

onBeforeUnmount(() => {
  disposed = true
  clearTimeout(transitionTimer)
  noticeDialog.value?.close()
})
</script>

<template>
  <section class="life-scene auth-scene">
    <FixedSiteBackground />
    <div class="auth-layout">
      <div class="auth-panel">
        <header class="auth-header">
          <div class="auth-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none" focusable="false">
              <circle cx="16" cy="11" r="5" />
              <path d="M6 27v-2a10 10 0 0 1 20 0v2" />
            </svg>
          </div>
          <h1>{{ isLogin ? t('login.titleLogin') : t('login.titleRegister') }}</h1>
          <p>{{ isLogin ? t('login.descLogin') : t('login.descRegister') }}</p>
        </header>

        <form @submit.prevent="handleSubmit" :aria-busy="busy">
          <fieldset :disabled="busy" class="auth-fields">
            <div class="auth-field">
              <label for="auth-username">{{ t('login.username') }}</label>
              <input id="auth-username" v-model="form.username" name="username" type="text"
                autocomplete="username" autocapitalize="none" :spellcheck="false" required
                :placeholder="t('login.placeholderUser')" />
            </div>
            <div class="auth-field">
              <label for="auth-password">{{ t('login.password') }}</label>
              <input id="auth-password" v-model="form.password" name="password" type="password"
                :autocomplete="isLogin ? 'current-password' : 'new-password'" required
                :placeholder="t('login.placeholderPwd')" />
            </div>
            <div v-if="!isLogin" class="auth-field">
              <div class="auth-label-row">
                <label for="auth-captcha">{{ t('login.captcha') }}</label>
                <button type="button" class="auth-refresh" :disabled="captchaLoading" @click="fetchCaptcha">
                  <span aria-hidden="true">↻</span> {{ t('login.btnRefresh') }}
                </button>
              </div>
              <div class="auth-captcha-row">
                <input id="auth-captcha" v-model="form.captcha" name="captcha" type="text"
                  autocomplete="off" autocapitalize="characters" :spellcheck="false" required maxlength="4"
                  :placeholder="t('login.placeholderCaptcha')" />
                <button type="button" class="auth-captcha" :disabled="captchaLoading"
                  :aria-label="copy.refreshCaptcha" :title="copy.refreshCaptcha" @click="fetchCaptcha">
                  <img v-if="captchaImg" :src="captchaImg" :alt="t('login.captcha')" />
                  <span v-else role="status">{{ captchaFailed ? copy.captchaFailed : t('login.loading') }}</span>
                </button>
              </div>
            </div>
            <button type="submit" class="auth-submit" :disabled="busy || (!isLogin && captchaLoading)">
              <span>{{ busy ? copy.submitting : isLogin ? t('login.btnLogin') : t('login.btnRegister') }}</span>
              <svg v-if="!busy" viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
                <path d="M4 12h15m-6-6 6 6-6 6" />
              </svg>
              <span v-else class="auth-spinner" aria-hidden="true"></span>
            </button>
          </fieldset>
        </form>

        <footer class="auth-footer">
          <span>{{ isLogin ? t('login.noAccount') : t('login.hasAccount') }}</span>
          <button type="button" :disabled="busy" @click="isLogin = !isLogin">
            {{ isLogin ? t('login.toRegister') : t('login.toLogin') }}
          </button>
        </footer>
      </div>
    </div>

    <Teleport to="body">
      <dialog ref="noticeDialog" class="life-dialog auth-dialog" aria-labelledby="auth-notice-title"
        aria-describedby="auth-notice-description" @cancel="notice = null" @close="notice = null">
        <div class="life-dialog-content">
          <div class="auth-notice-top">
            <div class="auth-notice-mark" aria-hidden="true">
              <svg viewBox="0 0 32 32" fill="none" focusable="false">
                <template v-if="notice === 'error'">
                  <circle cx="16" cy="16" r="11" /><path d="M16 9v8m0 5h.01" />
                </template>
                <template v-else>
                  <circle cx="16" cy="16" r="11" /><path d="m10 16 4 4 8-8" />
                </template>
              </svg>
            </div>
            <button v-if="notice === 'error'" type="button" class="life-dialog-close"
              :aria-label="copy.close" @click="notice = null">×</button>
          </div>
          <h2 id="auth-notice-title">{{ noticeTitle }}</h2>
          <p id="auth-notice-description" class="life-dialog-note" role="status">{{ noticeDescription }}</p>
          <div v-if="notice === 'error'" class="life-dialog-actions">
            <button type="button" class="life-dialog-primary" @click="notice = null">{{ copy.confirm }}</button>
          </div>
          <div v-else class="auth-progress" aria-hidden="true"><span></span></div>
        </div>
      </dialog>
    </Teleport>
  </section>
</template>

<style scoped>
.auth-scene { display: grid; align-items: center; }
.auth-layout { width: 100%; padding: 56px 24px 72px; }
.auth-panel { --site-focus: #2857a5; width: min(460px, 100%); margin: 0 auto; padding: 36px 40px 30px; color: #214777; background: #fff; border: 1px solid #d4deec; border-radius: 8px; box-shadow: 0 16px 48px rgb(16 41 82 / 12%); }
.auth-panel ::selection, .auth-dialog ::selection { background: #dbe8fc; color: #214777; }
.auth-header { margin-bottom: 30px; }
.auth-mark, .auth-notice-mark { display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; color: #2857a5; background: #edf3fc; border-radius: 50%; }
.auth-mark { margin-bottom: 22px; }
.auth-mark svg, .auth-notice-mark svg { width: 28px; height: 28px; stroke: currentColor; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
.auth-header h1 { margin: 0; color: #214777; font-size: 28px; line-height: 1.4; letter-spacing: -.03em; font-weight: 600; }
.auth-header p { margin: 10px 0 0; color: #4a6382; font-size: 14px; line-height: 1.8; }
.auth-fields { display: grid; gap: 22px; min-width: 0; margin: 0; padding: 0; border: 0; }
.auth-field { min-width: 0; }
.auth-field label { display: block; margin-bottom: 9px; font-size: 13px; font-weight: 500; }
.auth-field input { width: 100%; min-width: 0; min-height: 48px; padding: 12px 14px; color: #214777; background: #f6f8fc; border: 1px solid #a9bdd8; border-radius: 4px; font-size: 14px; line-height: 1.6; transition: border-color .2s, background-color .2s; }
.auth-field input::placeholder { color: #607899; opacity: 1; }
.auth-field input:hover { border-color: #6284b6; }
.auth-field input:focus { border-color: #2857a5; background: #fff; outline: 2px solid rgb(40 87 165 / 18%); outline-offset: 2px; }
.auth-label-row { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.auth-refresh { padding: 0 0 7px; color: #2857a5; background: transparent; border: 0; font-size: 12px; }
.auth-refresh:hover { text-decoration: underline; text-underline-offset: 4px; }
.auth-captcha-row { display: grid; grid-template-columns: minmax(0, 1fr) 120px; gap: 12px; }
.auth-captcha { display: flex; align-items: center; justify-content: center; min-height: 48px; overflow: hidden; padding: 0; color: #4a6382; background: #f6f8fc; border: 1px solid #a9bdd8; border-radius: 4px; }
.auth-captcha:hover { border-color: #2857a5; }
.auth-captcha img { display: block; width: 100%; height: 48px; object-fit: contain; }
.auth-captcha span { padding: 4px 8px; font-size: 11px; line-height: 1.5; }
.auth-submit { display: flex; align-items: center; justify-content: center; gap: 14px; width: 100%; min-height: 48px; margin-top: 4px; padding: 12px 18px; color: #fff; background: #2857a5; border: 1px solid #2857a5; border-radius: 4px; font-size: 14px; font-weight: 500; transition: background-color .2s; }
.auth-submit:hover { background: #214b8d; }
.auth-submit svg { width: 18px; height: 18px; stroke: currentColor; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
.auth-footer { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: center; gap: 8px; margin-top: 28px; padding-top: 22px; border-top: 1px solid #dce5f1; font-size: 13px; line-height: 1.8; }
.auth-footer span { color: #4a6382; }
.auth-footer button { padding: 0; color: #2857a5; background: transparent; border: 0; text-decoration: underline; text-underline-offset: 5px; text-decoration-color: #9db4d7; }
.auth-footer button:hover { color: #193f7e; text-decoration-color: currentColor; }
.auth-panel button { cursor: pointer; }
.auth-panel button:disabled, .auth-fields:disabled { opacity: .65; cursor: default; }
.auth-dialog { width: min(400px, calc(100% - 32px)); }
.auth-notice-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.auth-dialog p { margin: 12px 0 0; }
.auth-dialog .life-dialog-close { margin: -6px -8px 0 0; }
.auth-progress { height: 2px; margin-top: 26px; overflow: hidden; background: #e1eaf7; }
.auth-progress span { display: block; width: 100%; height: 100%; background: #2857a5; transform-origin: left; animation: auth-complete 1.5s linear both; }
.auth-spinner { width: 16px; height: 16px; border: 1.5px solid rgb(255 255 255 / 40%); border-top-color: #fff; border-radius: 50%; animation: auth-spin .8s linear infinite; }
@keyframes auth-complete { from { transform: scaleX(0); } to { transform: scaleX(1); } }
@keyframes auth-spin { to { transform: rotate(360deg); } }
@media (max-width: 640px) {
  .auth-layout { padding: 32px 20px 48px; }
  .auth-panel { padding: 28px 24px 24px; }
  .auth-header h1 { font-size: 26px; }
  .auth-captcha-row { grid-template-columns: minmax(0, 1fr) 104px; gap: 10px; }
}
@media (prefers-reduced-motion: reduce) {
  .auth-spinner { animation: none; }
  .auth-progress span { animation: none; }
}
</style>
