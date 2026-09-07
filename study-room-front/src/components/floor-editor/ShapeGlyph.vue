<template>
  <g>
    <!-- 矩形 / 圆角矩形 -->
    <rect
      v-if="s.kind === 'rect' || s.kind === 'roundRect'"
      :x="s.x"
      :y="s.y"
      :width="s.w"
      :height="s.h"
      :rx="s.kind === 'roundRect' ? rx : 0"
      :fill="fill"
      :stroke="stroke"
      :stroke-width="strokeWidth"
    />
    <!-- 椭圆 -->
    <ellipse
      v-else-if="s.kind === 'ellipse'"
      :cx="s.x + s.w / 2"
      :cy="s.y + s.h / 2"
      :rx="Math.max(0.5, s.w / 2)"
      :ry="Math.max(0.5, s.h / 2)"
      :fill="fill"
      :stroke="stroke"
      :stroke-width="strokeWidth"
    />
    <!-- 圆 -->
    <circle
      v-else-if="s.kind === 'circle'"
      :cx="s.x + s.w / 2"
      :cy="s.y + s.h / 2"
      :r="Math.max(0.5, Math.min(s.w, s.h) / 2)"
      :fill="fill"
      :stroke="stroke"
      :stroke-width="strokeWidth"
    />
    <!-- 三角形 -->
    <polygon
      v-else-if="s.kind === 'triangle'"
      :points="trianglePoints"
      :fill="fill"
      :stroke="stroke"
      :stroke-width="strokeWidth"
      stroke-linejoin="round"
    />
    <!-- 菱形 -->
    <polygon
      v-else-if="s.kind === 'diamond'"
      :points="diamondPoints"
      :fill="fill"
      :stroke="stroke"
      :stroke-width="strokeWidth"
      stroke-linejoin="round"
    />
  </g>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { LayoutGraphicShape } from '@/types/layout'

const props = withDefaults(
  defineProps<{
    shape: LayoutGraphicShape
    /** 是否仅显示描边（绘制预览等场景） */
    outline?: boolean
  }>(),
  { outline: false }
)

const s = computed(() => props.shape)
const rx = computed(() => Math.max(0, Math.min(s.value.rx ?? 8, Math.min(s.value.w, s.value.h) / 2)))
const fill = computed(() => (props.outline ? 'none' : s.value.fill || 'rgba(140, 150, 180, 0.20)'))
const stroke = computed(() => s.value.stroke || '#8f96ab')
const strokeWidth = computed(() => s.value.strokeWidth || 1.5)

const trianglePoints = computed(() => {
  const { x, y, w, h } = s.value
  return `${x},${y + h} ${x + w / 2},${y} ${x + w},${y + h}`
})
const diamondPoints = computed(() => {
  const { x, y, w, h } = s.value
  return `${x + w / 2},${y} ${x + w},${y + h / 2} ${x + w / 2},${y + h} ${x},${y + h / 2}`
})
</script>
