import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      // 在 import 路徑寫 @ 代表 src 資料夾，例如 import AppToast from '@/components/AppToast.vue'
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 開發時的「轉接」：瀏覽器打 http://localhost:5173/api/...，Vite 幫忙轉給後端 http://localhost:8080/api/...
    // 對瀏覽器來說網頁和 API 是同一個網址，所以不用處理 CORS，Session Cookie 也能正常運作
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
