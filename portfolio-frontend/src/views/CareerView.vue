<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import axios from "axios";

interface Career {
  id?: number;
  period: string;
  company: string;
  position: string;
  description: string;
  tags: string;
}

// 暂时通过 localStorage 判断角色，等做完登录，这里会自动生效
const userRole = ref(localStorage.getItem('user_role') || 'GUEST');

// 定义一个计算属性，判断是否为管理员
const isAdmin = computed(() => userRole.value === 'ROLE_ADMIN');

const careerList = ref<Career[]>([]);
const showModal = ref(false); // 控制弹窗显示
const isEditing = ref(false);
const form = ref({
  id: undefined,
  startDate: "", // 开始日期
  endDate: "", // 结束日期
  isCurrent: false, // 是否至今
  company: "",
  position: "",
  description: "",
  tagsList: [] as string[], // 改用数组存储标签
});

const newTag = ref(""); // 当前正在输入的标签名

// 2. 添加标签的逻辑
const addTag = () => {
  const tag = newTag.value.trim();
  if (tag && !form.value.tagsList.includes(tag)) {
    form.value.tagsList.push(tag);
  }
  newTag.value = ""; // 清空输入框
};

// 3. 移除标签
const removeTag = (index: number) => {
  form.value.tagsList.splice(index, 1);
};

const fetchCareers = async () => {
  const res = await axios.get("/api/career/list");
  careerList.value = res.data;
};

const submitForm = async () => {
  // 拼接日期字符串
  const period = form.value.isCurrent
    ? `${form.value.startDate} - 至今`
    : `${form.value.startDate} - ${form.value.endDate}`;

  const payload = {
    ...form.value,
    period: period,
    tags: form.value.tagsList.join(","), // 数组转回逗号隔开的字符串
  };

  await axios.post("/api/career/save", payload);
  closeModal();
  fetchCareers();
};

// 打开弹窗进行新增
const openAddModal = () => {
  resetForm();
  showModal.value = true;
};

// 打开弹窗进行编辑
const editItem = (item: any) => {
  const [start, end] = item.period.split(" - ");
  form.value = {
    ...item,
    startDate: start,
    endDate: end === "至今" ? "" : end,
    isCurrent: end === "至今",
    tagsList: item.tags ? item.tags.split(",") : [],
  };
  isEditing.value = true;
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  resetForm();
};

const resetForm = () => {
  form.value = {
    id: undefined, // 确保清除 ID，防止“新增”变成“修改”
    startDate: "", // 清空开始日期
    endDate: "", // 清空结束日期
    isCurrent: false, // 默认不勾选“至今”
    company: "",
    position: "",
    description: "",
    tagsList: [], // 清空标签数组
  };
  newTag.value = ""; // 同时清空正在输入的标签文本框
  isEditing.value = false;
};

const showDeleteModal = ref(false); // 控制删除弹窗显示
const itemToDelete = ref<number | null>(null); // 记录待删除的 ID

// 修改函数签名，允许 id 为 number 或 undefined
const openDeleteModal = (id: number | undefined) => {
  if (id !== undefined) {
    itemToDelete.value = id;
    showDeleteModal.value = true;
  } else {
    console.warn("尝试删除一个没有 ID 的项目");
  }
};

// 真正执行删除的操作
const confirmDelete = async () => {
  if (itemToDelete.value !== null) {
    await axios.delete(
      `/api/career/delete/${itemToDelete.value}`
    );
    showDeleteModal.value = false;
    itemToDelete.value = null;
    fetchCareers(); // 刷新列表
  }
};

const closeDeleteModal = () => {
  showDeleteModal.value = false;
  itemToDelete.value = null;
};

onMounted(fetchCareers);
</script>

