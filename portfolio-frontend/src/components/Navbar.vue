<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { ref, onMounted } from "vue";

const route = useRoute();
const router = useRouter();
const username = ref<string | null>(null);
const showLogoutModal = ref(false);
const showGoodbyeModal = ref(false);

// 初始化时获取登录状态
onMounted(() => {
  username.value = localStorage.getItem("username");
});

// 退出登录逻辑
const handleLogout = () => {
  showLogoutModal.value = false;
  showGoodbyeModal.value = true;

  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("user_role");

  username.value = null;

  setTimeout(() => {
    router.push("/").then(() => {
      window.location.reload();
    });
  }, 1500);
};

// 定义导航链接
const navLinks = [
  { name: "主页", path: "/" },
  { name: "生涯", path: "/career" },
  { name: "生活", path: "/articles" },
];
</script>

<template>
  <nav class="fixed top-6 left-1/2 -translate-x-1/2 z-50 w-[90%] max-w-6xl">
    <div class="bg-slate-900/40 backdrop-blur-xl border border-white/10 px-6 py-3 rounded-full flex items-center shadow-2xl">

      <div class="flex-1 flex justify-start">
        <div class="text-xl font-black tracking-tighter text-blue-400 cursor-pointer">
          About me.
        </div>
      </div>

      <div class="flex items-center gap-1">
        <router-link
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          class="px-4 py-2 rounded-full text-sm font-medium transition-all duration-300 relative group"
          :class="route.path === link.path ? 'text-white' : 'text-slate-400 hover:text-slate-200'"
        >
          {{ link.name }}
          <div
            v-if="route.path === link.path"
            class="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 bg-blue-400 rounded-full shadow-[0_0_8px_#60a5fa]"
          >
          </div>
        </router-link>
      </div>

      <div class="flex-1 flex justify-end gap-3 items-center">
        <button class="p-2 text-slate-400 hover:text-white transition-colors">
          <span class="text-s font-mono">EN/JP</span>
        </button>

        <router-link
          v-if="!username"
          to="/login"
          class="px-5 py-1.5 bg-blue-500 hover:bg-blue-600 text-white text-xs font-bold rounded-full transition-all shadow-[0_0_15px_rgba(59,130,246,0.3)]"
        >
          LOGIN
        </router-link>

        <button
          v-else
          @click="showLogoutModal = true"
          class="flex items-center gap-2 px-4 py-1.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-full transition-all group"
        >
          <span class="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-pulse"></span>
          <span class="text-slate-200 group-hover:text-white text-xs font-bold tracking-wider">
            {{ username }}
          </span>
        </button>
      </div>

      <Teleport to="body">
        <Transition name="fade">
          <div
            v-if="showLogoutModal"
            class="fixed inset-0 z-100 flex items-center justify-center p-6"
          >
            <div
              class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"
              @click="showLogoutModal = false"
            ></div>

            <div class="relative w-full max-w-xs bg-slate-900/90 backdrop-blur-2xl border border-white/10 p-8 rounded-[2.5rem] shadow-2xl">
              <div class="text-center mb-6">
                <div class="w-12 h-12 bg-red-500/20 text-red-400 rounded-2xl flex items-center justify-center mx-auto mb-4 text-xl">
                  ⚠️
                </div>
                <h3 class="text-lg font-bold text-white">确认退出？</h3>
                <p class="text-slate-400 text-xs mt-2">退出后将回到游客身份，无法进行评论等操作</p>
              </div>

              <div class="space-y-3">
                <button
                  @click="handleLogout"
                  class="w-full py-3 bg-red-500 hover:bg-red-600 text-white text-sm font-bold rounded-2xl transition-all shadow-lg shadow-red-500/20"
                >
                  确定退出
                </button>
                <button
                  @click="showLogoutModal = false"
                  class="w-full py-3 bg-white/5 hover:bg-white/10 text-slate-300 text-sm font-bold rounded-2xl transition-all"
                >
                  返回
                </button>
              </div>
            </div>
          </div>
        </Transition>
        <Transition name="fade">
          <div
            v-if="showGoodbyeModal"
            class="fixed inset-0 z-100 flex items-center justify-center p-6"
          >
            <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"></div>

            <div class="relative bg-slate-900/90 backdrop-blur-2xl border border-white/10 px-10 py-8 rounded-4xl shadow-2xl flex flex-col items-center scale-in-center">
              <div class="text-4xl mb-4 animate-bounce">👋</div>
              <h3 class="text-xl font-black text-white">已安全退出</h3>
              <p class="text-slate-400 text-sm mt-2">期待与你的下一次相遇</p>
            </div>
          </div>
        </Transition>
      </Teleport>

    </div>
  </nav>
</template>

<style scoped>
.scale-in-center {
  animation: scale-in-center 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}
@keyframes scale-in-center {
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