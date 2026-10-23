// 呼叫後端 API 的小工具：用瀏覽器內建的 fetch，不用另外安裝 axios。
//
// 每次呼叫都回傳同樣格式的物件，頁面用起來很單純：
//   { ok: true/false, status: HTTP 狀態碼, data: 後端回傳的 JSON }
// 失敗時 data.message 一定有文字，可以直接拿去顯示。
//
// 用法：
//   const result = await api.get('/api/books/9789861343722')
//   if (result.ok) { ...result.data.title... } else { showToast(result.data.message, 'error') }

import { MESSAGES } from '@/messages'

async function request(method, url, body) {
  const options = { method, headers: {} }
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }

  let response
  try {
    // 網頁和 API 同一個網址（Vite proxy），fetch 預設就會帶上 Session Cookie
    response = await fetch(url, options)
  } catch {
    // 連不上（例如後端沒啟動、網路斷線）
    return { ok: false, status: 0, data: { message: MESSAGES.SERVER_ERROR } }
  }

  // 讀取回應的 JSON；沒有內容或不是 JSON 時當作 null
  let data = null
  try {
    data = await response.json()
  } catch {
    data = null
  }

  // 失敗卻沒有 message（例如 Vite 轉接時後端沒開）：補上通用訊息
  if (!response.ok && !(data && data.message)) {
    data = { message: MESSAGES.SERVER_ERROR }
  }

  return { ok: response.ok, status: response.status, data }
}

export const api = {
  get: (url) => request('GET', url),
  post: (url, body) => request('POST', url, body),
  put: (url, body) => request('PUT', url, body),
  delete: (url) => request('DELETE', url),
}
