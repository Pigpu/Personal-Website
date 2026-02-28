<script setup lang="ts">
import { ref, watch } from "vue";
import axios from "axios";
//import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
const { t } = useI18n();

//const router = useRouter();
const isLogin = ref(true); // 切换登录/注册状态
const showSuccessModal = ref(false);
const showRegisterSuccessModal = ref(false);
// 自定义消息提示状态
const showMessageModal = ref(false);
const messageConfig = ref({
  type: 'warning', // 'warning', 'error', 'success'
  title: '提示',
  content: ''
});
const errmes = ref(t('login.errDefault'));

const showMessage = (type: 'warning' | 'error' | 'success', title: string, content: string) => {
  messageConfig.value = { type, title, content };
  showMessageModal.value = true;
  setTimeout(() => {
    showMessageModal.value = false;
  }, 2000); // 2秒后自动关闭
};

// 验证码相关状态
const captchaImg = ref("");

// 表单数据增加了 captcha 和 uuid
const form = ref({
  username: "",
  password: "",
  captcha: "", 
  uuid: ""     
});

// 获取验证码的方法
const fetchCaptcha = async () => {
  try {
    const res = await axios.get("/api/auth/captcha");
    form.value.uuid = res.data.uuid;
    captchaImg.value = res.data.img;
  } catch (error) {
    console.error("验证码加载失败");
  }
};

// 监听状态切换：当切换到“注册”时，自动拉取验证码
watch(isLogin, (newVal) => {
  if (!newVal) { // false 表示处于注册模式
    fetchCaptcha();
    form.value.captcha = ""; // 切换时清空之前的输入
  }
});

