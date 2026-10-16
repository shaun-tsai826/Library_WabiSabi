// 路由：決定「哪個網址顯示哪一頁」，以及未登入時能不能進去（路由守衛）。

import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import FavoritesView from '@/views/FavoritesView.vue'
import ProfileView from '@/views/ProfileView.vue'
import { auth, loadCurrentUser } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: HomeView },
    { path: '/login', component: LoginView },
    {
      path: '/favorites',
      component: FavoritesView,
      // meta：自己加的備註。requiresAuth = 需要登入；loginMessage = 未登入時的提示
      meta: { requiresAuth: true, loginMessage: MESSAGES.FAVORITES_PAGE_LOGIN_REQUIRED },
    },
    {
      path: '/profile',
      component: ProfileView,
      meta: { requiresAuth: true, loginMessage: MESSAGES.PROFILE_PAGE_LOGIN_REQUIRED },
    },
    // 打錯網址：回首頁
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// 路由守衛：每次換頁「之前」都會執行
router.beforeEach(async (to) => {
  // 先等後端回答「現在有沒有登入」（只有打開網頁的第一次需要真的等）
  await loadCurrentUser()

  if (to.meta.requiresAuth && !auth.user) {
    showToast(to.meta.loginMessage, 'warning')
    return '/login' // 回傳網址 = 改去這一頁
  }
  // 不回傳任何東西 = 放行
})

export default router
