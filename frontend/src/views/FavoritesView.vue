<!--
  收藏頁 /favorites（DESIGN.md 5.3）：需要登入（路由守衛把關）。
  (a) 已登入：收藏清單，每本書旁邊有「刪除」；沒有收藏時顯示空狀態框。
  (b) 未登入提示畫面：只在「停留在本頁時 Session 失效（API 回 401）」時顯示（備用）。
-->
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseButton from '@/components/BaseButton.vue'
import ErrorBox from '@/components/ErrorBox.vue'
import EmptyBox from '@/components/EmptyBox.vue'
import { api } from '@/api'
import { auth, setLoggedOut } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'

const router = useRouter()

const favorites = ref([]) // 收藏清單
const loading = ref(true) // 第一次載入中
const sessionExpired = ref(false) // Session 失效 → 顯示未登入提示畫面
const errorMessage = ref('') // 其他錯誤（例如連不上後端）
const deletingId = ref(null) // 正在刪除哪一筆（按鈕暫時停用）

// onMounted：頁面顯示出來之後執行一次
onMounted(loadFavorites)

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
    // 從畫面上的清單拿掉這一筆（「共 N 本」會自動跟著更新）
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
      <h2 class="summary">會員「{{ auth.user?.nickname }}」的私人藏書 (共 {{ favorites.length }} 本)</h2>

      <EmptyBox v-if="favorites.length === 0">目前還沒有收藏的書籍</EmptyBox>

      <ul v-else class="list">
        <!-- v-for：清單有幾筆就畫幾張卡片；:key 讓 Vue 分辨每一筆 -->
        <li v-for="item in favorites" :key="item.id">
          <BaseCard class="item">
            <div class="item__info">
              <h3 class="item__title">{{ item.title }}</h3>
              <p class="item__meta">作者：{{ item.author || '—' }} | ISBN: {{ item.isbn }}</p>
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
  </div>
</template>

<style scoped>
/* 左對齊粗體副標：會員「Cooqle」的私人藏書 (共 2 本) */
.summary {
  margin: 0 0 24px;
  padding-left: 16px;
  font-size: 1.1em;
  font-weight: 700;
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

/* 平板：按鈕窄一點 */
@media (max-width: 1023px) {
  .item {
    padding-top: 32px;
    padding-bottom: 32px;
  }

  .item__delete {
    width: 160px;
  }
}

/* 手機：書名與資訊在上，「刪除」在下並滿寬 */
@media (max-width: 767px) {
  .summary {
    padding-left: 0;
  }

  .item {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
    padding-top: 24px;
    padding-bottom: 24px;
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
