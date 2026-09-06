<template>
  <div v-if="visible" class="booking-overlay" @click.self="close">
    <div class="booking-modal">
      <div class="booking-header">
        <h3 class="booking-title">预约座位 {{ seatLabel }}</h3>
        <button class="booking-close" @click="close" aria-label="关闭">&times;</button>
      </div>

      <div class="booking-body">
        <div class="booking-field">
          <label class="booking-label">预约日期</label>
          <input class="booking-input" type="date" v-model="date" :min="todayStr()" @change="onDateChange" />
          <span class="booking-hint">开放时间 08:00 - 21:30（每 10 分钟一档）</span>
          <span class="booking-hint">当天预约：从当前时间后可约至 21:30；提前一天预约：可约全天</span>
        </div>

        <template v-if="hasEnabledStart">
          <div class="booking-times">
            <div class="booking-time-col">
              <span class="booking-label">开始时间</span>
              <TimeWheel
                :options="allStartOptions"
                :model-value="startTime"
                :disabled-values="disabledStarts"
                @update:model-value="onStartChange"
              />
            </div>
            <span class="booking-time-sep">至</span>
            <div class="booking-time-col">
              <span class="booking-label">结束时间</span>
              <TimeWheel
                v-if="startTime"
                :options="endOptions"
                :model-value="endTime"
                :disabled-values="[]"
                @update:model-value="onEndChange"
              />
              <div v-else class="booking-input booking-input-empty">--:--</div>
            </div>
          </div>
        </template>
        <div v-else class="booking-empty">{{ noAvailableText }}</div>

        <div class="booking-summary">
          <span v-if="durationText">预约时长：{{ durationText }}</span>
          <span v-else-if="!hasEnabledStart">{{ noAvailableText }}</span>
          <span v-else-if="!endTime">请选择结束时间</span>
          <span v-else>请选择时间段</span>
        </div>

        <p v-if="errorMsg" class="booking-error">{{ errorMsg }}</p>
      </div>

      <div class="booking-footer">
        <button class="booking-btn cancel" @click="close">取消</button>
        <button class="booking-btn confirm" :disabled="loading || !canConfirm" @click="confirm">
          {{ loading ? '提交中...' : '确认预约' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getSeatReservations } from '@/api/seat'
import type { SeatReservation } from '@/types/api'
import TimeWheel from './TimeWheel.vue'

interface SeatLike {
  id?: number | string
  seat_no?: string
  seatNo?: string
}

const props = withDefaults(
  defineProps<{
    visible: boolean
    seat: SeatLike | null
    loading: boolean
    initialDate: string
  }>(),
  { visible: false, seat: null, loading: false, initialDate: '' }
)
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', payload: { seatId?: number | string; startTime: string; endTime: string }): void
}>()

// 开放时间：08:00 - 21:30，10 分钟一档；当天预约需提前 10 分钟
const OPEN_START_MIN = 8 * 60 // 08:00
const OPEN_END_MIN = 21 * 60 + 30 // 21:30
const STEP_MINUTES = 10 // 时间间隔 10 分钟
const MIN_DURATION = 10 // 最短预约 10 分钟
const ADVANCE_MINUTES = 10 // 当天预约需至少提前 10 分钟

const date = ref('')
const startTime = ref('')
const endTime = ref('')
const errorMsg = ref('')
// 该座位在所选日期的已预约时段（分钟），用于禁用冲突的时间档位
const blocked = ref<{ start: number; end: number }[]>([])

const pad = (n: number) => String(n).padStart(2, '0')
const todayStr = (): string => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
const slotToMin = (slot: string): number => {
  const [h, m] = String(slot).split(':').map(Number)
  return h * 60 + m
}
const toSlot = (min: number): string => `${pad(Math.floor(min / 60))}:${pad(min % 60)}`

// 开始时间档位：08:00 - 21:20（最晚开始，保证至少 10 分钟）
const allStartOptions: string[] = []
for (let m = OPEN_START_MIN; m <= OPEN_END_MIN - MIN_DURATION; m += STEP_MINUTES) {
  allStartOptions.push(toSlot(m))
}
// 结束时间档位：08:10 - 21:30
const allEndOptions: string[] = []
for (let m = OPEN_START_MIN + MIN_DURATION; m <= OPEN_END_MIN; m += STEP_MINUTES) {
  allEndOptions.push(toSlot(m))
}

const seatLabel = computed(() =>
  props.seat ? props.seat.seat_no || props.seat.seatNo || String(props.seat.id ?? '') : ''
)

