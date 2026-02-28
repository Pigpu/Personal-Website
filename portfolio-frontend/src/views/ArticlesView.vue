<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
const { t } = useI18n();

const router = useRouter();
const articles = ref<any[]>([]);
const showMessageModal = ref(false);
const messageConfig = ref({
  type: "warning", // 'warning', 'error', 'success'
  title: "提示",
  content: "",
});

const showMessage = (
  type: "warning" | "error" | "success",
  title: string,
  content: string
) => {
  messageConfig.value = { type, title, content };
  showMessageModal.value = true;
  setTimeout(() => {
    showMessageModal.value = false;
  }, 2000); // 2秒后自动关闭
};

// 暂时通过 localStorage 判断角色，等做完登录，这里会自动生效
const userRole = ref(localStorage.getItem("user_role") || "GUEST");

// 定义一个计算属性，判断是否为管理员
const isAdmin = computed(() => userRole.value === "ROLE_ADMIN");

// 从后端获取文章列表
const fetchArticles = async () => {
  try {
    const response = await fetch("/api/articles");
    articles.value = await response.json();
  } catch (error) {
    console.error("加载文章失败:", error);
  }
};

// 删除逻辑
const deleteArticle = async (id: number) => {
  // 前端权限第一层拦截
  if (!isAdmin.value) {
    showMessage("warning", "权限不足", "仅管理员可进行删除操作");
    return;
  }

  // 增加确认弹窗，防止误删
  if (!confirm("确定要永久删除这篇文章及其关联的物理图片吗？此操作不可撤销。"))
    return;

  try {
    const response = await fetch(`/api/articles/${id}`, {
      method: "DELETE",
      // 之后登录成功后，这里要带上 Token
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    // 2. 后端返回 403 时的处理
    if (response.status === 403 || response.status === 401) {
      showMessage("warning", "请重新登录", "您的会话已过期或权限不足");
      return;
    }

    if (response.ok) {
      // 成功后，无需刷新页面，直接从本地数组中过滤掉该文章，实现“即时消失”效果
      articles.value = articles.value.filter((article) => article.id !== id);
    } else {
      showMessage("warning", "删除失败", "服务器响应异常");
    }
  } catch (error) {
    console.error("删除请求出错:", error);
    showMessage("warning", "删除失败", "删除请求出错");
  }
};

onMounted(fetchArticles);

// 跳转到文章详情页
const goToDetail = (id: number) => {
  router.push(`/article/${id}`);
};
</script>

<template>
  <div class="max-w-7xl mx-auto">
    <div class="max-w-7xl mx-auto relative">
      <div class="flex justify-between items-center mb-12">
        <div class="text-left">
          <h2 class="text-4xl font-black bg-linear-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent pb-2 -mb-2">
            {{ t('life.title') }}
          </h2>
          <p class="text-slate-400 text-sm mt-1">{{ t('life.subtitle') }}</p>
        </div>
        <button
          v-if="isAdmin"
          @click="$router.push('/editor')"
          class="group flex items-center gap-2 px-6 py-3 bg-blue-600/20 hover:bg-blue-600/40 border border-blue-500/50 rounded-2xl backdrop-blur-md transition-all duration-300"
        >
          <span class="text-white group-hover:text-white font-bold mr-0.5">开始创作</span>
          <div class="w-6 h-6 bg-blue-500 rounded-lg flex items-center justify-center text-white group-hover:rotate-90 transition-transform">
            +
          </div>
        </button>
      </div>

    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <div
        v-for="article in articles"
        :key="article.id"
        @click="goToDetail(article.id)"
        class="group cursor-pointer bg-slate-900/40 backdrop-blur-md border border-white/10 rounded-3xl overflow-hidden hover:border-blue-500/50 transition-all duration-500 hover:-translate-y-2 shadow-xl hover:shadow-blue-500/10"
      >
        <div
          v-if="isAdmin"
          class="absolute top-4 right-4 z-30 flex gap-2 opacity-0 group-hover:opacity-100 transition-all duration-300"
        >
          <button
            @click.stop="$router.push(`/editor?id=${article.id}`)"
            class="p-2.5 bg-blue-500/20 hover:bg-blue-500 border border-blue-500/50 rounded-xl text-white backdrop-blur-md transition-all"
            title="编辑文章"
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
            @click.stop="deleteArticle(article.id)"
            class="p-2.5 bg-red-500/20 hover:bg-red-500 border border-red-500/50 rounded-xl text-white backdrop-blur-md transition-all"
            title="删除文章"
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
        <div class="h-48 overflow-hidden relative">
          <img
            v-if="article.coverUrl"
            :src="article.coverUrl"
            class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
          />
          <div
            v-else
            class="w-full h-full bg-linear-to-br from-slate-800 to-slate-900 flex items-center justify-center"
          >
            <span class="text-slate-600 font-black text-4xl">LOG</span>
          </div>
          <span class="absolute top-4 left-4 px-3 py-1 bg-blue-500/20 backdrop-blur-md border border-blue-500/30 rounded-full text-xs text-blue-300">
            {{ article.category }}
          </span>
        </div>

        <div class="p-6">
          <h3 class="text-xl font-bold mb-3 group-hover:text-blue-400 transition-colors">
            {{ article.title }}
          </h3>
          <p class="text-slate-400 text-sm line-clamp-2 mb-4 leading-relaxed">
            {{ article.summary }}
          </p>

          <div class="flex items-center justify-between text-xs text-slate-500">
            <div class="flex gap-4">
              <span>👁️ {{ article.viewCount }}</span>
              <span>❤️ {{ article.likeCount }}</span>
            </div>
            <span>{{ new Date(article.createdAt).toLocaleDateString() }}</span>
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