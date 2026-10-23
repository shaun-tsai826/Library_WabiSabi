<!--
  每月收藏趨勢折線圖（DESIGN.md 4.16）：手寫 SVG，不用另外安裝圖表套件。
  用法：<MonthlyLineChart :months="[{ key: '2025-11', year: 2025, month: 11, count: 0 }, ...]" />
    months：favoriteStats.js 的 monthlyCounts() 算好的 12 個月（由舊到新）

  寬度：先量出外框實際有幾 px，再讓 SVG 的座標寬度等於它（1 個座標單位 = 1px），
  這樣不管卡片多寬，文字大小都固定，不會被放大或縮小。
-->
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  months: { type: Array, required: true },
})

const HEIGHT = 240 // 整張圖的高度（px）
const PAD = { top: 32, right: 20, bottom: 56, left: 36 } // 四周留給數字與月份的空間
const NARROW = 480 // 圖寬小於這個值（手機）時，只標單數月份，避免文字重疊

const wrapper = ref(null)
const width = ref(600) // 量到實際寬度之前先用 600
let observer = null

onMounted(() => {
  // ResizeObserver：外框寬度一改變（例如轉動手機、縮放視窗）就重新量
  observer = new ResizeObserver((entries) => {
    width.value = Math.max(Math.round(entries[0].contentRect.width), 240)
  })
  observer.observe(wrapper.value)
})

onBeforeUnmount(() => {
  observer.disconnect() // 離開頁面時停止量測
})

/** 縱軸最大值：最多那個月的本數，至少為 1（全部都是 0 時線才不會擠在一起） */
const maxCount = computed(() => Math.max(1, ...props.months.map((m) => m.count)))

/** 每個月份點的座標，以及要不要標月份、年份 */
const points = computed(() => {
  const plotWidth = width.value - PAD.left - PAD.right
  const plotHeight = HEIGHT - PAD.top - PAD.bottom
  const step = plotWidth / (props.months.length - 1)
  const narrow = width.value < NARROW

  return props.months.map((m, index) => ({
    ...m,
    x: PAD.left + step * index,
    y: PAD.top + plotHeight * (1 - m.count / maxCount.value), // 本數越多越上面
    showMonth: !narrow || m.month % 2 === 1,
    showYear: index === 0 || m.month === 1, // 第一個月份與每年 1 月加註年份
  }))
})

/** 折線：把 12 個點用「x,y x,y ...」串起來給 <polyline> */
const linePoints = computed(() => points.value.map((p) => `${p.x},${p.y}`).join(' '))

/** 背景橫線（虛線）：0、中間、最大值；最大值是 1 時只畫 0 和 1 */
const gridLines = computed(() => {
  const plotHeight = HEIGHT - PAD.top - PAD.bottom
  const values = maxCount.value >= 2 ? [0, maxCount.value / 2, maxCount.value] : [0, maxCount.value]
  return values.map((value) => ({
    // 中間那條不是整數時（例如最大值 3 → 1.5），不標數字
    label: Number.isInteger(value) ? value : '',
    y: PAD.top + plotHeight * (1 - value / maxCount.value),
  }))
})
</script>

<template>
  <div ref="wrapper" class="chart">
    <svg
      :viewBox="`0 0 ${width} ${HEIGHT}`"
      :width="width"
      :height="HEIGHT"
      role="img"
      aria-label="最近 12 個月的收藏趨勢"
    >
      <!-- 背景橫線與左側刻度 -->
      <g v-for="line in gridLines" :key="line.y">
        <line class="grid" :x1="PAD.left" :x2="width - PAD.right" :y1="line.y" :y2="line.y" />
        <text class="axis" :x="PAD.left - 10" :y="line.y + 4" text-anchor="end">{{ line.label }}</text>
      </g>

      <!-- 折線 -->
      <polyline class="line" :points="linePoints" />

      <g v-for="point in points" :key="point.key">
        <!-- 資料點 -->
        <circle class="dot" :cx="point.x" :cy="point.y" r="4" />
        <!-- 數值不是 0 才在點的上方標數字 -->
        <text v-if="point.count > 0" class="value" :x="point.x" :y="point.y - 12" text-anchor="middle">
          {{ point.count }}
        </text>
        <!-- 橫軸：月份；年份在月份下方 -->
        <text v-if="point.showMonth" class="axis" :x="point.x" :y="HEIGHT - PAD.bottom + 24" text-anchor="middle">
          {{ point.month }}月
        </text>
        <text v-if="point.showYear" class="axis" :x="point.x" :y="HEIGHT - PAD.bottom + 44" text-anchor="middle">
          {{ point.year }}
        </text>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.chart {
  width: 100%;
}

svg {
  display: block;
  max-width: 100%;
}

.grid {
  stroke: var(--color-border);
  stroke-width: 1;
  stroke-dasharray: 4 4; /* 虛線 */
}

.line {
  fill: none;
  stroke: var(--color-text);
  stroke-width: var(--line);
}

.dot {
  fill: var(--color-text);
}

.value {
  fill: var(--color-text);
  font-size: 14px;
  font-weight: 700;
}

.axis {
  fill: var(--color-text-muted);
  font-size: 13px;
}
</style>
