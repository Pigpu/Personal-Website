<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

const route = useRoute();
const router = useRouter();
const project = ref<any>(null);
const hasLiked = ref(false);

const fetchProjectDetail = async () => {
  try {
    // 1. 获取详情 (后端会自动增加 viewCount)
    const res = await axios.get(`http://localhost:8080/api/projects/${route.params.id}`);
    project.value = res.data;
  } catch (error) {
    console.error("加载作品失败", error);
    router.push('/projects');
  }
};

const handleLike = async () => {
  if (hasLiked.value) return;
  try {
    const res = await axios.post(`http://localhost:8080/api/projects/${project.value.id}/like`);
    project.value.likeCount = res.data; // 后端返回最新的点赞数
    hasLiked.value = true;
  } catch (error) {
    alert("点赞失败，请先登录");
  }
};

const handleDownload = () => {
  // 创建一个隐藏的 a 标签进行下载
  const link = document.createElement('a');
  link.href = project.value.attachmentUrl;
  link.download = getFileName(project.value.attachmentUrl);
  link.target = "_blank";
  link.click();
};

const getFileName = (url: string) => {
  return url.split('/').pop() || '附件';
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString();
};

onMounted(fetchProjectDetail);
</script>

<style scoped>
.animate-spin-slow {
  animation: spin 8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 隐藏视频自带的默认控制条某些样式可以自定义，这里先用默认的 */
video::-webkit-media-controls-enclosure {
  border-radius: 0;
}
</style>

<template>
  <div v-if="project" class="min-h-screen px-6 pb-20">
    <div class="max-w-5xl mx-auto">
      
      <div class="flex items-center justify-between mb-8">
        <button @click="router.back()" class="flex items-center gap-2 text-slate-500 hover:text-white transition-colors group">
          <span class="group-hover:-translate-x-1 transition-transform">←</span>
          <span class="text-xs font-black uppercase tracking-widest">返回作品列表</span>
        </button>
        <div class="flex items-center gap-3">
          <span class="px-3 py-1 bg-blue-500/10 text-blue-400 rounded-full text-[10px] font-black uppercase tracking-tighter border border-blue-500/20">
            {{ project.category }}
          </span>
        </div>
      </div>

      <div class="relative w-full rounded-[3rem] overflow-hidden shadow-2xl border border-white/5 aspect-video mb-10 flex items-center justify-center">
        
        <video 
          v-if="project.mediaType === 'VIDEO'"
          :src="project.mediaUrl"
          controls
          class="w-full h-full object-contain"
          :poster="project.coverUrl"
        ></video>

        <div v-else class="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-slate-900 to-slate-950 p-12">
          <div class="w-48 h-48 rounded-full overflow-hidden border-4 border-white/10 shadow-2xl animate-spin-slow mb-8">
            <img :src="project.coverUrl" class="w-full h-full object-cover" />
          </div>
          <audio :src="project.mediaUrl" controls class="w-full max-w-md"></audio>
          <p class="mt-4 text-slate-500 text-xs font-black uppercase tracking-[0.3em]">Audio Mode Rendering</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-10">
        
        <div class="lg:col-span-2 space-y-6">
          <h1 class="text-4xl font-black text-white leading-tight">{{ project.title }}</h1>
          <div class="flex items-center gap-6 text-[10px] font-bold text-slate-500 uppercase tracking-widest">
            <span>发布于 {{ formatDate(project.createdAt) }}</span>
            <span>👁️ {{ project.viewCount }} 播放</span>
            <span>❤️ {{ project.likeCount }} 点赞</span>
          </div>
          <div class="prose prose-invert max-w-none">
            <p class="text-slate-400 leading-relaxed whitespace-pre-wrap">{{ project.description }}</p>
          </div>
        </div>

        <div class="space-y-6">
          <div class="bg-slate-900/40 border border-white/10 p-8 rounded-[2.5rem] text-center backdrop-blur-xl">
            <button 
              @click="handleLike"
              :class="['w-20 h-20 rounded-full flex items-center justify-center text-3xl transition-all mb-4 mx-auto border-2', 
                hasLiked ? 'bg-red-500 border-red-400 text-white scale-110 shadow-lg shadow-red-500/20' : 'bg-white/5 border-white/10 text-slate-500 hover:border-red-500/50 hover:text-red-500']"
            >
              ❤️
            </button>
            <p class="text-white font-black text-lg">{{ project.likeCount }}</p>
            <p class="text-slate-500 text-[10px] font-bold uppercase mt-1">给作品点个赞</p>
          </div>

          <div v-if="project.attachmentUrl" class="bg-blue-500/10 border border-blue-500/20 p-8 rounded-[2.5rem] backdrop-blur-xl">
            <h5 class="text-white font-black text-sm mb-4 flex items-center gap-2">
              <span class="text-blue-400 text-lg">📁</span> 附件资源
            </h5>
            <button 
              @click="handleDownload"
              class="w-full py-4 bg-blue-500 hover:bg-blue-600 text-white font-black rounded-2xl transition-all shadow-xl shadow-blue-500/20 active:scale-95 flex items-center justify-center gap-2"
            >
              <span>立即下载</span>
              <span class="text-xs opacity-50">({{ getFileName(project.attachmentUrl) }})</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>