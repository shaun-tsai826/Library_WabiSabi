// 登入狀態：全站共用的一個變數（不使用 Pinia）。
// reactive 讓它變成「會通知畫面更新」的物件：auth.user 一改，導覽列就會自動跟著變。

import { reactive } from 'vue'
import { api } from '@/api'

export const auth = reactive({
  /** 目前登入的會員 { nickname, mobile }；未登入是 null */
  user: null,
  /** 是否已經問過後端「現在有沒有登入」 */
  ready: false,
})

let loadingPromise = null

/**
 * 打開網頁時呼叫一次 GET /api/auth/me，取得登入狀態。
 * 重新整理頁面後，瀏覽器還留著 Session Cookie，後端就會回傳會員資料，畫面不會被誤判成未登入。
 * 重複呼叫只會真的問後端一次（第二次起直接回傳同一個 Promise）。
 */
export function loadCurrentUser() {
  if (!loadingPromise) {
    loadingPromise = api.get('/api/auth/me').then((result) => {
      auth.user = result.ok ? result.data : null
      auth.ready = true
    })
  }
  return loadingPromise
}

/** 登入成功後呼叫 */
export function setLoggedIn(user) {
  auth.user = { nickname: user.nickname, mobile: user.mobile }
}

/** 登出、或發現 Session 已失效（API 回 401）時呼叫 */
export function setLoggedOut() {
  auth.user = null
}
