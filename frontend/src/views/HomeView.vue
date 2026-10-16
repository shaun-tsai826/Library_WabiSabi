<!--
  首頁 /：用 ISBN 查詢書籍（DESIGN.md 5.1）。
  三種狀態：初始（虛線空狀態框）、查詢成功（結果卡片）、查詢錯誤（紅色錯誤框）。
-->
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseInput from '@/components/BaseInput.vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import ErrorBox from '@/components/ErrorBox.vue'
import EmptyBox from '@/components/EmptyBox.vue'
import { api } from '@/api'
import { auth, setLoggedOut } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'
import { validateIsbn } from '@/validators'

const router = useRouter()

// ===== 畫面狀態（ref：值一改，畫面就自動更新） =====
const isbnInput = ref('') // 輸入框的內容（保留使用者輸入的原樣）
const book = ref(null) // 查到的書；null = 沒有結果
const errorMessage = ref('') // 錯誤訊息；'' = 沒有錯誤
const searching = ref(false) // 是否正在查詢（按鈕顯示「查詢中...」）
const adding = ref(false) // 是否正在加入收藏（避免連按）

/** 按下「查詢」 */
async function search() {
  const isbn = isbnInput.value.trim()

  // 每次查詢前先清掉上一次的結果與錯誤
  book.value = null
  errorMessage.value = ''

  // 前端先驗證：空白、格式錯誤都不呼叫 API（設計決策 10）
  const invalid = validateIsbn(isbn)
  if (invalid) {
    errorMessage.value = invalid
    return
  }

  searching.value = true
  const result = await api.get(`/api/books/${encodeURIComponent(isbn)}`)
  searching.value = false

  if (result.ok) {
    book.value = result.data
  } else {
    // 404 找不到此書籍、503 查詢失敗，請稍後再試…都直接顯示後端的訊息
    errorMessage.value = result.data.message
  }
}

/** 按下「+ 加入收藏」 */
async function addFavorite() {
  // 未登入：提示並導向登入/註冊頁
  if (!auth.user) {
    goLogin()
    return
  }

  adding.value = true
  const result = await api.post('/api/favorites/add', { isbn: book.value.isbn })
  adding.value = false

  if (result.ok) {
    showToast(result.data.message) // 已成功加入收藏
  } else if (result.status === 409) {
    showToast(result.data.message, 'warning') // 此書籍已在收藏清單中，請勿重複收藏
  } else if (result.status === 401) {
    // 畫面以為有登入，但 Session 已經失效：當作未登入處理
    setLoggedOut()
    goLogin()
  } else {
    showToast(result.data.message, 'error')
  }
}

function goLogin() {
  showToast(MESSAGES.FAVORITE_LOGIN_REQUIRED, 'warning')
  router.push('/login')
}

/** 欄位沒有資料時顯示「—」 */
function orDash(value) {
  return value ? value : '—'
}
</script>

<template>
  <div class="page page--wide">
    <PageTitle title="首頁 — 書籍查詢" />

    <!-- form + @submit.prevent：按 Enter 也能查詢，而且不會重新整理網頁 -->
    <form class="search" @submit.prevent="search">
      <BaseInput
        v-model="isbnInput"
        class="search__input"
        label="ISBN 查詢"
        placeholder="請輸入 13 位數 ISBN 碼..."
        inputmode="numeric"
      />
      <BaseButton type="submit" class="search__button" spaced :disabled="searching">
        {{ searching ? '查詢中...' : '查詢' }}
      </BaseButton>
    </form>

    <!-- 狀態 (c)：查詢錯誤 -->
    <ErrorBox v-if="errorMessage">⚠ 錯誤提示：{{ errorMessage }}</ErrorBox>

    <!-- 狀態 (b)：查詢成功 -->
    <BaseCard v-else-if="book">
      <p class="result__caption"><strong>查詢結果</strong> (Search Result)</p>
      <h2 class="result__title">書名：{{ book.title }}</h2>
      <p class="result__line">作者：{{ orDash(book.author) }}</p>
      <p class="result__line">出版日期：{{ orDash(book.publishDate) }}</p>
      <p class="result__isbn">ISBN：{{ book.isbn }}</p>
      <BaseButton variant="secondary" class="result__add" :disabled="adding" @click="addFavorite">
        + 加入收藏
      </BaseButton>
    </BaseCard>

    <!-- 狀態 (a)：初始（查詢中不顯示） -->
    <EmptyBox v-else-if="!searching">
      請在上方輸入 ISBN 碼並點擊「查詢」以檢視書籍詳細資訊
    </EmptyBox>
  </div>
</template>

<style scoped>
/* ===== 查詢列：輸入框約 70%＋按鈕約 30%，同一列 ===== */
.search {
  display: flex;
  align-items: flex-end; /* 按鈕對齊輸入框底部（輸入框上方有標籤） */
  gap: 32px;
  margin-bottom: 48px;
}

.search__input {
  flex: 7;
}

.search__button {
  flex: 3;
}

/* ===== 結果卡片 ===== */
.result__caption {
  margin: 0 0 24px;
  color: var(--color-text-muted);
}

.result__title {
  margin: 0 0 20px;
  font-size: 1.3em;
  font-weight: 700;
}

.result__line {
  margin: 0 0 16px;
}

.result__isbn {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9em;
}

/* 「+ 加入收藏」：靠左、約卡片寬 35%；與 ISBN 行至少隔 16px（DESIGN.md 5.1-b） */
.result__add {
  width: 35%;
  margin-top: 24px;
}

/* 平板：按鈕約卡片寬 50% */
@media (max-width: 1023px) {
  .search {
    gap: 24px;
  }

  .result__add {
    width: 50%;
  }
}

/* 手機：輸入框與按鈕上下排列、各自滿寬；加入收藏按鈕滿寬 */
@media (max-width: 767px) {
  .search {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    margin-bottom: 32px;
  }

  .result__add {
    width: 100%;
  }
}
</style>
