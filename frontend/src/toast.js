// Toast 提示的狀態：任何頁面呼叫 showToast()，畫面上方就會出現提示，3 秒後自動消失。
// 同一時間只顯示一則，新的會直接取代舊的。

import { reactive } from 'vue'

export const toast = reactive({
  message: '',
  /** 'success'（成功／一般）、'warning'（警告）、'error'（錯誤） */
  type: 'success',
  visible: false,
})

let hideTimer = null

export function showToast(message, type = 'success') {
  toast.message = message
  toast.type = type
  toast.visible = true

  // 重新計時：前一則還沒消失就被新的取代時，從頭算 3 秒
  clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    toast.visible = false
  }, 3000)
}
