<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import { MdEditor } from "md-editor-v3";
import "md-editor-v3/lib/style.css";
import axios from "axios";

const router = useRouter();
const route = useRoute();

// --- 基础数据 ---
const title = ref("");
const content = ref("# 开始你的写作...");
const category = ref("技术");

// --- 需求：自定义下拉菜单状态 ---
const categories = ["技术", "生活", "杂谈", "其他"];
const isSelectOpen = ref(false);

// --- UI 状态控制 ---
const isSubmitting = ref(false);
const showLeaveModal = ref(false);     // 离开警告弹窗
const showSuccessModal = ref(false);   // 成功提示弹窗
const nextRoute = ref<any>(null);      // 暂存跳转目标
const forceLeave = ref(false);         // 强制离开开关

const isEditMode = computed(() => !!route.query.id);
const DRAFT_KEY = "blog_draft_data";

// 脏检查
const isModified = computed(() => {
  return title.value !== "" || content.value !== "# 开始你的写作...";
});

// --- 1. 下拉菜单选择逻辑 ---
const selectCategory = (cat: string) => {
  category.value = cat;
  isSelectOpen.value = false;
};

// --- 2. 草稿箱逻辑 ---
watch(
  [title, content],
  ([newTitle, newContent]) => {
    if (!isEditMode.value && (newTitle || newContent)) {
      const draft = { title: newTitle, content: newContent, time: Date.now() };
      localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
    }
  },
  { deep: true }
);

// --- 3. 初始化加载 ---
const loadData = async () => {
  if (isEditMode.value) {
    try {
      const res = await axios.get(`/api/articles/${route.query.id}`);
      title.value = res.data.title;
      content.value = res.data.content;
      category.value = res.data.category;
    } catch (error) {
      alert("加载失败，返回列表");
      router.push("/articles");
    }
    return;
  }
  
  const savedDraft = localStorage.getItem(DRAFT_KEY);
  if (savedDraft) {
    const { title: dTitle, content: dContent } = JSON.parse(savedDraft);
    if (dTitle || dContent) {
      if (confirm("发现上次未发布的草稿，是否恢复？")) {
        title.value = dTitle;
        content.value = dContent;
      } else {
        localStorage.removeItem(DRAFT_KEY);
      }
    }
  }
};

onMounted(() => {
  loadData();
  window.addEventListener("beforeunload", handleBeforeUnload);
  // 点击页面其他地方关闭下拉菜单
  window.addEventListener("click", closeDropdown);
});

const closeDropdown = (e: Event) => {
  // 简单的点击外部关闭逻辑
  const target = e.target as HTMLElement;
  if (!target.closest('.custom-select')) {
    isSelectOpen.value = false;
  }
};

// --- 4. 路由守卫 ---
onBeforeRouteLeave((to, _from, next) => {
  if (isSubmitting.value || forceLeave.value) {
    next();
    return;
  }
  if (isModified.value) {
    showLeaveModal.value = true;
    nextRoute.value = to;
    next(false);
  } else {
    next();
  }
});

const confirmLeave = () => {
  showLeaveModal.value = false;
  forceLeave.value = true;
  setTimeout(() => {
    if (nextRoute.value) router.push(nextRoute.value.path);
    else router.back();
  }, 10);
};

const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (isModified.value && !isSubmitting.value) {
    e.preventDefault();
    e.returnValue = "";
  }
};
onUnmounted(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
  window.removeEventListener("click", closeDropdown);
});

// --- 5. 图片上传 (Mock/Real) ---
const onUploadImg = async (files: File[], callback: (urls: string[]) => void) => {
  const res = await Promise.all(
    files.map((file) => {
      return new Promise((rev, rej) => {
        const form = new FormData();
        form.append("image", file);
        const token = localStorage.getItem('token');
        fetch("/api/upload", {
          method: "POST",
          body: form,
          headers: { 'Authorization': token ? `Bearer ${token}` : '' },
        })
          .then((res) => {
            if (res.status === 403) throw new Error("权限不足");
            return res.text();
          }) 
          .then((url) => rev(url))
          .catch((error) => rej(error));
      });
    })
  );
  callback(res as string[]);
};

