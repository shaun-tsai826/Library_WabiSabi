<!--
  總覽數字卡（DESIGN.md 4.14）：收藏總數、本月新增、最多的種類，3 張一列（手機 1 欄）。
  用法：<StatTiles :total="4" :added-this-month="3" top-category="心理勵志" />
-->
<script setup>
defineProps({
  total: { type: Number, required: true },
  addedThisMonth: { type: Number, required: true },
  topCategory: { type: String, required: true },
})
</script>

<template>
  <!-- dl：「標題／數值」成對的清單，螢幕報讀軟體會唸成「收藏總數，4 本」 -->
  <dl class="tiles">
    <div class="tile">
      <dt class="tile__label">收藏總數</dt>
      <dd class="tile__value">{{ total }} 本</dd>
    </div>
    <div class="tile">
      <dt class="tile__label">本月新增</dt>
      <dd class="tile__value">{{ addedThisMonth }} 本</dd>
    </div>
    <div class="tile">
      <dt class="tile__label">最多的種類</dt>
      <dd class="tile__value">{{ topCategory }}</dd>
    </div>
  </dl>
</template>

<style scoped>
.tiles {
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* 3 張一樣寬 */
  gap: 24px;
  margin: 0;
}

/* 4.3 卡片的縮小版：內距 24px */
.tile {
  padding: 24px;
  background: var(--color-surface);
  border: var(--line) solid var(--color-border);
  border-radius: var(--radius-card);
}

.tile__label {
  margin-bottom: 8px;
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.tile__value {
  margin: 0;
  font-size: 1.6em;
  font-weight: 700;
  overflow-wrap: anywhere;
}

@media (max-width: 767px) {
  .tiles {
    grid-template-columns: 1fr; /* 手機：上下排成 1 欄 */
    gap: 16px;
  }
}
</style>
