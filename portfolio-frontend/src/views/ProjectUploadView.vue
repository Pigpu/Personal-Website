<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from "vue";
import axios from "axios";
import { useRouter, useRoute, onBeforeRouteLeave } from "vue-router";

const router = useRouter();
const route = useRoute(); // 1. 获取路由对象

// 判断是否为编辑模式 (通过 URL 是否有 id 参数)
const isEditMode = computed(() => !!route.query.id);

const isSubmitting = ref(false);
const coverInput = ref<HTMLInputElement | null>(null);
const mediaInput = ref<HTMLInputElement | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);

const categories = ["视频剪辑", "音乐相关", "技术开发", "其他"];
const isSelectOpen = ref(false);
const showLeaveModal = ref(false);
const nextRoute = ref<any>(null);
const forceLeave = ref(false);
const showSuccessModal = ref(false); // 新增这个状态

const form = reactive({
  title: "",
  description: "",
  category: "视频剪辑",
  coverUrl: "",
  mediaUrl: "",
  mediaType: "VIDEO",
  attachmentUrl: "",
});

const previews = reactive({ cover: "" });
const uploadProgress = reactive({ media: 0, attachment: 0 });

// 2. 初始化加载数据 (如果是编辑模式)
const loadProjectData = async () => {
  if (!isEditMode.value) return;

  try {
    const res = await axios.get(
      `http://localhost:8080/api/projects/${route.query.id}`
    );
    const data = res.data;

    // 回填表单
    Object.assign(form, {
      title: data.title,
      description: data.description,
      category: data.category,
      coverUrl: data.coverUrl,
      mediaUrl: data.mediaUrl,
      mediaType: data.mediaType,
      attachmentUrl: data.attachmentUrl,
    });

    // 回填封面预览 (直接用远程 URL)
    previews.cover = data.coverUrl;
  } catch (error) {
    console.error("加载详情失败", error);
    alert("无法加载作品数据，即将返回列表");
    router.push("/projects");
  }
};

// 页面加载时执行
onMounted(() => {
  window.addEventListener("beforeunload", handleBeforeUnload);
  loadProjectData(); // 执行回填逻辑
});

// ... (中间的 isModified, onBeforeRouteLeave, uplaodFile 等逻辑保持不变) ...
// 注意：isModified 在回填数据后会变为 true，这会触发“未保存”警告，这是符合预期的 (防止用户误触返回)

// ... 中间的 handleBeforeUnload, selectCategory 等函数保持不变 ...

const isModified = computed(() => {
  return (
    form.title !== "" ||
    form.description !== "" ||
    form.mediaUrl !== "" ||
    form.coverUrl !== ""
  );
});

onBeforeRouteLeave((to, from, next) => {
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
    if (nextRoute.value) {
      router.push(nextRoute.value.path);
    } else {
      router.back();
    }
  }, 10);
};

const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (isModified.value && !isSubmitting.value) {
    e.preventDefault();
    e.returnValue = "";
  }
};
onBeforeUnmount(() =>
  window.removeEventListener("beforeunload", handleBeforeUnload)
);

const selectCategory = (cat: string) => {
  form.category = cat;
  isSelectOpen.value = false;
};

const uplaodFile = async (
  event: any,
  role: "cover" | "media" | "attachment"
) => {
  // ... (这里的代码完全不用动，保持你原来的即可) ...
  const file = event.target.files[0];
  if (!file) return;
  if (role === "media")
    form.mediaType = file.type.startsWith("video") ? "VIDEO" : "AUDIO";
  if (role === "cover") previews.cover = URL.createObjectURL(file);
  const formData = new FormData();
  formData.append("file", file);
  formData.append("type", role);
  try {
    const res = await axios.post(
      "http://localhost:8080/api/upload/project",
      formData,
      {
        onUploadProgress: (progressEvent: any) => {
          const percent = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          if (role === "media") uploadProgress.media = percent;
          if (role === "attachment") uploadProgress.attachment = percent;
        },
      }
    );
    if (role === "cover") form.coverUrl = res.data;
    if (role === "media") form.mediaUrl = res.data;
    if (role === "attachment") form.attachmentUrl = res.data;
  } catch (error) {
    alert("上传失败");
  }
};

