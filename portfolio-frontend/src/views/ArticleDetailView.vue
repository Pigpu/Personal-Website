<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRoute } from "vue-router";
import { MdPreview } from "md-editor-v3";
import axios from "axios";
import "md-editor-v3/lib/preview.css";

const route = useRoute();
const article = ref<any>(null);
const isLoading = ref(true);
const comments = ref<any[]>([]);
const newComment = ref("");
const isLoggedIn = computed(() => !!localStorage.getItem("token"));
const currentParentId = ref<number | null>(null); // 记录父评论 ID
const replyToUsername = ref(""); // 记录被回复人的名字
// --- 新增：删除弹窗的状态控制 ---
const showDeleteModal = ref(false);
const commentIdToDelete = ref<number | null>(null);

// 获取文章详情
const fetchArticle = async () => {
  const id = route.params.id;
  try {
    const response = await fetch(`http://localhost:8080/api/articles/${id}`);
    article.value = await response.json();
  } catch (error) {
    console.error("文章加载失败:", error);
  } finally {
    isLoading.value = false;
  }
};

// 点赞逻辑
const handleLike = async () => {
  await fetch(`http://localhost:8080/api/articles/${article.value.id}/like`, {
    method: "POST",
  });
  article.value.likeCount++; // 前端数值同步增加
};

onMounted(fetchArticle);

