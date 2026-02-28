<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import axios from "axios";
import { useI18n } from "vue-i18n";
const { t } = useI18n();

const route = useRoute();
const router = useRouter();
const project = ref<any>(null);
const hasLiked = ref(false);
const likesList = ref<any[]>([]);
const showLikesList = ref(false);

const isLoggedIn = computed(() => !!localStorage.getItem("token"));
const isAdmin = computed(
  () => localStorage.getItem("user_role") === "ROLE_ADMIN"
);

const showMessageModal = ref(false);
const messageConfig = ref({ type: "warning", title: "提示", content: "" });

const showMessage = (
  type: "warning" | "error" | "success",
  title: string,
  content: string
) => {
  messageConfig.value = { type, title, content };
  showMessageModal.value = true;
  setTimeout(() => (showMessageModal.value = false), 2000);
};

const fetchProjectDetail = async () => {
  try {
    const res = await axios.get(`/api/projects/${route.params.id}`);
    project.value = res.data;

    // 获取后加载点赞状态
    if (isLoggedIn.value) checkLikeStatus();
    if (isAdmin.value) fetchLikesList();
  } catch (error) {
    console.error(t("project.msgLoadFail"), error);
    router.push("/projects");
  }
};

const checkLikeStatus = async () => {
  try {
    const res = await axios.get(
      `/api/projects/${route.params.id}/like-status`,
      {
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      }
    );
    hasLiked.value = res.data;
  } catch (error) {
    console.error("获取点赞状态失败", error);
  }
};

const fetchLikesList = async () => {
  try {
    const res = await axios.get(`/api/projects/${route.params.id}/likes-list`, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    });
    likesList.value = res.data;
  } catch (error) {
    console.error("获取点赞列表失败", error);
  }
};

const handleLike = async () => {
  if (!isLoggedIn.value) {
    showMessage(
      "warning",
      t("article.msgNeedLoginTitle"),
      t("article.msgNeedLoginDesc")
    );
    return;
  }

  try {
    const res = await axios.post(
      `/api/projects/${project.value.id}/like`,
      {},
      {
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      }
    );

    project.value.likeCount = res.data.likeCount;
    hasLiked.value = res.data.isLiked;

    if (isAdmin.value) fetchLikesList();
  } catch (error) {
    console.error("点赞操作失败", error);
    showMessage(
      "error",
      t("article.msgOpFailTitle"),
      t("article.msgOpFailDesc")
    );
  }
};

const handleDownload = async () => {
  window.location.href = `/api/download?fileUrl=${encodeURIComponent(project.value.attachmentUrl)}`;
};

const getFileName = (url: string) => {
  return url.split("/").pop() || "附件";
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString();
};

const formatDateTime = (dateStr: string) => new Date(dateStr).toLocaleString();

onMounted(fetchProjectDetail);
</script>

