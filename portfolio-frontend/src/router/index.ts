import { createRouter, createWebHistory } from 'vue-router'
// 导入你刚创建的视图组件
import HomeView from '../views/HomeView.vue'
import CareerView from '../views/CareerView.vue'
import MomentsView from '../views/MomentsView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/career',
    name: 'career',
    component: CareerView
  },
  {
    path: '/moments',
    name: 'moments',
    component: MomentsView
  }
]

const router = createRouter({
  history: createWebHistory(), // 使用 HTML5 模式，URL 看起来很干净，没有 #
  routes,
  // 每次切换页面时，自动滚动到最顶部
  scrollBehavior() {
    return { top: 0 }
  }
})

export default router