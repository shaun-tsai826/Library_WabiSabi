<!--
  個人頁 /profile（DESIGN.md 5.4）：需要登入（路由守衛把關）。
  顯示會員暱稱、手機號碼，以及「登出帳號」按鈕。
-->
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseButton from '@/components/BaseButton.vue'
import { api } from '@/api'
import { auth, setLoggedOut } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'

const router = useRouter()
const loggingOut = ref(false)

/** 按下「登出帳號」 */
async function logout() {
  loggingOut.value = true
  const result = await api.post('/api/auth/logout')
  loggingOut.value = false

  // 成功（200）或 Session 早就失效（401），對使用者來說結果都一樣：已經登出了
  if (result.ok || result.status === 401) {
    setLoggedOut() // 導覽列變回「登入 / 註冊」
    showToast(result.ok ? result.data.message : MESSAGES.LOGOUT_SUCCESS) // 已成功登出
    router.push('/')
  } else {
    showToast(result.data.message, 'error')
  }
}
</script>

<template>
  <div class="page page--narrow">
    <PageTitle title="個人資訊" />

    <!-- 路由守衛保證進得來就一定有 auth.user；v-if 是多一層保險 -->
    <BaseCard v-if="auth.user">
      <h2 class="profile__heading">會員基本資料</h2>

      <!-- dl / dt / dd：HTML 的「名稱—內容」清單，剛好對應「標籤：內容」兩欄 -->
      <dl class="profile__info">
        <dt>會員暱稱：</dt>
        <dd class="profile__nickname">{{ auth.user.nickname }}</dd>
        <dt>手機號碼：</dt>
        <dd>{{ auth.user.mobile }}</dd>
      </dl>

      <BaseButton variant="danger" block spaced :disabled="loggingOut" @click="logout">登出帳號</BaseButton>
    </BaseCard>
  </div>
</template>

<style scoped>
/* 置中粗體「會員基本資料」，下方一條分隔線 */
.profile__heading {
  margin: 0 0 32px;
  padding-bottom: 28px;
  border-bottom: var(--line) solid var(--color-border);
  font-size: 1.15em;
  font-weight: 700;
  text-align: center;
}

/* 兩欄：左邊標籤（次要色）、右邊內容 */
.profile__info {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 32px 72px;
  margin: 0 0 40px;
  padding: 0 48px;
}

.profile__info dt {
  color: var(--color-text-muted);
}

.profile__info dd {
  margin: 0;
  min-width: 0; /* 長暱稱可以換行 */
}

.profile__nickname {
  font-weight: 700;
}

@media (max-width: 1023px) {
  .profile__info {
    gap: 24px 40px;
    padding: 0 16px;
  }
}

@media (max-width: 767px) {
  .profile__heading {
    margin-bottom: 24px;
    padding-bottom: 20px;
  }

  .profile__info {
    gap: 20px 24px;
    margin-bottom: 32px;
    padding: 0;
  }
}

/* 極窄螢幕（< 360px）：標籤與內容改成上下排列 */
@media (max-width: 359px) {
  .profile__info {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .profile__info dd {
    margin-bottom: 16px;
  }
}
</style>
