<!-- 卡片：暖灰背景、細邊框、圓角（DESIGN.md 4.3）。內容用 <slot /> 放進來 -->
<template>
  <div class="card">
    <slot />
  </div>
</template>

<style scoped>
.card {
  background: var(--color-surface);
  border: var(--line) solid var(--color-border);
  border-radius: var(--radius-card);
  padding: var(--card-padding);
}
</style>