<style scoped>
.animate-spin-slow {
  animation: spin 8s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 隐藏视频自带的默认控制条某些样式可以自定义，这里先用默认的 */
video::-webkit-media-controls-enclosure {
  border-radius: 0;
}
</style>

<template>
  <div
    v-if="project"
    class="min-h-screen px-6 pb-20"
  >
    <div class="max-w-5xl mx-auto">

      <div class="flex items-center justify-between mb-8">
        <button
          @click="router.back()"
          class="flex items-center gap-2 text-slate-500 hover:text-white transition-colors group"
        >
          <span class="group-hover:-translate-x-1 transition-transform">←</span>
          <span class="text-xs font-black uppercase tracking-widest">{{ t('project.backToList') }}</span>
        </button>
        <div class="flex items-center gap-3">
          <span class="px-3 py-1 bg-blue-500/10 text-blue-400 rounded-full text-[10px] font-black uppercase tracking-tighter border border-blue-500/20">
            {{ project.category }}
          </span>
        </div>
      </div>

      <div class="relative w-full bg-black rounded-[3rem] overflow-hidden shadow-2xl border border-white/5 aspect-video mb-10 flex items-center justify-center">

        <video
          v-if="project.mediaType === 'VIDEO'"
          :src="project.mediaUrl"
          controls
          class="w-full h-full object-contain"
          :poster="project.coverUrl"
        ></video>

        <div
          v-else
          class="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-slate-900 to-slate-950 p-12"
        >
          <div class="w-48 h-48 rounded-full overflow-hidden border-4 border-white/10 shadow-2xl animate-spin-slow mb-8">
            <img
              :src="project.coverUrl"
              class="w-full h-full object-cover"
            />
          </div>
          <audio
            :src="project.mediaUrl"
            controls
            class="w-full max-w-md"
          ></audio>
          <p class="mt-4 text-slate-500 text-xs font-black uppercase tracking-[0.3em]">Audio Mode Rendering</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-10">

        <div class="lg:col-span-2 space-y-6">
          <h1 class="text-4xl font-black text-white leading-tight">{{ project.title }}</h1>
          <div class="flex items-center gap-6 text-[10px] font-bold text-slate-500 uppercase tracking-widest">
            <span>{{ t('project.publishedOn') }} {{ formatDate(project.createdAt) }}</span>
            <span>📺 {{ project.viewCount }} {{ t('project.views') }}</span>
            <span>❤️ {{ project.likeCount }} {{ t('project.likes') }}</span>
          </div>
          <div class="prose prose-invert max-w-none">
            <p class="text-slate-400 leading-relaxed whitespace-pre-wrap">{{ project.description }}</p>
          </div>
        </div>

        <div class="space-y-6">

          <div class="bg-slate-900/40 border border-white/10 p-8 rounded-[2.5rem] text-center backdrop-blur-xl transition-all">
            <button
              @click="handleLike"
              :class="['w-20 h-20 rounded-full flex items-center justify-center text-3xl transition-all mb-4 mx-auto border-4', 
                hasLiked ? 'bg-red-500 border-red-500 shadow-xl shadow-red-500/40 scale-110' : 'bg-white/5 border-white/10 text-slate-500 hover:border-red-500/50 hover:text-red-500']"
            >
              ❤️
            </button>
            <p class="text-white font-black text-2xl">{{ project.likeCount }}</p>
            <p class="text-slate-500 text-[10px] font-bold uppercase mt-2">
              {{ hasLiked ? t('project.cancelLike') : t('project.promptLike') }}
            </p>
          </div>

          <div
            v-if="project.attachmentUrl"
            class="bg-blue-500/10 border border-blue-500/20 p-8 rounded-[2.5rem] backdrop-blur-xl"
          >
            <h5 class="text-white font-black text-sm mb-4 flex items-center gap-2">
              <span class="text-blue-400 text-lg">📁</span> {{ t('project.attachment') }}
            </h5>
            <button
              @click="handleDownload"
              class="w-full py-4 bg-blue-500 hover:bg-blue-600 text-white font-black rounded-2xl transition-all shadow-xl shadow-blue-500/20 active:scale-95 flex items-center justify-center gap-2"
            >
              <span>{{ t('project.download') }}</span>
              <span class="text-xs opacity-50 truncate max-w-25">({{ getFileName(project.attachmentUrl) }})</span>
            </button>
          </div>

          <div
            v-if="isAdmin"
            class="mt-6"
          >
            <button
              @click="showLikesList = !showLikesList"
              class="w-full flex items-center justify-between px-6 py-4 bg-slate-900/60 hover:bg-slate-800 border border-white/10 rounded-2xl text-slate-300 font-bold transition-all"
            >
              <span class="flex items-center gap-2 text-sm">
                <span class="text-blue-400">📊</span> {{ t('project.likeDetails') }} ({{ likesList.length }})
              </span>
              <span
                :class="{'rotate-180': showLikesList}"
                class="transition-transform duration-300 text-[10px]"
              >▼</span>
            </button>

            <Transition name="fade">
              <div
                v-if="showLikesList"
                class="mt-2 bg-slate-900/80 border border-white/5 rounded-2xl overflow-hidden max-h-60 overflow-y-auto custom-scrollbar"
              >
                <div
                  v-if="likesList.length === 0"
                  class="p-6 text-center text-slate-500 text-sm"
                >{{ t('project.noLikes') }}</div>
                <ul
                  v-else
                  class="divide-y divide-white/5"
                >
                  <li
                    v-for="like in likesList"
                    :key="like.id"
                    class="px-5 py-3 flex flex-col hover:bg-white/5 transition-colors"
                  >
                    <span class="text-white font-bold text-sm">{{ like.username }}</span>
                    <span class="text-slate-500 text-[10px]">{{ formatDateTime(like.createdAt) }}</span>
                  </li>
                </ul>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </div>
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showMessageModal"
          class="fixed inset-0 z-[100] flex items-center justify-center pointer-events-none p-6"
        >
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