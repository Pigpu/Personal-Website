<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import axios from "axios";
import { useI18n } from 'vue-i18n';
const { t } = useI18n();

interface Paper {
  id?: number;
  title: string;
  writeDate: string;
  summary: string;
  pdfUrl: string; // PDF 文件的在线地址
}

// 权限控制
const userRole = ref(localStorage.getItem('user_role') || 'GUEST');
const isAdmin = computed(() => userRole.value === 'ROLE_ADMIN');

const paperList = ref<Paper[]>([]);
const showModal = ref(false);
const isEditing = ref(false);

// PDF 阅读器相关状态
const showPdfViewer = ref(false);
const currentPdfUrl = ref("");

const form = ref<Paper>({
  id: undefined,
  title: "",
  writeDate: "",
  summary: "",
  pdfUrl: "",
});

const isUploading = ref(false);

// 1. 获取论文列表
const fetchPapers = async () => {
  try {
    // ⚠️ 注意：这里假设你后端会写一个 /api/papers/list 的接口
    const res = await axios.get("/api/papers/list");
    // 按写作日期降序排序（最新的在最上面）
    paperList.value = res.data.sort((a: Paper, b: Paper) => 
      new Date(b.writeDate).getTime() - new Date(a.writeDate).getTime()
    );
  } catch (error) {
    console.error("获取论文列表失败", error);
  }
};

// 2. 上传 PDF 文件
const handleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  
  // 🌟 使用可选链安全获取第一个文件
  const file = target.files?.[0];
  
  // 🌟 明确告诉 TypeScript：如果 file 不存在，直接结束。这样后面的 file 就绝对是安全的了。
  if (!file) return;

  if (file.type !== "application/pdf") {
    alert("只能上传 PDF 格式的文件！");
    return;
  }

  isUploading.value = true;
  const formData = new FormData();
  // 现在 TypeScript 知道 file 绝对是 File 对象了，红线消失
  formData.append("file", file); 
  formData.append("type", "attachment"); // 复用你 FileController 里的 attachment 逻辑

  try {
    const res = await axios.post("/api/upload/project", formData);
    form.value.pdfUrl = res.data; // 后端返回的 URL
  } catch (error) {
    console.error("上传 PDF 失败", error);
    alert("上传失败，请重试！");
  } finally {
    isUploading.value = false;
  }
};

// 3. 提交表单 (新增/修改)
const submitForm = async () => {
  if (!form.value.title || !form.value.pdfUrl) {
    alert("标题和PDF文件是必填的！");
    return;
  }
  try {
    // ⚠️ 注意：需在后端新建对应的保存接口
    await axios.post("/api/papers/save", form.value);
    closeModal();
    fetchPapers();
  } catch (error) {
    console.error("保存失败", error);
  }
};

// 4. 打开/关闭弹窗操作
const openAddModal = () => {
  resetForm();
  showModal.value = true;
};

const editItem = (item: Paper) => {
  form.value = { ...item };
  isEditing.value = true;
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  resetForm();
};

const resetForm = () => {
  form.value = { id: undefined, title: "", writeDate: "", summary: "", pdfUrl: "" };
  isEditing.value = false;
};

// 5. 删除逻辑
const deletePaper = async (id: number | undefined) => {
  if (id === undefined) return;
  if (confirm("确定要删除这篇论文吗？")) {
    await axios.delete(`/api/papers/delete/${id}`);
    fetchPapers();
  }
};

// 6. 打开 PDF 阅读器
const viewPdf = (url: string) => {
  currentPdfUrl.value = url;
  showPdfViewer.value = true;
};

onMounted(fetchPapers);
</script>

