<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRoute } from "vue-router";
import { MdPreview } from "md-editor-v3";
import axios from "axios";
import "md-editor-v3/lib/preview.css";
import { useI18n } from "vue-i18n";
const { t } = useI18n();

const route = useRoute();
const article = ref<any>(null);
const isLoading = ref(true);
const comments = ref<any[]>([]);
const newComment = ref("");
const isLoggedIn = computed(() => !!localStorage.getItem("token"));
const currentParentId = ref<number | null>(null); // 记录父评论 ID
const replyToUsername = ref(""); // 记录被回复人的名字
const hasLiked = ref(false);
// --- 新增：删除弹窗的状态控制 ---
const showDeleteModal = ref(false);
const commentIdToDelete = ref<number | null>(null);
const likesList = ref<any[]>([]); // 存储点赞列表
const showLikesList = ref(false); // 控制列表折叠/展开

// 自定义消息提示状态
const showMessageModal = ref(false);
const messageConfig = ref({
  type: 'warning', // 'warning', 'error', 'success'
  title: '提示',
  content: ''
});

// 触发自定义提示的函数（2秒后自动消失）
const showMessage = (type: 'warning' | 'error' | 'success', title: string, content: string) => {
  messageConfig.value = { type, title, content };
  showMessageModal.value = true;
  setTimeout(() => {
    showMessageModal.value = false;
  }, 2000); // 2秒后自动关闭
};

const isAdmin = computed(() => localStorage.getItem('user_role') === 'ROLE_ADMIN');

// 获取文章详情
const fetchArticle = async () => {
  const id = route.params.id;
  try {
    const response = await fetch(`/api/articles/${id}`);
    article.value = await response.json();
  } catch (error) {
    console.error("文章加载失败:", error);
  } finally {
    isLoading.value = false;
  }
  // 文章加载完后，获取点赞状态
  if (isLoggedIn.value) {
    checkLikeStatus();
  }
  // 如果是管理员，顺便把点赞列表拉下来
  if (isAdmin.value) {
    fetchLikesList();
  }
};

// 点赞/取消点赞处理
const handleLike = async () => {
  if (!isLoggedIn.value) {
    showMessage('warning', t('article.msgNeedLoginTitle'), t('article.msgNeedLoginDesc'));
    return;
  }
  
  try {
    const res = await axios.post(`/api/articles/${article.value.id}/like`, {}, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    });
    
    // 后端返回了最新的数量和状态
    article.value.likeCount = res.data.likeCount;
    hasLiked.value = res.data.isLiked;
    
    // 如果是管理员，点赞后刷新一下列表
    if (isAdmin.value) fetchLikesList();
    
  } catch (error) {
    console.error("点赞操作失败", error);
    showMessage('error', t('article.msgOpFailTitle'), t('article.msgOpFailDesc'));
  }
};

// 2. 检查当前用户是否点赞
const checkLikeStatus = async () => {
  try {
    const res = await axios.get(`/api/articles/${route.params.id}/like-status`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    });
    hasLiked.value = res.data;
  } catch (error) {
    console.error("获取点赞状态失败", error);
  }
};

// 3. 获取点赞列表 (管理员)
const fetchLikesList = async () => {
  try {
    const res = await axios.get(`/api/articles/${route.params.id}/likes-list`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    });
    likesList.value = res.data;
  } catch (error) {
    console.error("获取点赞列表失败", error);
  }
};

// 格式化时间辅助函数
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString(); // 包含具体时间
};

onMounted(fetchArticle);

// 1. 加载评论
const fetchComments = async () => {
  const articleId = route.params.id;
  try {
    const response = await axios.get(
      `/api/comments/article/${articleId}`
    );
    comments.value = response.data;
  } catch (error) {
    console.error("加载评论失败", error);
  }
};

const commentTree = computed(() => {
  const map: Record<number, any> = {};
  const tree: any[] = [];

  // 深拷贝数据，避免直接修改原数组导致副作用
  const sourceData = JSON.parse(JSON.stringify(comments.value));

  // 初始化 Map
  sourceData.forEach((comment: any) => {
    map[comment.id] = { ...comment, children: [] };
  });

  // 组装树
  sourceData.forEach((comment: any) => {
    if (comment.parentId && map[comment.parentId]) {
      // 如果有父节点，塞进父节点的 children
      map[comment.parentId].children.push(map[comment.id]);
    } else {
      // 否则是顶层评论
      tree.push(map[comment.id]);
    }
  });

  // 按时间倒序：最新的在最上面
  return tree.sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
});

