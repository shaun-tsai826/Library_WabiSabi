<!--
  清單／統計切換（DESIGN.md 4.13）：兩個膠囊按鈕，樣式和種類按鈕列（4.11）相同。
  用法：<ViewToggle v-model="view" />
    v-model：'list'（清單）或 'stats'（統計）
-->
<script setup>
const view = defineModel({ type: String, default: 'list' })

const options = [
  { value: 'list', label: '清單' },
  { value: 'stats', label: '統計' },
]
</script>

<template>
  <div class="toggle" role="group" aria-label="切換檢視方式">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      :class="['chip', { 'is-selected': view === option.value }]"
      :aria-pressed="view === option.value"
      @click="view = option.value"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<style scoped>
.toggle {
  display: flex;
  gap: 8px;
}

/* 膠囊形狀的按鈕（同 CategoryChips） */
.chip {
  min-height: var(--tap-min);
  padding: 0 20px;
  background: var(--color-surface);
  border: var(--line) solid var(--color-border);
  border-radius: 999px;
  color: var(--color-text);
  font: inherit;
  cursor: pointer;
}

.chip:hover {
  border-color: var(--color-text-muted);
}

.chip.is-selected {
  background: var(--color-text);
  border-color: var(--color-text);
  color: var(--color-bg);
}

@media (max-width: 767px) {
  .chip {
    padding: 0 16px;
    font-size: 15px;
  }
}
</style>