<template>
  <div class="relative min-h-screen text-slate-100 font-sans overflow-x-hidden">
    <div class="relative z-10 px-8 pb-8">
      <header class="max-w-4xl mx-auto flex justify-between items-center mb-12">
        <div>
          <h1 class="text-4xl font-black bg-linear-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent pb-2 -mb-2">
            {{ t('paper.title') || '学术论文' }}
          </h1>
          <p class="text-slate-400 text-sm mt-1">{{ t('paper.subtitle') || '研究成果与学术记录' }}</p>
        </div>

        <button
          v-if="isAdmin"
          @click="openAddModal"
          class="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-5 py-2.5 rounded-full font-bold transition-all shadow-lg shadow-blue-900/40 active:scale-95"
        >
          <span class="text-xl">+</span> 上传论文
        </button>
      </header>

      <main class="max-w-4xl mx-auto grid gap-6">
        <div
          v-for="paper in paperList"
          :key="paper.id"
          class="group p-8 bg-white/5 border border-white/10 rounded-[2.5rem] hover:bg-white/10 transition-all cursor-pointer relative overflow-hidden"
          @click="viewPdf(paper.pdfUrl)"
        >
          <div class="absolute -right-20 -top-20 w-64 h-64 bg-blue-500/5 rounded-full blur-3xl group-hover:bg-blue-500/10 transition-colors pointer-events-none"></div>

          <div class="relative flex justify-between items-start">
            <div class="space-y-3 flex-1 pr-6">
              <div class="flex items-center gap-3">
                <span class="px-3 py-1 bg-blue-500/20 text-blue-400 text-xs font-black uppercase tracking-widest rounded-md border border-blue-500/30">
                  {{ paper.writeDate }}
                </span>
                <span class="text-xs text-slate-500 font-bold">PDF 文档</span>
              </div>
              
              <h3 class="text-2xl font-bold text-white group-hover:text-blue-300 transition-colors">
                {{ paper.title }}
              </h3>
              
              <p class="text-slate-400 leading-relaxed text-sm">
                {{ paper.summary }}
              </p>
            </div>

            <div class="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity" @click.stop>
              <button v-if="isAdmin" @click="editItem(paper)" class="p-2 text-slate-400 hover:text-blue-400 transition-colors bg-white/5 rounded-lg">编辑</button>
              <button v-if="isAdmin" @click="deletePaper(paper.id)" class="p-2 text-slate-400 hover:text-red-400 transition-colors bg-white/5 rounded-lg">删除</button>
            </div>
          </div>
        </div>
        
        <div v-if="paperList.length === 0" class="text-center py-20 text-slate-500">
          {{t('paper.loading')}}
        </div>
      </main>
    </div>

    <Transition name="fade">
      <div v-if="showModal" class="fixed inset-0 z-[60] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-950/80 backdrop-blur-md" @click="closeModal"></div>

        <div class="relative w-full max-w-lg bg-slate-900 border border-white/10 p-8 rounded-[2.5rem] shadow-2xl overflow-hidden">
          <h2 class="text-2xl font-bold mb-6 text-white">{{ isEditing ? '修改论文信息' : '上传新论文' }}</h2>

          <div class="space-y-5">
            <div class="space-y-1">
              <label class="text-xs text-slate-500 ml-1">论文标题</label>
              <input v-model="form.title" class="w-full bg-slate-800 rounded-xl p-3 outline-none focus:ring-2 focus:ring-blue-500 text-white" placeholder="输入论文题目" />
            </div>

            <div class="space-y-1">
              <label class="text-xs text-slate-500 ml-1">完成日期</label>
              <input type="date" v-model="form.writeDate" class="w-full bg-slate-800 rounded-xl p-3 outline-none focus:ring-2 focus:ring-blue-500 text-white scheme-dark" />
            </div>

            <div class="space-y-1">
              <label class="text-xs text-slate-500 ml-1">摘要 / 简介</label>
              <textarea v-model="form.summary" class="w-full bg-slate-800 rounded-xl p-3 outline-none focus:ring-2 focus:ring-blue-500 h-28 text-white" placeholder="简述论文的核心研究内容..."></textarea>
            </div>

            <div class="space-y-1">
              <label class="text-xs text-slate-500 ml-1">PDF 文件</label>
              <div class="flex items-center gap-3">
                <input type="file" accept="application/pdf" @change="handleFileUpload" class="hidden" id="pdf-upload" />
                <label for="pdf-upload" class="cursor-pointer bg-white/5 hover:bg-white/10 border border-white/10 px-4 py-3 rounded-xl text-sm font-bold text-slate-300 transition-colors flex-1 text-center">
                  {{ isUploading ? '正在上传...' : '选择 PDF 文件' }}
                </label>
              </div>
              <p v-if="form.pdfUrl" class="text-xs text-emerald-400 mt-2 pl-1 truncate">
                已上传: {{ form.pdfUrl.split('/').pop() }}
              </p>
            </div>
          </div>

          <div class="mt-8 flex gap-3">
            <button @click="submitForm" :disabled="isUploading" class="flex-1 bg-blue-600 hover:bg-blue-500 py-3.5 rounded-2xl font-bold text-white transition-all shadow-lg shadow-blue-900/40 disabled:opacity-50">
              {{ isEditing ? '确认修改' : '立即发布' }}
            </button>
            <button @click="closeModal" class="px-6 bg-slate-800 hover:bg-slate-700 py-3.5 rounded-2xl font-bold text-white transition-colors">
              取消
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="showPdfViewer" class="fixed inset-0 z-[100] flex items-center justify-center p-6 sm:p-12">
        <div class="absolute inset-0 bg-slate-950/80 backdrop-blur-sm" @click="showPdfViewer = false"></div>

        <div class="relative w-full max-w-6xl h-[85vh] bg-slate-900 border border-white/10 rounded-[2rem] shadow-2xl flex flex-col overflow-hidden">
          
          <div class="h-16 border-b border-white/10 flex items-center justify-between px-6 shrink-0 bg-slate-800/50">
            <span class="text-slate-300 font-bold text-sm flex items-center gap-2">
              <span class="text-red-500 text-lg">📄</span> 论文阅读
            </span>
            <button @click="showPdfViewer = false" class="w-10 h-10 rounded-full bg-white/5 hover:bg-red-500/20 text-slate-400 hover:text-red-400 flex items-center justify-center transition-colors text-xl font-bold">
              ×
            </button>
          </div>
          
          <div class="flex-1 w-full bg-slate-950/50 p-2 md:p-4">
            <iframe 
              :src="currentPdfUrl" 
              class="w-full h-full rounded-xl bg-white border border-slate-700 shadow-inner"
              title="PDF Viewer"
            ></iframe>
          </div>
        </div>
        
      </div>
    </Transition>

  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: scale(0.98);
}
</style>