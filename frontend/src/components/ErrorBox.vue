<!--
  錯誤／警示框：淡紅底、紅框、深紅粗體字（DESIGN.md 4.6）。
  用法：<ErrorBox>⚠ 錯誤提示：找不到此書籍</ErrorBox>
    center：文字置中（收藏頁的未登入提示用）
-->
<script setup>
defineProps({
  center: { type: Boolean, default: false },
})
</script>

<template>
  <div :class="['error-box', { 'error-box--center': center }]" role="alert">
    <slot />
  </div>
</template>

<style scoped>
.error-box {
  padding: 32px 48px;
  background: var(--color-danger-bg);
  border: var(--line) solid var(--color-danger-border);
  border-radius: var(--radius-input);
  color: var(--color-danger-text);
  font-weight: 700;
}

.error-box--center {
  text-align: center;
}

@media (max-width: 767px) {
  .error-box {
    padding: 24px;
  }
}
</style>
