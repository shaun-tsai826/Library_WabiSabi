<!--
  收藏頁 /favorites（DESIGN.md 5.3）：需要登入（路由守衛把關）。
  (a) 已登入：收藏清單，每本書旁邊有「刪除」；沒有收藏時顯示空狀態框。
      v1.4.0：搜尋框、種類按鈕列、卡片內的種類下拉選單（BDD 需求 9、10）。
  (c) v1.5.0 統計檢視 /favorites?view=stats：總覽數字、種類比例圓餅圖、每月收藏趨勢折線圖（BDD 需求 11）。
  (b) 未登入提示畫面：只在「停留在本頁時 Session 失效（API 回 401）」時顯示（備用）。
-->
<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseInput from '@/components/BaseInput.vue'
import BaseSelect from '@/components/BaseSelect.vue'
import BookCover from '@/components/BookCover.vue'
import BaseButton from '@/components/BaseButton.vue'
import CategoryChips from '@/components/CategoryChips.vue'
import ViewToggle from '@/components/ViewToggle.vue'
import StatTiles from '@/components/StatTiles.vue'
import CategoryPieChart from '@/components/CategoryPieChart.vue'
import MonthlyLineChart from '@/components/MonthlyLineChart.vue'
import ErrorBox from '@/components/ErrorBox.vue'
import EmptyBox from '@/components/EmptyBox.vue'
import { api } from '@/api'
import { auth, setLoggedOut } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'
import { summary, categoryBreakdown, monthlyCounts } from '@/favoriteStats'

const route = useRoute() // 目前的網址資訊（例如 ?view=stats）
const router = useRouter()

const favorites = ref([]) // 收藏清單
const categories = ref([]) // 種類清單（從後端 GET /api/categories 取得）
const loading = ref(true) // 第一次載入中
const sessionExpired = ref(false) // Session 失效 → 顯示未登入提示畫面
const errorMessage = ref('') // 其他錯誤（例如連不上後端）
const deletingId = ref(null) // 正在刪除哪一筆（按鈕暫時停用）
const savingCategoryId = ref(null) // 正在儲存哪一筆的種類（下拉選單暫時停用）

// ===== 篩選條件（BDD v1.4.0 需求 9、10） =====
const keyword = ref('') // 搜尋框的內容
const selectedCategory = ref('') // 選中的種類；'' 代表「全部」

// ===== 清單／統計切換（BDD v1.5.0 需求 11、DESIGN.md 4.13） =====
// 狀態記在網址：/favorites 是清單、/favorites?view=stats 是統計，所以重新整理後還會停在原本的檢視。
// computed 的 get / set：讀取時看網址，ViewToggle 改值時就換網址。
const view = computed({
  get: () => (route.query.view === 'stats' ? 'stats' : 'list'),
  set: (value) => router.push({ query: value === 'stats' ? { view: 'stats' } : {} }),
})

// 統計用的數字：一律以「全部收藏」計算（不受搜尋與種類篩選影響）
const stats = computed(() => summary(favorites.value, categories.value))
const breakdown = computed(() => categoryBreakdown(favorites.value, categories.value))
const months = computed(() => monthlyCounts(favorites.value))

// onMounted：頁面顯示出來之後執行一次；收藏與種類清單同時載入
onMounted(() => {
  loadFavorites()
  loadCategories()
})

async function loadFavorites() {
  loading.value = true
  const result = await api.get('/api/favorites')
  loading.value = false

  if (result.ok) {
    favorites.value = result.data
  } else if (result.status === 401) {
    handleSessionExpired()
  } else {
    errorMessage.value = result.data.message
  }
}

async function loadCategories() {
  const result = await api.get('/api/categories')
  if (result.ok) {
    categories.value = result.data
  }
  // 失敗時清單維持空的：下拉選單仍會顯示每本書目前的種類，只是暫時不能改成別的
}

// ===== computed：由其他資料「算出來」的值，來源一變就自動重算 =====

/** 種類按鈕：只列出有書的種類，依後端種類清單的順序排列 */
const chips = computed(() => {
  const counts = {}
  for (const item of favorites.value) {
    counts[item.category] = (counts[item.category] || 0) + 1
  }
  // 依種類清單排序；萬一有清單以外的種類（理論上不會），排在最後
  const ordered = [...categories.value, ...Object.keys(counts).filter((c) => !categories.value.includes(c))]
  return ordered.filter((name) => counts[name] > 0).map((name) => ({ name, count: counts[name] }))
})

/** 套用種類與搜尋後，要顯示的收藏 */
const visibleFavorites = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return favorites.value.filter((item) => {
    // 條件 1：種類（「全部」就不過濾）
    if (selectedCategory.value !== '' && item.category !== selectedCategory.value) {
      return false
    }
    // 條件 2：關鍵字（書名、作者、ISBN 任一包含就算符合，不分大小寫）
    if (word === '') {
      return true
    }
    const text = `${item.title} ${item.author || ''} ${item.isbn}`.toLowerCase()
    return text.includes(word)
  })
})