const handleSubmit = async () => {
  // 注册模式下的前端校验
  if (!isLogin.value && !form.value.captcha) {
    showMessage('warning', t('login.captcha'), t('login.msgInputCaptcha')); 
    return;
  }

  const url = isLogin.value ? "/api/auth/login" : "/api/auth/register";
  
  // 为了后端干净，登录时只传用户名密码，注册时全传
  const payload = isLogin.value 
    ? { username: form.value.username, password: form.value.password }
    : form.value;

  try {
    const response = await axios.post(
      `${url}`,
      payload
    );

    if (isLogin.value) {
      // --- 登录成功逻辑 ---
      const { token, username, role } = response.data;
      localStorage.setItem("token", token);
      localStorage.setItem("username", username);
      localStorage.setItem("user_role", role); 

      showSuccessModal.value = true;
      setTimeout(() => {
        window.location.href = "/";
      }, 1500);
    } else {
      // --- 注册成功逻辑 ---
      showRegisterSuccessModal.value = true;
      
      // 注册成功后清理表单，1.5秒后切回登录
      setTimeout(() => {
        showRegisterSuccessModal.value = false;
        isLogin.value = true; 
        form.value.username = "";
        form.value.password = "";
        form.value.captcha = "";
      }, 1500);
    }
  } catch (error: any) {
    if(error.response?.data=='用户名已存在'){
      errmes.value = t('login.errUsernameExistDesc')
    }
    if(error.response?.data=='验证码错误'){
      errmes.value = t('login.errRetry')
    }
    const errorTitle = error.response?.data === '用户名已存在' ? t('login.errUsernameExist') : 
                      (error.response?.data === '验证码错误' ? t('login.errCaptcha') : t('login.errDefault'));
    showMessage('warning', errorTitle, errmes.value);
    // 注册失败（如验证码错误/用户名重复），自动刷新验证码供用户重试
    if (!isLogin.value) {
      fetchCaptcha();
      form.value.captcha = ""; // 清空错误输入
    }
  }
};
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center px-6">
    <div class="w-full max-w-md bg-slate-900/40 backdrop-blur-xl border border-white/10 p-10 rounded-[2.5rem] shadow-2xl transition-all duration-300">
      <div class="text-center mb-10">
        <h2 class="text-3xl font-bold bg-linear-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
          {{ isLogin ? t('login.titleLogin') : t('login.titleRegister') }}
        </h2>
        <p class="text-slate-400 mt-2 text-sm">
          {{ isLogin ? t('login.descLogin') : t('login.descRegister') }}
        </p>
      </div>

      <form
        @submit.prevent="handleSubmit"
        class="space-y-6"
      >
        <div>
          <label class="block text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Username</label>
          <input
            v-model="form.username"
            type="text"
            required
            class="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 text-white focus:outline-none focus:border-blue-500 transition-all"
            :placeholder="t('login.placeholderUser')"
          />
        </div>

        <div>
          <label class="block text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Password</label>
          <input
            v-model="form.password"
            type="password"
            required
            class="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 text-white focus:outline-none focus:border-blue-500 transition-all"
            :placeholder="t('login.placeholderPwd')"
          />
        </div>

        <div v-if="!isLogin" class="space-y-2 animate-in fade-in slide-in-from-top-2 duration-300">
          <label class="block text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Captcha</label>
          <div class="flex gap-4">
            <input 
              v-model="form.captcha" 
              type="text" 
              required
              :placeholder="t('login.placeholderCaptcha')"
              maxlength="4"
              class="flex-1 bg-white/5 border border-white/10 rounded-2xl px-5 py-4 text-white placeholder-slate-600 focus:outline-none focus:border-blue-500 transition-all uppercase"
            />
            
            <div 
              @click="fetchCaptcha" 
              class="w-32 bg-white/5 rounded-2xl overflow-hidden cursor-pointer border border-white/10 hover:border-blue-500/50 transition-all flex items-center justify-center shrink-0 group relative"
              title="点击刷新验证码"
            >
              <img v-if="captchaImg" :src="captchaImg" class="w-full h-full object-contain" />
              <span v-else class="text-slate-500 text-xs animate-pulse">{{ t('login.loading') }}</span>
              
              <div class="absolute inset-0 bg-black/40 backdrop-blur-[2px] opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <span class="text-white text-xs font-bold">↻ {{ t('login.btnRefresh') }}</span>
              </div>
            </div>
          </div>
        </div>

        <button
          type="submit"
          class="w-full py-4 bg-linear-to-r from-blue-600 to-blue-500 hover:from-blue-500 hover:to-blue-400 text-white font-bold rounded-2xl shadow-lg shadow-blue-500/20 transition-all active:scale-95"
        >
          {{ isLogin ? t('login.btnLogin') : t('login.btnRegister') }}
        </button>
      </form>

      <div class="mt-8 text-center text-sm">
        <span class="text-slate-500">{{ isLogin ? t('login.noAccount') : t('login.hasAccount') }}</span>
        <button
          @click="isLogin = !isLogin"
          class="ml-2 text-blue-400 hover:text-blue-300 font-bold underline underline-offset-4"
        >
          {{ isLogin ? t('login.toRegister') : t('login.toLogin') }}
        </button>
      </div>
    </div>
    <Teleport to="body">
    <Transition name="fade">
      <div v-if="showSuccessModal" class="fixed inset-0 z-100 flex items-center justify-center p-6">
        <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"></div>
        <div class="relative bg-slate-900/90 backdrop-blur-2xl border border-emerald-500/30 px-10 py-8 rounded-4xl shadow-2xl flex flex-col items-center scale-in-center">
          <div class="w-16 h-16 bg-emerald-500/20 text-emerald-400 rounded-full flex items-center justify-center text-3xl mb-4 border border-emerald-500/30">✨</div>
          <h3 class="text-2xl font-black text-white">{{ t('login.successLoginTitle') }}</h3>
          <p class="text-emerald-400/80 text-sm mt-2 font-bold">{{ t('login.successLoginDesc') }}</p>
        </div>
      </div>
    </Transition>
  </Teleport>

  <Teleport to="body">
    <Transition name="fade">
      <div v-if="showRegisterSuccessModal" class="fixed inset-0 z-100 flex items-center justify-center p-6">
        <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"></div>
        <div class="relative bg-slate-900/90 backdrop-blur-2xl border border-blue-500/30 px-10 py-8 rounded-4xl shadow-2xl flex flex-col items-center scale-in-center">
          <div class="w-16 h-16 bg-blue-500/20 text-blue-400 rounded-full flex items-center justify-center text-3xl mb-4 border border-blue-500/30 animate-bounce">🎉</div>
          <h3 class="text-2xl font-black text-white">{{ t('login.successRegTitle') }}</h3>
          <p class="text-blue-400/80 text-sm mt-2 font-bold">{{ t('login.successRegDesc') }}</p>
        </div>
      </div>
    </Transition>
  </Teleport>

  <Teleport to="body">
      <Transition name="fade">
        <div v-if="showMessageModal" class="fixed inset-0 z-100 flex items-center justify-center pointer-events-none p-6">
          <div 
            class="bg-slate-900/90 backdrop-blur-2xl border p-8 rounded-[2.5rem] shadow-2xl text-center scale-in-center pointer-events-auto"
            :class="{
              'border-amber-500/30': messageConfig.type === 'warning',
              'border-red-500/30': messageConfig.type === 'error',
              'border-emerald-500/30': messageConfig.type === 'success'
            }"
          >
            <div 
              class="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 text-3xl border"
              :class="{
                'bg-amber-500/10 text-amber-500 border-amber-500/20': messageConfig.type === 'warning',
                'bg-red-500/10 text-red-500 border-red-500/20': messageConfig.type === 'error',
                'bg-emerald-500/10 text-emerald-500 border-emerald-500/20': messageConfig.type === 'success'
              }"
            >
              {{ messageConfig.type === 'warning' ? '⚠️' : (messageConfig.type === 'error' ? '❌' : '✨') }}
            </div>
            
            <h3 class="text-xl font-black text-white">{{ messageConfig.title }}</h3>
            <p class="text-slate-400 text-sm mt-2">{{ messageConfig.content }}</p>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 缩放进场动画 */
.scale-in-center {
  animation: scale-up 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes scale-up {
  0% { transform: scale(0.9); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
</style>