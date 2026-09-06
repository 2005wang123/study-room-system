<template>
  <div class="tw">
    <button type="button" class="tw-arrow" :disabled="!canStep(-1)" @click="step(-1)" aria-label="上一个时间">▲</button>
    <div class="tw-viewport" ref="viewportEl" @scroll="onScroll">
      <div class="tw-band" aria-hidden="true"></div>
      <div class="tw-list" :style="{ paddingTop: pad + 'px', paddingBottom: pad + 'px' }">
        <div
          v-for="opt in options"
          :key="opt"
          class="tw-item"
          :class="{ 'is-active': opt === modelValue, 'is-disabled': isDisabledOpt(opt) }"
          :style="{ height: ROW + 'px' }"
          @click="pick(opt)"
        >
          {{ opt }}
        </div>
      </div>
    </div>
    <button type="button" class="tw-arrow" :disabled="!canStep(1)" @click="step(1)" aria-label="下一个时间">▼</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'

const ROW = 40 // 每一档的高度
const VISIBLE = 5 // 可视行数

const props = withDefaults(
  defineProps<{
    // 当前选中时间（HH:mm）
    modelValue: string
    // 可展示的所有档位（含禁用项）
    options: string[]
    // 禁用的档位（灰色且不可停留/选中）
    disabledValues: string[]
  }>(),
  { modelValue: '', options: () => [], disabledValues: () => [] }
)
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const viewportEl = ref<HTMLElement | null>(null)
const pad = ((VISIBLE - 1) * ROW) / 2

const disabledSet = computed(() => new Set(props.disabledValues))
const isDisabledOpt = (opt: string): boolean => disabledSet.value.has(opt)

// 可选中档位下标
const enabledIndexes = computed(() => {
  const list: number[] = []
  props.options.forEach((opt, i) => {
    if (!disabledSet.value.has(opt)) list.push(i)
  })
  return list
})

const currentIndex = computed(() => props.options.indexOf(props.modelValue))

const scrollToIndex = (idx: number, smooth: boolean) => {
  const el = viewportEl.value
  if (!el || idx == null) return
  const max = Math.max(0, el.scrollHeight - el.clientHeight)
  const top = Math.min(Math.max(0, idx * ROW), max)
  if (smooth) {
    el.scrollTo({ top, behavior: 'smooth' })
  } else {
    el.scrollTop = top
  }
}

const selectIdx = (idx: number, smooth: boolean) => {
  scrollToIndex(idx, smooth)
  const opt = props.options[idx]
  if (opt != null && opt !== props.modelValue) {
    emit('update:modelValue', opt)
  }
}

const nearestEnabled = (idx: number): number => {
  const list = enabledIndexes.value
  if (!list.length) return -1
  let best = list[0]
  for (const i of list) {
    const dCur = Math.abs(i - idx)
    const dBest = Math.abs(best - idx)
    if (dCur < dBest || (dCur === dBest && i < best)) best = i
  }
  return best
}

const scrollToCurrent = () => {
  const el = viewportEl.value
  if (!el) return
  let i = props.options.indexOf(props.modelValue)
  if (i < 0) {
    i = enabledIndexes.value[0]
    if (i == null) return
  }
  scrollToIndex(i, false)
}

// 用户滚动停止后，吸附到最近的可用档位
let settleTimer: number | null = null
const onScroll = () => {
  if (settleTimer) clearTimeout(settleTimer)
  settleTimer = window.setTimeout(settle, 120)
}
const settle = () => {
  const el = viewportEl.value
  if (!el || !props.options.length) return
  const idx = Math.round(el.scrollTop / ROW)
  const target = nearestEnabled(idx)
  if (target === -1) return
  selectIdx(target, target !== idx)
}

const step = (dir: number) => {
  const list = enabledIndexes.value
  if (!list.length) return
  const cur = currentIndex.value
  let pos = list.indexOf(cur)
  if (pos === -1) pos = dir > 0 ? -1 : list.length
  const idx = list[pos + dir]
  if (idx == null) return
  selectIdx(idx, true)
}

const canStep = (dir: number): boolean => {
  const list = enabledIndexes.value
  if (!list.length) return false
  const pos = list.indexOf(currentIndex.value)
  if (dir < 0) return pos > 0
  return pos !== -1 && pos < list.length - 1
}

const pick = (opt: string) => {
  if (isDisabledOpt(opt)) return
  const idx = props.options.indexOf(opt)
  if (idx !== -1) selectIdx(idx, true)
}

// 选项 / 选中值 / 禁用项变化后，把当前值滚到中间
watch(
  () => props.options.join('|') + '#' + props.modelValue + '#' + props.disabledValues.join('|'),
  () => {
    nextTick(() => requestAnimationFrame(scrollToCurrent))
  }
)

onMounted(() => {
  nextTick(() => requestAnimationFrame(scrollToCurrent))
})
</script>

<style scoped>
.tw {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}
.tw-arrow {
  width: 100%;
  height: 22px;
  border: none;
  background: transparent;
  color: var(--text-secondary, #9a9aa5);
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  transition: color 0.2s;
  padding: 0;
}
.tw-arrow:hover:not(:disabled) {
  color: var(--text-primary, #e8e8ec);
}
.tw-arrow:disabled {
  opacity: 0.25;
  cursor: default;
}
.tw-viewport {
  position: relative;
  width: 100%;
  height: 200px;
  overflow-y: auto;
  scroll-snap-type: y proximity;
  scrollbar-width: none;
  -webkit-mask-image: linear-gradient(to bottom, transparent, #000 26%, #000 74%, transparent);
  mask-image: linear-gradient(to bottom, transparent, #000 26%, #000 74%, transparent);
}
.tw-viewport::-webkit-scrollbar {
  display: none;
}
.tw-band {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  height: 40px;
  background: var(--sidebar-hover, rgba(255, 255, 255, 0.08));
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.12));
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  pointer-events: none;
  z-index: 1;
}
.tw-list {
  position: relative;
  z-index: 2;
}
.tw-item {
  display: flex;
  align-items: center;
  justify-content: center;
  scroll-snap-align: center;
  font-size: 15px;
  font-variant-numeric: tabular-nums;
  color: var(--text-secondary, #9a9aa5);
  cursor: pointer;
  user-select: none;
  transition: color 0.15s;
}
.tw-item.is-active {
  color: var(--text-primary, #e8e8ec);
  font-weight: 700;
  font-size: 17px;
}
.tw-item.is-disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
</style>
