<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { onBeforeRouteLeave } from "vue-router";
import { MdEditor } from "md-editor-v3";
import "md-editor-v3/lib/style.css";
import { useRouter } from "vue-router";

const router = useRouter();
const title = ref("");
const content = ref("# 开始你的写作...");
const category = ref("技术");
const isSaved = ref(false); // 标记是否已点击发布

// --- 逻辑 A: 草稿箱系统 ---
const DRAFT_KEY = "blog_draft_data";

// 自动保存草稿
watch(
  [title, content],
  ([newTitle, newContent]) => {
    if (newTitle || newContent) {
      const draft = { title: newTitle, content: newContent, time: Date.now() };
      localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
    }
  },
  { deep: true }
);

// 加载草稿
onMounted(() => {
  const savedDraft = localStorage.getItem(DRAFT_KEY);
  if (savedDraft) {
    const { title: dTitle, content: dContent } = JSON.parse(savedDraft);
    // 如果发现有未保存的内容，询问用户是否恢复
    if (dTitle || dContent) {
      if (confirm("发现上次未发布的草稿，是否恢复？")) {
        title.value = dTitle;
        content.value = dContent;
      } else {
        localStorage.removeItem(DRAFT_KEY); // 用户拒绝则清除
      }
    }
  }
  // 注册浏览器刷新拦截
  window.addEventListener("beforeunload", handleBeforeUnload);
});

// --- 逻辑 B: 拦截器 ---

// 1. 拦截浏览器刷新/关闭
const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (!isSaved.value && (title.value || content.value)) {
    e.preventDefault();
    e.returnValue = ""; // 现代浏览器必须设置此项才会弹出提示
  }
};

// 2. 拦截 Vue 路由跳转 (比如点击 Navbar)
onBeforeRouteLeave((to, from, next) => {
  if (!isSaved.value && (title.value || content.value)) {
    const answer = window.confirm(
      "当前文章尚未发布，离开将导致内容丢失，确定离开吗？"
    );
    if (answer) {
      next();
    } else {
      next(false); // 停留在当前页
    }
  } else {
    next();
  }
});

// 清理监听器
onUnmounted(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
});

// 处理图片上传逻辑
const onUploadImg = async (
  files: File[],
  callback: (urls: string[]) => void
) => {
  const res = await Promise.all(
    files.map((file) => {
      return new Promise((rev, rej) => {
        const form = new FormData();
        form.append("image", file);

        // 调用你刚才写的后端 Spring Boot 接口
        fetch("http://localhost:8080/api/upload", {
          method: "POST",
          body: form,
        })
          .then((res) => res.text()) // 后端返回的是字符串 URL
          .then((url) => rev(url))
          .catch((error) => rej(error));
      });
    })
  );

  // 将后端返回的图片 URL 注入编辑器
  callback(res.map((item: any) => item));
};

// 手动处理拖拽
const handleManualDrop = (event: DragEvent) => {
  // 1. 阻止浏览器默认打开图片的动作
  event.preventDefault();

  const files = event.dataTransfer?.files;
  if (files && files.length > 0) {
    // 过滤出图片文件
    const imageFiles = Array.from(files).filter((file) =>
      file.type.startsWith("image/")
    );
    if (imageFiles.length > 0) {
      // 2. 直接调用我们写好的上传函数
      onUploadImg(imageFiles, (urls) => {
        // 3. 将返回的 URL 手动追加到 Markdown 内容里
        urls.forEach((url) => {
          content.value += `\n![图片描述](${url})\n`;
        });
      });
    }
  }
};

// 必须还要阻止 dragover，否则 drop 事件不会触发
const handleDragOver = (event: DragEvent) => {
  event.preventDefault();
};

// 保存文章到数据库
const saveArticle = async () => {
  // 正则表达式：匹配 Markdown 中的第一张图片链接
  const imgMatch = content.value.match(/!\[.*?\]\((.*?)\)/);
  const firstImageUrl = imgMatch ? imgMatch[1] : "";
  const articleData = {
    title: title.value,
    content: content.value,
    category: category.value,
    coverUrl: firstImageUrl, // 自动提取第一张图作为封面
    summary: content.value.substring(0, 100) + "...", // 简单截取前100字作为摘要
  };
  try {
    // 2. 发起请求，并在此处【定义】response
    const response = await fetch("http://localhost:8080/api/articles", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(articleData),
    });

    // 3. 【使用】response 进行判断
    if (response.ok) {
      isSaved.value = true; // 标记已保存，此时拦截器会放行
      localStorage.removeItem(DRAFT_KEY); // 清除草稿箱
      alert("发布成功！");
      router.push("/articles"); // 发布成功后自动跳回列表页
    } else {
      alert("发布失败，请检查后端服务");
    }
  } catch (error) {
    console.error("保存失败:", error);
    alert("网络错误，无法连接到后端");
  }
};
</script>

<template>
  <div class="max-w-6xl mx-auto p-6">
    <div class="mb-6 flex gap-4">
      <select
        v-model="category"
        class="bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-slate-300 focus:outline-none focus:border-blue-500 transition-all appearance-none cursor-pointer"
      >
        <option value="技术">💻 技术</option>
        <option value="生活">📸 生活</option>
        <option value="杂谈">🎵 杂谈</option>
        <option value="其他">✍️ 其他</option>
      </select>
      <input
        v-model="title"
        placeholder="输入文章标题..."
        class="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-2xl font-bold focus:outline-none focus:border-blue-500 transition-all"
      />
      <button
        @click="saveArticle"
        class="px-8 py-3 bg-blue-600 hover:bg-blue-500 rounded-xl font-bold transition-all shadow-lg shadow-blue-500/20"
      >
        发布文章
      </button>
    </div>

    <div
      class="rounded-2xl overflow-hidden border border-white/10 shadow-2xl"
      @drop="handleManualDrop"
      @dragover="handleDragOver"
    >
      <MdEditor
        v-model="content"
        @onUploadImg="onUploadImg"
        theme="dark"
        class="min-h-150"
      />
    </div>
  </div>
</template>

<style>
/* 深度适配你的暗黑毛玻璃风格 */
.md-editor {
  position: relative;
  z-index: 10;
  --md-bk-color: rgba(15, 23, 42, 0.8) !important;
  backdrop-filter: blur(12px);
}
</style>