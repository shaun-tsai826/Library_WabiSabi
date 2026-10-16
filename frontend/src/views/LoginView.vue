<!--
  登入 / 註冊 /login（DESIGN.md 5.2）：登入與註冊合一，只要暱稱＋手機。
  錯誤訊息一律用 Toast（錯誤樣式）顯示。
-->
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseInput from '@/components/BaseInput.vue'
import BaseButton from '@/components/BaseButton.vue'
import { api } from '@/api'
import { setLoggedIn } from '@/auth'
import { showToast } from '@/toast'
import { validateLogin } from '@/validators'

const router = useRouter()

const nickname = ref('')
const mobile = ref('')
const submitting = ref(false) // 送出中：按鈕暫時停用，避免連按

/** 按下「登入 / 註冊」 */
async function submit() {
  const trimmedNickname = nickname.value.trim()
  const trimmedMobile = mobile.value.trim()

  // 前端先驗證（規則與後端相同）
  const invalid = validateLogin(trimmedNickname, trimmedMobile)
  if (invalid) {
    showToast(invalid, 'error')
    return
  }

  submitting.value = true
  const result = await api.post('/api/auth/login', { nickname: trimmedNickname, mobile: trimmedMobile })
  submitting.value = false

  if (result.ok) {
    setLoggedIn(result.data) // 導覽列馬上變成「暱稱 (個人頁)」
    showToast(result.data.message) // 登入成功（新註冊也是這句）
    router.push('/')
  } else {
    // 暱稱與手機號碼不符、此暱稱已被使用…都顯示後端的訊息
    showToast(result.data.message, 'error')
  }
}
</script>

<template>
  <div class="page page--narrow">
    <PageTitle title="會員 登入 / 註冊" />

    <BaseCard>
      <form class="login-form" @submit.prevent="submit">
        <BaseInput v-model="nickname" label="暱稱 (Nickname)" placeholder="例如：Cooqle" autocomplete="nickname" />
        <BaseInput
          v-model="mobile"
          label="手機號碼 (Mobile)"
          placeholder="例如：0900123456"
          type="tel"
          inputmode="numeric"
          autocomplete="tel"
        />
        <BaseButton type="submit" class="login-form__submit" block spaced :disabled="submitting">
          登入 / 註冊
        </BaseButton>
      </form>
    </BaseCard>
  </div>
</template>

<style scoped>
.login-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding: 8px 40px;
}

.login-form__submit {
  margin-top: 16px;
}

/* 平板以下：卡片本身的內距已經變小，表單不再額外內縮 */
@media (max-width: 1023px) {
  .login-form {
    padding: 0;
  }
}

@media (max-width: 767px) {
  .login-form {
    gap: 24px;
  }

  .login-form__submit {
    margin-top: 8px;
  }
}
</style>
