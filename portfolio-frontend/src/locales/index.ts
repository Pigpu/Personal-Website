import { createI18n } from 'vue-i18n'

// 定义我们的“词典”
const messages = {
  zh: {
    nav: {
      home: '主页',       // 对应原来的“主页”
      career: '生涯',     // 对应原来的“生涯”
      articles: '生活',   // 对应原来的“生活”
      projects: '作品',   // 对应原来的“作品”
      login: '登录'       // 替换原本的“LOGIN”
    }
  },
  en: {
    nav: {
      home: 'Home',
      career: 'Career',
      articles: 'Life',
      projects: 'Projects',
      login: 'LOGIN'
    }
  },
  ja: {
    nav: {
      home: 'ホーム',
      career: 'キャリア',
      articles: 'ライフ',
      projects: '作品',
      login: 'ログイン'
    }
  }
}

// 读取用户上次选择的语言，如果没有则默认中文
const savedLang = localStorage.getItem('site_lang') || 'zh'

const i18n = createI18n({
  legacy: false, // 必须设置为 false，才能在 Vue 3 的 Composition API (<script setup>) 中使用
  locale: savedLang,
  fallbackLocale: 'en', // 如果某种语言缺了某个词，默认用英文兜底
  messages
})

export default i18n