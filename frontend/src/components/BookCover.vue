<!--
  書籍封面（DESIGN.md 4.10）：用 ISBN 向 Open Library 封面服務取得圖片。
  載入中、沒有封面、載入失敗：都只顯示空白外框（不放圖示、不放文字、不顯示錯誤）。
  用法：<BookCover :isbn="book.isbn" :title="book.title" size="lg" />
    size：lg（首頁結果卡片 120×180，手機 100×150）／sm（收藏卡片 64×96，手機 56×84）
    lazy：捲動到附近才載入（收藏頁使用）
-->
<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  isbn: { type: String, required: true },
  title: { type: String, default: '' },
  size: { type: String, default: 'lg' },
  lazy: { type: Boolean, default: false },
})

// default=false：沒有封面時 Open Library 回 404，瀏覽器就會觸發 @error
const src = computed(() => `https://covers.openlibrary.org/b/isbn/${props.isbn}-M.jpg?default=false`)

const loaded = ref(false) // 圖片已經載入完成
const failed = ref(false) // 沒有封面或載入失敗

// 換了一本書（例如在首頁重新查詢）時，狀態要重新開始
watch(
  () => props.isbn,
  () => {
    loaded.value = false
    failed.value = false
  },
)
</script>

<template>
  <!-- 外框本身就是空白封面框；圖片載入成功才會蓋在上面 -->
  <div :class="['cover', `cover--${size}`]" :aria-hidden="failed ? 'true' : undefined">
    <img
      v-if="!failed"
      :src="src"
      :alt="`《${title}》封面`"
      :loading="lazy ? 'lazy' : 'eager'"
      :class="['cover__img', { 'is-loaded': loaded }]"
      @load="loaded = true"
      @error="failed = true"
    />
  </div>
</template>

<style scoped>
/* 2:3 的直式外框；沒有圖片時看到的就是這個空白框 */
.cover {
  flex-shrink: 0; /* 放在 flex 排版裡時，不會被擠扁 */
  aspect-ratio: 2 / 3;
  overflow: hidden;
  background: var(--color-bg);
  border: var(--line) solid var(--color-border);
  border-radius: 4px;
}

.cover--lg {
  width: 120px;
}

.cover--sm {
  width: 64px;
}

/* 圖片填滿外框（比例略有不同時，上下會被裁掉一點點） */
.cover__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  visibility: hidden; /* 還沒載入完成前先藏起來，避免看到破圖的小圖示 */
}

.cover__img.is-loaded {
  visibility: visible;
}

/* 手機：尺寸縮小（DESIGN.md 3.1） */
@media (max-width: 767px) {
  .cover--lg {
    width: 100px;
  }

  .cover--sm {
    width: 56px;
  }
}
</style>
