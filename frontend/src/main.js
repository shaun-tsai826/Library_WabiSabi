import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
// 全域樣式（CSS 變數、基本設定）：只在這裡載入一次，所有元件都能用
import './styles/base.css'

const app = createApp(App)

app.use(router)

app.mount('#app')