/** 是否正在篩選（有選種類或有輸入關鍵字）：決定要不要顯示「顯示 X 本」 */
const isFiltering = computed(() => selectedCategory.value !== '' || keyword.value.trim() !== '')

// 選中的種類變成 0 本時（改了種類或刪除之後），自動切回「全部」（DESIGN.md 4.11）
watch(chips, (list) => {
  if (selectedCategory.value !== '' && !list.some((chip) => chip.name === selectedCategory.value)) {
    selectedCategory.value = ''
  }
})

/** 在下拉選單選了新的種類：立刻存檔 */
async function changeCategory(item, newCategory) {
  if (newCategory === item.category) {
    return
  }

  savingCategoryId.value = item.id
  const result = await api.put(`/api/favorites/${item.id}/category`, { category: newCategory })
  savingCategoryId.value = null

  if (result.ok) {
    item.category = newCategory // 存檔成功才更新畫面；種類按鈕的本數會自動重算
    showToast(result.data.message) // 已更新書籍種類
  } else if (result.status === 401) {
    handleSessionExpired()
  } else {
    // 失敗時 item.category 沒變，下拉選單會自動回到原本的種類
    showToast(result.data.message, 'error')
  }
}

/** 按下某本書的「刪除」 */
async function removeFavorite(item) {
  // 瀏覽器內建的確認視窗：按「取消」就什麼都不做（DESIGN.md 4.9）
  if (!window.confirm(MESSAGES.DELETE_CONFIRM)) {
    return
  }

  deletingId.value = item.id
  const result = await api.delete(`/api/favorites/${item.id}`)
  deletingId.value = null

  if (result.ok) {
    showToast(result.data.message) // 已移除收藏
    // 從畫面上的清單拿掉這一筆（「共 N 本」與種類按鈕的本數會自動跟著更新）
    favorites.value = favorites.value.filter((f) => f.id !== item.id)
  } else if (result.status === 401) {
    handleSessionExpired()
  } else {
    // 例如另一個分頁已經刪掉了（404 找不到此收藏）：提示後重新載入清單
    showToast(result.data.message, 'error')
    loadFavorites()
  }
}

function handleSessionExpired() {
  setLoggedOut() // 導覽列變回「登入 / 註冊」
  favorites.value = [] // 不再顯示任何收藏內容
  sessionExpired.value = true
}
</script>

<template>
  <div class="page page--wide">
    <PageTitle title="我的收藏 — 書籍清單" />

    <!-- (b) 未登入提示畫面（Session 失效時的備用） -->
    <template v-if="sessionExpired">
      <ErrorBox center class="guest">
        <p class="guest__title">您目前為未登入狀態</p>
        <p class="guest__text">請先登入或註冊會員後，即可檢視與管理您的個人藏書</p>
      </ErrorBox>
      <div class="guest__action">
        <BaseButton class="guest__button" @click="router.push('/login')">前往登入 / 註冊</BaseButton>
      </div>
    </template>

    <!-- 其他錯誤 -->
    <ErrorBox v-else-if="errorMessage">⚠ 錯誤提示：{{ errorMessage }}</ErrorBox>

    <!-- (a) 已登入：清單（載入中先不顯示，避免閃一下「目前還沒有收藏」） -->
    <template v-else-if="!loading">
      <!-- 副標＋清單／統計切換（有收藏才顯示切換；手機版切換在副標下方） -->
      <div class="header">
        <h2 class="summary">會員「{{ auth.user?.nickname }}」的私人藏書 (共 {{ favorites.length }} 本)</h2>
        <ViewToggle v-if="favorites.length > 0" v-model="view" />
      </div>

      <EmptyBox v-if="favorites.length === 0">目前還沒有收藏的書籍</EmptyBox>

      <!-- (c) 統計檢視（DESIGN.md 5.3-c）：總覽數字 → 種類比例 → 每月收藏趨勢 -->
      <div v-else-if="view === 'stats'" class="stats">
        <StatTiles :total="stats.total" :added-this-month="stats.addedThisMonth" :top-category="stats.topCategory" />

        <BaseCard class="stats__card">
          <h3 class="stats__title">種類比例</h3>
          <CategoryPieChart :total="favorites.length" :items="breakdown" />
        </BaseCard>

        <BaseCard class="stats__card">
          <h3 class="stats__title">每月收藏趨勢（最近 12 個月）</h3>
          <MonthlyLineChart :months="months" />
        </BaseCard>
      </div>

      <template v-else>
        <!-- 搜尋框＋種類按鈕列（DESIGN.md 5.3-a） -->
        <div class="filters">
          <BaseInput
            v-model="keyword"
            class="filters__search"
            label="搜尋收藏"
            placeholder="輸入書名、作者或 ISBN..."
            type="search"
          />
          <CategoryChips v-model="selectedCategory" :total="favorites.length" :chips="chips" />
          <p v-if="isFiltering" class="filters__count">顯示 {{ visibleFavorites.length }} 本</p>
        </div>

        <EmptyBox v-if="visibleFavorites.length === 0">{{ MESSAGES.FAVORITES_NO_MATCH }}</EmptyBox>

        <ul v-else class="list">
          <!-- v-for：清單有幾筆就畫幾張卡片；:key 讓 Vue 分辨每一筆 -->
          <li v-for="item in visibleFavorites" :key="item.id">
            <BaseCard class="item">
              <!-- 左邊：封面縮圖＋書名與資訊（手機版這兩個仍在同一列） -->
              <div class="item__main">
                <BookCover :isbn="item.isbn" :title="item.title" size="sm" lazy />
                <div class="item__info">
                  <h3 class="item__title">{{ item.title }}</h3>
                  <p class="item__meta">作者：{{ item.author || '—' }} | ISBN: {{ item.isbn }}</p>
                  <!-- 種類下拉選單：選完立即存檔（DESIGN.md 4.12） -->
                  <BaseSelect
                    class="item__category"
                    label="種類："
                    :model-value="item.category"
                    :options="categories"
                    :disabled="savingCategoryId === item.id"
                    @update:model-value="(value) => changeCategory(item, value)"
                  />
                </div>
              </div>
              <BaseButton
                variant="danger"
                class="item__delete"
                :disabled="deletingId === item.id"
                @click="removeFavorite(item)"
              >
                刪除
              </BaseButton>
            </BaseCard>
          </li>
        </ul>
      </template>
    </template>
  </div>