// --- 6. 保存文章 ---
const saveArticle = async () => {
  const imgMatch = content.value.match(/!\[.*?\]\((.*?)\)/);
  const firstImageUrl = imgMatch ? imgMatch[1] : "";
  const articleData = {
    title: title.value,
    content: content.value,
    category: category.value,
    coverUrl: firstImageUrl,
    summary: content.value.substring(0, 100) + "...",
  };

  isSubmitting.value = true;
  try {
    let res;
    if (isEditMode.value) {
      res = await axios.put(`/api/articles/${route.query.id}`, articleData);
    } else {
      res = await axios.post("/api/articles", articleData);
    }

    if (res.status === 200 || res.status === 201) {
      if (!isEditMode.value) localStorage.removeItem(DRAFT_KEY);
      showSuccessModal.value = true;
      setTimeout(() => {
        router.push("/articles");
      }, 1500);
    }
  } catch (error: any) {
    console.error(error);
    alert("保存失败");
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="max-w-6xl mx-auto p-6 pt-24 min-h-screen">
    <div class="mb-6 flex gap-4 relative z-50">
      
      <div class="relative w-40 custom-select">
        <div 
          @click.stop="isSelectOpen = !isSelectOpen"
          class="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-slate-300 cursor-pointer flex justify-between items-center hover:bg-white/10 transition-all select-none"
        >
          <span class="font-bold">{{ category }}</span>
          <span :class="{'rotate-180': isSelectOpen}" class="text-[10px] transition-transform duration-300 opacity-60">▼</span>
        </div>
        
        <Transition name="fade">
          <div v-if="isSelectOpen" class="absolute top-full left-0 w-full mt-2 bg-slate-900/95 backdrop-blur-2xl border border-white/10 rounded-xl overflow-hidden shadow-2xl z-50">
            <div 
              v-for="cat in categories" :key="cat"
              @click="selectCategory(cat)"
              class="px-4 py-3 text-sm text-slate-300 hover:bg-blue-600 hover:text-white cursor-pointer transition-colors"
            >
              {{ cat }}
            </div>
          </div>
        </Transition>
      </div>

      <input 
        v-model="title" 
        placeholder="输入文章标题..." 
        class="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-2xl font-bold focus:outline-none focus:border-blue-500 transition-all text-white placeholder-slate-600" 
      />
      
      <button 
        @click="saveArticle" 
        :disabled="isSubmitting" 
        class="px-8 py-3 bg-blue-600 hover:bg-blue-500 rounded-xl font-bold transition-all shadow-lg shadow-blue-500/20 text-white disabled:opacity-50 active:scale-95"
      >
        {{ isSubmitting ? '保存中...' : (isEditMode ? '保存修改' : '发布文章') }}
      </button>
    </div>

    <div class="rounded-2xl overflow-hidden border border-white/10 shadow-2xl relative z-0">
      <MdEditor v-model="content" theme="dark" class="h-[80vh]" @onUploadImg="onUploadImg" />
    </div>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showLeaveModal" class="fixed inset-0 z-[100] flex items-center justify-center p-6">
          <div class="absolute inset-0 bg-slate-950/60 backdrop-blur-md" @click="showLeaveModal = false"></div>
          <div class="relative w-full max-w-xs bg-slate-900/90 backdrop-blur-2xl border border-white/10 p-8 rounded-[2.5rem] shadow-2xl text-center scale-in-center">
            <div class="w-12 h-12 bg-amber-500/10 text-amber-500 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl border border-amber-500/20">⚠️</div>
            <h3 class="text-lg font-black text-white">未保存的更改</h3>
            <p class="text-slate-400 text-xs mt-2 leading-relaxed">离开后，你当前编辑的内容将会丢失。</p>
            <div class="space-y-3 mt-6">
              <button @click="confirmLeave" class="w-full py-3 bg-red-500/80 hover:bg-red-500 text-white text-sm font-bold rounded-2xl transition-all">确定离开</button>
              <button @click="showLeaveModal = false" class="w-full py-3 bg-white/5 text-slate-300 text-sm font-bold rounded-2xl border border-white/5">继续编辑</button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="fade">
        <div v-if="showSuccessModal" class="fixed inset-0 z-[100] flex items-center justify-center pointer-events-none">
          <div class="bg-slate-900/90 backdrop-blur-2xl border border-emerald-500/30 p-8 rounded-[2.5rem] shadow-2xl text-center scale-in-center pointer-events-auto">
            <div class="w-16 h-16 bg-emerald-500/20 text-emerald-400 rounded-full flex items-center justify-center mx-auto mb-4 text-3xl border border-emerald-500/20">🎉</div>
            <h3 class="text-xl font-black text-white">
              {{ isEditMode ? '修改已保存' : '发布成功' }}
            </h3>
            <p class="text-slate-400 text-sm mt-2">正在跳转至列表页...</p>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.scale-in-center { animation: scale-in 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) both; }
@keyframes scale-in { 0% { transform: scale(0.9); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
</style>