// 将接口返回的时间字符串转为分钟数（如 2026-09-03 10:30 -> 630）
const toMin = (dt?: string | null): number => {
  const t = String(dt || '').replace('T', ' ')
  const m = t.match(/(\d{2}):(\d{2})/)
  if (!m) return -1
  return Number(m[1]) * 60 + Number(m[2])
}

// 加载该座位在所选日期的已预约时段
const loadBlocked = async () => {
  if (!props.seat || !date.value) {
    blocked.value = []
    return
  }
  try {
    const res = await getSeatReservations(Number(props.seat.id), date.value)
    let data: unknown = res
    if ((data as { code?: number })?.code === 200) data = (data as { data: SeatReservation[] }).data
    else if ((data as { data?: { code?: number } })?.data?.code === 200)
      data = (data as { data: { data: SeatReservation[] } }).data.data
    const list = Array.isArray(data) ? (data as SeatReservation[]) : []
    blocked.value = list.map((r) => ({ start: toMin(r.startTime), end: toMin(r.endTime) }))
  } catch (err) {
    blocked.value = []
  }
}

// [startMin, endMin] 是否与已预约时段冲突
const overlaps = (startMin: number, endMin: number): boolean =>
  blocked.value.some((b) => startMin < b.end && endMin > b.start)

// 今天只能选当前时间至少 10 分钟之后的档位；非当天（提前预约）可选全天
const isSlotPast = (slot: string, d: string): boolean => {
  if (d !== todayStr()) return false
  const [hh, mm] = slot.split(':').map(Number)
  const t = new Date()
  t.setHours(hh, mm, 0, 0)
  return t.getTime() <= Date.now() + ADVANCE_MINUTES * 60 * 1000
}
const isDisabledStart = (slot: string): boolean => {
  if (isSlotPast(slot, date.value)) return true
  const sm = slotToMin(slot)
  return overlaps(sm, sm + MIN_DURATION)
}

const hasEnabledStart = computed(() => allStartOptions.some((t) => !isDisabledStart(t)))
const disabledStarts = computed(() => allStartOptions.filter((t) => isDisabledStart(t)))
const noAvailableText = computed(() =>
  date.value === todayStr() ? '今天已无可预约时段，请选择其他日期' : '该日期该座位无可预约时段，请选择其他日期'
)

const startMinute = computed(() => (startTime.value ? slotToMin(startTime.value) : 0))

// 结束时间：晚于开始至少 10 分钟、不超过 21:30，且不与已预约时段冲突
const endOptions = computed(() => {
  if (!startTime.value) return []
  const sm = startMinute.value
  return allEndOptions.filter((t) => {
    const em = slotToMin(t)
    return em > sm && em >= sm + MIN_DURATION && em <= OPEN_END_MIN && !overlaps(sm, em)
  })
})

const durationText = computed(() => {
  if (!startTime.value || !endTime.value) return ''
  const mins = slotToMin(endTime.value) - startMinute.value
  if (mins <= 0) return ''
  const h = Math.floor(mins / 60)
  const m = mins % 60
  if (h > 0 && m > 0) return `${h}小时${m}分钟`
  if (h > 0) return `${h}小时`
  return `${m}分钟`
})

const canConfirm = computed(
  () =>
    !!date.value &&
    hasEnabledStart.value &&
    !!startTime.value &&
    !!endTime.value &&
    endOptions.value.includes(endTime.value)
)

// 默认结束：开始后 2 小时，不超过 21:30（闭馆）；不可用则取最后一个可用结束档位
const pickDefaultEnd = (startSlot: string): string => {
  const sm = slotToMin(startSlot)
  const candidate = toSlot(Math.min(sm + 120, OPEN_END_MIN))
  const opts = endOptions.value
  if (!opts.length) return ''
  return opts.includes(candidate) ? candidate : opts[opts.length - 1]
}

// 今天默认开始：当前时间 + 10 分钟后向上取整到 10 分钟档
const nextStartSlot = (): string => {
  const t = new Date(Date.now() + ADVANCE_MINUTES * 60 * 1000)
  let h = t.getHours()
  let m = Math.ceil(t.getMinutes() / STEP_MINUTES) * STEP_MINUTES
  if (m >= 60) {
    m = 0
    h += 1
  }
  let mins = h * 60 + m
  if (mins < OPEN_START_MIN) mins = OPEN_START_MIN
  if (mins > OPEN_END_MIN - MIN_DURATION) mins = OPEN_END_MIN - MIN_DURATION
  return toSlot(mins)
}

