<!--
  Toast 提示（DESIGN.md 4.8）：畫面上方置中、3 秒後自動消失、同時只顯示一則。
  只要在 App.vue 放一個；各頁面呼叫 toast.js 的 showToast() 就會出現。
-->
<script setup>
import { toast } from '@/toast'
</script>

<template>
  <!-- Transition：出現與消失時套用淡入淡出（唯一允許的動畫） -->
  <Transition name="toast-fade">
    <div v-if="toast.visible" :class="['toast', `toast--${toast.type}`]" role="status" aria-live="polite">
      {{ toast.message }}
    </div>
  </Transition>
</template>

<style scoped>
.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  width: max-content;
  max-width: min(560px, 90vw); /* 手機上最多為螢幕寬度的 90% */
  padding: 14px 28px;
  border: var(--line) solid transparent;
  border-radius: var(--radius-input);
  text-align: center;
  font-weight: 500;
}

/* 成功／一般提示 */
.toast--success {
  background: var(--color-text);
  border-color: var(--color-text);
  color: var(--color-bg);
}

/* 警告 */
.toast--warning {
  background: var(--color-border);
  border-color: var(--color-text-muted);
  color: var(--color-text);
}

/* 錯誤：沿用錯誤框的紅色系 */
.toast--error {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
  font-weight: 700;
}

/* 淡入淡出 */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.3s;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
}
</style>
