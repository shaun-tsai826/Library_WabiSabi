<!-- 空狀態框：虛線邊框、置中次要色文字（DESIGN.md 4.7） -->
<template>
  <div class="empty-box">
    <slot />
  </div>
</template>

<style scoped>
.empty-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 24px;
  border: var(--line) dashed var(--color-border);
  border-radius: var(--radius-card);
  color: var(--color-text-muted);
  text-align: center;
}
</style>
