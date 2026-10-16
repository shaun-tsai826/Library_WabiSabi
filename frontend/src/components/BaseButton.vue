<!--
  按鈕（DESIGN.md 4.5）。
  用法：<BaseButton variant="danger" block @click="logout">登出帳號</BaseButton>
    variant：primary（炭灰，預設）／secondary（棕灰）／danger（紅）
    block：滿版寬度
    spaced：文字加字距（例如「登 入 / 註 冊」）
-->
<script setup>
defineProps({
  variant: { type: String, default: 'primary' },
  type: { type: String, default: 'button' },
  block: { type: Boolean, default: false },
  spaced: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    :class="['btn', `btn--${variant}`, { 'btn--block': block, 'btn--spaced': spaced }]"
  >
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 56px;
  padding: 0 28px;
  border: none;
  border-radius: var(--radius-btn);
  color: var(--color-bg);
  font-weight: 500;
  cursor: pointer;
}

.btn--primary {
  background: var(--color-text);
}

.btn--secondary {
  background: var(--color-btn-secondary);
}

.btn--danger {
  background: var(--color-danger);
}

.btn--block {
  width: 100%;
}

.btn--spaced {
  letter-spacing: 0.1em;
  font-weight: 700;
}

.btn:hover:not(:disabled) {
  opacity: 0.88;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 767px) {
  .btn {
    min-height: 48px; /* 仍大於 44px，手指好點 */
  }
}
</style>
