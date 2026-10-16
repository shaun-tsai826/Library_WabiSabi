<!--
  頂部導覽列（DESIGN.md 4.1）：所有頁面共用。
  左：LIBRARY / 侘寂書閣（回首頁）；右：首頁 ｜ 我的收藏 ｜ 登入 / 註冊（或「暱稱 (個人頁)」）
-->
<script setup>
import { useRoute } from 'vue-router'
import { auth } from '@/auth'

// 目前所在的網址，用來把「目前頁面」的項目加粗
const route = useRoute()
</script>

<template>
  <header class="navbar">
    <RouterLink to="/" class="navbar__logo">LIBRARY / <strong>侘寂書閣</strong></RouterLink>

    <nav class="navbar__menu">
      <RouterLink to="/" :class="['navbar__item', { 'is-active': route.path === '/' }]">首頁</RouterLink>

      <!-- 未登入點這裡時，路由守衛會提示並導向登入頁（見 router/index.js） -->
      <RouterLink
        to="/favorites"
        :class="['navbar__item', 'navbar__item--divider', { 'is-active': route.path === '/favorites' }]"
      >
        我的收藏
      </RouterLink>

      <!-- 還沒問完後端「有沒有登入」之前先不顯示，避免畫面閃一下「登入 / 註冊」 -->
      <template v-if="auth.ready">
        <RouterLink
          v-if="auth.user"
          to="/profile"
          :class="['navbar__item', 'navbar__item--divider', { 'is-active': route.path === '/profile' }]"
        >
          {{ auth.user.nickname }} (個人頁)
        </RouterLink>
        <RouterLink
          v-else
          to="/login"
          :class="['navbar__item', 'navbar__item--divider', 'navbar__item--muted', { 'is-active': route.path === '/login' }]"
        >
          登入 / 註冊
        </RouterLink>
      </template>
    </nav>
  </header>
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 104px;
  padding: 16px max(var(--page-gutter), calc((100% - 1440px) / 2));
  background: var(--color-surface);
}

.navbar__logo {
  font-size: 30px;
  white-space: nowrap;
}

.navbar__logo strong {
  font-weight: 700;
}

.navbar__menu {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 40px;
}

.navbar__item {
  display: inline-flex;
  align-items: center;
  min-height: var(--tap-min); /* 至少 44px 高，方便手指點擊 */
  font-size: 20px;
}

/* 「我的收藏」與第三項前面的細直線 */
.navbar__item--divider::before {
  content: '';
  display: inline-block;
  width: 1px;
  height: 1em;
  margin-right: 14px;
  background: var(--color-text);
}

/* 「登入 / 註冊」平常是較淺的灰色 */
.navbar__item--muted {
  color: var(--color-text-muted);
}

/* 目前所在頁面：加粗 */
.navbar__item.is-active {
  font-weight: 700;
}

/* 平板：字小一點 */
@media (max-width: 1023px) {
  .navbar__logo {
    font-size: 24px;
  }

  .navbar__menu {
    gap: 8px 24px;
  }

  .navbar__item {
    font-size: 18px;
  }
}

/* 手機：logo 一列置中；三個項目在下一列置中（不使用漢堡選單） */
@media (max-width: 767px) {
  .navbar {
    flex-direction: column;
    gap: 4px;
    min-height: auto;
    padding: 16px var(--page-gutter) 8px;
  }

  .navbar__logo {
    font-size: 22px;
  }

  .navbar__menu {
    justify-content: center;
    gap: 0 16px;
  }

  .navbar__item {
    font-size: 16px;
  }

  .navbar__item--divider::before {
    margin-right: 10px;
  }
}
</style>
