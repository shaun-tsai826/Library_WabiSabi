// 前端欄位驗證：規則與後端相同（後端仍會再驗證一次，前端驗證只是讓使用者更快看到提示）。
// 每個函式回傳「錯誤訊息」；沒有錯誤時回傳空字串 ''。

import { MESSAGES } from '@/messages'

/** ISBN：去除前後空白後，必須是 13 碼數字 */
export function validateIsbn(isbn) {
  if (isbn === '') {
    return MESSAGES.ISBN_REQUIRED
  }
  if (!/^[0-9]{13}$/.test(isbn)) {
    return MESSAGES.ISBN_INVALID
  }
  return ''
}

/**
 * 登入/註冊（傳入已去除前後空白的值）。
 * 檢查順序與後端相同：暱稱空白 → 手機空白 → 暱稱格式 → 暱稱長度 → 手機格式
 */
export function validateLogin(nickname, mobile) {
  if (nickname === '') {
    return MESSAGES.NICKNAME_REQUIRED
  }
  if (mobile === '') {
    return MESSAGES.MOBILE_REQUIRED
  }
  if (!/^[A-Za-z0-9_-]+$/.test(nickname)) {
    return MESSAGES.NICKNAME_INVALID
  }
  if (nickname.length > 20) {
    return MESSAGES.NICKNAME_TOO_LONG
  }
  if (!/^[0-9]{10}$/.test(mobile)) {
    return MESSAGES.MOBILE_INVALID
  }
  return ''
}
