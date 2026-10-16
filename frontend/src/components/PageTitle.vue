<!-- 頁面標題：置中粗體＋下方短底線（DESIGN.md 4.2） -->
<script setup>
defineProps({
  title: { type: String, required: true },
})
</script>

<template>
  <h1 class="page-title">{{ title }}</h1>
</template>

<style scoped>
.page-title {
  margin: 0 0 40px;
  font-size: var(--title-size);
  font-weight: 700;
  text-align: center;
  line-height: 1.4;
}

/* 標題下方的短底線 */
.page-title::after {
  content: '';
  display: block;
  width: var(--title-underline);
  max-width: 100%;
  height: var(--line);
  margin: 12px auto 0;
  background: var(--color-text);
}

@media (max-width: 767px) {
  .page-title {
    margin-bottom: 28px;
  }
}
</style>