<template>
  <div class="relative min-h-screen text-slate-100 font-sans overflow-x-hidden">
    <div class="relative z-10 px-8 pb-8">
      <header class="max-w-4xl mx-auto flex justify-between items-center mb-12">
        <div>
          <h1 class="text-4xl font-black bg-linear-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
            生涯经历
          </h1>
          <p class="text-slate-500 text-sm mt-1">这里展示我的生涯经历（只有我可以修改哦）</p>
        </div>

        <button
          v-if="isAdmin"
          @click="openAddModal"
          class="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-5 py-2.5 rounded-full font-bold transition-all shadow-lg shadow-blue-900/40 active:scale-95"
        >
          <span class="text-xl">+</span> 添加经历
        </button>
      </header>

      <main class="max-w-4xl mx-auto grid gap-6">
        <div
          v-for="item in careerList"
          :key="item.id"
          class="group p-8 bg-white/5 border border-white/10 rounded-4xl hover:bg-white/8 transition-all"
        >
          <div class="flex justify-between">
            <div class="space-y-1">
              <div class="text-blue-500 font-mono text-sm font-bold">{{ item.period }}</div>
              <h3 class="text-2xl font-bold">{{ item.company }}</h3>
              <p class="text-slate-400 font-medium">{{ item.position }}</p>
            </div>
            <div class="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                v-if="isAdmin"
                @click="editItem(item)"
                class="p-2 text-slate-400 hover:text-blue-400 transition-colors"
              >编辑</button>
              <button
                v-if="isAdmin"
                @click="openDeleteModal(item.id)"
                class="p-2 text-slate-400 hover:text-red-400 transition-colors"
              >删除</button>
            </div>
          </div>
          <p class="mt-4 text-slate-300 leading-relaxed">{{ item.description }}</p>
          <div
            v-if="item.tags"
            class="mt-6 flex flex-wrap gap-2"
          >
            <span
              v-for="tag in item.tags.split(',')"
              :key="tag"
              class="px-3 py-1 text-xs font-semibold rounded-lg 
                 bg-blue-500/10 text-blue-300 border border-blue-500/20
                 group-hover:bg-blue-500/20 group-hover:border-blue-500/40 transition-colors"
            >
              # {{ tag.trim() }}
            </span>
          </div>
        </div>
      </main>
    </div>

    <Transition name="fade">
      <div
        v-if="showModal"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <div
          class="absolute inset-0 bg-slate-950/60 backdrop-blur-md"
          @click="closeModal"
        ></div>

        <div class="relative w-full max-w-lg bg-slate-900 border border-white/20 p-8 rounded-[2.5rem] shadow-2xl overflow-hidden">
          <h2 class="text-2xl font-bold mb-6">{{ isEditing ? '修改经历信息' : '记录新的足迹' }}</h2>

          <div class="space-y-5">
            <div class="space-y-2">
              <label class="text-xs text-slate-500 ml-1">起止时间</label>
              <div class="flex items-center gap-3">
                <div class="relative flex-1 group">
                  <input
                    type="date"
                    v-model="form.startDate"
                    class="w-full bg-slate-800/50 border border-white/10 rounded-xl p-3 text-sm outline-none 
                  focus:border-blue-500/50 focus:ring-4 focus:ring-blue-500/10 transition-all
                  scheme-dark"
                  />
                </div>

                <span class="text-slate-600 font-bold">→</span>

                <div class="relative flex-1 group">
                  <input
                    type="date"
                    v-model="form.endDate"
                    :disabled="form.isCurrent"
                    class="w-full bg-slate-800/50 border border-white/10 rounded-xl p-3 text-sm outline-none 
                  focus:border-blue-500/50 focus:ring-4 focus:ring-blue-500/10 transition-all
                  disabled:opacity-20 disabled:cursor-not-allowed scheme-dark"
                  />
                </div>
              </div>
              <div class="flex items-center justify-between p-3 bg-white/5 rounded-xl border border-white/10 mt-1">
                <span class="text-s text-slate-400">目前在职 / 在读（至今）</span>
                <div
                  @click="form.isCurrent = !form.isCurrent"
                  :class="form.isCurrent ? 'bg-blue-600' : 'bg-slate-700'"
                  class="relative w-11 h-6 rounded-full cursor-pointer transition-colors duration-300"
                >
                  <div
                    :class="form.isCurrent ? 'translate-x-6' : 'translate-x-1'"
                    class="absolute top-1 w-4 h-4 bg-white rounded-full transition-transform duration-300 shadow-sm"
                  ></div>
                </div>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1">
                <label class="text-xs text-slate-500 ml-1">组织/机构</label>
                <input
                  v-model="form.company"
                  class="w-full bg-slate-800 rounded-xl p-3 outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="如：重庆邮电大学"
                />
              </div>
              <div class="space-y-1">
                <label class="text-xs text-slate-500 ml-1">职位/专业</label>
                <input
                  v-model="form.position"
                  class="w-full bg-slate-800 rounded-xl p-3 outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="如：软件工程"
                />
              </div>
            </div>

            <div class="space-y-1">
              <label class="text-xs text-slate-500 ml-1">详细描述</label>
              <textarea
                v-model="form.description"
                class="w-full bg-slate-800 rounded-xl p-3 outline-none focus:ring-2 focus:ring-blue-500 h-24"
                placeholder="具体负责的内容或取得的成就..."
              ></textarea>
            </div>

            <div class="space-y-2">
              <label class="text-xs text-slate-500 ml-1">技术标签 (回车添加)</label>

              <div
                v-if="form.tagsList.length > 0"
                class="flex flex-wrap gap-2 mb-3 animate-in fade-in duration-300"
              >
                <span
                  v-for="(tag, index) in form.tagsList"
                  :key="index"
                  class="px-3 py-1 bg-blue-500/10 text-blue-300 rounded-lg text-xs flex items-center gap-2 border border-blue-500/20 group/tag"
                >
                  {{ tag }}
                  <button
                    @click="removeTag(index)"
                    class="hover:text-red-400 transition-colors"
                  >×</button>
                </span>
              </div>

              <div class="relative group">
                <input
                  v-model="newTag"
                  @keydown.enter.prevent="addTag"
                  class="w-full bg-slate-800 rounded-xl p-3 pr-12 outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                  placeholder="输入技术按回车添加..."
                />

                <button
                  @click="addTag"
                  class="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 flex items-center justify-center 
                   text-2xl text-slate-500 hover:text-blue-500 hover:bg-blue-500/10 rounded-lg transition-all"
                >
                  +
                </button>
              </div>
            </div>
          </div>

          <div class="mt-8 flex gap-3">
            <button
              @click="submitForm"
              class="flex-1 bg-blue-600 hover:bg-blue-500 py-3.5 rounded-2xl font-bold transition-all shadow-lg shadow-blue-900/40"
            >
              {{ isEditing ? '确认修改' : '立即发布' }}
            </button>
            <button
              @click="closeModal"
              class="px-6 bg-slate-800 hover:bg-slate-700 py-3.5 rounded-2xl font-bold transition-colors"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="fade">
      <div
        v-if="showDeleteModal"
        class="fixed inset-0 z-60 flex items-center justify-center p-4"
      >
        <div
          class="absolute inset-0 bg-slate-950/80 backdrop-blur-sm"
          @click="closeDeleteModal"
        ></div>

        <div class="relative w-full max-w-sm bg-slate-900 border border-red-500/20 p-8 rounded-4xl shadow-2xl text-center">
          <div class="mx-auto w-16 h-16 bg-red-500/10 rounded-full flex items-center justify-center mb-4">
            <span class="text-3xl text-red-500">⚠️</span>
          </div>

          <h3 class="text-xl font-bold text-white mb-2">确认删除？</h3>
          <p class="text-slate-400 text-sm mb-8">
            此操作不可撤销，这条经历将从你的作品集中永久移除。
          </p>

          <div class="flex gap-3">
            <button
              @click="confirmDelete"
              class="flex-1 bg-red-600 hover:bg-red-500 py-3 rounded-xl font-bold transition-colors shadow-lg shadow-red-900/40"
            >
              确认删除
            </button>
            <button
              @click="closeDeleteModal"
              class="flex-1 bg-slate-800 hover:bg-slate-700 py-3 rounded-xl font-bold transition-colors"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </Transition>

  </div>
</template>

<style scoped>
/* 定义弹窗淡入淡出的动效 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* 隐藏滚动条但保留功能 (选配) */
::-webkit-scrollbar {
  width: 0px;
}

/* 让原生日期选择器的日历图标颜色变亮，或者直接隐藏它（如果你想更简洁） */
input[type="date"]::-webkit-calendar-picker-indicator {
  filter: invert(1); /* 反转颜色，让图标在深色背景下变白 */
  opacity: 0.5;
  cursor: pointer;
}

input[type="date"]::-webkit-calendar-picker-indicator:hover {
  opacity: 1;
}
</style>