// 1. 加载评论
const fetchComments = async () => {
  const articleId = route.params.id;
  try {
    const response = await axios.get(
      `http://localhost:8080/api/comments/article/${articleId}`
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
      "http://localhost:8080/api/comments/save",
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
    alert("发布失败，请检查网络或登录状态");
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

// 1. 新增：获取当前用户角色
const userRole = ref<string | null>(null);

onMounted(() => {
  fetchComments();
  // 从本地存储读取角色
  userRole.value = localStorage.getItem("user_role");
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
    await axios.delete(`http://localhost:8080/api/comments/${commentIdToDelete.value}`);
    // 删除成功后
    showDeleteModal.value = false;
    commentIdToDelete.value = null;
    await fetchComments(); // 刷新列表
  } catch (error: any) {
    alert("删除失败：" + (error.response?.data || "权限不足或网络错误"));
  }
};
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <div
      v-if="isLoading"
      class="text-center py-20 text-slate-400"
    >正在加载文章...</div>

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
          <span>👁️ {{ article.viewCount }} 阅读</span>
          <button
            @click="handleLike"
            class="hover:text-pink-400 transition-colors"
          >
            ❤️ {{ article.likeCount }} 喜欢
          </button>
        </div>
      </header>

      <div
        v-if="article.coverUrl"
        class="mb-12 rounded-3xl overflow-hidden shadow-2xl"
      >
        <img
          :src="article.coverUrl"
          class="w-full object-cover max-h-100"
        />
      </div>

      <div class="bg-slate-900/40 backdrop-blur-xl border border-white/10 p-8 md:p-12 rounded-4xl shadow-2xl">
        <MdPreview
          :modelValue="article.content"
          theme="dark"
        />
      </div>

      <footer class="mt-12 flex justify-center">
        <button
          @click="$router.push('/articles')"
          class="px-8 py-3 rounded-full border border-white/10 hover:bg-white/5 transition-all text-slate-400"
        >
          ← 返回列表
        </button>
      </footer>

      <section class="max-w-4xl mx-auto mt-20 px-6 pb-20">
        <div class="flex items-center gap-4 mb-10">
          <h3 class="text-2xl font-black text-white">说点什么...</h3>
          <span class="px-3 py-1 bg-blue-500/10 text-blue-400 text-xs font-bold rounded-full border border-blue-500/20">
            {{ comments.length }} 条评论
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
              正在回复 <span class="font-bold">@{{ replyToUsername }}</span>
            </span>
            <button
              @click="cancelReply"
              class="text-xs text-slate-500 hover:text-slate-300 transition-colors"
            >取消</button>
          </div>

          <textarea
            id="comment-area"
            v-model="newComment"
            :placeholder="currentParentId ? '写下你的回复...' : '写下你的想法...'"
            class="w-full bg-transparent border-none text-slate-200 placeholder-slate-500 resize-none focus:ring-0 min-h-25"
          ></textarea>

          <div class="flex justify-between items-center mt-4 pt-4 border-t border-white/5">
            <span class="text-xs text-slate-500">支持 Markdown 语法</span>
            <button
              @click="() => submitComment()"
              class="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-bold rounded-full transition-all shadow-lg shadow-blue-500/20"
            >
              {{ currentParentId ? '提交回复' : '发布评论' }}
            </button>
          </div>
        </div>

        <div
          v-else
          class="bg-white/5 border border-dashed border-white/10 p-8 rounded-4xl text-center mb-12"
        >
          <p class="text-slate-400 text-sm mb-4">登录后即可参与讨论</p>
          <router-link
            to="/login"
            class="text-blue-400 font-bold hover:underline"
          >立即登录 →</router-link>
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
                      <span class="text-sm font-bold text-slate-200">{{ comment.username || '匿名用户' }}</span>
                      <span
                        v-if="comment.role === 'ROLE_ADMIN'"
                        class="text-[10px] bg-blue-500/20 text-blue-400 px-1.5 py-0.5 rounded border border-blue-500/30 font-black tracking-tighter"
                      >STAFF</span>
                    </div>
                    <span class="text-[10px] text-slate-500 uppercase tracking-widest">{{ formatDate(comment.createdAt) }}</span>
                  </div>
                </div>

                <div class="flex items-center gap-3 mt-1">
                  <button
                    v-if="userRole === 'ROLE_ADMIN'"
                    @click="openDeleteModal(comment.id)"
                    class="text-xs text-red-500/50 hover:text-red-500 font-bold transition-colors px-2 py-1"
                  >删除</button>

                  <button
                    @click="setReply(comment)"
                    class="text-xs text-slate-500 hover:text-blue-400 font-bold transition-colors px-3 py-1 hover:bg-blue-400/10 rounded-full"
                  >回复</button>
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
                    <span class="text-xs font-bold text-blue-400">{{ child.username  || '匿名用户' }}</span>
                    <span class="text-[10px] text-slate-500 uppercase font-medium">回复了</span>
                    <span class="text-[10px] text-slate-300">@{{ comment.username  || '匿名用户'  }}</span>
                  </div>
                  <div class="flex items-center gap-3 mt-1">
                    <button
                      v-if="userRole === 'ROLE_ADMIN'"
                      @click="openDeleteModal(child.id)"
                      class="text-[12px] text-red-500/50 hover:text-red-500 font-bold transition-colors px-2"
                    >删除</button>
                    <button
                      @click="setReply(child)"
                      class="text-[12px] text-slate-500 hover:text-white font-black uppercase tracking-tighter transition-colors"
                    >回复</button>
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
            <p class="text-slate-500 text-sm">还没有评论，快来抢沙发吧～</p>
          </div>
        </div>
      </section>
      <Teleport to="body">
  <Transition name="fade">
    <div v-if="showDeleteModal" class="fixed inset-0 z-100 flex items-center justify-center p-6">
      
      <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-md" @click="showDeleteModal = false"></div>
      
      <div class="relative w-full max-w-xs bg-slate-900/90 backdrop-blur-2xl border border-white/10 p-8 rounded-[2.5rem] shadow-2xl scale-in-center">
        
        <div class="w-12 h-12 bg-red-500/10 text-red-500 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl border border-red-500/20">
          🗑️
        </div>

        <div class="text-center mb-6">
          <h3 class="text-lg font-black text-white">确定要删除吗？</h3>
          <p class="text-slate-400 text-xs mt-2 leading-relaxed">
            如果是父评论，下面的所有回复也会一起消失，且<span class="text-red-400">无法恢复</span>。
          </p>
        </div>
        
        <div class="space-y-3">
          <button 
            @click="confirmDelete"
            class="w-full py-3 bg-red-500 hover:bg-red-600 text-white text-sm font-bold rounded-2xl transition-all shadow-lg shadow-red-500/30 active:scale-95"
          >
            确认删除
          </button>
          
          <button 
            @click="showDeleteModal = false"
            class="w-full py-3 bg-white/5 hover:bg-white/10 text-slate-300 text-sm font-bold rounded-2xl transition-all border border-white/5 active:scale-95"
          >
            我再想想
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
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* 弹窗内容缩放进场 */
.scale-in-center {
  animation: scale-up 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes scale-up {
  0% { transform: scale(0.9); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
</style>