</template>

<style scoped>
/* 副標在左、清單／統計切換在右 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

/* 左對齊粗體副標：會員「Cooqle」的私人藏書 (共 2 本) */
.summary {
  margin: 0;
  padding-left: 16px;
  font-size: 1.1em;
  font-weight: 700;
}

/* ===== 統計檢視 ===== */
.stats {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stats__title {
  margin: 0 0 24px;
  font-size: 1.1em;
  font-weight: 700;
}

/* ===== 搜尋框＋種類按鈕列 ===== */
.filters {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
}

.filters__search {
  width: 50%; /* 桌機約 50%（DESIGN.md 3.1） */
}

.filters__count {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.95em;
}

/* ===== 收藏清單 ===== */
.list {
  display: flex;
  flex-direction: column;
  gap: 24px; /* 卡片間距約 24px */
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 左：書名與資訊；右：「刪除」垂直置中 */
.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  padding-top: 40px;
  padding-bottom: 40px;
}

/* 封面縮圖（64×96）與書名資訊排成一列 */
.item__main {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: 0;
}

.item__info {
  min-width: 0; /* 讓長書名可以換行，不會把按鈕擠出去 */
}

.item__title {
  margin: 0 0 16px;
  font-size: 1.1em;
  font-weight: 700;
}

.item__meta {
  margin: 0;
  color: var(--color-text-muted);
}

/* 種類下拉選單：在作者行下方 */
.item__category {
  margin-top: 12px;
}

.item__delete {
  flex-shrink: 0;
  width: 220px;
}

/* ===== 未登入提示畫面 ===== */
.guest {
  margin-top: 48px;
}

.guest__title {
  margin: 0 0 8px;
  font-size: 1.15em;
}

.guest__text {
  margin: 0;
  font-weight: 400;
}

.guest__action {
  display: flex;
  justify-content: center;
  margin-top: 56px;
}

.guest__button {
  width: 340px;
  max-width: 100%;
}

/* 平板：按鈕窄一點；搜尋框約 70% */
@media (max-width: 1023px) {
  .filters__search {
    width: 70%;
  }

  .item {
    padding-top: 32px;
    padding-bottom: 32px;
  }

  .item__delete {
    width: 160px;
  }
}

/* 手機：書名與資訊在上，「刪除」在下並滿寬；搜尋框滿寬 */
@media (max-width: 767px) {
  .header {
    flex-direction: column; /* 切換按鈕移到副標下方、靠左 */
    align-items: flex-start;
  }

  .summary {
    padding-left: 0;
  }

  .stats {
    gap: 16px;
  }

  .filters {
    gap: 16px;
    margin-bottom: 24px;
  }

  .filters__search {
    width: 100%;
  }

  .item {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
    padding-top: 24px;
    padding-bottom: 24px;
  }

  .item__main {
    gap: 16px;
  }

  .item__info {
    flex: 1; /* 讓手機版的下拉選單可以撐滿資訊欄 */
  }

  .item__delete {
    width: 100%;
  }

  .guest {
    margin-top: 24px;
  }

  .guest__action {
    margin-top: 32px;
  }
}
</style>
