// 前端用到的提示訊息（文字來自 BDD.md），集中在這裡管理。
// API 的結果訊息（登入成功、已成功加入收藏…）由後端回傳，前端直接顯示 response 的 message，不寫在這裡。
// 這裡只放「前端自己判斷、自己顯示」的訊息；前端欄位驗證的文字與後端 Messages.java 相同。

export const MESSAGES = {
  // ===== 書籍查詢（前端驗證） =====
  ISBN_REQUIRED: '請輸入 ISBN 碼',
  ISBN_INVALID: 'ISBN 碼格式錯誤或無效',

  // ===== 登入 / 註冊（前端驗證） =====
  NICKNAME_REQUIRED: '請輸入暱稱',
  MOBILE_REQUIRED: '請輸入手機號碼',
  NICKNAME_INVALID: '暱稱僅限英文、數字、- 與 _',
  NICKNAME_TOO_LONG: '暱稱長度不可超過 20 個字元',
  MOBILE_INVALID: '請輸入有效的手機號碼格式 (10碼)',

  // ===== 未登入的提示 =====
  /** 未登入點「加入收藏」 */
  FAVORITE_LOGIN_REQUIRED: '請先登入或註冊會員以使用收藏功能',
  /** 未登入進入收藏頁 /favorites */
  FAVORITES_PAGE_LOGIN_REQUIRED: '請先登入後再檢視收藏內容',
  /** 未登入進入個人頁 /profile */
  PROFILE_PAGE_LOGIN_REQUIRED: '請先註冊/登入',

  // ===== 登出 =====
  /** 登出時 Session 早已失效（API 回 401），前端仍當作登出成功（與後端 LOGOUT_SUCCESS 相同） */
  LOGOUT_SUCCESS: '已成功登出',

  // ===== 收藏 =====
  /** 刪除前的確認視窗文字（DESIGN.md 4.9） */
  DELETE_CONFIRM: '確定要移除這本書的收藏嗎？',

  // ===== 通用 =====
  /** 連不上後端、或後端回應看不懂時（與後端的 SERVER_ERROR 相同） */
  SERVER_ERROR: '系統發生錯誤，請稍後再試',
}
