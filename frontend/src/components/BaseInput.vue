<!--
  輸入框：上方標籤＋白色輸入框（DESIGN.md 4.4）。
  用法：<BaseInput v-model="isbn" label="ISBN 查詢" placeholder="請輸入 13 位數 ISBN 碼..." />
-->
<script setup>
import { useId } from 'vue'

defineProps({
  label: { type: String, required: true },
  placeholder: { type: String, default: '' },
  type: { type: String, default: 'text' },
  /** 手機鍵盤類型，例如 'numeric' 會跳出數字鍵盤 */
  inputmode: { type: String, default: undefined },
  autocomplete: { type: String, default: 'off' },
})

// defineModel：讓父元件可以用 v-model 綁定這個輸入框的值
const model = defineModel({ type: String, default: '' })

// 產生不重複的 id，讓 <label for> 對到 <input id>（點標籤就能聚焦輸入框）
const inputId = useId()
</script>

<template>
  <div class="field">
    <label class="field__label" :for="inputId">{{ label }}</label>
    <input
      :id="inputId"
      v-model="model"
      class="field__input"
      :type="type"
      :placeholder="placeholder"
      :inputmode="inputmode"
      :autocomplete="autocomplete"
    />
  </div>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.field__label {
  color: var(--color-text-muted);
  font-size: 0.95em;
  padding-left: 4px;
}

.field__input {
  width: 100%;
  min-height: 56px;
  padding: 0 24px;
  background: var(--color-input-bg);
  border: var(--line) solid var(--color-border);
  border-radius: var(--radius-input);
  outline: none;
}

.field__input::placeholder {
  color: #A59C92;
}

/* 聚焦時邊框變深，讓使用者知道正在輸入哪一格 */
.field__input:focus {
  border-color: var(--color-text-muted);
}

@media (max-width: 767px) {
  .field__input {
    min-height: 48px;
    padding: 0 16px;
  }
}
</style>