const chooseDefaultStart = () => {
  const starts = allStartOptions.filter((t) => !isDisabledStart(t))
  if (starts.length === 0) {
    startTime.value = ''
    endTime.value = ''
    return
  }
  let chosen = starts[0]
  if (date.value === todayStr()) {
    const candidate = nextStartSlot()
    if (starts.includes(candidate)) chosen = candidate
    else {
      const next = starts.find((t) => t > candidate)
      if (next) chosen = next
    }
  }
  startTime.value = chosen
  endTime.value = pickDefaultEnd(chosen)
}

const onStartChange = (v: string) => {
  errorMsg.value = ''
  startTime.value = v
  if (!v) {
    endTime.value = ''
    return
  }
  endTime.value = pickDefaultEnd(v)
}

const onEndChange = (v: string) => {
  errorMsg.value = ''
  endTime.value = v
}

const resetForm = async () => {
  date.value = props.initialDate && props.initialDate >= todayStr() ? props.initialDate : todayStr()
  errorMsg.value = ''
  await loadBlocked()
  chooseDefaultStart()
}

const onDateChange = async () => {
  if (!date.value) return
  await loadBlocked()
  errorMsg.value = ''
  if (!hasEnabledStart.value) {
    startTime.value = ''
    endTime.value = ''
    return
  }
  if (!startTime.value || isDisabledStart(startTime.value)) {
    chooseDefaultStart()
  } else {
    endTime.value = pickDefaultEnd(startTime.value)
  }
}

const confirm = () => {
  errorMsg.value = ''
  if (!date.value || !startTime.value || !endTime.value) {
    errorMsg.value = hasEnabledStart.value ? '请选择完整的时间段' : noAvailableText.value
    return
  }
  if (date.value < todayStr()) {
    errorMsg.value = '预约日期不能早于今天'
    return
  }
  if (isSlotPast(startTime.value, date.value)) {
    errorMsg.value = '开始时间太早，请选择至少10分钟后的时段'
    return
  }
  if (overlaps(startMinute.value, startMinute.value + MIN_DURATION)) {
    errorMsg.value = '该时段与已有预约冲突，请重新选择开始时间'
    return
  }
  if (!endOptions.value.includes(endTime.value)) {
    errorMsg.value = '结束时间无效，请重新选择'
    return
  }
  emit('confirm', {
    seatId: props.seat?.id,
    startTime: `${date.value} ${startTime.value}`,
    endTime: `${date.value} ${endTime.value}`
  })
}

const close = () => emit('update:visible', false)

watch(
  () => props.visible,
  (v) => {
    if (v) resetForm()
  }
)
</script>

<style scoped>
.booking-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 16px;
}
.booking-modal {
  width: 100%;
  max-width: 400px;
  background: var(--bg-primary);
  color-scheme: var(--color-scheme, dark);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  overflow: hidden;
}
.booking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}
.booking-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.booking-close {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}
.booking-close:hover {
  background: var(--sidebar-hover);
  color: var(--text-primary);
}
.booking-body {
  padding: 18px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.booking-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.booking-label {
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
}
.booking-hint {
  font-size: 11px;
  color: var(--text-secondary);
  opacity: 0.85;
  line-height: 1.4;
}
.booking-input {
  background: var(--sidebar-hover);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 10px;
  padding: 9px 12px;
  font-size: 14px;
  outline: none;
}
.booking-input:focus {
  border-color: #667eea;
}
.booking-input-empty {
  text-align: center;
  color: var(--text-secondary);
}
.booking-times {
  display: flex;
  align-items: stretch;
  gap: 4px;
}
.booking-time-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.booking-time-sep {
  align-self: center;
  color: var(--text-secondary);
  font-size: 13px;
  padding-top: 18px;
}
.booking-empty {
  border: 1px dashed var(--border-color);
  border-radius: 10px;
  padding: 26px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
}
.booking-summary {
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
  min-height: 18px;
  text-align: center;
}
.booking-error {
  color: #f44336;
  font-size: 13px;
  margin: 0;
}
.booking-footer {
  display: flex;
  gap: 12px;
  padding: 14px 20px 16px;
  border-top: 1px solid var(--border-color);
}
.booking-btn {
  flex: 1;
  padding: 10px 0;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.booking-btn.cancel {
  background: var(--sidebar-hover);
  color: var(--text-primary);
}
.booking-btn.confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.booking-btn.confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
