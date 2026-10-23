<!--
  種類比例圓餅圖（DESIGN.md 4.15）：中間挖空的甜甜圈＋右側圖例（手機改成上下排列）。
  用法：<CategoryPieChart :total="4" :items="[{ name: '心理勵志', count: 2, percent: 50, color: 'var(--chart-5)' }]" />
    items：favoriteStats.js 的 categoryBreakdown() 算好的結果（已排序）
-->
<script setup>
import { computed } from 'vue'

const props = defineProps({
  total: { type: Number, required: true },
  items: { type: Array, required: true },
})

/**
 * conic-gradient：從 12 點鐘方向順時針，一段一段塗上顏色。
 * 例如 [心理勵志 50%、散文 25%、未分類 25%] 會變成
 *   conic-gradient(灰藍 0% 50%, 紅土 50% 75%, 淺灰 75% 100%)
 * 這裡用「本數 ÷ 總數」的精確比例，不用四捨五入後的 percent，才不會加起來不是 100%。
 */
const background = computed(() => {
  let start = 0
  const parts = props.items.map((item) => {
    const end = start + (item.count / props.total) * 100
    const part = `${item.color} ${start}% ${end}%`
    start = end
    return part
  })
  return `conic-gradient(${parts.join(', ')})`
})
</script>

<template>
  <div class="pie">
    <!-- 圖形本身對螢幕報讀軟體隱藏，資料由右側圖例文字提供 -->
    <div class="pie__donut" :style="{ background }" aria-hidden="true">
      <div class="pie__hole">{{ total }} 本</div>
    </div>

    <ul class="legend">
      <li v-for="item in items" :key="item.name" class="legend__row">
        <span class="legend__swatch" :style="{ background: item.color }" aria-hidden="true"></span>
        <span class="legend__name">{{ item.name }}</span>
        <span class="legend__count">{{ item.count }} 本</span>
        <span class="legend__percent">{{ item.percent }}%</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.pie {
  display: flex;
  align-items: center;
  gap: 48px;
}

/* 甜甜圈：直徑 200px 的圓 */
.pie__donut {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 200px;
  border-radius: 50%;
}

/* 中間挖空約 60%：用卡片底色的圓蓋在上面 */
.pie__hole {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60%;
  height: 60%;
  border-radius: 50%;
  background: var(--color-surface);
  font-weight: 700;
}

/* 圖例：■ 種類　本數　比例，對齊成三欄 */
.legend {
  display: grid;
  grid-template-columns: auto auto auto auto;
  gap: 12px 16px;
  align-items: center;
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 讓每個 li 的 4 個 span 直接參與外層的格線，欄位才會上下對齊 */
.legend__row {
  display: contents;
}

.legend__swatch {
  width: 14px;
  height: 14px;
  border-radius: 3px;
}

.legend__count,
.legend__percent {
  color: var(--color-text-muted);
  text-align: right;
}

@media (max-width: 767px) {
  .pie {
    flex-direction: column; /* 甜甜圈在上置中、圖例在下 */
    gap: 24px;
  }

  .pie__donut {
    width: 160px;
    height: 160px;
  }
}
</style>
