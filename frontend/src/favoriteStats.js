// 收藏統計的計算（BDD v1.5.0 需求 11、DESIGN.md 4.14～4.16）。
// 這裡都是「純函式」：給收藏清單，回傳算好的數字，不碰畫面也不呼叫 API，方便閱讀與檢查。
//
// 每筆收藏長這樣（GET /api/favorites）：
//   { id, isbn, title, author, publishDate, category: '散文', createdAt: '2026-10-24T10:15:30.123456' }

const UNCATEGORIZED = '未分類'

/** 把日期轉成「年-月」字串，例如 2026-10（月份補 0，和 createdAt 的前 7 個字同格式） */
function monthKey(date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${date.getFullYear()}-${month}`
}

/**
 * 每個種類的顏色（DESIGN.md 第 2 節圖表色票）。
 * 種類清單第 0 個是「未分類」，所以「小說」是第 1 個 → --chart-1，依此類推。
 * 萬一出現清單以外的種類（理論上不會），用「其他」的顏色。
 */
export function categoryColor(name, categories) {
  if (name === UNCATEGORIZED) {
    return 'var(--chart-uncategorized)'
  }
  const index = categories.indexOf(name)
  return index > 0 ? `var(--chart-${index})` : 'var(--chart-10)'
}

/**
 * 同數時的排序：依種類清單順序，但「未分類」排最後（BDD v1.5.0 範例：散文 1 本排在未分類 1 本前面）。
 * 清單以外的種類（理論上不會）排在未分類前面。
 */
function orderOf(name, categories) {
  if (name === UNCATEGORIZED) {
    return categories.length + 1
  }
  const index = categories.indexOf(name)
  return index === -1 ? categories.length : index
}

/**
 * 種類比例（圓餅圖與圖例）：依本數由多到少，同數時依種類清單順序（未分類最後）。
 * 回傳 [{ name: '心理勵志', count: 2, percent: 50, color: 'var(--chart-5)' }, ...]
 */
export function categoryBreakdown(favorites, categories) {
  const counts = {}
  for (const item of favorites) {
    counts[item.category] = (counts[item.category] || 0) + 1
  }

  const total = favorites.length
  return Object.keys(counts)
    .map((name) => ({
      name,
      count: counts[name],
      percent: Math.round((counts[name] / total) * 100), // 四捨五入到整數
      color: categoryColor(name, categories),
    }))
    .sort((a, b) => b.count - a.count || orderOf(a.name, categories) - orderOf(b.name, categories))
}

/**
 * 總覽數字卡：收藏總數、本月新增、最多的種類。
 * today 可以傳入日期（方便說明與測試）；沒傳就用今天。
 */
export function summary(favorites, categories, today = new Date()) {
  const thisMonth = monthKey(today)
  const breakdown = categoryBreakdown(favorites, categories)
  return {
    total: favorites.length,
    addedThisMonth: favorites.filter((item) => item.createdAt.slice(0, 7) === thisMonth).length,
    // breakdown 已經依本數排好，第一個就是最多的種類（同數時是清單中較前面的）
    topCategory: breakdown.length > 0 ? breakdown[0].name : '—',
  }
}

/**
 * 最近 12 個月（含本月）每個月新增的收藏本數，由舊到新。
 * 回傳 [{ key: '2025-11', year: 2025, month: 11, count: 0 }, ..., { key: '2026-10', year: 2026, month: 10, count: 3 }]
 */
export function monthlyCounts(favorites, today = new Date()) {
  const months = []
  for (let i = 11; i >= 0; i--) {
    // new Date(年, 月 - i, 1)：月份小於 0 時 JavaScript 會自動往前一年，例如 10 月往前 11 個月是去年 11 月
    const date = new Date(today.getFullYear(), today.getMonth() - i, 1)
    months.push({ key: monthKey(date), year: date.getFullYear(), month: date.getMonth() + 1, count: 0 })
  }

  for (const item of favorites) {
    const month = months.find((m) => m.key === item.createdAt.slice(0, 7))
    if (month) {
      month.count++ // 12 個月以前的收藏找不到對應月份，就不算進來
    }
  }
  return months
}
