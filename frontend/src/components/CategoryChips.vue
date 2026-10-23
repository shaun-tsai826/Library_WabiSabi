<!--
  種類按鈕列（DESIGN.md 4.11）：膠囊形狀的按鈕，點了就切換篩選的種類。
  用法：<CategoryChips v-model="selected" :total="5" :chips="[{ name: '散文', count: 1 }]" />
    v-model：目前選中的種類；空字串 '' 代表「全部」
    total：全部的本數（第一個按鈕「全部 (N)」）
    chips：要顯示的種類與本數（只傳有書的種類，順序由呼叫的人決定）
-->
<script setup>
defineProps({
  total: { type: Number, required: true },
  chips: { type: Array, default: () => [] },
})

const selected = defineModel({ type: String, default: '' })
</script>

<template>
  <div class="chips" role="group" aria-label="依種類篩選">
    <button
      type="button"
      :class="['chip', { 'is-selected': selected === '' }]"
      :aria-pressed="selected === ''"
      @click="selected = ''"
    >
      全部 ({{ total }})
    </button>
    <button
      v-for="chip in chips"
      :key="chip.name"
      type="button"
      :class="['chip', { 'is-selected': selected === chip.name }]"
      :aria-pressed="selected === chip.name"
      @click="selected = chip.name"
    >
      {{ chip.name }} ({{ chip.count }})
    </button>
  </div>
</template>

<style scoped>
.chips {
  display: flex;
  flex-wrap: wrap; /* 按鈕太多時自動換行 */
  gap: 12px;
}

/* 膠囊形狀的按鈕 */
.chip {
  min-height: var(--tap-min); /* 至少 44px，方便手指點 */
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

/* 選中的按鈕：和主要按鈕同色系 */
.chip.is-selected {
  background: var(--color-text);
  border-color: var(--color-text);
  color: var(--color-bg);
}

@media (max-width: 767px) {
  .chips {
    gap: 8px;
  }

  .chip {
    padding: 0 16px;
    font-size: 15px;
  }
}
</style>