// 3. 修改提交逻辑：区分 新增 vs 更新
const submitProject = async () => {
  if (!form.mediaUrl || !form.coverUrl) {
    alert("请至少上传封面和媒体文件");
    return;
  }
  isSubmitting.value = true;
  try {
    let res;
    if (isEditMode.value) {
      // 编辑模式：PUT 请求
      res = await axios.put(
        `http://localhost:8080/api/projects/${route.query.id}`,
        form
      );
    } else {
      // 新增模式：POST 请求
      res = await axios.post("http://localhost:8080/api/projects/save", form);
    }

    if (res.status === 200 || res.status === 201) {
      showSuccessModal.value = true;
      // 成功后处理
      Object.assign(form, {
        title: "",
        description: "",
        category: "视频剪辑",
        coverUrl: "",
        mediaUrl: "",
        mediaType: "VIDEO",
        attachmentUrl: "",
      });
      previews.cover = "";
      setTimeout(() => {
        router.push("/projects");
      }, 1500);
    }
  } catch (error) {
    console.error("操作失败:", error);
    alert("操作失败，请检查网络或权限");
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="min-h-screen pt-24 px-6 pb-20">
    <div class="max-w-3xl mx-auto bg-slate-900/40 backdrop-blur-2xl border border-white/10 p-10 rounded-[3rem] shadow-2xl">
      <h2 class="text-3xl font-black text-white mb-8 flex items-center gap-3">
        <span class="text-blue-500">/</span>
        {{ isEditMode ? '编辑作品' : '发布新作品' }}
      </h2>

      <form
        @submit.prevent="submitProject"
        class="space-y-8"
      >
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <label class="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">作品标题</label>
            <input
              v-model="form.title"
              type="text"
              required
              class="w-full bg-white/5 border border-white/10 rounded-2xl py-3 px-4 text-white focus:border-blue-500/50 focus:ring-0 transition-all"
            />
          </div>
          <div class="space-y-2">
            <label class="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">所属分类</label>
            <div
              @click="isSelectOpen = !isSelectOpen"
              class="w-full bg-white/5 border border-white/10 rounded-2xl py-3 px-4 text-white cursor-pointer flex justify-between items-center hover:bg-white/10 transition-all"
            >
              <span>{{ form.category }}</span>
              <span
                :class="{'rotate-180': isSelectOpen}"
                class="text-[10px] transition-transform duration-300"
              >▼</span>
            </div>
            <Transition name="fade">
              <div
                v-if="isSelectOpen"
                class="absolute z-50 w-full bg-slate-900/95 backdrop-blur-2xl border border-white/10 rounded-2xl overflow-hidden shadow-2xl"
              >
                <div
                  v-for="cat in categories"
                  :key="cat"
                  @click="selectCategory(cat)"
                  class="px-4 py-3 text-sm text-slate-300 hover:bg-blue-500 hover:text-white cursor-pointer transition-colors"
                >
                  {{ cat }}
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <div class="space-y-2">
          <label class="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">作品简介</label>
          <textarea
            v-model="form.description"
            rows="4"
            class="w-full bg-white/5 border border-white/10 rounded-3xl py-3 px-4 text-white focus:border-blue-500/50 focus:ring-0 transition-all"
          ></textarea>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div class="space-y-2">
            <label class="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">封面图</label>
            <div
              @click="coverInput?.click()"
              class="aspect-video bg-white/5 border border-dashed border-white/10 rounded-2xl flex flex-col items-center justify-center cursor-pointer hover:bg-white/10 transition-all overflow-hidden relative"
            >
              <img
                v-if="previews.cover"
                :src="previews.cover"
                class="absolute inset-0 w-full h-full object-cover"
              />
              <span
                v-else
                class="text-[10px] text-slate-500"
              >点击上传</span>
              <input
                type="file"
                ref="coverInput"
                hidden
                @change="uplaodFile($event, 'cover')"
                accept="image/*"
              />
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">媒体文件 (MP4/MP3)</label>
            <div
              @click="mediaInput?.click()"
              class="aspect-video bg-white/5 border border-dashed border-white/10 rounded-2xl flex flex-col items-center justify-center cursor-pointer hover:bg-white/10 transition-all relative"
            >
              <div
                v-if="uploadProgress.media > 0 && uploadProgress.media < 100"
                class="absolute inset-0 bg-blue-500/20 flex items-center justify-center"
              >
                <span class="text-blue-400 font-black text-lg">{{ uploadProgress.media }}%</span>
              </div>
              <span
                v-if="form.mediaUrl"
                class="text-[10px] text-emerald-400 font-bold px-4 text-center break-all"
              >{{ form.mediaUrl.split('/').pop() }}</span>
              <span
                v-else
                class="text-[10px] text-slate-500"
              >点击上传媒体</span>
              <input
                type="file"
                ref="mediaInput"
                hidden
                @change="uplaodFile($event, 'media')"
                accept="video/*,audio/*"
              />
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">附件 (ZIP/PDF)</label>
            <div
              @click="fileInput?.click()"
              class="aspect-video bg-white/5 border border-dashed border-white/10 rounded-2xl flex flex-col items-center justify-center cursor-pointer hover:bg-white/10 transition-all relative"
            >
              <div
                v-if="uploadProgress.attachment > 0 && uploadProgress.attachment < 100"
                class="absolute inset-0 bg-slate-500/20 flex items-center justify-center"
              >
                <span class="text-white font-black text-lg">{{ uploadProgress.attachment }}%</span>
              </div>
              <span
                v-if="form.attachmentUrl"
                class="text-[10px] text-slate-300 font-bold px-4 text-center break-all uppercase"
              >已就绪</span>
              <span
                v-else
                class="text-[10px] text-slate-500"
              >点击上传附件</span>
              <input
                type="file"
                ref="fileInput"
                hidden
                @change="uplaodFile($event, 'attachment')"
                accept=".zip,.rar,.pdf,.7z"
              />
            </div>
          </div>
        </div>
        <div class="flex gap-6 w-full mt-10">
          <button
            type="button"
            @click="router.back()"
            class="flex-1 py-4 bg-white/5 hover:bg-white/10 text-slate-400 font-bold rounded-2xl border border-white/5 transition-all"
          >
            取消返回
          </button>

          <button
            type="submit"
            :disabled="isSubmitting"
            class="flex-1 py-4 bg-linear-to-r from-blue-600 to-emerald-600 text-white font-black rounded-2xl shadow-xl shadow-blue-500/20 hover:scale-[1.02] active:scale-95 transition-all disabled:opacity-50"
          >
            {{ isSubmitting ? '正在提交...' : (isEditMode ? '保存修改' : '确认发布作品') }}
          </button>
        </div>
      </form>
    </div>
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showLeaveModal"
          class="fixed inset-0 z-100 flex items-center justify-center p-6"
        >
          <div
            class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"
            @click="showLeaveModal = false"
          ></div>
          <div class="relative w-full max-w-xs bg-slate-900/90 backdrop-blur-2xl border border-white/10 p-8 rounded-[2.5rem] shadow-2xl text-center scale-in-center">
            <div class="w-12 h-12 bg-amber-500/10 text-amber-500 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl border border-amber-500/20">⚠️</div>
            <h3 class="text-lg font-black text-white">内容尚未保存</h3>
            <p class="text-slate-400 text-xs mt-2 leading-relaxed">现在离开，当前填写的作品资料将会丢失。</p>
            <div class="space-y-3 mt-6">
              <button
                @click="confirmLeave"
                class="w-full py-3 bg-red-500/80 hover:bg-red-500 text-white text-sm font-bold rounded-2xl transition-all"
              >确定离开</button>
              <button
                @click="showLeaveModal = false"
                class="w-full py-3 bg-white/5 text-slate-300 text-sm font-bold rounded-2xl border border-white/5"
              >继续编辑</button>
            </div>
          </div>
        </div>
      </Transition>
      <Transition name="fade">
          <div
            v-if="showSuccessModal"
            class="fixed inset-0 z-[100] flex items-center justify-center pointer-events-none"
          >
            <div class="bg-slate-900/90 backdrop-blur-2xl border border-emerald-500/30 p-8 rounded-[2.5rem] shadow-2xl text-center scale-in-center pointer-events-auto">
              <div class="w-16 h-16 bg-emerald-500/20 text-emerald-400 rounded-full flex items-center justify-center mx-auto mb-4 text-3xl border border-emerald-500/20">✨</div>
              <h3 class="text-xl font-black text-white">
                {{ isEditMode ? '作品已更新' : '发布成功' }}
              </h3>
              <p class="text-slate-400 text-sm mt-2">精彩内容已上线</p>
            </div>
          </div>
        </Transition>
    </Teleport>
  </div>
</template>