const setReply = (comment: any) => {
  // 1. 设置父 ID 和被回复人名
  currentParentId.value = comment.id;
  replyToUsername.value = comment.username;

  // 滚动到输入框
  const textarea = document.getElementById("comment-area");
  if (textarea) {
    textarea.scrollIntoView({ behavior: "smooth", block: "center" });
    setTimeout(() => textarea.focus(), 500); // 稍微延迟聚焦
  }
};

// --- 取消回复模式 ---
const cancelReply = () => {
  currentParentId.value = null;
  replyToUsername.value = "";
};

// 2. 提交评论
const submitComment: () => Promise<void> = async () => {
  if (!newComment.value.trim()) return;

  const payload = {
    content: newComment.value,
    articleId: Number(route.params.id),
    parentId: currentParentId.value, // 直接从 ref 读取，不走参数
    userId: null,
  };

  try {
    console.log("正在发送评论...", payload);
    const res = await axios.post(
      "/api/comments/save",
      payload
    );

    if (res.status === 200) {
      console.log("发布成功，准备清理现场...");
      // 1. 清空输入框
      newComment.value = "";
      // 2. 重置回复状态
      currentParentId.value = null;
      replyToUsername.value = "";
      // 3. 立即刷新列表
      await fetchComments();
      console.log("列表刷新完成");
    }
  } catch (error: any) {
    console.error("发布失败:", error);
    alert(t('article.msgPublishFail'));
  }
};

// 格式化时间
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

onMounted(fetchComments);

// 1. 新增：获取当前用户角色和用户名
const userRole = ref<string | null>(null);
const currentUsername = ref<string | null>(null);

onMounted(() => {
  fetchComments();
  // 从本地存储读取角色和用户名
  userRole.value = localStorage.getItem("user_role");
  currentUsername.value = localStorage.getItem("username");
});

// 2. 新增：删除评论函数
// 1. 点击删除按钮 -> 打开弹窗
const openDeleteModal = (id: number) => {
  commentIdToDelete.value = id;
  showDeleteModal.value = true;
};

