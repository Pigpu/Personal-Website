<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import axios from "axios";

const projects = ref<any[]>([]);
const searchKeyword = ref("");
const currentSort = ref("latest");
const userRole = localStorage.getItem("user_role");

const fetchProjects = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/projects`, {
      params: { sort: currentSort.value },
    });
    projects.value = res.data;
  } catch (error) {
    console.error("加载失败", error);
  }
};

const handleSearch = async () => {
  if (!searchKeyword.value) {
    fetchProjects();
    return;
  }
  const res = await axios.get(`http://localhost:8080/api/projects/search`, {
    params: { keyword: searchKeyword.value },
  });
  projects.value = res.data;
};

const handleDelete = async (id: number) => {
  if (!confirm("确定删除该作品吗？")) return;
  await axios.delete(`http://localhost:8080/api/projects/${id}`);
  fetchProjects();
};

watch(currentSort, fetchProjects);
onMounted(fetchProjects);
</script>

<template>
  <div class="min-h-screen px-6 pb-20">
    <div class="max-w-7xl mx-auto">
      <div class="flex justify-between items-center mb-6">
        <div class="text-left">
          <h2 class="text-4xl font-black bg-linear-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
            作品分享
          </h2>
          <p class="text-slate-500 text-sm mt-1">基于兴趣爱好或专业知识造出来的一些小作品展示</p>
        </div>
      </div>
      <div class="flex flex-col md:flex-row gap-6 justify-between items-center mb-12">
        <div class="relative w-full md:w-96">
          <input
            v-model="searchKeyword"
            @input="handleSearch"
            type="text"
            placeholder="搜索作品标题或描述..."
            class="w-full bg-white/5 border border-white/10 rounded-2xl py-3 px-12 text-white focus:border-blue-500/50 focus:ring-0 transition-all"
          />
          <span class="absolute left-4 top-3.5 text-slate-500">🔍</span>
        </div>

        <div class="flex items-center gap-2 bg-white/5 p-1 rounded-2xl border border-white/10">
          <button
            @click="currentSort = 'latest'"
            :class="['px-6 py-2 rounded-xl text-sm font-bold transition-all', currentSort === 'latest' ? 'bg-blue-500 text-white shadow-lg' : 'text-slate-400 hover:text-white']"
          >最新发布</button>
          <button
            @click="currentSort = 'likes'"
            :class="['px-6 py-2 rounded-xl text-sm font-bold transition-all', currentSort === 'likes' ? 'bg-blue-500 text-white shadow-lg' : 'text-slate-400 hover:text-white']"
          >最多点赞</button>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <router-link
          v-if="userRole === 'ROLE_ADMIN'"
          to="/projects/upload"
          class="group border-2 border-dashed border-white/10 rounded-[2.5rem] flex flex-col items-center justify-center p-10 hover:border-blue-500/50 hover:bg-blue-500/5 transition-all min-h-[400px]"
        >
          <div class="w-16 h-16 bg-blue-500/20 text-blue-400 rounded-full flex items-center justify-center text-3xl mb-4 group-hover:scale-110 transition-transform">+</div>
          <span class="text-slate-400 font-bold">发布新作品</span>
        </router-link>

        <div
          v-for="project in projects"
          :key="project.id"
          class="group bg-slate-900/40 border border-white/10 rounded-[2.5rem] overflow-hidden hover:border-white/20 transition-all flex flex-col"
        >
          <div class="relative aspect-video overflow-hidden">
            <img
              :src="project.coverUrl"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            />
            <div class="absolute top-4 left-4 px-3 py-1 bg-black/60 backdrop-blur-md rounded-full text-[10px] font-black text-white uppercase tracking-widest">
              {{ project.mediaType }}
            </div>
            <div
              v-if="userRole === 'ROLE_ADMIN'"
              class="absolute top-4 right-4 z-30 flex gap-2 opacity-0 group-hover:opacity-100 transition-all duration-300"
            >
              <button
                @click.stop.prevent="$router.push(`/projects/upload?id=${project.id}`)"
                class="p-2.5 bg-blue-500/20 hover:bg-blue-500 border border-blue-500/50 rounded-xl text-white backdrop-blur-md transition-all"
                title="编辑作品"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                  />
                </svg>
              </button>

              <button
                @click.stop.prevent="handleDelete(project.id)"
                class="p-2.5 bg-red-500/20 hover:bg-red-500 border border-red-500/50 rounded-xl text-white backdrop-blur-md transition-all"
                title="删除作品"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                  />
                </svg>
              </button>
            </div>
          </div>

          <div class="p-6 flex-1 flex flex-col">
            <h4 class="text-xl font-black text-white mb-2 line-clamp-1">{{ project.title }}</h4>
            <p class="text-slate-400 text-sm line-clamp-2 mb-6 flex-1">{{ project.description }}</p>

            <div class="flex items-center justify-between pt-4 border-t border-white/5">
              <div class="flex items-center gap-4 text-[10px] font-bold text-slate-500 uppercase">
                <span>👁️ {{ project.viewCount }}</span>
                <span>❤️ {{ project.likeCount }}</span>
              </div>
              <router-link
                :to="`/projects/${project.id}`"
                class="text-xs font-black text-blue-400 hover:text-blue-300 uppercase"
              >查看详情 →</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>