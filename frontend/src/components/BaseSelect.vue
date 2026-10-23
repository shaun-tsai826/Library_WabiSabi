<!--
  下拉選單（DESIGN.md 4.12）：前面一行次要色標籤＋白色下拉選單，標籤與選單在同一列。
  用法：<BaseSelect v-model="category" label="種類：" :options="['未分類', '小說']" />
    想在使用者「選了之後」做事（例如存檔），可以聽 @update:model-value
-->
<script setup>
import { useId } from 'vue'

defineProps({
  label: { type: String, required: true },
  /** 選項清單（字串陣列），顯示文字與值相同 */
  options: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
})

const model = defineModel({ type: String, default: '' })

// <label for> 對到 <select id>，點標籤就能聚焦選單
const selectId = useId()
</script>

<template>
  <div class="select-field">
    <label class="select-field__label" :for="selectId">{{ label }}</label>
    <select :id="selectId" v-model="model" class="select-field__select" :disabled="disabled">
      <!-- 目前的值不在清單裡時（例如清單還沒載入），仍然要顯示得出來 -->
      <option v-if="model && !options.includes(model)" :value="model">{{ model }}</option>
      <option v-for="option in options" :key="option" :value="option">{{ option }}</option>
    </select>
  </div>
</template>

<style scoped>
.select-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.select-field__label {
  flex-shrink: 0;
  color: var(--color-text-muted);
  font-size: 0.95em;
}

.select-field__select {
  width: 160px;
  min-height: var(--tap-min); /* 至少 44px，方便手指點 */
  padding: 0 12px;
  background: var(--color-input-bg);
  border: var(--line) solid var(--color-border);
  border-radius: var(--radius-input);
  font: inherit;
  color: var(--color-text);
  cursor: pointer;
}

.select-field__select:focus {
  outline: none;
  border-color: var(--color-text-muted);
}

.select-field__select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 手機：選單滿寬（DESIGN.md 3.1） */
@media (max-width: 767px) {
  .select-field__select {
    flex: 1;
    width: auto;
    min-width: 0;
  }
}
</style>
