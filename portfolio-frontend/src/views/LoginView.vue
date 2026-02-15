<script setup lang="ts">
import { ref } from "vue";
import axios from "axios";
import { useRouter } from "vue-router";

const router = useRouter();
const isLogin = ref(true); // 切换登录/注册状态
const showSuccessModal = ref(false);
const showRegisterSuccessModal = ref(false);

const form = ref({
  username: "",
  password: "",
});

const handleSubmit = async () => {
  const url = isLogin.value ? "/api/auth/login" : "/api/auth/register";
  try {
    const response = await axios.post(
      `http://localhost:8080${url}`,
      form.value
    );

    if (isLogin.value) {
      // 登录成功逻辑
      const { token, username, role } = response.data;
      localStorage.setItem("token", token);
      localStorage.setItem("username", username);
      localStorage.setItem("user_role", role); // 关键：这就是 v-if 判断的依据

      // 不直接跳转，而是显示弹窗
      showSuccessModal.value = true;

      // 1.5秒后执行硬跳转
      setTimeout(() => {
        window.location.href = "/";
      }, 1500);
    } else {
      // 注册成功逻辑
      showRegisterSuccessModal.value = true;

      // 1.5秒后自动关闭弹窗，并切换到登录表单
      setTimeout(() => {
        showRegisterSuccessModal.value = false;
        isLogin.value = true; // 自动切回登录页
      }, 1500);
    }
  } catch (error: any) {
    alert(error.response?.data || "操作失败，请联系管理员");
  }
};
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center px-6">
    <div class="w-full max-w-md bg-slate-900/40 backdrop-blur-xl border border-white/10 p-10 rounded-[2.5rem] shadow-2xl">
      <div class="text-center mb-10">
        <h2 class="text-3xl font-bold bg-linear-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
          {{ isLogin ? '欢迎回来' : '开启旅程' }}
        </h2>
        <p class="text-slate-400 mt-2 text-sm">
          {{ isLogin ? '登录以管理你的生涯与生活' : '加入我们，开启你的数字空间' }}
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
            placeholder="输入用户名"
          />
        </div>

        <div>
          <label class="block text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Password</label>
          <input
            v-model="form.password"
            type="password"
            required
            class="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 text-white focus:outline-none focus:border-blue-500 transition-all"
            placeholder="••••••••"
          />
        </div>

        <button
          type="submit"
          class="w-full py-4 bg-linear-to-r from-blue-600 to-blue-500 hover:from-blue-500 hover:to-blue-400 text-white font-bold rounded-2xl shadow-lg shadow-blue-500/20 transition-all active:scale-95"
        >
          {{ isLogin ? '立即登录' : '提交注册' }}
        </button>
      </form>

      <div class="mt-8 text-center text-sm">
        <span class="text-slate-500">{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
        <button
          @click="isLogin = !isLogin"
          class="ml-2 text-blue-400 hover:text-blue-300 font-bold underline underline-offset-4"
        >
          {{ isLogin ? '点此注册' : '返回登录' }}
        </button>
      </div>
    </div>
  </div>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showSuccessModal"
        class="fixed inset-0 z-100 flex items-center justify-center p-6"
      >
        <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"></div>

        <div class="relative bg-slate-900/90 backdrop-blur-2xl border border-emerald-500/30 px-10 py-8 rounded-4xl shadow-2xl flex flex-col items-center scale-in-center">
          <div class="w-16 h-16 bg-emerald-500/20 text-emerald-400 rounded-full flex items-center justify-center text-3xl mb-4 border border-emerald-500/30">
            ✨
          </div>
          <h3 class="text-2xl font-black text-white">欢迎回来</h3>
          <p class="text-emerald-400/80 text-sm mt-2 font-bold">正在跳转首页...</p>
        </div>
      </div>
    </Transition>
  </Teleport>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showRegisterSuccessModal"
        class="fixed inset-0 z-100 flex items-center justify-center p-6"
      >
        <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"></div>

        <div class="relative bg-slate-900/90 backdrop-blur-2xl border border-blue-500/30 px-10 py-8 rounded-[2rem] shadow-2xl flex flex-col items-center scale-in-center">
          <div class="w-16 h-16 bg-blue-500/20 text-blue-400 rounded-full flex items-center justify-center text-3xl mb-4 border border-blue-500/30 animate-bounce">
            🎉
          </div>

          <h3 class="text-2xl font-black text-white">注册成功！</h3>
          <p class="text-blue-400/80 text-sm mt-2 font-bold">账号已创建，即将跳转登录...</p>
        </div>
      </div>
    </Transition>
  </Teleport>
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
  0% {
    transform: scale(0.9);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>