// 2. 在弹窗里点击确定 -> 执行 API 删除
const confirmDelete = async () => {
  if (!commentIdToDelete.value) return;

  try {
    await axios.delete(
      `/api/comments/${commentIdToDelete.value}`
    );
    // 删除成功后
    showDeleteModal.value = false;
    commentIdToDelete.value = null;
    await fetchComments(); // 刷新列表
  } catch (error: any) {
    alert(t('article.msgDeleteFail') + (error.response?.data || ""));
  }
};
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <div
      v-if="isLoading"
      class="text-center py-20 text-slate-400"
    >{{ t('article.loading') }}</div>

    <div v-else-if="article">
      <header class="mb-12 text-center">
        <span class="text-blue-400 font-mono text-sm tracking-widest uppercase">
          {{ article.category }}
        </span>
        <h1 class="text-4xl md:text-5xl font-black mt-4 mb-6 leading-tight">
          {{ article.title }}
        </h1>
        <div class="flex items-center justify-center gap-6 text-slate-500 text-sm">
          <span>📅 {{ new Date(article.createdAt).toLocaleDateString() }}</span>
          <span>👁️ {{ article.viewCount }} {{ t('article.views') }}</span>
          <button
            @click="handleLike"
            class="hover:text-pink-400 transition-colors"
          >
            ❤️ {{ article.likeCount }} {{ t('article.likes') }}
          </button>
        </div>
      </header>

      <div class="bg-slate-900/40 backdrop-blur-xl border border-white/10 p-8 md:p-12 rounded-4xl shadow-2xl">
        <MdPreview
          :modelValue="article.content"
          theme="dark"
        />
      </div>

      <div class="mt-20 pt-10 border-t border-white/10 flex flex-col items-center justify-center gap-4">
          <button 
            @click="handleLike"
            :class="['group relative flex flex-col items-center justify-center w-24 h-24 rounded-full border-4 transition-all duration-300', 
              hasLiked ? 'bg-red-500 border-red-500 shadow-xl shadow-red-500/30 scale-110' : 'bg-slate-800 border-slate-700 hover:border-red-500 hover:bg-slate-700']"
          >
            <svg xmlns="http://www.w3.org/2000/svg" :class="['h-10 w-10 transition-colors', hasLiked ? 'text-white' : 'text-slate-400 group-hover:text-red-500']" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd" />
            </svg>
            <span v-if="hasLiked" class="absolute mt-16 text-xs font-bold text-white-500 animate-bounce">{{ t('article.liked') }}</span>
          </button>
          <p class="text-slate-500 text-sm">{{ hasLiked ? t('article.cancelLike') : t('article.promptLike') }}</p>

          <div v-if="isAdmin" class="mt-12 w-full max-w-md mx-auto">
            <button 
              @click="showLikesList = !showLikesList"
              class="w-full flex items-center justify-between px-6 py-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-slate-300 font-bold transition-all"
            >
              <span class="flex items-center gap-2">
                <span class="text-blue-400">📊</span> {{ t('article.adminLikes') }} ({{ likesList.length }})
              </span>
              <span :class="{'rotate-180': showLikesList}" class="transition-transform duration-300">▼</span>
            </button>
            
            <Transition name="fade">
              <div v-if="showLikesList" class="mt-2 bg-slate-900/50 border border-white/5 rounded-2xl overflow-hidden max-h-60 overflow-y-auto custom-scrollbar">
                <div v-if="likesList.length === 0" class="p-6 text-center text-slate-500 text-sm">
                  {{ t('article.noLikes') }}
                </div>
                <ul v-else class="divide-y divide-white/5">
                  <li v-for="like in likesList" :key="like.id" class="px-6 py-4 flex items-center justify-between hover:bg-white/5 transition-colors">
                    <span class="text-white font-bold text-sm">{{ like.username }}</span>
                    <span class="text-slate-500 text-xs">{{ formatDateTime(like.createdAt) }}</span>
                  </li>
                </ul>
              </div>
            </Transition>
          </div>
        </div>

      <footer class="mt-12 flex justify-center">
        <button
          @click="$router.push('/articles')"
          class="px-8 py-3 rounded-full border border-white/10 hover:bg-white/5 transition-all text-slate-400"
        >
          {{ t('article.backToList') }}
        </button>
      </footer>

      <section class="max-w-4xl mx-auto mt-20 px-6 pb-20">
        <div class="flex items-center gap-4 mb-10">
          <h3 class="text-2xl font-black text-white">{{ t('article.commentTitle') }}</h3>
          <span class="px-3 py-1 bg-blue-500/10 text-blue-400 text-xs font-bold rounded-full border border-blue-500/20">
            {{ comments.length }} {{ t('article.commentCount') }}
          </span>
        </div>

        <div
          v-if="isLoggedIn"
          class="bg-slate-900/40 backdrop-blur-xl border border-white/10 p-6 rounded-4xl mb-12"
        >
          <div
            v-if="currentParentId"
            class="flex items-center justify-between mb-4 px-4 py-2 bg-blue-500/10 border border-blue-500/20 rounded-xl"
          >
            <span class="text-xs text-blue-400">
              {{ t('article.replyingTo') }} <span class="font-bold">@{{ replyToUsername }}</span>
            </span>
            <button
              @click="cancelReply"
              class="text-xs text-slate-500 hover:text-slate-300 transition-colors"
            >{{ t('article.cancel') }}</button>
          </div>

          <textarea
            id="comment-area"
            v-model="newComment"
            :placeholder="currentParentId ? t('article.placeholderReply') : t('article.placeholderComment')"
            class="w-full bg-transparent border-none text-slate-200 placeholder-slate-500 resize-none focus:ring-0 min-h-25"
          ></textarea>

          <div class="flex justify-between items-center mt-4 pt-4 border-t border-white/5">
            <button
              @click="() => submitComment()"
              class="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-bold rounded-full transition-all shadow-lg shadow-blue-500/20"
            >
              {{ currentParentId ? t('article.submitReply') : t('article.submitComment') }}
            </button>
          </div>
        </div>

        <div
          v-else
          class="bg-white/5 border border-dashed border-white/10 p-8 rounded-4xl text-center mb-12"
        >
          <p class="text-slate-400 text-sm mb-4">{{ t('article.loginPrompt') }}</p>
          <router-link
            to="/login"
            class="text-blue-400 font-bold hover:underline"
          >{{ t('article.loginNow') }}</router-link>
        </div>

        <div class="space-y-10">
          <div
            v-for="comment in commentTree"
            :key="comment.id"
            class="flex flex-col"
          >

            <div class="group bg-slate-900/20 hover:bg-slate-900/40 border border-white/5 hover:border-white/10 p-6 rounded-4xl transition-all relative z-10">
              <div class="flex justify-between items-start mb-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-linear-to-br from-blue-500 to-emerald-500 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-lg shadow-blue-500/10">
                    {{ (comment.username || '?').charAt(0).toUpperCase() }}
                  </div>
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="text-sm font-bold text-slate-200">{{ comment.username || t('article.anonymous') }}</span>
                      <span
                        v-if="comment.role === 'ROLE_ADMIN'"
                        class="text-[10px] bg-blue-500/20 text-blue-400 px-1.5 py-0.5 rounded border border-blue-500/30 font-black tracking-tighter"
                      >{{ t('article.staff') }}</span>
                    </div>
                    <span class="text-[10px] text-slate-500 uppercase tracking-widest">{{ formatDate(comment.createdAt) }}</span>
                  </div>
                </div>

                <div class="flex items-center gap-3 mt-1">
                  <button
                    v-if="userRole === 'ROLE_ADMIN'|| comment.username === currentUsername"
                    @click="openDeleteModal(comment.id)"
                    class="text-xs text-red-500/50 hover:text-red-500 font-bold transition-colors px-2 py-1"
                  >{{ t('article.deleteBtn') }}</button>

                  <button
                    @click="setReply(comment)"
                    class="text-xs text-slate-500 hover:text-blue-400 font-bold transition-colors px-3 py-1 hover:bg-blue-400/10 rounded-full"
                  >{{ t('article.replyBtn') }}</button>
                </div>
              </div>
              <div class="text-slate-300 text-sm leading-relaxed pl-13">
                {{ comment.content }}
              </div>
            </div>

            <div
              v-if="comment.children && comment.children.length > 0"
              class="ml-12 mt-4 space-y-4 border-l-2 border-white/5 pl-6"
            >
              <div
                v-for="child in comment.children"
                :key="child.id"
                class="group bg-white/5 hover:bg-white/8 border border-white/5 p-5 rounded-3xl transition-all"
              >
                <div class="flex justify-between items-start mb-3">
                  <div class="flex items-center gap-2">
                    <div class="w-6 h-6 bg-slate-700 rounded-full flex items-center justify-center text-[10px] text-white font-bold">
                      {{ (child.username || '?').charAt(0).toUpperCase() }}
                    </div>
                    <span class="text-xs font-bold text-blue-400">{{ child.username  || t('article.anonymous') }}</span>
                    <span class="text-[10px] text-slate-500 uppercase font-medium">{{ t('article.repliedTo') }}</span>
                    <span class="text-[10px] text-slate-300">@{{ comment.username  || t('article.anonymous')  }}</span>
                  </div>
                  <div class="flex items-center gap-3 mt-1">
                    <button
                      v-if="userRole === 'ROLE_ADMIN'|| child.username === currentUsername"
                      @click="openDeleteModal(child.id)"
                      class="text-[12px] text-red-500/50 hover:text-red-500 font-bold transition-colors px-2"
                    >{{ t('article.deleteBtn') }}</button>
                    <button
                      @click="setReply(child)"
                      class="text-[12px] text-slate-500 hover:text-white font-black uppercase tracking-tighter transition-colors"
                    >{{ t('article.replyBtn') }}</button>
                  </div>
                </div>
                <p class="text-slate-300 text-sm pl-8">
                  {{ child.content }}
                </p>
              </div>
            </div>

          </div>

          <div
            v-if="comments.length === 0"
            class="py-20 text-center"
          >
            <p class="text-slate-500 text-sm">{{ t('article.noComments') }}</p>
          </div>
        </div>
      </section>
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
        <Transition name="fade">
          <div
            v-if="showDeleteModal"
            class="fixed inset-0 z-100 flex items-center justify-center p-6"
          >

            <div
              class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"
              @click="showDeleteModal = false"
            ></div>

            <div class="relative w-full max-w-xs bg-slate-900/90 backdrop-blur-2xl border border-white/10 p-8 rounded-[2.5rem] shadow-2xl scale-in-center">

              <div class="w-12 h-12 bg-red-500/10 text-red-500 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl border border-red-500/20">
                🗑️
              </div>

              <div class="text-center mb-6">
                <h3 class="text-lg font-black text-white">{{ t('article.delConfirmTitle') }}</h3>
                <p class="text-slate-400 text-xs mt-2 leading-relaxed">
                  {{ t('article.delConfirmDesc1') }}<span class="text-red-400">{{ t('article.delConfirmDesc2') }}</span>。
                </p>
              </div>

              <div class="space-y-3">
                <button
                  @click="confirmDelete"
                  class="w-full py-3 bg-red-500 hover:bg-red-600 text-white text-sm font-bold rounded-2xl transition-all shadow-lg shadow-red-500/30 active:scale-95"
                >
                  {{ t('article.delConfirmBtn') }}
                </button>

                <button
                  @click="showDeleteModal = false"
                  class="w-full py-3 bg-white/5 hover:bg-white/10 text-slate-300 text-sm font-bold rounded-2xl transition-all border border-white/5 active:scale-95"
                >
                  {{ t('article.delCancelBtn') }}
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>
    </div>
  </div>
</template>

<style>
/* 深度自定义预览器的样式，使其融入你的毛玻璃背景 */
.md-editor-preview-wrapper {
  background: transparent !important;
  padding: 0 !important;
}
.md-editor-preview {
  color: #cbd5e1 !important; /* slate-300 */
}
</style>

<style scoped>
/* 弹窗背景淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 弹窗内容缩放进场 */
.scale-in-center {
  animation: scale-up 0.3s cubic-bezier(0.16, 1, 0.3, 1);
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